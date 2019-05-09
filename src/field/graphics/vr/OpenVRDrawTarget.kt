package field.graphics.vr

import field.graphics.FBO
import field.graphics.GraphicsContext
import field.graphics.Scene
import field.graphics.StereoCameraInterface
import field.linalg.Mat4
import field.linalg.Vec3
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL11.*
import org.lwjgl.openvr.*
import org.lwjgl.openvr.VR.*
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryStack.stackPush
import java.nio.IntBuffer
import org.lwjgl.openvr.VR.ETrackedDeviceProperty_Prop_SerialNumber_String
import org.lwjgl.openvr.VR.k_unTrackedDeviceIndex_Hmd
import org.lwjgl.openvr.VR.ETrackedDeviceProperty_Prop_ModelNumber_String
import org.lwjgl.openvr.VRSystem.*


class OpenVRDrawTarget {

    internal var warmUp = 0

    @JvmField
    var fbo: FBO? = null

    var debug = true

    val buttons = Buttons { b ->
        val axes = listOf("button0_left", "button1_left", "button2_left", "button32_left", "button33_left", "axis0_left_x", "axis0_left_y", "axis1_left_x")
        axes.forEach { b.addAxis(it) }
        axes.forEach { b.addAxis(it.replace("left", "right")) }
    }

    val leftTouched = mutableSetOf<String>();
    val rightTouched = mutableSetOf<String>();


    var lastTD = 0L;

    fun getPreviewTexture() = fbo

    fun init(w: Scene) {
        w.attach(0, "__initopenvr__", { _: Int ->
            actualInit(w)
            false
        })

        w.attach(100, "__go_openvr__", { _: Int ->

            if (fbo != null && true)
                stackPush().use {

                    GraphicsContext.checkError{
                        "before OpenVR FBO"
                    }

                    val poses = TrackedDevicePose.callocStack(VR.k_unMaxTrackedDeviceCount)
                    val gposes = TrackedDevicePose.callocStack(VR.k_unMaxTrackedDeviceCount) // no idea what this is?
                    VRCompositor.VRCompositor_WaitGetPoses(poses, gposes)

                    for (n in 0 until 8) {

                        val clazz = VRSystem_GetTrackedDeviceClass(n)

                        val pose = poses.get(n)
                        val valid = pose.bPoseIsValid()
                        val connected = pose.bDeviceIsConnected()

                        if (!valid || !connected) continue

                        val h34 = pose.mDeviceToAbsoluteTracking()
                        if (clazz == VR.ETrackedDeviceClass_TrackedDeviceClass_HMD)
                            head.set(h34.m(0), h34.m(1), h34.m(2), h34.m(3), h34.m(4), h34.m(5), h34.m(6), h34.m(7), h34.m(8), h34.m(9), h34.m(10), h34.m(11), 0f, 0f, 0f, 1f)
                        else if (clazz == VR.ETrackedDeviceClass_TrackedDeviceClass_Controller) {
                            val role = VRSystem_GetControllerRoleForTrackedDeviceIndex(n)
                            if (role == VR.ETrackedControllerRole_TrackedControllerRole_LeftHand) {
                                hand1.set(h34.m(0), h34.m(1), h34.m(2), h34.m(3), h34.m(4), h34.m(5), h34.m(6), h34.m(7), h34.m(8), h34.m(9), h34.m(10), h34.m(11), 0f, 0f, 0f, 1f)
                                var state = VRControllerState.calloc()
                                VRSystem_GetControllerState(n, state)
                                for (q in 0 until VR.k_unControllerStateAxisCount) {
                                    buttons.setAxis("axis${q}_left_x", state.rAxis(q).x())
                                    buttons.setAxis("axis${q}_left_y", state.rAxis(q).y())
                                }
                                val bd = state.ulButtonPressed();
                                val td = state.ulButtonTouched();
                                leftTouched.clear()

                                // No API to get real names for these ?
                                for (q in 0 until 63) {
                                    buttons.setAxis("button${q}_left", if ((bd and (1L shl q)) != 0L) 1f else 0f)
                                    if ((td and (1L shl q)) != 0L)
                                        leftTouched.add("button${q}_left")
                                }
                            }
                            else {
                                hand2.set(h34.m(0), h34.m(1), h34.m(2), h34.m(3), h34.m(4), h34.m(5), h34.m(6), h34.m(7), h34.m(8), h34.m(9), h34.m(10), h34.m(11), 0f, 0f, 0f, 1f)
                                var state = VRControllerState.calloc()
                                VRSystem_GetControllerState(n, state)
                                for (q in 0 until VR.k_unControllerStateAxisCount) {
                                    buttons.setAxis("axis${q}_right_x", state.rAxis(q).x())
                                    buttons.setAxis("axis${q}_right_y", state.rAxis(q).y())
                                }
                                val bd = state.ulButtonPressed();
                                val td = state.ulButtonTouched();
                                rightTouched.clear()

                                for (q in 0 until 63) {
                                    buttons.setAxis("button${q}_right", if ((bd and (1L shl q)) != 0L) 1f else 0f)
                                    if ((td and (1L shl q)) != 0L)
                                        rightTouched.add("button${q}_right")
                                }
                            }
                        }
                        else if (clazz == VR.ETrackedDeviceClass_TrackedDeviceClass_GenericTracker)
                        {
                            tracker0.set(h34.m(0), h34.m(1), h34.m(2), h34.m(3), h34.m(4), h34.m(5), h34.m(6), h34.m(7), h34.m(8), h34.m(9), h34.m(10), h34.m(11), 0f, 0f, 0f, 1f)
//                            println("found tracker at "+tracker0)
                        }


                        if (debug)
                            print("device $n is valid is $clazz, $valid, $connected ${h34.m(0)}\n")
                    }


                    buttons.run()

//                    GraphicsContext.checkError{
//                        "before OpenVR A1"
//                    }


//                    print("HEAD = \n$head")


                    glEnable(GL_CLIP_PLANE0)
                    fbo?.draw()
                    glDisable(GL_CLIP_PLANE0)

//                    GraphicsContext.checkError{
//                        "before OpenVR A2"
//                    }

                    val t = Texture.callocStack()
                    t.eColorSpace(VR.EColorSpace_ColorSpace_Linear)
                    t.eType(VR.ETextureType_TextureType_OpenGL)
                    t.handle(fbo!!.openGLTextureNameInCurrentContext.toLong())

                    GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

//                    println("texture name is ${fbo!!.openGLTextureNameInCurrentContext.toLong()}")

                    val b = VRTextureBounds.callocStack()

//                    GraphicsContext.checkError{
//                        "before OpenVR A3"
//                    }

                    b.uMin(0f)
//                    b.uMax(fbo!!.specification.width / 2f)
                    b.uMax(0.5f)
                    b.vMin(0f)
                    b.vMax(1f)
                    val res0 = VRCompositor.VRCompositor_Submit(EVREye_Eye_Left, t, b, VR.EVRSubmitFlags_Submit_Default) /// we can used Submit_GLRenderBuffer to submit an MSAA pre-resolve renderbuffer

//                    val vv = GL11.glGetError()
//                    if (vv!=0)
//                        println(" PHANTOM ERROR $vv")


//                    GraphicsContext.checkError{
//                        "before OpenVR A4"
//                    }

                    b.uMin(0.5f)
                    b.uMax(1f)
                    b.vMin(0f)
                    b.vMax(1f)
                    val res1 = VRCompositor.VRCompositor_Submit(EVREye_Eye_Right, t, b, VR.EVRSubmitFlags_Submit_Default)
                    GL11.glGetError()

//                    GraphicsContext.checkError{
//                        "before OpenVR A5"
//                    }

                    // who knows, yet?
//                    glFinish();

                    val TD = System.nanoTime()-lastTD
                    lastTD = TD

                    if (debug)
                        print(" submitted frame: $res0 $res1 $TD\n")


//                    GraphicsContext.checkError{
//                        "before OpenVR A6"
//                    }


                    for(P in fbo!!.passes)
                        fbo!!.perform(P)


                }

            true
        })
    }

    val cameraInterface: StereoCameraInterface by lazy {
        object : StereoCameraInterface {
            override fun projectionMatrix(stereoSide: Float): Mat4 {
                if (stereoSide < 0) return Mat4(left_p)
                return Mat4(right_p)
            }

            override fun view(stereoSide: Float): Mat4 {
                if (stereoSide < 0) {
                    return Mat4.mul(Mat4(left_v), Mat4(head), Mat4()).invert()
                } else {
                    return Mat4.mul(Mat4(right_v), Mat4(head), Mat4()).invert()
                }
            }

            override fun getPosition(): Vec3 {
                // not tested
                return head.transform(Vec3())
            }
        }
    }

    fun leftView()  = cameraInterface.view(-1f).transpose()
    fun rightView()  = cameraInterface.view(1f).transpose()
    fun leftProjectionMatrix()  = cameraInterface.projectionMatrix(-1f).transpose()
    fun rightProjectionMatrix()  = cameraInterface.projectionMatrix(1f).transpose()


    fun cameraInterface(): StereoCameraInterface {
        return cameraInterface
    }

    fun getScene(): Scene {
        return fbo?.scene!!
    }

    val left_p: Mat4 = Mat4()
    val right_p: Mat4 = Mat4()
    val left_v: Mat4 = Mat4()
    val right_v: Mat4 = Mat4()

    val head: Mat4 = Mat4()

    val hand1: Mat4 = Mat4()
    val hand2: Mat4 = Mat4()
    val tracker0: Mat4 = Mat4()

    var inited = false;

    var textureW = -1;
    var textureH = -1;

    private fun actualInit(w: Scene) {
        if (inited)
            return
        inited = true

        println()
        println("OpenVR init time is here ----------------------------------------------------------------------------------------- ")
        println("VR_IsRuntimeInstalled() = " + VR_IsRuntimeInstalled())
        println("VR_RuntimePath() = " + VR_RuntimePath())
        println("VR_IsHmdPresent() = " + VR_IsHmdPresent())

        stackPush().use { stack: MemoryStack ->
            val peError = stack.mallocInt(1)

            val token = VR_InitInternal(peError, EVRApplicationType_VRApplication_Scene)

            if (peError.get(0) == 0)

                try {
                    OpenVR.create(token)
                    println("Model Number : " + VRSystem_GetStringTrackedDeviceProperty(
                            k_unTrackedDeviceIndex_Hmd,
                            ETrackedDeviceProperty_Prop_ModelNumber_String,
                            peError
                    ))
                    println("Serial Number: " + VRSystem_GetStringTrackedDeviceProperty(
                            k_unTrackedDeviceIndex_Hmd,
                            ETrackedDeviceProperty_Prop_SerialNumber_String,
                            peError
                    ))

                    val w = stack.mallocInt(1)
                    val h = stack.mallocInt(1)
                    VRSystem_GetRecommendedRenderTargetSize(w, h)
                    println("Recommended width : " + w.get(0))
                    println("Recommended height: " + h.get(0))

                    textureW = w.get(0)
                    textureH = h.get(0)


//                    fbo = FBO(FBO.FBOSpecification.rgbaMultisample(10, textureW * 2, textureH))
                    fbo = FBO(FBO.FBOSpecification.rgba(10, textureW * 2, textureH))

                    val left_projection = HmdMatrix44.malloc()
                    val right_projection = HmdMatrix44.malloc()

                    VRSystem_GetProjectionMatrix(EVREye_Eye_Left, 0.01f, 10000f, left_projection)
                    VRSystem_GetProjectionMatrix(EVREye_Eye_Right, 0.01f, 10000f, right_projection)

                    left_p.set(left_projection.m())
                    right_p.set(right_projection.m())

                    val view = HmdMatrix34.malloc()
                    VRSystem_GetEyeToHeadTransform(EVREye_Eye_Left, view)
                    left_v.set(view.m(0), view.m(1), view.m(2), view.m(3), view.m(4), view.m(5), view.m(6), view.m(7), view.m(8), view.m(9), view.m(10), view.m(11), 0f, 0f, 0f, 1f)
                    VRSystem_GetEyeToHeadTransform(EVREye_Eye_Right, view)
                    right_v.set(view.m(0), view.m(1), view.m(2), view.m(3), view.m(4), view.m(5), view.m(6), view.m(7), view.m(8), view.m(9), view.m(10), view.m(11), 0f, 0f, 0f, 1f)

                    print(" static camera matrix elements ")
                    print(" left_p :\n$left_p")
                    print(" right_p :\n$right_p")
                    print(" left_v :\n$left_v")
                    print(" right_v :\n$right_v")

                    VRChaperone.VRChaperone_ForceBoundsVisible(false);

                    fbo!!.scene.attach(-100, { k ->

                        GL11.glClearColor(0.9f, 0.0f, 0.9f, 1f)
                        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT or GL11.GL_DEPTH_BUFFER_BIT)

                        GL11.glDisable(GL_SCISSOR_TEST)

                        true
                    })

                } finally {
//                    VR_ShutdownInternal()
                }
            else {
                println("INIT ERROR SYMBOL: " + VR_GetVRInitErrorAsSymbol(peError.get(0)))
                println("INIT ERROR  DESCR: " + VR_GetVRInitErrorAsEnglishDescription(peError.get(0)))
            }
        }



        println()

    }
}

private fun <T : AutoCloseable> T.use(function: (T) -> Unit) {
    try {
        function.invoke(this)
    } finally {
        this.close()
    }
}
