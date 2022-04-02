package trace.sound

import auw.FBuffer
import auw.signal.Fft
import java.nio.FloatBuffer

class SimpleFFT {

    companion object {
        val cache = mutableMapOf<Int, Fft>()
    }

    fun process(fb: FBuffer): FloatArray {

        val f = fb.a.array()

        val fft = cache.computeIfAbsent(f.size) { Fft(f.size) }

        val r = FloatBuffer.allocate(f.size)
        val i = FloatBuffer.allocate(f.size)
        r.put(f)
        r.rewind()

        fft.transform(r, i)

        val m = FloatArray(f.size) { index ->
            r[index] * r[index] + i[index] * i[index]
        }

        return m;
    }

    fun process(fb: FBuffer, reduceTo: Int): FloatArray {

        val f = fb.a.array()

        val fft = cache.computeIfAbsent(f.size) { Fft(f.size) }

        val r = FloatBuffer.allocate(f.size)
        val i = FloatBuffer.allocate(f.size)
        r.put(f)
        r.rewind()

        fft.transform(r, i)

        val m = FloatArray((f.size/4) / reduceTo) { index ->
            val ra = (0 until reduceTo)
            ra.sumOf { it -> r[index * reduceTo + it] * r[index * reduceTo + it] + i[index * reduceTo + it] * i[index * reduceTo + it].toDouble() }
                .toFloat()
        }

        return m;
    }

    fun process(f: FloatArray): FloatArray {

        val fft = cache.computeIfAbsent(f.size) { Fft(f.size) }

        val r = FloatBuffer.allocate(f.size)
        val i = FloatBuffer.allocate(f.size)
        r.put(f)
        r.rewind()

        fft.transform(r, i)

        val m = FloatArray(f.size) { index ->
            r[index] * r[index] + i[index] * i[index]
        }

        return m;
    }

    fun process(f: FloatArray, reduceTo: Int): FloatArray {

        val fft = cache.computeIfAbsent(f.size) { Fft(f.size) }

        val r = FloatBuffer.allocate(f.size)
        val i = FloatBuffer.allocate(f.size)
        r.put(f)
        r.rewind()

        fft.transform(r, i)

        val m = FloatArray((f.size/4) / reduceTo) { index ->
            val ra = (0 until reduceTo)
            ra.sumOf { it -> r[index * reduceTo + it] * r[index * reduceTo + it] + i[index * reduceTo + it] * i[index * reduceTo + it].toDouble() }
                .toFloat()
        }

        return m;
    }

}