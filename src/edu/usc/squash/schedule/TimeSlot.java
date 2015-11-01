/*
 *
 * Copyright (C) 2015 Mohammad Javad Dousti, Alireza Shafaei, and Massoud Pedram, SPORT lab,
 * University of Southern California. All rights reserved.
 *
 * Please refer to the LICENSE file for terms of use.
 *
*/


package edu.usc.squash.schedule;

import java.util.ArrayList;

import edu.usc.squash.dfg.Vertex;

public class TimeSlot {
	private final long time;
	private ArrayList<Vertex> list=new ArrayList<Vertex>();

	
	public TimeSlot(long t) {
		time=t;
	}
	public void addVertex(Vertex v){
		list.add(v);
	}
	
	public long getTime(){
		return time;
	}
	
	public ArrayList<Vertex> getList(){
		return list;
	}
}
