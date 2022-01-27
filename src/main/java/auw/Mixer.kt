package auw

import auw.simple.Oscs
import field.app.ThreadSync2
import field.graphics.util.Saver
import fieldbox.boxes.Box
import fieldbox.boxes.Boxes
import fieldbox.execution.Execution
//import jm.util.Write
import java.io.File
import java.lang.IllegalArgumentException
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import java.util.concurrent.*

class Mixer(val root: Box) {

    companion object {
        val globalSound = Sound().init(5)

        @JvmStatic
        var saving = false

        var saveBuffer = FloatArray(0)
        var index = 0

        @JvmStatic
        fun startSaving() {
            saving = true
            saveBuffer = FloatArray(0)
            index = 0
        }

        @JvmStatic
        fun finishSaving(): String? {
            if (!saving) {
                throw IllegalArgumentException(" can't call finish saving if you haven't called startSaving")
            }

            saving = false;

            val base =
                System.getProperty("user.home") + File.separatorChar + "Desktop" + File.separatorChar + "field_audio_recordings" + File.separatorChar

            var x = 1
            while (File(base + Saver.pad(x)).exists()) x++

            val prefix = File(base + Saver.pad(x))
            prefix.mkdirs()

//            Write.audio(saveBuffer, File(prefix, "mixer.wav").absolutePath, 1, IO.sampleRate, 16);

            return File(prefix, "mixer.wav").absolutePath
        }

        fun copyToSaveBuffer(a: FloatBuffer) {

            a.clear()

            if (index + a.capacity() > saveBuffer.size) {

                val o = saveBuffer
                saveBuffer = FloatArray((o.size * 1.5).toInt() + a.limit())
                System.arraycopy(o, 0, saveBuffer, 0, index)
                System.out.println(" growing save buffer to ${saveBuffer.size}")
            }

            a.get(saveBuffer, index, a.capacity())
            a.clear()
            index += a.capacity()

        }
    }

    val running = mutableListOf<Box>()
    val tools = BoxTools()

    val max_data = RollingTimer("max_data", 50, 5f)
    val min_data = RollingTimer("min_data", 50, 5f)
    val average_data = RollingTimer("average_data", 50, 5f)
    val rmsAverage_data = RollingTimer("rmsAverage_data", 50, 5f)

    val callable = mutableListOf<Triple<Box, Callable<out Any?>, CompletableFuture<out Any?>>>()
    val always = mutableListOf<_FBuffer>()

    val executor = Executor()

    val lib = Library()

    val oscs = Oscs()

    init {
        always.add(oscs)
    }


    inner class Executor : ExecutorService, ThreadSync2.TrappedReturn, ThreadSync2.TrappedExecutorName {
        val prefix = "audio-" + System.identityHashCode(this) + "."
        override fun executionNamePrefix(): String {
            return prefix
        }

        var ret: Any? = null

        override fun notifyReturn(a: Any?) {
            ret = a
        }

        override fun shutdown() {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun <T : Any?> submit(task: Callable<T>?): Future<T> {
            val c = CompletableFuture<T>()
            synchronized(callable) {
                callable.add(Triple(Execution.context.get().peek(), task!!, c))
            }
            return c
        }

        override fun <T : Any?> submit(task: Runnable?, result: T): Future<T> {

            val c = CompletableFuture<T>()
            synchronized(callable) {
                callable.add(Triple(Execution.context.get().peek(), Callable {
                    task!!.run()
                    result
                }, c))
            }
            return c

        }

        override fun submit(task: Runnable?): Future<*> {
            return submit(task, null)
        }

        override fun shutdownNow(): MutableList<Runnable> {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun isShutdown(): Boolean {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun awaitTermination(timeout: Long, unit: TimeUnit?): Boolean {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun <T : Any?> invokeAny(tasks: MutableCollection<out Callable<T>>?): T {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun <T : Any?> invokeAny(
            tasks: MutableCollection<out Callable<T>>?,
            timeout: Long,
            unit: TimeUnit?
        ): T {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun isTerminated(): Boolean {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun <T : Any?> invokeAll(tasks: MutableCollection<out Callable<T>>?): MutableList<Future<T>> {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun <T : Any?> invokeAll(
            tasks: MutableCollection<out Callable<T>>?,
            timeout: Long,
            unit: TimeUnit?
        ): MutableList<Future<T>> {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun execute(command: Runnable?) {
            submit(command, null)
        }

    }

    val io = object : IO(globalSound) {
        override fun yield() {
            Thread.sleep(5)
        }

        override fun process(result: ShortBuffer) {
            zero(result)
//            tools.output = result
            BoxTools.stack.get().push("/").use {
                val accumulator = BoxTools.stack.get().allocate()
                BufferTools.zero(accumulator)
                synchronized(running) {
                    ArrayList(running)
                }.forEach {

                    try {
                        val c = BoxTools.stack.get().allocate()

                        val kill = !BoxTools.runBox(it.properties.getOrConstruct(fieldbox.io.IO.id), c, it)

                        if (kill)
                            running.remove(it)

                        BufferTools.add(accumulator, c, accumulator)
                    } catch (e: Throwable) {
                        System.err.println("inside audio thread ")
                        e.printStackTrace()

                        // todo, better than that?
                    }
                }

                val a = synchronized(callable) {
                    val r = ArrayList(callable)
                    callable.clear()
                    r
                }

                a.forEach {
                    try {
                        val c = BoxTools.stack.get().allocate()

                        executor.ret = null
                        BoxTools.runBox(
                            it.first.properties.getOrConstruct(fieldbox.io.IO.id),
                            c,
                            it.first,
                            it.second,
                            { executor.ret })
                        it.third.complete(null)

                        BufferTools.add(accumulator, c, accumulator)
                    } catch (e: Throwable) {
                        System.err.println("inside audio thread ")
                        e.printStackTrace()

                        // todo, better than that?
                    }
                }

                always.forEach {
                    BufferTools.add(accumulator, it.get(), accumulator)
                }

                root.forEach {
                    val x = it.properties.get(Boxes.insideRunLoop)
                    if (x == null || x.size == 0) return@forEach

                    val r = x.entries.iterator()
                    while (r.hasNext()) {
                        val n = r.next()
                        try {
                            if (n.key.startsWith(executor.prefix))
                                try {
                                    val c = BoxTools.stack.get().allocate()

                                    executor.ret = null
                                    if (!BoxTools.runBox(
                                            it.properties.getOrConstruct(fieldbox.io.IO.id),
                                            c,
                                            it,
                                            n.value
                                        )
                                    )
                                        r.remove()

                                    BufferTools.add(accumulator, c, accumulator)
                                } catch (e: Throwable) {
                                    System.err.println("inside audio thread ")
                                    e.printStackTrace()

                                    // todo, better than that?
                                }
                        } catch (t: Throwable) {
                            t.printStackTrace()
                            try {
                                r.remove()
                            } catch (tt: Throwable) {
                            }

                        }

                    }
                }

//                BufferTools.convert(accumulator, result)
                val data = BufferTools.convertWithStats(accumulator.a, result)

                min_data.add(data[0])
                average_data.add(data[1])
                rmsAverage_data.add(data[2])
                max_data.add(data[3])


                if (saving) {
                    copyToSaveBuffer(accumulator.a)
                }
            }
        }
    }


    init {
        var t = Thread {
            io.launch()
        }

        t.name = "AUW Audio Output Thread"
        t.start()
    }

    fun start(b: Box) {
        synchronized(running) {
            running.add(b) // handle fade up and down over one vector size
        }
    }

    fun stop(b: Box) {
        synchronized(running) {
            running.remove(b) // handle fade up and down over one vector si
        }
    }

}