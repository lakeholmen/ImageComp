import java.net.URL

import com.mongodb.DBObject
import com.mongodb.casbah.{MongoCollection, MongoClient}
import com.mongodb.casbah.commons.MongoDBObject
import sun.misc.IOUtils

import scala.io.Source

/**
 * Created by ivizvary on 2015-03-20.
 */
object Hello  {


  def main(args:Array[String]) = {
    val dnld = new Thread(new Downloader())
    dnld.start()
    //downloader.clear()
    //downloader.add("http://commons.wikimedia.org/wiki/Stained_glass","page")
    //val pgproc = new Thread(new PageProcessor())
    val improc = new Thread(new ImageProcessor())
    //pgproc.start()
    improc.start()
    println("DONE")
  }



}
