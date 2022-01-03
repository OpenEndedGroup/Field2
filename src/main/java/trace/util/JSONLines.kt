package trace.util

import org.json.JSONObject
import org.json.JSONString
import org.json.JSONStringer
import java.io.File
import java.lang.IllegalArgumentException
import java.nio.file.Files

class JSONLines(val fn: String) {
    val lines = mutableListOf<String>()

    var parseNow: java.util.function.BiFunction<JSONLines, Int, Any>? = null

    init {
        if (!File(fn).exists())
            throw IllegalArgumentException(" file '$fn' doesn't exist?")

        val lines = Files.readAllLines(File(fn).toPath())

        for (ll in lines) {
            val t = ll.trim()
            if (t.length > 0) {
                this.lines.add(t)
            }
        }
    }

    fun parse(n: Int): Any {
        return parseNow!!.apply(this, n)
    }
}