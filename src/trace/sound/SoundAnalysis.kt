package trace.sound

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import org.lwjgl.util.WaveData
import trace.sound.Sound.*
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.FileReader
import java.nio.FloatBuffer

class SoundAnalysis(val fn: String) {
    private var lowlevel: JsonElement
    private var loudness: JsonArray

    @JvmField
    var duration: Float

    private var floats: FloatBuffer
    private var sampleRate: Int

    init {
        val parsed = JsonParser().parse(BufferedReader(FileReader(fn + ".yaml_frames")))

        lowlevel = parsed.asJsonObject.get("lowlevel")
        loudness = lowlevel.asJsonObject.get("loudness_ebu128").asJsonObject.get("momentary").asJsonArray

        floats = floatsFromFile(fn);
        val data = WaveData.create(BufferedInputStream(
                FileInputStream(fn)))
        sampleRate = data.samplerate

        duration = parsed.asJsonObject.get("metadata").asJsonObject.get("audio_properties").asJsonObject.get("analysis").asJsonObject.get("length").asFloat
    }

    val cache_getName = object : LinkedHashMap<String, FloatArray>() {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, FloatArray>?): Boolean {
            return size > 50
        }
    }

    fun get(name: String): FloatArray {
        return cache_getName.computeIfAbsent(name, {

            val n = if (name.equals("loudness")) loudness else lowlevel.asJsonObject.get(name).asJsonArray

            val o = FloatArray(n.size())

            for (i in 0 until n.size()) {
                val f = n.get(i).asFloat
                o[i] = f
            }
            return@computeIfAbsent o
        })
    }

    fun getRawSamples(t: Double): FloatArray {
        val r = FloatArray(sampleRate / 24);
        val start = Math.max(0, (t * sampleRate).toInt())
        for (a in start until (start + r.size)) {
            if (a < floats.capacity())
                r[a - start] = floats.get(a)
        }
        return r
    }


    fun getRange(name: String): Pair<Float?, Float?> {
        val d = get(name)
        return d.min() to d.max()
    }

    fun get(name: String, t: Double): Double {
        val data = get(name)
        val alpha = Math.max(0.0, Math.min(1.0, t / duration.toDouble()))
        val at = (alpha * data.size).toInt()
        val at2 = if (at < 0) 0 else if (at >= data.size) data.size - 1 else at
        return data[at2].toDouble()
    }

    fun getNormalized(name: String, t: Double): Double {
        val range = getRange(name)
        return (get(name, t) - range.first!!) / (range.second!! - range.first!!)
    }

}