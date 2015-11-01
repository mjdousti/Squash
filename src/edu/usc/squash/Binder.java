/*
 *
 * Copyright (C) 2015 Mohammad Javad Dousti, Alireza Shafaei, and Massoud Pedram, SPORT lab,
 * University of Southern California. All rights reserved.
 *
 * Please refer to the LICENSE file for terms of use.
 *
*/


package edu.usc.squash;

import java.util.Iterator;

import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;

import edu.usc.squash.dfg.Vertex;
import gurobi.*;

/*
 * This file binds k partitions to k quantum cores
 */

public class Binder {
	public static int [][]calcEdgeCut(int k, SimpleDirectedGraph<Vertex, DefaultEdge> DAG){
		int [][]cut=new int[k][k];
		
		int currentPartition, newPartition;
		for (Vertex v1: DAG.vertexSet()){
			if (v1.isSentinel())	//We do not count the edge cuts for sentinels
				continue;
			
			currentPartition = v1.getPartNo();
			for (DefaultEdge edge: DAG.outgoingEdgesOf(v1)){
				Vertex v2 =DAG.getEdgeTarget(edge);
				
				if (v2.isSentinel())
					continue;
				
				newPartition=v2.getPartNo();
				if (newPartition != currentPartition){
					cut[currentPartition][newPartition]++;
//					System.out.println("("+currentPartition+")"+v1+"->"+"("+newPartition+")"+v2);
				}
			}
		}
		if (RuntimeConfig.DEBUG){		
			System.out.println("Edge Cut:");
			System.out.println("---------");
			for (int i = 0; i < k; i++) {
				for (int j = 0; j < k; j++) {
					System.out.print(cut[i][j]+"\t");
				}
				System.out.println();
			}
		}		
		return cut;
	}
	
	
	public static void bind(int k, int [][]d, SimpleDirectedGraph<Vertex, DefaultEdge> dag, ReQuP requp){
		int []binding=new int[k];
		int [][]w = calcEdgeCut(k, dag);
		
		try {
			GRBEnv env = new GRBEnv();
			if (!RuntimeConfig.GUROBI)	//Disabling the output of Gurobi
				env.set(GRB.IntParam.OutputFlag, 0);
			env.set(GRB.DoubleParam.TimeLimit, Main.getGurobiTimeLimit());	//setting 120sec timeout for Gurobi
			GRBModel  model = new GRBModel(env);


			// Create variables
			GRBVar [][]vars=new GRBVar[k][k];
			for (int i = 0; i < k; i++) {
				for (int j = 0; j < k; j++) {
					vars[i][j] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, null);
				}
			}
			
			// Integrate new variables
			model.update();

			// Set objective
			GRBQuadExpr qexpr = new GRBQuadExpr();
			for (int m = 0; m < k; m++) {
				for (int n = 0; n < k; n++) {
					for (int x = 0; x < vars.length; x++) {
						for (int y = 0; y < vars.length; y++) {
							int coeff = d[n][y] * w[m][x];
							if (coeff != 0)
								qexpr.addTerm(coeff, vars[m][n], vars[x][y]);							
						}
					}
				}
			}
			model.setObjective(qexpr, GRB.MINIMIZE);

			// Add constraints:

			GRBLinExpr expr;
			//Constraint (3) in the paper
			for (int i = 0; i < k; i++) {
				expr = new GRBLinExpr();
				for (int j = 0; j < k; j++) {
					expr.addTerm(1, vars[i][j]);
				}
				model.addConstr(expr, GRB.EQUAL, 1.0, null);
			}

			//Constraint (4) in the paper
			for (int i = 0; i < k; i++) {
				expr = new GRBLinExpr();
				for (int j = 0; j < k; j++) {
					expr.addTerm(1, vars[j][i]);
				}
				model.addConstr(expr, GRB.EQUAL, 1.0, null);
			}
			
			// Optimize model
			model.optimize();
 
			
			/* Retrieving the results */
			for (int i = 0; i < k; i++) {
				for (int j = 0; j < k; j++) {
					if (vars[i][j].get(GRB.DoubleAttr.X)==1){
						binding[i]=j;
						if (RuntimeConfig.BINDING){
							System.out.println("Partition "+i+" is bound to Core "+j+".");
						}
					}
				}
			}
			
			//Assigning each vertex to a core based on the result of binding
			for (Vertex v: dag.vertexSet()){
				if(v.isSentinel())
					continue;
				
				int coreNo = binding[v.getPartNo()];

				v.setCoreNo(coreNo);
				v.setLocX(requp.getCoreCenterX(coreNo));
				v.setLocY(requp.getCoreCenterY(coreNo));
			}

			// Dispose of model and environment
			model.dispose();
			env.dispose();
		} catch (GRBException e) {
			System.out.println("Error code: " + e.getErrorCode() + ". " +
					e.getMessage());
		}
		System.out.println("Partitions are bound successfully.");
	}
}

