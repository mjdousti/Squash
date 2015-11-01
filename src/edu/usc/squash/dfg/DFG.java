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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;

import edu.usc.squash.Library;
import edu.usc.squash.RuntimeConfig;

public class DFG {
	private TreeMap<String, Vertex> dependencyList=new  TreeMap<String, Vertex>();
	private Set<Module> modules = new HashSet<Module>();
	private Set<String> ancillaList = new HashSet<String>();
	
//	private int ancillaQubitNo=0;
	private int operationNo=0;
	List<Vertex> operations=new ArrayList<Vertex>();
	private SimpleDirectedGraph<Vertex, DefaultEdge> QODG;
	private Vertex startVertex, endVertex;
	long totalWeight=0;
	Library library;
	
	private ArrayList<Boolean> usedCores=new ArrayList<Boolean>();

	public DFG(Library ml){
		library=ml;
		QODG = new SimpleDirectedGraph<Vertex, DefaultEdge>(DefaultEdge.class);
//		usedCores.add(new Boolean(true));	//by default one core is required
	}
	
	public void setPartition(int vertex, int partNo){
		if (operations.get(vertex).isSentinel()){	//assertion
			System.err.println("The sentinel "+operations.get(vertex) +" is partitioned!");
			System.exit(-1);
		}
			
		operations.get(vertex).setPartition(partNo);
//		System.out.println("Assinging "+operations.get(vertex)+" to partition "+partNo+".");

		while(usedCores.size()-1<partNo){
			usedCores.add(new Boolean(false));
		}
		if (usedCores.get(partNo)==false){
			usedCores.set(partNo, new Boolean(true));
		}
	}
	
	public void useSingleCore(){
		if (usedCores.size()>0){
			System.err.println(usedCores.size()+" have already been allocated.");
			System.exit(-1);
		}else
			usedCores.add(new Boolean(true));
	}
	
	public ArrayList<Boolean> getCoreUsage(){
		return usedCores;
	}

	
	public void addStart(){
		//adding "start" node in graph
		operations.add(0, new Vertex("start", 0));
		QODG.addVertex(operations.get(0));
		startVertex=operations.get(0);
		incOperationNo();

	}
	
	public void addEnd(){
		//adding "end" node in graph
		operations.add(new Vertex("end", operationNo));
		QODG.addVertex(operations.get(operationNo));
		endVertex=operations.get(operationNo);
		
		for (Iterator<Entry<String, Vertex>> it1 = dependencyList.entrySet().iterator(); it1.hasNext();) {
			Entry<String, Vertex> entry = it1.next();
			Vertex v = entry.getValue();
			if (v!=null)
				QODG.addEdge(v, endVertex);
//			else
//				System.out.println(entry.getKey());
		}

	}
	//Some auxiliary functions
	public Vertex getParent(Vertex v){
		DefaultEdge dwe=QODG.incomingEdgesOf(v).iterator().next();
		return QODG.getEdgeSource(dwe);
	}
	public Vertex getChild(Vertex v){
		DefaultEdge dwe=QODG.outgoingEdgesOf(v).iterator().next();
		return QODG.getEdgeTarget(dwe);
	}

//	Getters
	public Vertex getStartVertex(){
		return startVertex;
	}
	
	public Vertex getEndVertex(){
		return endVertex;
	}
	
	
	public SimpleDirectedGraph<Vertex, DefaultEdge> getDFG(){
		return QODG;
	}
	
	public List<Vertex> getOperationsList(){
		return operations;		
	}
	
	public String[] getQubitList(){
		String[] qubits=new String[dependencyList.size()];
		Iterator<Entry<String, Vertex>> it=dependencyList.entrySet().iterator();
		int i=0;
		while (it.hasNext()){
			qubits[i]=it.next().getKey();
			i++;
		}
		return qubits;
	}
//	Print functions
	public void printQMDG(){
		for (DefaultEdge e : QODG.edgeSet()) {
			System.out.println(e.toString());                    
		}
	}
	
	

	public void printOperations(){
		for (int i = 0; i < operations.size(); i++) {
		 	System.out.println(operations.get(i));
		}
	}
	
	public void printDependancyList(){
		for (Map.Entry<String, Vertex> entry : dependencyList.entrySet())
		{
			System.out.println(entry.getKey());
		}
	}

	/****************************************************************************/
	// For helping parser
	@SafeVarargs
	public static <T> ArrayList<T> createArrayList(T ... elements) { 
		ArrayList<T> list = new ArrayList<T>();  
		for (T element : elements) { 
			list.add(element); 
		} 
		return list; 
	} 

	private void parseError(String token){
		System.err.println("Qubit `"+token+"` is not defined.");
		//TODO: convert to an exception with correct message
		System.exit(-1);
	}

	public void incOperationNo(){
		operationNo++;
	}
	
	private void addToDependencyList(String qubit){
		if (dependencyList.containsKey(qubit)==true){
			System.err.println("Qubit "+qubit+" is already defined.");
			try {
				throw new Exception();
			} catch (Exception e) {
				e.printStackTrace();
			}
			System.exit(-1);
		}
		dependencyList.put(qubit, null);
//		System.out.println("Defining "+qubit);
	}
	
	public void defineQubits(ArrayList<Operand> data, ArrayList<Operand> ancilla){
//		System.out.println("data size: "+data.size());
//		System.out.println("ancilla size: "+ancilla.size());
		for (Operand op: data){
			if (op.isArray){
				for (int i = 0; i < op.getLength(); i++) {
					addToDependencyList(op.getName()+"["+i+"]");
				}
			}else{
				addToDependencyList(op.getName());				
			}
		}
		for (Operand op: ancilla){
			if (op.isArray){
				for (int i = 0; i < op.getLength(); i++) {
					addToDependencyList(op.getName()+"["+i+"]");
					ancillaList.add(op.getName()+"["+i+"]");
				}
			}else{
				addToDependencyList(op.getName());
				ancillaList.add(op.getName());
			}
		}
	}
	
	//Qubit definition for QASM
//	public void defineQubit(String qubit, boolean isAncilla){
////		System.out.println(qubit);
//		addToDependencyList(qubit);
//		if (isAncilla){
//			ancillaList.add(qubit);
//		}
//	}
	
//	public void defineAncilla(String qubit, String array){
//		int arrayLength;
//		if (array!=null){
//			arrayLength = Integer.parseInt(array.substring(1, array.length()-1));
////			ancillaQubitNo+=arrayLength;
//			for (int i = 0; i < arrayLength; i++) {
//				defineQubit(qubit+"[" + i + "]", true);
//			}
//		}else{
//			defineQubit(qubit, true);
////			ancillaQubitNo++;
//		}
//	}
	
	public int getAncillaNo(){
//		return ancillaQubitNo;
		return ancillaList.size();
	}
	public boolean isAncilla(String name){
		return ancillaList.contains(name);
	}
	
	public void setMainDFG(){	//main module has no logical ancilla
//		ancillaQubitNo = 0;
		ancillaList.clear();
	}
	
	public void addInst(String operation, ArrayList<String> operands){
		if (operation.equalsIgnoreCase("Measz")){
//			System.out.println("Measz");
		}
		
		ArrayList<String> extendedOperandList = new ArrayList<String>();//arrays are extended and added to this list
		//Reports error if the used qubits are not defined before
		for (String operand: operands){
			if (!(dependencyList.containsKey(operand) ||
				(operand.indexOf('[')==-1 && dependencyList.containsKey(operand+"[0]")) ||			//Operand is not an array and is not defined
				(operand.indexOf('[')!=-1 && dependencyList.containsKey(operand)))){	//It is an array and is not defined 
				parseError(operand);
			}
			if (dependencyList.containsKey(operand)){
				extendedOperandList.add(operand);
			}else{
				int i=0;
				while (dependencyList.containsKey(operand+"["+i+"]")){
					extendedOperandList.add(operand+"["+i+"]");
					i++;
				}
			}
		}

		//Checking the library to see if it has the required gate
		if (!library.containsNode(operation)){
			System.err.println("The library does not have gate "+operation+ " with "+operationNo+".");
			System.exit(-1);
		}
		
		//Adding the operation in operations list
		Vertex newVertex =new Vertex (library.getNode(operation), operationNo, operands, false); 
		operations.add(newVertex);

		//Adding new operation to the graph
		QODG.addVertex(newVertex);
		if (RuntimeConfig.DEBUG){
//			System.out.println("Vertex "+newVertex+" is added to the QMDG.");
		}

		//Adding an edge to the node which depends on
		for (String operand: extendedOperandList){
			if (dependencyList.get(operand)!=null){
				QODG.addEdge(dependencyList.get(operand), newVertex);
				if (RuntimeConfig.DEBUG){
					System.out.println(dependencyList.get(operand)+"->"+newVertex+" is added.");
				}
			}else{
				QODG.addEdge(startVertex, operations.get(operationNo));
				if (RuntimeConfig.DEBUG){
					System.out.println(startVertex+"->"+newVertex+" is added.");
				}
			}
			//Changing the dependency of its operand to point to itself
			dependencyList.put(operand, newVertex);
		}
		incOperationNo();
	}
	
	public Set<Module> getModules(){
		return modules;
	}
	
	public void addModule(Module m, ArrayList<String> operands){
		modules.add(m);
		
		ArrayList<String> extendedOperandList = new ArrayList<String>();//arrays are extended and added to this list
		//Reports error if the used qubits are not defined before
		for (String operand: operands){
			if (!(dependencyList.containsKey(operand) ||
				(operand.indexOf('[')==-1 && dependencyList.containsKey(operand+"[0]")) ||			//Operand is not an array and is not defined
				(operand.indexOf('[')!=-1 && dependencyList.containsKey(operand)))){	//It is an array and is not defined 
				parseError(operand);
			}
			if (dependencyList.containsKey(operand)){
				extendedOperandList.add(operand);
			}else{
				int i=0;
				while (dependencyList.containsKey(operand+"["+i+"]")){
					extendedOperandList.add(operand+"["+i+"]");
					i++;
				}
			}
		}
		
		//Adding the operation in operations list
		Vertex newVertex =new Vertex (m, operationNo, operands, true); 
		operations.add(newVertex);

		//Adding new operation to the graph
		QODG.addVertex(newVertex);
		if (RuntimeConfig.DEBUG){
			System.out.println("Module "+newVertex+" is added to the QMDG.");
		}
		
		//Adding an edge to the node which depends on
		for (String operand: extendedOperandList){
			if (dependencyList.get(operand)!=null){	//operand is already defined
				if (dependencyList.get(operand)!=newVertex)
					QODG.addEdge(dependencyList.get(operand), newVertex);
			}else{
				QODG.addEdge(startVertex, operations.get(operationNo));
				if (RuntimeConfig.DEBUG){
					System.out.println(startVertex+"->"+newVertex+" is added.");
				}
			}
			//Changing the dependency of its operand to point to itself
			dependencyList.put(operand, newVertex);
		}
		incOperationNo();
	}
}
