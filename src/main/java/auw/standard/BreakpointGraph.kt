package auw.standard

import field.utility.Quad

class BreakpointGraph(val x: FloatArray, val y: FloatArray, val p: FloatArray) {

    init {
        if (x.size != y.size) throw IllegalArgumentException(" size missmatch between x ${x.size} and y ${y.size}")
        if (x.size != p.size) throw IllegalArgumentException(" size missmatch between x ${x.size} and p ${p.size}")
    }

    var lastLeft = -1f
    var lastRight = -1f
    var lastLeftI = -1
    var lastRightI = -1

    fun get(t: Float): Float {
        return get(Math.max(x.first(), Math.min(x.last(), t)))
    }

    fun _get(t: Float): Float {
        if (t >= lastLeft && t <= lastRight) {

            var t2 = (t - lastLeft) / (lastRight - lastLeft + 1e-10)
            t2 = Math.pow(t2, p[lastLeftI].toDouble())

            return (y[lastLeftI] * (1 - t2) + t2 * y[lastRightI]).toFloat()

        } else {
            val (a, b, ai, bi) = bracket(t)


            var t2 = (t - lastLeft) / (lastRight - lastLeft + 1e-10)
            t2 = Math.pow(t2, p[lastLeftI].toDouble())

            return (y[lastLeftI] * (1 - t2) + t2 * y[lastRightI]).toFloat()


            lastLeft = a
            lastRight = b
            lastLeftI = ai
            lastRightI = bi
        }
    }

    fun bracket(f: Float): Quad<Float, Float, Int, Int> {
        for (i in 0 until x.size) {
            if (f < x[i]) return Quad(x[i - 1], x[i], i - 1, i)
        }
        return Quad(x[x.size - 2], x[x.size - 1], x.size - 2, x.size - 1)
    }
}

private operator fun <A, B, C, D> Quad<A, B, C, D>.component1(): A {
    return this.first
}

private operator fun <A, B, C, D> Quad<A, B, C, D>.component2(): B {
    return this.second
}

private operator fun <A, B, C, D> Quad<A, B, C, D>.component3(): C {
    return this.third
}

private operator fun <A, B, C, D> Quad<A, B, C, D>.component4(): D {
    return this.fourth
}
