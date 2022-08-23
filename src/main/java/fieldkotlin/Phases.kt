package fieldkotlin

import org.jetbrains.kotlin.serialization.js.ast.JsAstProtoBuf.Throw
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

    companion object {
        var context: PhaseList? = null
    }

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

    var endHooks = mutableMapOf<String, () -> Unit>()

    override fun get(): Boolean {
        return update(endNext)
    }

    override fun accept(t: Boolean) {
        endNext = true
    }

    var exitHook: () -> Unit = {}

    class EndNow : RuntimeException()


    fun update(end: Boolean): Boolean {
        context = this
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
                    var e = endHooks
                    endHooks = mutableMapOf()
                    e.forEach {
                        it.value()
                    }
                }
                3 -> {
                    exitHook()
                    return false
                }
            }
            return status != 3
        } catch (e: EndNow) {
            status = 3
            exitHook()
            return false
        } catch (e : Throwable)
        {
            println(" exception throw in state $status, ending this phaselist")
            e.printStackTrace()
            status = 4
            exitHook()
            throw(e)
        }
        finally {
            context = null
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
            val a = try {
                i.hasNext()
            } catch (e: Throwable) {
                throw(e)
            }
            if (a)
                i.next()
            else
                throw PhaseList.EndNow()
        }
    }
}
