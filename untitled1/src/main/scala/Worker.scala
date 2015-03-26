import com.mongodb.casbah.MongoClient

/**
 * Created by ivizvary on 2015-03-23.
 */
trait Worker extends Runnable{

  val mongoClient = MongoClient("localhost",27017)
  val db = mongoClient("image-comp")
  var stopping = false

  def doWorkPiece():Unit
  def hasWork:Boolean

  def shouldStop:Boolean =
  {
    stopping
  }

  def stop():Unit =
  {
    stopping = true
  }

  def waitForWork(): Unit =
  {
    Thread.sleep(100)
  }


  override def run(): Unit =
  {
    work
  }

  def work():Unit =
  {
     while(!shouldStop)
     {
        if (hasWork) {
          doWorkPiece
        }
        else{
          waitForWork
        }
     }

  }


}
