package trace.util

import field.graphics.FastJPEG
import field.linalg.Vec3
import fieldbox.io.IO.pad
import java.io.File
import java.lang.Exception
import java.lang.Math.floor
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors
import java.util.stream.Stream
import kotlin.experimental.and
import kotlin.streams.toList

class SlitScanner(val source: String) {


    var data: MutableList<ByteBuffer>
    var dim: IntArray = intArrayOf(0, 0)

    init {
        var files = Files.list(File(source).toPath()).filter { it.toString().endsWith(".jpg") && !it.fileName.startsWith(".") }.sorted().toList()
        while (files.size > 500)
            files.toList().filterIndexed { i, b -> i % 2 == 0 }.toList()
        data = files.map {
            dim = FastJPEG.j.dimensions(it.toString())
            val dat = ByteBuffer.allocateDirect(3 * dim[0] * dim[1]).order(ByteOrder.nativeOrder())
            FastJPEG.j.decompress(it.toString(), dat, dim[0], dim[1])

            dat
        }.toMutableList()
    }

    fun access(x: Int, y: Int, n: Int, out: FloatArray, offset: Int) {
        val o = 3 * (y * dim[0] + x)
        out[0 + offset] = (data[n].get(o + 0).toInt() and 255) / 255f
        out[1 + offset] = (data[n].get(o + 1).toInt() and 255) / 255f
        out[2 + offset] = (data[n].get(o + 2).toInt() and 255) / 255f
    }

    fun buildImage(fn: String, w: Int, h: Int, func: (Float, Float, Float, Vec3) -> Unit) {

        val o = ByteBuffer.allocateDirect(3 * w * h).order(ByteOrder.nativeOrder())

        val b = object : ThreadLocal<FloatArray>() {
            override fun initialValue(): FloatArray {
                return FloatArray(24)
            }
        }

        val pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 2)

        for (y in 0 until h) {
            pool.submit {
                print("$y / $h ")
                val oo = b.get()
                val a = Vec3()
                try {
                    for (x in 0 until w) {

                        func(x / w.toFloat(), y / h.toFloat(), 0.5f, a)
                        accessI(a.x.toFloat(), a.y.toFloat(), a.z.toFloat(), oo)

                        o.put(3 * y * w + 3 * x + 0, (Math.max(0f, Math.min(1f, oo[0])) * 255).toByte())
                        o.put(3 * y * w + 3 * x + 1, (Math.max(0f, Math.min(1f, oo[1])) * 255).toByte())
                        o.put(3 * y * w + 3 * x + 2, (Math.max(0f, Math.min(1f, oo[2])) * 255).toByte())

                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        pool.shutdown()
        pool.awaitTermination(1, TimeUnit.HOURS)

        FastJPEG().compress(fn, o, w, h)
    }


    fun buildVideoSequence(fn: String, w: Int, h: Int, d: Int, func: (Float, Float, Float, Vec3) -> Unit) {

        try {
            File(fn).mkdirs()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val o = ByteBuffer.allocateDirect(3 * w * h).order(ByteOrder.nativeOrder())

        val b = object : ThreadLocal<FloatArray>() {
            override fun initialValue(): FloatArray {
                return FloatArray(24)
            }
        }

        val pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 2)

        for (z in 0 until d) {
            val futures = mutableListOf<Future<*>>()
            for (y in 0 until h) {
                print("$z / $d ")
                futures.add(pool.submit {
                    val oo = b.get()
                    val a = Vec3()
                    try {
                        for (x in 0 until w) {

                            func(x / w.toFloat(), y / h.toFloat(), z / (d - 1f), a)
                            accessI(a.x.toFloat(), a.y.toFloat(), a.z.toFloat(), oo)

                            o.put(3 * y * w + 3 * x + 0, (Math.max(0f, Math.min(1f, oo[0])) * 255).toByte())
                            o.put(3 * y * w + 3 * x + 1, (Math.max(0f, Math.min(1f, oo[1])) * 255).toByte())
                            o.put(3 * y * w + 3 * x + 2, (Math.max(0f, Math.min(1f, oo[2])) * 255).toByte())

                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                })
            }
            futures.forEach { it.get() }

            FastJPEG().compress(fn + "/o_" + pad(z) + ".jpg", o, w, h)

        }
        pool.shutdown()

    }


    // out must have length 24, answer in first three elements
    fun accessI(x: Float, y: Float, n: Float, out: FloatArray) {

        val X0 = x * dim[0]
        val X = kotlin.math.floor(X0.toDouble()).toInt()
        val Y0 = y * dim[1]
        val Y = kotlin.math.floor(Y0.toDouble()).toInt()
        val Z0 = n * data.size
        val Z = kotlin.math.floor(Z0.toDouble()).toInt()
        val dm1 = dim[0] - 1
        val d1m1 = dim[1] - 1
        val dsm1 = data.size - 1

        access(clamp(0, dm1, X), clamp(0, d1m1, Y), clamp(0, dsm1, Z), out, 0)
        access(clamp(0, dm1, X + 1), clamp(0, d1m1, Y), clamp(0, dsm1, Z), out, 3)
        access(clamp(0, dm1, X + 1), clamp(0, d1m1, Y + 1), clamp(0, dsm1, Z), out, 6)
        access(clamp(0, dm1, X), clamp(0, d1m1, Y + 1), clamp(0, dsm1, Z), out, 9)

        access(clamp(0, dm1, X), clamp(0, d1m1, Y), clamp(0, dsm1, Z + 1), out, 12 + 0)
        access(clamp(0, dm1, X + 1), clamp(0, d1m1, Y), clamp(0, dsm1, Z + 1), out, 12 + 3)
        access(clamp(0, dm1, X + 1), clamp(0, d1m1, Y + 1), clamp(0, dsm1, Z + 1), out, 12 + 6)
        access(clamp(0, dm1, X), clamp(0, d1m1, Y + 1), clamp(0, dsm1, Z + 1), out, 12 + 9)


        val a0 = X0 - X
        val a1 = Y0 - Y
        val a2 = Z0 - Z
        val ma0 = 1 - a0
        val ma1 = 1 - a1
        val ma2 = 1 - a2


        for (n in 0 until 2)
            out[n + 0] = (out[n + 0] * ma0 * ma1 + out[n + 3] * a0 * ma1 + out[n + 6] * a0 * a1 + out[n + 9] * ma0 * a1) * ma2 + (out[n + 12] * ma0 * ma1 + out[n + 15] * a0 * ma1 + out[n + 18] * a0 * a1 + out[n + 21] * ma0 * a1) * a2


    }

    inline fun clamp(l: Int, u: Int, x: Int): Int {
        return if (x < l) l else if (x > u) u else x
    }
}