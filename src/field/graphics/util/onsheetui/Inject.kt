package field.graphics.util.onsheetui

import com.google.common.primitives.Floats
import field.app.RunLoop
import field.utility.*
import field.utility.Util.safeEq
import fieldbox.DefaultMenus
import fieldbox.boxes.Box
import fieldbox.boxes.Boxes
import fieldbox.boxes.Callbacks
import fieldbox.boxes.plugins.*
import fieldbox.boxes.plugins.Chorder.begin
import fieldbox.execution.Execution
import fieldbox.io.IO
import fieldlinker.AsMap

import java.util.LinkedHashMap
import java.util.function.BiConsumer
import java.util.function.Function

/**
 * needed, a quick way of making these on demand
 */
class Inject(val r: Box) {

    val width = 300.0
    val height = 14.0

    val code_slider = BoxDefaultCode.findSource(this.javaClass, "makeSlider")
    val code_checkbox = BoxDefaultCode.findSource(this.javaClass, "makeCheckbox")

    fun edit(propertyName: String, on: Box) {
        val pp = Dict.Prop<Any>(propertyName).toCanon<Any>()
        val v = (on up pp)
        when (v) {
            is Number -> {
                val rng = pp.attributes.getOr(Dict.range, { null })
                if (rng != null) {
                    val i = rng.iterator()
                    makeSlider(propertyName, on, (i.next() as Number).toFloat(), (i.next() as Number).toFloat())
                } else
                    makeSlider(propertyName, on, 0f, v.toFloat() * 2);
            }
            is Boolean -> {
                makeCheckbox(propertyName, on)
            }
            else -> {
                throw IllegalArgumentException("don't know how to edit " + propertyName + " with value " + v + " " + (if (v == null) null else v.javaClass))
            }
        }
    }

    fun makeCheckbox(propertyName: String, on: Box) {
        val uigroup = UIGroup.getOrMake("propertiesUI", on)

        val d = Dict.Prop<Boolean>(propertyName).toCanon<Boolean>()
        if (!on.find(d, on.upwards()).findFirst().isPresent) {
            on += d to true
        }

        val canvas = (on up DefaultMenus.ensureChildOfClass)!!.apply(on, "." + propertyName, SimpleCanvas::class.java)

        // TODO: suspend all this on window.disable ?
        d.attributes.putToMap(Missing.watch, "__uiGroup__" + propertyName, BiConsumer<Box, Any?> { inside, oldValue ->
            if (inside == on && !safeEq(oldValue, inside.properties.get(d)) && !inside.disconnected) {
                (inside get Pseudo.next)!!["update_" + propertyName] = Runnable {
                    (canvas up Exec.exec)?.apply(canvas, (canvas get Execution.code))
                }
            }
        })

        (canvas get Callbacks.onDelete)!!.put("__uiGroup__" + propertyName, Box.FunctionOfBox { box ->
            if (box == canvas) {
                d.attributes.removeFromMap(Missing.watch, "__uiGroup__" + propertyName)
                uigroup.remove(canvas)
            }
        })

        canvas += Box.name to "." + propertyName
        canvas += Boxes.dontSave to true
        canvas += Dispatch.shyConnections to true
        if (canvas get DefaultMenus.wasNew == true) {
            val aa = uigroup.initialPositionDown()
            canvas += Box.frame to Rect(uigroup.spotx + uigroup.margin / 2 + 2, aa.toDouble(), height, height)
        }

        (canvas as IO.Loaded).loaded()

        uigroup.add(canvas)

        val codep = ShaderPreprocessor().preprocess(canvas, code_checkbox,
                mutableMapOf(
                        "property" to Function { propertyName }
                ))

        canvas += Execution.code to codep

        (canvas up Exec.exec)?.apply(canvas, codep)

    }

    fun makeSlider(propertyName: String, on: Box, start: Float, end: Float) {
        val uigroup = UIGroup.getOrMake("propertiesUI", on)

        val d = Dict.Prop<Number>(propertyName).toCanon<Number>()
        if (!on.find(d, on.upwards()).findFirst().isPresent) {
            on += d to (start + end) / 2
        }

        val canvas = (on up DefaultMenus.ensureChildOfClass)!!.apply(on, "." + propertyName, SimpleCanvas::class.java)

        // TODO: suspend all this on window.disable ?
        d.attributes.putToMap(Missing.watch, "__uiGroup__" + propertyName, BiConsumer<Box, Object> { inside, oldValue ->
            if (inside == on && oldValue !== inside.properties.get(d) && !inside.disconnected) {
                (inside get Pseudo.next)!!["update_" + propertyName] = Runnable {
                    (canvas up Exec.exec)?.apply(canvas, (canvas get Execution.code))
                }
            }
        })

        (canvas get Callbacks.onDelete)!!.put("__uiGroup__" + propertyName, Box.FunctionOfBox { box ->
            if (box == canvas) {
                d.attributes.removeFromMap(Missing.watch, "__uiGroup__" + propertyName)
                uigroup.remove(canvas)
            }
        })

        canvas += Box.name to "." + propertyName
        canvas += Boxes.dontSave to true
        canvas += Dispatch.shyConnections to true
        if (canvas get DefaultMenus.wasNew == true) {
            val aa = uigroup.initialPositionDown()
            canvas += Box.frame to Rect(uigroup.spotx + uigroup.margin / 2 + 2, aa.toDouble(), width, height)
        }

        (canvas as IO.Loaded).loaded()

        uigroup.add(canvas)

        val codep = ShaderPreprocessor().preprocess(canvas, code_slider,
                mutableMapOf(
                        "property" to Function { propertyName },
                        "startRange" to Function { "" + start },
                        "endRange" to Function { "" + end }
                ))

        canvas += Execution.code to codep

        (canvas up Exec.exec)?.apply(canvas, codep)

//        Gizmos().makeCloseBox(canvas, relative = Vec2(1,0.5), frame = Rect(height*2, -height/2, height, height))
    }


}

infix fun <T> AsMap.get(p: Dict.Prop<T>): T? {
    return this.asMap_get(p.name) as T
}

private inline infix fun <T> Box.up(next: Dict.Prop<T>): T? {
    return this.find(next, this.upwards()).findFirst().orElseGet { null }
}
