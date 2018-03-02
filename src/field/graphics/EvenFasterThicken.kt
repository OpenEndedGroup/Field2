package field.graphics

import field.linalg.Vec2
import field.utility.minus
import field.utility.plus
import field.utility.times

/**
 * the fastest, worst thicken imaginable
 */
class EvenFasterThicken(val w: Double) {

    fun renderToMeshByThickening(line: FLine, target: MeshBuilder) {
        line.flattenAuxProperties()

        var previous: Vec2? = null

        var face = arrayOf<Vec2?>(null, null)

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
                    previous = it.to.toVec2()
                }
                is FLine.LineTo -> {
                    segment(previous, it.to.toVec2(), target, face)
                    previous = it.to.toVec2()
                }
                is FLine.MoveTo -> {
                    previous = it.to.toVec2()
                    face[0] = null
                    face[1] = null
                }
            }
        }
    }

    private fun segment(previous: Vec2?, it: Vec2, target: MeshBuilder, face: Array<Vec2?>) {
        if (face[0] == null) {
            var d = (it - previous!!).normalize()
            var d2 = Vec2(-d.y, d.x) * w / 2.0
            face[0] = previous + d2
            face[1] = previous - d2
        }
        target.v(face[0])
        target.v(face[1])

        var d = (it - previous!!).normalize()
        var d2 = Vec2(-d.y, d.x) * w / 2.0;
        face[0] = it + d2
        face[1] = it - d2

        target.v(face[1])
        target.v(face[0])

        target.e(0, 1, 2)
        target.e(0, 2, 3)
    }

    private fun segment(previous: Vec2?, it: FLine.CubicTo, target: MeshBuilder, face: Array<Vec2?>) {

        var p = previous!!
        for (i in 1 until 20) {
            val o = Vec2()
            FLinesAndJavaShapes.evaluateCubicFrame(p, it.c1.toVec2(), it.c2.toVec2(), it.to.toVec2(), i / 19.0, o)
            segment(p, o, target, face)
            p = o;
        }
    }


}