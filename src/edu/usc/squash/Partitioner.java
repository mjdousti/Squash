/*
 *
 * Copyright (C) 2015 Mohammad Javad Dousti, Alireza Shafaei, and Massoud Pedram, SPORT lab,
 * University of Southern California. All rights reserved.
 *
 * Please refer to the LICENSE file for terms of use.
 *
*/


package edu.usc.squash;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;

import edu.usc.squash.dfg.DFG;
import edu.usc.squash.dfg.Vertex;
import edu.usc.squash.dfg.VertexNoCompare;
import edu.usc.squash.schedule.Schedule;
import edu.usc.squash.schedule.Schedule.Scheduling;

public class Partitioner {
	private TreeMap<Integer, TreeSet<Vertex>> asapDirectory=null;
	private DFG dfg;
	
	private String linux = "lnx",
			mac = "mac",
			windows ="win";
	private String metis_dir = Main.getMetisDir();//System.getProperty("user.dir") + "/metis/";
	
	private String metis_name = "gpmetis";
	private Library library;

	private void setExecutablePermission(){
		boolean out = new File(metis_dir+"/"+metis_name).setExecutable(true, false);

		if (!out){
			System.err.println("Failed to set the executable permission for the METIS executable file: "+metis_dir+"/"+metis_name);
			System.exit(-1);
		}
	}
	
	public Partitioner(DFG dfg, Library library) {
		this.dfg=dfg;
		this.library=library;
		
		//Using proper binary version of Metis
		String os_type = System.getProperty("os.name").toLowerCase();
		
		if (os_type.indexOf("win") >= 0){
			metis_dir +=  "/" + windows;
			metis_name += ".exe";
		}else if (os_type.indexOf("nux") >= 0){
			metis_dir += "/" + linux;
			setExecutablePermission();
		}else if (os_type.indexOf("mac") >= 0){
			metis_dir += "/" + mac;
			setExecutablePermission();
		}
		
		try {
			metis_dir = new File(metis_dir).getCanonicalPath();
		} catch (IOException e) {
		}
		
	}



	public int partition(int parts) throws Exception {
		if (parts<2){
			return parts;
		}
		
		//RB is not compatible with vol
		String pType = "kway"; //rb or kway
		String objtype="vol"; //vol or cut
		String inputGraph = "metisIn.mgarph";
		String metisInputGraph = metis_dir+"/"+inputGraph; 

		parts = generateMetisInput(metisInputGraph, parts);

		if (parts==1){
			return parts;
		}
		
		System.out.println("Running partitioner...");
		if (!new File(metis_dir+"/"+metis_name).exists()){
			System.err.println("Squash failed to find Metis in "+metis_dir+"/"+metis_name);
			System.exit(-1);
		}		
		ProcessBuilder pb = new ProcessBuilder(metis_dir+"/"+metis_name, 
				inputGraph, 
				String.valueOf(parts),
				"-ptype="+pType,
				"-objtype="+objtype);
		pb.directory(new File(metis_dir));
		Process process=null;
		try{
			process = pb.start();
		}catch(Exception e){
			e.printStackTrace();
			System.exit(-1);
		}
		final InputStream input = process.getInputStream();
		final InputStream error = process.getErrorStream();
		new StreamCleaner(input).start();
		new StreamCleaner(error).start();

		process.waitFor();

		String metisOutput = metis_dir+"/"+inputGraph+".part." + parts; 
		try{
			parseMetisOutput(metisOutput);
		}catch(FileNotFoundException e){
			System.err.println("Metis failed to generate "+inputGraph+".part." + parts);
			System.exit(-1);
		}
		System.out.println("QODG is partitioned successfully.");
		new File(metisInputGraph).delete();		
		new File(metisOutput).delete();
		return parts;
	}
	private static class StreamCleaner extends Thread {
		private BufferedReader input;


		public StreamCleaner(InputStream input) {
			this.input = new BufferedReader(new InputStreamReader(input));
		}

		@Override
		public void run() {
			try {
				String line = null;
				while ( (line = input.readLine()) != null) {
					if (RuntimeConfig.METIS)
						System.out.println(line);
				}
				input.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void parseMetisOutput(String addr) throws Exception{
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(addr)));
		String partString = br.readLine();
		int i=1;
		while(partString != null) {
			int part = Integer.parseInt(partString);
			if ( i > dfg.getDFG().vertexSet().size()-2)	//Skipping over the duplicated T gates
				break;
			dfg.setPartition(i, part);
			i++;
			partString = br.readLine();
		}
		br.close();
	}

	private int duplicateTGates(){
		SimpleDirectedGraph<Vertex, DefaultEdge> DAG = dfg.getDFG();
		int initialNodeCount=DAG.vertexSet().size() - 1; //Neglecting the End node
		int tNodeNo = 0;

//		new TreeSet<Vertex>(new VertexNoCompare())
		
		TreeSet<Vertex> sortedVertices = new TreeSet<Vertex>(new VertexNoCompare());
		sortedVertices.addAll(DAG.vertexSet());
		for (Vertex v: sortedVertices){
			if (v.getName().compareToIgnoreCase("T")==0){	//duplicate T nodes
				Vertex child = dfg.getChild(v);
				DAG.removeEdge(v, child);

				//Adding the operation in operations list
				Vertex newVertex =new Vertex (library.getNode(v.getName()), initialNodeCount + tNodeNo, v.getOperands(), false);
				tNodeNo++;

				DAG.addVertex(newVertex);
				DAG.addEdge(v, newVertex);
				DAG.addEdge(newVertex, child);

				v.setDelay(5000);
				newVertex.setDelay(5000);			
			}else if (v.getDelay()>4000 && !v.isModule()){
				v.setDelay(5000);
			}
		}

		dfg.getEndVertex().setOperationsNo(DAG.vertexSet().size());
		return tNodeNo;
	}

	private void restoreDuplicatedTGates(int tNodeNo){
		SimpleDirectedGraph<Vertex, DefaultEdge> DAG = dfg.getDFG();

		int indexOfLastNodeToKeep=DAG.vertexSet().size() - tNodeNo-1;	//-1 is added since the End vertex has the highest node #
		//Restoring the DAG
		for (Iterator<Vertex> iterator = new HashSet<Vertex>(DAG.vertexSet()).iterator(); iterator.hasNext();) {
			Vertex v = iterator.next();

			if (v.getOperationNo()>=indexOfLastNodeToKeep){	//a duplicated T nodes
				if (v.isSentinel())	//Skpipping over the end node
					continue;
				if (v.getName().compareToIgnoreCase("T")!=0){//assertion
					System.err.println("Wrong restoration of DAG."+" Encountered "+v+".");
					System.exit(-1);
				}
				Vertex child = dfg.getChild(v);
				Vertex parent = dfg.getParent(v);

				DAG.removeVertex(v);
				DAG.addEdge(parent, child);
			}else if (v.getDelay()>4000 && !v.isModule()){
				v.setDelay(library.getNode(v.getName()).getDelay());
			}
		}

		dfg.getEndVertex().setOperationsNo(DAG.vertexSet().size());
	}

	private void levelize(){
		asapDirectory = new TreeMap<Integer, TreeSet<Vertex>>();
		SimpleDirectedGraph<Vertex, DefaultEdge> DAG = dfg.getDFG();

		Schedule.BasicScheduling(Scheduling.ASAP, DAG);

		for (Vertex v: new HashSet<Vertex>(DAG.vertexSet())){
			if (asapDirectory.containsKey((int)v.getASAPLevel())){
				asapDirectory.get((int)v.getASAPLevel()).add(v);
			}else{
				TreeSet<Vertex> hs = new TreeSet<Vertex>(new VertexNoCompare());
				hs.add(v);
				asapDirectory.put((int) v.getASAPLevel(), hs);
			}
		}
		//Getting rid of ASAP level -1 which belongs to the sentinel nodes (start & end)
		asapDirectory.remove((int)(-1));

		if (RuntimeConfig.DEBUG){
			for (Entry<Integer, TreeSet<Vertex>> e: asapDirectory.entrySet()){
				System.out.print("ASAP Level "+e.getKey()+":\t");
				for (Iterator<Vertex> iterator2 = e.getValue().iterator(); iterator2.hasNext();) {
					System.out.print(iterator2.next()+" ");
				}
				System.out.println();
			}
		}
	}

	private int calcNCon(int parts){
		int ncon=0;
		for (Entry<Integer, TreeSet<Vertex>> e : asapDirectory.entrySet()){
			if (e.getValue().size()>= parts)
				ncon++;
		}
		return ncon;
	}

	private int generateMetisInput(String filename, int parts){
		if (new File(filename).exists())
			new File (filename).delete();
		
		SimpleDirectedGraph<Vertex, DefaultEdge> DAG = dfg.getDFG();
		String fmt = "011";


		//Duplicating T nodes
		int tNodeNo = duplicateTGates();

		levelize();
		int ncon;
		do{
			ncon = calcNCon(parts);
			if (ncon==0){
				if (parts>1){
					parts-=1;
				}
			}
		}while (ncon==0);
		
		if (parts==1){	//no need to do any partitioning
			return parts;
		}
		
		if (RuntimeConfig.DEBUG)
			System.out.println("Ncon:\t"+ncon);

		//System.out.println(ncon);

		int n = DAG.vertexSet().size() - 2;	//removing Start and End nodes
		int m = DAG.edgeSet().size() - DAG.outDegreeOf(dfg.getStartVertex()) - DAG.inDegreeOf(dfg.getEndVertex());

		//graph has no edges -> no partitioning
		//TODO: use trivial partitioning later on
		if (m==0)
			return 1;
		
		try {
			File file = new File(filename);
			BufferedWriter bw=null;
			//Windows is stupid when files are made and deleted over and over again
			//This loop ensures that the file is actually created
			while(bw==null){
				try{
					bw= new BufferedWriter(new FileWriter(file.getAbsolutePath()));
				}catch(FileNotFoundException e){
					
				}
			}
				

			//The first line: n, m, fmt, ncon
			bw.write(n + " " + m + " " + fmt + " " + ncon + System.lineSeparator());
			//bw.write(n+" "+m+System.lineSeparator());
			//

			int index=1;
			for (Entry<Integer, TreeSet<Vertex>> e: asapDirectory.entrySet()){
				for (Vertex vertex : e.getValue()){
					if (e.getValue().size()>= parts){
						vertex.setWeightVectorIndex(index);
					}else
						vertex.setWeightVectorIndex(0);
				}
				if (e.getValue().size()>= parts){
					index++;
				}				
			}
			ArrayList<Vertex> sortedNodes = new ArrayList<Vertex>(DAG.vertexSet());
			Collections.sort(sortedNodes, new VertexNoCompare());

			int indexOfLastNodeToKeep=DAG.vertexSet().size() - tNodeNo-1;
			for (Vertex vertex : sortedNodes){
				if (vertex.isSentinel())
					continue;

				int weightVector = vertex.getWeightVectorIndex();
				for (int i = 1; i <= ncon; i++) {
					if (i==weightVector)
						bw.write(1+" ");
					else
						bw.write(0+" ");
				}

				//parent nodes
				ArrayList<Vertex> sortedParents = new ArrayList<Vertex>();
				for (DefaultEdge de:DAG.incomingEdgesOf(vertex)){
					Vertex v2 = DAG.getEdgeSource(de);

					if (v2.isSentinel())
						continue;
					sortedParents.add(v2);
				}
				Collections.sort(sortedParents, new VertexNoCompare());
				for (Vertex v2: sortedParents){
					if (vertex.getOperationNo()>=indexOfLastNodeToKeep)
						bw.write(v2.getOperationNo()+" "+Integer.MAX_VALUE +" ");
					else
						bw.write(v2.getOperationNo()+" "+1+" ");
				}

				//child nodes
				ArrayList<Vertex> sortedChildren = new ArrayList<Vertex>();
				for (DefaultEdge de:DAG.outgoingEdgesOf(vertex)){
					Vertex v2 = DAG.getEdgeTarget(de);

					if (v2.isSentinel())
						continue;
					
					sortedChildren.add(v2);
				}
				Collections.sort(sortedChildren, new VertexNoCompare());
				for (Vertex v2: sortedChildren){
					if (v2.getOperationNo()>=indexOfLastNodeToKeep)
						bw.write(v2.getOperationNo()+" "+Integer.MAX_VALUE +" ");
					else
						bw.write(v2.getOperationNo()+" "+1+" ");
				}

				bw.write(System.lineSeparator());
			}

			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		restoreDuplicatedTGates(tNodeNo);
		return parts;
	}
	
	public TreeMap<Integer, TreeSet<Vertex>> getASAPDirectory(){
		if (asapDirectory==null){
			levelize();
		}
		return asapDirectory;
	}
}