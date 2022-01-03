package trace.random

import field.graphics.FLinesAndJavaShapes

class Random {
    var source: MersenneTwisterFast

    var forked = 0

    var a: Double = 0.0
    var b: Double = 0.0
    var bi = -4
    var c: Double = 0.0
    var d: Double = 0.0

    var T = 0.0

    constructor() {
        source = MersenneTwisterFast()
    }

    constructor(seed: String) {
        source = MersenneTwisterFast(seed.hashCode().toLong())
    }

    constructor(seed: String, num: Double) {
        source = MersenneTwisterFast(seed.hashCode().toLong().xor(num.hashCode().toLong()))
    }

    fun fork(): Random {
        val v = Random()

        val ns = source.mt!!.copyOf()
        ns[10] = forked++
        v.source = MersenneTwisterFast(ns)

        v.a = a
        v.b = b
        v.bi = bi
        v.c = c
        v.d = d
        v.T = T

        return v
    }

    fun nextDouble(): Double {
        while (bi < 0)
            nextDouble(1.0)

        bi = 0
        T = 0.0
        b = source.nextDouble()
        return b * 2 - 1
    }

    fun nextDouble(increment: Double): Double {

        val i = (if (increment > 4) increment % 4 else increment)/4.0
        T += i
        while (bi < Math.floor(T)) {
            a = b
            bi += 1
            b = c
            c = d
            d = source.nextDouble()
        }

        val alpha = T - (bi)

        val o = FLinesAndJavaShapes.evaluateCubicFrame(b, ((c - a) / 6 + b), (-(d - b) / 6 + c), c, alpha).x

        return o * 2 - 1
    }

    fun nextGaussian(): Double {
        while (bi < 0)
            nextDouble(1.0)

        bi = 0
        T = 0.0
        b = source.nextGaussian()
        return b
    }

    fun nextGaussian(increment: Double): Double {
        val i = if (increment > 4) increment % 4 else increment
        T += i
        while (bi < Math.floor(T)) {
            a = b
            bi += 1
            b = c
            c = d
            d = source.nextGaussian()
        }

        val alpha = T - (bi)

        val o = FLinesAndJavaShapes.evaluateCubicFrame(b, ((c - a) / 6 + b), (-(d - b) / 6 + c), c, alpha).x

        return o
    }

}