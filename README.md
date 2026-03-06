# README #
# HyperSpark: Edge Computing & NRP Optimization Framework 🚀

![CI Status](https://github.com/saccolucax2/HyperSpark-Revived/actions/workflows/docker-build.yml/badge.svg)
*(Based on the Bachelor's Thesis in Computer Engineering: "Metaheuristic Optimization for the Next Release Problem in Heterogeneous Edge Computing Environments" by Luca Sacco)*

> **⚠️ PROJECT STATUS: EXTENDED FOR EDGE BENCHMARKING & NRP**
> This repository represents a major architectural evolution of the original "HyperSpark" framework. 
> Originally designed for Cloud environments, the codebase has been completely refactored, containerized, and hardened to evaluate **Distributed Metaheuristics (Simulated Annealing) on heterogeneous, resource-constrained Edge Computing clusters** solving the **Next Release Problem (NRP)**.

**HyperSpark NRP** is a distributed framework written in **Scala 2.11** and managed by **Apache Spark 2.4**. It simulates and benchmarks the performance of severely constrained hardware executing NP-Hard optimization algorithms under soft real-time constraints (60 seconds).

## 🔬 Scientific Benchmark Setup (3-Tier Architecture)

This framework was specifically engineered to deploy MapReduce tasks across a simulated multi-tier Edge architecture using Docker and WSL2 resource limits:
* **Tier 1 (Gateway):** Simulated NVIDIA Jetson Nano (High CPU/RAM)
* **Tier 2 (Mid-End):** Simulated Raspberry Pi 4
* **Tier 3 (Low-End):** Simulated Raspberry Pi 3

### Key Engineering Contributions:
* **Resolution of the Netty Deadlock Bug:** Refactored the Spark deployment configuration (`numOfAlgorithms >= 2`) to prevent Network Thread Starvation and timeout disconnects on low-power devices.
* **The "Dynamic Escape Valve":** Modified the Simulated Annealing source code to include a dynamic breaking mechanism (`maxAttempts`), preventing *Constraint Starvation* and *Infinite Polling Traps* on hyper-constrained NRP instances.
* **Hardware-Aware Hyperparameter Tuning:** Dynamically maps instance complexity (Topological Depth) to algorithmic constraints (Constraint Tightness Normalization) to guarantee Fault Tolerance.

## ⚙️ The Automated DataOps Pipeline

The project abandons manual testing in favor of a fully automated, multi-language DataOps pipeline designed for rigorous scientific validation:

1. **Orchestration (PowerShell):** Manages Docker Compose lifecycle, enforces resource limits, and implements a *Watchdog Timer* for safety.
2. **Execution (Scala/Spark):** Runs the distributed metaheuristic across the Edge cluster.
3. **High-Speed ETL (Rust):** A custom bare-metal parser written in Rust safely extracts latencies and topological metrics from gigabytes of raw logs without triggering Host Out-Of-Memory (OOM) exceptions.
4. **Statistical Analysis (Python):** Automatically generates Boxplots, Scatter plots (Pareto frontiers), and complete ANOVA statistical reports to mathematically prove the *Hardware Inverse Scalability* and the *Fitness Paradox*.

## ⚡️ Quick Start (Automated Benchmark)

### 1. Prerequisites
* Docker Desktop & WSL2 (for Windows users)
* Python 3.x (with `pandas`, `seaborn`, `scipy`, `matplotlib`)
* Rust & Cargo (for the high-speed log extractor)

### 2. Run the "Golden Run"
Execute the master script to start the benchmark across all NRP instances (NRP1 to NRP5), automatically scaling the budget from 90% down to 30%:
```powershell
.\run_full_benchmark.ps1
```
What the script does: It tears down old networks, drops OS RAM caches, deploys the 5-node cluster, runs the Spark jobs, rotates the logs into `data/logs/archive/`, triggers the Rust parser, and finally executes the Python plotting engine.

### 3. Generated Artifacts
After the run, check the `data/graphs/` directory for:

* `anova_statistical_report.csv`: Complete P-Value significance validation.
* `boxplot_time_*.png` & `boxplot_fitness_*.png`: Execution metrics.
* `scatter_tradeoff_*.png`: The Time vs. Fitness Pareto evaluation.

## 🔐 Fixed Dependencies (Gold Standard)
During the restoration process, the following test dependencies were frozen to ensure compatibility with Scala 2.11:
- scalatest: *2.2.6*
- scalacheck: *1.12.5*
- junit: *4.12*

---
# 📖 Original Documentation

### What is this repository for? ###

HyperSpark is a framework for running meta-heuristic algorithms on a cluster of commodity computers. The project is written in Scala, and the cluster is managed by Spark. 

The current version of the project implements the algorithms for Permutation Flowshop problems (PFSP), but it could be easily extended for any kind of problem. 

The general idea is to use the "Big Calculations" paradigm for running algorithms written by the user. What does that mean? It means that we perform some heavy computation algorithms on a cluster of machines, and we strive to optimize the usage of resources and time.

### Goals of the project ###

- Examine the performance of popular algorithms for PFSP when run in Scala/Spark environment
- Show that the performance of one parallel run is more efficient than the performance of a single algorithm run
- Support the execution of both light-weight and CPU-intensive parallel algorithms.
- Show that reusing the best solution found during a single parallel run for next runs is more efficient than just taking the best solution out of N independent runs.
- Show that Scala/Spark environment suits better for implementation of cooperative algorithms than Hadoop's environment.
- Simplicity: Show that the use of Scala/Spark environment is much simpler than using Hadoop environment, and that there is almost no setup of cluster using our framework.

### How to use the framework? ###

- The user writes its Problem, Solution, EvaluatedSolution, and Algorithm classes. 
- Optionally, the user implements a StoppingCondition or uses an existing one "TimeExpired".
- Optionally, the user implements a MapReduceHandler or uses the default one (already set in FrameworkConf class).
- The user specifies how many algorithms will be run in parallel to evaluate the problem. 
- The user specifies a stopping condition for parallel runs. (S)he uses the implemented TimeExpired stopping condition, or (s)he writes a custom stopping condition class.
- What a user could do is to define how the seeding solution (if exists) is provided to each of the algorithms that are run in parallel and how the results of the algorithms are combined (min, max, sum,etc.) and whether if the final result will be reused for another desired parallel run.

### Implementation guide ###

- Extend the "Problem" class (or use the existing one for PFSP, PFSProblem). 
- Extend the "Solution" and "EvaluatedSolution" class (or use the existing one for PFSP, PFSSolution, and PFSEvaluatedSolution).
- Implement ```evaluate(s: Solution)``` inside Problem class.
- Extend the "Algorithm" trait and implement evaluate function signatures inside your custom algorithm class.
- Extend the "StoppingCondition" and implement ```isSatisfied()``` function. Optionally, use the already implemented "TimeExpired" stopping condition class.
- Write your application that uses the HyperSpark framework. To start, create a Scala object with the main function. Inside the main write a FrameworkConf and provide it a problem, an algorithm, a parallelism multiplier, seeding solutions, and the stopping condition for one parallel run. See *it.polimi.hyperh.apps.LocalApp.scala* example.
- Use ```Framework.run(conf: FrameworkConf)``` or ```Framework.multipleRuns(conf: FrameworkConf, runs: Int)``` to get solution(s). There is an advanced option of using the best solution found of one parallel run as a seeding solution in the next iteration of a parallel run. This option can be enabled by creating/using an existing "SeedingStrategy" class and setting in inside FrameworkConf before calling ```Framework.run method```.
- Inside ```MapReduceHandler.class``` there are two methods: ```hyperMap(...)``` – which runs the ```algorithm.evaluate``` over the problem, and ```hyperReduce(...)``` – which for now takes the solution with minimum value. For purposes other than PFSP problems, this class should be extended, and then a custom ```MapReduceHandler``` should be set by using ```FrameworkConf.setMapReduceHandler(h: MapReduceHandler)```.

### Algorithms implemented ###

- **NEH**, Nawaz, Enscore and Ham (NEHAlgorithm)
- **Iterated Greedy** (IGAlgorithm)
- **Genetic Algorithm**, Reeves 1995 (GAAlgorithm)
- **Hybrid Genetic Algorithm**, Zheng 2003 (HGAAlgorithm)
- **Simulated Annealing**, Osman's addaption for PFSP 1989 (SAAlgorithm)
- **Improved Simulated Annealing**, Xu and Oja (ISAAlgorithm)
- **Taboo Search**, Taillard 1989 (TSAlgorithm)
- **Taboo Search with backjump tracking**, Novicki and Smutnicki 1994 (TSABAlgorithm)
- **Max Min Ant System**, Stutzle (MMASAlgorithm)
- **m-MMAS**, Rajendran and Ziegler 2002 (MMMASAlgorithm)
- **PACO**, Rajendran and Ziegler 2002 (PACOAlgorithm)

## 📜 Credits & License
- **Core Modernization, NRP Adaptation & Edge Pipeline**: Luca Sacco (Thesis Project - 2026)
- **Original Legacy Framework (HyperSpark)**: Based on the original research by Ciavotta et al. (2019) and the master's thesis extension by Jarno Smit (2021).
The tool is available for use and improvement under a dual-licensing schema envisioning an Apache 2 license as well as a GPL-V3 License. Please contact the repository owners for further clarifications.
