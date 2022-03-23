package auw.standard

import auw.*
import auw.BufferTools.zero
import auw.signal.Fft
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class ToFFT {
    val gather = ByteBuffer.allocate(IO.vectorSize * 2 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()

    val reals = BoxTools.stack2.get().allocate()
    val imgs = BoxTools.stack2.get().allocate()

    val fft = Fft(IO.vectorSize * 2)


    fun apply(a: _FBuffer): CBuffer {

        reals.a.clear()
        reals.a.put(a.get().a)
        a.get().a.clear()

        zero(imgs)

        fft.transform(reals.a, imgs.a)
        
        return CBuffer(reals, imgs)
    }
}