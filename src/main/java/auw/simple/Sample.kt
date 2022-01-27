package auw.simple

import auw.*
import auw.functional.memoize
import auw.signal.Buffer
import auw.signal.Buffers
import field.app.ThreadSync2
import field.graphics.FLine
import field.utility.Vec2
import fieldbox.boxes.Box
import fieldbox.boxes.FLineDrawing
import fieldbox.execution.Execution
import java.io.File
import java.lang.IllegalArgumentException
import java.util.function.Supplier

class Sample(val filename: String, val o: Any? = null) : Soundable {

    init {
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

    private var b: Buffer

    @JvmField
    var rate = 1.0
    @JvmField
    var amplitude = 0.1

    @JvmField
    var phaseOffset = 0.0

    @JvmField
    var decay = 0.9


    val _rate = FInterpolator()
    val _amp = FInterpolator()
    val _phase0 = FInterpolator()

    init {
        if (!File(filename).exists()) throw IllegalArgumentException(" can't find file '$filename'")
        b = Buffers.buffer(filename)
    }

    val internalBuffer = BoxTools.stack.get().allocate()


    override fun get(): FBuffer {
        _rate.next(rate)
        _amp.next(amplitude)
        _phase0.next(phaseOffset)

        BufferTools.zero(internalBuffer)

        if (sounding())
            load(internalBuffer)

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

    var phase = 0.0

    fun load(output: FBuffer) {
        val d = output.length.toDouble()
        for (i in 0 until output.a.limit()) {
            val alpha = i / d

            output.a[i] = b.get(phase + _phase0.apply(alpha)) * _amp.apply(alpha)
            phase += _rate.apply(alpha)

        }
    }

//    fun analysis(): SoundAnalysis? {
//        try {
//            return SoundAnalysis(filename)
//        } catch (t: Throwable) {
//            return null
//        }
//    }

    fun time(): Double {
        return phase / IO.sampleRate
    }


//    val _readAnalysis: (String) -> FeatureReader = { name: String ->
//        FeatureReader({ 1000.0 * phase / IO.sampleRate }, featuresFor(name))
//    }.memoize()
//
//
//    fun readAnalysis(name: String): FeatureReader {
//        return _readAnalysis(name)
//    }
//
//    fun featuresFor(name: String): Map<String, List<VampCache.Feat>> {
//        return VampCache.featuresFor(filename, b.floats, name)
//    }


    override fun sounding(): Boolean {
        return !(_amp.isStatic() && Math.abs(_amp.now) < 1e-10) || phase > b.floats.capacity()
    }

    override fun toString() = "sample($filename)"


}