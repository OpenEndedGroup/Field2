package trace.mocap

import c3dv.model.C3DFile
import c3dv.model.C3DReader
import c3dv.model.CharParameter
import field.linalg.Quat
import field.linalg.Vec3
import field.utility.Documentation
import field.utility.minus
import field.utility.plus
import field.utility.times
import trace.util.TCB3

class Mocap(val fn: String) {
    @JvmField
    val reader: C3DReader

    @JvmField
    val file: C3DFile

    val splines = mutableListOf<TCB3>()

    private var actualCenter: Vec3

    init {
        reader = C3DReader()
        file = reader.load(fn)

        val data = mutableListOf<Vec3>()

        for (p in 0 until file.num3DPoints) {
            for (f in file.frames) {
                if (f.isValid(p)) {
                    val datum = Vec3(f.x[p].toDouble(), -f.z[p].toDouble(), f.y[p].toDouble()) * 0.17
                    data.add(datum)
                }
            }
        }

        val center = data.reduce { a, b -> a + b }.mul(1.0 / data.size)
        val distance = data.map { it.distance(center) }.maxOrNull()!!
        val lowest = data.maxByOrNull { it: Vec3 -> it.y }!!.y

        val data2 = mutableListOf<Vec3>()

        for (p in 0 until file.num3DPoints) {
            val t = TCB3()
            var frameNum = 0
            for (f in file.frames) {
                if (f.isValid(p)) {
                    val datum = (Vec3(f.x[p].toDouble(), -f.z[p].toDouble(), f.y[p].toDouble()) * 0.17 - Vec3(
                        center.x,
                        center.y,
                        center.z
                    )) * (50.0 / distance) + Vec3(50.0, 50.0, 0.0)

                    data2.add(datum)
                    val nn = t.Node(frameNum.toDouble(), datum)
                }
                frameNum++
            }
            t.tcbAll()
            splines.add(t)
        }

        actualCenter = data2.reduce { a, b -> a + b }.mul(1.0 / data2.size)

    }

    @Documentation("`_.numPoints()` tells you how many points of motion does this mocap file have in it?")
    fun numPoints(): Int {
        return file.num3DPoints
    }

    private val ap = (0 until numPoints()).toCollection(ArrayList())

    @Documentation("`_.allPoints()` returns list of numbers with one number corresponding to each point")
    fun allPoints(): List<Int> {
        return ap
    }


    @Documentation("`_.centerAtTime(time)` returns the center of the motion capture at time 'time'")
    fun centerAtTime(time: Double): Vec3 {
        val tot = (0 until numPoints()).map { positionAtTime(time, it) }.reduce({ a, b -> a + b })
        return tot * (1.0 / numPoints())
    }

    @Documentation("`_.center()` returns the center of the motion capture over the whole clip")
    fun center(): Vec3 {
        return actualCenter
    }

    @Documentation("_.rotate(point, center, 34) rotates the point `point` around `center` by 34 degrees (in the 'xz' plane; up stays up)")
    fun rotate(point: Vec3, center: Vec3, angle: Double): Vec3 {
        return Quat().fromAxisAngleDeg(Vec3(0.0, 1.0, 0.0), angle).transform((point - center)) + center
    }


    @Documentation("`mc.nameOfPoint(4)` gives the 'name' of point 4, if there is one, otherwise 'unknown'")
    fun nameOfPoint(n: Int): String {
        val p = file.getParameter("POINT", "LABELS")
        when (p) {
            is CharParameter -> {
                if (p.data == null) return "no labels in file"
                if (n >= p.data.size) return "not enough labels in file"
                return p.data[n]
            }
            else ->
                return "wrong kind of labels in file"
        }
    }

    @Documentation("returns the position of point `pointNum` at time `time` where time goes from 0 to 1 over the course of the clip")
    fun positionAtTime(time: Double, pointNum: Int): Vec3 {
        if (pointNum >= splines.size) throw IllegalArgumentException(" no such point $pointNum, there are only ${splines.size} points in this file")
        return splines[pointNum].evaluate(time * file.frames.size, Vec3())
    }

    @Documentation("returns the velocity of point `pointNum` at time `time` where time goes from 0 to 1 over the course of the clip")
    fun velocityAtTime(time: Double, pointNum: Int): Vec3 {
        if (pointNum >= splines.size) throw IllegalArgumentException(" no such point $pointNum, there are only ${splines.size} points in this file")
        var e = 0.5;
        val p1 = splines[pointNum].evaluate(-e + time * file.frames.size, Vec3())
        val p2 = splines[pointNum].evaluate(+e + time * file.frames.size, Vec3())
        return p2 - p1
    }

}