package trace.video

import java.io.File
import java.nio.file.Files

// loads a csv file for the images in a directory

class SimpleAnalysis(val fn: String) {
    val data = mutableListOf<List<Float>>()

    init {
        val lines = Files.readAllLines(File(fn).toPath())
        for (s in lines) {
            val pieces = s.split(",")
            data.add(pieces.map { it.toFloat() }.toList())
        }
    }

    fun map(x: java.util.function.Function<List<Float>, Number?>): List<Int> {
        return data.mapIndexed { i, d -> i to x.apply(d) }.filter { it.second != null }.sortedBy { it.second!!.toDouble() }.map { it.first }
    }
}
