import org.keyczar.Crypter
import scala.io.{BufferedSource, Source}
import spray.json._
import DefaultJsonProtocol._
import scala.reflect.io.{Path, File}
import org.apache.log4j

object App {
  log4j.Logger.getRootLogger.setLevel(log4j.Level.OFF)

  type Db = List[Entry]
  case class Entry(title:String, payload:List[(String, String)])

  object EntryJsonProtocol extends DefaultJsonProtocol {
    implicit val entryFormat = jsonFormat2(Entry)
  }
  import EntryJsonProtocol._

  val fp = "/Users/nick/my.secrets"
  val crypter = new Crypter("/Users/nick/.keyczar")

  def encrypt(text:String) = crypter.encrypt(text)
  def decrypt(cyphertext:String) = crypter.decrypt(cyphertext)
  def read = File(fp).slurp
  def write(cyphertext:String) = Path(fp).toFile.writeAll(cyphertext)
  def serialize(db:Db) = db.toJson.toString
  def deserialize(json:String) = json.asJson.convertTo[Db]
  def save(db:Db) = write(encrypt(serialize(db)))
  def load:Db = deserialize(decrypt(read))

  def list = {
    val db:Db = load

    db.foreach(e => println(s"${e.title}\t${e.payload(0)._2}"))
  }

  def importFromSource(source:BufferedSource) = {
    val entryMatch = """(^[^\[].*)[\s](.*)""".r
    val db:Db = source.getLines.collect {
      case entryMatch(title, password) => Entry(title, List(("password", password)))
    }.toList

    save(db)
  }

  def main(args:Array[String]) = args match {
    case Array("list") => list
    case Array("import") => importFromSource(Source.stdin)
    case Array("import", filePath) => importFromSource(Source.fromFile(filePath))
    case _ =>
  }
}