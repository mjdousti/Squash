# Squash

Squash v2 divides a given quantum circuit into a number of quantum modules--each module is divided into k parts such that each part will run on one of k available cores. Then it maps the modules to a multi-core reconfigurable quantum processor architecture, called Requp, which supports a hierarchical approach to mapping a quantum algorithm and ancilla sharing. Each core is capable of performing any quantum instruction.

## Change Log
```
Version |    Change
-------------------------------------------------------------------------------
1.00    |    Initial release.
```

## License
Please refer to the [LICENSE](LICENSE) file.

## Directories & Files Structure
```
Squash_v2
|-- metis -> METIS binary files for Windows, Mac, and Linux
|-- sample_inputs
    |-- benchmark
        |-- QASM -> Benchmarks in QASM format for baseline comparisons
        `-- HFQ -> Benchmarks in hierarchical fault-tolerant QASM
    |-- library
        `-- library.xml -> QEC library describing Steane and Bacon-Shor resource requirements in Ion-Trap fabric 
|-- src
    |-- edu -> Java source code directory
    |-- libs
        |-- commons-cli-1.2.jar -> Appache Commons CLI library
        |-- commons-lang3-3.3.2 -> Apache Commons Lang library
        |-- gurobi.jar -> Gurobi 6.0.5 Java interface
        |-- jar-in-jar-loader.zip -> Jar loader file taken from Eclipse.
        |-- javacc.jar -> Java Compiler Compiler (JavaCC)
        `-- jgrapht-core-0.9.0 -> JGraphT library
|-- README -> This readme file
|-- LICENSE -> License file
`-- build.xml -> Ant build file
```

## Requirements
1. [Ant 1.7](http://ant.apache.org)
2. [Oracle Java 7-JDK](http://www.oracle.com/technetwork/java/javase/downloads/index.html) or higher
3. [Gurobi Optimizer 6.0.5](http://www.gurobi.com) (free for academic use)

**Note:**
* If you intend to use any version of Gurobi other than 6.0.5, you must replace src/libs/gurobi.jar with the one provided in the version you have (located in the "lib" directory) and recompile the project.
* Squash v2 can be run on MacOS X, Windows, or Linux. The only restriction is that 64-bit versions of these OSes are supported.
    
## Preinstall
Make sure that all the requirements are already installed. The following environmental variables should be set before the installation/running of the program.
* `JAVA_HOME` should point where `java` and `javac` binary files are located.
* `GUROBI_HOME` and `GRB_LICENSE_FILE` should point to the appropriate location. Please refer to the installation readme of Gurobi. `PATH` and `LD_LIBRARY_PATH` should also be updated accordingly.

## Compile
An `ant` script takes care of the build process. You may enter the following commands to build and clean the project, respectively:
```
$ ant        # Makes Squash.jar
$ ant clean  # Cleans the project
```
Again, note that if you intend to use any version of Gurobi other than 6.0.5, you must replace `src/libs/gurobi.jar` with the one provided in the version you have (located in the `lib` directory) and recompile the project.

## Run
Run `java -jar Squash.jar` to perform the mapping. The options of this command are listed below:
```
usage: squash [-a <number>] [-b <number>] [-e <type>] [-g <number>] [-h] [-k <number>] [-l
       <file>] [-m <path>] [-p <number>] [-q <file>] [-t <number>]
Squash v2: A hierarchical scalable considering ancilla sharing
 -a,--alpha_int <number>   Alpha_int (Default: 3)
 -b,--beta_pmd <number>    Beta_PMD (Default: 10)
 -e,--ecc <type>           Error correcting code (Default: Steane)
 -g,--gamma_mem <number>   Gamma_memory (Default: 0.2)
 -h,--help                 Shows this help menu
 -k,--cores <number>       Total number of cores (Default: 6)
 -l,--lib <file>           Library file
 -m,--metis <path>         Metis directory (Default: ./metis)
 -p,--physical <number>    Physical ancilla budget (Default: 600)
 -q,--hf-qasm <file>       HF-QASM input file
 -t,--timeout <number>     Gurobi timelimit for binding (Default: 120s)
```

### Example
Getting the physical resource estimation for the FT-H gate, Ion Trap PMD, and [[7,1,3]] Steane code:
```
$ java -Xss100m -jar Squash.jar -q sample_inputs/benchmark/HFQ/3M_Binary_Welded_Tree_s5.hfq \
		-l sample_inputs/library/library.xml -t 60 -k 5  -p  500
```

**Note:** Make sure to use -Xss100m in order to allocate more stack to Squash.

### Sample Outputs
```
Squash v2.0
----------------------------------------------
Library is parsed successfully.
HF-QASM is parsed successfully (1st pass).
HF-QASM is parsed successfully (2nd pass).
A_L_i_max: 97
----------------------------------------------
Mapping module Oracle...
Running partitioner...
QODG is partitioned successfully.
Partition count: 5
K is selected as 5.
ReQuP model is generated successfully.
Partitions are bound successfully.
QODG is Scheduled successfully.
Latency:    84210090 us
----------------------------------------------
Mapping module TimeStep...
Running partitioner...
QODG is partitioned successfully.
Partition count: 5
K is selected as 5.
ReQuP model is generated successfully.
Partitions are bound successfully.
QODG is Scheduled successfully.
Latency:    17551500 us
----------------------------------------------
Mapping module main...
Running partitioner...
QODG is partitioned successfully.
Partition count: 5
K is selected as 5.
ReQuP model is generated successfully.
Partitions are bound successfully.
QODG is Scheduled successfully.
Latency:    3719439031 us
----------------------------------------------
B_P: 710
Total Runtime:    4.931 sec
```

### Benchmarking Results
The provided tool is fully tested on a laptop machine with the following specification:
* OS: MacOS X Yosemite 10.10.5
* CPU: 1.7 GHz Intel Core i7
* Memory: 8 GB 1600 MHz DDR3

Example runtime result:  less than 5 seconds

**Note:** We have tested Squash in Debian Linux and Windows 7 and it worked flawlessly.

## Developers
* [Mohammad Javad Dousti](<dousti@usc.edu>)
* [Alireza Shafaei](<shafaeb@usc.edu>)
* [Massoud Pedram](<pedram@usc.edu>)

## Questions or Bugs?
You may contact [Mohammad Javad Dousti](<dousti@usc.edu>) for any questions you may have or bugs that you find.
