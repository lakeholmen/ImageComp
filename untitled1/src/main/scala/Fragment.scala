/**
 * Created by ivizvary on 2015-03-25.
 */
class Fragment(val relRFreq:Double, val relGFreq:Double, val relBFreq:Double, val totR:Double,val totG:Double,val totB:Double,val width:Int,val height:Int,val left:Int,val top:Int) {

  override def toString():String =
  {
     f"$totR%2.1f;$totG%2.1f;$totB%2.1f($relRFreq%2.1f; $relGFreq%2.1f; $relBFreq%2.1f = ${relRFreq+relGFreq+relBFreq}%2.1f)  ($left,$top) ($width,$height)"
  }

   override def hashCode():Int =
   {
      left+100*top+width+1000*height
   }

  override def equals(o: Any) = o match {
    case that: Fragment => width==that.width && height==that.height && left==that.left && top ==that.top
    case _ => false
  }
}
