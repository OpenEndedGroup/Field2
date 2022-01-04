package field.graphics

import field.linalg.Vec3
import field.utility.minus
import field.utility.plus
import field.utility.times

/**
 * the fastest, worst thicken imaginable
 */
class EvenFasterThicken(val w: Double) {

    fun renderToMeshByThickening(line: FLine, target: MeshBuilder) {
        line.flattenAuxProperties()

        var previous: Vec3? = null

        var face = arrayOf<Vec3?>(null, null)

        line.nodes.forEach {
            if (it.flatAuxData != null)
                for (i in it.flatAuxData.indices) {
                    val channel = it.flatAux[i]
                    val value = it.flatAuxData[i]
                    if (value != null && channel > 0) {
                        target.aux(channel, value)
                    }
                }

            when (it) {
                is FLine.CubicTo -> {
                    segment(previous, it, target, face)
                    previous = it.to
                }
                is FLine.LineTo -> {
                    segment(previous, it.to, target, face)
                    previous = it.to
                }
                is FLine.MoveTo -> {
                    previous = it.to
                    face[0] = null
                    face[1] = null
                }
            }
        }
    }

    private fun segment(previous: Vec3?, it: Vec3, target: MeshBuilder, face: Array<Vec3?>) {
        if (face[0] == null) {
            var d = (it - previous!!).normalize()
            var d2 = prep(d) * w / 2.0
            face[0] = previous + d2
            face[1] = previous - d2
        }
        target.v(face[0])
        target.v(face[1])

        var d = (it - previous!!).normalize()
        var d2 = prep(d) * w / 2.0;
        face[0] = it + d2
        face[1] = it - d2

        target.v(face[1])
        target.v(face[0])

        target.e(0, 1, 2)
        target.e(0, 2, 3)
    }

    private fun prep(d: Vec3): Vec3 {

        val v = Vec3(-d.y, d.x, Math.random()*1e-10)
        val v2 = Vec3.cross(v, d, Vec3())
        return Vec3.cross(d, v2, Vec3())
    }

    private fun segment(previous: Vec3?, it: FLine.CubicTo, target: MeshBuilder, face: Array<Vec3?>) {

        var p = previous!!
        for (i in 1 until 10) {
            val o = Vec3()
            FLinesAndJavaShapes.evaluateCubicFrame(p, it.c1, it.c2, it.to, i / 9.0, o)
            segment(p, o, target, face)
            p = o;
        }
    }


}