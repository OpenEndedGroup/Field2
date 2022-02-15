package auw

import java.nio.FloatBuffer
import java.nio.ShortBuffer

object BufferTools {


    fun add(a: FloatBuffer, b: FloatBuffer, out: FloatBuffer) {
        assert(a.limit() == b.limit())
        assert(a.limit() == out.limit())

        val i = a.limit()
        for (q in 0 until i) {
            out[q] = a[q] + b[q]
        }
    }

    fun add(a: FBuffer, b: FBuffer, out: FBuffer) {
        add(a.a, b.a, out.a)
    }

    fun add(a: FloatBuffer, b: Float, out: FloatBuffer) {
        assert(a.limit() == out.limit())

        val i = a.limit()
        for (q in 0 until i) {
            out[q] = a[q] + b
        }
    }

    fun add(a: FBuffer, b: Float, out: FBuffer) {
        add(a.a, b, out.a)

    }

    fun sub(a: FloatBuffer, b: FloatBuffer, out: FloatBuffer) {
        assert(a.limit() == b.limit())
        assert(a.limit() == out.limit())

        val i = a.limit()
        for (q in 0 until i) {
            out[q] = a[q] - b[q]
        }
    }

    fun sub(a: FBuffer, b: FBuffer, out: FBuffer) {
        sub(a.a, b.a, out.a)
    }

    fun sub(a: FloatBuffer, b: Float, out: FloatBuffer) {
        assert(a.limit() == out.limit())

        val i = a.limit()
        for (q in 0 until i) {
            out[q] = a[q] - b
        }
    }

    fun sub(a: FBuffer, b: Float, out: FBuffer) {
        sub(a.a, b, out.a)

    }

    fun sub(a: Float, b: FloatBuffer, out: FloatBuffer) {
        assert(b.limit() == out.limit())

        val i = b.limit()
        for (q in 0 until i) {
            out[q] = a - b[q]
        }
    }

    fun sub(a: Float, b: FBuffer, out: FBuffer) {
        sub(a, b.a, out.a)

    }

    fun div(a: FloatBuffer, b: Float, out: FloatBuffer) {
        assert(a.limit() == out.limit())

        val i = a.limit()
        for (q in 0 until i) {
            out[q] = a[q] / b
        }
    }

    fun div(a: FBuffer, b: Float, out: FBuffer) {
        div(a.a, b, out.a)
    }


    fun div(a: Float, b: FloatBuffer, out: FloatBuffer) {
        assert(b.limit() == out.limit())

        val i = b.limit()
        for (q in 0 until i) {
            out[q] = a / b[q]
        }
    }

    fun div(a: Float, b: FBuffer, out: FBuffer) {
        div(a, b.a, out.a)
    }

    fun div(a: FloatBuffer, b: FloatBuffer, out: FloatBuffer) {
        assert(a.limit() == out.limit())

        val i = a.limit()
        for (q in 0 until i) {
            out[q] = a[q] / b[q]
        }
    }

    fun div(a: FBuffer, b: FBuffer, out: FBuffer) {
        div(a.a, b.a, out.a)
    }

    fun mul(a: FloatBuffer, b: FloatBuffer, out: FloatBuffer) {
        assert(a.limit() == out.limit())

        val i = a.limit()
        for (q in 0 until i) {
            out[q] = a[q] * b[q]
        }
    }

    fun mul(a: FBuffer, b: FBuffer, out: FBuffer) {
        mul(a.a, b.a, out.a)
    }

    fun mul(a: FloatBuffer, b: Float, out: FloatBuffer) {
        assert(a.limit() == out.limit())

        val i = a.limit()
        for (q in 0 until i) {
            out[q] = a[q] * b
        }
    }

    fun mul(a: FBuffer, b: Float, out: FBuffer) {
        mul(a.a, b, out.a)
    }


    fun convert(a: FloatBuffer, b: ShortBuffer) {

        assert(a.limit() == b.limit())
        for (q in 0 until a.limit()) {

            b.put(
                q, Math.min(
                    java.lang.Short.MAX_VALUE.toFloat() - 1,
                    Math.max(java.lang.Short.MIN_VALUE.toFloat() + 1, java.lang.Short.MAX_VALUE * a.get(q))
                ).toInt().toShort()
            )

        }

    }

    fun convertWithStats(a: FloatBuffer, b: ShortBuffer): List<Float> {
        assert(a.limit() == b.limit())

        var min = Float.MAX_VALUE
        var max= Float.MIN_VALUE
        var average = 0f
        var rmsAverage = 0f

        for (q in 0 until a.limit()) {

            val z = a.get(q)

            min = Math.min(z, min)
            max = Math.max(z, max)
            average += z
            rmsAverage += z*z

            b.put(
                q, Math.min(
                    java.lang.Short.MAX_VALUE.toFloat() - 1,
                    Math.max(java.lang.Short.MIN_VALUE.toFloat() + 1, java.lang.Short.MAX_VALUE * z)
                ).toInt().toShort()
            )

        }

        average /= a.limit()
        rmsAverage /= a.limit()
        rmsAverage = Math.sqrt(rmsAverage.toDouble()).toFloat()
        return listOf(min, average, rmsAverage, max)
    }


    fun convert(a: FBuffer, b: ShortBuffer) {

        convert(a.a, b)

    }

    fun zero(output: FloatBuffer) {
        for (x in 0 until output.limit()) {
            output.put(x, 0f)
        }
    }

    fun zero(output: FBuffer): FBuffer {
        zero(output.a)
        return output
    }

    fun wet(a1: FBuffer, a2: FBuffer, wet: FBuffer, out: FBuffer) {
        for (x in 0 until wet.length) {
            val z = wet.a[x]
            out.a[x] = a1.a[x]*(1-z) + z*a2.a[x]
        }
    }

    fun copy(from: FBuffer, to: FBuffer) {
        from.a.clear()
        to.a.clear()
        to.a.put(from.a)
        from.a.clear()
        to.a.clear()
    }


}

inline operator fun FloatBuffer.set(x: Int, value: Float) {
    this.put(x, value)
}

inline operator fun FloatBuffer.set(i: Int, value: Double) {
    this.put(i, value.toFloat())
}
