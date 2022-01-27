package auw.standard

import auw.*

class Pulse {

    var freq = FInterpolator()
    var phaseO = FInterpolator()
    var amp = FInterpolator()
    var frac = FInterpolator()

    // state
    var phase = 0.0

    init {
        print(" -- new sin -- ${DynamicScope.at.get().name}")
    }

    @JvmOverloads
    open fun apply(frequency: Float, amplitude: Float = 1f, fraction: Float = 0.5f, phase: Float = 0f): _FBuffer {

        return FBufferSource(this) {
            val output = BoxTools.stack.get().allocate()
            freq.next(frequency.toDouble() * conversion)
            amp.next(amplitude.toDouble())
            frac.next(fraction.toDouble())
            phaseO.next(phase.toDouble())

            _sin(output)
            output
        }
    }

    private val conversion = Math.PI * 2 / IO.sampleRate


    open fun _sin(output: FBuffer) {
        val d = output.length.toDouble()
        for (i in 0 until output.a.limit()) {
            val alpha = i / d

            output.a[i] = sin(phase + phaseO.apply(alpha), frac.apply(alpha)) * amp.apply(alpha)
            phase += freq.apply(alpha)
        }
    }

    open fun sin(d: Double, f: Double): Double {
        val d2 = d%(2*Math.PI)
        return if (d2 / (2 * Math.PI) < f) 1.0 else -1.0
    }
}