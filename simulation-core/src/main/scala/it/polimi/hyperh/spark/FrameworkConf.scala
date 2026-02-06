package it.polimi.hyperh.spark

import it.polimi.hyperh.algorithms.Algorithm
import it.polimi.hyperh.solution.Solution
import it.polimi.hyperh.solution.EvaluatedSolution
import it.polimi.hyperh.problem.Problem
import util.Random

/**
 * @author Nemanja
 */
class FrameworkConf {
  private var algs: Array[Algorithm] = Array()
  private var sds: Array[Option[Solution]] = Array()
  private var problem: Option[Problem] = None
  private var iter: Int = 1
  private var properties: List[(String,String)] = loadDefaults()
  private var handler: MapReduceHandler = new MapReduceHandler()
  private var seedingStrategy: SeedingStrategy = new SameSeeds()
  private var stoppingCondition: StoppingCondition = new TimeExpired(300)
  private var randomSeed: Long = 0

  def setProblem(p: Problem): FrameworkConf = {
    problem = Some(p)
    this
  }
  def getProblem: Problem = { problem.getOrElse(throw new RuntimeException("FrameworkConf: Problem is not set.")) }
  
  def setRandomSeed(seed: Long): FrameworkConf = {
    randomSeed = seed
    this
  }

  private def setAlgorithms(algorithms: Array[Algorithm]) = {
    algs = algorithms
    setNumberOfResultingRDDPartitions(algs.length)
    this
  }
  def getAlgorithms: Array[Algorithm] = algs.clone()
  
  def setNAlgorithms(algorithm: Algorithm, N: Int): FrameworkConf = {
    algs = Array.fill(N)(algorithm)
    setAlgorithms(algs)
  }

  def setNAlgorithms(makeAlgo: () => Algorithm, N: Int): FrameworkConf = {
    var random = new Random()
    if (randomSeed > 0) {
      random = new Random(randomSeed)
    }
    algs = (1 to N).map(i => makeAlgo().setRandomSeed( random.nextLong() )).toArray
    setAlgorithms(algs)
  }
  def clearAlgorithms(): FrameworkConf = {
    algs = Array()
    this
  }
  
  def setInitialSeeds(seeds: Array[Option[Solution]]): FrameworkConf = {
    sds = seeds
    this
  }
  def setNInitialSeeds(seedOption: Option[EvaluatedSolution], N: Int): FrameworkConf = {
    sds = Array.fill(N)(seedOption)
    this
  }
  def setNDefaultInitialSeeds(N: Int): FrameworkConf = {
    sds = Array.fill(N)(None)
    this
  }
  def clearSeeds(): FrameworkConf = {
    sds = Array()
    this
  }
  def getInitialSeeds: Array[Option[Solution]] = sds.clone()
  
  def setSeedingStrategy(strategy: SeedingStrategy): FrameworkConf = {
    seedingStrategy = strategy
    this
  }
  def getSeedingStrategy: SeedingStrategy = { seedingStrategy }
  
  def setStoppingCondition(stopCond: StoppingCondition): FrameworkConf = {
    stoppingCondition = stopCond
    this
  }
  def getStoppingCondition: StoppingCondition = { stoppingCondition }
  
  def setNumberOfIterations(n: Int): FrameworkConf = {
    iter = n
    this
  }
  def getNumberOfIterations: Int = { iter }
  
  //for properties reference visit
  //http://spark.apache.org/docs/latest/configuration.html#viewing-spark-properties
  
  private def setProperty(key: String, value: String) = {
    //remove default entry for provided key
    properties = properties.filterNot{case (k, v) => k == key}
    properties = properties :+ (key, value)
    this
  }
  def getProperties: List[(String, String)] = properties

  private def setSparkMaster(url: String) = {
    setProperty("spark.master", url)
    this
  }
  private def getSparkMaster: String = {
    val result = properties.filter{case (key, value) => key == "spark.master"}
    if(result.nonEmpty)
      result.head._2
    else {
      println("WARN FrameworkConf : Spark master url is not set")
      "(none)"
    }
  }
  def setDeploymentLocalNoParallelism(): FrameworkConf = { setSparkMaster("local") }
  def setDeploymentLocalMaxCores(): FrameworkConf = { setSparkMaster("local[*]") }
  def setDeploymentLocalNumExecutors(numExecutors: Int): FrameworkConf = { setSparkMaster("local["+numExecutors.toString+"]") }
  def setDeploymentSpark(host: String, port: Int): FrameworkConf = { setSparkMaster("spark://"+host+":"+port.toString) }
  def setDeploymentSpark(host: String): FrameworkConf = { setSparkMaster("spark://"+host+":7077") }
  def setDeploymentMesos(host: String, port: Int): FrameworkConf = { setSparkMaster("mesos://"+host+":"+port.toString) }
  def setDeploymentMesos(host: String): FrameworkConf = { setSparkMaster("mesos://"+host+":5050") }
  def setDeploymentYarnClient(): FrameworkConf = { setSparkMaster("yarn-client") }
  def setDeploymentYarnCluster(): FrameworkConf = { setSparkMaster("yarn-cluster") }
  
  def setAppName(name: String): FrameworkConf = {
    setProperty("spark.app.name", name)
  }
  def setNumberOfExecutors(N: Int): FrameworkConf = {
    setProperty("spark.executor.instances", N.toString)
  }
  private def setNumberOfResultingRDDPartitions(N: Int) = {
    setProperty("spark.default.parallelism", N.toString)
  }
  private def loadDefaults() = {
    List(
        ("spark.app.name","HyperH")
        )
  }
  def enableDynamicResourceAllocation(): FrameworkConf = {
    if(getSparkMaster.contains("yarn")) {
      setProperty("spark.shuffle.service.enabled","true")
      setProperty("spark.dynamicAllocation.enabled", "true")
    }
    else {
      println("WARN FrameworkConf : Dynamic Resource Allocation is supported only in Yarn deployment mode.")
      this
    }
  }
  def setMapReduceHandler(h: MapReduceHandler): FrameworkConf = {
    handler = h
    this
  }
  def getMapReduceHandler: MapReduceHandler = handler
}
