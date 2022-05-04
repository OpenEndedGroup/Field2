package trace.graphics

import field.linalg.Vec2
import field.linalg.Vec3
import field.linalg.Vec4
import java.io.File

object LoadOBJ {

    @JvmStatic
    fun loadToLayer(l: Stage.ShaderGroup, fn: String) {
        val t = l.triangleBuilder(fn)

        t.open()

        var v = mutableListOf<Vec3>()
        var vn = mutableListOf<Vec3>()
        var vt = mutableListOf<Vec2>()

        File(fn).useLines {
            it.forEach {
                val s = it.trim()
                if (s.startsWith("#")) return@forEach

                if (s.startsWith("v ")) {
                    val p = s.split(" ")
                    v.add(Vec3(p[1].toDouble(), p[2].toDouble(), p[3].toDouble()))
                }
                if (s.startsWith("vn ")) {
                    val p = s.split(" ")
                    vn.add(Vec3(p[1].toDouble(), p[2].toDouble(), p[3].toDouble()))
                }

                if (s.startsWith("vt ")) {
                    val p = s.split(" ")
                    vt.add(Vec2(p[1].toDouble(), 1-p[2].toDouble()))
                }

                if (s.startsWith("f ")) {
                    val num = s.count { it == '/' }
                    when (num) {
                        6 -> {
                            val p = s.split(" ")
                            val a = p[1].split("/")
                            val b = p[2].split("/")
                            val c = p[3].split("/")

                            t.aux(4, vt[a[1].toInt() - 1])
                            t.aux(3, vn[a[2].toInt() - 1])
                            t.aux(1, Vec4(1.0, 1.0, 1.0, 1.0))
                            t.v(v[a[0].toInt() - 1])

                            t.aux(4, vt[b[1].toInt() - 1])
                            t.aux(3, vn[b[2].toInt() - 1])
                            t.aux(1, Vec4(1.0, 1.0, 1.0, 1.0))
                            t.v(v[b[0].toInt() - 1])

                            t.aux(4, vt[c[1].toInt() - 1])
                            t.aux(3, vn[c[2].toInt() - 1])
                            t.aux(1, Vec4(1.0, 1.0, 1.0, 1.0))
                            t.v(v[c[0].toInt() - 1])

                            t.e(0, 1, 2)
                        }

                        // todo
                        else -> throw java.lang.IllegalArgumentException(it)
                    }
                }
            }
        }
        t.close()

    }
}