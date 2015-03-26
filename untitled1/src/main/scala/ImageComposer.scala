import java.awt.image.BufferedImage
import java.io.{ByteArrayInputStream, FileInputStream}
import javax.imageio.ImageIO

import com.mongodb.casbah.MongoClient
import com.mongodb.casbah.commons.MongoDBObject

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
 * Created by ivizvary on 2015-03-26.
 */
class ImageComposer {

  class ImageComponent(val r:Int,val g:Int,val b:Int)
  {
     lazy val luminosity = CalcLuminosity(r,g,b)
     val images = new ListBuffer[(Fragment,BufferedImage)]
  }

  def CalcLuminosity(r: Int, g: Int, b: Int):Double = {0.375 * r + 0.5 * g + 0.125 * b}

  var fragments = new mutable.HashMap[Tuple3[Int,Int,Int],mutable.HashMap[Tuple3[Int,Int,Int],ImageComponent]]()
  var allImages = new ListBuffer[BufferedImage]
  val resolution = 25;
  def LoadFragments(): Unit = {
    val mongoClient = MongoClient("localhost", 27017)
    val db = mongoClient("image-comp")
    val coll = db("urls")
    val frags = db("frags")
    val images = coll.find(MongoDBObject("status" -> "processed", "type" -> "image"))
    val resDiv = resolution/256.0;
    for (img <- images) {
      var u = img.get("url")
      println(s"$u")
      var str = new ByteArrayInputStream(img.get("res").asInstanceOf[Array[Byte]])
      var bufImage : BufferedImage = null;
      try {
        bufImage = ImageIO.read(str)
        allImages += bufImage
      }
      finally {
        str.close()
      }
       for (frag <- frags.find(MongoDBObject("url"->u)))
       {
           var tr = frag.get("r").asInstanceOf[Double]
           var tg = frag.get("g").asInstanceOf[Double]
           var tb = frag.get("b").asInstanceOf[Double]
           var r = (math floor (tr)).asInstanceOf[Int]
           var g = (math floor (tg)).asInstanceOf[Int]
           var b = (math floor (tb)).asInstanceOf[Int]
           var ri = (math floor (r * resDiv)).asInstanceOf[Int]
           var gi = (math floor (g * resDiv)).asInstanceOf[Int]
           var bi = (math floor (b * resDiv)).asInstanceOf[Int]
           var i = (ri,gi,bi)
           if (!fragments.contains(i)) fragments.put(i,new mutable.HashMap[Tuple3[Int,Int,Int],ImageComponent]())
           var pc = fragments.get(i).get
           var j = (r,g,b)
           if (!(pc contains j)) pc.put(j,new ImageComponent(r,g,b))
           var comp = pc.get(j).get
           var fr= new Fragment(0,0,0,tr,tg,tb,frag.get("w").asInstanceOf[Int],frag.get("h").asInstanceOf[Int],frag.get("l").asInstanceOf[Int],frag.get("t").asInstanceOf[Int])
           comp.images += Tuple2(fr,bufImage)
       }
    }
    for ((fk,fv) <- fragments)
    {
      println(fk)
      for ((ck,cv) <- fv)
      {
        println(s"-$ck -> ${cv.images.length} ")
      }
    }
    println("")
  }


  //def Match(r:Int,g:Int,b:Int):ImageComponent =
  //{


  //}

  def Process(img:BufferedImage):Unit =
  {
      LoadFragments();
  }

  def Process(path:String):Unit = {
    var str = new FileInputStream(path)
    try {
      var img = ImageIO.read(str)
      Process(img)
    }
    finally {str.close()}





  }

}
