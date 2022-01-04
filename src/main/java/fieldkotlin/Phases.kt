package fieldkotlin

import java.util.function.Consumer
import java.util.function.Supplier

enum class Phases {
    begin, cont, end
}

/**
 * we may well need these to be a) reusable and b) delegatable
 * since compile times are likely to stall render threads right now.
 *
 * Let's not go nuts on this, let's use coroutines instead
 */
class PhaseList : Supplier<Boolean>, Consumer<Boolean> {

    val children = mutableMapOf<Phases, () -> Unit>()

    fun initPhase(tag: Phases, init: () -> Unit) {
        children[tag] = init
    }

    var status = 0

    fun reset() {
        status = 0
        endNext = false
    }

    var endNext = false

    override fun get(): Boolean {
        return update(endNext)
    }

    override fun accept(t: Boolean) {
        endNext = true
    }

    class EndNow : RuntimeException()

    fun update(end: Boolean): Boolean {
        _ended = end
        try {
            when (status) {
                0 -> {
                    status = 1
                    val c = children[Phases.begin]
                    if (c != null) c()
                    update(end)
                }
                1 -> {
                    if (end) {
                        status = 2
                    }
                    val c = children[Phases.cont]
                    if (c != null) c()
                    if (end) {
                        update(end)
                    }
                }
                2 -> {
                    status = 3
                    val c = children[Phases.cont]
                    if (c != null) {
                        c()
                    }
                }
                3 -> {

                }
            }
            return status != 3
        } catch (e: EndNow) {
            status = 3
            return false
        }
    }


    fun begin(init: () -> Unit) = initPhase(Phases.begin, init)

    fun cont(init: () -> Unit) = initPhase(Phases.cont, init)

    fun end(init: () -> Unit) = initPhase(Phases.end, init)


}


fun phases(init: PhaseList.() -> Unit): PhaseList {
    val phases = PhaseList()
    phases.init()
    return phases
}

fun frame(m: () -> Unit): PhaseList {
    return phases {
        cont {
            m()
        }
    }
}


fun frames(m: suspend SequenceScope<Any>.() -> Unit): PhaseList {
    return phases {
        val s = sequence<Any>(m)
        lateinit var i: Iterator<Any>
        begin {
            i = s.iterator()
        }
        cont {
            if (i.hasNext())
                i.next()
            else
                throw PhaseList.EndNow()
        }
    }
}
