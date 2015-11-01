/*
 *
 * Copyright (C) 2015 Mohammad Javad Dousti, Alireza Shafaei, and Massoud Pedram, SPORT lab,
 * University of Southern California. All rights reserved.
 *
 * Please refer to the LICENSE file for terms of use.
 *
*/


package edu.usc.squash.schedule;

import edu.usc.squash.dfg.Vertex;

public class QubitInfo {
	int x, y, coreNo;
	Vertex origin;

	
	public QubitInfo(int coreNo, int x, int y, Vertex origin) {
		this.x = x;
		this.y = y;
		this.coreNo = coreNo;		
		this.origin = origin;
	}
	
	
	public int getCoreNo(){
		return coreNo;
	}
	public int getX(){
		return x;
	}
	public int getY(){
		return y;
	}
	public Vertex getOrigin(){
		return origin;
	}
	
	
}
