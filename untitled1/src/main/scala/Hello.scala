// done - forbidden sites, allowed sites
// todo - nie bardzo wąskie obrazki na odpowiednim tle [srednim]
// todo - obrazki z białego tła  na tle szarym raczej tle [umiemy? antyaliasing!]
// todo - lower rank when a fragment was already used
// todo - wrong calculated rgb for fragments!
// todo - rather put tint on dark places that light ? possible ?
// todo - summary of images && images used
// todo - klatki z filmów, okładki płyt, książek. partytury, okładki gazet, ilustracje opowiadań
// todo - katalog na dysku z jakąś rozsądną strukturą ( podział na foldery kategoryczne, wewnątrz podział
//        na odrzucone, oczekujące, do pocięcia, do użycia w całości, ręczne wrzucanie, ale i wrzucanie przez program.
//        ręczne z własnymi nazwami, przez programami z guidem) . Preprogram, który przygotuje gotowe fragmenty z opisem RGB
//        do których pikseli trzeba było najbardziej podrasować jakieś zdjęcie
// todo - raport, w którym jest opisane jakie są dostepne RGB, jakie najbardziej brakowały [żeby można było pomyśleć] - w sensie
// todo - czy można obrazki umieścić rzucone? obrócone wokół miejsca? jak bardzo to zakłóci całość [wypróbować]? najpierw za pomocą powiększenia
//        do 48x48 i rzucenia w tym samym miejscu, ale w "losowej" kolejności. Potem można różne rozmiary no i obroty.


/**
 * Created by ivizvary on 2015-03-20.
 */
object Hello  {


  def main(args:Array[String]) = {
//    download()
    process()
//    report()
  }
    def report() =
    {
      var  ipr = new ImageProcessor()
      println(ipr.getReport())
  }

  def process() =
  {
    new ImageComposer().Process( """c:\dogshit.jpg""")
  }

  def download():Unit = {
    val imgdnld = new Thread(new Downloader("image"))
    imgdnld.start()
    var dnld = new Downloader("page")
    /*dnld.add("http://en.wikipedia.org/wiki/Sculpture","page");
    dnld.add("http://en.wikipedia.org/wiki/Drawing","page");
    dnld.add("http://en.wikipedia.org/wiki/Category:18th-century_painters","page");
    dnld.add("http://en.wikipedia.org/wiki/Category:Collections_of_the_National_Gallery_of_Art","page");
    dnld.add("http://en.wikipedia.org/wiki/Category:19th-century_painters","page");
    dnld.add("http://en.wikipedia.org/wiki/Category:20th-century_painters","page");
    dnld.add("http://en.wikipedia.org/wiki/Category:14th-century_painters","page");
    dnld.add("http://en.wikipedia.org/wiki/Category:13th-century_painters","page");
    dnld.add("http://en.wikipedia.org/wiki/Category:12th-century_painters","page");
    dnld.add("http://commons.wikimedia.org/wiki/Stained_glass","page");

//    dnld.add("http://commons.wikimedia.org/wiki/Painting","page")
//    dnld.add("http://commons.wikimedia.org/wiki/Painters","page")*/
    val pagedlnd= new Thread(dnld)
    pagedlnd.start()
    val pgproc = new Thread(new PageProcessor())
    pgproc.start()
    val improc = new Thread(new ImageProcessor())
    improc.start()

    //println("DONE")
  }



}
