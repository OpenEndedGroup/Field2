package fieldbox.boxes.plugins

import field.graphics.FLine
import field.graphics.StandardFLineDrawing
import field.linalg.Vec4
import field.utility.*
import fieldbox.boxes.Box
import fieldbox.boxes.Drawing
import fieldlinker.AsMap
import java.awt.BasicStroke
import java.util.function.Supplier

import java.util.stream.Collectors


class SimpleSnapHelper(private val inside: Box, val THRESHOLD: Int = 10) {


    // going, start, current
    fun help(ongoing: Collection<Triple<Box, Rect, Rect>>): List<FLine> {
        if (ongoing.isEmpty()) return emptyList()

        val min = ongoing.minBy { it.second.x }!!.third.x
        val max = ongoing.maxBy { it.second.x + it.second.w }!!.third.xw

        val pop = population(ongoing.map { it.first }.toSet())

        val m1 = mark(min, pop, { x, mark -> x.toDouble() - min + mark.second });

        if (m1 != null) {
            val start = ongoing.first().first!!

            val viewBounds = start.find(Drawing.drawing, start.both()).findFirst().get().getCurrentViewBounds(start);

            // min should be m1.second by aligning to box m1.first
            ongoing.forEach {
                it.third.x = m1.third.invoke(it.third.x, m1.first to m1.second).toFloat()
            }

            val f = FLine().moveTo(m1.second.toDouble(), viewBounds.y.toDouble()).lineTo(m1.second.toDouble(), viewBounds.yh.toDouble())
            f += StandardFLineDrawing.color to Vec4(1, 1, 1, 0.5)
            f += StandardFLineDrawing.thicken to BasicStroke(3f, 0, 0, 1f, floatArrayOf(15f, 15f), 0f);
            return listOf(f)
        }

        val m2 = mark(max, pop, { x, mark -> x.toDouble() - max + mark.second });
        if (m2 != null) {
            val start = ongoing.first().first!!

            val viewBounds = start.find(Drawing.drawing, start.both()).findFirst().get().getCurrentViewBounds(start);

            // min should be m1.second by aligning to box m1.first
            ongoing.forEach {
                it.third.x = m2.third.invoke(it.third.xw, m2.first to m2.second).toFloat()-it.third.w
            }

            val f = FLine().moveTo(m2.second.toDouble(), viewBounds.y.toDouble()).lineTo(m2.second.toDouble(), viewBounds.yh.toDouble())
            f += StandardFLineDrawing.color to Vec4(1, 1, 0, 0.5)
            f += StandardFLineDrawing.thicken to BasicStroke(3f, 0, 0, 1f, floatArrayOf(15f, 15f), 0f);
            return listOf(f)
        }


        return emptyList()
    }

    fun mark(x: Number, pop: Collection<Box>, applicator: (Number, kotlin.Pair<Box, Float>) -> Number): kotlin.Triple<Box, Float, (Number, kotlin.Pair<Box, Float>) -> Number>? {
        val sortedBy = pop.flatMap { listOf(it to it.properties.get(Box.frame).x, it to it.properties.get(Box.frame).xw) }.sortedBy { Math.abs(it.second - x.toFloat()) }

        if (sortedBy.size == 0) return null;
        if (Math.abs(sortedBy.first().second - x.toFloat()) < THRESHOLD) return kotlin.Triple(sortedBy.first().first, sortedBy.first().second, applicator)
        println(" no snap ${sortedBy.first().second} $x")
        return null
    }

    protected fun population(exclude: Set<Box>): Collection<Box> {
        return inside.breadthFirst(inside.downwards())
                .filter { x -> !x.properties.isTrue(Chorder.nox, false) }
                .filter { x -> x.properties.has(Box.frame) }
                .filter { x -> !exclude.contains(x) }.collect(Collectors.toList())
    }

}

private val Rect.yh: Float
    get() {
        return this.y + this.h;
    }

private fun <A, B> kotlin.Pair<A, B>.toPair(): Pair<A, B> {
    return field.utility.Pair(this.first, this.second)
}


private val Rect.xw: Float
    get() {
        return this.x + this.w;
    }
