package trace.sound

import com.illposed.osc.OSCListener
import com.illposed.osc.OSCPortIn
import field.app.RunLoop

class OSCIn(val p: Int) {
    val oin = OSCPortIn(p)

    val values = mutableMapOf<String, Any>()

    init {
        oin.addListener("", { d, mess ->
            println(mess)
            RunLoop.main.once {
                values[mess.address] = mess.arguments
            }
        })
        oin.startListening()
    }

    fun <T> get(name: String, d: Any): T {
        return values.getOrDefault(name, d) as T
    }

}