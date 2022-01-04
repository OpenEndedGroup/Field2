package field.graphics

import field.graphics.Scene.Perform
import org.lwjgl.opengl.ARBTextureStorage
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL12
import org.lwjgl.opengl.GL12.glTexSubImage3D
import org.lwjgl.opengl.GL13
import org.lwjgl.opengl.GL30.GL_TEXTURE_2D_ARRAY
import java.io.File
import java.io.FilenameFilter
import java.nio.ByteBuffer

/**
 * This is a brief partial attempt to write a texture array, designed, currently, to
 */
class TextureArray(var unit: Int, val w: Int, val h: Int, val d: Int, val source: ByteBuffer) : BaseScene<TextureArray.State>(), Perform, OffersUniform<Int> {
    inner class State : Modifiable() {
        var name = -1
    }

    override fun perform0(): Boolean {
        val s: TextureArray.State = GraphicsContext.get(this)
        GL13.glActiveTexture(GL13.GL_TEXTURE0 + unit)
        GL11.glBindTexture(GL_TEXTURE_2D_ARRAY, s.name)
        return true
    }

    override fun setup(): State {
        val s: TextureArray.State = State()
        s.name = GL11.glGenTextures()
        GL13.glActiveTexture(GL13.GL_TEXTURE0 + unit)
        GL11.glBindTexture(GL_TEXTURE_2D_ARRAY, s.name)

        GL11.glTexParameteri(GL_TEXTURE_2D_ARRAY, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR)
        GL11.glTexParameteri(GL_TEXTURE_2D_ARRAY, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR)
        GL11.glTexParameteri(GL_TEXTURE_2D_ARRAY, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE)
        GL11.glTexParameteri(GL_TEXTURE_2D_ARRAY, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE)

        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1)
        GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, w);

        ARBTextureStorage.glTexStorage3D(GL_TEXTURE_2D_ARRAY, 1, GL11.GL_RGBA8, w, h, d)
        glTexSubImage3D(GL_TEXTURE_2D_ARRAY, 0, 0, 0, 0, w, h, d, GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, source)

        return s
    }

    override fun getPasses(): IntArray {
        return intArrayOf(-1)
    }

    override fun deallocate(s: State) {
        GL11.glDeleteTextures(s.name)
    }

    override fun getUniform(): Int {
        return unit
    }

    companion object {

        @JvmStatic
        val dirCache = HashMap<String, TextureArray?>()

        @JvmStatic
        @JvmOverloads
        // because of the way that this rewrites the unit of a, potentially already loaded, texture list this isn't very useful outside of the context of Trace
        fun fromDirectory(unit: Int, fn: String, maxNum: Int = -1, loadEvery: Int = 1): TextureArray? {

            val uu = dirCache.computeIfAbsent(fn + " " + maxNum) {
                var jj = File(fn).listFiles(FilenameFilter { _, name ->
                    name.endsWith(".jpg") && !name.startsWith(".")
                }) ?: return@computeIfAbsent null

                jj.sort()

                var jj2 = jj.toList().windowed(1, loadEvery).map { it[0] }

                val dim = SlowJPEG().dimensions(jj2[0].absolutePath)

                val ll = if (maxNum == -1) jj2.size else Math.min(jj2.size, maxNum)
                val dat = ByteBuffer.allocateDirect(dim[0] * dim[1] * 3 * (ll+4))

                jj2.forEachIndexed { i, n ->
                    if (maxNum != -1 && i >= ll) return@forEachIndexed

                    dat.position(i * dim[0] * dim[1] * 3)
                    dat.limit((i + 1) * dim[0] * dim[1] * 3)
                    println("loading ${n.absolutePath}")
                    SlowJPEG().decompressRaw(n.absolutePath, dat, dim[0], dim[1])
                    dat.clear()

                    println(i)
                    println(dat[30])
                    println(dat[30 + dim[0] * dim[1] * 3])
                    println()
                }
                dat.clear()

                TextureArray(unit, dim[0], dim[1], ll, dat)
            }
            uu?.unit = unit
            return uu
        }


        @JvmStatic
        // because of the way that this rewrites the unit of a, potentially already loaded, texture list this isn't very useful outside of the context of Trace
        fun fromFile(unit: Int, fn: String): TextureArray? {

            val uu = dirCache.computeIfAbsent(fn) {
                val jj = mutableListOf(File(fn))

                jj.sort()

                val dim = SlowJPEG().dimensions(jj[0].absolutePath)

                val dat = ByteBuffer.allocateDirect(dim[0] * dim[1] * 3 * jj.size)

                jj.forEachIndexed { i, n ->
                    dat.position(i * dim[0] * dim[1] * 3)
                    dat.limit((i + 1) * dim[0] * dim[1] * 3)
                    SlowJPEG().decompressRaw(n.absolutePath, dat, dim[0], dim[1])
                    dat.clear()

                }
                dat.clear()

                TextureArray(unit, dim[0], dim[1], jj.size, dat)
            }
            uu?.unit = unit
            return uu
        }

    }
}