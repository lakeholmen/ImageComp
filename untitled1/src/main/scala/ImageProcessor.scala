import java.awt.image.{BufferedImage, DataBufferByte}
import java.io.{ByteArrayInputStream, FileInputStream}
import javax.imageio.ImageIO

import com.mongodb.DBObject
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.casbah.query.Imports._

/**
 * Created by ivizvary on 2015-03-23.
 */
class ImageProcessor extends Worker {

  val coll = db("urls")
  val fragsColl = db("frags")
  var next: Option[DBObject] = None

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
  def toUnsigned(i:Byte):Int = { if (i>=0) i else 256+i}
  def getFragment(image: BufferedImage, rgb:Array[Byte],x: Int, y: Int, w: Int, h: Int, stride:Int): Fragment =
  {
     val rfreq = new Array[Int](256)
     val gfreq = new Array[Int](256)
     val bfreq = new Array[Int](256)
     var totR = 0.0
     var totG = 0.0
     var totB = 0.0
     val rowF = 1.0/w
     val totF = 1.0/h
     var offset = stride*y+x
     for (i <- 1 to w)
     {
       var rowR = 0.0
       var rowG = 0.0
       var rowB = 0.0
       var offs = offset
       for (j <- 1 to h)
       {
         val r=  toUnsigned(rgb(2+offs))
         val g = toUnsigned(rgb(1+offs))
         val b = toUnsigned(rgb(offs))
         rfreq(r)+=1
         gfreq(g)+=1
         bfreq(b)+=1
         rowR += r*rowF
         rowG += g*rowF
         rowB += b*rowF
         //println(s"$i,$j: $r $g $b -> $rowR $rowG $rowB")
         offs += 3
       }
       totR += rowR * totF
       totG += rowG * totF
       totB += rowB * totF
       offset += stride
     }
    var avgxfreq = (w * h) / 256.0;
    var maxrfreq = rfreq.max
    var maxgfreq = gfreq.max
    var maxbfreq = bfreq.max

    new Fragment(maxrfreq/avgxfreq, maxgfreq/avgxfreq, maxbfreq/avgxfreq, totR,totG,totB,w,h,x,y)
  }

  def getFragments(image:BufferedImage):Seq[Fragment] =
  {
     var wd = image.getWidth
     var hg = image.getHeight
     var rgb = image.getRaster().getDataBuffer().asInstanceOf[DataBufferByte].getData
     var res:Seq[Fragment]= null
     if (wd>hg)
     {
       val r = 0 to wd/hg
       res = r.map(i=>getFragment(image,rgb,min(i*hg,wd-hg),0,hg,hg,3*wd))
     }
     else
     {
       val r = 0 to hg/wd
       res = r.map(i=>getFragment(image,rgb,0,min(i*wd,hg-wd),wd,wd,3*wd))
     }
    res.distinct.filter(x=> x.relGFreq + x.relRFreq + x.relBFreq < 100 && x.width >= 30 && x.height >= 30)
  }


  def addFragment(f:Fragment,url:String):Unit =
  {
     var builder = MongoDBObject.newBuilder
     builder += "url" -> url
     builder += "l"->f.left
     builder += "t"->f.top
     builder += "w"->f.width
     builder += "h"->f.height
     builder += "r"->f.totB
     builder += "g"->f.totG
     builder += "b"->f.totB
     fragsColl.insert (builder.result())
  }


  def doWorkPiece(): Unit = {
    next match {
      case None =>
      case Some(s)=>
        next = None
        val bytes = s.get("res").asInstanceOf[Array[Byte]]
        val url = s.get("url").asInstanceOf[String]
        var upd:DBObject = null
        try
        {
          var is = new ByteArrayInputStream(bytes)
          try
          {
            var f = getFragments(ImageIO.read(is))
            if (f.isEmpty)
              upd = $set("status"->"empty")
            else {
              upd = $set("status" -> "processed")
              f.map(fr=>addFragment(fr,url))
            }
          }
          finally
          {
            if (is!=null) is.close()
          }
        }
        catch {
          case e: Throwable =>
            println(e.getMessage)
            upd = $set("status"->"error")

        }
        coll.update(s, upd)


    }
  }

}
