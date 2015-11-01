/*
 *
 * Copyright (C) 2015 Mohammad Javad Dousti, Alireza Shafaei, and Massoud Pedram, SPORT lab,
 * University of Southern California. All rights reserved.
 *
 * Please refer to the LICENSE file for terms of use.
 *
*/


package edu.usc.squash.dfg;

import java.util.ArrayList;
import java.util.HashMap;

import edu.usc.squash.Node;
import edu.usc.squash.ReQuP;
import edu.usc.squash.schedule.QubitInfo;


public class Module extends Node{
	private DFG dfg;
	private ReQuP requp;
	private boolean visited=false;
	private boolean childrenVisited=false;
	private ArrayList<Operand> dataQubits=new ArrayList<Operand>();
	private ArrayList<Operand> ancillaQubits=new ArrayList<Operand>();

	private ArrayList<CalledModule> childModules = new ArrayList<CalledModule>();
	
	private HashMap<String, QubitInfo> dataQubitInitLocation = new HashMap<String, QubitInfo>();
	private HashMap<String, QubitInfo> dataQubitFinalLocation = new HashMap<String, QubitInfo>();
	private int ancillaReq;
	

	
	public Module(String name, ArrayList<Operand> data, ArrayList<Operand> ancilla, int delay) {
		super(name, data.size(), ancilla.size(), delay);
		this.dataQubits.addAll(data);
		this.ancillaQubits.addAll(ancilla);
		
	}
	
	
	public int getDataQubitNo(){
		int size=0;
		for (Operand op: dataQubits) {
			if (op.isArray)
				size+=op.getLength();
			else
				size++;
		}
		return size;
	}
	public int getAncillaQubitNo(){
		int size=0;
		for (Operand op: ancillaQubits) {
			if (op.isArray)
				size+=op.getLength();
			else
				size++;
		}
		return size;
	}
	
	public void setUnvisited(){
		childrenVisited=false;
		visited=false;
	}
	
	public void setAncillaReq(int n){
		ancillaReq=n;
	}
	public int getAncillaReq(){
		return ancillaReq;
	}
	
	public ArrayList<CalledModule> getChildModules(){
		return childModules;
	}
	
	public void addChildModule(Module module, ArrayList<String> ops){
//		ArrayList<Operand> operands = new ArrayList<Operand>();
//		boolean found;
//		for (String op: ops){
//			found =false;
//			for (Operand data: dataQubits){
//				if (data.getName().equals(op)){
//					operands.add(data);
//					found = true;
//				}
//			}
//			if (!found){
//				for (Operand ancilla: ancillaQubits){
//					if (ancilla.getName().equals(op)){
//						operands.add(ancilla);
//						found = true;
//					}
//				}
//			}
//			if (!found){
//				System.err.println("Operand "+op+" is not defined in module "+getName()+".");
//				System.exit(-1);
//			}
//		}
		
		childModules.add(new CalledModule(module, ops));
	}
	
	public void addAncilla(Operand op){
		ancillaQubits.add(op);
	}
	
	public void addDFG(DFG dfg){
		this.dfg=dfg;
	}
	
	public String getOperandName(int i){
		return dataQubits.get(i).getName();
	}
	
	public Operand getOperand(int i){
		return dataQubits.get(i);
	}
	
	public ArrayList<Operand> getArguments(){
		return dataQubits;
	}
	
	public ArrayList<Operand> getAncilla(){
		return ancillaQubits;
	}

	
	public int getOperandLength(String name){
		for (Operand op: dataQubits){
			if (op.getName().equals(name) && op.isArray)
				return op.getLength();
		}
		for (Operand op: ancillaQubits){
			if (op.getName().equals(name) && op.isArray)
				return op.getLength();
		}
		System.err.println("Array "+name + " is not defined in module "+getName()+".");
		System.exit(-1);
		
		return -1;
	}
	public int getOperandCount(){
		return dataQubits.size();
	}
	
	public DFG getDFG(){
		return dfg;
	}
	
	public void setVisited(){
		visited=true;
	}
	
	public void unsetVisited(){
		visited=false;
	}
	public void setChildrenVisited(){
		childrenVisited=true;
	}
	
	public boolean isChildrenVisited(){
		return childrenVisited;
	}
	public boolean isVisited(){
		return visited;
	}

	public void setDataQubitLocation(String op, QubitInfo qInfo) {
		if (dataQubitInitLocation.containsKey(op)){
			dataQubitFinalLocation.put(op, qInfo);
		}else{
			dataQubitInitLocation.put(op, qInfo);
//			System.out.println("Initing " + op + " in module "+ getName());
		}			
	}

	public void setDataQubitLocation(HashMap<String, QubitInfo> qubitsFinalLocation) {
		for (String qubit: qubitsFinalLocation.keySet()) {
			setDataQubitLocation(qubit, qubitsFinalLocation.get(qubit));
		}		
	}

	
	public HashMap<String, QubitInfo> getQubitsInitLocation(){
		return dataQubitInitLocation;
	}
	
	public HashMap<String, QubitInfo> getQubitsFinalLocation(){
		return dataQubitFinalLocation;
	}
	
	public HashMap<String, QubitInfo> getQubitInitLocation(String s){
		HashMap<String, QubitInfo> locs = new HashMap<String, QubitInfo>();
		
		if (dataQubitInitLocation.containsKey(s))
			locs.put(s, dataQubitInitLocation.get(s));
		else{
			for(String it: dataQubitInitLocation.keySet()){
				if (it.startsWith(s+"[")){
					locs.put(it, dataQubitInitLocation.get(it));
				}
			}
		}
		return locs;
	}

	public QubitInfo getQubitFinalLocation(String s){
		if (!dataQubitFinalLocation.containsKey(s))
			return null;
		else
			return dataQubitFinalLocation.get(s);
	}
	public void setRequp(ReQuP requp){
		this.requp = requp;
	}

	public ReQuP getRequp(){
		return requp;
	}

}
