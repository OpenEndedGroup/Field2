package trace.graphics

import field.graphics.BaseMesh
import field.linalg.Vec3
import java.io.*


import field.graphics.MeshBuilder
import field.linalg.Quat
import field.utility.remAssign
import field.utility.times
import marc.math.Flann
import org.lwjgl.util.vector.Quaternion

import java.io.BufferedReader
import java.io.File
import java.io.FileNotFoundException
import java.io.FileReader
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel.MapMode
import java.util.ArrayList


class FastLoadPly(val fn: String) {

    @JvmField
    var next : FastLoadPly? = null

    private var boundTo: BaseMesh? = null

    private var lp: LoadPly

    var filename: String? = null

    lateinit var positions: FloatBuffer
    lateinit var color: FloatBuffer
    lateinit var normal: FloatBuffer
    lateinit var size: FloatBuffer

    var dirty_positions = false
    var dirty_color = false
    var dirty_normal = false
    var dirty_size = false


    data class Point(val at: Vec3, val color: Vec3?, val normal: Vec3?) {
    }

    init {
        lp = LoadPly(fn)

        positions = ByteBuffer.allocateDirect(4 * lp.points.size * 3).order(ByteOrder.nativeOrder()).asFloatBuffer()
        color = ByteBuffer.allocateDirect(4 * lp.points.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        normal = ByteBuffer.allocateDirect(4 * lp.points.size * 3).order(ByteOrder.nativeOrder()).asFloatBuffer()
        size = ByteBuffer.allocateDirect(4 * lp.points.size * 1).order(ByteOrder.nativeOrder()).asFloatBuffer()

        lp.points.forEachIndexed { i, p ->

            positions.put(i * 3 + 0, p.at.x.toFloat())
            positions.put(i * 3 + 1, p.at.y.toFloat())
            positions.put(i * 3 + 2, p.at.z.toFloat())

            color.put(i * 4 + 0, p.color?.x?.toFloat() ?: 1f)
            color.put(i * 4 + 1, p.color?.y?.toFloat() ?: 1f)
            color.put(i * 4 + 2, p.color?.z?.toFloat() ?: 1f)
            color.put(i * 4 + 3, 1f)

            normal.put(i * 3 + 0, p.normal?.x?.toFloat() ?: 0f)
            normal.put(i * 3 + 1, p.normal?.y?.toFloat() ?: 0f)
            normal.put(i * 3 + 2, p.normal?.z?.toFloat() ?: 1f)

            size.put(i, 1f)
        }
    }


    var first = true

    fun show(layer: Stage.ShaderGroup, name: String) {
        val v = layer.pointBuilder(name)

        if (first) {
            v.open()
            for (i in 0 until positions.capacity() / 3) {
                v.aux(2, size.get(i))
                v.aux(1, color.get(4 * i + 0), color.get(4 * i + 1), color.get(4 * i + 2), color.get(4 * i + 3))
                v.aux(4, normal.get(3 * i + 0), normal.get(3 * i + 1), normal.get(3 * i + 2))
                v.aux(15, positions.get(3 * i + 0), positions.get(3 * i + 1), positions.get(3 * i + 2))
                v.v(positions.get(3 * i + 0), positions.get(3 * i + 1), positions.get(3 * i + 2))
            }
            v.close()
            first = false

            dirty_positions = false
            dirty_color = false
            dirty_normal = false
            dirty_size = false

            boundTo = v.target

        } else {

        }
    }

    fun showKnn(layer: Stage.ShaderGroup, name: String, k: Int) {
        val v = layer.lineBuilder(name)


        if (first) {
            val f = Flann()
            f.build3d(lp.points, { it.at })

            v.open()
            val num = positions.capacity() / 3
            for (i in 0 until num) {
                v.aux(2, size.get(i))
                v.aux(1, color.get(4 * i + 0), color.get(4 * i + 1), color.get(4 * i + 2), color.get(4 * i + 3))
                v.aux(4, normal.get(3 * i + 0), normal.get(3 * i + 1), normal.get(3 * i + 2))
                v.aux(15, positions.get(3 * i + 0), positions.get(3 * i + 1), positions.get(3 * i + 2))
                v.v(positions.get(3 * i + 0), positions.get(3 * i + 1), positions.get(3 * i + 2))
            }

            var index: IntArray? = null
            for (i in 0 until num) {
                index = f.find3(Vec3(positions.get(3 * i + 0), positions.get(3 * i + 1), positions.get(3 * i + 2)), k, index)!!

                for (q in 1 until index.size) {
                    if (index[q] > -1) {
                        v.e(num - 1 - i, num - 1 - index[q])
                    }
                }

            }
            v.close()
            first = false

            dirty_positions = false
            dirty_color = false
            dirty_normal = false
            dirty_size = false

            boundTo = v.target

        } else {

        }
    }

    fun planarRotate(center: Vec3, up: Vec3, amount: Float) {

        if (boundTo == null) return

        if (boundTo != null)
            positions = boundTo!!.vertex()

        val q = Quat().setAngleAxis(amount.toDouble(), up.normalize())

        for (i in 0 until lp.points.size) {
            var x = positions.get(3 * i + 0) - center.x
            var y = positions.get(3 * i + 1) - center.y
            var z = positions.get(3 * i + 2) - center.z

            var d = x * up.x + y * up.y + z * up.z
            if (d > 0) {

                val num = q.x * 2.0
                val num2 = q.y * 2.0
                val num3 = q.z * 2.0
                val num4 = q.x * num
                val num5 = q.y * num2
                val num6 = q.z * num3
                val num7 = q.x * num2
                val num8 = q.x * num3
                val num9 = q.y * num3
                val num10 = q.w * num
                val num11 = q.w * num2
                val num12 = q.w * num3

                val dx = (1.0 - (num5 + num6)) * x + (num7 - num12) * y + (num8 + num11) * z
                val dy = (num7 + num12) * x + (1.0 - (num4 + num6)) * y + (num9 - num10) * z
                val dz = (num8 - num11) * x + (num9 + num10) * y + (1.0 - (num4 + num5)) * z

                positions.put(3 * i + 0, (dx + center.x).toFloat())
                positions.put(3 * i + 1, (dy + center.y).toFloat())
                positions.put(3 * i + 2, (dz + center.z).toFloat())
            }
        }


        if (next!=null)
            next!!.planarRotate(center, up, amount)

    }

}
