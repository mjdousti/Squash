/*
 *
 * Copyright (C) 2015 Mohammad Javad Dousti, Alireza Shafaei, and Massoud Pedram, SPORT lab,
 * University of Southern California. All rights reserved.
 *
 * Please refer to the LICENSE file for terms of use.
 *
*/


package edu.usc.squash;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;

import edu.usc.squash.dfg.CalledModule;
import edu.usc.squash.dfg.DFG;
import edu.usc.squash.dfg.Module;
import edu.usc.squash.dfg.Operand;
import edu.usc.squash.dfg.Vertex;
import edu.usc.squash.parser.hfq.HFQParser;
import edu.usc.squash.schedule.Schedule;


public class Main {
	/* Squash input parameters */
	private static int k=6;
	private static int physicalAncillaBudget=k*100;
	private static int beta_pmd = 10;
	private static int alpha_int = 3;
	private static double gamma_memory = 0.2;
	private static String currentECC = "Steane";
	//private static final String currentECC = "Bacon-Shor";		

	private static String metisDir = "./metis";
	private static int gurobiTimeLimit = 120;

	private static String libraryPath = null;
	private static String hfqPath = null;

	private static int B_P=0;
	
	public static void main(String[] args) {
		Stack<Module> modulesStack;
		Module module;

		if (parseInputs(args)==false){
			System.exit(-1);	//The input files do not exist
		}

		String separator = "----------------------------------------------";
		System.out.println("Squash v2.0");
		System.out.println(separator);		
		long start = System.currentTimeMillis();

		// Parsing the input library
		Library library = QLib.readLib(libraryPath);
		library.setCurrentECC(currentECC);

		HashMap<String, Module> modules = parseQASMHF(library);
		Module mainModule = modules.get("main");
		
		//Finding max{A_L_i}
		int childModulesLogicalAncillaReq;
		int moduleAncillaReq;		
		
		modulesStack = new Stack<Module>();

		modulesStack.add(mainModule);
		while (!modulesStack.isEmpty()){
			module = modulesStack.peek();
			if (!module.isVisited() && module.isChildrenVisited()){
				//Finding the maximum 
				childModulesLogicalAncillaReq=0;
				for (Module child : module.getDFG().getModules()) {
					childModulesLogicalAncillaReq = Math.max(childModulesLogicalAncillaReq, child.getAncillaReq());
				}
				moduleAncillaReq = module.getAncillaQubitNo() + childModulesLogicalAncillaReq;
				module.setAncillaReq(moduleAncillaReq);
//				System.out.println("Module "+module.getName()+" requires "+moduleAncillaReq+" ancilla.");
				
				modulesStack.pop();
				module.setVisited();
			}else if(module.isVisited()){
				modulesStack.pop();
			}else if (!module.isChildrenVisited()){
				modulesStack.addAll(module.getDFG().getModules());
				module.setChildrenVisited();
			}
		}
		
		int totalLogicalAncilla=mainModule.getAncillaReq();

		System.out.println("A_L_i_max: "+totalLogicalAncilla);
		
		
		final int Q_L = mainModule.getDataQubitNo();

		/* 
		 * In order traversal of modules
		 */
		//Making sure all of the modules are unvisited
		for (Module m: modules.values()) {
			m.setUnvisited();
		}		
		modulesStack = new Stack<Module>();

		modulesStack.add(mainModule);
		while (!modulesStack.isEmpty()){
			module = modulesStack.peek();
			if (!module.isVisited() && module.isChildrenVisited()){
				System.out.println(separator);

				mapModule(module, k, physicalAncillaBudget, totalLogicalAncilla, Q_L, beta_pmd, alpha_int, gamma_memory, library);

				modulesStack.pop();
				module.setVisited();
			}else if(module.isVisited()){
				modulesStack.pop();
			}else if (!module.isChildrenVisited()){
				modulesStack.addAll(module.getDFG().getModules());
				module.setChildrenVisited();
			}
		}

		System.out.println(separator);
		double runtime=(System.currentTimeMillis() - start)/1000.0;
		System.out.println("B_P: "+B_P);
		System.out.println("Total Runtime:\t"+runtime +" sec");	
	}


	public static String getMetisDir(){
		return metisDir;
	}
	
	public static int getGurobiTimeLimit(){
		return gurobiTimeLimit;
	}

	public static void assertAncillaeRequirement(int k, Module module, int physicalAncillaBudget, int logicalAncillaBudget){
		SimpleDirectedGraph<Vertex, DefaultEdge> QODG = module.getDFG().getDFG();
		int physMinAncilla=0;

		for (Vertex v: QODG.vertexSet()){
			if (v.isModule())
				continue;
			if (v.getAncillaQubitNo()>physMinAncilla)
				physMinAncilla=	v.getAncillaQubitNo();		
		}
		//Checking to see if the allocated number of ancillae were sufficient
		if (physMinAncilla > (physicalAncillaBudget/k)){
			System.err.println("Not enough physical ancilla is allocated! At least "+physMinAncilla+" ancilla per core are neaded!");
			System.exit(-1);
		}

//		int minLogicalAncillaReq = 0;
//
//		for (Module m : module.getDFG().getModules()) {
//			if (m.getAncillaQubitNo() > minLogicalAncillaReq)
//				minLogicalAncillaReq = m.getAncillaQubitNo(); 
//		}
//
//		if (module.getAncillaQubitNo() + minLogicalAncillaReq > logicalAncillaBudget){
//			System.out.flush();
//			System.err.flush();
//			System.err.println("Not enough logical ancilla is allocated! At least "+ (module.getAncillaQubitNo() + minLogicalAncillaReq)+
//					" ancilla are neaded for module "+module.getName()+" but only "+logicalAncillaBudget+" provided.");
//			System.exit(-1);
//		}
	}

	private static void print_usage(Options options){
		// automatically generate the help statement
		HelpFormatter formatter = new HelpFormatter();
		formatter.setWidth(90);
		formatter.printHelp( "squash", "Squash v2: A hierarchical scalable considering ancilla sharing", options,"", true);
	}

	private static boolean parseInputs(String [] args){
		Options options=new Options();

		options.addOption("k", "cores", true, "Total number of cores (Default: "+k+")");
		options.getOption("k").setArgName("number");

		options.addOption("p", "physical", true, "Physical ancilla budget (Default: "+physicalAncillaBudget+")");
		options.getOption("p").setArgName("number");

		options.addOption("b", "beta_pmd", true, "Beta_PMD (Default: "+beta_pmd+")");
		options.getOption("b").setArgName("number");

		options.addOption("a", "alpha_int", true, "Alpha_int (Default: "+alpha_int+")");
		options.getOption("a").setArgName("number");

		options.addOption("g", "gamma_mem", true, "Gamma_memory (Default: "+gamma_memory+")");
		options.getOption("g").setArgName("number");

		options.addOption("e", "ecc", true, "Error correcting code (Default: "+currentECC+")");
		options.getOption("e").setArgName("type");

		options.addOption("q", "hf-qasm", true, "HF-QASM input file");
		options.getOption("q").setArgName("file");

		options.addOption("l", "lib", true, "Library file");
		options.getOption("l").setArgName("file");

		options.addOption("m", "metis", true, "Metis directory (Default: "+metisDir+")");
		options.getOption("m").setArgName("path");
		
		options.addOption("t", "timeout", true, "Gurobi timelimit for binding (Default: "+gurobiTimeLimit+"s)");
		options.getOption("t").setArgName("number");

		options.addOption("h", "help", false, "Shows this help menu");

		CommandLineParser parser=new GnuParser();
		CommandLine cmd=null;
		try {
			cmd = parser.parse(options, args);
		} catch (ParseException e){
			System.err.println(e.getMessage());
			System.exit(-1);
		}

		if (!cmd.hasOption("lib")||!cmd.hasOption("hf-qasm")||cmd.hasOption("help")){
			//TODO: remove this
			if (libraryPath==null || hfqPath==null || cmd.hasOption("help")){
				print_usage(options);
				return false;
			}
		}

		if (cmd.hasOption("lib")){
			libraryPath = cmd.getOptionValue("lib");
			if (!new File(libraryPath).exists()){
				System.err.println("Library file "+hfqPath+" does not exist.");
				return false; 
			}
		}

		if (cmd.hasOption("hf-qasm")){
			hfqPath=cmd.getOptionValue("hf-qasm");
			if (!new File(hfqPath).exists()){
				System.err.println("HF-QASM file "+hfqPath+" does not exist.");
				return false; 
			}
		}

		if (cmd.hasOption("cores")){
			k = Integer.parseInt(cmd.getOptionValue("cores"));
		}
		
		if (cmd.hasOption("timelimit")){
			gurobiTimeLimit = Integer.parseInt(cmd.getOptionValue("timelimit"));
		}

		if (cmd.hasOption("physical")){
			physicalAncillaBudget = Integer.parseInt(cmd.getOptionValue("physical"));
		}

		if (cmd.hasOption("beta_pmd")){
			beta_pmd = Integer.parseInt(cmd.getOptionValue("beta_pmd"));
		}

		if (cmd.hasOption("alpha_int")){
			alpha_int = Integer.parseInt(cmd.getOptionValue("alpha_int"));
		}

		if (cmd.hasOption("gamma_memory")){
			gamma_memory = Integer.parseInt(cmd.getOptionValue("gamma_memory"));
		}
		if (cmd.hasOption("ecc")){
			currentECC = cmd.getOptionValue("ecc");
		}

		if (cmd.hasOption("metis")){
			metisDir = cmd.getOptionValue("metis");
		}

		return true;
	}



	public static void mapModule(Module module, int k, int b_qrcr, int logicalAncillaBudget, final int Q_L, 
			int beta_pmd, int alpha_int, double gamma_memory, Library library){
		DFG dfg = module.getDFG();
		System.out.println("Mapping module "+module.getName()+"...");

		//Checking if the physical and logical budget is enough
		assertAncillaeRequirement(k, module, b_qrcr, logicalAncillaBudget);

		/*
		 * Partitioning and picking the highest possible part no. 
		 * which is smaller than k
		 */
		Partitioner partitioner = new Partitioner(dfg, library);
		try {
			//			partitioner.partition(k);
			int parts = partitioner.partition(k);
			System.out.println("Partition count: "+parts);
		} catch (Exception e) {
			e.printStackTrace();
		}

		for (Module m : module.getDFG().getModules()) {
			if (m.getDFG().getCoreUsage().size() > k)
				k=m.getDFG().getCoreUsage().size(); 
		}

		if (k==1){
			module.getDFG().useSingleCore();
		}

		System.out.println("K is selected as "+k+".");

		/*
		 * Building and characterizing a ReQup Architecture
		 */

		ReQuP requp = new ReQuP(module, k, b_qrcr, logicalAncillaBudget, Q_L, beta_pmd, alpha_int, 
				gamma_memory, dfg.getDFG(), library, partitioner.getASAPDirectory());

		module.setRequp(requp);
		B_P = Math.max(B_P, requp.getTotalUsedPhsicalAncilla());


		/*
		 * Binding
		 */
		if (k>1){	//otherwise no binding is required.
			Binder.bind(k, requp.getInterCoreRoutingDelay(), dfg.getDFG(), requp);
		}

		Schedule.listScheduling(module, k, b_qrcr, logicalAncillaBudget, requp.getInterCoreRoutingDelay(), dfg);

		System.out.println("Latency:\t"+module.getDelay()+" us");
	}

	private static HashMap<String, Module> parseQASMHF(Library library){
		HFQParser hfqParser = null;
		/*
		 * Pass 1: Getting module info
		 */
		try {
			hfqParser = new HFQParser(new FileInputStream(hfqPath));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		HashMap<String, Module> moduleMap=hfqParser.parse(library, true, null);

		/* 
		 * In order traversal of modules
		 */
		
		ArrayList<CalledModule> modulesList = new ArrayList<CalledModule>();
		modulesList.add(new CalledModule(moduleMap.get("main"), new ArrayList<String>()));
		while (!modulesList.isEmpty()){
			Module module = modulesList.get(0).getModule();
			
			if (!module.isVisited()){
				module.setVisited();

				ArrayList<CalledModule> calledModules=module.getChildModules();
				modulesList.addAll(calledModules);
				for (CalledModule calledModule: calledModules){
					Module childModule = calledModule.getModule();
					for (int i=0; i<calledModule.getOps().size(); i++){
						Operand operand = childModule.getOperand(i);
						if (operand.isArray() && operand.getLength()==-1){
							operand.setLength(module.getOperandLength(calledModule.getOps().get(i)));							
						}
					}
				}
			}
			modulesList.remove(0);
		}


		/*
		 * Pass 2: Making hierarchical QMDG
		 */
		try {
			hfqParser = new HFQParser(new FileInputStream(hfqPath));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		HashMap<String, Module> modules = hfqParser.parse(library, false, moduleMap);

		return modules;
	}


}
