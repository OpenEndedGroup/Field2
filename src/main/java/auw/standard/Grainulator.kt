package auw.standard

import auw.BoxTools
import auw.FBuffer
import auw.IO
import auw.set
import auw.signal.Buffer
import auw.signal.Buffers

class Grainulator {
    var sourcefn: String? = null
    private lateinit var source: Buffer

    inner class Grain(var sample: Float, var speed: Float, var volume: Float, var window: Float, var duration: Float) {
        fun compute(i: Int): Float {

            var at = age + i
            var w = win(at / duration, window)
            return (source!!.get((sample + at * speed).toDouble()) * w).toFloat() * volume
        }

        private fun win(m: Float, window: Float): Float {

            var z = Math.max(0f, Math.min(1f, m))
            z *= (1 - z) * 4
            z = Math.pow(z.toDouble(), window.toDouble()).toFloat()
            return z

        }

        fun advance(): Boolean {
            age += IO.vectorSize
            return (age <= duration)
        }

        var age = 0
    }

    var grains = ArrayList<Grain>()

    fun apply(fn: String): Grainulator {
        if (sourcefn != null && !sourcefn.equals(fn))
            clear()

        source = Buffers.buffer(fn)

        return this
    }

    fun compute(): FBuffer {
        var a = BoxTools.stack.get().allocate()

        for (g in grains) {
            for (i in 0 until a.length) {

                a.a[i] += g.compute(i)

            }
        }

        grains.removeIf { !it.advance() }

        return a
    }

    @JvmOverloads
    fun addGrain(
        start: Float,
        duration: Float = 0.2f,
        volume: Float = 0.5f,
        speed: Float = 1.0f,
        window: Float = 0.5f,
        max: Int = 10
    ): Boolean {

        if (grains.size > max) return false
        grains.add(Grain(start * IO.sampleRate, speed, volume, window, duration * IO.sampleRate))

        return true
    }

    private fun clear() {

        grains.clear()
    }
}