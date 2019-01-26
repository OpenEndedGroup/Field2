package trace.sound

import java.util.*

class Events(val f: DoubleArray) {

    val fs = DoubleArray(f.size)

    init {
        System.arraycopy(f, 0, fs, 0, f.size)
        fs.sort()
    }

    var timeWas = 0.0

    fun reset(timeNow: Double) {
        timeWas = timeNow
    }

    fun read(timeNow: Double): List<Double> {
        var index1 = fs.binarySearch(timeWas)
        var index2 = fs.binarySearch(timeNow-1e-6)
        timeWas = timeNow-1e-6
        if (index1 == index2) return Collections.emptyList<Double>()

        if (index1 < 0) index1 = -index1 - 1
        if (index2 < 0) index2 = -index2 - 1

        index1 = Math.max(0, Math.min(index1, fs.size-1))
        index2 = Math.max(0, Math.min(index2, fs.size))

        return fs.slice(index1..index2-1)
    }
}