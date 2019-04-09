package trace.graphics

import field.app.RunLoop
import field.app.ThreadSync2
import field.graphics.GlfwCallback
import field.graphics.Scene
import field.graphics.Window
import field.graphics.vr.FakeOculusDrawTarget
import field.graphics.vr.OculusDrawTarget2
import field.graphics.vr.OpenVRDrawTarget
import field.linalg.Vec2
import fieldbox.boxes.Boxes.window
import fieldbox.ui.GlfwCallbackDelegate
import org.cef.OS
import trace.graphics.remote.RemoteServer

class SimpleOculusTarget {

    companion object {

        @JvmStatic
        var disable = false
        @JvmStatic
        var openvr = false

        val window: Window
        var o: OculusDrawTarget2? = null
        var o2: OpenVRDrawTarget? = null
        var fake: FakeOculusDrawTarget? = null

        val queue = mutableListOf<() -> Unit>()

        @JvmField
        val camera: SimpleCamera? = null

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

            if (!disable and OS.isWindows() and !openvr) {
                o = OculusDrawTarget2()
                o!!.debugBlit = true
                //        o.debugFBO = true
                o!!.init(window.scene)
            } else if (!disable and OS.isWindows() and openvr) {
                o2 = OpenVRDrawTarget()
                o2!!.debug = true
                o2!!.init(window.scene)
            } else {
                fake = FakeOculusDrawTarget(1344, 1600)
                fake!!.debugBlit = true
                fake!!.init(window.scene)
            }


            RunLoop.main.mainLoop.attach {

                if (o != null) {
                    if (o!!.textureW != -1) {
                        queue.forEach { it() }
                        queue.clear()
                    }
                } else if (o2 != null) {
                    if (o2!!.textureW != -1) {
                        queue.forEach { it() }
                        queue.clear()
                    }
                } else {
                    queue.forEach { it() }
                    queue.clear()

                }
                return@attach true
            }
        }

        fun textureW(): Int {
            if (o != null) return o!!.textureW
            if (o2 != null) return o2!!.textureW
            return 1344;
        }

        fun textureH(): Int {
            if (o != null) return o!!.textureH
            if (o2 != null) return o2!!.textureH
            return 1600;
        }

        fun scene(): Scene {
            if (o != null) return o!!.scene
            else if (o2 != null) return o2!!.getScene();
            else return fake!!.scene!!
        }

        fun getTarget(): Any {
            if (o != null) return o!!;
            if (o2 != null) return o2!!;
            return fake!!;
        }

        fun dimensions(): Vec2 {
            if (o != null) return Vec2(o!!.textureW.toDouble(), o!!.textureH.toDouble())
            else if (o2 != null) return Vec2(o2!!.textureW.toDouble(), o2!!.textureH.toDouble())
            else return Vec2(fake!!.wPerEye.toDouble(), fake!!.hPerEye.toDouble())
        }

        fun whenInited(a: () -> Unit) {
            if (o != null && o!!.textureW != -1) {
                a()
            } else if (o2 != null && o2!!.textureW != -1) {
                a()
            } else if (o == null && o2 == null) {
                a()
            } else queue.add(a)
        }

        fun waitUntilInited() {
            while (true) {
                println(" ... waiting .... ")
                ThreadSync2.yield()
                if (o != null && o!!.textureW != -1) {
                    return
                }
                if (o2 != null && o2!!.textureW != -1) {
                    return
                }
                if ((o == null) && o2 == null) {
                    return
                }
            }
        }

    }
}