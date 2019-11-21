package trace.graphics

import field.graphics.FLine
import field.graphics.StandardFLineDrawing
import field.linalg.Vec2
import field.linalg.Vec3
import field.linalg.Vec4
import field.utility.Dict
import java.io.File
import java.nio.file.Files
import java.util.function.Supplier

class PointCloud(val fn: String) {
    val points = mutableListOf<Triple<Vec3, Vec4, Vec3?>>()

    init {
        if (fn.endsWith(".txt")) {
            val lines = Files.readAllLines(File(fn).toPath())
            lines.forEach {
                if (!it.startsWith("#")) {
                    val p = it.split(" ")

                    val at = Vec3(p[1].toDouble(), p[2].toDouble(), p[3].toDouble())
                    val col = Vec4(p[4].toDouble() / 255.0, p[5].toDouble() / 255.0, p[6].toDouble() / 255.0, 1.0)


                    points.add(Triple(at, col, null))
                }
            }

        } else if (fn.endsWith(".ply")) {
            val p = LoadPly(fn)
            p.points.forEach {

//                println("normal! $it.normal")
                if (it.color != null)
                    points.add(Triple(it.at, Vec4(it.color!!.x, it.color!!.y, it.color!!.z, 1.0), it.normal))
                else
                    points.add(Triple(it.at, Vec4(1.0, 1.0, 1.0, 1.0), it.normal))
            }
        }
    }


    val normal: Dict.Prop<Supplier<Vec3>> = Dict.Prop<Any>("normal")
            .toCanon<Any>()
            .doc<Any>("adds normals to a node along the line").set(Dict.domain, "fnode")


    fun toFLine(f: FLine) {
        points.forEach {
            f.moveTo(it.first)
            f.node().attributes.put(StandardFLineDrawing.color, it.second)
            if (it.third != null)
                f.node().attributes.put(normal, it.third)
        }

        f.addAuxProperties(4, normal.name)

    }

    fun toFLine(f: FLine, decimate: Int) {
        points.forEachIndexed { index, it ->
            if (index % decimate == 0) {
                f.moveTo(it.first)
                f.node().attributes.put(StandardFLineDrawing.color, it.second)
                if (it.third != null)
                    f.node().attributes.put(normal, it.third)
            }
        }

        f.addAuxProperties(4, normal.name)

    }

    fun getPosition(n: Int): Vec3 {
        return points[n].first
    }

    fun getColor(n: Int): Vec4 {
        return points[n].second
    }


}