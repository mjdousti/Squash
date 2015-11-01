/*
 *
 * Copyright (C) 2015 Mohammad Javad Dousti, Alireza Shafaei, and Massoud Pedram, SPORT lab,
 * University of Southern California. All rights reserved.
 *
 * Please refer to the LICENSE file for terms of use.
 *
*/


package edu.usc.squash.dfg;

import java.util.HashMap;

import org.jgrapht.graph.*;

public class QMDG {
	private SimpleDirectedGraph<Vertex, DefaultEdge> QMDG;
	private HashMap<String, Module> modules;
	
	public QMDG(){
		modules = new HashMap<String, Module>();
		QMDG = new SimpleDirectedGraph<Vertex, DefaultEdge>(DefaultEdge.class);
	}
	
	public void addModule(Module m){
		if (modules.get(m.getName())!=null){
			System.err.println("Module "+ m.getName() + " is redefined.");
			System.exit(-1);
		}else
			modules.put(m.getName(), m);
	}
}
