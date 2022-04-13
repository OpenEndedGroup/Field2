package trace.video

import field.utility.Pair
import field.utility.Triple
import fieldbox.boxes.Box
import fieldbox.boxes.plugins.Exec
import fieldcef.plugins.up
import org.json.JSONObject
import java.io.File
import javax.script.ScriptEngineManager


class PythonHead() {

    companion object {
        lateinit var box: Box
        lateinit var ce: Box.BiFunctionOfBoxAnd<String, Triple<Any, MutableList<String>, MutableList<Pair<Int, String>>>>
        @JvmStatic
        var now: Any? = JSONObject()
        @JvmStatic
        lateinit var head: PythonHead
        @JvmStatic
        var started: Boolean = false

        @JvmStatic
        fun start(box: Box, python: String, path: String, args: List<String>) {
            if (PythonHead.started) return

            ce = (box up Exec.exec)!!
            PythonHead.box = box

            if (!File(path).exists()) throw IllegalArgumentException(" can't find the path '${path}', typo?")
            if (!File(python).exists()) throw IllegalArgumentException(" can't find the python executable '${python}', typo?")
            if (!File(path + "/process.py").exists()) throw IllegalArgumentException(" path '${path}' exists, but doesn't contain a python head installation, typo?")

            val b = ProcessBuilder()
            b.command(listOf(python, "process.py") + args).directory(File(path)).redirectErrorStream(true)
            val p = b.start()
            val reader = p.inputStream.reader().buffered(5)

            PythonHead.head = PythonHead()


            Thread {
                PythonHead.started = true

                while (p.isAlive) {
                    try {
                        val rr = reader.readLine()

                        println("R")
                        if (rr != null && rr.trim().startsWith("{"))
                            PythonHead.now = PythonHead.head!!.readLine(rr)
                        else if (rr != null)
                            print("r " + rr)
                        Thread.sleep(2)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Thread.sleep(5)
                    }
                }
            }.start()
        }

    }

    private fun readLine(rr: String): Any? {

//        val res = ce.apply(box, "___="+rr)

        return Json.engine.eval("___=" + rr)

//        return res.first
//        return JSONObject(rr)
    }
}

object Json {
//    val jsonParseScript = java.util.Scanner(Json::class.java.getResourceAsStream("jsonparser.js"), "UTF-8").useDelimiter("\\A").next()

    val engine = ScriptEngineManager().getEngineByName("nashorn")


}