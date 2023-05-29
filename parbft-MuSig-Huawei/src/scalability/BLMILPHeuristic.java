package scalability;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;

import scalability.MIPLeaderModel;
import scalability.MIPFollowerModel;
import scalability.Peer;

public class BLMILPHeuristic {
	
	static double[] failurerate;
	
	// threshold on byzantine failure rate to identify peers prone to failures
	static double byz_fail_threshold;
	
	static boolean[] beta_i;
	
	private static boolean solved;
	
	private static int nodesNb;
	
	private static Properties config;
	
	private static String delaysFile;
	
	private static String failuresFile;
	
	static double[][] AverageP2PDelays;
	
	static double[][] Delay;
	
	private static int leaderNb;
	
	private static int followerNb;
	
	private static int maxLNb;
	
	private static double maxcapacity;
	
	private static double requiredcapacity;
	
	private static int fmin;
	
	private static int [] allocate;
	
	private static int maxPeersPerCom;
	
	private static int [] PeersofCom;
	
	
	private static String iniConfig_FilePath;
	
	static Map<Integer, String> ID2IP = new HashMap<Integer, String>();
	
	static Peer[] peersList;
	
	static int[][][][] XijkHeuristicSol =null;
	//static int[][][] ZijkSol =null;
	static double[][] TSol = null;
	static double[][] T1ckSol = null;
	
	static int randomGenCounter;
	
	public static void initializepara(String propFilePath, String initConfigFilePath, int sys_peers_nb) {
		config = new Properties();
		iniConfig_FilePath  = initConfigFilePath;
		getProperties(propFilePath); 
		
		
		
		
		
		
		// TODO add sys_peers_nb as input for quicker input acquisition
		// Download system info from existing files
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
			AverageP2PDelays = new double[nodesNb][nodesNb];
			failurerate = new double[nodesNb];
			//Delay = new double[nodesNb][nodesNb];
			generateRandomData(sys_peers_nb);
		}
		
		
		
		
		
		//AverageP2PDelays = new double[nodesNb][nodesNb];
		//failurerate = new double[nodesNb];
		//Delay = new double[nodesNb][nodesNb];
		
		solved=true;
		
		requiredcapacity = 1;
		
		maxcapacity = 100000;
		
		fmin = Integer.parseInt(config.getProperty("MinNumberOfFaultyPeersPerCommittee"));
		
		leaderNb=(int) Math.min(Math.floor(maxcapacity/requiredcapacity),Math.floor((nodesNb-1)/(3*fmin+1)));
		
		maxPeersPerCom=nodesNb-1-(3*fmin+1)*(leaderNb-1);
		
		followerNb= nodesNb-1-leaderNb;
		
		allocate = new int[followerNb];
		
		PeersofCom =  new int[leaderNb];
		
		System.out.println("initializepara ==> nodesNb: " + nodesNb);
		System.out.println("initializepara ==> leaderNb: " + leaderNb);
		System.out.println("initializepara ==> followerNb: " + followerNb);
		System.out.println("initializepara ==> maxPeersPerCom: " + maxPeersPerCom);
		
		
		// sort(failurerate);
		
		peersList = new Peer[nodesNb-1];
		
		byz_fail_threshold = Double.parseDouble(config.getProperty("ByzantineFaultProbabilityThreshold"));
		
		
		beta_i = new boolean[nodesNb-1];
		for (int i=0;i<beta_i.length;i++){
			peersList[i] = new Peer(failurerate[i], i);
			if(failurerate[i] > 1.-byz_fail_threshold){
				beta_i[i] = true;
			}
			else{
				beta_i[i] = false;
			}
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
						AverageP2PDelays[i][j] = Double.parseDouble(parts[j]);
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

	
	public static void sortPeersByFailureRate(double[] failruerate) {
		//importfailurerate("/Users/xieyifei/eclipse-workspace/SP_ParBFT/resources/FailureRateA.txt" );
        for (int i=0; i<nodesNb; i++) {
        	peersList[i]=new Peer();
        	peersList[i].fr = failurerate[i];
        	peersList[i].identifier = i;
            //System.out.println(p[i].fr + "\t" + p[i].index);
        }
        
        for (int i =1; i<nodesNb; i++) {
        	for (int j =1; j<nodesNb; j++) {
        		Peer temp=null;
        		if  (peersList[i].fr < peersList[j].fr) {
        			temp=peersList[i];
        			peersList[i]=peersList[j];
        			peersList[j]=temp;
        		}
        	}
        }
        
        for (int i=0; i<nodesNb; i++) {
        	System.out.println(peersList[i].fr + "\t" + peersList[i].identifier);
        }
	}
	
	/**
	public static void greedy() {
		
		for (int i=0; i<leaderNb; i++) {
			for (int j=0; j< followerNb; j++) {
				Delay[i][j]= AverageP2PDelays[peersList[i+1].index][peersList[j+leaderNb+1].index] + AverageP2PDelays[peersList[j+leaderNb+1].index][peersList[i+1].index];
				//System.out.println(Delay[i][j]);
			}
		}
		
		
		for (int j=0; j< followerNb; j++) {
			allocate[j]=0;
			PeersofCom[0]=followerNb;
		}
		
		for (int i=1; i<leaderNb; i++) {
			PeersofCom[i]=0;
		}
		
		for (int j=0; j<followerNb; j++) {
			for (int i=1; i< leaderNb; i++) {
				if(Delay[i][j]<Delay[allocate[j]][j] && PeersofCom[i]< maxPeersPerCom-1) {
					// compare the delay between the leader with other peers
					PeersofCom[allocate[j]]= PeersofCom[allocate[j]]-1;
					allocate[j]=i;
					PeersofCom[i]=PeersofCom[i]+1;
				}
			}
			//for (int i=0; i<leaderNb; i++) {if (PeersofCom[i] > maxPeersPerCom) {}}
		}
	}
	**/
	

	public static Peer[] getMostReliablePeers(Peer[] peersList, double failureRateThreshold) {
		Peer[] mostReliablePeers;
		int c = (int) Arrays.stream(peersList).filter(e -> e.fr < failureRateThreshold).count();
		mostReliablePeers = new Peer[c];

		for (int i = 0; i < c; i++) {
		    mostReliablePeers[i] = new Peer(peersList[i]);
		    //System.out.println("[getMostReliablePeers] mostReliablePeers["+i+"].fr = " + mostReliablePeers[i].fr);
		}
		return mostReliablePeers;

		/*int t = 0;  
		for (int i = 0; i < peersList.length; i++) {
			System.out.println("[getMostReliablePeers] peersList["+i+"].fr = " + peersList[i].fr);
		    if (peersList[i].fr < failureRateThreshold) {
		        t++;    // count elements < failureRateThreshold
		    }
		}
		mostReliablePeers = new Peer[t];
		int j = 0;
		for (int i = 0; i < failurerate.length; i++) {
			System.out.println("[getMostReliablePeers] peersList["+i+"].fr = " + peersList[i].fr);
		    if (failurerate[i] < failureRateThreshold) { 
		    	mostReliablePeers[j] = new Peer();
		    	mostReliablePeers[j].fr = failurerate[i];  
		    	mostReliablePeers[j].index = i;
		        j++;
		    }
		}*/		
		
	}
	

	public static Peer[] getPeersProneToFailures(Peer[] peersList, double failureRateThreshold) {
		Peer[] peersProneToFailures;
		int c = (int) Arrays.stream(peersList).filter(e -> e.fr > 1.-failureRateThreshold).count();
		peersProneToFailures = new Peer[c];
		
		for (int i = peersList.length-1; i > peersList.length-(c+1); i--) {
			//System.out.println("peersProneToFailures.length = " + peersProneToFailures.length +   "\t peersList.length = " + peersList.length);
			
			int j = peersList.length-(i+1);
			//System.out.println(" i = " + i + " \t j = " + j); System.out.println("peersList["+i+"] = " + peersList[i] + " \t peersProneToFailures["+j+"] = " + peersProneToFailures[j]);
			peersProneToFailures[j] = new Peer(peersList[i]);
		    // System.out.println("[peersProneToFailures] peersProneToFailures["+j+"]: {id = " + peersProneToFailures[j].identifier + ", fr = " + peersProneToFailures[j].fr + "}" );
		}
		
		return peersProneToFailures;
		
		/*
		int t = 0;    

		for (int i = 0; i < failurerate.length; i++) {
			System.out.println("[getPeersProneToFailures] failurerate["+i+"] = " + failurerate[i]);
		    if (failurerate[i] >= 1.0-failureRateThreshold) {
		        t++;    // count elements < failureRateThreshold
		    }
		}
		peersProneToFailures = new Peer[t];
		int j = 0;
		for (int i = failurerate.length-1; i >= 0 ; i--) {
		    if (failurerate[i] >= 1.0-failureRateThreshold) { 
		    	peersProneToFailures[j] = new Peer();
		    	peersProneToFailures[j].fr = failurerate[i];  
		    	peersProneToFailures[j].identifier = i;
		        j++;
		    }
		}*/	
	}

	public static Peer[] getRemainingPeers(Peer[] leaderPeers, Peer[][]followersPeers) {
			
		List<Integer> allocatedPeersIdentifiers = new LinkedList<Integer> ();
		int leadersCnt = (int) Arrays.stream(leaderPeers).filter(e -> e != null).count();
		
		for (int i=0; i<leadersCnt; i++){
			allocatedPeersIdentifiers.add(leaderPeers[i].identifier);
			Peer[] currentFollowersPeers = followersPeers[i];
			//System.out.println("currentFollowersPeers.length = " + currentFollowersPeers.length);
			for (int j=0; j<currentFollowersPeers.length; j++){
				//System.out.println("currentFollowersPeers["+j+"]=" + currentFollowersPeers[j]);
				if(currentFollowersPeers[j] != null){
					allocatedPeersIdentifiers.add(currentFollowersPeers[j].identifier);
				}
			}		
		}
		
		Peer[] remainingPeers = new Peer[nodesNb-1-allocatedPeersIdentifiers.size()];
		
		
		//System.out.println("\nremainingPeers.length = " + remainingPeers.length);
		//System.out.println("peersList.length = " + peersList.length);
		//System.out.println("allocatedPeersIdentifiers.size() = " + allocatedPeersIdentifiers.size());
		//for (int i = 0; i<allocatedPeersIdentifiers.size();i++){System.out.println("allocatedPeersIdentifiers["+i+"] = " + allocatedPeersIdentifiers.get(i));}
		
		int ind = 0;
		for (int i=0; i<peersList.length; i++){
			//System.out.println("peersList["+i+"].identifier = " + peersList[i].identifier);
			if(!allocatedPeersIdentifiers.contains(peersList[i].identifier)) {
				//System.out.println("i = " + i + "\t ind = " + ind);
				Peer peer = new Peer(peersList[i]);
				remainingPeers[ind]= peer;
				ind++;
			}			
		}
		
		
		int f = (int) Arrays.stream(remainingPeers).filter(e -> e != null).count();
		System.out.println("remainingPeers non null items = " + f);
		
		return remainingPeers;
	}
	
	
	public static int[] getClosestLeadersToPeer(int peerID, Peer[] leaderPeers){
		//int f = (int) Arrays.stream(leaderPeers).filter(e -> e != null).count();
		int leaderNb = (int) Arrays.stream(leaderPeers).filter(e -> e != null).count(); // leaderPeers.length;
		int[] closestLeadersIDs = new int[leaderNb];
		double[] delaysToLeaders = new double[leaderNb];
		//System.out.println("peerID = "+ peerID + " \t leaderNb = " + leaderNb);
		
		for (int i=0; i<leaderNb; i++){
			//System.out.println("leaderPeers[" + i + "].identifier = " + leaderPeers[i].identifier);			
			delaysToLeaders[i] = AverageP2PDelays[leaderPeers[i].identifier][peerID];
			//+ AverageP2PDelays[peersList[j+leaderNb+1].index][peersList[i+1].index];
			closestLeadersIDs[i] = leaderPeers[i].identifier;			
		}
		
		/*for (int i =1; i<leaderNb; i++) {
        	for (int j =1; j<leaderNb; j++) {
        		double temp;
        		int tempi ;
        		if  (delaysToLeaders[i] < delaysToLeaders[j]) {
        			temp=delaysToLeaders[i];
        			delaysToLeaders[i]=delaysToLeaders[j];
        			delaysToLeaders[j]=temp;
        			
        			tempi = closestLeadersIDs[i];
        			closestLeadersIDs[i] = closestLeadersIDs[j];
        			closestLeadersIDs[j] = tempi;
        		}
        	}
        }	*/	
		return closestLeadersIDs;
	}
	

	public static double runBLMILPHeuristic() {
		
		//boolean bh_solved = false;
		
		// get Beta, number of overall peers prone to byzantine failures
		int Beta  = 0;		
		for(boolean b : beta_i) {
			Beta += b ? 1 : 0;
		}
		
		System.out.println("peersList.length = " + peersList.length);
		System.out.println("leaderNb+followerNb = " + (leaderNb+followerNb));
		System.out.println("Beta " + Beta);
		

				
		if(leaderNb+followerNb < 3*Beta +1){
			System.out.println("NO SOLUTION! High number of peers prone to failure does not allow Byzantine tolerance condition!");
			System.out.println("leaderNb = " + leaderNb + " \t followerNb = " + followerNb + "\t 3*Beta +1 = " + (3*Beta +1));
			return -1.;
		}
		
		
		//sortPeersByFailureRate(failurerate);
		
		Arrays.sort(peersList, new Comparator<Peer>() {
	        public int compare(Peer a, Peer b) {
		        return Double.compare(a.fr, b.fr);
		    }
	        //public int compare(Peer p1, Peer p2) {
	            //return p1.fr.compareTo(p2.fr);
	        //}
	    });
		
		//for (int i =0; i<peersList.length; i++){System.out.println("After sort peersList["+i+"].fr = " + peersList[i].fr);}
		
		Peer[] mostReliablePeers =  getMostReliablePeers(peersList, byz_fail_threshold);
		
		/*System.out.println("List of most reliable peers of this setting:\n");
		for (int i =0; i<mostReliablePeers.length; i++){
			System.out.println("mostReliablePeers["+i+"] = " + mostReliablePeers[i]+ "\t fr = " + mostReliablePeers[i].fr);
		}  */
		
		if (mostReliablePeers.length == 0){
			System.out.println("NO SOLUTION! Not enough reliable peers to serve as Leaders!");
			return -1.;
		}
		
		// in case the number of most reliable peers is strictly less than possible number of peers
		leaderNb = Math.min(leaderNb, mostReliablePeers.length);
		followerNb = nodesNb - 1 - leaderNb;
		System.out.println("leaderNb = " + leaderNb + " \t followerNb = " + followerNb);
		
		
		Peer[]  peersProneToFailures = getPeersProneToFailures(peersList, byz_fail_threshold);
		
		double[] objectiveFunctionsHeuristic = new double[leaderNb];
		for (int i=0; i<objectiveFunctionsHeuristic.length ; i++){
			objectiveFunctionsHeuristic[i]=Double.MAX_VALUE;
		}
		//XijkHeuristicSol = new int[leaderNb][nodesNb][nodesNb][leaderNb];
		
		//int maxLeadersNb = mostReliablePeers.length;
		
		
		// m=0 means no sharding
		for (int m=leaderNb; m>=1 ; m--) {
			int minByzantinePeersNbPerCommittee;
			int maxByzantinePeersNbPerCommittee;
			

			//Peer[][] leaderPeers = new Peer[leaderNb][leaderNb];
			//Peer[][][] followersPeers = new Peer[leaderNb][leaderNb][leaderNb];
			
			Peer[][] leaderPeers = new Peer[m][m];
			Peer[][][] followersPeers = new Peer[m][m][nodesNb-1-m];
			
			/* ================================================
			 * Allocate leaders to committees
			   ================================================ */
			for (int k=0; k<m ; k++){ 
				//XijkHeuristicSol[m][:][:][k]=1;
				// Set up leaders
				System.out.println("mostReliablePeers.length = " + mostReliablePeers.length);
				System.out.println("leaderPeers.length = " + leaderPeers.length);
				System.out.println("m = " + m + "\t k = " + k);
				
				leaderPeers[m-1][k] = mostReliablePeers[k];			
			}
			 
			int minNb = (int) Math.ceil(Beta/m) + 1;
			System.out.println("minNb = " + minNb + "\t Math.ceil(" +Beta+"/"+m+"+1) = " + (Math.ceil(Beta/m)+1) + "\t" +Beta+"/"+m+ " = " + (Beta/m) );
			minByzantinePeersNbPerCommittee = (int) Math.ceil(Beta/m); //(minNb == 0) ? 1 : minNb; 
			maxByzantinePeersNbPerCommittee = (int) Math.ceil(Beta/m) + 1; //(minNb == 0) ? 1 : minNb; // nodesNb-1-(m-1)*(3*1+1); // (nodesNb-1- m -Beta)/m; 
			//int maxPeersNbPerCommittee = 3*Beta; // nodesNb-1-(m-1)*(3*Beta+1);
			int minPeersNbPerCommittee = (int) Math.ceil((nodesNb-1-m)/m);
			int maxPeersNbPerCommittee = (int) Math.ceil((nodesNb-1-m)/m) + 1;;
			
			// TODO: verify how these two variables are computed
			System.out.println();
			System.out.println("minByzantinePeersNbPerCommittee = " + minByzantinePeersNbPerCommittee);
			System.out.println("maxByzantinePeersNbPerCommittee = " + maxByzantinePeersNbPerCommittee);
			System.out.println("minPeersNbPerCommittee = " + minPeersNbPerCommittee);
			System.out.println("maxPeersNbPerCommittee = " + maxPeersNbPerCommittee);
			
			// int f = followersPeers[m][k].length;	// maxPeersPerCom=nodesNb-1-(3*fmin+1)*(leaderNb-1);
						 
			/* ================================================
			   Allocate peers prone to failure to leaders
			   ================================================ */
			for (int j=0; j<peersProneToFailures.length; j++){
				int peerID = peersProneToFailures[j].identifier;
				int[] closestLeadersIDs =  getClosestLeadersToPeer(peerID, leaderPeers[m-1]);
				
				boolean allocated = false;
				//int leaderCnt = 0;
				//int[] processedleadersIDs = new int[];
				List<Integer> processedleadersIDs = new LinkedList<Integer> ();
				while(!allocated & processedleadersIDs.size() < closestLeadersIDs.length){
					int randomLeaderIDArrayIndex = ThreadLocalRandom.current().nextInt(0, closestLeadersIDs.length);
					int randomLeaderID = closestLeadersIDs[randomLeaderIDArrayIndex];
					// count how many peers already allocated to this committee
					int f = (int) Arrays.stream(followersPeers[m-1][randomLeaderIDArrayIndex]).filter(e -> e != null).count();
					//System.out.println("peer " + j + " prone to failure \t f = " + f+ " \t randomLeaderID = " + randomLeaderID + "\t m-1 = " + (m-1));
					if (f < maxByzantinePeersNbPerCommittee){
						followersPeers[m-1][randomLeaderIDArrayIndex][f] = peersProneToFailures[j];
						allocated= true;
					}
					if (!processedleadersIDs.contains(randomLeaderID)){
						processedleadersIDs.add(randomLeaderID);
						//leaderCnt++;
					}
					//System.out.println("processedleadersIDs.size() = " + processedleadersIDs.size());
				}
			}
				
			/* ================================================
			   Allocate remaining peers to leaders
			   ================================================ */	
			/*System.out.println("Allocate remaining peers to leaders");
			for (int k=0; k<followersPeers[m].length; k++){
				for (int j=0; j<followersPeers[m][k].length; j++){
					if (followersPeers[m][k][j] != null){
						System.out.println("followersPeers[" +m+ "][" + k + "].identifier = "+ followersPeers[m][k][j].identifier);
					}
				}
			}*/
			
			
			Peer[]  remainingPeers = getRemainingPeers(leaderPeers[m-1], followersPeers[m-1]);
			
			for (int j=0; j<remainingPeers.length; j++){
				int peerID = remainingPeers[j].identifier;
				int[] closestLeadersIDs =  getClosestLeadersToPeer(peerID, leaderPeers[m-1]);
				
				boolean allocated = false;
				//int leaderCnt = 0;
				//int[] processedleadersIDs = new int[];
				List<Integer> processedleadersIDs = new LinkedList<Integer> ();
				while(!allocated & processedleadersIDs.size() < closestLeadersIDs.length){
					int randomLeaderIDArrayIndex = ThreadLocalRandom.current().nextInt(0, closestLeadersIDs.length);
					int randomLeaderID = closestLeadersIDs[randomLeaderIDArrayIndex];
					// count how many peers already allocated to this committee
					int f = (int) Arrays.stream(followersPeers[m-1][randomLeaderIDArrayIndex]).filter(e -> e != null).count();
					//System.out.println("remaining peer " + j +" \t f = " + f + " \t randomLeaderID = " + randomLeaderID);
					if (f < maxPeersNbPerCommittee){
						followersPeers[m-1][randomLeaderIDArrayIndex][f] = remainingPeers[j];
						allocated= true;
					}
					if (!processedleadersIDs.contains(randomLeaderID)){
						processedleadersIDs.add(randomLeaderID);
						//leaderCnt++;
					}	
					//System.out.println("processedleadersIDs.size() = " + processedleadersIDs.size() + "\t closestLeadersIDs.length = " + closestLeadersIDs.length);
				}						
			}
			
			
			/*System.out.println();
			for (int k=0; k<leaderPeers[m-1].length; k++){
				if (leaderPeers[m-1][k] != null){
					System.out.println("leaderPeers["+(m-1)+"]["+k+"].identifier = " +leaderPeers[m-1][k].identifier);
				}
				for (int j=0; j< followersPeers[m-1][k].length; j++){
					if (followersPeers[m-1][k][j] != null){
						System.out.println("\t followersPeers["+(m-1)+"]["+k+"]["+j+"].identifier = " + followersPeers[m-1][k][j].identifier);
					}
				}
				
			}*/
			objectiveFunctionsHeuristic[m-1] = computeObjFunction(leaderPeers[m-1], followersPeers[m-1]);
			
			
		}
		int minIndex = indexOfSmallest(objectiveFunctionsHeuristic);
		// for (int i =0; i<objectiveFunctionsHeuristic.length ; i++){System.out.println("objectiveFunctionsHeuristic[" + i + "] = " + objectiveFunctionsHeuristic[i]);}
		System.out.println("minIndex = " + minIndex + "\t Min objectiveFunctionsHeuristic[" + minIndex + "] = " + objectiveFunctionsHeuristic[minIndex]);
		
		
		
		
		
		
			/*for (int i = 0; i<closestLeadersIDs.length; i++){
			int f = (int) Arrays.stream(followersPeers[m][i]).filter(e -> e != null).count();
		}				
		int randomLeaderIDArrayIndex = ThreadLocalRandom.current().nextInt(0, closestLeadersIDs.length);
		int randomLeaderID = closestLeadersIDs[randomLeaderIDArrayIndex];
		int f = (int) Arrays.stream(followersPeers[m][randomLeaderIDArrayIndex]).filter(e -> e != null).count();
		if (f <= maxByzantinePeersNbPerCommittee){
			followersPeers[m][randomLeaderIDArrayIndex][f] = peersProneToFailures[j];	
		}*/				
		//int leader_cnt = 0;
		//int closestLeaderID = closestLeadersIDs[leader_cnt];
		//long f = Arrays.stream(followersPeers[m][closestLeaderID]).filter(e -> e != null).count();
		// System.out.println("f = " + f);				
		//followersPeers[m][k][f] =   		
		
	 
		return objectiveFunctionsHeuristic[minIndex];
	}
		
	public static double computeObjFunction(Peer[] leaderPeers, Peer[][] followersPeers){
		double objectiveFunction = 0.0;
		//int followersCnt = (int) Arrays.stream(followersPeers).filter(e -> e != null).count();
		
		for (int k =0; k<leaderPeers.length; k++){
			if (leaderPeers[k] != null){
				int leaderID = leaderPeers[k].identifier;
			
				for (int j=0; j<followersPeers[k].length; j++){
					if (followersPeers[k][j] != null){				
						int followerID = followersPeers[k][j].identifier;				
						objectiveFunction =+ AverageP2PDelays[leaderID][followerID];
						//if(k == 0){	System.out.println("AverageP2PDelays["+leaderID+"]["+followerID+"] = " + AverageP2PDelays[leaderID][followerID]);	}
					}
				}
			}
		}
		
		return objectiveFunction;
	}
		
	public static void writeConfig() {
		// iniConfig_FilePath
		String FilePath = System.getProperty("user.dir")+ File.separator +"resources" + File.separator  +  "heuristicssolution.txt";// "intConfigOpt.txt";
		
		if (solved) {
			
			try {
				PrintWriter writerf = new PrintWriter(FilePath, "UTF-8");
				
				String line_i = "";
				
				
				writerf.write("@" + ID2IP.get(1) + "=[0,VL]#\n");
				
				for (int i=0; i<leaderNb; i++) {
					line_i = "@" + ID2IP.get(peersList[i+1].identifier+1) + "=[" + (i+1) + ",CL]#\n"; 
					writerf.write(line_i);
					for(int j=0; j< followerNb; j++) {
						if (allocate[j] == i) {
							line_i = "@" + ID2IP.get(peersList[j+1+leaderNb].identifier+1) + "=[" + (i+1) + ",CF]#\n"; 
							writerf.write(line_i);
						}
					}
					/*if(i==0) {
						writerf.write("###");
						writerf.write("\r\n");
						writerf.write("\r\n");
					}*/
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
	
	public static int indexOfSmallest(double[] array){

	    // add this
	    if (array.length == 0)
	        return -1;

	    int index = 0;
	    double min = array[index];

	    for (int i = 1; i < array.length; i++){
	        if (array[i] <= min){
	        min = array[i];
	        index = i;
	        }
	    }
	    return index;
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
	
	
	public static void write2File(double[][] p2pDelays, double[] failProbs, String spec) {
		
		String delaysFilePath = System.getProperty("user.dir")+ File.separator +"resources" + File.separator + "delays_" + spec +".txt";
		String failuresFilePath = System.getProperty("user.dir")+ File.separator +"resources" + File.separator + "failures_" + spec + ".txt";
		System.out.println("delaysFilePath = " + delaysFilePath);
		System.out.println("failuresFilePath = " + failuresFilePath);
		try {
			PrintWriter writerd = new PrintWriter(delaysFilePath, "UTF-8");
			PrintWriter writerf = new PrintWriter(failuresFilePath, "UTF-8");
			
			for (int i=0; i<p2pDelays.length; i++) {
				String line_i = "";
				for (int j=0; j<p2pDelays[i].length; j++) {	
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
	
	
	// util functions
	/*public static <T> T[] removeFirstElement(T[] arr) {
        T[] newArr = (T[]) Array.newInstance(arr.getClass(), arr.length - 1); //new T[arr.length - 1];
        for (int i = 1; i < arr.length; i++) {
            newArr[i-1] = arr[i];
        }
        return newArr;
    }*/


	public static float computeAverage(float[] anArray){
        int sum = 0;
 
        // sum of all values in array using for loop
        for (int i = 0; i < anArray.length; i++) {
            sum += anArray[i];
        }
 
        float average = sum/anArray.length;         
        //System.out.println("Average of array : "+average);
        return average;
	}
	
	public static double computeAverage(double[] anArray){
        int sum = 0;
 
        // sum of all values in array using for loop
        for (int i = 0; i < anArray.length; i++) {
            sum += anArray[i];
        }
 
        double average = sum/anArray.length;         
        //System.out.println("Average of array : "+average);
        return average;
	}
	
	public static void saveToFile(String filename, double[] objFunc, float[] execTimes){
		String outFilePath = System.getProperty("user.dir")+ File.separator +"out" + File.separator + filename +".tsv";
		
		try {
			PrintWriter writero = new PrintWriter(outFilePath, "UTF-8");
			
			for (int i=0; i<execTimes.length;i++){
				writero.write(objFunc[i] + "\t" + execTimes[i] + "\n");
			}
			writero.close();
			
		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public static void saveToFile(String filename, double[] objFunc, float[] execTimes, int[] peersNb){
		String outFilePath = System.getProperty("user.dir")+ File.separator +"out" + File.separator + filename +".tsv";
		
		try {
			PrintWriter writero = new PrintWriter(outFilePath, "UTF-8");
			
			for (int i=0; i<execTimes.length;i++){
				writero.write(peersNb[i] + "\t" + objFunc[i] + "\t" + execTimes[i] + "\n");
			}
			writero.close();
			
		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	public static void saveToFile(String filename, double[] objFunc, float[] execTimesB, float[] execTimesS){
		String outFilePath = System.getProperty("user.dir")+ File.separator +"out" + File.separator + filename +".tsv";
		
		try {
			PrintWriter writero = new PrintWriter(outFilePath, "UTF-8");
			
			for (int i=0; i<execTimesB.length;i++){
				writero.write(objFunc[i] + "\t" + (execTimesB[i]+execTimesS[i]) + "\t" + execTimesB[i]  + "\t" + execTimesS[i] + "\n");
			}
			writero.close();
			
		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public static void saveToFile(String filename, double[] objFunc, float[] execTimesB, float[] execTimesS, int[] peersNb){
		String outFilePath = System.getProperty("user.dir")+ File.separator +"out" + File.separator + filename +".tsv";
		
		try {
			PrintWriter writero = new PrintWriter(outFilePath, "UTF-8");
			
			for (int i=0; i<execTimesB.length;i++){
				writero.write(peersNb[i] + "\t" + objFunc[i] + "\t" + (execTimesB[i]+execTimesS[i]) + "\t" + execTimesB[i]  + "\t" + execTimesS[i] + "\n");
			}
			writero.close();
			
		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	public static void main(String[] args) {
		
		int[] testingPeersNb = {50, 100, 150, 200, 500, 1000, 5000};
		int iterationsNB = 100;
		
		float[] execTimesMBAll = new float[testingPeersNb.length];
		float[] execTimesMSAll = new float[testingPeersNb.length];
		double[] objFuncMAll = new double[testingPeersNb.length];
		
		float[] execTimesHAll = new float[testingPeersNb.length];
		double[] objFuncHAll = new double[testingPeersNb.length];
		
		String mainFilenameM = "cplex_exec_objfunc_All_iter_" + iterationsNB + "_N";
		String mainFilenameH = "heuristic_exec_objfunc_All_iter_" + iterationsNB + "_N";
		
		//for (int i = testingPeersNb.length-1; i>=0;i--){	
		for (int i = 0; i<testingPeersNb.length;i++){		
		
			int sys_peers_nb = testingPeersNb[i];
			mainFilenameM += "_" + sys_peers_nb;
			mainFilenameH += "_" + sys_peers_nb;
			
			float[] execTimesMB = new float[iterationsNB];
			float[] execTimesMS = new float[iterationsNB];
			double[] objFuncM = new double[iterationsNB];
			
			float[] execTimesH = new float[iterationsNB];
			double[] objFuncH = new double[iterationsNB];
			
			
			String sep = System.getProperty("file.separator");		
			// iniConfig_FilePath = System.getProperty("user.dir")+  sep + "resources" + sep + "iniconfig_bh.txt";
			String initConfigFilePath = System.getProperty("user.dir")+  sep + "resources" + sep + "iniconfig_bh.txt";
			String propFilePath = System.getProperty("user.dir")+  File.separator + "resources" + File.separator + "config_bh.properties";
			
			
			// Collect and save cplex solution info
			String filenameM = "cplex_exec_objfunc_N_" + sys_peers_nb + "_iter_" + iterationsNB;
			
			for (int iter = 0; iter<iterationsNB; iter++){
				System.out.println("\n=========================================");
				System.out.println("N = "+ testingPeersNb[i] + "\tIteration = " + iter);
				System.out.println("=========================================\n");
				
				// CPLEX Solver Solution				
				long startTimeMB=System.currentTimeMillis();
				MIPLeaderModel lModel = new MIPLeaderModel();
				lModel.initializeModelParam(propFilePath, initConfigFilePath, sys_peers_nb);
				lModel.buildModel();
				long endTimeMB=System.currentTimeMillis();
				
				float excTimeMB=(float)(endTimeMB-startTimeMB)/1000;
				
				execTimesMB[iter]=excTimeMB;
				
				long startTimeMS=System.currentTimeMillis();
				lModel.solveProblem();
				long endTimeMS=System.currentTimeMillis();
				
				float excTimeMS=(float)(endTimeMS-startTimeMS)/1000;	
				
				double minObjFuncM = lModel.getOptimalObjFunc();
				
				System.out.println("building time model =\t"+excTimeMB+ "\tsolving time model =\t"+excTimeMS+ "\t min objective function model=\t" + minObjFuncM);
				
				execTimesMS[iter] = excTimeMS;
				
				objFuncM[iter] = minObjFuncM;
				
				saveToFile(filenameM, objFuncM, execTimesMB, execTimesMS);
				
				
				// Greedy Heurisic Algorithm Solution
				long startTimeH=System.currentTimeMillis();		
	
				// initializepara(propFilePath, iniConfig_FilePath, sys_peers_nb);
				initializepara(propFilePath, initConfigFilePath, sys_peers_nb);
				
				// generate some random delays and failure rates data
				// generateRandomData(sys_peers_nb);
				// System.out.println("Random data generated for a system of "+sys_peers_nb+" peers.");
				
				// run heuristic algorithm given the read input parameters
				double minObjFuncH = runBLMILPHeuristic();
				
				//printSolutions();
				
				//writeConfig();
				long endTimeH=System.currentTimeMillis();
				
				// compute excution time
				float excTimeH=(float)(endTimeH-startTimeH)/1000;		
			    System.out.println("excution time heuristic =\t"+excTimeH+"\t min objective function Heuristic=\t" + minObjFuncH);
			    
			    execTimesH[iter] = excTimeH;
			    objFuncH[iter] = minObjFuncH;
			    
			    
			    
			}
			
			saveToFile(filenameM, objFuncM, execTimesMB, execTimesMS);
			
			execTimesMBAll[i] = computeAverage(execTimesMB);
			execTimesMSAll[i] = computeAverage(execTimesMS);
			objFuncMAll[i] = computeAverage(objFuncM);
			saveToFile(mainFilenameM, objFuncMAll, execTimesMBAll, execTimesMSAll, testingPeersNb);
					
			
			// Collect and save heuristic solution info
			String filenameH = "heuristic_exec_objfunc_N_" + sys_peers_nb + "_iter_" + iterationsNB;
			saveToFile(filenameH, objFuncH, execTimesH);
			
			execTimesHAll[i] = computeAverage(execTimesH);
			objFuncHAll[i] = computeAverage(objFuncH);
			saveToFile(mainFilenameH, objFuncHAll, execTimesHAll, testingPeersNb);
		}
		
		saveToFile(mainFilenameM, objFuncMAll, execTimesMBAll, execTimesMSAll, testingPeersNb);
		saveToFile(mainFilenameH, objFuncHAll, execTimesHAll, testingPeersNb);

	}

}
