package auw.standard

import auw.*
import auw.functional.Functions

class AutoGain {

    val g = Line()

    @JvmOverloads
    fun apply(input: _FBuffer, decay : Float=0.9f, target : Float=0.5f): _FBuffer {

        var gain = 1f

        return FBufferSource(this) {

            var i = input.get()

            var mx = maxAbs(i)

            val targetGain = target/(1e-3+mx)

            if (targetGain<gain)
                gain = targetGain.toFloat()
            else
                gain = (gain*decay+(1-decay)*targetGain).toFloat()

            val out = BoxTools.stack.get().allocate()


            BufferTools.mul(i, g.apply(gain).get(), out)

            out
        }
    }

    private fun maxAbs(i: FBuffer): Float {

        var m = 0f
        for (index in 0 until i.length) {
            val z = i.a[index]
            if (z > m)
                m = z
            if (-z > m)
                m = -z
        }

        return m
    }
}