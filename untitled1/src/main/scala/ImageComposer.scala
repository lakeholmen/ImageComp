import java.awt.Color
import java.awt.image.{BufferedImage, DataBufferByte, RenderedImage}
import java.io.{ByteArrayInputStream, FileInputStream, FileOutputStream}
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
     var idx = 0
     val images = new ListBuffer[(Fragment,BufferedImage)]
  }

  def CalcLuminosity(r: Int, g: Int, b: Int):Double = {0.375 * r + 0.5 * g + 0.125 * b}
  def CalcLuminosity(r: Double, g: Double, b: Double):Double = {0.375 * r + 0.5 * g + 0.125 * b}

  var fragments = new mutable.HashMap[Tuple3[Int,Int,Int],mutable.HashMap[Tuple3[Int,Int,Int],ImageComponent]]()
  var allImages = new ListBuffer[BufferedImage]
  val resolution = 25
  val resDiv = resolution/256.0
  val intvCount = 255/resolution + 1

  def LoadFragments(): Unit = {
    val mongoClient = MongoClient("localhost", 27017)
    val db = mongoClient("image-comp")
    val coll = db("urls")
    val frags = db("frags")
    val images = coll.find(MongoDBObject("status" -> "processed", "type" -> "image"))
    var c= images.count()
    for (img <- images) {
      var u = img.get("url")
      //println(s"$u")
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
           //var cal = calcFrag(bufImage,fr.left,fr.top,fr.width,fr.height)
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
  }


  def findNearestIn(ri: Int, gi: Int, bi: Int, r: Int, g: Int, b: Int, inp:Option[Tuple3[Double,Double,ImageComponent]] ):Option[Tuple3[Double,Double,ImageComponent]] =
  {
    var res = inp
    fragments.get((ri, gi, bi)) match
    {
      case None => res
      case Some(frag) =>
        var lum = CalcLuminosity(r,g,b)
        for ((ck,cv) <- frag)
        {
            if (cv.luminosity>0)
            {
               var f = lum/cv.luminosity
               if (math.abs(f-1)< maxRescale)
               {
                  var fr = math min (255,f*cv.r)
                  var fg = math min (255,f*cv.g)
                  var fb = math min (255,f*cv.b)
                  var fd = CalcLuminosity(math abs (fr-r), math abs (fg-g), math abs (fb-b))
                  res match
                  {
                    case None => res =Some((fd,f,cv))
                    case Some(s) => if (fd<s._1)
                      res = Some((fd,f,cv))
                  }
               }
            }
            var fd = CalcLuminosity(math abs (cv.r-r), math abs (cv.g-g), math abs (cv.b-b))
            res match
            {
              case None => res =Some((fd,1.0,cv))
              case Some(s) => if (fd<s._1)
                res = Some((fd,1.0,cv))
            }
        }
        res
    }
  }


  def findNearestInDist(d:Int,ri: Int, gi: Int, bi: Int, r: Int, g: Int, b: Int, inp:Option[Tuple3[Double,Double,ImageComponent]] ):Option[Tuple3[Double,Double,ImageComponent]] =
  {
    var rep = inp
    for (dr <- -d to d) for (dg <- -d to d) for (db <- -d to d)
      if ((math abs dr)+(math abs dg)+(math abs db)==d)
        rep = findNearestIn(ri+dr,gi+dg,bi+db,r,g,b,rep)
    rep
  }

  def Match(r:Int,g:Int,b:Int):Option[Tuple3[Double,Double,ImageComponent]] =
  {
    var ri = (math floor (r * resDiv)).asInstanceOf[Int]
    var gi = (math floor (g * resDiv)).asInstanceOf[Int]
    var bi = (math floor (b * resDiv)).asInstanceOf[Int]
    var c = findNearestInDist(0,ri,gi,bi,r,g,b,None)
    c = findNearestInDist(1,ri,gi,bi,r,g,b,c)
    for (i <- 2 to intvCount) {
      if (c == None)
        c = findNearestInDist(i, ri, gi, bi, r, g, b, None)
    }

    c
  }

  def toUnsigned(i:Byte):Int = { if (i>=0) i else 256+i}
  def toByte(i:Int):Byte = { (if (i<=128) i else i-256).asInstanceOf[Byte]}

  var maxRescale = 0.7;

  def calcFrag(image: BufferedImage,x:Int,y:Int,w:Int,h:Int): Fragment =
  {
    val stride = image.getWidth()*3
    var rgb = image.getRaster().getDataBuffer().asInstanceOf[DataBufferByte].getData
    if (image.getType()!=BufferedImage.TYPE_3BYTE_BGR)
    {
      printf("IMAGE-TYPE!!!!!!")

    }

    val rfreq = new Array[Int](256)
    val gfreq = new Array[Int](256)
    val bfreq = new Array[Int](256)
    var totR = 0.0
    var totG = 0.0
    var totB = 0.0
    var absTotR = 0l
    var absTotG = 0l
    var absTotB = 0l
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
        absTotR+=r
        absTotG+=g
        absTotB+=b
        //println(s"$r $g $b")
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

  def calcFrag(image: BufferedImage): Fragment =
  {
    val x = 0
    val y = 0
    val w = image.getWidth()
    val h = image.getHeight()
    val stride = w*3
    var rgb = image.getRaster().getDataBuffer().asInstanceOf[DataBufferByte].getData
    val rfreq = new Array[Int](256)
    val gfreq = new Array[Int](256)
    val bfreq = new Array[Int](256)
    var totR = 0.0
    var totG = 0.0
    var totB = 0.0
    var absTotR = 0l
    var absTotG = 0l
    var absTotB = 0l
    val rowF = 1.0/w
    val totF = 1.0/h
    var offset = stride*y+x
    for (i <- 1 to h)
    {
      var rowR = 0.0
      var rowG = 0.0
      var rowB = 0.0
      var offs = offset
      for (j <- 1 to w)
      {
        val r=  toUnsigned(rgb(2+offs))
        val g = toUnsigned(rgb(1+offs))
        val b = toUnsigned(rgb(offs))
        absTotR+=r
        absTotG+=g
        absTotB+=b
        //println(s"$r $g $b")
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

  def Save(image: BufferedImage, s: String) =
  {
    var fos = new FileOutputStream(s)
    try
    {
       ImageIO.write(image, "png", fos);
    }
    finally
    {
      fos.close()
    }
  }


  def apply(d: Int, bytes: Array[Byte], i: Int,l:Int): Int = {
    var ix = l-1
    var dx = d
    if (dx > 0) {
      var mod = false
      while (dx != 0 && ix > 0) {
        val id = ix * 3 + i
        ix-=1
        var v = toUnsigned(bytes(id))
        if (v<255) {
          bytes(id) = toByte((v+1))
          dx -= 1
          mod =true
        }
      }
      if (mod) dx else 0
    } else {
      var mod = false
      while (dx != 0 && ix > 0) {
        val id = ix * 3 + i
        ix-=1
        var v = toUnsigned(bytes(id))
        if (v>0) {
          bytes(id) = toByte(v-1)
          dx += 1
          mod =true
        }
      }
      if (mod) dx else 0
    }
  }

  def addRgb(image: BufferedImage, dr: Double, dg: Double, db: Double):Unit =
  {
    val x = 0
    val y = 0
    val w = image.getWidth()
    val h = image.getHeight()
    var rgb = image.getRaster().getDataBuffer().asInstanceOf[DataBufferByte].getData
    var totDr = (dr*w*h).asInstanceOf[Int]
    var totDg = (dg*w*h).asInstanceOf[Int]
    var totDb = (db*w*h).asInstanceOf[Int]
    while (totDr!=0)
    {
        totDr = apply(totDr,rgb,2,w*h)
    }
    while (totDg!=0)
    {
       totDg = apply(totDg,rgb,1,w*h)
    }
    while (totDb!=0)
    {
      totDb = apply(totDb,rgb,0,w*h)
    }
  }


  def Process(img:BufferedImage):RenderedImage =
  {
      LoadFragments();
      var tileWidth = 40
      var tileHeight = 40
      var imgWidth = img.getWidth * tileWidth
      var imgHeight = img.getHeight * tileHeight
      var res = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_INT_RGB)
      var g2 = res.getGraphics();
      var rgb = img.getRaster().getDataBuffer().asInstanceOf[DataBufferByte].getData
      var idx = 0
      for (y<-1 to img.getHeight)
      {
        var ix =idx
        for (x <- 1 to img.getWidth)
        {
          var r = toUnsigned(rgb(ix+2))
          var g = toUnsigned(rgb(ix+1))
          var b = toUnsigned(rgb(ix))
          ix+=3
          if (true)
          {
            var x0 = (x-1)*tileWidth
            var y0 = (y-1)*tileHeight
            Match(r,g,b) match
            {
              case None =>
                g2.setColor(new Color(r,g,b))
                g2.fillRect(x0,y0,tileWidth,tileHeight)
              case Some(s) =>
                s._3.idx = (s._3.idx+1) % s._3.images.length
                var fr = s._3.images(s._3.idx)
                var sci = new BufferedImage(tileWidth,tileHeight,BufferedImage.TYPE_3BYTE_BGR);
                var scig = sci.createGraphics()
                //scig.setPaint(new Color(200,255,100))
                //scig.fillRect(0,0,tileWidth,tileHeight)
                scig.drawImage(fr._2,0,0,tileWidth,tileHeight,fr._1.left,fr._1.top,fr._1.left+fr._1.width,fr._1.top+fr._1.width,null)
                scig.dispose()
                //Save(sci,s"c:\\$x-$y-orig.png")
                var origInfo = calcFrag(sci)
                //addRgb(sci,r-s._3.r,g-s._3.g,b-s._3.b)
                //println(s"$x,$y -> ${s._3.r} ${s._3.g} ${s._3.b} matched:${origInfo.totR} ${origInfo.totG} ${origInfo.totB}")
                addRgb(sci,r-origInfo.totR,g-origInfo.totG,b-origInfo.totB)
                //addRgb(sci,r-200,g-255,b-100)
                //var filteredInfo = calcFrag(sci)
                //println(f"${filteredInfo.totR}%{2.2} ${filteredInfo.totG}%{2.2} ${filteredInfo.totB}%{2.2}")
                //println(f"${r-filteredInfo.totR} ${g-filteredInfo.totG} ${b-filteredInfo.totB}")
                //Save(sci,s"c:\\$x-$y-scaled.png")
                g2.drawImage(sci,x0,y0,x0+tileWidth,y0+tileHeight,0,0,tileWidth,tileHeight,null)
              }
            }
          }
        idx+=img.getWidth*3
      }
    g2.dispose()
    res
  }

  def Process(path:String):Unit = {
    var ostr = new FileOutputStream(path + ".prc.jpg")
    try {
      var str = new FileInputStream(path)
      try {
        var img = ImageIO.read(str)
        var prc = Process(img)
        ImageIO.write(prc, "jpg", ostr);
      }
      finally {
        str.close()
      }
    }
    finally {
      ostr.close()
    }
  }

}
