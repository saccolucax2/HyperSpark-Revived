package util

import scala.io.Source
import scala.tools.nsc.io.File
import scala.tools.nsc.io.Path

/**
 * @author Nemanja
 */
object FileManager {
  def read(filepath: String): List[String] = {
    val source = Source.fromFile(filepath)
    try {
      source.getLines().toList
    } finally {
      source.close()
    }
  }
  def write(filepath: String, content: String): Unit = {
    val f = new java.io.File(filepath)
    if(! f.getParentFile.exists())
      f.getParentFile.mkdirs()
    if (!f.exists())
      //f.createNewFile();
      Path(filepath).createFile().writeAll(content)  
  }
  def append(filepath: String, content: String): Unit = {
    File(filepath).appendAll(content)
  }
}