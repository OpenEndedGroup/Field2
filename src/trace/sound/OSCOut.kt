package trace.sound

import com.illposed.osc.*
import field.app.RunLoop
import java.net.InetAddress

class OSCOut(val p: Int) {
    val oin = OSCPortOut(InetAddress.getLocalHost(), p)

//    fun <T> send(name: String, d: Any): T {
//
//        if (d is Number)
//        {
////            OSCMessage(name, mutableListOf(d.toFloat()))
//        }
//
//
//    }

}