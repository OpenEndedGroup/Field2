package trace.util

import java.io.BufferedOutputStream
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.PrintWriter

class SimpleOutput(val f: String) {
    var w: PrintWriter

    init {
        w = PrintWriter(FileWriter(f))
    }

    fun print(n: Any) {
        w.print(n)
    }

    fun println(n: Any) {
        w.println(n)
    }

    fun close() {
        w.close()
    }
}