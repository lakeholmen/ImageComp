import java.awt.image.BufferedImage
import java.io.{FileInputStream, ByteArrayInputStream}
import javax.imageio.ImageIO

import com.mongodb.DBObject
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.casbah.query.Imports._

/**
 * Created by ivizvary on 2015-03-23.
 */
class ImageProcessor extends Worker {

  val coll = db("urls")
  var next: Option[DBObject] = None

  lazy val dnloader = new Downloader()

  def downloadedCount(): Int = {
    val all = coll.find(MongoDBObject("status" -> "downloaded", "type" -> "image"))
    all.count()
  }

  def Test(filepath: String): Unit =
  {
    var str = new FileInputStream(filepath)
    var image = ImageIO.read(str)
    var frags = getFragments(image)
    for (f<-frags)
    {
      println(f)
    }
  }

  def hasWork:Boolean =
  {
    next match {
      case Some(s) => true
      case None =>
      {
        next = coll.find(MongoDBObject("status"->"downloaded","type"->"image")).find(x=>true) // jak to zrobić prosciej?
        next match {
          case Some(s) => true
          case None => false
        }
      }
    }
  }

  def min(i1:Int,i2:Int):Int = { if (i1>i2) i2 else i1}
  def getFragment(image: BufferedImage, x: Int, y: Int, w: Int, h: Int, stride:Int): Fragment =
  {
     var totR = 0.0
     var totG = 0.0
     var totB = 0.0

     new Fragment(totR,totG,totB,w,h,x,y)
  }


  def getFragments(image:BufferedImage):Seq[Fragment] =
  {
     var wd = image.getWidth
     var hg = image.getHeight
     if (wd>hg)
     {
       val r = 0 to (wd-1)/hg+1
       r.map(i=>getFragment(image,min(i*hg,wd-hg),0,hg,hg,wd)).distinct
     }
     else
     {
       val r = 0 to (hg-1)/wd+1
       r.map(i=>getFragment(image,0,min(i*wd,hg-wd),wd,wd,wd)).distinct
     }


  }


  def doWorkPiece(): Unit = {
    next match {
      case None =>
      case Some(s)=>
        next = None
        val bytes = s.get("res").asInstanceOf[Array[Byte]]
        val url = s.get("url").asInstanceOf[String]
        var status = "error"
        var upd:DBObject = null
        try
        {
          var is = new ByteArrayInputStream(bytes)
          try
          {
            getFragments(ImageIO.read(is))
          }
          finally
          {
            if (is!=null) is.close()
          }
        }
        catch {
          case e: Throwable =>
            println(e.getMessage)
            upd = $set("status"->status)

        }
        //coll.update(s, upd)


    }
  }

}
