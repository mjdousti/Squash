/*
 *
 * Copyright (C) 2015 Mohammad Javad Dousti, Alireza Shafaei, and Massoud Pedram, SPORT lab,
 * University of Southern California. All rights reserved.
 *
 * Please refer to the LICENSE file for terms of use.
 *
*/


package edu.usc.squash;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

import javax.xml.parsers.*;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


public class QLib {
	public static Library readLib(String libFile) {
		String QEC_Type;
		int QEC_Length;
		Library library = new Library();
		
		try {
			Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(libFile);
			XPath xPath = XPathFactory.newInstance().newXPath();
			
			NodeList qec_nodes = (NodeList)xPath.evaluate("/library/*", doc.getDocumentElement(), XPathConstants.NODESET); 

			for (int i = 0; i < qec_nodes.getLength(); i++) {
				Element qec_element = (Element) qec_nodes.item(i);
				
				if (qec_element.getNodeName().equalsIgnoreCase("QEC")){
					QEC_Type = qec_element.getAttribute("name");
					QEC_Length = Integer.parseInt(qec_element.getAttribute("length"));
					library.setCodeLength(QEC_Type, QEC_Length);
					
					NodeList gate_nodes = qec_element.getElementsByTagName("gate"); 
					
					for (int j = 0; j < gate_nodes.getLength(); j++) {
						Element gate_element = (Element) gate_nodes.item(j);
						String gateName = gate_element.getAttribute("name");
						int delay = Integer.parseInt(gate_element.getElementsByTagName("delay").item(0).getTextContent());
						int data_qubit_no = Integer.parseInt(gate_element.getElementsByTagName("data_qubit").item(0).getTextContent());
						int ancilla_qubit_no = Integer.parseInt(gate_element.getElementsByTagName("ancilla_qubit").item(0).getTextContent());
						
						Node gate = new Node(gateName, data_qubit_no, ancilla_qubit_no, delay);
						
						library.addNode(QEC_Type, gate);
					}
				}
			}
			
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (RuntimeConfig.VERBOSE){
			System.out.println("Library is parsed successfully.");
		}
		
		return library;
	}
}
