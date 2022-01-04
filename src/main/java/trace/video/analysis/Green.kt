package trace.video.analysis

import field.graphics.FastJPEG
import trace.video.Analysis
import java.io.File
import java.nio.ByteBuffer
import java.util.*

class Green : Analysis.Fast {

    val j = FastJPEG()
    override fun perform(data: List<File>): List<List<Float>> {
        var b: ByteBuffer? = null

        return data.map {

            val dim = j.dimensions(it.absolutePath)

            if (b == null || b!!.capacity() != 3 * dim[0] * dim[1]) {
                b = ByteBuffer.allocateDirect(3 * dim[0] * dim[1])
            }
            j.decompress(it.absolutePath, b, dim[0], dim[1])

            val f = process(b!!)
            Collections.singletonList(f)
        }.toList()
    }

    fun process(b: ByteBuffer): Float {

        var tot = 0

        for (x in 0 until b.capacity() / 3) {
            val r = b.get(x * 3 + 0).toInt() and 0xff
            val g = b.get(x * 3 + 1).toInt() and 0xff
            val bl = b.get(x * 3 + 2).toInt() and 0xff

            tot += g
        }
        return tot / (255f * b.capacity())

    }

}

