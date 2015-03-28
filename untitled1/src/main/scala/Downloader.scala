import java.io.InputStream
import java.net.URL

import com.mongodb.DBObject
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.MongoDBObject
import sun.misc.IOUtils

/**
 * Created by ivizvary on 2015-03-23.
 */

class Downloader(val restype:String) extends Worker {

  val coll = db("urls")
  var next: Option[DBObject] = None

  def add(url:String,resType:String): Unit =
  {
     val existing = coll.find(MongoDBObject("url"->url)).limit(1)
     if (existing.hasNext) return
     var builder = MongoDBObject.newBuilder
     builder += "url" -> url
     builder += "status" -> "queued"
     builder += "type" -> resType
     val query = builder.result
     coll.insert (query)
  }

  def queuedCount():Int =
  {
    val all = coll.find(MongoDBObject("status"->"queued","type"->restype))
    all.count()
  }

  def clear():Unit =
  {
    coll.dropCollection()
  }

  def hasWork:Boolean =
  {
    next match {
      case Some(s) => true
      case None =>
      {
        next = coll.find(MongoDBObject("status"->"queued","type"->restype)).find(x=>true) // jak to zrobić prosciej
        next match {
          case Some(s) => true
          case None => false
        }
      }
    }
  }

  val reExtractBase = "(http:\\/\\/[^\\/]+)\\/".r
  val allowed = Set("upload.wikimedia.org","commons.wikimedia.org",".wikipedia.org")
  val forbidden = Set("/math/","/wiki/Commons","/wiki/User:","/wiki/Main_Page", "/wiki/Portal:", "/wiki/Wikipedia:", "/wiki/Special:", "/wiki/Help:", "/wiki/Talk:", "/wiki/Category:Valued_image", "/wiki/Commons:Valued_image")
  def doWorkPiece: Unit = {
    next match {
      case None =>
      case Some(s) => {
        next = None
        val su = s.get("url")
        var status = "downloaded"
        var bytes: Array[Byte] = null
        var str: InputStream = null
        val url = new URL(su.asInstanceOf[String])
        if ((allowed.contains(url.getHost) || (allowed.exists(p=>url.getHost().endsWith(p)))) && !forbidden.exists(p=>url.getPath().startsWith(p))) {
          try {
            println(s"downloading $restype from $url")
            try {
              str = url.openStream()
              bytes = IOUtils.readFully(str, -1, true)
              if (bytes.length > 1000000) {
                bytes = null;
                status = "too-large"
              }
            }
            finally {
              if (str != null) str.close()
            }
          }
          catch {
            case ex: Throwable => {
              status = "error"
              println("error")
            }
          }
        }
        else {
          status = "not-allowed"
        }
        var upd = $set("status" -> status, "res" -> bytes)
        coll.update(s, upd)
      }
    }
  }
}
