package auw.standard

import auw._FBuffer

class Amplitude {
    fun apply(f: _FBuffer): Double {
        var q = 0.0
        val ff = f.get()
        for (n in 0 until ff.a.capacity()) {
            q += ff.a[n] * ff.a[n]
        }
        return Math.pow(q, 0.5)
    }
}
