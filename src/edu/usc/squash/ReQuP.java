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
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;

import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;

import edu.usc.squash.dfg.Module;
import edu.usc.squash.dfg.Vertex;

public class ReQuP {
	private int k;
	private int beta_pmd;
	private int alpha_int;
	private double gamma_cache_L2;
	private SimpleDirectedGraph<Vertex, DefaultEdge> DAG;
	TreeMap<Integer, TreeSet<Vertex>> asapDirectory;

	
	private int l_code;
	private int A_min_i;

	private int L_max_i;
	private int D_L_total;
	private int Q_mem;
	private int A_L_i_max;
	private int D_L_i;
	private int A_L_i;
	private int B_qrcr;
	private int alpha_qrcr;
	private int alpha_core;
	private int alpha_cache_L1;
	private int alpha_cache_L2;
	private int alpha_memory;
	private int[][] routingDelayMatrix;

//	private int B_L;
//	private int A_P_i;
//	private int A_par_i;
	
	public ReQuP(Module module, int k, int b_qrcr, int logicalAncillaBudget, final int Q_L, int beta_pmd, int alpha_int, double gamma_memory, 
			SimpleDirectedGraph<Vertex, DefaultEdge> DAG, Library lib, TreeMap<Integer, TreeSet<Vertex>> asapDirectory) {
		this.k = k;
		this.beta_pmd = beta_pmd;
		this.alpha_int=alpha_int;
		this.DAG = DAG;
		this.l_code = lib.getCodeLength();
		this.A_min_i = lib.getMinNonZeroAncillaBudget();
		this.gamma_cache_L2=gamma_memory;
		this.asapDirectory=asapDirectory;
		this.D_L_total = Q_L;
		


		this.B_qrcr = b_qrcr;
		this.A_L_i_max = logicalAncillaBudget;

		this.D_L_i = module.getDataQubitNo();
		this.A_L_i = module.getAncillaReq();
		
		
		calcL_max_i();
//		calcA_P_i();
		calcQ_mem();
		
		calcAlphaQRCR();
		calcAlphaCore();
		calcAlphaL1Cache();
		calcAlphaL2Cache();
		calcAlphaMem();
		calcInterCoreRoutingDelay(k);

		if (RuntimeConfig.requpa){
			System.out.println("ReQuP Information:");
			System.out.println("------------------");

			System.out.println("l_code: " + l_code);
			System.out.println("B_QRCR: " + B_qrcr);
			System.out.println("max{A_L_i}: " + A_L_i_max);
			
			System.out.println("Q_L: " + this.D_L_total);
			System.out.println("D_L^i: " + D_L_i);
			System.out.println("L_max_i: "+L_max_i);

			
			System.out.println("A_L^i: " + A_L_i);
//			System.out.println("A_par^i: " + A_par_i);

			
			System.out.println("Alpha_QRCR: "+alpha_qrcr);
			System.out.println("Alpha_L1Cache: "+alpha_cache_L1);
			System.out.println("Alpha_L2Cache: "+alpha_cache_L2);
			System.out.println("Alpha_Core: "+alpha_core);
			System.out.println("Alpha_Memory: "+alpha_memory);	
		

			System.out.println("Routing Delay:");
			System.out.println("--------------");
			
			for (int i = 0; i < k; i++) {
				for (int j = 0; j < k; j++) {
					System.out.print(routingDelayMatrix[i][j]+"\t");
				}
				System.out.println();
			}
		}
		System.out.println("ReQuP model is generated successfully.");
	}
	
	
	private void calcL_max_i(){
		/*
		 * D_max is the maximum number of data qubits a core may accommodate.
		 * It can be calculated by referring to the partitioned set of operations for each core
		 */
		ArrayList<HashSet<String>> D = new ArrayList<HashSet<String>>(k);
		
		for (int i = 0; i < k; i++) {
			D.add(new HashSet<String>());
		}
		
		for (Vertex vertex: DAG.vertexSet()){			
			if (vertex.isSentinel() || vertex.isModule())
				continue;
			
			for (int i = 0; i < vertex.getOperandsNumber(); i++) {
				D.get(vertex.getCoreNo()).add(vertex.getOperand(i));	
			}
		}

		L_max_i = 0;
		for (int i = 0; i < k; i++) {
			L_max_i = Math.max(L_max_i, D.get(i).size());
		}
	}
	
	
	private void calcAlphaQRCR(){
		alpha_qrcr = (int) Math.ceil(Math.sqrt(( (B_qrcr*1.0)/(k*1.0) / (A_min_i*1.0)) * l_code*1.0 
				+ (B_qrcr*1.0)/(k*1.0))); 
	}
	
	private void calcAlphaCore(){
		//TODO: check more!
//		if (L_max_i <= Math.ceil((A_P_i*1.0)/(k*1.0) / A_min_i)){
//			if (alpha_qrcr==0){
//				System.err.println("alpha_qrcr is not calculated!");
//				System.exit(-1);
//			} 
//			alpha_core = alpha_qrcr;
//		}else
		alpha_core = (int) Math.ceil(Math.sqrt( Math.ceil(L_max_i*9.0/8.0)*l_code + (B_qrcr*1.0)/(k*1.0)));
		//make sure that alpha_core >=  alpha_qrcr
		alpha_core = Math.max(alpha_core, alpha_qrcr);
	}
	
	
	//Assuming that cache has twice the capacity of the QRCR
	private void calcAlphaL1Cache(){
		//TODO: check later.
//		if (alpha_core==0){
//			System.err.println("alpha_core is not calculated!");
//			System.exit(-1);
//		}else
			alpha_cache_L1 = Math.min((alpha_core-alpha_qrcr)/2, (int) Math.ceil((Math.sqrt(3)-1)/2 * alpha_qrcr));
	}
	
	private void calcAlphaL2Cache(){
		alpha_cache_L2 = (int) Math.ceil((alpha_core*1.0 -  alpha_qrcr*1.0)/2.0 - alpha_cache_L1);
	}
	

	/*
	 * The architecture will have side1 x (k/side1) dimension
	 *  side1 is determined such that it is the largest divisor of k
	 */
	public int getSideX(int k){
		int side1=1;
		for (int i = (int)Math.sqrt(k); i>=1; i--) {
			if (k%i ==0){
				side1 = i;
				break;
			}
		}
		return side1;
	}
	
	public int getCoreCenterX(int coreNo){
		int sideX=getSideX(k);
		int x = (int)( ((coreNo % sideX)*1.0 + 0.5) * alpha_core + 
				(coreNo % sideX) * alpha_int) + alpha_memory;
		
		return x;
	}
	
	public int getCoreCenterY(int coreNo){
		int sideX=getSideX(k);
		int y = (int)( ((coreNo / sideX)*1.0 + 0.5) * alpha_core + 
				(coreNo / sideX) * alpha_int) + alpha_memory;
		
		return y;
	}

	public int getRequpWidth(){
		return 2*alpha_memory + getSideX(k) * alpha_core + (getSideX(k)-1) * alpha_int;
	}
	
	public int getRequpHeight(){
		return 2*alpha_memory + (k/getSideX(k)) * alpha_core + (k/getSideX(k)-1) * alpha_int;
	}
	
	public void calcInterCoreRoutingDelay(int k){
		int intraCoreRoutingDistance = (int)Math.ceil(alpha_qrcr*1.0 + alpha_cache_L1*1.0 + gamma_cache_L2* alpha_cache_L2*1.0)/2;
		//A default value of 0 for arrays of integral types is guaranteed by the Java language spec;
		//No need to initialize it!
		routingDelayMatrix=new int[k][k];
		
		int side1=getSideX(k);
//		int side2=k/side1;
	
		int x1, x2, y1, y2;
		

		for (int i = 0; i < k; i++) {
			for (int j = 0; j < k; j++) {
				x1 = i % side1;
				y1 = i / side1;
				x2 = j % side1;
				y2 = j / side1;
				//calculating the Manhattan distance
				if (i!=j){
					routingDelayMatrix[i][j]= ((Math.abs(x2-x1) + Math.abs(y2-y1)) * (alpha_core + alpha_int)) * beta_pmd;
				}else{
					routingDelayMatrix[i][j] = intraCoreRoutingDistance * beta_pmd;
				}
			}
		}
	}

//	private void calcA_P_i(){
//		int[] perCoreAncillaReq = new int[k];
//		for (Entry<Integer, TreeSet<Vertex>> e: asapDirectory.entrySet()){
//			int[] temp = new int[k];
//			for (Vertex v: e.getValue()){
//				if (!v.isModule()){
//					temp[v.getCoreNo()] += v.getAncillaQubitNo();
//				}
//			}
//			for (int i=0; i<k; i++){
//				perCoreAncillaReq[i] = Math.max(perCoreAncillaReq[i], temp[i]);
//			}
//		}
//		A_par_i=0;
//		for (int i: perCoreAncillaReq){
//			A_par_i = Math.max(A_par_i, i);
//		}
//		
////		A_P_i = Math.min(B_P, k*A_par_i);
//		A_P_i = B_qrcr;
//	}

	
	
	private void calcAlphaMem() {
		int side1 = getSideX(k);
		int n_1_k = (Math.abs(k % side1- 1) + Math.abs(k / side1 - 0));
		int alpha_requp_i = (n_1_k + 1) * alpha_core + (n_1_k + 1) * alpha_int;
		
		alpha_memory = (int) Math.ceil(Math.sqrt(alpha_requp_i*alpha_requp_i + 2*Q_mem) - alpha_requp_i);		
	}


	private void calcQ_mem() {
		Q_mem = (int) ((A_L_i_max - A_L_i)*l_code + Math.ceil(9.0/8.0*(D_L_total - D_L_i))*l_code);
	}


	public int[][] getInterCoreRoutingDelay(){
		return routingDelayMatrix;
	}
	
	public int getTotalUsedPhsicalAncilla(){
//		System.out.println("A_L_i: "+A_L_i);
//		System.out.println("D_L_i: "+D_L_i);
//		System.out.println("(Math.ceil(1.0/8.0 * (A_L_i+D_L_i)): "+ Math.ceil(1.0/8.0 * (A_L_i+D_L_i)));
//		System.out.println("Math.ceil(1.0/8.0*(Q_L - D_L_i)): "+Math.ceil(1.0/8.0*(Q_L - D_L_i)));
		
		return (int) (B_qrcr + (Math.ceil(1.0/8.0 * (A_L_i+D_L_i)) + Math.ceil(1.0/8.0*(D_L_total - D_L_i)))  * l_code);
	}

	public int getCoreCount(){
		return k;
	}
	
	/*
	 * Transformation is done from the current class to the ReQuP provided as an argument
	 */
	public int calcTransformTime(ReQuP requp, int[] ancillaLocationsA, int[] ancillaLocationsB) {
		/*
		 * No time is required to move physical ancilla qubits
		 */
		int routingTime = 0;
		if (k != requp.getCoreCount()){
			for (int i = 0; i < requp.getCoreCount(); i++) {
				if (i < k){	//center to center move cost
					routingTime = Math.max(Math.abs(getCoreCenterX(i) - requp.getCoreCenterX(i)) 
											+ Math.abs(getCoreCenterY(i) - requp.getCoreCenterY(i)), routingTime);				
				}else{ //bringing new physical ancilla from memory
					int distanceFromTop = requp.getCoreCenterY(i);
					int distanceFromBottom = Math.abs(getRequpHeight() - distanceFromTop);
					
					int distanceFromLeft = requp.getCoreCenterX(i);
					int distanceFromRight = Math.abs(getRequpWidth() - distanceFromLeft);
					
					int temp = Math.min(distanceFromTop, distanceFromBottom);
					temp = Math.min(temp, distanceFromLeft);
					temp = Math.min(temp, distanceFromRight);
					
					routingTime = Math.max(routingTime, temp);				
				}
			}
		}
		//cost of providing logical ancilla
		if (ancillaLocationsA!=null && ancillaLocationsB!=null){	//moving ancilla from A to B
			//make a copy to avoid altering the original one
			int[] ancillaLocationsA_copy = new int[ancillaLocationsA.length];
			int[] ancillaLocationsB_copy = new int[ancillaLocationsB.length];

			System.arraycopy(ancillaLocationsA, 0, ancillaLocationsA_copy, 0, ancillaLocationsA.length );
			System.arraycopy(ancillaLocationsB, 0, ancillaLocationsB_copy, 0, ancillaLocationsB.length );
		
			
			for (int i=0; i<ancillaLocationsB.length; i++){
				for (int j=0; j<ancillaLocationsA.length; j++){
					if (ancillaLocationsA[j]>0){
						int temp = Math.min(ancillaLocationsA[j], ancillaLocationsB[i]);
						ancillaLocationsA[j] -=temp;
						ancillaLocationsB[i] -=temp;
						
						routingTime = Math.max(Math.abs(getCoreCenterX(j) - requp.getCoreCenterX(i)) 
								+ Math.abs(getCoreCenterY(j) - requp.getCoreCenterY(i)), routingTime);	
						
						if (ancillaLocationsB[i]==0)
							continue;
					}
				}
			}
		}else if (ancillaLocationsA==null && ancillaLocationsB!=null){	//moving ancilla from memory to B
			for (int i=0; i<ancillaLocationsB.length; i++){
				int distanceFromTop = requp.getCoreCenterY(i);
				int distanceFromBottom = Math.abs(getRequpHeight() - distanceFromTop);
				
				int distanceFromLeft = requp.getCoreCenterX(i);
				int distanceFromRight = Math.abs(getRequpWidth() - distanceFromLeft);
				
				int temp = Math.min(distanceFromTop, distanceFromBottom);
				temp = Math.min(temp, distanceFromLeft);
				temp = Math.min(temp, distanceFromRight);
				
				routingTime = Math.max(routingTime, temp);
			}
		}

        return routingTime*beta_pmd;
	}
	public int getBetaPMD(){
		return beta_pmd;
	}
	
}
