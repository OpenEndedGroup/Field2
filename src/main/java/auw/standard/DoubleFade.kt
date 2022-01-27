package auw.standard

import auw.*
import java.nio.FloatBuffer

class DoubleFade() {

    val prev = FBuffer(FloatBuffer.allocate(IO.vectorSize), BoxTools.stack.get())
    val half = FBuffer(FloatBuffer.allocate(IO.vectorSize), BoxTools.stack.get())
    val lastOut = FBuffer(FloatBuffer.allocate(IO.vectorSize), BoxTools.stack.get())

    fun apply(v: (FBuffer) -> FBuffer, input: FBuffer): FBuffer {

        for (i in 0 until IO.vectorSize / 2) {
            half.a[i] = prev.a[i + IO.vectorSize / 2]
        }
        for (i in 0 until IO.vectorSize / 2) {
            half.a[i + IO.vectorSize / 2] = input.a[i]
        }

        val out1 = DynamicScope.push("A").use {
            v(half)
        }
        val out2 = DynamicScope.push("B").use {
            v(input)
        }

        val out = BoxTools.stack.get().allocate()

        for (i in 0 until IO.vectorSize / 2) {
            val a = i / (IO.vectorSize / 2.0)
            out.a[i] = lastOut.a[i + IO.vectorSize / 2] * (1 - a) + a * out1.a[i]
        }

        for (i in 0 until IO.vectorSize / 2) {
            val a = i / (IO.vectorSize / 2.0)
            out.a[i+IO.vectorSize/2] = (1 - a) * out1.a[i + IO.vectorSize / 2] + out2.a[i] * a
        }

        BufferTools.copy(input, prev)
        BufferTools.copy(out2, lastOut)

        return out

    }

}