package trace.sound

import field.graphics.FastJPEG
import field.graphics.FastThicken.Companion.t
import jm.util.Write
import java.io.File
import java.nio.ByteBuffer
import java.util.*
import kotlin.experimental.and

class Hard(val dir: String, val w: Int, var h: Int) {

    var oh = 1000;

    init {
        val j = File(dir).listFiles { f, n -> n.endsWith(".jpg") }
        Arrays.sort(j)

        var t = ByteBuffer.allocateDirect(w * h * 3);

        oh = 1000

        var f = FloatArray(j.size * oh * 2)
        j.forEachIndexed { index, file ->

            doStereo(f, index * oh, file, t)

        }

        postFilter(f, 2, 0)
        clamp(f, 2, 0)
        postFilter(f, 2, 1)
        clamp(f, 2, 1)
        Write.audio(f, dir + "stereo.wav", 2, 48000, 16)

        f = FloatArray(j.size * oh)
        j.forEachIndexed { index, file ->

            doMono(f, index * oh, file, t)

        }

        postFilter(f, 1, 0)
        clamp(f, 1, 0)
        Write.audio(f, dir + "mono.wav", 1, 48000, 16)
    }

    private fun clamp(f: FloatArray, step: Int, offset: Int) {
        var max = Float.NEGATIVE_INFINITY
        var min= Float.POSITIVE_INFINITY
        var index = offset;
        while(index<f.size)
        {
            max = Math.max(max, f[index])
            min = Math.min(min, f[index])
            index += step
        }

        print(" range is $min -> $max")

        if (max!=min) {
            index = offset;
            while (index < f.size) {
                f[index] = -0.5f+(f[index] - min) / (max - min)
                index += step
            }
        }
    }

    private fun postFilter(f: FloatArray, step: Int, offset: Int) {
        var index = offset;
        var lp = 0.0;
        val alpha = 0.9999
        while(index<f.size)
        {
            lp = lp*alpha+(1-alpha)*f[index]
//            println("$lp ...")
            f[index] -= lp.toFloat()
            index += step
        }
    }

    private fun doStereo(f: FloatArray, i: Int, file: File, t: ByteBuffer) {
        t.rewind()
        FastJPEG.j.decompress(file.absolutePath, t, w, h);
        for (y in 12 until oh+12) {
            var a = 0.0
            var ax = 0.0

            for (x in 0 until w) {

                val r = t[3 * y * w + 3 * x + 0].toInt() and 0xff
                val g = t[3 * y * w + 3 * x + 1].toInt() and 0xff
                val b = t[3 * y * w + 3 * x + 2].toInt() and 0xff
                val v = (r + g + b) / 3.0
                a += v
                ax += v * (x - w / 2)
            }


            if (a > 0)
                ax /= a

            a /= w;

            ax /= w / 2

            a/=255;

//            println("$a $ax")


            f[i * 2 + 2*(y-12) + 0] = a.toFloat() * Math.max(0.0, ax+1).toFloat()
            f[i * 2 + 2*(y-12) + 1] = a.toFloat() * Math.max(0.0, -ax+1).toFloat()
        }
    }

    private fun doMono(f: FloatArray, i: Int, file: File, t: ByteBuffer) {
        t.rewind()
        FastJPEG.j.decompress(file.absolutePath, t, w, h);
        for (y in 12 until oh+12) {
            var a = 0.0
            var ax = 0.0

            for (x in 0 until w) {

                val r = t[3 * y * w + 3 * x + 0].toInt() and 0xff
                val g = t[3 * y * w + 3 * x + 1].toInt() and 0xff
                val b = t[3 * y * w + 3 * x + 2].toInt() and 0xff
                val v = (r + g + b) / 3.0
                a += v
                ax += v * (x - w / 2)
            }

            a /= w;
            if (a > 0)
                ax /= a;

            ax /= w / 2

            a/=255;


            f[i + (y-12)] = a.toFloat()
        }
    }

}