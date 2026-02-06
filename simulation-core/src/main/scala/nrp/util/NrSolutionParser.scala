package nrp.util

import scala.io.Source
import nrp.solution.NrSolution
import nrp.solution.NrEvaluatedSolution

import scala.util.matching.Regex
import scala.util.parsing.combinator.RegexParsers

object NrSolutionParser {
  def apply(fileName: String): NrSolution = {
    val path = "src/main/resources/" + fileName
    val source = Source.fromFile(path)
    try {
      val solution = source.getLines()
        .map(_.trim)
        .filter(_.nonEmpty)
        .map(_.toInt)
        .toArray
      new NrSolution(solution)
    } finally {
      source.close()
    }
  }
}

object NrEvaluatedSolutionParser extends RegexParsers {
  def number: Parser[Int] = """\d+""".r ^^ { _.toInt }
  def identifier: Regex = """[_\p{L}][_\p{L}\p{Nd}]*""".r
  def row: Parser[Array[Int]] = number.+ ^^ {_.toArray}
  def solution: Parser[NrEvaluatedSolution] = identifier ~> number ~ row ^^ {
    case ms ~ r => new NrEvaluatedSolution(ms,r)
  }
  def apply(input: String): Option[NrEvaluatedSolution] = parseAll(solution, input) match {
    case Success(result, _) => Some(result)
    case NoSuccess(_, _) => None
  }
}