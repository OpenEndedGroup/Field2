package auw.standard

import auw.*

class Distort {

    val _gain = FInterpolator()

    fun apply(signal: _FBuffer, gain: Double): _FBuffer {

        return FBufferSource(this) {

            val s = signal.get()
            _gain.next(gain)

            val out = BoxTools.stack.get().allocate()


            for (x in 0 until out.length) {
                out.a[x] = Math.signum(s.a[x]) *
                        (1 - Math.exp(-Math.abs(s.a[x]) * _gain.apply(x / out.length.toDouble())))
            }

            out
        }

    }

    fun apply(signal: _FBuffer, gain: _FBuffer): _FBuffer {

        return FBufferSource(this) {

            val s = signal.get()
            val gg = gain.get()
            _gain.next(gg.a[gg.a.capacity() - 1].toDouble())

            val out = BoxTools.stack.get().allocate()


            for (x in 0 until out.length) {
                out.a[x] = Math.signum(s.a[x]) *
                        (1 - Math.exp((-Math.abs(s.a[x]) * gg.a[x]).toDouble()))
            }

            out
        }

    }


}