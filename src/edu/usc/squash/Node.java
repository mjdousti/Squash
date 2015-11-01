/*
 *
 * Copyright (C) 2015 Mohammad Javad Dousti, Alireza Shafaei, and Massoud Pedram, SPORT lab,
 * University of Southern California. All rights reserved.
 *
 * Please refer to the LICENSE file for terms of use.
 *
*/


package edu.usc.squash;

public class Node {
	private String name;
	int dataQubitNo, ancillaQubitNo;
	private long delay, actualDelay;

	public Node(){
		
	}
	public Node(Node m){
		name=m.getName();
		dataQubitNo=m.getDataQubitNo();
		ancillaQubitNo=m.getAncillaQubitNo();
		actualDelay =delay=m.getDelay();
	}
	public Node(String n, int d, int a, int delay) {
		name=n;
		dataQubitNo=d;
		ancillaQubitNo=a;
		this.delay=delay;
	}
	/* Getters */
	public String getName(){
		return name;
	}
	public int getDataQubitNo(){
		return dataQubitNo;
	}
	public int getAncillaQubitNo(){
		return ancillaQubitNo;
	}
	public long getDelay(){
		return delay;
	}

	/* Setters */
	public void setName(String n){
		name=n;
	}
	public void setData(int n){
		dataQubitNo=n;
	}
	public void setAncilla(int n){
		ancillaQubitNo=n;
	}
	public void setDelay(long n){
		delay=n;
	}
	
	public long getActualDelay(){
		return actualDelay;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("\tGate: "+getName() + System.lineSeparator());
		sb.append("\t\tDelay: "+getDelay()+System.lineSeparator());
		sb.append("\t\tData #: "+getDataQubitNo()+System.lineSeparator());
		sb.append("\t\tAncilla #: "+getAncillaQubitNo()+System.lineSeparator());
		return sb.toString();
	}
}
