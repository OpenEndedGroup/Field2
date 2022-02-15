package auw.simple

import auw.*
import auw.standard.Sin
import field.app.ThreadSync2
import field.graphics.FLine
import field.utility.Vec2
import field.utility.plusAssign
import fieldbox.boxes.Box
import fieldbox.boxes.FLineDrawing
import fieldbox.execution.Execution
import java.util.function.Supplier

class Osc : Soundable {

    @JvmOverloads
    constructor(o: Any? = null) {
        when (o) {
            null -> {
                if (Oscs.default_oscs?.known != null) {
                    synchronized(Oscs.default_oscs!!.known) {
                        Oscs.default_oscs?.known?.add(this)
                        Oscs.default_oscs?.retained?.add(this)
                    }
                }
            }
            is Oscs -> {
                synchronized(o.known) {
                    o.known?.add(this)
                    o.retained?.add(this)
                }
            }
            is Submixer -> {
                synchronized(o.oscs.known) {
                    o.oscs.known.add(this)
                    o.oscs.known.add(this)
                }
            }
        }
    }


    @JvmField
    var frequency = 440.0
    @JvmField
    var amplitude = 0.1
    @JvmField
    var phaseOffset = 0.0

    @JvmField
    var decay = 0.9

    val internalBuffer = BoxTools.stack.get().allocate()

    var phase = 0.0

    val freq = FInterpolator()
    val phase0 = FInterpolator()
    val amp = FInterpolator()


    @JvmField
    var sin = 1.0
    private val _sin = FInterpolator()

    @JvmField
    var saw = 0.0
    private val _saw = FInterpolator()

    @JvmField
    var tri = 0.0
    private val _tri = FInterpolator()

    @JvmField
    var square = 0.0
    private val _square = FInterpolator()

    @JvmField
    var white = 0.0
    private val _white = FInterpolator()


    override fun get(): FBuffer {
        freq.next(frequency / IO.sampleRate)
        amp.next(amplitude)
        phase0.next(phaseOffset)

        _sin.next(sin)
        _saw.next(saw)
        _tri.next(tri)
        _square.next(square)
        _white.next(white)

        BufferTools.zero(internalBuffer)

        if (sounding())
            _sin(internalBuffer)

        amplitude *= decay
        if (Math.abs(amplitude) < 1e-10) {
            amplitude = 0.0
        }

        return internalBuffer
    }

    @JvmOverloads
    fun scope(xscale: Double = 1.0, yscale: Double = 1.0): Supplier<FLine> {
        var f = Supplier<FLine> {
            val f = FLine()

            for (i in 0 until internalBuffer.length) {
                f.lineTo(i * xscale * 0.25, -yscale * internalBuffer.getSlot(i) * 50)
            }
            f
        }

        val q = Execution.context.get()
        if (q.size > 0) {
            val inside = q.peek()
            if (inside != null) {
                return FLineDrawing.boxOrigin(f, Vec2(1.1, 1.1), inside)
            }
        }

        val fibre = ThreadSync2.thisFibre.get()
        if (fibre != null) {
            when (val t = fibre.tag) {
                is Box -> {
                    return FLineDrawing.boxOrigin(f, Vec2(1.1, 1.1), t)
                }
            }
        }

        return f
    }


    override fun sounding(): Boolean {
        return !(amp.isStatic() && Math.abs(amp.now) < 1e-10)
    }

    fun OSC(a: Double, p0: Double): Double {
        var o = 0.0
        val p = p0 % 1

        var sin = _sin.apply(a)
        val saw = _saw.apply(a)
        val tri = _tri.apply(a)
        val square = _square.apply(a)
        val white = _white.apply(a)

        val tot = sin + saw + tri + square + white
        if (tot == 0.0) sin = 1.0

        if (sin > 0)
            o += sin * Math.sin(2 * Math.PI * p)
        if (saw > 0)
            o += saw * ((p * 2) % 2 - 1.0)
        if (tri > 0)
            o += tri * ((if (p < 0.5) p * 2 else 2 - p * 2) * 2 - 1.0)
        if (square > 0)
            o += square * (if (p < 0.5) 1 else -1)
        if (white > 0)
            o += white * (Math.random() - 0.5)

        return o / tot
    }

    fun _sin(output: FBuffer) {
        val d = output.length.toDouble()
        for (i in 0 until output.a.limit()) {
            val alpha = i / d

            output.a[i] = OSC(alpha, phase + phase0.apply(alpha)) * amp.apply(alpha)
            phase += freq.apply(alpha)

        }
        phase = phase % (1.0)
    }


}