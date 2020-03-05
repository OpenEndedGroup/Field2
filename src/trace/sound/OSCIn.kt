package trace.sound

import com.illposed.osc.OSCListener
import com.illposed.osc.OSCPortIn
import com.illposed.osc.OSCPortOut
import field.app.RunLoop
import java.net.InetAddress

class OSCIn(val p: Int) {

    companion object {
        val portage = mutableMapOf<Int, OSCPortIn>()

        fun make(p: Int): OSCPortIn = portage.computeIfAbsent(p) {
            val o = OSCPortIn(it)
            o.startListening()
            o
        }
    }

    val oin = make(p)

    val values = mutableMapOf<String, Any>()

    init {
        oin.addListener("") { _, mess ->
            println(mess.address+" "+mess.arguments)
            RunLoop.main.once {
                values[mess.address] = mess.arguments
            }
        }
//        oin.startListening()
    }

    fun <T> get(name: String, d: Any): T {
        return values.getOrDefault(name, d) as T
    }

}