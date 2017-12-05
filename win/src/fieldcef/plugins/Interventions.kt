package fieldcef.plugins

import field.app.RunLoop
import field.graphics.util.onsheetui.SimpleCanvas
import field.graphics.util.onsheetui.get
import field.utility.*
import fieldbox.DefaultMenus
import fieldbox.boxes.*
import fieldbox.boxes.FrameManipulation.selection
import fieldbox.boxes.plugins.BoxDefaultCode
import fieldbox.boxes.plugins.Chorder
import fieldbox.boxes.plugins.Chorder.begin
import fieldbox.boxes.plugins.DragToCopy
import fieldbox.execution.Execution
import fieldbox.io.IO
import fielded.RemoteEditor
import fielded.webserver.Server
import org.json.JSONObject
import java.math.RoundingMode
import java.text.DecimalFormat
import java.util.*
import java.util.function.Predicate
import java.util.function.Function
import java.util.function.Supplier
import java.util.stream.Collectors
import java.util.stream.Stream

class Interventions(val root: Box) : Box(), IO.Loaded {
    private var editorLoaded: Boolean = false

    internal var code_trackFactory = BoxDefaultCode.findSource(Interventions::class.java, "track")
    internal var code_keyframe = BoxDefaultCode.findSource(Interventions::class.java, "keyframe")

    var editor : RemoteEditor? = null;

    override fun loaded() {

        RunLoop.main.mainLoop.attach {
            val first = this.first(RemoteEditor.editor, both())
            Log.log("tap") { "is the editor loaded yet ? " + first }

            if (first.isPresent) {
                editorLoaded = true
                editor = first.get()

                editor!!.server.addHandlerLast({ x -> x == "interventions.changed" }, { s, socket, address, payload ->
                    val p = payload as JSONObject

                    val newlyAdded = p.getBoolean("newlyAdded")
                    if (!newlyAdded) {

                        val box = findBoxByID(p.getString("box"))

                        if (box.isPresent) {
                            val uid = p.getString("uid")
                            val typeName = p.getString("name")
                            val text = p.getString("textNow")

                            setKeyframeBox(box.get(), time(box.get()), uid, typeName, text)
                        }
                    }

                    println(" EDITOR IS LOADED ")
                    payload
                })

                println(" EDITOR IS NOT LOADED YET ")
                false
            } else
                true
        }

        timeSlider = (this both TimeSlider.time)

        properties.putToMap(Boxes.insideRunLoop, "main.__updateTimeSlider__", Supplier<Boolean> {

            val tn = time(this);
            val sel = selection()
            if (tn != timeWas || sel != selectionWas) {
                timeWas = tn
                selectionWas = sel

                if (sel.size==1)
                    updateInterventionsForBox(sel.first())
            }


            true
        });

        root.properties.put(Dict.Prop<TriFunctionOfBoxAnd<String, Number, Double>>("intervention"), TriFunctionOfBoxAnd<String, Number, Double> { box, name, cv ->

            val b = box.children().find { ("intervention." + name).equals(it.properties.get(Box.name)) }

            if (b != null)
                eval(b, cv.toDouble())
            else
                cv.toDouble()
        })
    }

    private fun updateInterventionsForBox(first: Box) {
        first.children.filter { (it get name)!!.startsWith("intervention.") }.forEach {
            val uid = (it get name)!!.split(Regex("\\."))[2]
            val value = eval(it, time(first))

            val df = DecimalFormat("#.###")
            val name = df.format(value)

            // throttle!!
            editor!!.sendJavaScript("setIntervention('"+uid+"', '"+name+"')")
        }
    }

    private fun selection(): Set<Box> {
        return breadthFirst(both()).filter { x -> x.properties.isTrue(Mouse.isSelected, false) }.collect(Collectors.toSet())
    }


    var timeWas = -1.0;
    var selectionWas: Set<Box> = Collections.emptySet();

    var timeSlider: TimeSlider? = null;

    private fun time(box: Box): Double = timeSlider!!.getTime(box)


    fun setKeyframeBox(parent: Box, time: Double, trackName: String, typeName: String, key: String) {

        val trac = parent.first(DefaultMenus.ensureChild)
                .get()
                .apply(parent, "intervention." + typeName + "." + trackName)

        if (trac.properties.isTrue(DefaultMenus.wasNew, false)) {
            trac.properties.put(Execution.code, code_trackFactory)
            root.disconnect(trac)
            trac.properties.put(DragToCopy._ownedByParent, true)

            // todo: layout

            (trac up begin)!!.apply(trac)
        }

        val box = ensureChildAtTime(trac, time)

        if (box.properties.isTrue(DefaultMenus.wasNew, false)) {

            root.disconnect(box)
            box.properties.put(DragToCopy._ownedByParent, true)
        }

        // todo, 3-way merge

        val codep = ShaderPreprocessor().preprocess(trac, code_keyframe,
                mutableMapOf(
                        "value" to Function<String, String> { key }
                ))

        box += Execution.code to codep


        (box up Chorder.begin)!!.apply(box)


    }

    private fun eval(box: Box, cv: Double): Double {
        val t = time(box)
        return box.properties.getOr(Taps.evalInterpolation, { Function<Double, Double> { cv } }).apply(t)
    }

    fun ensureChildAtTime(box: Box, time: Double): Box {
        val found = box.children.find { Math.abs(it.properties.get(Box.frame).x - time) < 1e-2 }
        if (found != null) {
            found += DefaultMenus.wasNew to false
            return found
        }

        val newBox = (box up DefaultMenus.newBox)?.apply(box)
        if (newBox != null) {
            newBox += Box.name to "__" + time
            newBox += DefaultMenus.wasNew to true
            newBox += Box.frame to Rect(time, (box up Box.frame)!!.yh + 5.0, 10.0, 10.0)

            // todo, layout group

            return newBox
        } else
            throw NullPointerException()
    }

    protected fun findBoxByID(uid: String): Optional<Box> {
        return breadthFirst(both()).filter { x -> Util.safeEq(x.properties.get(IO.id), uid) }
                .findFirst()
    }
}

private inline infix fun <T> Box.up(next: Dict.Prop<T>): T? {
    return this.find(next, this.upwards()).findFirst().orElseGet { null }
}

private inline infix fun <T> Box.down(next: Dict.Prop<T>): T? {
    return this.find(next, this.downwards()).findFirst().orElseGet { null }
}

private inline infix fun <T> Box.both(next: Dict.Prop<T>): T? {
    return this.find(next, this.both()).findFirst().orElseGet { null }
}
