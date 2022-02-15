package auw.standard

import auw.BoxTools
import auw.FBuffer
import auw._FBuffer
import auw.set

class ASDR(val attack: Float, val sustain: Float, val sustain_level: Float, val decay: Float, val release: Float) {
    val bp = BreakpointGraph(
        floatArrayOf(0f, attack, attack + sustain, attack + sustain + decay, attack + sustain + decay + release),
        floatArrayOf(0f, 1f, sustain_level, sustain_level, 0f),
        floatArrayOf(1f, 0.2f, 1f, 0.5f, 1f)
    )


    fun apply(t: Float): Float {
        return bp.get(t)
    }


    fun apply(t: FBuffer): FBuffer {

        var out = BoxTools.stack.get().allocate()
        for (x in 0 until out.length) {
            out.a[x] = bp.get(t.a[x])
        }

        return out
    }

    fun apply(t: _FBuffer): FBuffer {

        var out = BoxTools.stack.get().allocate()
        var g = t.get()

        for (x in 0 until out.length) {
            out.a[x] = bp.get(g.a[x])
        }

        return out
    }


}