package trace.util

import field.graphics.FLine
import field.utility.IdempotencyMap

class LinePiper<T>(val head: T, val construct: () -> T, val duplicate: (T) -> T, LATENCY: Int) {

    val stack = MutableList<T>(LATENCY, { construct() })

    init {
        stack.add(0, head)
    }

    fun update() {
        // move head forward
        for (x in stack.size - 1 downTo 2) {
            stack[x] = stack[x - 1]
        }

        stack[1] = duplicate(stack[0])
    }

    fun read(): T {
        return stack.last()
    }

    fun tail(): T {

        return stack.last()

    }

    fun tailUpdate(): T {

        update()
        return stack.last()

    }

}