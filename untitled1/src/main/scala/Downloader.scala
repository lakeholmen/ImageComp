import java.io.InputStream
import java.net.URL

import com.mongodb.DBObject
import com.mongodb.casbah.{MongoCollection, MongoClient}
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.casbah.Imports._
import sun.misc.IOUtils

/**
 * Created by ivizvary on 2015-03-23.
 */
class Downloader extends Worker {

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
    val all = coll.find(MongoDBObject("status"->"queued"))
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
        next = coll.find(MongoDBObject("status"->"queued")).find(x=>true) // jak to zrobić prosciej
        next match {
          case Some(s) => true
          case None => false
        }
      }
    }
  }

  def doWorkPiece: Unit = {
    next match {
      case None => return
      case Some(s)=>
      {
        next = None
        val su = s.get("url")
        var status = "downloaded"
        var bytes:Array[Byte] = null
        var str:InputStream= null
        try {
          val url = new URL(su.asInstanceOf[String])
          println("downloading "+url)
          try {
            str = url.openStream()
            bytes = IOUtils.readFully(str, -1, true)
          }
          finally {
            if (str!=null) str.close()
          }
        }
        catch
        {
          case ex :Throwable =>
          {
            status = "error"
            println("error")
          }
        }
        var upd = $set("status"->status,"res"->bytes)
        coll.update(s, upd)
      }
    }
  }

}
