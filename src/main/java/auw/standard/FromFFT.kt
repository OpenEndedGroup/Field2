package auw.standard

import auw.*
import auw.BufferTools.zero
import auw.signal.Fft
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class FromFFT {
    val gather = ByteBuffer.allocate(IO.vectorSize * 2 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()

    val reals = BoxTools.stack2.get().allocate()
    val imgs = BoxTools.stack2.get().allocate()

    val fft = Fft(IO.vectorSize * 2)


    fun apply(a: _FBuffer, a2: _FBuffer): CBuffer {

        reals.a.put(a.get().a)
        a.get().a.clear()

        imgs.a.put(a2.get().a)
        a2.get().a.clear()

        fft.transform(reals.a, imgs.a)

        return CBuffer(reals, imgs)
    }
}