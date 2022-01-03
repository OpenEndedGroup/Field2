package fieldkotlin

// like a sequence, but has a time
class TemporalSequence(val apply: suspend SequenceScope<Any>.() -> Unit) {

    companion object {
        val _time = ThreadLocal<Double>()

        var _t: Double
            get() = _time.get()
            set(n: Double) = _time.set(n)


        suspend fun SequenceScope<Any>.yield(): Double {
            yield(true)
            return _t
        }

        fun ts(apply: suspend SequenceScope<Any>.() -> Unit): TemporalSequence {
            return TemporalSequence(apply)
        }
    }

    var seq = sequence(apply).iterator()

    fun start() {
        seq = sequence(apply).iterator()
    }

    var lastReturn: Any? = null
    fun next(t: Double): Boolean {
        TemporalSequence._t = t
        lastReturn = seq.next()

        // do something cool with lastReturn? stack machine?
        // return special things to loop?
        // return special things to exit?
        // pipe those through helpers above?
        return seq.hasNext()
    }


}