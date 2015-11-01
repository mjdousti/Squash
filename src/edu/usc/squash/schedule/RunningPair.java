/*
 *
 * Copyright (C) 2015 Mohammad Javad Dousti, Alireza Shafaei, and Massoud Pedram, SPORT lab,
 * University of Southern California. All rights reserved.
 *
 * Please refer to the LICENSE file for terms of use.
 *
*/


package edu.usc.squash.schedule;

import edu.usc.squash.dfg.*;

class RunningPair implements Comparable<RunningPair>{
	private long TTF;
	private Vertex vertex;
	
	RunningPair(long ttf, Vertex v) {
		vertex=v;
		TTF=ttf;
	}
	
	public void setTTF(int ttf){
		TTF=ttf;
	}
	
	public void decTTF(long d){
		TTF-=d;
	}
	
	public long getTTF(){
		return TTF;
	}
	public Vertex getVertex(){
		return vertex;
	}
	
	@Override
	public int compareTo(RunningPair o) {
        if (this.TTF > o.TTF)
        	return  1;
        else if (this.TTF < o.TTF)
        	return -1;
        else if(this.vertex.getOperandsNumber() > o.vertex.getOperandsNumber())	/* tie breaker */
        	return 1;
        else if (this.vertex.getOperandsNumber() < o.vertex.getOperandsNumber())
        	return -1;
        else
        	return 0;
	}

}
