package fieldbox.boxes.plugins

import field.app.RunLoop
import field.graphics.FLine
import field.graphics.Scene
import field.graphics.StandardFLineDrawing
import field.linalg.Vec2
import field.utility.Dict
import field.utility.Rect
import field.utility.Vec4
import field.utility.plusAssign
import fieldbox.boxes.Box
import fieldbox.boxes.Callbacks
import fieldbox.boxes.FLineDrawing
import java.util.function.Supplier

/**
 * a spot for making non-savable child boxes all nicely arranged with respect to a parent box
 */
class UIGroup(name: String, private val parent: Box) {
    private var spotIs: Vec2? = null

    var margin = 10f

    internal var contents: Dict.Prop<List<Box>> = Dict.Prop<Any>("__uiGroup__" + name + "_contents").toCanon()
    internal var mark: Dict.Prop<Boolean> = Dict.Prop<Any>("__uiGroup__" + name + "_mark").toCanon()

    companion object {
        fun getOrMake(name: String, parent: Box): UIGroup {
            return parent.properties.computeIfAbsent(Dict.Prop<UIGroup>("__uiGroup__" + name), { k -> UIGroup(name, parent) })
        }
    }

    init {
        spotIs = spot(parent)
        parent.properties.putToMap(Callbacks.onFrameChanged, "__uiGroup__" + name, Box.BiFunctionOfBoxAnd<Rect, Rect> { box, rect ->

            if (box === parent) {

                val spotNow = spot(box, rect)

                spotNow.sub(spotIs)

                if (spotNow.length() > 0.0f) {
                    updateChildren(spotNow)
                }

                spotIs = spot(box, rect)
            }

            rect
        })

        parent.properties.putToMap(FLineDrawing.bulkLines, "__uiGroup__" + name, Supplier<Collection<Supplier<FLine>>> {
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
    }

    fun add(box: Box) {
        val q = parent.properties.computeIfAbsent(contents) { k -> ArrayList() }
        if (!q.contains(box))
            (q as MutableList<Box>).add(box)
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

    private fun updateChildren(spotNow: Vec2) {
        parent.properties.computeIfAbsent(contents) { k -> ArrayList() }.forEach { x -> x.properties.put(Box.frame, Callbacks.frameChange(x, x.properties.get<Rect>(Box.frame)!!.translate(spotNow))) }
    }

    private fun spot(parent: Box, rect: Rect? = null): Vec2 {
        val to = (if (rect != null) rect else parent.properties.get<Rect>(Box.frame))!!.convert(0.5, 1.0)
        to.y += margin.toDouble()
        return to
    }

    fun initialPositionDown(): Float {

        val m = parent.properties.computeIfAbsent(contents) { k -> ArrayList() }.filter { !it.disconnected }
            .map { it.properties.get(Box.frame).yh }.maxOrNull()

        return margin + (if (m != null) m else spoty.toFloat())

    }

    val spotx: Double
        get() = spot(parent).x

    val spoty: Double
        get() = spot(parent).y

}
