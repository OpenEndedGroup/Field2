package auw

import field.app.ThreadSync2
import field.graphics.FLine
import field.graphics.StandardFLineDrawing.color
import field.utility.*
import fieldbox.boxes.Box
import fieldbox.boxes.Boxes
import fieldbox.boxes.Drawing
import fieldbox.boxes.FLineDrawing
import fieldbox.boxes.FLineDrawing.lines
import fieldbox.boxes.plugins.Exec
import fieldbox.execution.Execution
import fieldbox.io.IO
import fieldcef.plugins.up
import fielded.live.Asta
import fieldnashorn.NashornExecution
import org.openjdk.nashorn.api.scripting.JSObject
import org.openjdk.nashorn.api.scripting.NashornException
import java.util.function.Supplier

class MixerRoot : Box(), IO.Loaded {

    lateinit var mixer: Mixer

    init {
        properties.put(Dict.Prop<MixerRoot>("_mixerRoot"), this)
        properties.put(Dict.Prop<BiFunctionOfBoxAnd<_FBuffer, Boolean>>("scope"), BiFunctionOfBoxAnd { box, buffer ->

            var b = buffer.get()

            val f = FLine()

            for (i in 0 until b.length) {
                f.lineTo(i * 0.25, -b.a.get(i) * 200.0)
            }

            val f2 = FLine()
            f2.lineTo(0.0, 0.0).lineTo(b.length*0.25, 0.0)
            f2.attributes.put(color, Vec4(0,0,0,0.3))

            val f3 = FLineDrawing.boxOrigin(f, Vec2(1.1, 1.1), box)
            box.properties.putToMap(lines, "_scope_", f3)

            val f4 = FLineDrawing.boxOrigin(f2, Vec2(1.1, 1.1), box)
            box.properties.putToMap(lines, "_scopeh_", f4)

            Drawing.dirty(box)

            true
        })
    }

    override fun loaded() {

        mixer = Mixer(parents.first())

        val was = LinkedHashSet<Box>()

        properties.putToMap(Boxes.insideRunLoop, "main.checkChildren", Supplier {

            (children - was).forEach { onboard(it) }
            (was - children).forEach { offboard(it) }

            was.clear()
            was.addAll(children)

            true
        })

        this += Dict.Prop<Mixer>("mixer") to mixer
    }

    fun findByName(n: String): Box? {
        val m =
            children().find { it.properties.get(Box.name).equals(n) && it.properties.get(Definitions.audio) is JSObject }
        return m
    }

    private fun offboard(b: Box) {
        println("offboard $b")
        b.properties.remove(NashornExecution.customExecutor)
        b.properties.remove(OverloadedMath.withFunctionRewriting)
        b.properties.remove(OverloadedMath.functionRewriteTrap)
    }

    private fun onboard(b: Box) {
        println("onboard $b")
        (b up Exec.exec)!!.apply(b, mixer.lib._stdlib)
        b += NashornExecution.customExecutor to mixer.executor

    }

}