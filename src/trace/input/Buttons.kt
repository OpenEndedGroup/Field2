package trace.input

import field.app.RunLoop
import field.graphics.Window
import field.utility.IdempotencyMap
import org.lwjgl.glfw.GLFW

import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.*
import java.util.function.Supplier

/**
 * Created by marc on 2/28/17.
 */
class Buttons : Runnable {

    private val nameMap = LinkedHashMap<String, Int>()
    private val nameMapBackwards = LinkedHashMap<Int, String>()

    @JvmField
    val edge_down = IdempotencyMap<Supplier<Boolean>>(Supplier::class.java)
    @JvmField
    val while_down = IdempotencyMap<Supplier<Boolean>>(Supplier::class.java)
    @JvmField
    val edge_up = IdempotencyMap<Supplier<Boolean>>(Supplier::class.java)

    protected var downEvents: MutableSet<Int> = LinkedHashSet()
    protected var upEvents: MutableSet<Int> = LinkedHashSet()
    protected var down: MutableSet<Int> = LinkedHashSet()

    private var on: Window? = null

    fun attachToWindow(on: Window) {
        on.addKeyboardHandler(this::state)
        on.addMouseHandler(this::mouseState)
        this.on = on

        val f = GLFW::class.java.declaredFields
        for (ff in f) {
            if (Modifier.isStatic(ff.modifiers) && ff.name.startsWith("GLFW_KEY_")) {
                try {
                    nameMap.put(ff.name.replace("GLFW_KEY_", "").toLowerCase(), ff.getInt(null))
                    nameMapBackwards.put(ff.getInt(null), ff.name.replace("GLFW_KEY_", "").toLowerCase())
                } catch (e: IllegalAccessException) {
                    e.printStackTrace()
                }
            }
        }

        nameMap.put("mouse0", -1);
        nameMap.put("mouse1", -2);
        nameMap.put("mouse2", -3);
        nameMapBackwards.put(-1, "mouse0");
        nameMapBackwards.put(-2, "mouse1");
        nameMapBackwards.put(-3, "mouse2");

    }


    private fun state(keyboardStateEvent: Window.Event<Window.KeyboardState>): Boolean {
        var a = LinkedHashSet(keyboardStateEvent.after.keysDown)
        var b = LinkedHashSet(keyboardStateEvent.before.keysDown)
        a.addAll(keyboardStateEvent.after.charsDown.values.map { nameMap[it.toString()] }.filter { it!=null })
        b.addAll(keyboardStateEvent.before.charsDown.values.map { nameMap[it.toString()] }.filter { it!=null })

        a.removeAll(b)
        downEvents.addAll(a)
        down.addAll(a)
        a = LinkedHashSet(keyboardStateEvent.after.keysDown)
        b = LinkedHashSet(keyboardStateEvent.before.keysDown)
        a.addAll(keyboardStateEvent.after.charsDown.values.map { nameMap[it.toString()] }.filter { it!=null })
        b.addAll(keyboardStateEvent.before.charsDown.values.map { nameMap[it.toString()] }.filter { it!=null })
        b.removeAll(a)
        upEvents.addAll(b)
        down.removeAll(b)
        return true
    }

    private fun mouseState(mouseEventState: Window.Event<Window.MouseState>): Boolean {
        var a = LinkedHashSet(mouseEventState.after.buttonsDown.map { x -> -x - 1 })
        var b = LinkedHashSet(mouseEventState.before.buttonsDown.map { x -> -x - 1 })
        a.removeAll(b)
        downEvents.addAll(a)
        down.addAll(a)
        a = LinkedHashSet(mouseEventState.after.buttonsDown.map { x -> -x - 1 })
        b = LinkedHashSet(mouseEventState.before.buttonsDown.map { x -> -x - 1 })
        b.removeAll(a)
        upEvents.addAll(b)
        down.removeAll(b)

        return true
    }


    override fun run() {
        val ii = down.iterator()
        while (ii.hasNext()) {
            if (on != null && !on!!.currentKeyboardState.keysDown.contains(ii.next()))
                ii.remove()
        }

        for (d in downEvents) {
            val name = nameMapBackwards[d]
            if (name != null) {
                val q = edge_down[name]
                if (q != null && !q.get())
                    edge_down.remove(name)
            } else {
                System.err.println(" unknown key pressed :" + d)
            }
        }
        for (d in down) {
            val name = nameMapBackwards[d]
            if (debug) {
                println("down :$d -> $name")
            }
            if (name != null) {
                val q = while_down[name]
                if (q != null && !q.get())
                    edge_down.remove(name)
            } else {
                System.err.println(" unknown key pressed :" + d)
            }
        }

        for (d in upEvents) {
            val name = nameMapBackwards[d]
            if (name != null) {
                val q = edge_up[name]
                if (q != null && !q.get())
                    edge_down.remove(name)
            } else {
                System.err.println(" unknown key pressed :" + d)
            }
        }

        upEvents.clear()
        downEvents.clear()
    }

    companion object {

        @JvmField
        var debug = false
    }
}
