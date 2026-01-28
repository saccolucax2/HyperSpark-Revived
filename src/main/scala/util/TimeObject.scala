package util

/**
 * @author Nemanja
 */
class TimeObject(val timeMillis: Long) {
  def this() = this(System.currentTimeMillis())
  private val date = new java.util.Date(timeMillis)
  private val formatter = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S")
  override def toString: String = {
    formatter.format(date).replace('.', ':').replace(':', '-')
  }
  private def diff(that: TimeObject): Long = this.timeMillis - that.timeMillis
  def diffInSeconds(that: TimeObject): Double = this.diff(that) / 1000.0
}
object CurrentTime {
  def apply() = new TimeObject()
}