package fieldbox.boxes.plugins

import field.graphics.FLine
import field.graphics.StandardFLineDrawing.*
import field.linalg.Vec4
import field.utility.Rect
import field.utility.plusAssign
import fieldbox.boxes.Box
import fieldbox.boxes.TimeSlider.localTime
import java.awt.BasicStroke
import java.util.function.Consumer

/**
 * works with TimeSlider to convert timebases
 */
class LocalTime : Box() {
    companion object {

        @JvmOverloads
        fun growTimeFor(root: Box?, boxes: Set<Box>, time: Double, seen: MutableSet<Box> = LinkedHashSet()): Map<Box, Double> {
            val actualParents = LinkedHashSet<Box>()

            boxes.stream().filter { x -> !parentOverrides(x, boxes) }.forEach(Consumer<Box> { actualParents.add(it) })

            val r = LinkedHashMap<Box, Double>()
            for (b in actualParents) {
                if (b.properties.has(localTime)) {
                    r.put(b, time)
                    for (bb in b.children) {
                        val t2 = b.properties.get(localTime)!!.apply(bb, time)
                        r.put(bb, t2)
                        if (seen.contains(bb) && intersects(t2, bb.properties.get(Box.frame))) {
                            seen.add(bb)
                            r.putAll(growTimeFor(root, setOf(bb), t2, r.keys))
                        }
                    }
                } else {
                    r.put(b, time)
                }
            }

            return r
        }

        fun drawTimesFor(boxes: Map<Box, Double>, time: Double): List<FLine> {
            val r = ArrayList<FLine>()

            boxes.forEach { k, v ->
                if (Math.abs(v - time) > 1) {
                    val f = FLine().moveTo(v, k.properties.get(Box.frame)!!.y.toDouble()-10).lineTo(v, (k.properties.get(Box.frame)!!.y + k.properties.get(Box.frame)!!.h+10).toDouble())
                    f += color to Vec4(1.0, 0.0, 0.0, 1.0)
                    f += thicken to BasicStroke(5f)
                    r.add(f)
                    val f2 = FLine().rect(v, k.properties.get(Box.frame)!!.y.toDouble(), 30.0, k.properties.get(Box.frame)!!.h.toDouble())
                    f2 += color to Vec4(1.0, 0.0, 0.0, -0.5)
                    f2 += filled to true
                    r.add(f2)
                    val f3 = FLine().circle(v, k.properties.get(Box.frame)!!.y.toDouble(), 4.0).circle(v, k.properties.get(Box.frame)!!.yh.toDouble(), 4.0)
                    f3 += color to Vec4(1.0, 0.0, 0.0, 0.5)
                    f3 += filled to true
                    r.add(f3)
                }
            }

            return r

        }

        private fun intersects(t: Double, r: Rect?): Boolean {
            return r!!.intersectsX(t)
        }

        private fun parentOverrides(x: Box, others: Set<Box>): Boolean {
            return x.breadthFirst(x.upwards()).filter { i -> i !== x /*&& others.contains(i)*/ && i.properties.has(localTime) }.findAny().isPresent
        }
    }
}
