package field.graphics

import org.lwjgl.opengl.GL15.glBindBuffer
import org.lwjgl.opengl.GL30
import org.lwjgl.opengl.GL43.GL_SHADER_STORAGE_BUFFER

class ShaderStorage(val target : BaseMesh, val aux : Int, val binding : Int) : Scene.Perform
{


    override fun perform(pass: Int): Boolean {

        if (GraphicsContext.get<Any>(target.buffers[0]!!)==null)
            target.perform(0)

        if (aux==-1)
        {
            val elements = target.elements!! as SimpleArrayBuffer
            glBindBuffer(GL_SHADER_STORAGE_BUFFER, elements.openGLNameInCurrentContext)
            GL30.glBindBufferBase(GL_SHADER_STORAGE_BUFFER, binding, elements.openGLNameInCurrentContext)
        }
        else
        {
            val elements = target.buffers[aux]!! as SimpleArrayBuffer
            glBindBuffer(GL_SHADER_STORAGE_BUFFER, elements.openGLNameInCurrentContext)
            GL30.glBindBufferBase(GL_SHADER_STORAGE_BUFFER, binding, elements.openGLNameInCurrentContext)
        }
        return true;
    }

    override fun getPasses(): IntArray {
        return intArrayOf(-1)
    }

}