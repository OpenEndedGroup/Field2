package trace.video

import field.utility.Pair
import field.utility.Triple
import fieldbox.boxes.Box
import fieldbox.boxes.plugins.Exec
import fieldcef.plugins.up
import org.json.JSONObject
import java.io.File
import javax.script.ScriptEngineManager


class PythonHead2() {


    private lateinit var p: Process
    private lateinit var b: ProcessBuilder

    lateinit var box: Box
    lateinit var ce: Box.BiFunctionOfBoxAnd<String, Triple<Any, MutableList<String>, MutableList<Pair<Int, String>>>>
    var current: Any? = JSONObject()
    lateinit var head: PythonHead
    var started: Boolean = false

    fun nc(box: Box, nc: String, args: List<String>) {

        ce = (box up Exec.exec)!!
        this.box = box

        if (!File(nc).exists()) throw IllegalArgumentException(" can't find the nc executable '${nc}', typo?")

        b = ProcessBuilder()
        b.command(listOf(nc) + args).redirectErrorStream(true)
        p = b.start()!!
        val reader = p.inputStream.reader().buffered(5)

        this.started = true

        Thread {
            started = true

            while (p.isAlive && started) {
                try {
                    val rr = reader.readLine()

                    println("R")
                    if (rr != null && rr.trim().startsWith("{"))
                    {
                        current = this.readLine(rr)
                        PythonHead2.now = current
                    }
                    else if (rr != null)
                        print("pythonhead says $rr")

                    Thread.sleep(2)
                } catch (e: Exception) {
                    e.printStackTrace()
                    Thread.sleep(5)
                }
            }
        }.start()
    }


    fun start(box: Box, python: String, path: String, args: List<String>) {

        ce = (box up Exec.exec)!!
        this.box = box

        if (!File(path).exists()) throw IllegalArgumentException(" can't find the path '${path}', typo?")
        if (!File(python).exists()) throw IllegalArgumentException(" can't find the python executable '${python}', typo?")
        if (!File(path + "/process.py").exists()) throw IllegalArgumentException(" path '${path}' exists, but doesn't contain a python head installation, typo?")

        b = ProcessBuilder()
        b.command(listOf(python, "process.py") + args).directory(File(path)).redirectErrorStream(true)
        p = b.start()!!
        val reader = p.inputStream.reader().buffered(5)

        this.started = true

        Thread {
            started = true

            while (p.isAlive && started) {
                try {
                    val rr = reader.readLine()

                    println("R")
                    if (rr != null && rr.trim().startsWith("{"))
                    {
                        current = this.readLine(rr)
                        PythonHead2.now = current
                    }
                    else if (rr != null)
                        print("pythonhead says $rr")

                    Thread.sleep(2)
                } catch (e: Exception) {
                    e.printStackTrace()
                    Thread.sleep(5)
                }
            }
        }.start()
    }

    fun quit() {
        started = false
        p.destroy()
        heads.entries.removeIf { it.value == this }
    }


    companion object {

        val heads = mutableMapOf<List<String>, PythonHead2>()

        @JvmStatic
        var now: Any? = JSONObject()

        @JvmStatic
        fun start(box: Box, python: String, path: String, args: List<String>): PythonHead2 {
            val a = (args + python) + path
            return heads.computeIfAbsent(a) {
                val p = PythonHead2()
                p.start(box, python, path, args)
                p
            }
        }

        @JvmStatic
        fun nc(box: Box, nc: String, args: List<String>): PythonHead2 {
            val a = (args)
            return heads.computeIfAbsent(a) {
                val p = PythonHead2()
                p.nc(box, nc, args)
                p
            }
        }
    }

    private fun readLine(rr: String): Any? {

        return Json.engine.eval("___=$rr")

    }
}
