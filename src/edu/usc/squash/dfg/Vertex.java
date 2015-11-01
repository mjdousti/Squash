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

public class Vertex extends Node implements Comparable<Vertex>{
	private int x, y;
	private long scheduledTime=0;
	private int weightVectorIndex;
	private int operationNo;
	private ArrayList<String> operands;
	
	private long path_to_sink=-1;
	private int readyInpts=0;
	//Scheduling level
	private long level, asapLevel, alapLevel;
	private boolean sentinel=false;
	private int partNo=0;
	private int coreNo=0;
	private Module module = null;
	
//	private boolean ready;
//	private List<String> operands;
	
	public Vertex(Node m, int no, ArrayList<String> ops, boolean isModule){
		super(m);
		operationNo=no;
		operands=ops;
		if (isModule)
			module = (Module) m;
	}
	
	//a dummy node (start & end)
	public Vertex(String s, int no){
		if (!s.equalsIgnoreCase("start") && !s.equalsIgnoreCase("end")
				&& !s.equalsIgnoreCase("dummy1") && !s.equalsIgnoreCase("dummy2")){
			System.out.println("Unknown dummy node!");
			try{
				throw new Exception ("WWW");
			}catch (Exception e){
				e.printStackTrace();
			}
			System.exit(-1);
		}
		sentinel=true;
		setName(s);
		operationNo=no;
		operands=new ArrayList<String>();
	}
	
	//***********Getters***********
	private String getModuleParamName(String op){
		if (!isModule()){
			System.err.println(getName()+" is not a module.");
			System.exit(-1);
		}
		String tempOp1, tempOp2, array;
		for (int i=0; i<operands.size(); i++){
			String op2 = operands.get(i);
			if (op.indexOf("[")!=-1){
				tempOp1 = op.substring(0, op.indexOf('['));
				array = op.substring(op.indexOf('['), op.length());
			}else{
				tempOp1 = op;
				array = "";
			}
			if (op2.indexOf("[")!=-1){
				tempOp2 = op2.substring(0, op2.indexOf('['));
			}else{
				tempOp2 = op2;
			}

			if (op2.equalsIgnoreCase(op) || (
					(op.indexOf('['))!=-1 ^ op2.indexOf('[')!=-1) && //make sure either op1 or op2 is an array with index
					tempOp1.equals(tempOp2)){

				return module.getOperandName(i)+array;
			}
		}
		return null;
	}
	
	public HashMap<String, QubitInfo> getInitLocation(String op){
		return module.getQubitInitLocation(getModuleParamName(op));
	}

	public QubitInfo getFinalLocation(String op){
		return module.getQubitFinalLocation(getModuleParamName(op));
	}
	
	public HashMap<String, QubitInfo> getQubitsFinalLocation(){
		HashMap<String, QubitInfo> locs = new HashMap<String, QubitInfo>();
		HashMap<String, QubitInfo> temp = module.getQubitsFinalLocation();
		
		
		//doing name translation
		for (String qubit: temp.keySet()) {
			//TODO: the qubit was an ancilla
			//consider it later
			if (getModuleParamName(qubit)==null){
				continue;
			}
			locs.put(getModuleParamName(qubit), temp.get(qubit));
		}
		
		return locs;
	}
	
	public int[] getAncillaFinalLocation(){
		HashMap<String, QubitInfo> temp = module.getQubitsFinalLocation();
		
		int []perCoreLogicalAncillaCount = new int [module.getRequp().getCoreCount()];
		
		
		//doing name translation
		for (String qubit: temp.keySet()) {
			//The qubit is an ancilla
			if (getModuleParamName(qubit)==null){
				perCoreLogicalAncillaCount[temp.get(qubit).getCoreNo()]++;
			}
		}
		
		return perCoreLogicalAncillaCount;
	}
	
	public HashMap<String, QubitInfo> getQubitsInitLocation(){
		HashMap<String, QubitInfo> locs = new HashMap<String, QubitInfo>();
		HashMap<String, QubitInfo> temp = module.getQubitsInitLocation();
		
		//doing name translation
		for (String qubit: temp.keySet()) {
			//TODO: the qubit was an ancilla
			//consider it later
			if (getModuleParamName(qubit)==null){
				continue;
			}
			locs.put(getModuleParamName(qubit), temp.get(qubit));
		}
		
		return locs;
	}
	
	public int[] getAncillaInitLocation(){
		HashMap<String, QubitInfo> temp = module.getQubitsInitLocation();
		
		int []perCoreLogicalAncillaCount = new int [module.getRequp().getCoreCount()];
				
		//doing name translation
		for (String qubit: temp.keySet()) {
			//The qubit is an ancilla
			if (getModuleParamName(qubit)==null){
				perCoreLogicalAncillaCount[temp.get(qubit).getCoreNo()]++;
			}
		}
		
		return perCoreLogicalAncillaCount;
	}
	
	public int getDataQubitNo(){
		if (module!=null){
			return module.getDataQubitNo();
		}else 
			return super.getDataQubitNo();
	}
	public int getAncillaQubitNo(){
		if (module!=null){
			return module.getAncillaReq();
		}else
			return super.getAncillaQubitNo();
	}
	public long getDelay(){
		if (module!=null)
			return module.getDelay();
		else{
			return super.getDelay();
		}			
	}
	public boolean isModule(){
		return (module==null) ? false : true;
	}
	public DFG getDFG(){
		if (module!=null)
			return module.getDFG();
		else{
			return null;
		}			
	}
	public long getScheduledTime(){
		return scheduledTime;
	}
	public int getWeightVectorIndex(){
		return weightVectorIndex;
	}
	
	public int getCoreNo(){
		return coreNo;
	}
	
	public int getPartNo(){
		return partNo;
	}
	
	public long getLevel(){
		return level;
	}
	public long getASAPLevel(){
		return asapLevel;
	}
	public long getALAPLevel(){
		return alapLevel;
	}
	public int getReadyInputs(){
		return readyInpts;
	}
	public int getOperationNo(){
		return operationNo;
	}
	public long getPath_to_Sink(){
		return path_to_sink;
	}
	
	public boolean isSentinel(){
		return sentinel;
	}
	public String getOperand(int i){
		if (i>operands.size()-1){
			System.err.println("Wrong operand number for "+toString());
			System.exit(-1);
		}
		return operands.get(i);
	}
	
	public ArrayList<String> getOperands(){
		return new ArrayList<String>(operands);
	}

	
	public int getOpNoInCommon(Vertex v){
		int i=0;
		for (int j = 0; j < v.getOperandsNumber(); j++) {
			if (hasOperand(v.operands.get(j)))
				i++;
		}		
		return i;
	}
	public int getOperandsNumber() {
		return operands.size();
	}
//TODO: Should be added again if needed
//	public Qubit[] getOperands(){
//		return operands;
//	}
	
	//***********Setters***********
	public void setScheduledTime(long t){
		scheduledTime = t;
	}
	public void setWeightVectorIndex(int i){
		weightVectorIndex=i;
	}
	
	public void setPartition(int part){
		partNo=part;
	}
	
	public void setCoreNo(int core){
		coreNo=core;
	}
	
	public void setLocX(int x){
		this.x = x;
	}

	public void setLocY(int y){
		this.y = y;
	}
	
	public int getLocX(){
		return x;
	}

	public int getLocY(){
		return y;
	}
	
	public void setLevel(int k){
		level=k;				
	}
	
	public void setASAPLevel(long k){
		asapLevel=k;				
	}
	
	public void setALAPLevel(long k){
		alapLevel=k;				
	}

	public void incReadyInpts(){
		readyInpts++;
	}
	
	public void setPath_to_Sink (long p){
		path_to_sink=p;
	}
	
	public void addPriority (int p){
		path_to_sink+=p;
	}

//	public void setReady(boolean d){
//		ready=d;
//	}	

	public void setOperationsNo(int no){
		operationNo=no;
	}

	public void setReadyInpts(int i) {
		readyInpts=i;		
	}
	
	public String toString() {
		String out="["+String.valueOf(operationNo)+"] "+getName();
		if (!isSentinel())
			out+=" (";
		for (int i = 0; i < getOperandsNumber(); i++) {
			if (i!=getOperandsNumber()-1)
				out+=operands.get(i)+", ";
			else
				out+=operands.get(i);
		}
		if (!isSentinel())
			out+=")";
		return out;
	}

	@Override
	public int compareTo(Vertex o) {
		if (path_to_sink>o.getPath_to_Sink())
			return -1;
		else if (path_to_sink<o.getPath_to_Sink())
			return 1;
		else{

			if (alapLevel - asapLevel < o.getALAPLevel() - o.getASAPLevel())
				return -1;
			else if (alapLevel - asapLevel > o.getALAPLevel() - o.getASAPLevel())
				return 1;
				
			else{
				if (operationNo<o.getOperationNo())
					return -1;
				else if (operationNo>o.getOperationNo())
					return 1;
				else
					return 0;
			}
		}
			
	}
	

	
	public boolean hasOperand(String qubit){
		for (int i = 0; i < getOperandsNumber(); i++) {
			if (operands.get(i).equals(qubit))
				return true;
		}
		return false;
	}
	public ReQuP getRequp(){
		return module.getRequp();
	}


}
