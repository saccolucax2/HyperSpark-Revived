package it.polimi.hyperh.apps

import it.polimi.hyperh.spark.{Framework, FrameworkConf, TimeExpired}
import nrp.problem.NrProblem
import nrp.algorithms.SAAlgorithm
import it.polimi.hyperh.spark.MapReduceHandlerMaximization
import java.io.{File, FileWriter, BufferedWriter}

object LocalAppNRP {
  def main(args: Array[String]): Unit = {
    val instanceName = sys.env.getOrElse("NRP_INSTANCE", "NRP1")
    println(s"--- [START] Solving Instance: $instanceName ---")
    val t1 = System.nanoTime
    val problem = NrProblem.fromResources(name = instanceName)
    val algo = new SAAlgorithm(initT = 100.0, minT = 0.01, b = 0.0000005, totalCosts = 820, boundPercentage = 0.3)
    val numOfAlgorithms = 4
    val stopCond = new TimeExpired(60000)
    val randomSeed = 118337975
    val conf = new FrameworkConf()
      .setRandomSeed(randomSeed)
      .setDeploymentLocalNumExecutors(numOfAlgorithms)
      .setProblem(problem)
      .setNAlgorithms(algo, numOfAlgorithms)
      .setNDefaultInitialSeeds(numOfAlgorithms)
      .setNumberOfIterations(1)
      .setMapReduceHandler(new MapReduceHandlerMaximization())
      .setStoppingCondition(stopCond)
    val solution = Framework.run(conf)
    val duration = (System.nanoTime - t1) / 1e9d

    // --- Dynamic Log Save ---
    try {
      val outputDir = new File("/app/logs")
      if (!outputDir.exists()) outputDir.mkdirs()
      val logFileName = sys.env.getOrElse("LOG_FILE_NAME", "result.log")
      val bw = new BufferedWriter(new FileWriter(new File(outputDir, logFileName), true))
      val timestamp = java.time.LocalDateTime.now().toString
      bw.write(s"[$timestamp] Instance: $instanceName | Duration: ${duration}s | Solution: $solution\n")
      bw.close()
    } catch {
      case e: Exception => println(s"Error saving log: ${e.getMessage}")
    }
  }
}