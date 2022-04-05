package trace.sound

import com.illposed.osc.OSCMessage
import com.illposed.osc.transport.OSCPortOut
import field.linalg.Vec2
import field.linalg.Vec3
import field.linalg.Vec4
import java.net.InetAddress


class OSCOut(val p: Int, val ip: String) {

    @JvmOverloads
    constructor(p: Int) : this(p, "127.0.0.1") {
    }

    companion object {
        val portage = mutableMapOf<Pair<InetAddress, Int>, OSCPortOut>()

        fun make(p: Int, ip: String = "127.0.0.1"): OSCPortOut =
            portage.computeIfAbsent(InetAddress.getAllByName(ip)[0] to p) {
                OSCPortOut(it.first, it.second)
            }
    }

    val oout = make(p, ip)

    fun <T> send(name: String, d: T): T {
        print("OSC $name, $d")

        var al = ArrayList<Any>()
        when (d) {
            is Number -> {
                val f = d.toFloat()
                al.add(f)
            }
            is Vec2 -> {
                val f = d.x
                al.add(d.x.toFloat())
                al.add(d.y.toFloat())

            }
            is Vec3 -> {
                al.add(d.x.toFloat())
                al.add(d.y.toFloat())
                al.add(d.z.toFloat())

            }
            is Vec4 -> {
                al.add(d.x.toFloat())
                al.add(d.y.toFloat())
                al.add(d.z.toFloat())
                al.add(d.w.toFloat())
            }
            is String -> {
                val message = OSCMessage(name, mutableListOf<Any>(d))
                oout.send(message)
                return@send d
            }

        }

        val message = OSCMessage(name, al)
        oout.send(message)

        return d
    }

    fun sendList(name: String, dd: List<Any>) {
        print("OSC $name, $dd")

        var al = ArrayList<Any>()
        for (d in dd) {
            when (d) {
                is Number -> {
                    val f = d.toFloat()
                    al.add(f)
                }
                is Vec2 -> {
                    val f = d.x
                    al.add(d.x.toFloat())
                    al.add(d.y.toFloat())

                }
                is Vec3 -> {
                    al.add(d.x.toFloat())
                    al.add(d.y.toFloat())
                    al.add(d.z.toFloat())

                }
                is Vec4 -> {
                    al.add(d.x.toFloat())
                    al.add(d.y.toFloat())
                    al.add(d.z.toFloat())
                    al.add(d.w.toFloat())
                }
                is String -> {
                    al.add(d)
                }

            }
        }

        val message = OSCMessage(name, al)
        oout.send(message)

    }

}