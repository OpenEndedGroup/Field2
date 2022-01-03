package trace.util

import field.graphics.FLinesAndJavaShapes
import field.graphics.StandardFLineDrawing.notation
import field.linalg.Vec2
import field.linalg.Vec3
import fieldbox.boxes.Box
import fieldbox.boxes.FLineDrawing

class Intersections(val b: Box) {

    fun x(x: Double): MutableList<Vec2> {
        val r = mutableListOf<Vec2>();

        ix(b, x, r)
        b.children.forEach {
            ix(it, x, r)
        }
        return r
    }

    private fun ix(it: Box, x: Double, r: MutableList<Vec2>) {
        it.properties.get(FLineDrawing.lines)?.values?.forEach {
            val m = it.get()
            if (m != null) {
                if (m.attributes.isTrue(notation, false)) {
                    val intersections = FLinesAndJavaShapes.intersectX(m, x)
                    if (intersections != null)
                        intersections.forEach { r.add(it.toVec2()) }
                }
            }
        }
    }

    fun y(x: Double): MutableList<Vec2> {
        val r = mutableListOf<Vec2>();

        iy(b, x, r)
        b.children.forEach {
            iy(it, x, r)
        }
        return r
    }

    private fun iy(it: Box, x: Double, r: MutableList<Vec2>) {
        it.properties.get(FLineDrawing.lines)?.values?.forEach {
            val m = it.get()
            if (m != null) {
                if (m.attributes.isTrue(notation, false)) {
                    val intersections = FLinesAndJavaShapes.intersectY(m, x)
                    if (intersections != null)
                        intersections.forEach { r.add(it.toVec2()) }
                }
            }
        }
    }


}