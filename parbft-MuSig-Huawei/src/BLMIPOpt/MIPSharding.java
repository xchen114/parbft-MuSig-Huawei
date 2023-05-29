package BLMIPOpt;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import ilog.concert.*;
import ilog.cplex.*;

public class MIPSharding {
	
	private static Map<String, String> ip_attr = new HashMap<String, String>();
	private static int node_amount = 17;
	private static String iniConfig_FilePath;
	// For Xiao's code end 
	
	//ArrayList peersIDs = new ArrayList();
	
	static Map<Integer, String> ID2IP = new HashMap<Integer, String>();

	public static void main(String[] args) {

		
		String sep = System.getProperty("file.separator");		
		iniConfig_FilePath = System.getProperty("user.dir")+  sep + "resources" + sep + "iniconfig.txt";

		String propFilePath = System.getProperty("user.dir")+  File.separator + "resources" + File.separator + "config.properties";
		MIPLeaderModel lModel = new MIPLeaderModel();
		lModel.initializeModelParam(propFilePath, iniConfig_FilePath);
		lModel.buildModel();
		lModel.solveProblem();
		lModel.printSolutions();
		lModel.writeConfig();

	}
	
	
	/* readConfig()
	 * Read the initial node attributes from a conifg-file, and put these attributes to the map as Values,
	 * corresponding to their address in Keys.
     * */
	public static void readConfig() {
		// id =1 is for VL
		int id = 2;
		// Wait until completing node admin, then suspend adminNode().
		while(true) {
			if (ip_attr.size() == node_amount) {
				//adminNodeFlag.setFlagFalse();
				System.out.println("^^^^^^^^^^^^^^^adminNode() done:  Monitor is blocked for admining new nodes.\n\n");
				break;
			} else {
				try { TimeUnit.MICROSECONDS.sleep(100);} catch (Exception e) {System.exit(0);}
				continue;
			}
		}
		
		// Read initial config file.
        File file = new File(iniConfig_FilePath);  
        BufferedReader reader = null;  
        try {  
            reader = new BufferedReader(new FileReader(file));  
            String tempString = null;  
            while (true) {  
            	if ((tempString = reader.readLine()).equals("###")) {
            		break;
            	} else {
                    System.out.println("Monitor reads initial configuration: " + tempString);  
                    String ip = tempString.substring(tempString.indexOf("@")+1, tempString.indexOf("="));
                    String attr = tempString.substring(tempString.indexOf("=")+1, tempString.indexOf("#")); 
                    // BER add 
                    String vlIP = "";
                    String[] parts = attr.split(",");
                    if (parts[1].contains("VL")) {
                    	vlIP = ip;
                    	ID2IP.put(1,vlIP);
                    }
                    // Put the current attribute in ip_attr map corresponding to the Key. 
                    Iterator<Entry<String, String>> it = ip_attr.entrySet().iterator();
                    while (true) {
            	        if(it.hasNext()){
            	        	Entry<String, String> nodeInfo = it.next();
            	        	if (nodeInfo.getKey().equals(ip)) {
            	        		ip_attr.replace(ip, attr);
            	        		// BER Add: allocate an ID to the corresponding node ip
            	        		ID2IP.put(id, ip);
            	        		id++;
            	        	}
            	        	continue;
            	        } else {
                    		break;
            	        }
                    }
            	}
            }  
            reader.close();
        } catch (IOException e) {  
            e.printStackTrace();  
        } finally {  
            if (reader != null) {  
                try {  
                    reader.close();  
                    //adminNodeFlag.setFlagTrue();
                } catch (IOException e1) {  
                }  
            }  
        } 
        
        // Print all nodes and their initial config info in ip_attr map.
        Iterator<Entry<String, String>> it = ip_attr.entrySet().iterator();
        while (true) {
	        if(it.hasNext()){
	        	Entry<String, String> nodeInfo = it.next();
	        	System.out.println("IP:" + nodeInfo.getKey().toString() + " - Attribute:" + nodeInfo.getValue());
	        	continue;
	        } else {
        		break;
	        }
        }
        System.out.println("^^^^^^^^^^^^^^^readConfig() done:  Read initial configration done!\n\n");
	}
	

}
