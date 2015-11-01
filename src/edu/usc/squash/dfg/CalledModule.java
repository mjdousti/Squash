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

public class CalledModule{
	Module module;
	ArrayList<String> ops;
	
	public CalledModule(Module module, ArrayList<String> ops){
		this.module = module;
		this.ops = ops;
	}
	public Module getModule(){
		return module;
	}
	
	public ArrayList<String> getOps(){
		return ops;
	}
}
