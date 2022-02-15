package auw.standard

import auw.*
import auw.signal.Buffer
import auw.signal.Buffers

class GrainulatorDelay {
    var sourcefn: String? = null

    private var source: Delay = Delay()

    inner class Grain(var sample: Float, var speed: Float, var volume: Float, var window: Float, var duration: Float) {
        fun compute(i: Int): Float {

            var at = age + i
            var w = win(at / duration, window)
            return (source!!.f.getI((sample + at * speed).toDouble()) * w).toFloat() * volume
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

    fun apply(fn: _FBuffer): GrainulatorDelay {

        source.apply(fn, 0f)

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
        
        grains.add(
            Grain(
                source.writeHead - IO.vectorSize - Math.min(
                    source.MAX - 10f - IO.vectorSize,
                    start * IO.sampleRate
                ), speed, volume, window, duration * IO.sampleRate
            )
        )

        return true
    }

    private fun clear() {

        grains.clear()
    }
}

fun FloatArray.getI(sample: Double): Double {
    try {
        val a = Math.max(0.0, Math.min(this.size - 1.0, sample))
        val left = a.toInt()
        val right = Math.min(this.size - 1, left + 1)

        val alpha = a - left

        return this[left] * (1 - alpha) + alpha * this[right]
    }
    catch(t : Throwable)
    {
        return 0.0
    }

}
