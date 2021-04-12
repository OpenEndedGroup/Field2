package fieldbox.ui

import field.utility.Rect
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.system.Configuration

object ScreenGeometry {
    @JvmStatic
    fun primaryMonitorBounds(): Rect {
        Configuration.GLFW_CHECK_THREAD0.set(false)

        val primary = glfwGetPrimaryMonitor()
        val x = IntArray(1)
        val y = IntArray(1)

        glfwGetMonitorPos(primary, x, y)
        val videoMode = glfwGetVideoMode(primary)
        return Rect(x[0].toDouble(), y[0].toDouble(), videoMode!!.width().toDouble(), videoMode!!.height().toDouble())
    }
}