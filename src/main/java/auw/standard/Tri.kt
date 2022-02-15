package auw.standard

class Tri : Sin() {
    override fun sin(d: Double): Double {
        val d2 = (d+Math.PI/2)%(2*Math.PI)
        return (if (d2 < Math.PI) d2 / Math.PI else 2 - d2 / Math.PI)*2 - 1
    }
}