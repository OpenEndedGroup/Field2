package fieldbox.boxes.plugins

import field.app.RunLoop
import field.graphics.FLine
import field.graphics.StandardFLineDrawing.*
import field.utility.*
import fieldbox.boxes.Box
import fieldbox.boxes.Boxes
import fieldbox.boxes.Colors
import fieldbox.boxes.Drawing
import fieldbox.boxes.FLineDrawing.bulkLines
import fieldbox.boxes.FLineDrawing.frame
import fieldbox.execution.Execution
import fieldcef.plugins.up
import fieldkotlin.color
import fieldkotlin.filled
import fieldkotlin.invoke
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Supplier

// simple debugging annotations, targeted at dataflow analysis for complex boxes in Kotlin
// e.g
// t.mark("bob", Math.random(), strobe=5)
// can be compiled out to nothing
// decay to amber, red and then disappear

/*
import fieldbox.boxes.plugins.TrafficLights as t
t.off = false
var _r = sequence {
	var x = 0
	while(x++<300)
	{
		t.mark("bob")
		t.mark("marc", Math.random(), strobe=40)
		yield()
		yield()
	}
	while(x++<600)
	{
		t.mark("bob2")
		t.mark("marc2", Math.random(), strobe=40)
		yield()
	}
}
 */
class TrafficLights(val root: Box) {

    companion object {
        var max_lost = 100
        var line_height = 15.0
        var margin = 3.0

        var off = false

        var tick = 0L

        private fun defaultBox() = Execution.context.get().peek()

        lateinit var tl: TrafficLights

        private fun events(): TrafficLights {
            if (!this::tl.isInitialized)
                tl = TrafficLights(defaultBox())
            return tl
        }

        fun mark(n: String, o: Any? = null, strobe: Int = 0, b: Box = defaultBox()) {
            if (off) return
            val t = events().events.computeIfAbsent(b) { ConcurrentHashMap() }
                .computeIfAbsent(n) { Traces(n) }
            t.events.add(o)

            if (o!=null && (strobe == 0 || (tick % strobe) == 0L)) {
                t.tick++
            }

        }
    }


    class Traces(val name: String) {
        var mark = false
        var seen = 0
        var lost = 0
        var state = 0
        var tick = 0

        var row = -1

        val events = mutableListOf<Any?>()
        var visualization: kotlin.Pair<String, MutableList<FLine>>? = null

        var history: Any? = null
    }

    val events = ConcurrentHashMap<Box, ConcurrentHashMap<String, Traces>>()

    init {
        RunLoop.main.loop.attach {
            beat()
            true
        }
    }


    var lastHash = 0L

    private fun beat() {
        tick++
        events.entries.map { it.value.values }.forEach {
            it.removeIf {
                it.lost > max_lost
            }
        }

        events.entries.removeIf {

            if (it.value.isEmpty()) it.key.properties.putToMap(bulkLines, "__traffic__", Supplier {
                emptyList()
            })

            it.value.isEmpty()
        }

        var s = 0
        var h = 0L
        events.entries.forEach { (box, traces: ConcurrentHashMap<String, Traces>) ->
            val v = mutableListOf<FLine>()
            traces.forEach { (name, trace) ->

                if (trace.row == -1)
                    trace.row = allocate(traces)

                trace.visualization = visualize(box, trace.row, trace.name, trace)
                trace.events.clear()
                trace.visualization?.let {
                    v.addAll(it.second)
                    println(it.first+", ")
                    h = 31 * h + it.first.hashCode()
                }
            }

            box.properties.putToMap(bulkLines, "__traffic__", Supplier {
                v
            })
        }

        if (h != lastHash) {
            println("$lastHash : $h")
            Drawing.dirty(root)
            lastHash = h
        }

    }

    private fun allocate(traces: ConcurrentHashMap<String, Traces>): Int {
        var s = LinkedHashSet<Int>()
        IntRange(0, traces.size + 1).forEach { s.add(it) }
        traces.filter { it.value.row != -1 }.forEach { s.remove(it.value.row) }
        return s.first()
    }

    class Sparkline(val h: Int = 100) {
        val d = mutableListOf<Double>()
        var min = Double.POSITIVE_INFINITY
        var max = Double.NEGATIVE_INFINITY

        fun add(n: Double) {
            d.add(n)
            min = Math.min(min, n)
            max = Math.max(max, n)
            while (d.size > h) d.removeAt(0)
        }

        fun r(d: Double) = String.format("%.2f", d)
        fun label() = "${r(min)} -> ${r(max)}"
        fun visualizeOff(x: Double, y: Double): List<FLine> {
            if (max == min) {
                var m = FLine()
                m.moveTo(x, y)
                m.last()[text] = "=${max}"
                m.last() += textAlign to 0.0
                m += hasText to true
                return listOf(m)
            } else {
                var m = FLine()
                m.rect(x, y, 100.0, line_height)
                m[color] = vec(0, 0.1, 0.2, 0.1)
                m[filled] = true
                m += hasText to true
                var m2 = FLine()
                (0 until d.size) {
                    m2.lineTo(
                        x + 100 * it / (d.size - 1.0),
                        y + line_height - line_height * (d[it] - min) / (max - min)
                    )
                }
                m2[color] = vec(0, 0.1, 0.2, 1.0)
                return listOf(m, m2)
            }

            return emptyList()
        }

        fun visualizeOn(x: Double, y: Double): List<FLine> {
            return visualizeOff(x, y)
        }
    }

    private fun visualize(key: Box, slot: Int, name: String, value: Traces): kotlin.Pair<String, MutableList<FLine>> {

        var frame = key.get(frame)
        frame = Rect(frame.x + frame.w + margin * 3, frame.y.d, frame.w.d, frame.h.d)

        if (value.events.size == 0) {
            var cursor = frame.x.d

            value.lost += 1
            value.seen = Math.max(0, value.seen - 1)
            value.state = 0

            val mm = mutableListOf<FLine>()

            if (value.history != null) {
                (value.history as? Sparkline)?.let {
                    mm += it.visualizeOff(
                        cursor,
                        frame.y + (line_height + margin) * slot,
                    )
                    cursor = mm[0].bounds().rightx() + margin
                }
            }

            var m = FLine().rect(cursor, frame.y + (line_height + margin) * slot, line_height.d, line_height.d)
            m[filled] = false
            m[color] = vec(0, 0, 0, 0.5)
            m.moveTo(cursor + line_height.d + margin, frame.y + (line_height + margin) * slot + line_height)
            var label = "$name [${value.lost}+ ago]"
            m.last() += text to label
            m.last() += textAlign to 0.0
            m += hasText to true

            mm.add(m)

            return ("$name : OFF : ${value.tick} : ${tick/10}") to mm
        }

        if (value.events.size >= 0) {
            value.lost = 0
            value.seen = Math.min(max_lost, value.seen + 1)
            value.state = 1

            var cursor = frame.x.d

            if (value.events.first() is Number && value.history == null) value.history = Sparkline()

            val mm = mutableListOf<FLine>()

            var extraLabel = ""
            (value.history as? Sparkline)?.let { spark ->
                value.events.forEach { spark.add((it as Number).d) }
                mm += spark.visualizeOn(
                    cursor,
                    frame.y + (line_height + margin) * slot,
                )
//                value.tick++

                extraLabel = " | " + spark.label()

                cursor = mm[0].bounds().rightx() + margin
            }

            var m = FLine().rect(cursor, frame.y + (line_height + margin) * slot, line_height.d, line_height.d)
            m[filled] = true
            m[color] = Vec4(0, 0.1, 0.2, 0.5)
            m.moveTo(cursor + line_height.d + margin, frame.y + (line_height + margin) * slot + line_height)
            m.last() += text to (name + extraLabel)
            m.last() += textAlign to 0.0
            m += hasText to true

            mm.add(m)

            println(value.tick)

            return ("$name : ${value.tick}") to mm
        }

        var m = FLine().rect(frame.x + 0.0, frame.y + (line_height + margin) * slot, line_height.d, line_height.d)
        m.moveTo(frame.x.d, frame.y + (line_height + margin) * slot)
        m.last() += text to "$name [unknown]"

        return "unknown" to mutableListOf()
    }

}