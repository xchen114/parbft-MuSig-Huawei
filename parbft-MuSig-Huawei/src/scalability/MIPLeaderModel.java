package scalability;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.Properties;

import ilog.concert.IloException;
import ilog.cplex.IloCplex;

public class MIPLeaderModel {
	
	private static IloCplex cplexModel;
	
	private static boolean solved;
	
	private static double optimalObjFunc;
	
	private static int [][][] optXijk;
	
	private static int [][] optLik;
	
	private static int optm;
	
	private static Properties config;
	
	private static boolean isRandom;
	
	private static int nodesNb;
	
	private static int minPeersPerCom;
	
	private static int maxPeersPerCom;
	
	private static int vlID;
	
	private static int vlCapacity;
	
	private static int leaderNb;
	
	private static int maxLNb;
	
	private static double minP2PDelay;
	
	private static double maxP2PDelay;
	
	static double[][] p2pDelays;
	
	static double[][] AverageP2PDelays;
	
	static double[] failurerate;
	
	static double[][] Delay;
	
	static int randomGenCounter;
	
	// probability of failure of each node
	static double minFailProb;
	static double maxFailProb;
	static double[] failProbs;
	
	static double[] byzFailStatus;
	
	static double maxsysFailProb;
	
	static int  fmin;
	
	static double byzFailTh;
	
	static int byzPeersNb;
	
	static double blockSize;
	
	static double blockHeaderSize;
	
	static double blockMetadataSize;
	
	private static String delaysFile;
	
	private static String failuresFile;
	
	private static String iniConfig_FilePath;
	
	private static String outputFileName;
	
	static Map<Integer, String> ID2IP = new HashMap<Integer, String>();
	
	
	// Decision variables
	//static int[][] LikSol =null;
	//static int[][][] Xijk =null;
	//static int[][][] ZijkSol =null;
	//static double[] TSol = null;
	//static double[] T1ckSol = null;
	//static double[] DkSol = null;
	

	public MIPLeaderModel() {
		// TODO Auto-generated constructor stub
		try {
			cplexModel = new IloCplex();
			solved = false;
			optXijk = null;
			optimalObjFunc = Double.MAX_VALUE ;
			config = new Properties();
		} catch (IloException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void getProperties(String filePath) {
		
		try {
			FileInputStream fis = new FileInputStream(filePath);
			try {
				config.load(fis);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
	//public static void initializeModelParam(String propFilePath, int n,  double[][] sysp2pDelays, double[] sysfailProb, double sysFailProb, int sysfmin, double sysblockSize, double sysblockHeaderSize, double sysblockMetadataSize)
	public static void initializeModelParam(String propFilePath, String initConfigFilePath) {
		
		iniConfig_FilePath  = initConfigFilePath;

		
		getProperties(propFilePath); 			
		/* Number of peers in the system */
		
		isRandom = Boolean.parseBoolean(config.getProperty("Random"));
		
		System.out.println("isRandom="+isRandom);
		if(!isRandom) {
			readConfig();
			/*for (int i : ID2IP.keySet()) {
				 System.out.println("key: " + i + " value: " + ID2IP.get(i));
			}*/
			
			System.out.println("ID2IP.size(): " + ID2IP.size());
			nodesNb = ID2IP.size();
		}
		else {
			nodesNb = Integer.parseInt(config.getProperty("PeersNumber"))+1;
			for (int i = 1; i<=nodesNb; i++) {
				ID2IP.put(i, "ip"+i);
			}
		}
		
		minP2PDelay = Double.parseDouble(config.getProperty("MinP2PDelay"));
		maxP2PDelay = Double.parseDouble(config.getProperty("MaxP2PDelay"));
		p2pDelays = new double[nodesNb][nodesNb];
		
		minFailProb = Double.parseDouble(config.getProperty("MinFailProb"));
		maxFailProb = Double.parseDouble(config.getProperty("MaxFailProb"));
		failProbs =new double[nodesNb];
		byzFailStatus=new double[nodesNb];
		byzPeersNb = 0;

		System.out.println("Number of peers: " + nodesNb);

		vlID = Integer.parseInt(config.getProperty("VLID"));
		
		vlCapacity = Integer.parseInt(config.getProperty("VLCapacity"));
				


		maxsysFailProb = Double.parseDouble(config.getProperty("MaxSystemFailureProbability"));
		
		fmin = Integer.parseInt(config.getProperty("MinNumberOfFaultyPeersPerCommittee"));
		
		byzFailTh = Double.parseDouble(config.getProperty("ByzantineFaultProbabilityThreshold"));
		
		blockSize = Double.parseDouble(config.getProperty("BlockSize"));
		blockHeaderSize = Double.parseDouble(config.getProperty("BlockHeaderSize"));
		blockMetadataSize = Double.parseDouble(config.getProperty("BlockMetadataSize"));
		
		outputFileName = config.getProperty("MIPOutputFileName");
		
		if (isRandom) {
		
			generateRandomData();
			
		}
		else{
			delaysFile = config.getProperty("DelaysFileName");
			failuresFile=config.getProperty("FailuresFileName");
			String delaysFilePath = System.getProperty("user.dir")+ File.separator +"resources" + File.separator + delaysFile+ ".txt";
			String failuresFilePath = System.getProperty("user.dir")+ File.separator +"resources" + File.separator + failuresFile + ".txt";
			importPeersDelays(delaysFilePath);
			importPeersFailures(failuresFilePath);
			for (int i=0; i<failProbs.length; i++) {
				if (failProbs[i] < byzFailTh) {
					byzFailStatus[i] = 0;
				}
				else {
					byzFailStatus[i] = 1;
					byzPeersNb++;
					System.out.println("Peer " + (i+1) + " is byzantine faulty.\n");
				}				
			}
		}
		

		
	}
	
	public static void initializeModelParam(String propFilePath, String initConfigFilePath, int sys_peers_nb) {
		
		config = new Properties();
		iniConfig_FilePath  = initConfigFilePath;
		getProperties(propFilePath); 
		
		if (sys_peers_nb == 0){
			readConfig();
			System.out.println("initializepara ==> ID2IP.size(): " + ID2IP.size());
			nodesNb = ID2IP.size();
			AverageP2PDelays = new double[nodesNb][nodesNb];
			failurerate = new double[nodesNb];
			Delay = new double[nodesNb][nodesNb];
			
			delaysFile = config.getProperty("DelaysFileName");
			failuresFile=config.getProperty("FailurerateFileName");
			String delaysFilePath = System.getProperty("user.dir")+ File.separator +"resources" + File.separator + delaysFile+ ".txt";
			String failuresFilePath = System.getProperty("user.dir")+ File.separator +"resources" + File.separator + failuresFile + ".txt";
			importPeersDelays(delaysFilePath);
			importFailurerate(failuresFilePath);
		}
		// generate random system infor using
		else{
			nodesNb = sys_peers_nb+1;
			for (int i = 1; i<=nodesNb; i++) {
				ID2IP.put(i, "ip"+i);
			}
			AverageP2PDelays = new double[nodesNb][nodesNb];
			failurerate = new double[nodesNb];
			//Delay = new double[nodesNb][nodesNb];
			generateRandomData(sys_peers_nb);
		}
		
		minP2PDelay = Double.parseDouble(config.getProperty("MinP2PDelay"));
		maxP2PDelay = Double.parseDouble(config.getProperty("MaxP2PDelay"));
		p2pDelays = new double[nodesNb][nodesNb];
		
		minFailProb = Double.parseDouble(config.getProperty("MinFailProb"));
		maxFailProb = Double.parseDouble(config.getProperty("MaxFailProb"));
		failProbs =new double[nodesNb];
		byzFailStatus=new double[nodesNb];
		byzPeersNb = 0;

		System.out.println("Number of peers: " + nodesNb);

		vlID = Integer.parseInt(config.getProperty("VLID"));
		
		vlCapacity = Integer.parseInt(config.getProperty("VLCapacity"));
				


		maxsysFailProb = Double.parseDouble(config.getProperty("MaxSystemFailureProbability"));
		
		fmin = Integer.parseInt(config.getProperty("MinNumberOfFaultyPeersPerCommittee"));
		
		byzFailTh = Double.parseDouble(config.getProperty("ByzantineFaultProbabilityThreshold"));
		
		blockSize = Double.parseDouble(config.getProperty("BlockSize"));
		blockHeaderSize = Double.parseDouble(config.getProperty("BlockHeaderSize"));
		blockMetadataSize = Double.parseDouble(config.getProperty("BlockMetadataSize"));
		
		outputFileName = config.getProperty("MIPOutputFileName");
		
		if (sys_peers_nb == 0){
			delaysFile = config.getProperty("DelaysFileName");
			failuresFile=config.getProperty("FailuresFileName");
			String delaysFilePath = System.getProperty("user.dir")+ File.separator +"resources" + File.separator + delaysFile+ ".txt";
			String failuresFilePath = System.getProperty("user.dir")+ File.separator +"resources" + File.separator + failuresFile + ".txt";
			importPeersDelays(delaysFilePath);
			importPeersFailures(failuresFilePath);
			for (int i=0; i<failProbs.length; i++) {
				if (failProbs[i] < byzFailTh) {
					byzFailStatus[i] = 0;
				}
				else {
					byzFailStatus[i] = 1;
					byzPeersNb++;
					System.out.println("Peer " + (i+1) + " is byzantine faulty.\n");
				}				
			}
		}
		else{
			generateRandomData();
		}		
	}
	
	
	
	public static void generateRandomData() {
		// delays initialisation, normally, we should retrieve delays from the running system
		// ===========================================
		System.out.println("\n ========== Generate Random delays and failures probabilities ! ==========\n");
		for (int i=0; i<nodesNb; i++) {
			for (int j=0; j<nodesNb; j++) {	
				//p2pDelays[i][j] = Math.random()*100;
				if (i!=j) {
					p2pDelays[i][j] = Math.random()*(maxP2PDelay-minP2PDelay) + minP2PDelay;
				}
				else{
					p2pDelays[i][j] =0.0;
				}
				//System.out.println("p2pDelays["+i+"]["+j+"]="+p2pDelays[i][j]);
			}
			//failProbs[i]=Math.random();
			failProbs[i]=Math.random()*(maxFailProb-minFailProb) + minFailProb;
			//System.out.println("failProbs["+i+"]="+failProbs[i]);
		}
		
		String spec = nodesNb + "_" + minP2PDelay + "-" + maxP2PDelay + "_" + minFailProb + "-" + maxFailProb;
		write2File(p2pDelays, failProbs, spec);
	}
	public static void generateRandomData(int sys_peers_nb) {
		// delays initialisation, normally, we should retrieve delays from the running system
		// ===========================================
		double[][] p2pDelays;
		p2pDelays = new double[sys_peers_nb][sys_peers_nb];
		double minP2PDelay = Double.parseDouble(config.getProperty("MinP2PDelay"));
		double maxP2PDelay = Double.parseDouble(config.getProperty("MaxP2PDelay"));
		
		double[] failProbs;
		failProbs =new double[sys_peers_nb];
		double minFailProb = Double.parseDouble(config.getProperty("MinFailProb"));
		double maxFailProb = Double.parseDouble(config.getProperty("MaxFailProb"));
		
		System.out.println("\n ========== Generate Random delays and failures probabilities ! ==========\n");
		for (int i=0; i<sys_peers_nb; i++) {
			for (int j=0; j<sys_peers_nb; j++) {	
				//p2pDelays[i][j] = Math.random()*100;
				if (i!=j) {
					p2pDelays[i][j] = Math.random()*(maxP2PDelay-minP2PDelay) + minP2PDelay;
				}
				else{
					p2pDelays[i][j] =0.0;
				}
				//System.out.println("p2pDelays["+i+"]["+j+"]="+p2pDelays[i][j]);
			}
			//failProbs[i]=Math.random();
			failProbs[i]=Math.random()*(maxFailProb-minFailProb) + minFailProb;
			//System.out.println("failProbs["+i+"]="+failProbs[i]);
		}
		
		AverageP2PDelays = p2pDelays;
		failurerate = failProbs;
		
		String spec = sys_peers_nb + "_" + minP2PDelay + "-" + maxP2PDelay + "_" + minFailProb + "-" + maxFailProb + "_R" + randomGenCounter;
		//write2File(p2pDelays, failProbs, spec);
		randomGenCounter++;
	}	
	
	public static void buildModel() {
		minPeersPerCom = 3*fmin+1;
		System.out.println("Minimum number of peers/committee = " + minPeersPerCom);
		//leaderNb = nodesNb-1;
		int maxCommittes = Math.abs((nodesNb-1)/(minPeersPerCom));
		System.out.println("maxCommittes= " + maxCommittes + "\tvlCapacity="+vlCapacity);
		maxLNb = Math.min(maxCommittes, vlCapacity) ;
		System.out.println("Maximum number of leaders= " + maxLNb); 
	}
	
	public static void solveProblem() {
		String sep = System.getProperty("file.separator");		
		String propFilePath = System.getProperty("user.dir")+  File.separator + "resources" + File.separator + "config.properties";
		String initConfigFilePath = System.getProperty("user.dir")+  sep + "resources" + sep + "iniconfig.txt";
		
		int iter = 0;
		int maxIter = 5;
		if (nodesNb-1 > 3*byzPeersNb+1) {
		
			//for (int m=1; m<=maxLNb ; m++) {
			for (int m=maxLNb; m>=1 ; m--) {
				MIPFollowerModel lModel = new MIPFollowerModel();
				
				lModel.importModelParam(nodesNb, p2pDelays, failProbs, maxsysFailProb, fmin, byzFailStatus, byzPeersNb);
				
				lModel.buildModel(m, minPeersPerCom);
				if(lModel.solveModel()) {
					int [][][] Xijk = new int[nodesNb][nodesNb][m];
					//int [][] Lik = new int[nodesNb][m];
					double ObjFunc = 0.0;
					Xijk= lModel.getSolutions();
					//Lik = lModel.getLeaders();
					for (int k=0;k<m;k++) {
						//for (int i=0; i<nodesNb;i++) {
						int peersNBPerCommittee=0;
						double committeeCumulDelay = 0.0;
						
						double maxDelayPerCommittee = 0.0;
						
						for (int i=1; i<nodesNb;i++) {
							//for (int j=0; j<nodesNb;j++) {
							if (Xijk[i][i][k] == 1) {
								for (int j=1; j<nodesNb;j++) {
									//if(Xijk[i][j][k]*p2pDelays[i][j] > 0) {
									//ObjFunc = ObjFunc +  Xijk[i][j][k]*p2pDelays[i][j];
									if(Xijk[i][j][k] >=0.9) {
										//System.out.println("Xijk["+i+"]["+j+"]["+k+"]="+Xijk[i][j][k] + "\tp2pDelays["+i+"]["+j+"]=" + p2pDelays[i][j]);
										//committeeCumulDelay = committeeCumulDelay + Xijk[i][j][k]*p2pDelays[i][j];
										//peersNBPerCommittee++;
										maxDelayPerCommittee = Math.max(maxDelayPerCommittee, Xijk[i][j][k]*p2pDelays[i][j]);
									}
									
								}
							}
						}
						
						//ObjFunc = ObjFunc + (committeeCumulDelay/peersNBPerCommittee);
						ObjFunc = Math.max(ObjFunc, maxDelayPerCommittee);
					}
					System.out.println("ObjFunc="+ObjFunc + "\toptimalObjFunc="+optimalObjFunc);
					if (ObjFunc < optimalObjFunc) {
						optimalObjFunc = ObjFunc;
						optXijk = Xijk;
						//optLik = Lik;
						optm = m;
						solved=true;
					}
					else {
						iter++;
					}
					if (iter >=maxIter) {
						System.out.println("Objective Function is increasing for "+maxIter+" time: algorithm stopped!");
						break;
					}
				}
				
			}
		}
		else {
			System.out.println("\n !!! It is not possible to partion "+ (nodesNb-1) + " when " + byzPeersNb + " of them are potentially byzantine failed!");
		}
	}
	
	public static void printSolutions() {
		System.out.println("\n==========================================" );
		System.out.println("========= Leader Problem Logging =========" );
		System.out.println("==========================================\n" );
		if (solved) {
			System.out.println("The minimal objective function: "+optimalObjFunc + " is obtained with "+ optm + " committees.\n" );
			System.out.println("The Committees Leaders are:\n");
			for (int k=0;k<optm;k++) {
				for (int i=0; i<nodesNb;i++) {
					//if (optLik[i][k]>=0.999) {	System.out.println("Lik["+i+"]["+k+"]="+optLik[i][k]);
						for (int j=0; j<nodesNb;j++) {
							if (i==0 && optXijk[i][j][k] >= 0.9) {
								System.out.println("Xijk["+i+"]["+j+"]["+k+"]="+optXijk[i][j][k]);
							}
							else if (j==0 && optXijk[i][j][k] >= 0.9) {
								System.out.println("Xijk["+i+"]["+j+"]["+k+"]="+optXijk[i][j][k]);
							}
							else if (i!=0 && i==j && optXijk[i][j][k] >= 0.9) {
								System.out.println("\tXijk["+i+"]["+j+"]["+k+"]="+optXijk[i][j][k]);
							}
							else if (i!=0 && i!=j && optXijk[i][j][k] >= 0.9) {
								System.out.println("\t\tXijk["+i+"]["+j+"]["+k+"]="+optXijk[i][j][k]);
							}
						}
					//}	
				}
			}
		}
		else {
			System.out.println("No solutions to print!");
		}
	}
	
	
	public static void write2File(double[][] p2pDelays, double[] failProbs, String spec) {
		
		String delaysFilePath = System.getProperty("user.dir")+ File.separator +"resources" + File.separator + "delays_" + spec +".txt";
		String failuresFilePath = System.getProperty("user.dir")+ File.separator +"resources" + File.separator + "failures_" + spec + ".txt";
		try {
			PrintWriter writerd = new PrintWriter(delaysFilePath, "UTF-8");
			PrintWriter writerf = new PrintWriter(failuresFilePath, "UTF-8");
			
			for (int i=0; i<nodesNb; i++) {
				String line_i = "";
				for (int j=0; j<nodesNb; j++) {	
					line_i = line_i + p2pDelays[i][j]+ "\t";
				}
				writerd.write(line_i + "\n");
				writerf.write(i+ "\t"+failProbs[i]+ "\n");
			}
			writerd.close();
			writerf.close();
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public static void importPeersDelays(String delaysFilePath) {
		
		try {
			
			File filed = new File(delaysFilePath);			
			BufferedReader br = new BufferedReader(new FileReader(filed));

			String line;
			int i = 0;
	        try {
				while((line=br.readLine())!=null && (i<nodesNb))  { 
					//String printSTR = "";
					String[] parts = line.split("\t");
					//System.out.println(line + "\n" );
					for (int j=0; j<nodesNb; j++) {
						//System.out.println(parts[j].trim() + "\n" );
						p2pDelays[i][j] = Double.parseDouble(parts[j]);
						//printSTR = printSTR + p2pDelays[i][j] + "\t";
					}
					//System.out.println(printSTR + "\n" );
					i++;
					//line=br.readLine();
				}
				br.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}    
	
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		/*try {
			
			File filed = new File(delaysFilePath);			
			BufferedReader br = new BufferedReader(new FileReader(filed));

			String line;
			int i = 0;
	        try {
				while((line=br.readLine())!=null)  { 
					String printSTR = "";
					String[] parts = line.split("\t");
					//System.out.println(line + "\n" );
					for (int j=0; j<parts.length; j++) {
						//System.out.println(parts[j].trim() + "\n" );
						p2pDelays[i][j] = Double.parseDouble(parts[j].trim());
						printSTR = printSTR + p2pDelays[i][j] + "\t";
					}
					//System.out.println(printSTR + "\n" );
					i++;
					//line=br.readLine();
				}
				br.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}    
	
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		
	}
	
	public static void importFailurerate(String failurerateFilePath) {
		try {
			
			File filef = new File(failurerateFilePath);			
			BufferedReader br = new BufferedReader(new FileReader(filef));

			String line;
			int i = 0;
	        try {
				while( ((line=br.readLine())!=null) && (i< nodesNb))  { 
					failurerate[i] = Double.parseDouble(line);
					//System.out.println(i + "\t" + failProbs[i] + "\n" );
					//System.out.println(failurerate[i] + "\t" + i);
					i++;
					//line=br.readLine();
				}
				br.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}    
	
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	public static void importPeersFailures(String failuresFilePath) {	
		
		try {
			
			File filef = new File(failuresFilePath);			
			BufferedReader br = new BufferedReader(new FileReader(filef));

			String line;
			int i = 0;
	        try {
				while( ((line=br.readLine())!=null) && (i< nodesNb))  { 
					failProbs[i] = Double.parseDouble(line);
					//System.out.println(i + "\t" + failProbs[i] + "\n" );
					System.out.println(failProbs[i] + "\t" + i);
					i++;
					//line=br.readLine();
				}
				br.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}    
	
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		/*try {
			
			File filef = new File(failuresFilePath);			
			BufferedReader br = new BufferedReader(new FileReader(filef));

			String line;
			int i = 0;
	        try {
				while((line=br.readLine())!=null)  { 
					String[] parts = line.split("\t");
					failProbs[i] = Double.parseDouble(parts[1].trim());
					//System.out.println(i + "\t" + failProbs[i] + "\n" );
					i++;
					//line=br.readLine();
				}
				br.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}    
	
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		
	}
	
	
	public static void readConfig() {
		try {
			
			File file = new File(iniConfig_FilePath);			
			BufferedReader br = new BufferedReader(new FileReader(file));

			String line;
			int i = 0;
			// VL has ID = 1
			int id = 0;//2;
	        try {
				while((line=br.readLine())!=null && !line.contains("###"))  { 
					// @<127.0.0.1:22000>=[2,CL]#
					String ip =  line.substring(line.indexOf("@")+1, line.indexOf("="));
                    String attr = line.substring(line.indexOf("=")+1, line.indexOf("#"));
                    String idStr = line.substring(line.indexOf("#")+1, line.indexOf("%"));
					String[] parts = attr.split(",");
                    if (parts[1].contains("VL")) {
                    	ID2IP.put(1,ip);
                    }
                    else {
                    	id = Integer.parseInt(idStr);
                    	ID2IP.put(id,ip);
                    	//System.out.println(id + "\t" + ip + "\n" );
                    	//id++;
                    }
					
					i++;
				}
				br.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}    
	
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	public static void writeConfig() {
		// iniConfig_FilePath
		String FilePath = System.getProperty("user.dir")+ File.separator +"resources" + File.separator  +  outputFileName;// "intConfigOpt.txt";
		
		if (solved) {
			
			try {
				PrintWriter writerf = new PrintWriter(FilePath, "UTF-8");
				
				String line_i = "";
				
				for (int i : ID2IP.keySet()) {
					if(i==1) {
						line_i = "@" + ID2IP.get(1) + "=[0,VL]#\n";
						writerf.write(line_i);
					}
					else {
						for (int k=0;k<optm;k++) {
							for (int j=1; j<nodesNb;j++) {
								if (optXijk[i-1][j][k] >=0.9 && j!=(i-1)) {
									line_i = "@" + ID2IP.get(j+1) + "=[" + (k+1) + ",CF]#\n"; 
									writerf.write(line_i);
								}
								else if (optXijk[i-1][j][k] >=0.9 && j==(i-1)) {
									line_i = "@" + ID2IP.get(i) + "=[" + (k+1) + ",CL]#\n"; 
									writerf.write(line_i);
								}
								
							}
						}
					}
				
				}
				
				writerf.close();
				
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		else {
			System.out.println("*initconfig* file is not updated because optimisation problem is not solved!");
		}
		
	}
	
	public static double getOptimalObjFunc(){
		return optimalObjFunc;
	}
	
	/*public static void main(String[] args) {
		// TODO Auto-generated method stub

	}*/


}
