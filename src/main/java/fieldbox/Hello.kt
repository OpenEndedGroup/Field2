package fieldbox

import org.lwjgl.glfw.GLFW

class Hello {

    companion object {
        @JvmStatic
        fun main(m : Array<String>)
        {
            println("hello!")
            GLFW.glfwInit()
        }
    }
}