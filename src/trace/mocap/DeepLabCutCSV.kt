package trace.mocap

import field.linalg.Vec3
import java.io.File
import java.nio.file.Files

class DeepLabCutCSV(val fn: String, val w: Int, val h: Int) {

    val joints = mutableListOf<List<Vec3>>()

    init {

        val ll = Files.readAllLines(File(fn).toPath())
        val rows = ll.map {

            it.split(",").map { it.toFloat() }

        }.toList()

        for (n in 0 until rows.size / 3) {
            val x = rows[3 * n + 0]
            val y = rows[3 * n + 1]
            val c = rows[3 * n + 2]

            joints.add(x.mapIndexed { i, x ->

                Vec3(100 * x / w, 100 * y[i] / w, c[i])
                
            }.toList())
        }
    }

    fun numFrames() = joints[0].size


}