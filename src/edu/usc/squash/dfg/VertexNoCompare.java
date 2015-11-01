/*
 *
 * Copyright (C) 2015 Mohammad Javad Dousti, Alireza Shafaei, and Massoud Pedram, SPORT lab,
 * University of Southern California. All rights reserved.
 *
 * Please refer to the LICENSE file for terms of use.
 *
*/


package edu.usc.squash.dfg;

import java.util.Comparator;

public class VertexNoCompare implements Comparator<Vertex>{

	@Override
	public int compare(Vertex o1, Vertex o2) {
		if (o1.getOperationNo() < o2.getOperationNo())
			return -1;
		else if (o1.getOperationNo() > o2.getOperationNo())
			return 1;
		else
			return 0;
	}


}
