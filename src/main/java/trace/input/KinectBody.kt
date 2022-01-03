package trace.input

import field.app.RunLoop
import field.linalg.Vec3
import trace.sound.OSCIn

class KinectBody {

    class Body {
        val joints = mutableMapOf<String, Vec3>()
        val previous = mutableMapOf<String, Vec3>()
    }

    companion object {
        private var osc: OSCIn

        var bodies = listOf<Body>()

        init {
            osc = OSCIn(8900)
            RunLoop.main.mainLoop.attach {

                val bb = mutableMapOf<Int, Body>()

                osc.values.forEach { k, v ->

                    try {
                        val p = k.split("/")
                        if (p[1]=="num")return@forEach
                        val body = p[1].toInt()
                        val joint = p[2]
                        val axis = when (p[3]) {
                            "x" -> 0
                            "y" -> 1
                            "z" -> 2
                            else -> throw NumberFormatException()
                        }

                        bb.computeIfAbsent(body) {
                            Body()
                        }.joints.computeIfAbsent(joint) { Vec3() }!![axis] = (v as List<Number>)[0].toDouble()


                    } catch (e: Exception) {
                        println(k + " " + v)
//                        e.printStackTrace()
                    }

                }

                bb.forEach { t, u ->
                    if (bodies.size <= t) {
                        u.joints.forEach { name, pos ->
                            u.previous[name] = Vec3(pos)
                        }
                    } else {
                        val o = bodies[t]
                        u.joints.forEach { name, pos ->
                            if (o.joints.contains(name)) {
                                u.previous[name] = Vec3(o.joints[name])
                            } else u.previous[name] = Vec3(pos)
                        }
                    }
                }

                bodies = bb.toSortedMap().values.toList()

                true
            }
        }
    }
}