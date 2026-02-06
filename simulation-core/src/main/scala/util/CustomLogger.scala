package util

import org.apache.spark.internal.Logging  // changed from: org.apache.spark.Logging
/**
 * @author Nemanja
 */
class CustomLogger extends Logging {
  protected var params: List[String] = List()
  private def reformat(ps: List[String]) = {
    def produceBlanks(N: Int) = {
      if(N==0) ""
      else
        (for(i<-1 to N) yield " ").reduceLeft(_ concat _).concat("\t")
    }
    def fixsize(str: String) = {
      str.concat(produceBlanks(15- str.length))
    }
    ps.map { x => fixsize(x) }
  }
  def setFormat(parameters: List[String]): Unit = {
    params = parameters
    params = reformat(params)
  }
  def getFormatString: String = {
    val toprint = params.reduceLeft(_ concat _).concat("\n")
    toprint
  }
  def printInfo(msg: String): Unit = {
    print(msg)
    logInfo(msg)
  }
  def printFormat(): Unit = { printInfo(getFormatString) }
  def getValuesString(values: List[Any]): String = {
    reformat(values.map { x => x.toString }).reduceLeft(_ concat _).concat("\n")
  }
  def printValues(values: List[Any]): Unit = { printInfo(getValuesString(values)) }
}
object CustomLogger {
  def apply() = new CustomLogger()
  def apply(parameters: List[String]): Unit = new CustomLogger().setFormat(parameters)
}