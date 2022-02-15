package auw.standard

import auw._FBuffer

class Peak {
    fun apply(f: _FBuffer): Double {
        var q = 0f
        val ff = f.get()
        for (n in 0 until ff.a.capacity()/2) {
            val z = Math.abs(ff.a[n])
            if (z>q)
                q = z

            if (n<5)
                System.out.println("$n = $z")

        }
        return q.toDouble()
    }
}
