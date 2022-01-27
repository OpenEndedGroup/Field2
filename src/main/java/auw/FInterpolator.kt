package auw

import auw.functional.FFunction


class FInterpolator : FFunction {

    @JvmField
    var was: Double = 0.0
    @JvmField
    var now: Double = 0.0

    var first = true

    var interpolator: (Double) -> Double = { was + (now-was)*it }

    fun next(n: Double): FInterpolator {
        if (first)
        {
            first = false
            was = n
            now = n
            return this
        }
        was = now
        now = n
        return this
    }

    fun isStatic(): Boolean {
        return Math.abs(was-now)<1e-10
    }

    override fun apply(t: Double): Double {
        return interpolator(t)
    }

}


