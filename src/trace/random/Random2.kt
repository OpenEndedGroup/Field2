package trace.random

class Random2 {
    val r = mutableListOf<Random>()
    val h = mutableListOf<Double>()
    val a = mutableListOf<Double>()

    constructor() {
        r.addAll(mutableListOf(Random(), Random(), Random(), Random()))
        h.addAll(mutableListOf(1.0, 2.0, 3.0, 4.0))
        a.addAll(mutableListOf(1.0, 1 / 2.0, 1 / 4.0, 1 / 8.0))
        r.forEach { it.nextDouble(Math.random()) }
    }

    constructor(seed: String) {
        r.addAll(mutableListOf(Random(seed + "0"), Random(seed + "1"), Random(seed + "2"), Random(seed + "3")))
        h.addAll(mutableListOf(1.0, 2.0, 3.0, 4.0))
        a.addAll(mutableListOf(1.0, 1 / 2.0, 1 / 4.0, 1 / 8.0))
        r.forEachIndexed { index, ran -> ran.nextDouble((seed + "$index").hashCode() / 2000.0) }
    }

    constructor(seed: String, num: Double) {
        r.addAll(mutableListOf(Random(seed + "0", num), Random(seed + "1", num), Random(seed + "2", num), Random(seed + "3", num)))
        h.addAll(mutableListOf(1.0, 2.0, 3.0, 4.0))
        a.addAll(mutableListOf(1.0, 1 / 2.0, 1 / 4.0, 1 / 8.0))
        r.forEach { it.nextDouble(Math.random()) }
        r.forEachIndexed { index, ran -> ran.nextDouble(num + (seed + "$index").hashCode() / 2000.0) }
    }

    fun nextDouble(increment: Double): Double {
        return r.mapIndexed { index, rr -> rr.nextDouble(increment * h[index]) * a[index] }.sum()
    }

    fun nextGaussian(increment: Double): Double {
        return r.mapIndexed { index, rr -> rr.nextGaussian(increment * h[index]) * a[index] }.sum()
    }

    fun fork(): Random2 {
        val ret = Random2()

        ret.r.clear()
        r.forEach { ret.r.add(it.fork()) }

        ret.h.clear()
        h.forEach { ret.h.add(it) }

        ret.a.clear()
        a.forEach { ret.a.add(it) }
        return ret;
    }

}
