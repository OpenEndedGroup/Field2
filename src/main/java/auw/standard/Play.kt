package auw.standard

import auw.*
import auw.signal.Buffers
import java.lang.IllegalArgumentException
import java.nio.FloatBuffer

class Play {
    var at = 0.0

    var speed = FInterpolator()
    var offset = FInterpolator()

    private lateinit var fn: String

    @JvmOverloads
    fun apply(fn: String, speed: Double = 1.0, offset: Double = 0.0, looping: Boolean = true): _FBuffer {
        this.fn = fn
        return apply(Buffers.buffer(fn).floats, speed, offset, looping)
    }

    @JvmOverloads
    fun apply(input: FloatBuffer, speed: Double = 1.0, offset: Double = 0.0, looping: Boolean = true): _FBuffer {
        return FBufferSource(this) {

            fun FloatBuffer.access(index: Int): Float {
                return if (looping)
                    this.get(Math.max(0, index % this.limit()))
                else
                    this.get(Math.max(0, Math.min(this.limit() - 1, index)))
            }

            var speed = this.speed.next(Math.max(0.0, speed))
            var offset = this.offset.next(offset)

            val output = BoxTools.stack.get().allocate()

            for (q in 0 until output.length) {

                val lalpha = q / output.length.toDouble()
                val lat = at + offset.apply(lalpha)

                val x0 = (lat - 2)
                val xi0 = x0.toInt()
                val y0 = input.access(xi0)
                val x1 = (lat - 1)
                val xi1 = x1.toInt()
                val y1 = input.access(xi1)
                val x2 = (lat)
                val xi2 = x2.toInt()
                val y2 = input.access(xi2)
                val x3 = (lat + 1)
                val xi3 = x3.toInt()
                val y3 = input.access(xi3)

                val alpha = (x1 - xi1).toFloat()

                output.a.put(q, interpolate(y0, y1, y2, y3, alpha))

                at += speed.apply(lalpha)

            }


            output
        }
    }

//    var cachedSoundAnalysis: SoundAnalysis? = null
//
//    fun analysis(): SoundAnalysis? {
//        if (fn == null) throw IllegalArgumentException(" can't figure out the filename of this Play unit")
//        if (cachedSoundAnalysis != null) return cachedSoundAnalysis
//
//        try {
//            cachedSoundAnalysis = SoundAnalysis(fn)
//            return cachedSoundAnalysis
//        } catch (t: Throwable) {
//            throw IllegalArgumentException(" can't load analysis files for $fn")
//        }
//    }

    fun time(): Double {
        return at / IO.sampleRate
    }


    private fun interpolate(y0: Float, y1: Float, y2: Float, y3: Float, alpha: Float): Float {


        val oma = 1 - alpha
        val oma2 = oma * oma
        val oma3 = oma2 * oma
        val alpha2 = alpha * alpha
        val alpha3 = alpha2 * alpha

        val a =
            y1 * oma3 + 3 * (y1 + (y2 - y0) / 6) * alpha * oma2 + 3 * (y2 - (y3 - y2) / 6) * alpha2 * oma + y2 * alpha3

        return a
    }

}

