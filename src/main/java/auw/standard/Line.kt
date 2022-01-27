package auw.standard

import auw.*
import auw.functional.Functions

open class Line : Functions.F1, Functions.F2 {
    var target = FInterpolator()
    var power = FInterpolator()


    override fun apply(to: Float): FBuffer {

        val output = BoxTools.stack.get().allocate()

        target.next(to.toDouble())

        _line(output)
        return output
    }

    fun apply(to: Boolean): FBuffer {

        val output = BoxTools.stack.get().allocate()

        target.next(if (to) 1.0 else 0.0)

        _line(output)
        return output
    }

    fun _line(output: FBuffer) {
        val d = output.a.limit().toDouble()
        for (i in 0 until output.a.limit()) {
            val alpha = i / d

            output.a[i] = target.apply(alpha)
        }
    }

    override fun apply(to: Float, power: Float): _FBuffer {
        return FBufferSource(this) {
            val output = BoxTools.stack.get().allocate()

            target.next(to.toDouble())
            this.power.next(power.toDouble())

            _line2(output)
            output
        }
    }

    fun _line2(output: FBuffer) {
        val d = output.a.limit().toDouble()
        for (i in 0 until output.a.limit()) {
            val alpha = i / d

            output.a[i] = target.apply(Math.pow(alpha, power.apply(alpha)))
        }
    }

}