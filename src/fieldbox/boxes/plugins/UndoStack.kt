package fieldbox.boxes.plugins

import field.app.RunLoop
import field.utility.Dict
import field.utility.Dict.Prop
import fieldbox.boxes.Box

class UndoStack {
    val maxLength = 100

    open class Undo(val description: String, val action: () -> Unit) {
        val time = System.currentTimeMillis()
    }

    class UndoProperty<T>(val box: Box, val prop: Prop<T>, val was: T) : Undo("$box.$prop = $was",
            {
                box.properties.put(prop, was)
            })

    var stack = mutableListOf<Undo>()
    var stackChanged = false

    fun <T> change(box: Box, prop: Prop<T>) {
        val v = box.properties.get(prop)
        if (v != null) {
            stack.add(UndoProperty(box, prop, v))
            stackChanged = true
        }
    }

    fun <T> change(box: Box, prop: Prop<T>, value: T) {
        val v = box.properties.get(prop)
        if (v != null) {
            stack.add(UndoProperty(box, prop, v))
            stackChanged = true
        }

        box.properties.put(prop, value);
    }

    fun coallesce() {
        if (!stackChanged) return

        val ord = LinkedHashMap<Triple<*, *, *>, Undo>()

        val dest = mutableListOf<Undo>()

        stack.groupingBy {
            when (it) {
                is UndoProperty<*> -> {
                    Triple(it.box, it.prop, quantize(it.time))
                }
                else -> {
                    Triple(0, 0, it.time)
                } // not mergeable
            }
        }.aggregateTo(ord, { key : Any, acc: Undo?, element : Long, first -> if (first) element else acc!! })
                .values.toCollection(dest)

        stack = dest

    }

    private fun quantize(time: Long): Long {
        return time / 1000
    }

    companion object {
        @JvmField
        val u  = UndoStack()

        init {
            RunLoop.main.mainLoop.attach(0, {
                u.coallesce()
            })
        }
    }
}