/*
 *
 * Copyright (C) 2015 Mohammad Javad Dousti, Alireza Shafaei, and Massoud Pedram, SPORT lab,
 * University of Southern California. All rights reserved.
 *
 * Please refer to the LICENSE file for terms of use.
 *
*/


package edu.usc.squash.dfg;

public class Operand {
	String name;
	boolean isArray;
	int length;
	
	public Operand(String name, boolean isArray, int length) {
		this.name = name;
		this.length = length;
		this.isArray=isArray;
	}
	
	public String getName(){
		return name;
	}
	
	public boolean isArray(){
		return isArray;
	}
	
	public int getLength(){
		return length;
	}
	
	public void setLength(int c){
		if (c==-1){
//			System.err.println("FUCK");
			System.exit(-1);			
		}
			
		length = c;
	}
	
	@Override
	public String toString() {
		return getName();
	}
}
