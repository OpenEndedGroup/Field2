package field.app

import field.utility.AsMapDelegator
import field.utility.Dict
import field.utility.IdempotencyMap
import field.utility.Options
import fieldlinker.AsMap
import java.util.concurrent.*
import java.util.function.Consumer

/**
 * Created by marc on 7/23/17.
 */
class ThreadSync2 {

    interface TrappedReturn {
        fun notifyReturn(a: Any?)
    }

    interface TrappedExecutorName {
        fun executionNamePrefix(): String
    }


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

    open class KilledException : RuntimeException()
    class TooLongKilledException(override val message: String) : RuntimeException()

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

        @JvmField
        var d = Dict()

        var customExecutor : ExecutorService? = null

        @Volatile
        @JvmField
        var finished = false
        @Volatile
        @JvmField
        var killed = false

        @Volatile
        @JvmField
        var shouldEnd = false

        @Volatile
        @JvmField
        var endingFor = 0

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

            if (customExecutor!=ThreadSync2.executor) throw IllegalArgumentException(" can't wait() on this thread (are you inside the audio thread?) ")

            output.put(license)
            license = null

            d.put(__yieldAtFrame, RunLoop.tick);

            if (killed) throw KilledException()

            serviceAlso(also)

            try {

//                license = input.take()
                var pl: Any? = null
                while (pl == null) {
                    pl = input.poll(1, TimeUnit.SECONDS)
                    if (pl == null) {
                        System.err.println(" -- waited in ${Thread.currentThread().name} for 5 seconds inside yield(), still waiting --")

//                        throw IllegalArgumentException("asdfasdF")
                    }
                }

                license = pl
            } catch (e: InterruptedException) {

            }

            if (killed) throw KilledException()
        }


        @Volatile
        @JvmField
        var lastReturn: Any? = null;

        @JvmOverloads
        fun launch(license: Any?, r: Callable<*>, e: ExecutorService = executor) {
            this.license = license
            e.submit {
                thisFibre.set(this)
                thisThread = Thread.currentThread()

//                println(" \n\n\n\n THIS THREAD = $thisThread \n\n\n\n")

                try {
                    lastReturn = r.call()

                    if (e is TrappedReturn)
                        e.notifyReturn(lastReturn)

                    while (serviceAlso(also))
                        yield()
                } catch (t: TooLongKilledException) {
                    if (errorHandler != null)
                        errorHandler!!.accept(t)
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

    @Volatile
    internal var license: Any? = "-- license -- "

    fun service(): Boolean {
        val didWork = current.size > 0

        for (f in ArrayList(current)) {
            if (f.paused) continue

            if (f.finished)
                current.remove(f)
            else {
                val ll = license
                license = null

//                f.input.put(ll)

                while (!f.input.offer(ll, 5, TimeUnit.SECONDS)) {
                    System.err.println(" waited 5 seconds to fibre $f from ${Thread.currentThread()}")
                }

                license = f.output.take()
            }

            if (f.pauseNext) {
                f.pauseNext = false
                f.paused = true
            }
        }

        return didWork
    }

    fun launchAndServiceOnce(r: Callable<*>, e: ExecutorService = executor): Fibre? {
        // call only from "main thread"
        val f = Fibre()
        f.customExecutor = e
        val ll = license
        license = null
        f.launch(ll, r, e)
        license = f.output.take()
        if (!f.finished) {
            current.add(f)
            return f
        }
        return null
    }

    @JvmOverloads
    fun launchAndServiceOnce(debugText: String, r: Callable<*>, t: Consumer<Throwable>, e: ExecutorService = executor): Fibre? {
        // call only from "main thread"
        val f = Fibre()
        f.customExecutor = e
        f.debugText = debugText
        f.errorHandler = t
        val ll = license
        license = null
        f.launch(ll, r, e)
        license = f.output.take()
        if (!f.finished) {
            current.add(f)
            return f
        }
        return f
    }


    companion object {

        @JvmStatic
        val __yieldAtFrame = Dict.Prop<Long>("__yieldAtFrame")
        @JvmStatic
        val __maybeYieldAtFrame = Dict.Prop<Long>("__maybeYieldAtFrame")

        @JvmStatic
        val enabled = Options.dict().isTrue(Dict.Prop<Boolean>("thread2"), true)

        init {
        }

        @JvmStatic
        var sync: ThreadSync2? = null
        var thisFibre = ThreadLocal<Fibre>()

        @JvmStatic
        val executor = Executors.newCachedThreadPool()

        @JvmStatic
        fun yield() {
            thisFibre.get().yield()
        }

        @JvmStatic
        fun yieldIfPossible() {
            thisFibre.get()?.yield()
        }

        @JvmStatic
        fun fibre(): Fibre? {
            return thisFibre.get()
        }

        @JvmStatic
        @Throws(Exception::class)
        fun <T> callInMainThreadAndWait(c: Callable<T>): T {
            if (Thread.currentThread() === RunLoop.main.mainThread) {
                println("||||||||||||||||||||||||||||||| already main thread")
                return c.call()
            } else {

                println(" current thread :" + Thread.currentThread() + " " + RunLoop.main.mainThread)

                val f = CompletableFuture<T>()
                println("||||||||||||||||||||||||||||||| call once ")
                RunLoop.main.once {
                    try {
                        f.complete(c.call())
                    } catch (t: Throwable) {
                        f.completeExceptionally(t)
                    }
                }

                // should be rejoin?
                println("||||||||||||||||||||||||||||||| begin wait ")
                while (!f.isDone) {
                    println("||||||||||||||||||||||||||||||| yield to main loop")
                    yield()
                    println("||||||||||||||||||||||||||||||| back from main loop")
                }
                println("||||||||||||||||||||||||||||||| f is :" + f)

                return f.get()
            }
        }


        var debug = false;

        // kotlin interface
        @JvmStatic
        fun <T> inMainThread(c: () -> T): T {
            if (Thread.currentThread() === RunLoop.main.mainThread) {
                if (debug) println("||||||||||||||||||||||||||||||| already main thread")
                return c()
            } else {

                if (debug) println(" current thread :" + Thread.currentThread() + " " + RunLoop.main.mainThread)

                val f = CompletableFuture<T>()
                if (debug) println("||||||||||||||||||||||||||||||| call once ")
                RunLoop.main.once {
                    try {
                        f.complete(c())
                    } catch (t: Throwable) {
                        f.completeExceptionally(t)
                    }
                }

                // should be rejoin?
                if (debug) println("||||||||||||||||||||||||||||||| begin wait ")
                while (!f.isDone) {
                    if (debug) println("||||||||||||||||||||||||||||||| yield to main loop")
                    yield()
                    if (debug) println("||||||||||||||||||||||||||||||| back from main loop")
                }
                if (debug) println("||||||||||||||||||||||||||||||| f is :" + f)

                return f.get()
            }
        }


    }


}
