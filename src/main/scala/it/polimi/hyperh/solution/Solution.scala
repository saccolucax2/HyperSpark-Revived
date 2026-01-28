package it.polimi.hyperh.solution

import it.polimi.hyperh.problem.Problem


abstract class Solution extends Serializable {
  
	def evaluate(p:Problem):EvaluatedSolution = {
    p.evaluate(this)
  }
  override def toString = "abstract solution"
}

