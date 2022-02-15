package auw.standard

class Sqr : Sin() {
    override fun sin(d: Double): Double {
        if ((d%(2*Math.PI)) < Math.PI) return 1.0 else return -1.0
    }
}