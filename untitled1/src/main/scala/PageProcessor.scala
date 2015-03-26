import java.net.URL

import com.mongodb.DBObject
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.MongoDBObject
import sun.misc.IOUtils
import scala.util.matching.Regex
/**
 * Created by ivizvary on 2015-03-23.
 */
class PageProcessor extends Worker {

  val coll = db("urls")
  var next: Option[DBObject] = None

  lazy val dnloader = new Downloader()
  def downloadedCount():Int =
  {
    val all = coll.find(MongoDBObject("status"->"downloaded","type"->"page"))
    all.count()
  }

  def hasWork:Boolean =
  {
    next match {
      case Some(s) => true
      case None =>
      {
        next = coll.find(MongoDBObject("status"->"downloaded","type"->"page")).find(x=>true) // jak to zrobić prosciej?
        next match {
          case Some(s) => true
          case None => false
        }
      }
    }
  }

  val reRelativeLink = "^\\/[^#]+$".r
  val reAbsoluteLink = "^\\/\\/[^#]+$".r
  val reExtractBase = "(http:\\/\\/[^\\/]+)\\/".r

  def addUrl(url: String, baseUrl:String, resType:String):Unit =
  {
    reAbsoluteLink.findFirstMatchIn(url) match
    {
      case Some(s)=>
        dnloader.add("http:"+url,resType)
      case None =>
      {
        reRelativeLink.findFirstMatchIn(url) match
        {
          case Some(s)=>
            dnloader.add(baseUrl+url,resType)
          case None =>
          {

          }
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
        val bytes = s.get("res").asInstanceOf[Array[Byte]]
        val url = s.get("url").asInstanceOf[String]
        var status = "error"
        var b = reExtractBase.findFirstMatchIn(url)
        b match
        {
          case None=>{}
          case Some(baseMatch)=>
          {
            var base = b.get.group(1)
            status = "processed"
            val str= new String(bytes,"UTF-8")
            val ExtractUrl = "<a[^>]*?href\\s*=\\s*[\"\"']?([^'\"\" >]+?)[ '\"\"][^>]*?>".r
            val links = ExtractUrl.findAllMatchIn(str).toList
            for(l<- links)
            {
              addUrl(l.group(1),base,"page")
            }
            val ExtractImg = "<img[^>]*?src\\s*=\\s*[\"\"']?([^'\"\" >]+?)[ '\"\"][^>]*?>".r
            val images = ExtractImg.findAllMatchIn(str).toList
            for(l<- images)
            {
              addUrl(l.group(1),base,"image")
            }
          }
        }
        var upd = $set("status"->status)
        coll.update(s, upd)
      }
    }
  }

}
