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
import java.util.HashMap;
import java.util.Iterator;
import java.util.PriorityQueue;

import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;

import edu.usc.squash.Resource;
import edu.usc.squash.RuntimeConfig;
import edu.usc.squash.dfg.DFG;
import edu.usc.squash.dfg.Module;
import edu.usc.squash.dfg.Vertex;

public class Schedule {
	public enum Scheduling {ASAP, ALAP};
	
	public static void listScheduling(Module module, int k, int physicalAncillaBudget, int logicalAncillaBudget, int [][]getInterCoreRoutingDelay, DFG dfg){
		long c_step;
		
		Resource remainingResource = new Resource(k, physicalAncillaBudget, logicalAncillaBudget);
		//Reducing ancilla defined at the top of a module
		remainingResource.useLogicalAncilla(module.getAncillaQubitNo());
		
	
		SimpleDirectedGraph<Vertex, DefaultEdge> QODG=dfg.getDFG();
		ArrayList<TimeSlot> result=new ArrayList<TimeSlot>();
		PriorityQueue<Vertex> readyQueue=new PriorityQueue<Vertex>();		
		PriorityQueue<RunningPair> ttfQueue=new PriorityQueue<RunningPair>();

		//Priority Calculation
		priorityCalculation(QODG);

		//Resetting the ready inputs to zero for all the vertices
		for (Vertex cur0: QODG.vertexSet()){
			//The input comes from the Start node is ready
			if (QODG.containsEdge(dfg.getStartVertex(), cur0))
				cur0.setReadyInpts(1);
			else
				cur0.setReadyInpts(0);
		}

		//Initialization of the ready queue
		Vertex vertexTemp, vertexTemp2;
		for (DefaultEdge edge: QODG.outgoingEdgesOf(dfg.getStartVertex())){
			vertexTemp = QODG.getEdgeTarget(edge);
			if (QODG.inDegreeOf(vertexTemp)==1){
				preProcessVertex(getInterCoreRoutingDelay, QODG, vertexTemp, 0, module);
				readyQueue.add(vertexTemp);
			}
		}

		//Updating the first scheduling level
		remainingResource=scheduleReadyVertices(module, result, readyQueue, ttfQueue, remainingResource, 0);
		c_step = 0;

		long minTimeToComplete;
		long scheduleTime=0;
		while(!ttfQueue.isEmpty()){
			minTimeToComplete=ttfQueue.peek().getTTF();
			scheduleTime+=minTimeToComplete;

			for (Iterator<RunningPair> iterator = ttfQueue.iterator(); iterator.hasNext();) {
				RunningPair rp=iterator.next();
				//advancing the time by the minimum time frame
				rp.decTTF(minTimeToComplete);
				//Extracting all instructions which are done				
				if (rp.getTTF()==0){
					vertexTemp=rp.getVertex();
					//Increasing the ready inputs by one for all the children of the node
					//and adding the ones which are ready to the ready queue
					for (DefaultEdge edge: QODG.outgoingEdgesOf(vertexTemp)){
						vertexTemp2=QODG.getEdgeTarget(edge);
						vertexTemp2.setReadyInpts(vertexTemp2.getReadyInputs()+1);
						//all inputs are ready
						
						if (vertexTemp2.getReadyInputs()==QODG.inDegreeOf(vertexTemp2)){
							preProcessVertex(getInterCoreRoutingDelay, QODG, vertexTemp2, scheduleTime, module);
							readyQueue.add(vertexTemp2);
						}
					}
					//reclaim the ancilla qubits
					if (vertexTemp.isModule()){
						remainingResource.returnLogicalAncilla(vertexTemp.getAncillaQubitNo());
						remainingResource.returnCore(vertexTemp.getDFG().getCoreUsage(), vertexTemp);
					}else{
						remainingResource.returnPhysicalAncilla(vertexTemp.getCoreNo(), vertexTemp.getAncillaQubitNo());
						remainingResource.returnCore(vertexTemp.getCoreNo(), vertexTemp);
					}
//					System.out.println(scheduleTime+": "+rp.getVertex());
					iterator.remove();
				}
			}
			//Scheduling the ready vertices; Adding a new scheduling level if needed
			remainingResource=scheduleReadyVertices(module, result, readyQueue, ttfQueue, remainingResource, scheduleTime);
			c_step = Math.max(c_step, scheduleTime);
		}

		//removing the last scheduling level which has the "end" node
		result.remove(result.size()-1);

		//reclaiming ancilla defined at the top of a module
		remainingResource.returnLogicalAncilla(module.getAncillaQubitNo());

		
		if (RuntimeConfig.VERBOSE){
			System.out.println("QODG is Scheduled successfully.");
		}

		module.setDelay(c_step);
		module.setAncilla(remainingResource.getMaxLogicalUsage());
	}
	
	public static void BasicScheduling(Scheduling type, SimpleDirectedGraph<Vertex, DefaultEdge> DFG){
		ArrayList<Vertex> readyVertices=new ArrayList<Vertex>();

		//Initialize graph for traversing
		for (Vertex currentVertex :  DFG.vertexSet()){
			if (type == Scheduling.ASAP)
				currentVertex.setASAPLevel(-1);
			else
				currentVertex.setALAPLevel(-1);
			currentVertex.setReadyInpts(0);
			if (currentVertex.isSentinel()){
				if ((type == Scheduling.ASAP && currentVertex.getName().compareToIgnoreCase("start")==0)||
						(type == Scheduling.ALAP && currentVertex.getName().compareToIgnoreCase("end")==0)){
					readyVertices.add(currentVertex);
				}
			}
		}	
		
		if (type==Scheduling.ASAP){
			while(!readyVertices.isEmpty()){
				Vertex currentVertex=readyVertices.remove(0);
			
				for (DefaultEdge edge: DFG.outgoingEdgesOf(currentVertex)){
					Vertex childVertex=DFG.getEdgeTarget(edge);
					if (childVertex.isSentinel())
						continue;
					childVertex.incReadyInpts();
					if (childVertex.getASAPLevel()==-1 && childVertex.getReadyInputs() == DFG.incomingEdgesOf(childVertex).size()){
						childVertex.setASAPLevel(currentVertex.getASAPLevel()+1);
						readyVertices.add(childVertex);
					}				
				}
			}
		}else{	//ALAP
			Vertex parentVertex;
			long alapLevels=0;
			while(!readyVertices.isEmpty()){
				Vertex currentVertex=readyVertices.remove(0);
			
				for (DefaultEdge edge:DFG.incomingEdgesOf(currentVertex)){
					parentVertex=DFG.getEdgeSource(edge);
					if (parentVertex.isSentinel())
						continue;
					parentVertex.incReadyInpts();
					if (parentVertex.getALAPLevel()==-1 && parentVertex.getReadyInputs() == DFG.outgoingEdgesOf(parentVertex).size()){
						parentVertex.setALAPLevel(currentVertex.getALAPLevel()+1);
						readyVertices.add(parentVertex);
						alapLevels = Math.max(alapLevels, parentVertex.getALAPLevel());
					}				
				}
			}
			for (Vertex currentVertex : DFG.vertexSet()){
				if (currentVertex.isSentinel())
					continue;
				currentVertex.setALAPLevel(alapLevels - currentVertex.getALAPLevel());				
			}
		}

	}

	private static void priorityCalculation(SimpleDirectedGraph<Vertex, DefaultEdge> QODG){
		Vertex startVertex=null;

		for (Vertex cur0: QODG.vertexSet()){
			cur0.setPath_to_Sink(0);
			//# of inputs = # of outputs
			cur0.setReadyInpts(0);
			if (cur0.getName().compareToIgnoreCase("start")==0){
				startVertex=cur0;
			}
		}
		calcHeight(QODG, startVertex);
	}

	private static long calcHeight(SimpleDirectedGraph<Vertex, DefaultEdge> QMDG, Vertex v){
		if (v.getPath_to_Sink()!=0){
			//Avoid recalculating the height for a node which its priority has already been calculated
			return v.getPath_to_Sink();
		}else{
			long height=0, temp;
			Vertex childVertex;
			for (DefaultEdge edge: QMDG.outgoingEdgesOf(v)){
				childVertex=QMDG.getEdgeTarget(edge);

				if (!childVertex.isSentinel()){//Not the end node
					temp=calcHeight(QMDG, childVertex);
					height=Math.max(temp, height);
//					height=Math.max(temp+1, height);
				}
			}
			height +=v.getDelay();
			
			v.setPath_to_Sink(height);
			return height;
		}
	}

	
	/*
	 * Sets the delay of the vertex to its intrinsic delay + routing delay of its operands
	 * The routing delay may comprises of intra- or inter-core routing delays
	 */
	private static void preProcessVertex(int [][]d, SimpleDirectedGraph<Vertex, DefaultEdge> QODG, Vertex vertex, long scheduleTime, Module module){
//		Vertex parent=null;
		int maxRoutingDelay;
		
		/*
		 * Jumping over the "end" vertex. It is assumed that the qubits stay where they are at the end of a module.
		 * The module/instruction after a module is responsible to route the qubits to proper positions.
		 */

		if (vertex.isSentinel())
			return;
		
		int beta_pmd = module.getRequp().getBetaPMD();

		maxRoutingDelay = 0;
		
		if (vertex.isModule()){
			for(String op: vertex.getQubitsInitLocation().keySet()){
				if (module.getQubitFinalLocation(op)==null){	//no routing delay for the initial location of the qubit
					module.setDataQubitLocation(vertex.getInitLocation(op));
				}else{
					HashMap <String, QubitInfo> map = vertex.getInitLocation(op);
					for (String s: map.keySet()){
						if (module.getQubitFinalLocation(s)==null){	//no routing delay for the initial location of the qubit
							module.setDataQubitLocation(s, map.get(s));
						}else if (module.getQubitFinalLocation(s).getCoreNo() != map.get(s).getCoreNo()){	//inter-core routing delay
							QubitInfo parent = module.getQubitFinalLocation(s);
							
							maxRoutingDelay = (int) Math.max(maxRoutingDelay, 
									Math.max((Math.abs(vertex.getLocX() - parent.getX())+ 
											Math.abs(vertex.getLocY() - parent.getY()))*beta_pmd
											- (scheduleTime - parent.getOrigin().getScheduledTime()-parent.getOrigin().getActualDelay()), 0));
						}else{	//intra-core routing delay
							maxRoutingDelay = Math.max(maxRoutingDelay, d[vertex.getCoreNo()][vertex.getCoreNo()]);
						}
					}
				}
			}
		}else{//vertex is not a module
			for(String op: vertex.getOperands()){
				if (module.getQubitFinalLocation(op)==null){	//no routing delay for the initial location of the qubit
					int x = module.getRequp().getCoreCenterX(vertex.getCoreNo());
					int y = module.getRequp().getCoreCenterY(vertex.getCoreNo());
					module.setDataQubitLocation(op, new QubitInfo(vertex.getCoreNo(), x, y, vertex));
				}else if (module.getQubitFinalLocation(op).getCoreNo() != vertex.getCoreNo()){	//inter-core routing delay
					QubitInfo parent = module.getQubitFinalLocation(op);
	
					maxRoutingDelay = (int) Math.max(maxRoutingDelay, 
							Math.max((Math.abs(vertex.getLocX() - parent.getX())+ 
									Math.abs(vertex.getLocY() - parent.getY()))*beta_pmd
									- (scheduleTime - parent.getOrigin().getScheduledTime()-parent.getOrigin().getActualDelay()), 0));
					
//					System.out.println("Vetex X: "+ vertex.getLocX() + " Y: "+vertex.getLocY() + " core #"+vertex.getCoreNo());
//					System.out.println("Parent X: "+ parent.getX() + " Y: "+parent.getY()+ " core #"+parent.getCoreNo());
//					System.out.println("Residue: "+(scheduleTime - parent.getOrigin().getScheduledTime()-parent.getOrigin().getActualDelay()));
				}else{	//intra-core routing delay
					maxRoutingDelay = Math.max(maxRoutingDelay, d[vertex.getCoreNo()][vertex.getCoreNo()]);
				}				
			}
		}
		
		//Find the last parent executed instruction before the current vertex
		Vertex parent=null;
		long time = 0;
		for (DefaultEdge dwe: QODG.incomingEdgesOf(vertex)){
			 Vertex temp = QODG.getEdgeSource(dwe);
			 if (temp.isSentinel())
				 continue;
			 if (time < temp.getScheduledTime() + temp.getActualDelay()){
				 time = temp.getScheduledTime() + temp.getActualDelay();
				 parent = temp;				 
			 }
		}
		//architecture should be reconfigured
		if (parent!=null && parent.isModule() && !vertex.isModule()){	
			maxRoutingDelay = Math.max(maxRoutingDelay, parent.getRequp().calcTransformTime(module.getRequp(), null, null));
		}else if (parent!=null && !parent.isModule() && vertex.isModule()){
			maxRoutingDelay = Math.max(maxRoutingDelay, module.getRequp().calcTransformTime(vertex.getRequp(), null, vertex.getAncillaInitLocation()));
		}else if (parent!=null && parent.isModule() && vertex.isModule()){
			maxRoutingDelay = Math.max(maxRoutingDelay, parent.getRequp().calcTransformTime(vertex.getRequp(), 
					parent.getAncillaFinalLocation(), vertex.getAncillaInitLocation()));
		}
			
		vertex.setDelay(vertex.getDelay() + maxRoutingDelay);
	}

	

	public static int minRequiredAncilla(SimpleDirectedGraph<Vertex, DefaultEdge> QMDG){
		int minAncilla=0;
		for (Iterator<Vertex> iterator = QMDG.vertexSet().iterator(); iterator.hasNext();) {
			Vertex v= iterator.next();
			if (v.getAncillaQubitNo()>minAncilla)
				minAncilla=	v.getAncillaQubitNo();		
		}
		return minAncilla;
	}

	private static Resource scheduleReadyVertices(Module module, ArrayList<TimeSlot> result, PriorityQueue<Vertex> readyQueue, PriorityQueue<RunningPair> ttfQueue, 
			Resource remainingResource, long scheduleTime){
		Vertex tempVertex;
		boolean addedNewLevel=false;

		for (Iterator<Vertex> iterator = readyQueue.iterator(); iterator.hasNext();) {
			tempVertex = iterator.next();
			if ((!tempVertex.isModule() && remainingResource.getPhysicalAncillaNo(tempVertex.getCoreNo())>=tempVertex.getAncillaQubitNo() 
					&& remainingResource.checkCoreAvailability(tempVertex.getCoreNo(), tempVertex))||
				(tempVertex.isModule() && tempVertex.getAncillaQubitNo() <= remainingResource.getLogicalAncillaNo()
					&& remainingResource.checkCoreAvailability(tempVertex.getDFG().getCoreUsage(), tempVertex))){
				//tempVertex is scheduled
				iterator.remove();
				//deducting the used ancilla
				if (tempVertex.isModule()){
					remainingResource.useLogicalAncilla(tempVertex.getAncillaQubitNo());
					remainingResource.useCore(tempVertex.getDFG().getCoreUsage(), tempVertex);

					module.setDataQubitLocation(tempVertex.getQubitsFinalLocation());
				}else{
					remainingResource.usePhysicalAncilla(tempVertex.getCoreNo(), tempVertex.getAncillaQubitNo());
					remainingResource.useCore(tempVertex.getCoreNo(), tempVertex);
					
					for (String op: tempVertex.getOperands()){
						int x = module.getRequp().getCoreCenterX(tempVertex.getCoreNo());
						int y = module.getRequp().getCoreCenterY(tempVertex.getCoreNo());
						module.setDataQubitLocation(op, new QubitInfo(tempVertex.getCoreNo(), x, y, tempVertex));
					}
				}
				
				ttfQueue.add(new RunningPair(tempVertex.getDelay(), tempVertex));
				tempVertex.setScheduledTime(scheduleTime);
//				System.out.println(scheduleTime+":\t"+tempVertex.getName());
				
				if (addedNewLevel==false){
					result.add(new TimeSlot(scheduleTime));
					addedNewLevel=true;
				}
				result.get(result.size()-1).addVertex(tempVertex);
			}
		}
		return remainingResource;
	}

	
	/*
	 * 
	 * Print functions
	 * 
	 */

	public static void print_ASAP_ALAP(SimpleDirectedGraph<Vertex, DefaultEdge> DFG){
		Vertex currentVertex;
		for (Iterator<Vertex> iterator = DFG.vertexSet().iterator(); iterator.hasNext();) {
			currentVertex = iterator.next();
			if (!currentVertex.isSentinel())
				System.out.println(currentVertex + "\t\tASAP: "+currentVertex.getASAPLevel() + "\t\tALAP: "+currentVertex.getALAPLevel());
		}		
	}

	private static void printPriority(SimpleDirectedGraph<Vertex, DefaultEdge> QMDG){
		Vertex cur0;
		for (Iterator<Vertex> iterator = QMDG.vertexSet().iterator(); iterator.hasNext();) {
			cur0=iterator.next();
			System.out.println(cur0+" priority="+cur0.getPath_to_Sink());
		}		
	}
	
	public static void printScheduling(ArrayList<TimeSlot> result){
		for (int i = 0; i < result.size(); i++) {
			TimeSlot timeSlot = result.get(i);
			System.out.print("(step: "+(i+1)+")\tTime: "+timeSlot.getTime()+" us\t");

			for (Iterator<Vertex> iterator2 = timeSlot.getList().iterator(); iterator2.hasNext();) {
				System.out.print(iterator2.next()+"\t");
			}
			System.out.println();
		}
	}

}
