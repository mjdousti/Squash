/*
 *
 * Copyright (C) 2015 Mohammad Javad Dousti, Alireza Shafaei, and Massoud Pedram, SPORT lab,
 * University of Southern California. All rights reserved.
 *
 * Please refer to the LICENSE file for terms of use.
 *
*/


package edu.usc.squash;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeSet;

import edu.usc.squash.dfg.Vertex;

public class Resource {
	private int[] phyical;
	private int[] maxPhysical;
	private int[] maxPhysicalUsage;
	private int coreCount;
//	private boolean[] coreAvailability;
	
	private ArrayList<TreeSet<Vertex>> coreAvailabilities;

	private int logical, maxLogical, maxLogicalUsage;
	
	public Resource(int coreCount, int totalPhysical, int totalLogical){
//		coreAvailability = new boolean[coreCount];
		coreAvailabilities = new ArrayList<TreeSet<Vertex>>();
		
		this.coreCount=coreCount;
		phyical = new int[coreCount];
		maxPhysical = new int[coreCount];
		maxPhysicalUsage = new int[coreCount];
		for (int i = 0; i < coreCount; i++) {
			coreAvailabilities.add(new TreeSet<Vertex>());
//			coreAvailability[i] = true; //true means available
			phyical[i]=totalPhysical/coreCount;
			maxPhysical[i]=phyical[i];
			maxPhysicalUsage [i] = 0;
		}
		logical = totalLogical;
		maxLogical = logical;
		maxLogicalUsage = 0;
	}
	
	public boolean checkCoreAvailability(ArrayList<Boolean> coreRequirement, Vertex v){
		if (!v.isModule()){
			System.err.println("Semantic error! This function should be used for modules only.");
			System.exit(-1);
		}
		if (coreRequirement.size()>coreCount){
			System.err.println("Core requirement ("+coreRequirement.size()+") cannot be greater than existing core count ("+coreCount+").");
			System.exit(-1);
		}
		for (int i = 0; i < coreRequirement.size(); i++) {
			if (coreRequirement.get(i)==true && coreAvailabilities.get(i).size()>0)
				return false;
		}
		return true;
	}
	public boolean checkCoreAvailability(int i, Vertex v){
		if (v.isModule()){
			System.err.println("Semantic error! This function should be used for non-modules only.");
			System.exit(-1);
		}
		if (coreAvailabilities.get(i).size()>0 && coreAvailabilities.get(i).iterator().next().isModule()){
			return false;
		}else
			return true;
	}
	
	public void useCore(ArrayList<Boolean> coreRequirement, Vertex v){//v is a module
		if (!v.isModule()){
			System.err.println("Semantic error! This function should be used for modules only.");
			System.exit(-1);
		}
		if (coreRequirement.size()>coreCount){
			System.err.println("Core requirement cannot be greater than existing core count.");
			System.exit(-1);
		}
		for (int i = 0; i < coreRequirement.size(); i++) {
			if (coreRequirement.get(i)==true && coreAvailabilities.get(i).size()>0){
				System.err.println("Core "+i+" is not available but is being requested.");
				System.exit(-1);
			}else if (coreRequirement.get(i)==true){
				coreAvailabilities.get(i).add(v);
			}
		}
	}
	public void useCore(int i, Vertex v){
		if (v.isModule()){
			System.err.println("Semantic error! This function should be used for non-modules only.");
			System.exit(-1);
		}		
		if (coreAvailabilities.get(i).size()==0){
			coreAvailabilities.get(i).add(v);
		}else if (coreAvailabilities.get(i).iterator().next().isModule()){
			System.err.println("Core "+i+" is already occupied by a module.");
			System.exit(-1);
		}else{	//adding v as a non-module vertex to a set of non-modules
			coreAvailabilities.get(i).add(v);
		}
	}
	
	public void returnCore(ArrayList<Boolean> coreRequirement, Vertex v){
		if (coreRequirement.size()>coreCount){
			System.err.println("Core requirement cannot be greater than existing core count.");
			System.exit(-1);
		}
		for (int i = 0; i < coreRequirement.size(); i++) {
			if (coreRequirement.get(i)==true){
				coreAvailabilities.get(i).remove(v);
			}
		}
	}
	
	public void returnCore(int i, Vertex v){
		coreAvailabilities.get(i).remove(v);
	}

	public int getCoreCount(){
		return coreCount;
	}
	

	
	public int getPhysicalAncillaNo(int k){
		return phyical[k];
	}
	public int getLogicalAncillaNo(){
		return logical;
	}
	
	public void usePhysicalAncilla(int index, int ancilla){
		phyical[index]-=ancilla;
		if (maxPhysical[index] - phyical[index] > maxPhysicalUsage[index])
			maxPhysicalUsage[index] = maxPhysical[index] - phyical[index];
	}
	public void returnPhysicalAncilla(int index, int ancilla){
		phyical[index]+=ancilla;
	}
	public void useLogicalAncilla(int ancilla){
		logical-=ancilla;
		if (maxLogical - logical > maxLogicalUsage)
			maxLogicalUsage = maxLogical - logical;
	}
	public void returnLogicalAncilla(int ancilla){
		logical+=ancilla;
	}
	public int getMaxLogicalUsage(){
		return maxLogicalUsage;
	}
}
