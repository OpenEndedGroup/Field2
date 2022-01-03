package trace.video

import trace.video.analysis.*
import java.io.File
import java.io.FileWriter
import java.io.FilenameFilter
import java.io.PrintWriter
import java.lang.IllegalArgumentException

object Analysis {

    interface Fast {
        fun perform(data: List<File>): List<List<Float>>
    }


    @JvmStatic
    val registry = mutableMapOf<String, Fast>()

    init {
        registry.put("brightness", Brightness())
        registry.put("red", Red())
        registry.put("green", Green())
        registry.put("blue", Blue())
    }

    @JvmStatic
    fun getAnalysis(name: String, directory: String): SimpleAnalysis {
        if (File(directory + "/" + name + ".csv").exists()) return SimpleAnalysis(directory + "/" + name + ".csv")
        if (registry.containsKey(name)) {
            val res = registry.get(name)!!.perform(File(directory).list(FilenameFilter { d, name -> name.endsWith(".jpg") }).sorted().map { File(directory, it) }.toList())
            PrintWriter(FileWriter(File(directory + "/" + name + ".csv"))).use { writer ->
                res.forEach {
                    writer.println(it.joinToString(",", transform = { it.toString() }))
                }
            }
            return SimpleAnalysis(directory + "/" + name + ".csv")
        } else
            throw IllegalArgumentException("can't find an analysis called $name")
    }


}