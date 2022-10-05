package fieldkotlin

import java.util.concurrent.BlockingDeque
import java.util.concurrent.LinkedBlockingDeque
import kotlin.concurrent.thread
import kotlin.experimental.ExperimentalTypeInference

public inline fun <T, R> Iterable<T>.pmap(crossinline transform: (T) -> R): List<R> =
    this.toList().stream().parallel().map { transform(it) }.toList()

class Output<T>(val q: Int) {
    val queue = LinkedBlockingDeque<T>(q)
    val total = LinkedBlockingDeque<T>()
    val complete = ArrayList<T>()

    @Volatile
    var killed = false

    @Volatile
    var running = false

    var lastReturnAt = 0L

    operator fun invoke(max: Int = 0, delay: Double = 0.0): List<T> {

        if ((System.currentTimeMillis() - lastReturnAt) / 1000.0 < delay) return emptyList()

        var r = mutableListOf<T>()

        var o = queue.poll()
        if (o != null) {
            r.add(o)
            while (r.size < max || max == 0) {
                var o = queue.poll()
                if (o == null) break
                r.add(o)
            }
        }

        if (r.size > 0)
            lastReturnAt = System.currentTimeMillis()

        return r
    }

    operator fun invoke(): T? {
        var q = invoke(1)
        if (q.size == 1) return q[0]
        return null
    }


}

@OptIn(ExperimentalTypeInference::class)
fun <T> fromThread(
    queue: Int = 10,
    retain: Int = 0,
    @BuilderInference t: suspend SequenceScope<T>.() -> Unit
): Output<T> {

    if (PhaseList.context == null)
        throw java.lang.IllegalArgumentException(" can't `threaded` outside of a phaselist")

    var o = Output<T>(queue)

    thread {
        var s = sequence(t).iterator()
        o.running = true
        while (!o.killed && s.hasNext()) {
            if (s.hasNext()) {
                var n = s.next()
                if (!o.killed) {
                    if (retain > 0) {
                        if (o.total.size - retain > 0)
                            o.total.drop(o.total.size - retain)
                        if (o.queue.size - retain > 0)
                            o.queue.drop(o.queue.size - retain)
                    }
                    o.queue.putLast(n)
                    o.total.putLast(n)
                }
            }
        }
        o.running = false
    }

    PhaseList.context?.let {
        it.endHooks.put("threaded") {
            println(" -- endHooks called ")
            o.killed = true
        }
    }

    return o
}
