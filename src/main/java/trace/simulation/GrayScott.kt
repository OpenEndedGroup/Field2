package trace.simulation

import field.graphics.Shader
import field.graphics.Texture
import field.utility.Rect
import java.lang.Math.min
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class GrayScott(val width: Int, val height: Int, val isTiling: Boolean) {

    @JvmField
    var u: FloatBuffer
    @JvmField
    protected var u_source: ByteBuffer
    @JvmField
    var v: FloatBuffer
    @JvmField
    protected var v_source: ByteBuffer

    @JvmField
    var uu: FloatBuffer
    @JvmField
    var vv: FloatBuffer

    @JvmField
    var f: Float = 0.toFloat()
    @JvmField
    var k: Float = 0.toFloat()

    @JvmField
    var dU: Float = 0.toFloat()
    @JvmField
    var dV: Float = 0.toFloat()


    init {
        var q = floatBuffer(width * height)
        this.u = q.first
        this.u_source = q.second

        q = floatBuffer(u.capacity())
        this.v = q.first
        this.v_source = q.second

        this.uu = floatBuffer(u.capacity()).first
        this.vv = floatBuffer(u.capacity()).first

        reset()
        // default config
        setCoefficients(0.023f, 0.077f, 0.16f, 0.08f)
    }

    fun attachTextureSources(s: Shader, a: Int, b: Int, nameA: String, nameB: String): Runnable {
        val spec_u = Texture.TextureSpecification.float1(a, width, height, u_source)
        val texture_u = Texture(spec_u)
        val spec_v = Texture.TextureSpecification.float1(b, width, height, v_source)
        val texture_v = Texture(spec_v)

        s.asMap_set(nameA, texture_u);
        s.asMap_set(nameB, texture_v);

        return Runnable {
            texture_u.upload(u_source, true)
            texture_v.upload(v_source, true)
        }
    }


    private fun floatBuffer(size: Int): Pair<FloatBuffer, ByteBuffer> {
        var b = ByteBuffer.allocateDirect(4 * size).order(ByteOrder.nativeOrder())
        return b.asFloatBuffer() to b
    }

    /**
     * Convenience method to access U array using 2D coordinates.
     *
     * @param x
     * @param y
     * @return current u value at the given position
     */
    fun getCurrentUAt(x: Int, y: Int): Float {
        return if (y >= 0 && y < height && x >= 0 && x < width) {
            u[y * width + x]
        } else 0f
    }

    /**
     * Convenience method to access V array using 2D coordinates.
     *
     * @param x
     * @param y
     * @return current v value at the given position
     */
    fun getCurrentVAt(x: Int, y: Int): Float {
        return if (y >= 0 && y < height && x >= 0 && x < width) {
            v[y * width + x]
        } else 0f
    }


    /**
     * Extension point for subclasses to modulate the F coefficient of the
     * reaction diffusion, based on spatial (or other) parameters. This method
     * is called for every cell/pixel of the simulation space from the main
     * [.update] cycle and can be used to create parameter
     * gradients, animations and other spatial or temporal modulations.
     *
     * @param x
     * @param y
     * @return the active F coefficient at the given position
     */
    fun getFCoeffAt(x: Int, y: Int): Float {
        return f.toFloat()
    }


    /**
     * Extension point for subclasses to modulate the K coefficient of the
     * reaction diffusion, based on spatial (or other) parameters. This method
     * is called for every cell/pixel of the simulation space and can be used to
     * create parameter gradients, animations and other spatial or temporal
     * modulations.
     *
     * @param x
     * @param y
     * @return the active K coefficient at the given position
     */
    fun getKCoeffAt(x: Int, y: Int): Float {
        return k
    }


    /**
     * Resets the simulation matrix to an initial, clean state.
     */
    fun reset() {
        for (i in 0 until uu.capacity()) {
            uu[i] = 1.0f
            vv[i] = 0.0f
        }
    }

    fun seedImage(pixels: IntArray, imgWidth: Int, imgHeight: Int) {
        var imgWidth = imgWidth
        var imgHeight = imgHeight
        val xo = clip((width - imgWidth) / 2, 0, width - 1)
        val yo = clip((height - imgHeight) / 2, 0, height - 1)
        imgWidth = min(imgWidth, width)
        imgHeight = min(imgHeight, height)
        for (y in 0 until imgHeight) {
            val i = y * imgWidth
            for (x in 0 until imgWidth) {
                if (0 < pixels[i + x] and 0xff) {
                    val idx = (yo + y) * width + xo + x
                    uu[idx] = 0.5f
                    vv[idx] = 0.25f
                }
            }
        }
    }

    fun setCoefficients(f: Float, k: Float, dU: Float, dV: Float) {
        this.f = f
        this.k = k
        this.dU = dU
        this.dV = dV
    }


    /**
     * @param x
     * @param y
     * @param w
     * @param h
     */
    fun setRect(x: Int, y: Int, w: Int, h: Int) {
        val mix = clip(x - w / 2, 0, width)
        val max = clip(x + w / 2, 0, width)
        val miy = clip(y - h / 2, 0, height)
        val may = clip(y + h / 2, 0, height)
        for (yy in miy until may) {
            for (xx in mix until max) {
                val idx = yy * width + xx
                uu[idx] = 0.5f
                vv[idx] = 0.25f
            }
        }
    }

    fun setRect(x: Int, y: Int, w: Int, h: Int, u : Float, v : Float) {
        val mix = clip(x - w / 2, 0, width)
        val max = clip(x + w / 2, 0, width)
        val miy = clip(y - h / 2, 0, height)
        val may = clip(y + h / 2, 0, height)
        for (yy in miy until may) {
            for (xx in mix until max) {
                val idx = yy * width + xx
                uu[idx] = u
                vv[idx] = v
            }
        }
    }

    fun setRect(r: Rect) {
        setRect(r.x.toInt(), r.y.toInt(), r.w.toInt(), r.h.toInt())
    }


    fun update(t: Float) {
        val w1 = width - 1
        val h1 = height - 1
        for (y in 1 until h1) {
            for (x in 1 until w1) {
                val idx = y * width + x
                val top = idx - width
                val bottom = idx + width
                val left = idx - 1
                val right = idx + 1
                val currF = getFCoeffAt(x, y)
                val currK = getKCoeffAt(x, y)
                val currU = uu[idx]
                val currV = vv[idx]
                val d2 = currU * currV * currV
                u[idx] = Math.max(
                        0f,
                        currU + t * (dU * ((uu[right]
                                + uu[left]
                                + uu[bottom] + uu[top]) - 4 * currU) - d2 + currF * (1.0f - currU)))
                v[idx] = Math
                        .max(
                                0f,
                                (currV + (t * (((((dV * ((((vv[right]
                                        + vv[left]
                                        + vv[bottom] + vv[top])) - 4 * currV))) + d2)) - (currK * currV))))))
            }
        }

        if (isTiling) {
            val w2 = w1 - 1
            val idxH1 = h1 * width
            val idxH2 = (h1 - 1) * width
            for (x in 0 until width) {
                val left = (if (x == 0) w1 else x - 1)
                val right = (if (x == w1) 0 else x + 1)
                val idx = idxH1 + x
                val cu = uu[x]
                val cv = vv[x]
                val cui = uu[idx]
                val cvi = vv[idx]
                var d = cu * cv * cv
                u[x] = Math
                        .max(
                                0f,
                                (cu + (t * (((((dU * ((((uu[right]
                                        + uu[left]
                                        + uu[width + x] + cui)) - 4 * cu))) - d)) + (f * (1.0f - cu)))))))
                v[x] = Math
                        .max(
                                0f,
                                (cv + (t * (((((dV * ((((vv[right]
                                        + vv[left]
                                        + vv[width + x] + cvi)) - 4 * cv))) + d)) - (k * cv))))))
                d = cui * cvi * cvi
                u[idx] = Math
                        .max(
                                0f,
                                (cui + (t * (((((dU * ((((uu[idxH1 + right]
                                        + uu[(idxH1 + left)]
                                        + cu + uu[(idxH2 + x)])) - 4 * cui))) - d)) + (f * (1.0f - cui)))))))
                v[idx] = Math
                        .max(
                                0f,
                                (cvi + (t * (((((dU * ((((vv[idxH1 + right]
                                        + vv[(idxH1 + left)]
                                        + cv + vv[(idxH2 + x)])) - 4 * cvi))) + d)) - (k * cvi))))))
            }

            for (y in 0 until height) {
                val idx = y * width
                val idxW1 = idx + w1
                val idxW2 = idx + w2
                val cu = uu[idx]
                val cv = vv[idx]
                val cui = uu[idxW1]
                val cvi = vv[idxW1]
                var d = cu * cv * cv
                val up = (if (y == 0) h1 else y - 1) * width
                val down = (if (y == h1) 0 else y + 1) * width
                u[idx] = Math
                        .max(
                                0f,
                                (cu + (t * (((((dU * ((((uu[idx + 1] + cui
                                        + uu[down] + uu[up])) - 4 * cu))) - d)) + (f * (1.0f - cu)))))))
                v[idx] = Math
                        .max(
                                0f,
                                (cv + (t * (((((dV * ((((vv[idx + 1] + cvi
                                        + vv[down] + vv[up])) - 4 * cv))) + d)) - (k * cv))))))
                d = cui * cvi * cvi
                u[idxW1] = Math
                        .max(
                                0f,
                                (cui + (t * (((((dU * ((((cu + uu[idxW2]
                                        + uu[down + w1] + uu[(up + w1)])) - 4 * cui))) - d)) + (f * (1.0f - cui)))))))
                v[idxW1] = Math
                        .max(
                                0f,
                                (cvi + (t * (((((dV * ((((cv + vv[idxW2]
                                        + vv[down + w1] + vv[(up + w1)])) - 4 * cvi))) + d)) - (k * cvi))))))
            }
        }

        u.clear()
        uu.clear()
        uu.put(u)
        u.clear()
        uu.clear()

        v.clear()
        vv.clear()
        vv.put(v)
        v.clear()
        vv.clear()
//        System.arraycopy(u, 0, uu, 0, u.capacity())
//        System.arraycopy(v, 0, vv, 0, v.capacity())
    }

    private fun clip(t: Float, low: Float, high: Float): Float {
        if (t < low) return low;
        if (t > high) return high;
        return t;
    }

    private fun clip(t: Int, low: Int, high: Int): Int {
        if (t < low) return low;
        if (t > high) return high;
        return t;
    }
}

private inline operator fun FloatBuffer.set(x: Int, value: Float) {
//    if (value != 0f && value != 1f)
//        println("value $x:$value")
    this.put(x, value)
}
