package auw.standard

class Saw : Sin() {
    override fun sin(d: Double): Double {
        val d2 = d%(2*Math.PI)
        if (d2 < Math.PI) return d2 / Math.PI else return -(2 - d2 / Math.PI)
    }
}