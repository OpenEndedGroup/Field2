package trace.video

import field.graphics.FastJPEG
import field.graphics.JPEGLoader
import field.graphics.SlowJPEG
import java.io.File
import java.io.FilenameFilter
import java.nio.ByteBuffer
import java.util.*
import kotlin.collections.Map.Entry
import java.util.function.Function

import field.utility.Dict
import field.utility.Options

class ImageCache(val width: Int, val height: Int, maxBuffer: Int, private val lookahead: Int, private var files: Function<Int, String>?) {

    private val p: Pipe
//    var jp = FastJPEG()

    internal var forwards = true

    init {

        p = object : Pipe(width, height) {
            internal var num = 0

            override fun createStorage(): ByteBuffer? {
                //				System.out.println(" creating storage <" + num + ">");
                if (num < maxBuffer) {
                    num++
                    return super.createStorage()
                } else
                    return null
            }

            override fun decompress(n: Entry<String, ByteBuffer>) {
                if (n.key == "--black--") {
                    synchronized(black) {
                        black.rewind()
                        n.value.put(black)
                        black.rewind()
                        n.value.rewind()
                    }
                } else {
                    Thread.currentThread().name = "decompression thread"
                    System.err.println(n.key + " " + n.value)
//                    jp.decompress(n.key, n.value, width, height)
                    FastJPEG.j.decompress(n.key, n.value, width, height)
                }
            }

            override fun check(): Int {
                val z = super.check()
                if (z != num) {
                    // throw new
                    // IllegalArgumentException(" failed check");
                    //					System.out.println(z + " != " + num);
                }
                return z
            }
        }
    }

    fun setFileMap(f: Function<Int, String>): ImageCache {
        this.files = f
        return this
    }

    fun prerollAndWait(frame: Int) {
        println(" prerolling ... ")
        val f = ArrayList<String>()
        for (i in 0 until lookahead - 2) {
            val n = files!!.apply(i + frame)
            if (n != null)
                f.add(n)
        }
        if (f.size > 0)
            p.preroll(*f.toTypedArray())

        try {
            Thread.sleep(250)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

    }

    fun fix(start: Int, end: Int) {
        val f = ArrayList<String>()
        for (i in start until end) {
            val n = files!!.apply(i)
            if (n != null)
                f.add(n)
        }
        if (f.size > 0)
            p.fix(*f.toTypedArray())
    }

    fun setPlaybackDirection(frame: Int, forwards: Boolean) {
        if (this.forwards && !forwards) {
            for (i in 0 until lookahead) {
                val fn = files!!.apply(frame - i - 1)
                if (fn != null)
                    p.schedule(fn)
            }
        } else {
            for (i in 0 until lookahead) {
                val fn = files!!.apply(frame + i + 1)
                if (fn != null)
                    p.schedule(fn)
            }
        }

        this.forwards = forwards
    }

    fun copy(frame: Int, out: ByteBuffer): Boolean {
        val o = files!!.apply(frame)
        if (o == null) {
            println(" NO FILE :" + frame)
            return false
        }

        //		synchronous = true;

        val oo = if (synchronous) p.getOrWait(o) else p.get(o)

        try {
            if (oo == null) {
                println(" did not load in time:$frame / $o")
                //                p.printStatus();

                if (synchronous)
                    System.err.println(" THIS CAN NEVER HAPPEN ")

                for (i in 0 until lookahead * 2) {
                    val fn = files!!.apply(frame - i - 1)
                    if (fn != null)
                        p.unschedule(fn)
                }

                for (i in 0 until lookahead) {
                    val fn = files!!.apply(frame + i)
                    if (fn != null)
                        p.schedule(fn)
                }


                return false
            }

            println(" loaded in time :$frame / $o")

            oo.rewind()
            out.put(oo)
            oo.rewind()

            val upcoming = LinkedHashSet<String>()
            for (i in 0 until lookahead) {
                val fn = files!!.apply(frame + i + 1)
                upcoming.add(fn)
            }
            for (i in 0 until lookahead * 2) {
                val fn = files!!.apply(frame - i - 2)
                if (fn != null && !upcoming.contains(fn))
                    p.unschedule(fn)
            }

            for (i in 0 until lookahead) {
                val fn = files!!.apply(frame + i + 1)
                if (fn != null)
                    p.schedule(fn)
            }

            return true
        } finally {
            p.free(o)
        }
    }

    class FileMap(var ff: Array<File?>) : Function<Int, String> {

        private var remap: Array<Int>

        init {
            remap = Array<Int>(ff.size, { it })
        }

        fun length(): Int {
            return remap.size
        }

        override fun apply(x: Int): String {
            var x = x
            if (x < 0) x = 0
            return ff[remap[x!! % remap.size]]!!.absolutePath
        }

        fun remap(num: Int, mapper: Function<Int, Int>) {
            remap = Array<Int>(ff.size, { mapper.apply(it) })
        }
    }

    companion object {

        var synchronous = Options.dict().isTrue(Dict.Prop<Int>("offline"), false)

        fun mapFromDirectory(dir: String, match: String): FileMap {

            val f = File(dir)
            val ff = f.listFiles { dir, name -> name.matches(match.toRegex()) }

            Arrays.sort(ff!!)

            return FileMap(ff)
        }

        fun mapFromDirectory(dir: String, match: String, dec: Int): FileMap {

            val f = File(dir)
            val ff = f.listFiles { dir, name -> name.matches(match.toRegex()) }

            Arrays.sort(ff!!)

            val q = arrayOfNulls<File>(ff.size / dec)
            for (i in q.indices) {
                q[i] = ff[i * dec]
            }

            return FileMap(q)
        }
    }

}
