package trace.mocap

import field.linalg.Vec3
import java.io.BufferedReader
import java.io.FileReader

class CSV(val f: String) {

    var points = mutableListOf<Vec3>()

    init {
        val reader = BufferedReader(FileReader(f))

        while (reader.ready()) {
            val l = reader.readLine()

            val pieces = l.split(",")

            points.add(Vec3(pieces[0].toDouble(), pieces[1].toDouble(), pieces[2].toDouble()))
        }

    }

}