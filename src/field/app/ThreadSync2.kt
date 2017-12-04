package field.app

import field.utility.AsMapDelegator
import field.utility.Dict
import field.utility.IdempotencyMap
import field.utility.Options
import fieldlinker.AsMap

import java.util.*
import java.util.concurrent.*
import java.util.function.*

/**
 * Created by marc on 7/23/17.
 */
class ThreadSync2 {

    // _.p = line( ... , ... , ...)   // trap that on asMap_set( ... )

    interface TrappedSet<T> : Iterator<T?>, Iterable<T?> {
        fun reset()

        override fun iterator(): Iterator<T?> {
            reset()
            return this
        }
    }


    var mainThread: Thread

    init {
        mainThread = Thread.currentThread()
        sync = this
    }

    class KilledException : RuntimeException()

    inner class Fibre : AsMapDelegator() {
        override fun delegateTo(): AsMap {
            return d
        }

        val input: BlockingQueue<Any> = LinkedBlockingDeque<Any>(1)
        val output: BlockingQueue<Any> = LinkedBlockingDeque<Any>(1)

        var debugText: String? = null
        var errorHandler: Consumer<Throwable>? = null
        var thisThread: Thread? = null

        var also = IdempotencyMap<TrappedSet<*>>(TrappedSet::class.java)

        var onExit: Runnable? = null

        var d = Dict()

        @Volatile
        @JvmField
        var finished = false
        @Volatile
        @JvmField
        var killed = false
        @Volatile
        @JvmField
        var paused = false
        @Volatile
        @JvmField
        var pauseNext = false

        @Volatile
        @JvmField
        var tag: Any? = null

        @Volatile
        @JvmField
        var license: Any? = null

        fun yield() {
            output.put(license)
            license = null

            if (killed) throw KilledException()

            serviceAlso(also)

            try {
                license = input.take()
            } catch (e: InterruptedException) {

            }

            if (killed) throw KilledException()
        }


        @Volatile
        @JvmField
        var lastReturn: Any? = null;

        fun launch(license: Any?, r: Callable<*>) {
            this.license = license
            executor.submit {
                thisFibre.set(this)

                try {
                    lastReturn = r.call()
                    while (serviceAlso(also))
                        yield()
                } catch (t: KilledException) {

                } catch (t: Throwable) {
                    System.err.println(" exception thrown in fibre '$debugText'")
                    t.printStackTrace()
                    if (errorHandler != null)
                        errorHandler!!.accept(t)
                    else {
                    }
                }

                finished = true
                if (license != null)
                    output.put(license)

                if (onExit != null) {
                    try {
                        onExit!!.run()
                    } catch (t: Throwable) {
                        System.err.println(" exception thrown in fibre's exit handler '$debugText'")
                        t.printStackTrace()
                        if (errorHandler != null)
                            errorHandler!!.accept(t)
                        else {
                        }
                    }
                }

                thisFibre.set(null)
            }
        }

        private fun serviceAlso(also: IdempotencyMap<ThreadSync2.TrappedSet<*>>): Boolean {

            if (also.size == 0) return false

            val vv = also.entries
            for (n in ArrayList(vv)) {
                try {
                    if (!n.value.hasNext()) {
                        vv.remove(n)
                    } else {
                        val v = n.value.next()
                        if (v == null) {
                            vv.remove(n)
                        }
                    }
                } catch (t: Throwable) {
                    println(" exception throw in fibre running also ${n.key} (will be removed)")
                    vv.remove(n)
                }
            }

            return also.size > 0
        }

        fun addAlso(name: String, t: TrappedSet<*>) {
            also.put(name, t)
        }
    }


    internal var current: MutableList<Fibre> = ArrayList()

    fun getFibres(): List<Fibre> {
        return ArrayList(current)
    }

    @Volatile internal var license: Any? = "-- license -- "

    fun service(): Boolean {
        val didWork = current.size > 0

        for (f in ArrayList(current)) {
            if (f.paused) continue

            if (f.finished)
                current.remove(f)
            else {
                val ll = license
                license = null
                f.input.put(ll)
                license = f.output.take()
            }

            if (f.pauseNext) {
                f.pauseNext = false
                f.paused = true
            }
        }

        return didWork
    }

    fun launchAndServiceOnce(r: Callable<*>): Fibre? {
        // call only from "main thread"
        val f = Fibre()
        val ll = license
        license = null
        f.launch(ll, r)
        license = f.output.take()
        if (!f.finished) {
            current.add(f)
            return f
        }
        return null
    }

    fun launchAndServiceOnce(debugText: String, r: Callable<*>, t: Consumer<Throwable>): Fibre? {
        // call only from "main thread"
        val f = Fibre()
        f.debugText = debugText
        f.errorHandler = t
        val ll = license
        license = null
        f.launch(ll, r)
        license = f.output.take()
        if (!f.finished) {
            current.add(f)
            return f
        }
        return f
    }


    companion object {
        @JvmStatic
        val enabled = Options.dict().isTrue(Dict.Prop<Boolean>("thread2"), false)
        @JvmStatic
        var sync: ThreadSync2? = null
        var thisFibre = ThreadLocal<Fibre>()

        private val executor = Executors.newCachedThreadPool()

        @JvmStatic
        fun yield() {
            thisFibre.get().yield()
        }

        @JvmStatic
        fun fibre(): Fibre? {
            return thisFibre.get()
        }
    }

}
