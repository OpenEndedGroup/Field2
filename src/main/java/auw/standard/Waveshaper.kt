package auw.standard

import auw.BoxTools
import auw.FBuffer
import auw.FBufferSource
import auw._FBuffer

class Waveshaper {

    var previousA: FloatArray = floatArrayOf(0f, 0f)

    fun apply(s: FBuffer, a: FloatArray): FBufferSource {
        return FBufferSource(this) {

            val output = BoxTools.stack.get().allocate()

            if (previousA == null) previousA = a

            for (i in 0 until output.length) {
                val z1 = a.getI(((a.size-1) * (s.a[i] / 2 + 0.5)) )
                val z2 = previousA!!.getI(((a.size-1) * (s.a[i] / 2 + 0.5)) )

                val alpha = i / output.length.toDouble()
                output.a.put(i, (z1 * (1 - alpha) + alpha * z2).toFloat())
            }

            previousA = a
            output
        }
    }
}