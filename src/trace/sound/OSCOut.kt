package trace.sound

import com.illposed.osc.*
import field.app.RunLoop
import field.linalg.Vec2
import field.linalg.Vec3
import field.linalg.Vec4
import java.net.InetAddress

class OSCOut(val p: Int) {

    companion object {
        val portage = mutableMapOf<Pair<InetAddress, Int>, OSCPortOut>()

        fun make(p: Int): OSCPortOut = portage.computeIfAbsent(InetAddress.getLocalHost() to p) {
            OSCPortOut(it.first, it.second)
        }
    }

    val oout = make(p)

    fun <T> send(name: String, d: T): T {

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
        }

        val message = OSCMessage(name, al)
        oout.send(message)

        return d
    }

}