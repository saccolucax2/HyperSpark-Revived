package it.polimi.hyperh.spark

import it.polimi.hyperh.algorithms.Algorithm
import it.polimi.hyperh.solution.Solution

/**
 * @author Nemanja
 */
class DistributedDatum(ind: Int, alg: Algorithm, seedOption: Option[Solution], stopCond: StoppingCondition) extends Serializable {
  def id: Int = ind
  def algorithm: Algorithm = alg
  def seed: Option[Solution] = seedOption
  def stoppingCondition: StoppingCondition = stopCond
}
object DistributedDatum {
  def apply(id: Int, algorithm: Algorithm, seed: Option[Solution], stopCond: StoppingCondition): DistributedDatum =  {
    new DistributedDatum(id, algorithm, seed, stopCond)
  }
}
object DistributedDataset {
  def apply(numOfNodes: Int, algorithms: Array[Algorithm], seeds: Array[Option[Solution]], stopCond: StoppingCondition): Array[DistributedDatum] =  {
    var array: Array[DistributedDatum] = Array()
    for(i <- 0 until numOfNodes) {
      val datum = DistributedDatum(i, algorithms(i), seeds(i), stopCond)
      array :+= datum
    }
    array
  }
}