package auw.signal

import auw.Sound
import java.nio.FloatBuffer

class Buffer(val fn: String) {
    var floats: FloatBuffer

    init {
        floats = Sound.floatsFromFile(fn)!!
    }

    override fun toString(): String {
        return "Buffer|$fn|"
    }

    fun get(sample: Double): Double {
        val a = Math.max(1.0, Math.min(floats.capacity() - 2.0, sample - 1))
        val left = a.toInt()
        val right = sample.toInt() + 1

        val alpha = a - left

        return floats.get(left) * (1 - alpha) + alpha * floats.get(right)
    }

}