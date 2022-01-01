package fieldbox.boxes.plugins

import field.app.RunLoop
import field.graphics.FLine
import field.graphics.FLinesAndJavaShapes
import field.graphics.Scene
import field.graphics.StandardFLineDrawing
import field.graphics.util.onsheetui.SimpleCanvas
import field.linalg.Vec2
import field.linalg.Vec3
import field.utility.*
import fieldbox.boxes.Box
import fieldbox.boxes.Callbacks
import fieldbox.boxes.FLineDrawing

import java.util.ArrayList
import java.util.function.Supplier

/**
 * a spot for making non-savable child boxes all nicely arranged with respect to a parent box
 */
class PadGroup(name: String, private val parent: Box) {
    private var spotIs: Rect? = null

    var margin = 18f;

    internal var contents: Dict.Prop<List<Box>> = Dict.Prop<Any>("__padGroup__" + name + "_contents").toCanon()
    internal var mark: Dict.Prop<Boolean> = Dict.Prop<Any>("__padGroup__" + name + "_mark").toCanon()

    companion object {
        fun getOrMake(name: String, parent: Box): PadGroup {
            return parent.properties.computeIfAbsent(Dict.Prop<PadGroup>("__padGroup__" + name), { k -> PadGroup(name, parent) })
        }
    }

    init {
        spotIs = spot(parent)
        parent.properties.putToMap(Callbacks.onFrameChanged, "__padGroup__" + name, Box.BiFunctionOfBoxAnd<Rect, Rect> { box, rect ->

            if (box === parent) {

                val spotNow = rect

                if (Math.abs(spotNow.x - spotIs!!.x) + Math.abs(spotNow.y - spotIs!!.y) > 0.0f) {
                    updateChildren(spotNow)
                }

                spotIs = spotNow.duplicate()
            }

            rect
        })

/*        parent.properties.putToMap(FLineDrawing.bulkLines, "__padGroup__" + name, Supplier<Collection<Supplier<FLine>>> {
            val r = mutableListOf<Supplier<FLine>>()

            val down = FLine()
            val s = spot(parent)
            val d = initialPositionDown()
            down.moveTo(s.x, s.y - margin.toDouble())
            down.lineTo(s.x, d.toDouble())

            down += StandardFLineDrawing.color to Vec4(1, 1, 1, 0.4)

            r += down

            val radius = 3.0
            val dot = FLine()
            dot.circle(s.x, s.y - margin.toDouble(), radius)

            dot += StandardFLineDrawing.filled to true
            dot += StandardFLineDrawing.fillColor  to Vec4(0.5, 0.5, 0.5, 1)
            dot += StandardFLineDrawing.color  to Vec4(0.5, 0.5, 0.5, 0.2)

            r += dot

            r
        })
 */


    }

    fun add(box: Box) {
        val q = parent.properties.computeIfAbsent(contents) { k -> ArrayList() }
        if (!q.contains(box))
            (q as MutableList<Box>).add(box)
        updateChildren(spot(parent))
    }

    fun clear() {
        val q = parent.properties.computeIfAbsent(contents) { k -> ArrayList() }
        q.forEach { x -> Callbacks.delete(x) }

        (q as MutableList<Box>).clear()
    }

    var beginCount: Int = 0

    fun begin() {
        if (beginCount == 0) {
            val q = parent.properties.computeIfAbsent(contents) { k -> ArrayList() }
            q.forEach { x -> x.properties.put(mark, false) }

            RunLoop.main.mainLoop.attach(Scene.perform(100, {
                end()
                false
            }))
            beginCount++
        }
    }

    fun mark(b: Box) {
        b.properties.put(mark, true)
    }

    fun end() {
        if (beginCount == 1) {
            val q = parent.properties.computeIfAbsent(contents) { k -> ArrayList() }
            val gone = ArrayList<Box>()

            q.forEach { x ->
                if (!x.properties.isTrue(mark, false)) {
                    Callbacks.delete(x)
                    gone.add(x)
                }
            }

            (q as MutableList<Box>).removeAll(gone)
            beginCount--
        }
    }


    fun remove(box: Box) {
        (parent.properties.computeIfAbsent(contents) { k -> ArrayList() } as MutableList<Box>).remove(box)
    }

    private fun updateChildren(spotNow: Rect) {
        parent.properties.computeIfAbsent(contents) { k -> ArrayList() }
                .forEach { x -> x.properties.put(Box.frame, Callbacks.frameChange(x, closeOn(spotIs!!, spotNow, x.properties.get<Rect>(Box.frame)))) }
    }

    private fun closeOn(outerFrameWas: Rect, outerFrame: Rect, innerFrame: Rect): Rect {

        var f = FLine().rect(outerFrameWas)
        val oldT = FLinesAndJavaShapes.closestT(f, Vec3(innerFrame.x + innerFrame.w / 2, innerFrame.y + innerFrame.h / 2, 0))

        val newCenter = run {
            var f = FLine().rect(outerFrame)
            f.cursor().setT(oldT).position()
        }

        return Rect(newCenter.x - innerFrame.w / 2, newCenter.y - innerFrame.h / 2, innerFrame.w.toDouble(), innerFrame.h.toDouble())
    }

    private fun spot(parent: Box, rect: Rect? = null): Rect {
        return rect ?: parent.properties.get<Rect>(Box.frame);
//        val to = (if (rect != null) rect else parent.properties.get<Rect>(Box.frame))!!.convert(0.5, 0.5)
//        to.y += margin.toDouble()
//        return to
    }

    fun initialPositionDown(): Vec2 {
        val others = parent.properties.computeIfAbsent(contents) { k -> ArrayList() }.filter { !it.disconnected }

        val f = FLine().rect(parent.properties.get(Box.frame));
        val c = f.cursor()

        for (x in 0..(c.lengthD() / margin).toInt()) {
            val vv = c.setD((x * margin).toDouble()).position().toVec2()
            val mx: Double? = others.map { it.properties.get(Box.frame).center.distance(vv) }.maxOrNull()
            if (mx == null) return c.position().toVec2()
            if (mx > margin) return c.position().toVec2()
        }

        // overflow....

        return c.setD(Math.random() * c.lengthD()).position().toVec2()
    }

}

private val Rect.center: Vec2
    get() {
        return Vec2(this.x + this.w / 2, this.y + this.h / 2);
    }
