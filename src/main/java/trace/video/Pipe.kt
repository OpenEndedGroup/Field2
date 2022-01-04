package trace.video

import field.utility.Dict
import field.utility.Options
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.collections.Map.Entry

open class Pipe(private val width: Int, private val height: Int) {

    internal var threads = Options.dict().getOr(Dict.Prop<Number>("decompressThreads")) { 12 }.toInt()

    private val pool: ExecutorService

    protected var unallocated = LinkedList<ByteBuffer>()

    protected var fixed = HashMap<String, ByteBuffer>()

    var decompressed = HashMap<String, ByteBuffer>()
    protected var decompressing = LinkedHashMap<String, ByteBuffer>()
    protected var inflight = LinkedHashMap<String, ByteBuffer>()
    protected var locked = HashMap<String, ByteBuffer>()
    protected var scavangable: LinkedHashMap<String, ByteBuffer> = object : LinkedHashMap<String, ByteBuffer>() {
        override fun removeEldestEntry(eldest: Map.Entry<String, ByteBuffer>?): Boolean {

            if (this.size > 20) {
                synchronized(decompressing) {

                    unallocated.add(eldest!!.value)

                    check()
                    return true
                }
            }

            return false

        }
    }

    internal var decompresTime = RollingTimer("decompressTime", 100, 5f)
    internal var forward = RollingTimer("forward", 100, 5f)

    var black: ByteBuffer =ByteBuffer.allocateDirect(width * height * 3)

    private val storage: ByteBuffer?
        get() = synchronized(decompressing) {
            return if (unallocated.size > 0) {
                val rr = unallocated.removeAt(0)
                scavangable.values.remove(rr)
                rr
            } else {
                createStorage()
            }
        }

    init {
        pool = Executors.newFixedThreadPool(threads)

        var previous = System.currentTimeMillis()
        for (i in 0 until threads) {
            pool.execute {
                while (true) {
                    var n: Entry<String, ByteBuffer>?  = null

                    synchronized(decompressing) {
                        while (decompressing.size == 0) {
                            try {
                                (decompressing as Object).wait()
                            } catch (e: InterruptedException) {
                                e.printStackTrace()
                            }

                        }
                        val e = decompressing.entries.iterator()
                        n = e.next()
                        e.remove()
                        inflight.put(n!!.key, n!!.value)
                    }
                    //						System.out.println(" starting file :" + n.getKey());

                    val `in` = System.currentTimeMillis()
                    decompress(n!!)
                    val out = System.currentTimeMillis()
                    decompresTime.add((out - `in`) / 1000f)

                    synchronized(decompressing) {
                        if (decompressed.containsKey(n!!.key)) {
                            // System.out.println(" simultanous decompress?");
                            inflight.remove(n!!.key)
                            unallocated.add(n!!.value)
                        } else {
                            //								System.out.println(" finished file :" + n.getKey());
                            decompressed.put(n!!.key, n!!.value)
                            inflight.remove(n!!.key)
                        }
                    }


                    if (i==0 && System.currentTimeMillis()-previous>2000)
                    {
                        printStatus()
                        previous = System.currentTimeMillis()
                    }

                }
            }
        }
    }

    fun printStatus() {
        synchronized(decompressing) {
            println(" decompressed: ")
            var es = decompressed.entries
            for ((key) in es) {
                println("    " + key)
            }
            println(" decompressing: ")
            es = decompressing.entries
            for ((key) in es) {
                println("    " + key)
            }
            println(" inflight: ")
            es = inflight.entries
            for ((key) in es) {
                println("    " + key)
            }
            println(" locked: ")
            es = locked.entries
            for ((key) in es) {
                println("    " + key)
            }
            println(" scavange: ")
            es = scavangable.entries
            for ((key) in es) {
                println("    " + key)
            }
        }
    }

    protected open fun decompress(n: Map.Entry<String, ByteBuffer>) {

    }

    fun preroll(vararg filenames: String) {
        for (s in filenames) {
            println(" prerolling :" + s)
            schedule(s)
        }
        while (decompressing.size > 0 || inflight.size > 0) {
            try {
                Thread.sleep(100)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }

        }
        println(" after preroll:")
        printStatus()
    }

    fun fix(vararg filenames: String) {
        for (s in filenames) {
            println(" fixing :" + s)
            schedule(s)
        }
        for (s in filenames) {
            val g = getOrWait(s)
            val copy = createStorage()!!
            println(" taking local copy of <$g> to <$copy>")
            copy.rewind()
            g!!.rewind()
            copy.put(g)
            copy.rewind()
            g.rewind()
            fixed.put(s, copy)
            free(s)
            check()
        }
    }

    operator fun get(filename: String): ByteBuffer? {
        // System.out.println(" get :" + filename);
        if (fixed.containsKey(filename)) {
// System.out.println(" -- fixed <"+q+">");
            return fixed[filename]
        }
        synchronized(decompressing) {
            if (scavangable.containsKey(filename)) {
                val r = scavangable.remove(filename)
                unallocated.remove(r)
                decompressed.put(filename, r!!)
                locked.put(filename, r)
                check()
                return r
            }
            if (decompressed.containsKey(filename)) {
                val r = decompressed[filename]
                locked.put(filename, r!!)

                forward.add(decompressed.size)
                check()

                return r
            }
            //			System.out.println(" no :"+filename+" in "+scavangable+" or "+decompressed);
        }


        return null
    }

    fun getOrWait(filename: String): ByteBuffer? {
        if (fixed.containsKey(filename))
            return fixed[filename]

        if (!decompressed.containsKey(filename)) {
            schedule(filename)
        }


        val q = get(filename)
        if (q != null)
            return q

        while (!decompressed.containsKey(filename)) {

            println(" -- waiting :" + filename)

            try {
                Thread.sleep(10)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }

            synchronized(decompressing) {

                if (decompressed.containsKey(filename)) {
                    val r = decompressed[filename]
                    locked.put(filename, r!!)

                    check()

                    return r
                } else
                    schedule(filename)

            }
        }

        synchronized(decompressing) {

            if (decompressed.containsKey(filename)) {
                val r = decompressed[filename]
                locked.put(filename, r!!)

                check()

                return r
            }
        }
        return null

    }

    protected open fun check(): Int {

        // System.out.println(" num buffers is <" + (unallocated.size()
        // + decompressed.size() + decompressing.size() +
        // inflight.size()) + "> with <" + locked.size() + "> locked");

        return unallocated.size + decompressed.size + decompressing.size + inflight.size + fixed.size + scavangable.size
    }

    fun schedule(filename: String) {

        if (fixed.containsKey(filename))
            return

        //		System.out.println(" schedule <" + filename + ">");

        synchronized(decompressing) {
            check()
            if (decompressing.containsKey(filename)) {
                				System.out.println(" already scheduled");
                //				System.err.println("          decompressed :" + decompressed.size() + "  " + decompressed.keySet());
                return
            }
            if (inflight.containsKey(filename)) {
                				System.out.println(" already in flight");
                return
            }
            if (decompressed.containsKey(filename)) {
//                				System.out.println(" already done ");
                return
            }
            if (scavangable.containsKey(filename)) {
                				System.out.println(" rescavanged ");
                val q = scavangable.remove(filename)

                unallocated.remove(q)

                decompressed.put(filename, q!!)
                return
            }

            var b: ByteBuffer? = storage
            if (b != null) {
                decompressing.put(filename, b)
                (decompressing as Object).notify()
                check()
                // System.out.println(" decompressing is :" +
                // decompressing + ">");
            } else {
                if (scavangable.size > 0) {
                    val k = scavangable.keys.iterator().next()
                    val storage = scavangable[k]
                    scavangable.remove(k)
                    unallocated.remove(storage)
                    decompressing.put(filename, storage!!)
                    (decompressing as Object).notify()
                    check()
                } else {

                    System.err.println(" out of storage <" + filename + "> in thread " + Thread.currentThread())
                    System.err.println("          decompressing :" + decompressing.size + " " + decompressing.keys)
                    System.err.println("          decompressed :" + decompressed.size + "  " + decompressed.keys)
                    System.err.println("          inflight:" + inflight.size + "  " + inflight.keys)
                    System.err.println("          locked :" + locked.size)
                    System.err.println("          unallocated :" + unallocated.size)
                    System.err.println("          scavange :" + scavangable.size)

                    System.err.println(" binning everything and starting again")

                    unscheduleEverything()
                    b = storage
                    if (b != null) {
                        decompressing.put(filename, b)
                        (decompressing as Object).notify()
                        check()
                    } else {

                        System.err.println(" even after unscheduling everything still cannot get storage ")

                        return
                    }
                }
            }
        }
    }

    fun unschedule(filename: String) {
        synchronized(decompressing) {
            if (locked.containsKey(filename))
                return
            var m: ByteBuffer? = decompressing.remove(filename)

            //			System.out.println(" unschedule <" + filename + ">");

            if (m != null)
                unallocated.add(m)
            if (decompressed.containsKey(filename)) {
                m = decompressed.remove(filename)
                unallocated.add(m!!)
            }
            check()
        }
    }

    fun unlockEverything() {
        synchronized(decompressing) {
            for (filename in ArrayList(locked.keys)) {
                free(filename)
                check()
            }
        }
    }

    fun unscheduleEverything() {
        synchronized(decompressing) {
            for (filename in ArrayList(decompressed.keys)) {
                if (locked.containsKey(filename))
                    continue
                decompressing.remove(filename)
                if (decompressed.containsKey(filename)) {
                    val m = decompressed.remove(filename)
                    unallocated.add(m!!)
                }
                check()
            }
        }
    }

    open protected fun createStorage(): ByteBuffer? {
        return ByteBuffer.allocateDirect(3 * width * height)
    }

    fun free(filename: String) {
        synchronized(decompressing) {

            val m = locked.remove(filename)
            if (m != null) {
                decompressed.remove(filename)
//                scavangable.put(filename, m)
                unallocated.add(m)

            } else {
                // System.err.println(" free without lock ? :" +
                // filename + " " + locked);
            }
        }

    }

    fun freeNoReturn(filename: String) {
        synchronized(decompressing) {

            val m = locked.remove(filename)
            if (m != null) {
                // unallocated.add(m);
                val q = decompressed.remove(filename)
                if (q !== m) {
                    println(" decompressed and locked are not the same!!!")
                }
                check()

            } else {
                // System.err.println(" free without lock ? :" +
                // filename + " " + locked);
            }
        }

    }

}
