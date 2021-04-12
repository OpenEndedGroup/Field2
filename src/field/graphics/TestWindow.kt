package field.graphics

import fieldagent.Main
import org.lwjgl.Version
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11.*
import org.lwjgl.system.Configuration

class TestWindow {

    companion object {
        private var window: Long = 0

        @JvmStatic
        fun main(s: Array<String>) {
            println(Version.getVersion())
            Configuration.GLFW_CHECK_THREAD0.set(false)
            glfwInit()

            GLFWErrorCallback.createPrint(System.err).set();

            glfwDefaultWindowHints()
            glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4)
            glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 1)
            glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE); // the window will stay hidden after creation
            glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE); // the window will be resizable

            glfwWindowHint(GLFW_DEPTH_BITS, 24)
            glfwWindowHint(GLFW_COCOA_GRAPHICS_SWITCHING, 1)
            glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE)
            glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, 1)
            if (Main.os == Main.OS.mac) {
                glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3)
                glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 2)
            } else {
                glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4)
                glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 2)
                if (Window.glDebugging) glfwWindowHint(GLFW_OPENGL_DEBUG_CONTEXT, 1)
            }

            glfwWindowHint(GLFW_DOUBLEBUFFER, if (Window.doubleBuffered) 1 else 0)

            glfwWindowHint(GLFW_DECORATED, 1)

            println(" open window > ")
            window = glfwCreateWindow(300, 300, "title", 0, 0);
            println(" < open window ")

            glfwMakeContextCurrent(window);
            glfwSwapInterval(1);

            println(" -- window -- $window")
            glfwSetWindowPos(window, 500, 500)
            glfwSetWindowSize(window, 310, 310)
            glfwShowWindow(window)

            glfwMakeContextCurrent(0)

            while(true)
            {
                glfwMakeContextCurrent(window)
                GL.createCapabilities();
                Thread.sleep(10)
                print(" -")
                glfwPollEvents();
                glfwSwapBuffers(window)

                glClearColor(Math.random().toFloat(), 0.0f, Math.random().toFloat(), 0.0f);
                glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT) // clear the framebuffer


            }

        }
    }

}