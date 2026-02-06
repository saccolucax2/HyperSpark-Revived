package kpp.util

import scala.io.Source
import kpp.solution.KpSolution
import kpp.solution.KpEvaluatedSolution

import scala.util.matching.Regex
import scala.util.parsing.combinator.RegexParsers

object KpSolutionParser {
  def apply(fileName: String): KpSolution = {
    val path = "src/main/resources/" + fileName
    val source = Source.fromFile(path)
    try {
      val solutionArray = source.getLines()
        .map(_.trim)
        .filter(_.nonEmpty)
        .map(_.toInt)
        .toArray
      new KpSolution(solutionArray)
    } finally {
      source.close()
    }
  }
}

object KpEvaluatedSolutionParser extends RegexParsers {
  def number: Parser[Int] = """\d+""".r ^^ { _.toInt }
  def identifier: Regex = """[_\p{L}][_\p{L}\p{Nd}]*""".r
  def row: Parser[Array[Int]] = number.+ ^^ {_.toArray}
  def solution: Parser[KpEvaluatedSolution] = identifier ~> number ~ row ^^ {
    case ms ~ r => new KpEvaluatedSolution(ms,r)
  }
  def apply(input: String): Option[KpEvaluatedSolution] = parseAll(solution, input) match {
    case Success(result, _) => Some(result)
    case NoSuccess(_, _) => None
  }
}