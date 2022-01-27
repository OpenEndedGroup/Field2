package auw

import java.util.ArrayList
import java.util.LinkedList

import field.app.RunLoop

class RollingTimer(private val name: String, private val historySize: Int, private val printEvery: Float, val warnOnStall: Boolean = false) {

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
        count++
    }

    fun run(b: () -> Unit) {
        val a = System.nanoTime()
        try {
            b()
        } finally {
            val e = System.nanoTime()
            add((e - a) / 1000000.0)
        }
    }

    fun update() {
        if (disabled) return

        if ((System.currentTimeMillis() - lastPrintedAt) / 1000L > printEvery) {
            print()
            lastPrintedAt = System.currentTimeMillis()
        }
    }

    var lastPrintedAtCount = -1L
    var count = 0L

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

        System.err.println("#$name <$max -> $average -> $min> " + (if (warnOnStall && lastPrintedAtCount == count) red("**STALLED**") else ""))

        lastPrintedAtCount = count
    }

    private fun red(s: String): String {
        return "\u001b[31m" + s + "\u001b[0m"
    }


}
