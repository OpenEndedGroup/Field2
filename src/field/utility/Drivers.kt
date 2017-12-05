package field.utility

import field.app.ThreadSync2

class Drivers {

    companion object {

        @JvmStatic
        fun provokeCurrentFibre(name: String, t: ThreadSync2.TrappedSet<*>) {
            if (ThreadSync2.enabled) {
                val tt = ThreadSync2.thisFibre.get() ?: throw IllegalArgumentException(" not currently in a Fibre")
                tt.addAlso(name, t)
            }
        }

        @JvmStatic
        fun <T> iteratorAsTrappedSet(i: Iterator<T>): ThreadSync2.TrappedSet<T> {
            return object : ThreadSync2.TrappedSet<T> {
                override fun reset() {

                }

                override fun next(): T? {
                    if (!i.hasNext()) return null
                    return i.next()
                }

                override fun hasNext(): Boolean {
                    return i.hasNext()
                }
            }
        }

        @JvmStatic
        fun <T> iterableAsTrappedSet(i: Iterable<T>): ThreadSync2.TrappedSet<T> {
            return object : ThreadSync2.TrappedSet<T> {
                override fun hasNext(): Boolean {
                    return q.hasNext()
                }

                var q = i.iterator()

                override fun next(): T? {
                    if (!q.hasNext()) return null
                    return q.next()
                }

                override fun reset() {
                    q = i.iterator()
                }
            }
        }

        @JvmStatic
        fun provokeCurrentFibre(name: String, filter: (Any?) -> Any?, t: ThreadSync2.TrappedSet<*>) {
            if (ThreadSync2.enabled) {
                val tt = ThreadSync2.thisFibre.get() ?: throw IllegalArgumentException(" not currently in a Fibre")
                tt.addAlso(name, object : ThreadSync2.TrappedSet<Any?> {
                    override fun next(): Any? {
                        val a = t.next()
                        val b = filter(a)
                        return b
                    }

                    override fun hasNext(): Boolean {
                        return t.hasNext()
                    }

                    override fun reset() {
                        t.reset()
                    }
                })
            }
        }

        val linear : (Double) -> Double = {it}

        @JvmOverloads
        @JvmStatic
        fun line(start: Double, end: Double, over: Int = 100, ease : (Double) -> Double = {it}): ThreadSync2.TrappedSet<Double> {
            return object : ThreadSync2.TrappedSet<Double> {
                var tick = 0
                var lastValue = start;

                override fun next(): Double? {
                    val d = start + (end - start) * ease(tick / over.toDouble())
                    lastValue = d
                    tick++
                    return if (tick < over + 2) d else null
                }

                override fun hasNext(): Boolean {
                    return tick < over + 1
                }

                override fun reset() {
                    tick = 0
                }

                override fun toString(): String = "line, $start -> $end / $over frames; at $lastValue"
            }
        }

        @JvmOverloads
        @JvmStatic
        fun <T> repeat(t: ThreadSync2.TrappedSet<T>, times: Int = 10): ThreadSync2.TrappedSet<T> {
            return object : ThreadSync2.TrappedSet<T> {
                var tick = 0
                override fun next(): T? {
                    var v = t.next()
                    if (v == null) {
                        tick++
                        if (tick < times) {
                            t.reset()
                            v = t.next()
                        }
                    }
                    return v
                }

                override fun hasNext(): Boolean {
                    return tick < times;
                }

                override fun reset() {
                    t.reset()
                    tick = 0;
                }
            }
        }
    }

}