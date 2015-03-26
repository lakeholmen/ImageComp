/**
 * Created by ivizvary on 2015-03-25.
 */
class Fragment(val totR:Double,val totG:Double,val totB:Double,val width:Int,val height:Int,val left:Int,val top:Int) {

   override def hashCode():Int =
   {
      left+100*top+width+1000*height
   }

  override def equals(o: Any) = o match {
    case that: Fragment => width==that.width && height==that.height && left==that.left && top ==that.top
    case _ => false
  }
}
