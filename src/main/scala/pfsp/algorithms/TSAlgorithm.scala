package pfsp.algorithms

import it.polimi.hyperh.problem.Problem
import it.polimi.hyperh.solution.Solution
import it.polimi.hyperh.solution.EvaluatedSolution
import it.polimi.hyperh.algorithms.Algorithm
import it.polimi.hyperh.spark.StoppingCondition
import it.polimi.hyperh.spark.TimeExpired
import pfsp.problem.PfsProblem
import pfsp.solution.PfsSolution
import pfsp.solution.PfsEvaluatedSolution
import pfsp.solution.NaivePfsEvaluatedSolution
import pfsp.neighbourhood.NeighbourhoodOperator
import scala.annotation.tailrec
/**
 * @author Nemanja
 */
class TSAlgorithm(
    seedOption: Option[PfsSolution]
    ) extends Algorithm {
  
  private var maxTabooListSize: Int = 7
  private var numOfRandomMoves: Int = 20
  private val neighbourhoodSearch: (List[Int], Int, Int) => List[Int] = NeighbourhoodOperator(random).INSdefineMove
  seed = seedOption
  /**
   * A secondary constructor.
   */
  def this(maxTabooListSize: Int, numOfRandomMoves: Int) {
    this(None)
    this.maxTabooListSize = maxTabooListSize
    this.numOfRandomMoves = numOfRandomMoves
  }
  def this(maxTabooListSize: Int) {
    this(None)
    this.maxTabooListSize = maxTabooListSize
  }

  def this() {
    this(None)
  }
  def getNumOfRandomMoves: Int = {
    val copy = numOfRandomMoves
    copy
  }
  def initNEHSolution(p: PfsProblem): PfsEvaluatedSolution = {
    val nehAlgorithm = new NEHAlgorithm()
    nehAlgorithm.evaluate(p).asInstanceOf[PfsEvaluatedSolution]
  }
  def initialSolution(p: PfsProblem): PfsEvaluatedSolution = {
    seed match {
      case Some(seedValue) => seedValue.evaluate(p).asInstanceOf[PfsEvaluatedSolution]
      case None => initNEHSolution(p)
    }
  }
  //with default time limit
  private def evaluateSmallProblem(p: PfsProblem): PfsEvaluatedSolution = {
    //algorithm time limit
    val timeLimit = p.getExecutionTime
    val stopCond = new TimeExpired(timeLimit).initialiseLimit()
    evaluateSmallProblem(p, stopCond)
  }
  private def evaluateSmallProblem(p: PfsProblem, stopCond: StoppingCondition): PfsEvaluatedSolution = {
    var evBestSolution = NaivePfsEvaluatedSolution(p)
    var allMoves: List[(Int,Int)] = List()//dummy initalization
    
    def loop(bestSolution: PfsEvaluatedSolution, taboo: List[Int], iter: Int): PfsEvaluatedSolution = {
      if(stopCond.isNotSatisfied) {
        if(iter == 1) {
          evBestSolution = initialSolution(p)
          allMoves = NeighbourhoodOperator(random).generateAllNeighbourhoodMoves(p.numOfJobs)
        } else {
          evBestSolution = bestSolution
        }
        val pair = firstImprovement(p, evBestSolution, allMoves, taboo, stopCond)
        val evNewSolution = pair._1
        evBestSolution = List(evNewSolution, evBestSolution).minBy(_.value)
        val tabooList = updateTabooList(taboo, evBestSolution)
        loop(evBestSolution, tabooList, iter + 1)
      }
      evBestSolution
    }
    loop(evBestSolution, List(), 1)
  }
  //with default time limit
  private def evaluateBigProblem(p: PfsProblem): PfsEvaluatedSolution = {
    //algorithm time limit
    val timeLimit = p.getExecutionTime
    val stopCond = new TimeExpired(timeLimit).initialiseLimit()
    evaluateBigProblem(p, stopCond)
  }
  private def evaluateBigProblem(p: PfsProblem, stopCond: StoppingCondition): PfsEvaluatedSolution = {
    var evBestSolution = NaivePfsEvaluatedSolution(p)
    
    @tailrec
    def loop(bestSolution: PfsEvaluatedSolution, taboo: List[Int], iter: Int): PfsEvaluatedSolution = {
      if(stopCond.isNotSatisfied) {
        if(iter == 1) {
          evBestSolution = initialSolution(p)
        } else {
          evBestSolution = bestSolution
        }
        //Examine a fixed number of moves that are not taboo, randomly generated. Good method for huge instances
        val allMoves = NeighbourhoodOperator(random).generateNRandomNeighbourhoodMoves(p.numOfJobs, numOfRandomMoves)
        val pair1 = firstImprovement(p, evBestSolution, allMoves, taboo, stopCond)
        val evNewSolution = pair1._1
        evBestSolution = List(evNewSolution, evBestSolution).minBy(_.value)
        val tabooList = updateTabooList(taboo, evBestSolution)
        loop(evBestSolution, tabooList, iter + 1)
      }
      else evBestSolution
    }
    loop(evBestSolution, List(), 1)
  }
  override def evaluate(problem: Problem): EvaluatedSolution = {
    val p = problem.asInstanceOf[PfsProblem]
    if(p.numOfJobs <= 11)
      evaluateSmallProblem(p)
    else
      evaluateBigProblem(p)
  }
  override def evaluate(problem:Problem, stopCond: StoppingCondition): EvaluatedSolution = {
    val p = problem.asInstanceOf[PfsProblem]
    if(p.numOfJobs <= 11)
      evaluateSmallProblem(p, stopCond)
    else
      evaluateBigProblem(p, stopCond)
  }
  override def evaluate(p:Problem, seedSol: Option[Solution], stopCond: StoppingCondition):EvaluatedSolution = {
    seed = seedSol
    evaluate(p, stopCond)
  }
  def updateTabooList(tabooList: List[Int], solution: PfsEvaluatedSolution): List[Int] = {
    if (tabooList.size == maxTabooListSize) {
        //remove the oldest forbidden move, and add new move at the end
        tabooList.drop(1) ::: List(solution.value)
      } else
        tabooList ::: List(solution.value)
  }
  
  def isForbidden(tabooList: List[Int], makespan: Int): Boolean = {
    tabooList.contains(makespan)//forbidden makespan if it is in taboo list
  }

  //Examine all provided moves and take the first which improves the current solution
  def firstImprovement(p: PfsProblem, evOldSolution: PfsEvaluatedSolution, allMoves: List[(Int, Int)], stopCond: StoppingCondition): (PfsEvaluatedSolution, (Int, Int)) = {
    var bestSolution = evOldSolution
    var candidateMoves = allMoves
    var move = (0, 1) //dummy initialization
    var betterNotFound = true
    while (betterNotFound && candidateMoves.nonEmpty && stopCond.isNotSatisfied) {
      val perturbed = neighbourhoodSearch(evOldSolution.solution.toList, candidateMoves.head._1, candidateMoves.head._2) 
      val evNewSolution = p.evaluate(PfsSolution(perturbed)).asInstanceOf[PfsEvaluatedSolution]
      if (evNewSolution.value < bestSolution.value) {
        bestSolution = evNewSolution
        move = candidateMoves.head
        betterNotFound = false
      }
      candidateMoves = candidateMoves.tail
    }
    (bestSolution, move)
  }
  //Examine the moves (that are not taboo) and take the first which improves the current solution
  def firstImprovement(p: PfsProblem, evOldSolution: PfsEvaluatedSolution, allMoves: List[(Int, Int)], tabooList: List[Int], stopCond: StoppingCondition): (PfsEvaluatedSolution, (Int, Int)) = {
    var bestSolution = evOldSolution
    var candidateMoves = allMoves
    var move = (0, 1) //dummy initialization
    var betterNotFound = true
    while (betterNotFound && candidateMoves.nonEmpty && stopCond.isNotSatisfied) {
      val perturbed = neighbourhoodSearch(evOldSolution.solution.toList, candidateMoves.head._1, candidateMoves.head._2)
      val evNewSolution = p.evaluate(PfsSolution(perturbed)).asInstanceOf[PfsEvaluatedSolution]
      if (evNewSolution.value < bestSolution.value && (! isForbidden(tabooList, evNewSolution.value))) {
        bestSolution = evNewSolution
        move = candidateMoves.head
        betterNotFound = false
      }
      candidateMoves = candidateMoves.tail
    }
    (bestSolution, move)
  }
  //Examine all the moves and take the best
  //the neighbourhood must be examined in parallel for big instances
  def bestImprovement(p: PfsProblem, evOldSolution: PfsEvaluatedSolution, allMoves: List[(Int, Int)], stopCond: StoppingCondition): (PfsEvaluatedSolution, (Int, Int)) = {
    var bestSolution = evOldSolution
    var candidateMoves = allMoves
    var move = (0, 1) //dummy initialization
    while (candidateMoves.nonEmpty && stopCond.isNotSatisfied) {
      val perturbed = neighbourhoodSearch(evOldSolution.solution.toList, candidateMoves.head._1, candidateMoves.head._2)
      val evNewSolution = p.evaluate(PfsSolution(perturbed)).asInstanceOf[PfsEvaluatedSolution]
      if (evNewSolution.value < bestSolution.value) {
        bestSolution = evNewSolution
        move = candidateMoves.head
      }
      candidateMoves = candidateMoves.tail
    }
    (bestSolution, move)
  }
  //Examine all the moves (that are not taboo) and take the best
  //the neighbourhood must be examined in parallel for big instances
  def bestImprovement(p: PfsProblem, evOldSolution: PfsEvaluatedSolution, allMoves: List[(Int, Int)], tabooList: List[Int], stopCond: StoppingCondition): (PfsEvaluatedSolution, (Int, Int)) = {
    var bestSolution = evOldSolution
    var candidateMoves = allMoves
    var move = (0, 1) //dummy initialization
    while (candidateMoves.nonEmpty && stopCond.isNotSatisfied) {
      val perturbed = neighbourhoodSearch(evOldSolution.solution.toList, candidateMoves.head._1, candidateMoves.head._2)
      val evNewSolution = p.evaluate(PfsSolution(perturbed)).asInstanceOf[PfsEvaluatedSolution]
      if (evNewSolution.value < bestSolution.value && (! isForbidden(tabooList, evNewSolution.value))) {
        bestSolution = evNewSolution
        move = candidateMoves.head
      }
      candidateMoves = candidateMoves.tail
    }
    (bestSolution, move)
  }
  
}
