package pfsp.solution

import it.polimi.hyperh.solution.Solution
import pfsp.util.PfsSolutionParser
import scala.io.Source

/**
 * @author Nemanja
 */
class PfsSolution(val permutation:Array[Int]) extends Solution {
  def asString(): String = "Array(" + permutation.mkString(", ")+")"
  override def toString: String = {
    val permString = asString()
    val str = "PfsSolution(permutation:" + permString+")"
    str
  }
  def toList: List[Int] = permutation.toList
}

object PfsSolution {
  def fromFile(path: String): PfsSolution = {
    val source = Source.fromFile(path)
    try {
      val content = source.getLines().mkString
      PfsSolutionParser.apply(content)
        .getOrElse(throw new RuntimeException(s"ParserError on file: $path"))

    } finally {
      source.close()
    }
  }
  def apply(permutation: Array[Int]) = new PfsSolution(permutation)
  def apply(permutation: List[Int]) = new PfsSolution(permutation.toArray)
}