package trace.graphics

import field.graphics.FLine
import field.graphics.StandardFLineDrawing
import field.linalg.Vec3
import field.linalg.Vec4
import java.io.File
import java.nio.file.Files

class PointCloud(val fn: String) {
    val points = mutableListOf<Pair<Vec3, Vec4>>()

    init {
        if (fn.endsWith(".txt")) {
            val lines = Files.readAllLines(File(fn).toPath())
            lines.forEach {
                if (!it.startsWith("#")) {
                    val p = it.split(" ")

                    val at = Vec3(p[1].toDouble(), p[2].toDouble(), p[3].toDouble())
                    val col = Vec4(p[4].toDouble() / 255.0, p[5].toDouble() / 255.0, p[6].toDouble() / 255.0, 1.0)


                    points.add(at to col)
                }
            }

        } else if (fn.endsWith(".ply")) {
            val p = LoadPly(fn)
            p.points.forEach {
                if (it.color != null)
                    points.add(it.at to Vec4(it.color!!.x, it.color!!.y, it.color!!.z, 1.0))
                else
                    points.add(it.at to Vec4(1.0, 1.0, 1.0, 1.0))
            }
        }
    }


    fun toFLine(f: FLine) {
        points.forEach {
            f.moveTo(it.first)
            f.node().attributes.put(StandardFLineDrawing.color, it.second)
        }
    }

    fun getPosition(n: Int): Vec3 {
        return points[n].first
    }

    fun getColor(n: Int): Vec4 {
        return points[n].second
    }



}