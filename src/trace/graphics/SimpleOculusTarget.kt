package trace.graphics

import field.app.RunLoop
import field.graphics.GlfwCallback
import field.graphics.Window
import field.graphics.vr.OculusDrawTarget2
import field.linalg.Vec2
import fieldbox.boxes.Boxes.window
import fieldbox.ui.GlfwCallbackDelegate

class SimpleOculusTarget {

    val window: Window
    val o: OculusDrawTarget2

    val queue = mutableListOf<() -> Unit>()

    init {

        window = object : Window(0, 0, 1024, 512, "Field / VR preview") {
            override fun makeCallback(): GlfwCallback {

                return object : GlfwCallbackDelegate(super.makeCallback()) {
                    override fun windowRefresh(l: Long) {
                        requestRepaint()
                    }

                    override fun windowClose(l: Long): Boolean {
                        return true
                    }
                }
            }
        }

        window!!.setBounds(0, 0, 1024, 512)

        o = OculusDrawTarget2()
        o.init(window.scene)

        RunLoop.main.mainLoop.attach {

            if (o.textureW != -1) {
                queue.forEach { it() }
                queue.clear()
            }

            return@attach true
        }
    }

    fun dimensions(): Vec2 {
        return Vec2(o.textureW.toDouble(), o.textureH.toDouble())
    }

    fun whenInited(a: () -> Unit) {
        if (o.textureW != -1) {
            a()
        } else
            queue.add(a)
    }
}