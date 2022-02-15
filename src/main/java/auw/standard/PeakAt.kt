package auw.standard

import auw._FBuffer

class PeakAt {
    fun apply(f: _FBuffer): Double {
        var q = 0f
        val ff = f.get()
        var at = 0
        for (n in 0 until ff.a.capacity()/2) {
            val z = Math.abs(ff.a[n])
            if (z > q) {
                q = z
                at = n
            }
        }
        return at.toDouble()
    }
}
