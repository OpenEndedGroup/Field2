package field.graphics

import org.lwjgl.opengl.GL13.GL_TEXTURE0
import org.lwjgl.opengl.GL13.glActiveTexture
import trace.graphics.GL.glBindTexture

class TextureUnitReplacement(val u: Int, val source: Texture) : Scene.Perform, OffersUniform<Int> {

    override fun getPasses(): IntArray {
        return source.passes
    }

    override fun getUniform(): Int {
        return u
    }

    override fun perform(pass: Int): Boolean {

        if (pass == source.passes[0]) {

            val s = GraphicsContext.get<Texture.State>(source) {
                source.setup()
            }

            glActiveTexture(GL_TEXTURE0 + u)
            glBindTexture(source.specification.target, s.name)
        } else if (pass == source.passes[1]) {
            glActiveTexture(GL_TEXTURE0 + u)
            glBindTexture(source.specification.target, 0)
        }

        return true
    }

}