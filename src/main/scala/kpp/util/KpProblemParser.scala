package kpp.util

import scala.io.Source
import kpp.problem.KpProblem

object KpProblemParser {
  def apply(fileName: String): KpProblem = {
    val path = "src/main/resources/" + fileName
    val source = Source.fromFile(path)
    try {
      val lines = source.getLines().toList
      if (lines.isEmpty) throw new Exception("Empty file")
      val capacity = lines.head.trim.toInt
      val itemLines = lines.tail
      val items = itemLines.map { line =>
        val parts = line.split("\\s+").map(_.toInt)
        (parts(0), parts(1))
      }
      val (profits, weights) = items.unzip
      new KpProblem(capacity, profits.toArray, weights.toArray)

    } finally {
      source.close()
    }
  }
}