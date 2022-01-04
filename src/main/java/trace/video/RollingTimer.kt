package trace.video

import field.app.RunLoop
import java.util.*

class RollingTimer(private val name: String, private val historySize: Int, private val printEvery: Float) {

    internal var history = LinkedList<Number>()

    internal var lastPrintedAt = System.currentTimeMillis()

    internal var disabled = false

    init {

        RunLoop.main.mainLoop.attach { x ->
            update()
            true
        }
    }

    @Synchronized
    fun add(n: Number) {
        history.add(n)
        if (history.size > historySize)
            history.removeAt(0)
    }

    fun update() {
        if (disabled) return

        if ((System.currentTimeMillis() - lastPrintedAt) / 1000L > printEvery) {
            print()
            lastPrintedAt = System.currentTimeMillis()
        }
    }

    private fun print() {
        if (history.size == 0) return
        var max = java.lang.Float.NEGATIVE_INFINITY
        var min = java.lang.Float.POSITIVE_INFINITY
        var average = 0f

        val c = ArrayList<Number>(history.size)
        synchronized(this) {
            c.addAll(history)
        }

        for (n in c) {
            average += n.toFloat()
            if (n.toFloat() > max) max = n.toFloat()
            if (n.toFloat() < min) min = n.toFloat()
        }

        average /= c.size.toFloat()

        System.err.println("#$name <$max -> $average -> $min>")
    }


}
