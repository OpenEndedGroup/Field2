package auw.standard

import auw.BoxTools
import auw.FBuffer
import auw.IO
import auw.set
import auw.signal.Fft

class Convolve {
    val fft = Fft(IO.vectorSize)


    fun apply(a: FBuffer, b: FBuffer): FBuffer {
        val out = BoxTools.stack.get().allocate()

        val s1 = BoxTools.stack.get().allocate()
        val s2 = BoxTools.stack.get().allocate()
        val s3 = BoxTools.stack.get().allocate()

        val aa = BoxTools.stack.get().allocate()
        val bb = BoxTools.stack.get().allocate()

        for (i in 0 until a.a.capacity()) {
            val v = i / a.a.capacity().toDouble()

            aa.a[i] = a.a[i] * (1e-2+v * (1 - v) * 4)
            bb.a[i] = b.a[i] * (1e-2+v * (1 - v) * 4)
        }

        fft.convolve(aa.a, bb.a, out.a, s1.a, s2.a, s3.a)

        for (i in 0 until a.a.capacity()) {
            val v = i / a.a.capacity().toDouble()

            out.a[i] = out.a[i] // (1e-2+v * (1 - v) * 4)

        }

        return out
    }
}