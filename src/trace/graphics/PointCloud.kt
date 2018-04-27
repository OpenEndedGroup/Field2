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
        val lines = Files.readAllLines(File(fn).toPath())
        lines.forEach {
            if (!it.startsWith("#")) {
                val p = it.split(" ")

                val at = Vec3(p[1].toDouble(), p[2].toDouble(), p[3].toDouble())
                val col = Vec4(p[4].toDouble() / 255.0, p[5].toDouble() / 255.0, p[6].toDouble() / 255.0, 1.0)


                points.add(at to col)
            }
        }
    }


    fun toFLine(f: FLine) {
        points.forEach {
            f.moveTo(it.first)
            f.node().attributes.put(StandardFLineDrawing.color, it.second)
        }
    }
}