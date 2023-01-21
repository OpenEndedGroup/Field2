package trace.util

import field.graphics.FLine
import field.linalg.Vec3
import java.util.LinkedList

class Perpendicularize(val steps: Int = 50) {

    var c = 0
    var h = mutableListOf<MutableList<Vec3>>()

    var touched = mutableSetOf<Int>()

    fun start() {
        c = 0
    }

    fun clear() {
        h.clear()
    }

    @JvmOverloads
    fun add(d: FLine, strobe: Int = 1) {

        (0 until d.nodes.size step strobe).forEach {
            add(c + it / strobe, d.nodes[it].to)
        }

        c += d.nodes.size / strobe

    }

    fun get(): FLine {
        val f = FLine()
        var index = 0
        h.removeIf {
            f.breakNext()
            it.forEach {
                f.lineTo(it)
            }

            index++

            if (!touched.contains(index - 1)) {
                if (it.size>0)
                    it.removeAt(0)
                it.size == 0
            } else
                if (it.size > steps) {
                    it.removeAt(0)
                    false
                } else
                    false
        }
        c = 0
        touched.clear()
        return f
    }

    private fun add(index: Int, to: Vec3) {
        while (h.size <= index) h.add(LinkedList())
        h[index].add(to)
        touched.add(index)
    }


}