package trace.mocap

import field.linalg.Vec3
import trace.util.TCB3
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.nio.file.Files
import java.util.*

class CSV2(val directory: String, val suffix: String) {

    var points = mutableListOf<TCB3>()

    init {

        val vv = File(directory).listFiles { dir, n -> n.endsWith(suffix) }

        if (vv == null) throw IllegalArgumentException("can't list '$directory'")
        if (vv.size == 0) throw IllegalArgumentException("'$directory' has no matching files")

        Arrays.sort(vv)

        val numPoints = Files.readAllLines(vv[0].toPath())

        (0 until numPoints.size).forEach { points.add(TCB3()) }

        for (i in 0 until vv.size) {
            val reader = BufferedReader(FileReader(vv[i]))

            var index = 0
            while (reader.ready()) {
                val l = reader.readLine()

                val pieces = l.split(",")

                points[index].newNode(i.toDouble(), Vec3(pieces[0].toDouble(), pieces[1].toDouble(), pieces[2].toDouble()))
                index++
            }

        }

        (0 until numPoints.size).forEach { points[it].tcbAll() }

    }


    fun atTime(time: Double): List<Vec3> {
        return points.map { it.evaluate(time * it.duration(), Vec3()) }
    }

    fun duration(): Double {
        return points[0].duration()
    }


}