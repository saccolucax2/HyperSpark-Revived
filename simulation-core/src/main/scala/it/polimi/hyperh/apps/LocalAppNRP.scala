package it.polimi.hyperh.apps

import it.polimi.hyperh.spark.{Framework, FrameworkConf, TimeExpired}
import nrp.problem.NrProblem
import nrp.algorithms.SAAlgorithm
import it.polimi.hyperh.spark.MapReduceHandlerMaximization
import java.io.{File, FileWriter, BufferedWriter}

object LocalAppNRP {
  def main(args: Array[String]): Unit = {

    val instanceName = sys.env.getOrElse("NRP_INSTANCE", "NRP1").toUpperCase
    println(s"--- [START] Solving Instance: $instanceName ---")

    val t1 = System.nanoTime
    val problem = NrProblem.fromResources(name = instanceName)

    val (budget, coolingRate, numOfAlgorithms, limitEnabled, maxAttempts) = instanceName match {
      case "NRP1" => (780.0,  0.0000005, 4, false, 0)   // 90%
      case "NRP2" => (3790.0, 0.00005,   4, false, 0)   // 75%
      case "NRP3" => (5285.0, 0.00005,   4, false, 0)   // 60%
      case "NRP4" => (6610.0, 0.0005,    2, true,  100) // 30%
      case "NRP5" => (1790.0, 0.0005,    2, false, 0)   // 45%
      case _      => (2000.0, 0.00005,   2, false, 0)
    }

    println(s"--- [TUNING] Budget: $budget | Cooling Rate: $coolingRate | Workers: $numOfAlgorithms | Limit Enabled: $limitEnabled ---")

    val algo = new SAAlgorithm(initT = 100.0, minT = 0.01, b = coolingRate, totalCosts = budget, boundPercentage = 0.3, isLimitEnabled = limitEnabled, maxAttemptsVal = maxAttempts)
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

    // --- Dynamic Log Saving ---
    try {
      val outputDir = new File("/app/logs")
      if (!outputDir.exists()) outputDir.mkdirs()
      val logFileName = sys.env.getOrElse("LOG_FILE_NAME", s"${instanceName}_result.log")
      val bw = new BufferedWriter(new FileWriter(new File(outputDir, logFileName), true))
      val timestamp = java.time.LocalDateTime.now().toString
      bw.write(s"[$timestamp] Instance: $instanceName | Duration: ${duration}s | Solution: $solution\n")
      bw.close()
      println(s"--- [END] Saved to $logFileName ---")
    } catch {
      case e: Exception => println(s"Error saving log: ${e.getMessage}")
    }
  }
}