package trace.input

import field.app.RunLoop
import field.graphics.Window
import field.graphics.Window.Event
import field.utility.IdempotencyMap
import org.jetbrains.kotlin.psi.Call
import java.util.concurrent.Callable
import java.util.function.Consumer

class SimpleMouse : java.util.function.Function<Event<Window.MouseState>, Boolean> {

    val onMouseDown = IdempotencyMap<Consumer<Window.MouseState>>(Consumer::class.java)
    val onMouseUp = IdempotencyMap<Consumer<Window.MouseState>>(Consumer::class.java)
    val onMouseMove = IdempotencyMap<Consumer<Window.MouseState>>(Consumer::class.java)
    val onMouseDrag = IdempotencyMap<Consumer<Window.MouseState>>(Consumer::class.java)

    val down = mutableSetOf<Int>()

    override fun apply(mouseEventState: Event<Window.MouseState>): Boolean {
        val downEvents = mutableSetOf<Int>()
        val upEvents = mutableSetOf<Int>()

        var a = LinkedHashSet(mouseEventState.after.buttonsDown)
        var b = LinkedHashSet(mouseEventState.before.buttonsDown)
        a.removeAll(b)
        downEvents.addAll(a)
        down.addAll(a)
        a = LinkedHashSet(mouseEventState.after.buttonsDown)
        b = LinkedHashSet(mouseEventState.before.buttonsDown)
        b.removeAll(a)
        upEvents.addAll(b)
        down.removeAll(b)


        RunLoop.main.once {
            downEvents.forEach {
                onMouseDown.values.forEach {
                    it.accept(mouseEventState.after)
                }
            }

            upEvents.forEach {
                onMouseUp.values.forEach {
                    it.accept(mouseEventState.after)
                }
            }

            if (mouseEventState.after.x != mouseEventState.before.x || mouseEventState.after.y != mouseEventState.before.y) {
                if (down.size > 0) {
                    onMouseDrag.values.forEach {
                        it.accept(mouseEventState.after)
                    }
                } else {
                    onMouseMove.values.forEach {
                        it.accept(mouseEventState.after)
                    }
                }
            }
        }
        return true
    }

}