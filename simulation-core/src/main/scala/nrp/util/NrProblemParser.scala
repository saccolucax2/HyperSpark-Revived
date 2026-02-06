package nrp.util

import scala.io.Source
import nrp.problem.NrProblem

object NrProblemParser {

  def apply(prefix: String): NrProblem = {
    val basePath = "src/main/resources/" + prefix
    def readLines(suffix: String): List[String] = {
      val fullPath = basePath + suffix
      val source = Source.fromFile(fullPath)
      try {
        source.getLines()
          .map(_.trim)
          .filter(_.nonEmpty)
          .toList
      } finally {
        source.close()
      }
    }
    val paramLines = readLines("Parameters.txt")
    val numCustomers = paramLines.head.toInt
    val numLevels    = paramLines(1).toInt
    val customerWeights = readLines("CustomerWeights.txt")
      .map(_.toDouble)
      .toArray
    val customerRequirements = readLines("CustomerRequirements.txt")
      .map(_.split(",").map(_.trim.toInt))
      .toArray
    val nodeCosts = readLines("NodeCosts.txt")
      .map(_.toDouble)
      .toArray
    val nodeParents = readLines("NodeParents.txt")
      .map(_.split(",").map(_.trim.toInt))
      .toArray
    new NrProblem(
      numCustomers,
      numLevels,
      customerWeights,
      customerRequirements,
      nodeCosts,
      nodeParents
    )
  }
}
