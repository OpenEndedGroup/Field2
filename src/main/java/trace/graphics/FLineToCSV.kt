package trace.graphics

import field.graphics.FLine
import field.utility.IdempotencyMap
import java.io.BufferedReader
import java.io.File
import java.io.OutputStreamWriter
import java.util.function.Supplier

class FLineToCSV {

    fun flinesToCSV(l: IdempotencyMap<Supplier<FLine>>, fn: String) {

        File(fn).writer().use { writer ->
            l.values.forEach {
                writeFLine(writer, it.get())
            }
        }
    }

    fun flineToCSV(l: FLine, fn: String) {

        File(fn).writer().use { writer ->
            writeFLine(writer, l)
        }
    }

    private fun writeFLine(writer: OutputStreamWriter, f: FLine) {
        writer.write(f.sampleByDistance(0.5).flatMap { listOf(it.x, it.y) }.joinToString(separator = ",")+"\n")
    }
}