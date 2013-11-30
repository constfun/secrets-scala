import org.keyczar._
import scala.io.{BufferedSource, Source}

object App {
  case class Entry(title:String, password:String)
  type Db = List[Entry]

  def main(args:Array[String]) = args match {
    case Array("import", filePath) => importFromSource(Source.fromFile(filePath))
  }

  def importFromSource(source:BufferedSource) = {
    val entryMatch = """(^[^\[].*)[\s](.*)""".r
    val db:Db = source.getLines.collect {
      case entryMatch(title, password) => Entry(title, password)
    }.toList

    db.foreach(e => println(e.title))
  }
}