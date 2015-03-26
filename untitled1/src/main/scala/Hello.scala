

/**
 * Created by ivizvary on 2015-03-20.
 */
object Hello  {


  def main(args:Array[String]) = {
    //val imgdnld = new Thread(new Downloader("image"))
    //imgdnld.start()
    //downloader.clear()
    //downloader.add("http://commons.wikimedia.org/wiki/Stained_glass","page")
    //val pgproc = new Thread(new PageProcessor())
    //pgproc.start()

    //val improc = new Thread(new ImageProcessor())
    //improc.start()
    //new ImageProcessor().Test("""C:\Users\ivizvary\Documents\GitHub\ImageComp\\untitled1\src\test\resources\a.jpg""")
    new ImageComposer().Process("""C:\Users\ivizvary\Documents\GitHub\ImageComp\\untitled1\src\test\resources\a.jpg""")
    println("DONE")
  }



}
