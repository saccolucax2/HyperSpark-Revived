package pfsp.algorithms

import it.polimi.hyperh.algorithms.Algorithm
import pfsp.problem.PfsProblem
import pfsp.neighbourhood.NeighbourhoodOperator
import pfsp.solution.PfsEvaluatedSolution
import pfsp.solution.PfsSolution
import it.polimi.hyperh.spark.TimeExpired
import it.polimi.hyperh.spark.StoppingCondition
import scala.annotation.tailrec

/**
 * @author Nemanja
 */
class MMASAlgorithm(p: PfsProblem, t0: Double, cand: Int, seedOption: Option[PfsSolution])
extends ACOAlgorithm(p, t0, seedOption) with Algorithm {
  /**
   * A secondary constructor.
   */
  def this(p: PfsProblem, seedOption: Option[PfsSolution]) {
    this(p, 0.2, 5, seedOption)//default values
  }
  def this(p: PfsProblem) {
    this(p, 0.2, 5, None)//default values
  }
  //private var seed = seedOption
  def initNEHSolution(p: PfsProblem): PfsEvaluatedSolution = {
    val nehAlgorithm = new NEHAlgorithm()
    nehAlgorithm.evaluate(p).asInstanceOf[PfsEvaluatedSolution]
  }
  override def initialSolution(): PfsEvaluatedSolution = {
    def getSolution ={
      seed match {    
      case Some(seedValue) => seedValue.evaluate(p).asInstanceOf[PfsEvaluatedSolution]
        case None => initNEHSolution(p)
      }
    }
    var solution = getSolution
    solution = localSearch(solution, new TimeExpired(300).initialiseLimit())
    updateTmax(solution)
    updateTmin()
    initializeTrails(Tmax)
    solution
  }
  override def probability(i: Int, j: Int, scheduled: List[Int], notScheduled: List[Int]): Double = {
    if(scheduled.contains(j))
      0
    else{
      def sumTrails(list: List[Int]): Double = {
        @tailrec
        def sum(list: List[Int], acc: Double): Double = {
          list match {
            case List() => acc
            case _ => sum(list.tail, acc + trail(list.head, j))
          }
        }
        sum(list, 0)
      }
      val pij = trail(i, j) / sumTrails(notScheduled)
      pij
    }
  }
  override def constructAntSolution(bestSolution: PfsEvaluatedSolution): PfsEvaluatedSolution = {  
    var scheduled: List[Int] = List()
    var jPos = 1
    var candidates: List[Int]  = List()
    val range = (1 to p.numOfJobs).toList
    var otherJobs: List[Int]  = range
    
    while(jPos <= p.numOfJobs) {
      var nextJob = -1
      val u = random.nextDouble()
      if(u <= p0) {
        candidates = bestSolution.solution.toList.filterNot(job => scheduled.contains(job)).take(cand)
        nextJob = getNextJob(scheduled, candidates, jPos)
      }
      else {
        otherJobs = range.filterNot(job => scheduled.contains(job))
        nextJob = getNextJob(scheduled, otherJobs, jPos)
      }     
      scheduled = scheduled ::: List(nextJob)
      jPos = jPos + 1
    }
    p.evaluatePartialSolution(scheduled)
  }
  def p0: Double = (p.numOfJobs - 4.0)/p.numOfJobs
  def persistenceRate: Double = 0.75
  private def evaporationRate: Double = 1 - persistenceRate
  private var Tmax = 0.2
  private var Tmin = 0.04
  def updateTmax(bestSolution: PfsEvaluatedSolution): Unit = { Tmax = 1/(evaporationRate * bestSolution.value) }
  def updateTmin(): Unit = { Tmin = Tmax / 5 }
  
  def getNextJob(scheduled: List[Int], notScheduled: List[Int], jPos: Int): Int = {
    //construct pairs of (notScheduledJob, probability)
    def constructItems(list: List[Int]): List[(Int, Int)] = {
      var items: List[(Int, Int)] = List()
      var jobs = list
      while(jobs.nonEmpty) {
        val iJob = jobs.head
        val pij: Int = (probability(iJob, jPos, scheduled, notScheduled)*100).asInstanceOf[Int]
        items = items ::: List((iJob,pij))
        jobs = jobs.tail
      }
      items.sortBy[Int](_._2)(Ordering.Int.reverse)//increasing
    }
    val items = constructItems(notScheduled)
    def sample(list: List[(Int, Int)]) = {
      var items = list
      val totalSum: Int = items.map(item => item._2).sum
      val index: Int = random.nextInt(totalSum + 1)
      var sum = 0
      var item = items.head
      while(sum < index) {
        sum = sum + items.head._2
        item = items.head
        items = items.tail
      }
      item._1//Return job position
    }
    sample(items)
  }
  override def localSearch(completeSolution: PfsEvaluatedSolution, stopCond: StoppingCondition): PfsEvaluatedSolution = {
    val tsAlgorithm = new TSAlgorithm()
    var bestSolution = completeSolution
    if(p.numOfJobs <= 50) {
      val moves = NeighbourhoodOperator(random).generateAllNeighbourhoodMoves(p.numOfJobs)
      while(stopCond.isNotSatisfied) {
        bestSolution = tsAlgorithm.firstImprovement(p, bestSolution, moves, stopCond)._1
      }
      bestSolution
    }
    else {
      while(stopCond.isNotSatisfied) {
        val moves = NeighbourhoodOperator(random).generateNRandomNeighbourhoodMoves(p.numOfJobs, tsAlgorithm.getNumOfRandomMoves)
        bestSolution = tsAlgorithm.firstImprovement(p, bestSolution, moves, stopCond)._1 
      }
      bestSolution
    }
  }
  
  def setT(iJob: Int, jPos: Int, newTij: Double): Unit = {
    val i = iJob - 1
    val j = jPos - 1
    if(newTij < Tmin)
        T(i)(j) = Tmin
      else if(newTij > Tmax)
        T(i)(j) = Tmax
      else
        T(i)(j) = newTij
  }
  override def updatePheromones(antSolution: PfsEvaluatedSolution, bestSolution: PfsEvaluatedSolution): Unit = {
    updateTmax(bestSolution)
    updateTmin()
    val usedSolution = bestSolution
    def deposit(iJob: Int,jPos: Int): Double = {
      if(usedSolution.permutation(jPos-1) == iJob)
        1.0/usedSolution.value
      else
        0.0
    }
    for(i <- 1 to p.numOfJobs)
      for(j <- 1 to p.numOfJobs) {
        val newTij = persistenceRate * trail(i,j) + deposit(i,j)
        setT(i, j, newTij)
      }
  }
  
}