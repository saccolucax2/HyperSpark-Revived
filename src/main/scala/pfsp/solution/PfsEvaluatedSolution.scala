package pfsp.solution

import it.polimi.hyperh.solution.EvaluatedSolution
import pfsp.problem.PfsProblem
import pfsp.util.PfsEvaluatedSolutionParser
import scala.io.Source
import java.io.InputStream

/**
 * @author Nemanja
 */
class PfsEvaluatedSolution(override val value: Int, override val solution: PfsSolution) extends EvaluatedSolution(value, solution)
{
  //Alternative constructor
  def this(value: Int, permutation: Array[Int]) = this(value, PfsSolution(permutation))
  override def toString: String = {
    val permString = solution.asString()
    val str = "PfsEvaluatedSolution(value:" + value + ", solution:" + permString + ")"
    str
  }
  def compare(that: EvaluatedSolution): Int = this.value - that.asInstanceOf[PfsEvaluatedSolution].value
  def compare(that: PfsEvaluatedSolution): Int = this.value - that.value
  def permutation: Array[Int] = solution.permutation
}

object PfsEvaluatedSolution {
  def fromFile(path: String): PfsEvaluatedSolution = {
    parseAndClose(Source.fromFile(path), sourceName = path)
  }
  def fromResources(name: String): PfsEvaluatedSolution = {
    val stream: InputStream = getClass.getResourceAsStream("/" + name)
    if (stream == null) throw new RuntimeException(s"Resource '/$name' not found in classpath")
    parseAndClose(Source.fromInputStream(stream), sourceName = name)
  }
  private def parseAndClose(source: Source, sourceName: String): PfsEvaluatedSolution = {
    try {
      val content = source.getLines().mkString
      PfsEvaluatedSolutionParser.apply(content)
        .getOrElse(throw new RuntimeException(s"ParserError processing: $sourceName"))
    } finally {
      source.close()
    }
  }
}
object NaivePfsEvaluatedSolution {
  def apply(problem: PfsProblem) = new PfsEvaluatedSolution(999999999, problem.jobs)
}