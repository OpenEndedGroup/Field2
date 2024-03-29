package trace.sound

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.apache.commons.math4.distribution.EmpiricalDistribution
import org.apache.commons.math4.stat.descriptive.SummaryStatistics
import org.lwjgl.util.WaveData
import trace.sound.Sound.floatsFromFile
import java.io.*
import java.nio.FloatBuffer

class SoundAnalysis2(val fn: String) {
    private var lowlevel: JsonElement

    @JvmField
    var duration: Float

    private var floats: FloatBuffer
    private var sampleRate: Int

    init {

        if (!File(fn).exists()) throw IllegalArgumentException(" can't find a file called $fn")

        val parsed = JsonParser().parse(BufferedReader(FileReader(fn + ".features.json")))

        lowlevel = parsed

        floats = floatsFromFile(fn);
        val data = WaveData.create(BufferedInputStream(FileInputStream(fn)))
        sampleRate = data.samplerate

        duration = (floats.capacity()/data.samplerate).toFloat()

    }

    val cache_getName = object : LinkedHashMap<String, FloatArray>() {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, FloatArray>?): Boolean {
            return size > 50
        }
    }

    val cache_getRange = object : LinkedHashMap<String, Pair<Float, Float>>() {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Pair<Float, Float>>?): Boolean {
            return size > 50
        }
    }

    val cache_getStats = object : LinkedHashMap<String, SummaryStatistics>() {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, SummaryStatistics>?): Boolean {
            return size > 50
        }
    }

    val cache_getDistributation = object : LinkedHashMap<String, EmpiricalDistribution>() {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, EmpiricalDistribution>?): Boolean {
            return size > 50
        }
    }

    fun listNames(): String {
        return lowlevel.asJsonObject.keySet().joinToString { it }
    }

    fun get(name: String): FloatArray {
        return cache_getName.computeIfAbsent(name, {

            val n = lowlevel.asJsonObject.get(name).asJsonArray

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
            if (a < floats.capacity()) r[a - start] = floats.get(a)
        }
        return r
    }


    fun getRange(name: String): Pair<Float?, Float?> {
        return cache_getRange.computeIfAbsent(name, {
            val d = get(name)
            return@computeIfAbsent d.minOrNull()!! to d.maxOrNull()!!
        })
    }

    fun getStatistics(name: String): SummaryStatistics {
        return cache_getStats.computeIfAbsent(name, {
            val d = get(name)
            val s = SummaryStatistics()
            d.forEach { s.addValue(it.toDouble()) }
            s
        })
    }

    fun getDistribution(name : String) : EmpiricalDistribution {
        return cache_getDistributation.computeIfAbsent(name, {
            val d = get(name)
            val ed = EmpiricalDistribution()

            ed.load(d.map { it.toDouble() }.toDoubleArray())

            ed
        })
    }


    fun get(name: String, t: Double): Double {
        val data = get(name)
        val alpha = Math.max(0.0, Math.min(1.0, t / duration.toDouble()))
        val at = (alpha * (data.size)).toInt()
        val at2 = if (at < 0) 0 else if (at >= data.size) data.size - 1 else at
        return data[at2].toDouble()
    }

    fun getNormalized(name: String, t: Double): Double {
        val range = getRange(name)
        return (get(name, t) - range.first!!) / (range.second!! - range.first!!)
    }

    fun getZNormalized(name: String, t: Double): Double {
        val ss = getStatistics(name)
        return (get(name, t) - ss.mean) / ss.standardDeviation
    }

    fun getWhitened(name: String, t: Double): Double {
        val ss = getDistribution(name)
        return ss.cumulativeProbability(get(name, t))
    }



    fun getRollingAverage(name: String, t: Double, window: Double): Double {

        val data = get(name)

        var wa = 0.0;
        var w = 0.0;

        var start = (data.size * (t - window) / duration.toDouble()).toInt()
        var center = (data.size * (t) / duration.toDouble())
        var end = (data.size * (t + window) / duration.toDouble()).toInt()

        start = Math.max(0, Math.min(data.size - 1, start))
        end = Math.max(0, Math.min(data.size - 1, end))

        for (x in start until end) {
            val ww = Math.exp(-Math.pow(4 * (x - center) / (end - start), 2.0))
            wa += ww * data[x]
            w += ww
        }

        if (w > 0) return wa / w
        return 0.0
    }

    fun maxima(name: String, window1: Double, window2: Int, fr: Double): List<Pair<Float, Double>> {
        val data = get(name)
        val d2 = data.mapIndexed { i: Int, v: Float ->
            v / getRollingAverage(name, duration * i / data.size.toDouble(), window1)
        }
        return d2.mapIndexed { index: Int, d: Double -> index to d }.filterIndexed { i, v -> v.first > window2 && v.first < data.size - window2 }
                .filterIndexed { i, v -> v.second > d2[v.first - 1] && v.second > d2[v.first + 1] }.filterIndexed { i, v -> percentageFilter(v.first, d2, window2, fr) }
                .map { v -> v.first * duration / data.size to v.second }
    }

    fun maxima2(name: String, window1: Double, window2: Int, fr: Double): List<Pair<Float, Double>> {
        val data = get(name)
        val d2 = data.mapIndexed { i: Int, v: Float ->
            v / getRollingAverage(name, duration * i / data.size.toDouble(), window1)
        }
        return d2.mapIndexed { index: Int, d: Double -> index to d }.filterIndexed { i, v -> v.first > window2 && v.first < data.size - window2 }
                .filterIndexed { i, v -> v.second > d2[v.first - 1] && v.second > d2[v.first + 1] }.filterIndexed { i, v -> biggerThan(v.first, d2, window2, fr) }.map { v -> v.first * duration / data.size to v.second }
    }

    fun furtherFilter(v: List<Pair<Float, Double>>, dt: Double): List<Pair<Float, Double>> {
        return v.filterIndexed { i, p ->

            if (i == 0) false
            else if (i == v.size - 1) false
            else {
                (Math.abs(v[i - 1].first - p.first) > dt || p.second > v[i - 1].second) && (Math.abs(v[i + 1].first - p.first) > dt || p.second > v[i + 1].second)
            }
        }
    }

    private fun biggerThan(first: Int, d2: List<Double>, window2: Int, fr: Double): Boolean {

        val found = d2.slice(first + 1..first + window2).find { it > d2[first] * fr }
        return found == null
    }

    private fun percentageFilter(i: Int, d2: List<Double>, window2: Int, fr: Double): Boolean {

        val vv = d2.slice(i - window2..i + window2).sortedByDescending { it }

        return d2[i] > 1.5 * vv[(vv.size * fr).toInt()]
    }


}