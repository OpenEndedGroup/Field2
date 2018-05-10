package trace.graphics

import field.graphics.FLine
import field.graphics.FastJPEG
import field.graphics.StandardFLineDrawing
import field.utility.Vec4
import org.apache.commons.math3.analysis.FunctionUtils.sample
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.experimental.and

class PointCloudFromJPG(val fn: String) {
    private var data: ByteBuffer

    private var dim: IntArray

    init {
        dim = FastJPEG.j.dimensions(fn)
        data = ByteBuffer.allocateDirect(3 * dim[0] * dim[1]).order(ByteOrder.nativeOrder())
        FastJPEG.j.decompress(fn, data, dim[0], dim[1])
    }

    fun toFLine(f: FLine, scalex: Double, scaley: Double, samplesX: Int, samplesY: Int, height: Double) {
        for (y in 0 until samplesY) {
            val py = dim[1] * y / (samplesY - 1.0)
            for (x in 0 until samplesX) {
                val px = dim[0] * x / (samplesX - 1.0)

                val h = get_interpolated(px.toFloat(), py.toFloat(), 0)

                f.moveTo(scalex * px / dim[0], scaley * py / dim[1], h * height)
                f.node().attributes.put(StandardFLineDrawing.color, Vec4(h, h, h, 1))

            }
        }
    }


    operator fun get(x: Int, y: Int, c: Int): Float {
        var x = x
        var y = y
        if (x < 0) x = 0
        if (x >= dim[0]) x = dim[0] - 1
        if (y < 0) y = 0
        if (y >= dim[1]) y = dim[1] - 1
        return get_raw(x, y, c)
    }

    fun get_raw(x: Int, y: Int, c: Int): Float {
        return (data.get(y * 3 * dim[0] + x * 3 + c).toInt() and 0xff) / 255f
    }

    fun get_interpolated(x: Float, y: Float, i: Int): Float {

        val s00 = get(x.toInt(), y.toInt(), i)
        val s01 = get(x.toInt(), y.toInt() + 1, i)
        val s10 = get(x.toInt() + 1, y.toInt(), i)
        val s11 = get(x.toInt() + 1, y.toInt() + 1, i)

        val a = x - x.toInt()
        val b = y - y.toInt()

        return s00 * (1 - a) * (1 - b) + s01 * (1 - a) * b + s10 * a * (1 - b) + s11 * a * b
    }

}