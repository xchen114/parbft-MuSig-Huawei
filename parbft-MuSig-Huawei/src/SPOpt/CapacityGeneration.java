package SPOpt;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map.Entry;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;

import java.util.Properties;
import java.util.Random;

public class CapacityGeneration {
	
	static int scenarioNB;
	static double maxcapacity;
	static double mincapacity;
	static double[] capacity;
	

	
	
	public static void main(String[] args) {
		
		scenarioNB=20;
		mincapacity=5;
		maxcapacity=6;
		capacity = new double [scenarioNB];
		
		try {
			FileWriter out = new FileWriter(new File(System.getProperty("user.dir")+ File.separator +"resources" + File.separator  + "capacities5" + ".txt"));
			for (int s=0; s<scenarioNB; s++) {
				capacity[s]= Math.random()*(maxcapacity-mincapacity) + mincapacity;
				out.write(capacity[s]+"\t");
				out.write("\r\n");
			}
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
