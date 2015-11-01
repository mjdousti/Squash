/*
 *
 * Copyright (C) 2015 Mohammad Javad Dousti, Alireza Shafaei, and Massoud Pedram, SPORT lab,
 * University of Southern California. All rights reserved.
 *
 * Please refer to the LICENSE file for terms of use.
 *
*/


package edu.usc.squash;

import java.util.HashMap;
import java.util.Iterator;

public class Library {
	private String currentECC;
	private HashMap<String, HashMap<String, Node>> library;
	private HashMap<String, Integer> codeLength = new HashMap<String, Integer>();
	
	public Library(Library lib) {
		currentECC=lib.getCurrentECC();
		library = new HashMap<String, HashMap<String, Node>>(lib.getHashTable());		
	}

	public Library() {
		currentECC="";
		library = new HashMap<String, HashMap<String, Node>>();		
	}

	public void setCodeLength(String qecc, int n){
		codeLength.put(qecc, n);
	}

	public int getCodeLength(String qecc){
		return codeLength.get(qecc);
	}
	
	public int getCodeLength(){
		return codeLength.get(currentECC);
	}
	
	public HashMap<String, HashMap<String, Node>> getHashTable(){
		return library;
	}
	
	public int getMinNonZeroAncillaBudget(){
		int ancillaBudget = Integer.MAX_VALUE;
		
		int currentAncilla;
		for (String Node: library.get(currentECC).keySet()){			
			currentAncilla = library.get(currentECC).get(Node).getAncillaQubitNo();
			
			if (currentAncilla>0)
				ancillaBudget = Math.min(ancillaBudget, currentAncilla);
		}
		return ancillaBudget;
	}
	
	public String getCurrentECC(){
		if (currentECC.isEmpty()){
			System.err.println("The current ECC value is not set.");
			System.exit(-1);
		}
		return currentECC;
	}
	
	public void setCurrentECC(String ecc){
		if (ecc.isEmpty()){
			System.err.println("The passed ECC value is empty.");
			System.exit(-1);
		}else if (!library.containsKey(ecc)){
			System.err.println("QECC "+ecc+" is not found.");
			System.exit(-1);
		}
		currentECC= ecc;
	}
	
	public void addNode(String ecc, Node g){
		if (library.containsKey(ecc)){
			library.get(ecc).put(g.getName(), g);
		}else{
			HashMap<String, Node> temp = new HashMap<String, Node>();
			temp.put(g.getName(), g);
			library.put(ecc, temp);
		}
		
	}
	
	public boolean containsNode(String Node){
		if (currentECC.isEmpty()){
			return false;
		}else if (!library.containsKey(currentECC)){
			return false;
		}else if (!library.get(currentECC).containsKey(Node)){
			return false;
		}else{
			return true;
		}
	}
	
	
	public Node getNode(String Node){
		if (currentECC.isEmpty()){
			System.err.println("The current ECC value is not set.");
			System.exit(-1);
			return null;
		}else if (!library.containsKey(currentECC)){
			System.err.println("ECC "+currentECC + " is not found.");
			System.exit(-1);
			return null;
		}else if (!library.get(currentECC).containsKey(Node)){
			System.err.println("Node "+Node + " is not found.");
			try{
				throw new Exception ();
			}catch(Exception e){
				e.printStackTrace();
			}
			System.exit(-1);
			return null;
		}else{
			return library.get(currentECC).get(Node);
		}
	}
	@Override
	public String toString(){
		StringBuffer sb = new StringBuffer();
		for (Iterator<String> it1 = library.keySet().iterator(); it1.hasNext();) {
			String qec = it1.next();
			sb.append("QEC: "+qec+System.lineSeparator());
			for (Iterator<String> it2 = library.get(qec).keySet().iterator(); it2.hasNext();) {
				String Node = it2.next();
				sb.append(library.get(qec).get(Node));
			}
		}
		return sb.toString();
	}
}
