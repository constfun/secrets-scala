import org.keyczar.{Crypter}
import scala.io.{BufferedSource, Source}
import scala.pickling._
import json._
import scala.io._
import scala.reflect.io.{Path, File}

object App {
  case class Entry(title:String, password:String)
  type Db = List[Entry]

  val fp = "test.encrypted"
  val crypter = new Crypter("/Volumes/Snappy/.keyczar")

  def encrypt(db:Db) = {
    val pickled = db.pickle
    val cyphertext = crypter.encrypt(pickled.value)

    Path(fp).toFile.writeAll(cyphertext)
  }

  def decrypt(cyphertext:String) = crypter.decrypt(cyphertext)
  def read = File(fp).slurp

  def list = {
    val db:List[Int] = decrypt(read).unpickle[List[Int]]
    println(db)
    //val db:Db = decrypt(read("test.encrypted"))
  }

  def importFromSource(source:BufferedSource) = {
    val entryMatch = """(^[^\[].*)[\s](.*)""".r
    val db:Db = source.getLines.collect {
      case entryMatch(title, password) => Entry(title, password)
    }.toList

    encrypt(db)
  }

  def main(args:Array[String]) = args match {
    case Array("list") => list
    case Array("import") => importFromSource(Source.stdin)
    case Array("import", filePath) => importFromSource(Source.fromFile(filePath))
    case _ =>
  }
}