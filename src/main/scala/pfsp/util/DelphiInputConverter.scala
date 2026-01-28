package pfsp.util

import scala.io.Source
import util.FileManager

/**
 * @author Nemanja
 */
object DelphiInputConverter {
  
  private def getMString(matrix: Array[Array[Int]]): String = {
    var str: String = ""
    val rsize = matrix.length
    val csize = matrix(0).length
    for(i<-0 until rsize) {
      for(j<-0 until csize) {
        val value = matrix(i)(j).toString
        value.length match {
          case 1 => str = str + " "+ value
          case _ => str = str + value
        }
        if(j<csize-1)
          str = str + " "
      }
      if(i<rsize-1)
        str = str + "\n"
    }
    str
  }
  def main(args : Array[String]): Unit = {
    val indir:String = "D:/Dropbox/Teza - Nemanja/benchmarks/Talillard-Delphi/"
    val outdir:String = "D:/Dropbox/Teza - Nemanja/benchmarks/Talillard-Scala/"
    def filename(prefix: String, i: Int) = {
      val str = i.toString
      str.length match {
        case 1 => prefix+"00"+str+".txt"
        case 2 => prefix+"0"+str+".txt"
        case _ => prefix+str+".txt"
      }
    }
    def processInstance(i: Int): Unit = {
      val inpath = indir + filename("Ta", i)
      val source = Source.fromFile(inpath)
      val rawInputString = try {
        source.getLines().mkString(" x ") + " x "
      } finally {
        source.close()
      }
      val p = DelphiProblemParser(rawInputString)
        .getOrElse(throw new RuntimeException(s"ParserError on file: $inpath"))
      val filecontent = p.numOfJobs + " " + p.numOfMachines + "\n" + getMString(p.jobTimesMatrix)
      val outpath = outdir + filename("inst_ta", i)
      FileManager.write(outpath, filecontent)
    }
   for(i<-1 to 120)
     processInstance(i)
  }
}