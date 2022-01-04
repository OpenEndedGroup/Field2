package trace.graphics

//import field.graphics.vr.FakeOculusDrawTarget
import field.app.RunLoop
import field.app.ThreadSync2
import field.graphics.GlfwCallback
import field.graphics.Scene
import field.graphics.Window
import field.graphics.vr.OculusDrawTarget2
import field.graphics.vr.OpenVRDrawTarget
import field.linalg.Mat4
import field.linalg.Vec2
import fieldagent.Main
import fieldbox.ui.GlfwCallbackDelegate

class SimpleOculusTarget {

    companion object {

        @JvmStatic
        var disable = !Stage.VR
        @JvmStatic
        var openvr = true

        var window: Window? = null
        var o: OculusDrawTarget2? = null
        var o2: OpenVRDrawTarget? = null
//        var fake: FakeOculusDrawTarget? = null

        val queue = mutableListOf<() -> Unit>()

        @JvmField
        val camera: SimpleCamera? = null

        init {

            if (disable)
            {

            }
            else {
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
            }

            if (!disable and Main.OS.isWindows() and !openvr) {
                o = OculusDrawTarget2()
                o!!.debugBlit = true
                //        o.debugFBO = true
                o!!.init(window!!.scene)
            } else if (!disable and Main.OS.isWindows() and openvr) {
                o2 = OpenVRDrawTarget()
                o2!!.debug = true
                o2!!.init(window!!.scene)
            } else if (!disable) {
//                fake = FakeOculusDrawTarget(1344, 1600)
//                fake!!.debugBlit = true
//                fake!!.init(window!!.scene)
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
//            else return fake!!.scene!!
            throw IllegalArgumentException()
        }

        fun getTarget(): Any {
            if (o != null) return o!!;
            if (o2 != null) return o2!!;
            throw IllegalArgumentException()
//            return fake!!;
        }

        fun dimensions(): Vec2 {
            if (o != null) return Vec2(o!!.textureW.toDouble(), o!!.textureH.toDouble())
            else if (o2 != null) return Vec2(o2!!.textureW.toDouble(), o2!!.textureH.toDouble())
            throw IllegalArgumentException()
//            else return Vec2(fake!!.wPerEye.toDouble(), fake!!.hPerEye.toDouble())
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


        fun isVR() : Boolean
        {
            return (o!=null || o2 !=null);
        }

        fun leftProjectionMatrix(): Mat4 {
            if (OculusDrawTarget2.isVR!=null)
                return OculusDrawTarget2.isVR!!.leftProjectionMatrix()
            if (o2!=null)
                return o2!!.cameraInterface!!.projectionMatrix(-1f).transpose()
            throw IllegalArgumentException("left projection matrix in a non-vr context")
        }
        fun rightProjectionMatrix(): Mat4 {
            if (OculusDrawTarget2.isVR!=null)
                return OculusDrawTarget2.isVR!!.rightProjectionMatrix()
            if (o2!=null)
                return  o2!!.cameraInterface!!.projectionMatrix(1f).transpose()
            throw IllegalArgumentException("right projection matrix in a non-vr context")
        }

        fun leftViewMatrix(): Mat4 {
            if (OculusDrawTarget2.isVR!=null)
                return OculusDrawTarget2.isVR!!.leftView()
            if (o2!=null)
                return o2!!.cameraInterface!!.view(-1f).transpose()
            throw IllegalArgumentException("left projection matrix in a non-vr context")
        }

        fun rightViewMatrix(): Mat4 {
            if (OculusDrawTarget2.isVR!=null)
                return OculusDrawTarget2.isVR!!.rightView()
            if (o2!=null)
                return o2!!.cameraInterface!!.view(1f).transpose()
            throw IllegalArgumentException("left projection matrix in a non-vr context")
        }
    }
}