package it.polimi.hyperh.apps

import it.polimi.hyperh.spark.Framework
import it.polimi.hyperh.spark.FrameworkConf
import it.polimi.hyperh.spark.TimeExpired
import pfsp.problem.PfsProblem
import pfsp.algorithms.GAAlgorithm
import java.io.{File, FileWriter, BufferedWriter}

/**
 * @author Nemanja
 * Modified by Luca for Edge Persistence
 */
object LocalApp {
  def main(args: Array[String]): Unit = {
    val problem = PfsProblem.fromResources("inst_ta054.txt")
    val makeAlgo = () => new GAAlgorithm()
    val numOfAlgorithms = 4
    val totalTime = problem.getExecutionTime
    val numOfIterations = 1
    val iterTimeLimit = totalTime / numOfIterations
    val stopCond = new TimeExpired(iterTimeLimit)
    val randomSeed = 118337975

    val conf = new FrameworkConf()
    .setRandomSeed(randomSeed)
    .setDeploymentLocalNumExecutors(numOfAlgorithms)
    .setProblem(problem)
    .setNAlgorithms(makeAlgo, numOfAlgorithms)
    .setNDefaultInitialSeeds(numOfAlgorithms)
    .setNumberOfIterations(numOfIterations)
    .setStoppingCondition(stopCond)

    val solution = Framework.run(conf)
    println(solution)
    try {
      val outputDir = new File("/app/data/logs")
      if (!outputDir.exists()) {
        outputDir.mkdirs()
      }
      val outputPath = "/app/data/logs/results.txt"
      val file = new File(outputPath)
      val bw = new BufferedWriter(new FileWriter(file, true))
      val timestamp = java.time.LocalDateTime.now().toString
      bw.write(s"[$timestamp] Solution found: " + solution.toString + "\n")
      bw.close()
      println(s"--- [EDGE PERSISTENCE] Result saved to $outputPath ---")
    } catch {
      case e: Exception =>
        println(s"--- [EDGE ERROR] Could not save to file: ${e.getMessage} ---")
    }
  }
}