package trace.sound

import com.illposed.osc.MessageSelector
import com.illposed.osc.OSCMessageEvent
import com.illposed.osc.transport.OSCPortIn
import field.app.RunLoop

/*
#  {'btl': ['23CCEAF4-4B70-1FA0-9D36-2DD4B6B92E68', 'HTC BS', '(null)', '{\n    kCBAdvDataIsConnectable = 1;\n    kCBAdvDataLocalName = "HTC BS 1B5BE7";\n    kCBAdvDataServiceUUIDs =     (\n        CB00\n    );\n    kCBAdvDataTimestamp = "608101878.588375";\n    kCBAdvDataTxPowerLevel = 4;\n}'], 'seq': 6824}
 */
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
    val times = mutableMapOf<String, Long>()

    init {
        oin.dispatcher.addListener(object : MessageSelector {
            override fun isInfoRequired() = false
            override fun matches(p0: OSCMessageEvent?) = true
        }) { mess ->
            val a = mess.message.address
            val b = mess.message.arguments
            val t = System.currentTimeMillis()

            RunLoop.main.once {
                values[a] = b
                times[a] = t
            }
        }
//        oin.startListening()
    }

    fun clear(name: String) {
        values.remove(name)
        times.remove(name)
    }

    fun <T> get(name: String, d: Any): T {
        return values.getOrDefault(name, d) as T
    }

    fun getTime(name: String, d: Any): Long? {
        return times.getOrDefault(name, -1L)
    }

}