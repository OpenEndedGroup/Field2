package trace.graphics

import field.graphics.Texture
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class TextureFromFloatBuffer(val w: Int, val h: Int) {
    var source: FloatBuffer
    var tex: Texture
    var source_b: ByteBuffer

    init {
        source_b = ByteBuffer.allocateDirect(w * h * 4).order(ByteOrder.nativeOrder())
        source = source_b.asFloatBuffer()
        tex = Texture(Texture.TextureSpecification.float1(7, w, h, source_b))
    }

    fun update() {
        tex.upload()
    }

    fun copyTo(s: FloatBuffer) {
        source.rewind()
        s.rewind()
        source.put(s)
        source.rewind()
        s.rewind()
        update()
    }

    fun copyTo(s: FloatArray) {
        source.rewind()
        source.put(s)
        source.rewind()
        update()
    }
}