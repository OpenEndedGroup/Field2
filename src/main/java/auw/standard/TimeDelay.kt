package auw.standard

import auw.BoxTools
import auw.BufferTools.zero
import auw.FBuffer
import auw.IO
import auw.signal.Fft

class TimeDelay
{
    val fft = Fft(IO.vectorSize)

    fun apply(a: FBuffer, b: FBuffer): FBuffer {


        val s1 = BoxTools.stack.get().allocate()
        val s2 = BoxTools.stack.get().allocate()

        val c = BoxTools.stack.get().allocate()
        val c2 = BoxTools.stack.get().allocate()

        val out = BoxTools.stack.get().allocate()

        zero(s2)

        c.copyFrom(a)
        fft.transform(c.a, s1.a)

        zero(s2)

        c2.copyFrom(b)
        fft.transform(c2.a, s2.a)

        for(q in 0 until IO.vectorSize)
        {
            out.a.put(q, angleSubtract(Math.atan2(s2.a[q].toDouble(), c2.a[q].toDouble()), Math.atan2(s1.a[q].toDouble(), c.a[q].toDouble())))
        }
        return out


    }

    private fun angleSubtract(a: Double, b: Double): Float {
            return Math.atan2(Math.sin(a-b), Math.cos(a-b)).toFloat()
    }
}