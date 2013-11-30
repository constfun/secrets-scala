import org.keyczar.{Crypter}
import scala.io.{BufferedSource, Source}
import scala.pickling._
import json._

object App {
  case class Entry(title:String, password:String)
  type Db = List[Entry]

  def encrypt(db:Db) = {
    val crypter = new Crypter("~/.keyczar")
    val cyphertext = crypter.encrypt("string")
  }

  def importFromSource(source:BufferedSource) = {
    val entryMatch = """(^[^\[].*)[\s](.*)""".r
    val db:Db = source.getLines.collect {
      case entryMatch(title, password) => Entry(title, password)
    }.toList

    encrypt(db)
  }

  def main(args:Array[String]) = args match {
    case Array("import") => importFromSource(Source.stdin)
    case Array("import", filePath) => importFromSource(Source.fromFile(filePath))
    case _ =>
  }
}