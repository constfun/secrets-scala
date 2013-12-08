import org.keyczar.Crypter
import scala.io.{BufferedSource, Source}
import spray.json._
import org.apache.log4j
import java.nio.charset.{StandardCharsets}
import java.nio.file.{StandardOpenOption, Paths, Files}
import scala.collection.JavaConverters._
import scala.sys.process._
import org.apache.commons.lang3.RandomStringUtils

object App {
  log4j.Logger.getRootLogger.setLevel(log4j.Level.OFF)

  type Db = List[Entry]
  case class Entry(title:String, payload:List[(String, String)])
  case class EntryAndPassword(entry:Entry, password:String)

  object WithPassword {
    def unapply(entry:Entry):Option[String] = entry.payload.collectFirst { case ("password", password) => password }
  }

  object EntryJsonProtocol extends DefaultJsonProtocol {
    implicit val entryFormat = jsonFormat2(Entry)
  }
  import EntryJsonProtocol._

  val fp = "/Volumes/Snappy/my.secrets"
  val crypter = new Crypter("/Volumes/Snappy/.keyczar")

  def encrypt(text:String) = crypter.encrypt(text)
  def decrypt(cyphertext:String) = crypter.decrypt(cyphertext)
  def read:String = Files.readAllLines(Paths.get(fp), StandardCharsets.UTF_8).asScala.mkString
  def write(cyphertext:String) = Files.write(Paths.get(fp), cyphertext.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE)
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

  def pbcopy(s:String) = {
    (s"echo $s" #| "pbcopy").!
  }

  def presentChoices(choices:List[EntryAndPassword]) {
    for( i <- 0 until choices.length) println(s"${i+1}: ${choices(i).entry.title}")
    print("Which one? ")
    pbcopy(choices(readInt() - 1).password)
  }

  def passfor(terms:List[String]) = {
    val db = load
    val naiveSearch = terms.mkString(".*?").r
    val matches = db.collect {
      case entry:Entry if naiveSearch.findFirstIn(entry.title).nonEmpty => entry match {
        case WithPassword(password) => EntryAndPassword(entry, password)
      }
    }
    matches match {
      case Nil => println("Nothing found")
      case m :: Nil => pbcopy(m.password)
      case m :: tail => presentChoices(matches)
    }
  }

  def randpass(title:String, len:Int = 20) = {
    val db = load
    val pass = RandomStringUtils.randomAscii(len)
    val entry = Entry(title=title, payload=List(("password", pass)))
    save(entry :: db)
    pbcopy(pass)
  }

  def main(args:Array[String]):Unit = args.toList match {
    case "list" :: Nil => list
    case "randpass" :: title :: Nil => randpass(title)
    case "randpass" :: title :: len :: Nil => randpass(title, len.toInt)
    case "passfor" :: terms => passfor(terms)
    case "import" :: Nil => importFromSource(Source.stdin)
    case "import" :: filePath :: Nil => importFromSource(Source.fromFile(filePath))
    case _ => println("Not a valid command.")
  }
}
