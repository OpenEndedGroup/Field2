package auw.standard

import auw.*
import auw.functional.Functions
import java.lang.IllegalArgumentException

open class Sin {
    var freq = FInterpolator()
    var phaseO = FInterpolator()
    var amp = FInterpolator()

    // state
    var phase = 0.0

    init {
        print(" -- new sin -- ${DynamicScope.at.get().name}")
    }

    @JvmOverloads
    open fun apply(frequency: Float, amplitude: Float = 1f, phase: Float = 0f): _FBuffer {

        return FBufferSource(this) {
            val output = BoxTools.stack.get().allocate()
            freq.next(frequency.toDouble() * conversion)
            amp.next(amplitude.toDouble())
            phaseO.next(phase.toDouble())

            _sin(output)
            output
        }
    }

    private val conversion = Math.PI * 2 / IO.sampleRate

    @JvmOverloads
    open fun apply(frequency: _FBuffer, amplitude: Float = 1f, phase: Float = 0f): _FBuffer {

        val fr = frequency.get()

        return FBufferSource(this) {
            val output = BoxTools.stack.get().allocate()

            freq.next(fr.a.get(fr.a.capacity() - 1) * conversion)

            amp.next(amplitude.toDouble())
            phaseO.next(phase.toDouble())

            _sin2(fr, output)
            output
        }
    }


    open fun _sin(output: FBuffer) {
        val d = output.length.toDouble()
        for (i in 0 until output.a.limit()) {
            val alpha = i / d

            output.a[i] = sin(phase + phaseO.apply(alpha)) * amp.apply(alpha)
            phase += freq.apply(alpha)
        }
    }

    open fun sin(d: Double): Double {
        return Math.sin(d)
    }

    open fun _sin2(frequency: FBuffer, output: FBuffer) {
        val d = output.length.toDouble()
        for (i in 0 until output.a.limit()) {
            val alpha = i / d

            output.a[i] = sin(phase + phaseO.apply(alpha)) * amp.apply(alpha)
            phase += frequency.a[i] * conversion
        }
    }

}

