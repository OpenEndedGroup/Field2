package auw.standard

import auw.*

class Delay {

    val MAX = IO.sampleRate * 3

    val f = FloatArray(MAX)

    var writeHead = 0

    val _delay = FInterpolator()
    val _feedback = FInterpolator().next(0.0).next(0.0)

    fun apply(i: _FBuffer, delay: Float): _FBuffer {
        return apply(i, delay, 0f)
    }

    fun apply(i: _FBuffer, delay: Float, feedback: Float): _FBuffer {

        val qq = if (delay > MAX - 1) (MAX - 1).toDouble() else delay.toDouble()
        _delay.next(qq)
        _feedback.next(feedback.toDouble())

        return FBufferSource(this) {

            val ii = i.get()

            write(ii)

            val readHeadO = (writeHead - IO.vectorSize)

            val output = BoxTools.stack.get().allocate()

            for (read in 0 until IO.vectorSize) {
                val o = readHeadO + read - _delay.apply(read / IO.vectorSize.toDouble())
                output.a[read] = interpolate(o.toFloat())
            }

            output
        }
    }


    fun apply(i: _FBuffer, delay: _FBuffer, feedback: Float): _FBuffer {

        val dd = delay.get()
        _delay.next(dd.a[dd.a.capacity() - 1].toDouble())
        _feedback.next(feedback.toDouble())

        return FBufferSource(this) {

            val ii = i.get()

            write(ii)

            val readHeadO = (writeHead - IO.vectorSize)

            val output = BoxTools.stack.get().allocate()


            for (read in 0 until IO.vectorSize) {
                val o = readHeadO + read - dd.a[read]
                output.a[read] = interpolate(o)
            }

            output
        }
    }

    private fun write(ii: FBuffer) {
        for (read in 0 until IO.vectorSize) {

            val z = _feedback.apply(read / IO.vectorSize.toDouble())

            f[writeHead] = (f[writeHead] * z + (1 - z) * ii.a[read]).toFloat()
            writeHead = (writeHead + 1) % MAX
        }
    }


    private fun interpolate(o: Float): Float {
        val oo = (if (o < 0)
            o + 10*MAX
        else
            o) % MAX

        val left = f[oo.toInt()]
        val right = f[(oo.toInt() + 1) % MAX]

        val alpha = oo - oo.toInt()

        return left * (1 - alpha) + alpha * right
    }
}