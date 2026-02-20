package nrp.algorithms

import it.polimi.hyperh.problem.Problem
import it.polimi.hyperh.solution.EvaluatedSolution
import it.polimi.hyperh.algorithms.Algorithm
import it.polimi.hyperh.spark.{StoppingCondition, TimeExpired}
import nrp.problem.NrProblem
import nrp.solution.{NrSolution, NrEvaluatedSolution}
import scala.util.Random
import scala.annotation.tailrec
import nrp.util.Moves

class SAAlgorithm extends Algorithm {
  // Define default values
  var initialTemperature: Double = 100.0
  var minTemperature: Double = 0.001
  var beta: Double = 0.00000005
  var totalCosts = 1000
  private var boundP: Double = 0.3
  private var bound: Double = (totalCosts * boundP).round
  val defaultTimeLimit: Int = 300000 // 5 minutes
  var limitEnabled: Boolean = false
  var maxAttempts: Int = 100

  // Secondary constructors
  def this(initT: Double, minT: Double, b: Double, totalCosts: Double, boundPercentage: Double, isLimitEnabled: Boolean, maxAttemptsVal: Int) {    this()
    initialTemperature = initT
    minTemperature = minT
    beta = b
    boundP = boundPercentage
    bound = (totalCosts * boundPercentage).round
    limitEnabled = isLimitEnabled
    maxAttempts = maxAttemptsVal
  }

  def this(initT: Double, minT: Double, b: Double, totalCosts: Double, boundPercentage: Double,
           seedOption: Option[NrSolution]) {
    this()
    initialTemperature = initT
    minTemperature = minT
    beta = b
    boundP = boundPercentage
    bound = (totalCosts * boundPercentage).round
    seed = seedOption
  }

  def this(seedOption: Option[NrSolution]) {
    this()
    seed = seedOption
  }

  def randomSolution(numCustomers: Int): List[Int] = {
    val solution = Array.fill(numCustomers)(0) // solution with only zeros
    val numInitialCustomers: Int = (0.1 * numCustomers * boundP).round.toInt
    val randomIndices = Seq.fill(numInitialCustomers)(Random.nextInt(numCustomers))
    randomIndices.foreach(solution(_) = 1)
    solution.toList
  }

  def initialSolution(p: NrProblem): NrEvaluatedSolution = {
    seed match { // if a seed is set, evaluate it. Otherwise create random initial solution.
      case Some(seedValue) => seedValue.evaluate(p).asInstanceOf[NrEvaluatedSolution]
      case None => p.evaluate(NrSolution(randomSolution(p.numCustomers))).asInstanceOf[NrEvaluatedSolution]
    }
  }

  override def evaluate(problem: Problem): EvaluatedSolution = {
    val p = problem.asInstanceOf[NrProblem]
    val stopCond = new TimeExpired(defaultTimeLimit)
    evaluate(p, stopCond)
  }

  override def evaluate(problem: Problem, stopCond: StoppingCondition): EvaluatedSolution = {
    val random = new Random
    val p = problem.asInstanceOf[NrProblem]

    def cost(solution: List[Int]): Double = {
      val customerIndices = solution.zipWithIndex.filter(pair => pair._1 == 1).map(pair => pair._2)
      p.calculateCosts(customerIndices)
    }

    def fitness(solution: List[Int]): NrEvaluatedSolution = p.evaluate(NrSolution(solution)).asInstanceOf[NrEvaluatedSolution]

    def checkConstraint(solution: List[Int]): Boolean = {
      if (cost(solution) <= bound) {
        true
      } else false
    }

    @tailrec
    def validMove(solution: List[Int], attempts: Int = 0): List[Int] = {
      if (limitEnabled && attempts >= maxAttempts) {
        return solution
      }

      var newSolution = List[Int]()
      var functionInt = 9999999
      val customerIndices = solution.zipWithIndex.filter(pair => pair._1 == 1).map(pair => pair._2)

      if (customerIndices.length < 1) {
        newSolution = Moves(random).addCustomer(solution)
      } else {
        functionInt = Random.nextInt(5) // 0, 1, 2, 3, or 4
        if ((functionInt == 0) || (functionInt == 1)) newSolution = Moves(random).addCustomer(solution)
        if ((functionInt == 2) || (functionInt == 3)) newSolution = Moves(random).swapCustomers(solution)
        if (functionInt == 4) newSolution = Moves(random).removeCustomer(solution)
      }

      // Check new solution
      if (checkConstraint(newSolution)) {
        newSolution
      } else {
        validMove(solution, attempts + 1)
      }
    }

    def acceptanceProbability(benefit: Double, temperature: Double): Double = scala.math.exp(benefit / temperature)

    val stop = stopCond.asInstanceOf[TimeExpired].initialiseLimit()
    val firstSolution = initialSolution(p)

    @tailrec
    def loop(currentSol: NrEvaluatedSolution, currentTemp: Double, iter: Int = 0): NrEvaluatedSolution = {

      if (iter % 10000 == 0) {
        println(s"[SA-HEARTBEAT] Iter: $iter | Temp: $currentTemp | Best Fitness: ${currentSol.value}")
      }

      if (currentTemp <= minTemperature || !stop.isNotSatisfied) {
        println(s"[SA-END] Algoritmo terminato. Temp finale: $currentTemp")
        currentSol
      } else {
        val newSolution = validMove(currentSol.solution.toList)
        val evNewSolution = fitness(newSolution)
        val benefit = evNewSolution.value - currentSol.value
        val ap = acceptanceProbability(benefit, currentTemp)
        val randNo = random.nextDouble()
        val nextSol = if (benefit > 0 || randNo <= ap) {
          evNewSolution
        } else {
          currentSol
        }
        val nextTemp = currentTemp / (1 + beta * currentTemp)
        loop(nextSol, nextTemp, iter + 1)
      }
    }

    loop(firstSolution, initialTemperature)
  }
}