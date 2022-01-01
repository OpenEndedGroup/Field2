package trace.video

import field.graphics.GraphicsContext
import field.graphics.OffersUniform
import jsyphon.JSyphonClient
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL11.glBindTexture
import org.lwjgl.opengl.GL13.GL_TEXTURE0
import org.lwjgl.opengl.GL13.glActiveTexture
import org.lwjgl.opengl.GL31

class Syphon(val unit : Int) : OffersUniform<Int>
{
    var first = true;

    private lateinit var client: JSyphonClient

    fun start()
    {
        client = JSyphonClient()
        client.init()
    }

    var last = System.currentTimeMillis()

    override fun getUniform(): Int {

        if (first)
        {
            start()
            first = false
        }

//        print("server :"+ client.serverDescription());

        val image = client.newFrameImageForContext()

        if (System.currentTimeMillis()-last>2000) {
            for (d in client.newFrameDataForContext()) {
                println(d);
            };
            last = System.currentTimeMillis()
        }

        if (image!=null)
        {
            GraphicsContext.checkError({"before bind"})
            glActiveTexture(GL_TEXTURE0 + unit)
            GraphicsContext.checkError({"before bind 0"})
            glBindTexture(GL11.GL_TEXTURE_2D, 0)
            GraphicsContext.checkError({"before bind 1"})
//            GL11.glEnable(GL31.GL_TEXTURE_RECTANGLE)
//            GraphicsContext.checkError({"before bind A"})
            GL11.glBindTexture(GL31.GL_TEXTURE_RECTANGLE, image.textureName());
            GraphicsContext.checkError({"before bind B"})

            return unit
        }

        print("error: no syphon image available")
        return 0
    }

}