package auw.standard

import auw.*


class FreeVerb(val o: Int = 0) {

    inner class Comb(val size: Int, private val damp1: Float, private val damp2: Float, private val feedback: Float) {
        val buffer = FloatArray(size)
        private var index = 0
        var f = 0f

        fun process_mix(a: FBuffer, b: FBuffer) {
            for (i in 0 until a.length) {
                val output = buffer[index]
                f = (output * damp2) + (f * damp1)
                buffer[index] = a.a[i] + f * feedback
                b.a[i] += output
                index = (index + 1) % size
            }
        }
    }

    inner class Allpass(val size: Int, private val feedback: Float) {
        val buffer = FloatArray(size)
        private var index = 0
        var f = 0f

        fun process_replace(a: FBuffer, b: FBuffer) {
            for (i in 0 until a.length) {
                val output = buffer[index]
                buffer[index] = a.a[i] + output * feedback
                b.a[i] = -a.a[i] + output
                index = (index + 1) % size
            }
        }
    }

    var all: List<Allpass>
    var comb: List<Comb>

    init {
        val combtuning =
            listOf((1116 + o), (1188 + o), (1277 + o), (1356 + o), (1422 + o), (1491 + o), (1557 + o), (1617 + o))
        val alltuning = listOf((556 + o), (441 + o), (341 + o), (225 + o))

        all = alltuning.map { Allpass(it, 0.5f) }
        comb = combtuning.map { Comb(it, 0.5f, 0.5f, 0.5f) }


    }

    fun process(a: FBuffer): FBuffer {
        val o = BoxTools.stack.get().allocate()
        comb.forEach {
            it.process_mix(a, o)
        }

        all.forEach {
            it.process_replace(o, o)
        }

        return o
    }

    val _wet = Line()

    fun apply(a: FBuffer, wet: Float): _FBuffer {
        return FBufferSource(this) {

            val l = _wet.apply(wet).get()

            val o = process(a)

            BufferTools.wet(a, o, l, o)

            o
        }

    }

}