

/**
 * Created by ivizvary on 2015-03-20.
 */
object Hello  {


  def main(args:Array[String]) = {
    //val dnld = new Thread(new Downloader())
    //dnld.start()
    //downloader.clear()
    //downloader.add("http://commons.wikimedia.org/wiki/Stained_glass","page")
    //val pgproc = new Thread(new PageProcessor())
    //val improc = new Thread(new ImageProcessor())
    //pgproc.start()
    //improc.start()
    new ImageProcessor().Test("""C:\Users\ivizvary\Documents\GitHub\ImageComp\\untitled1\src\test\resources\a.jpg""")
    println("DONE")
  }



}
