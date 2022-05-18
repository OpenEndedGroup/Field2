package trace.mocap

import field.linalg.Vec3
import field.utility.minus
import field.utility.plus
import field.utility.times

class CInterp3(val d: List<Pair<Double, Vec3>>) {

    constructor(x: List<Double>, y: List<Vec3>) : this(x.zip(y))

    data class Frame(
        val x0: Double, val x1: Double, val x2: Double, val x3: Double,
        val n0: Vec3, val n1: Vec3, val n2: Vec3, val n3: Vec3, val d1: Vec3, val d2: Vec3
    ) {
        fun interpolate(t: Double): Vec3 {
            val hi = (x2 - x1)
            val u = (t - x1) / hi

            val v = Vec3()

            v.x =
                (n1.x * (1 + 2 * u) + d1.x * u * hi) * (1 - u) * (1 - u) + (n2.x * (3 - 2 * u) - d2.x * hi * (1 - u)) * u * u
            v.y =
                (n1.y * (1 + 2 * u) + d1.y * u * hi) * (1 - u) * (1 - u) + (n2.y * (3 - 2 * u) - d2.y * hi * (1 - u)) * u * u
            v.z =
                (n1.z * (1 + 2 * u) + d1.z * u * hi) * (1 - u) * (1 - u) + (n2.z * (3 - 2 * u) - d2.z * hi * (1 - u)) * u * u

            return v
        }
    }


    var previousFrame: Frame? = null

    fun interpolate(t: Double): Vec3 {

        return previousFrame.let { pf ->
            (if (pf == null || t < pf.x1 || t > pf.x2) {
                val index = d.binarySearch {
                    -t.compareTo(it.first)
                }

                if (index >= 0) return d[index].second
                val i = -index - 1


                var before = i - 1
                var after = i

                var n0 = d(before - 1).second
                var n1 = d(before).second
                var n2 = d(after).second
                var n3 = d(after + 1).second

                var x0 = d(before - 1).first
                var x1 = d(before).first
                var x2 = d(after).first
                var x3 = d(after + 1).first

                var d1 = ((n2 - n1) / (x2 - x1) + (n1 - n0) / (x1 - x0)) * 0.5
                var d2 = ((n3 - n2) / (x3 - x2) + (n2 - n1) / (x2 - x1)) * 0.5

                previousFrame = Frame(x0, x1, x2, x3, n0, n1, n2, n3, d1, d2)
                previousFrame
            } else previousFrame)!!.let {
                it.interpolate(t)
            }
        }
    }

    operator fun <T> List<T>.invoke(x: Int) = this[Math.max(0, Math.min(size - 1, x))]
}

class CInterp(val d: List<Pair<Double, Double>>) {

    constructor(x: List<Double>, y: List<Double>) : this(x.zip(y))

    data class Frame(
        val x0: Double, val x1: Double, val x2: Double, val x3: Double,
        val n0: Double, val n1: Double, val n2: Double, val n3: Double, val d1: Double, val d2: Double
    ) {
        fun interpolate(t: Double): Double {
            val hi = (x2 - x1)
            val u = (t - x1) / hi

            return (n1 * (1 + 2 * u) + d1 * u * hi) * (1 - u) * (1 - u) + (n2 * (3 - 2 * u) - d2 * hi * (1 - u)) * u * u
        }
    }


    var previousFrame: Frame? = null

    fun interpolate(t: Double): Double {

        return previousFrame.let { pf ->
            (if (pf == null || t < pf.x1 || t > pf.x2) {
                val index = d.binarySearch {
                    -t.compareTo(it.first)
                }

                if (index >= 0) return d[index].second
                val i = -index - 1


                var before = i - 1
                var after = i

                var n0 = d(before - 1).second
                var n1 = d(before).second
                var n2 = d(after).second
                var n3 = d(after + 1).second

                var x0 = d(before - 1).first
                var x1 = d(before).first
                var x2 = d(after).first
                var x3 = d(after + 1).first

                var d1 = safe((n2 - n1) / (x2 - x1), (n1 - n0) / (x1 - x0))
                var d2 = safe((n3 - n2) / (x3 - x2), (n2 - n1) / (x2 - x1))

                previousFrame = Frame(x0, x1, x2, x3, n0, n1, n2, n3, d1, d2)
                previousFrame
            } else previousFrame)!!.let {
                it.interpolate(t)
            }
        }
    }

    private fun safe(d: Double, d1: Double): Double {
        if (d.isNaN()) {
            if (d1.isNaN())
                return 0.0
            return d1
        } else
            if (d1.isNaN()) {
                return d
            }
        return (d + d1) / 2;
    }

    operator fun <T> List<T>.invoke(x: Int) = this[Math.max(0, Math.min(size - 1, x))]
}