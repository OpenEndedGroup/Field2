package trace.util

import field.graphics.FLine
import field.graphics.FLinesAndJavaShapes
import field.graphics.util.Saver
import field.utility.IdempotencyMap
import field.utility.Rect
import field.utility.Vec2
import java.awt.Desktop
import java.io.File
import java.util.function.Supplier

class SaveForRobot {
    fun saveForRobot(f: List<FLine>) {
        if (f.size == 0) throw IllegalArgumentException(" no lines to save ")

        val base = System.getProperty("user.home") + File.separatorChar + "field_robot" + File.separatorChar

        var x = 1
        while (File(base + Saver.pad(x)).exists()) x++

        val prefix = File(base + Saver.pad(x) + ".robot")
        prefix.parentFile.mkdirs()

        var bounds: Rect? = null
        f.forEach {
            val b = FLinesAndJavaShapes.flineToJavaShape_notThickened(it).bounds2D
            bounds = Rect.union(bounds, Rect(b.x, b.y, b.width, b.height))
        }

        val m = Math.max(bounds!!.w, bounds!!.h)

        println("bounds are $bounds")

        val r = Rasterizer(prefix.absolutePath, {
            var r = Vec2((it.toVec2().x - bounds!!.x) / m, (it.toVec2().y - bounds!!.y) / m)
            println(r)
            r
        })

        f.forEach { r.append(it) }
        r.finish()

        Desktop.getDesktop().browseFileDirectory(prefix.parentFile)
    }

    fun saveForRobot(f: IdempotencyMap<Supplier<FLine>>) {
        saveForRobot(f.values.filter { it != null }.map { it.get() }.filter { it != null }.flatMap { it.pieces() }.toList())
    }

}