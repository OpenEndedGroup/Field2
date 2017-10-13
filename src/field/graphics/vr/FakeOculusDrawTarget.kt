package field.graphics.vr

import field.graphics.Camera
import field.graphics.FBO
import field.graphics.Scene
import field.linalg.Mat4
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL13
import org.lwjgl.opengl.GL32
import org.lwjgl.ovr.OVRInputState

class FakeOculusDrawTarget {

    var floatResolution = false

    val wPerEye: Int = 4096
    val hPerEye: Int = 2048

    val camera = Camera()

    var fbo: FBO? = null

    var scene: Scene? = null

    var debugFBO = false
    var debugBlit = false
    var debugControllers = false

    var leftInputState: OVRInputState = OVRInputState.calloc()
    var rightInputState: OVRInputState = OVRInputState.calloc()
    var pauseCamera = false


    fun init(scene: Scene) {
        if(floatResolution)
            fbo = FBO(FBO.FBOSpecification.singleFloat16(5, wPerEye * 2, hPerEye))
        else
            fbo = FBO(FBO.FBOSpecification.srgba(5, wPerEye * 2, hPerEye))
        this.scene = fbo!!.getScene()!!

        fbo!!.scene.attach(-100, "fake", { x: Int ->
            GL11.glEnable(GL32.GL_DEPTH_CLAMP)
            GL11.glEnable(GL11.GL_BLEND)
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
            GL11.glEnable(GL13.GL_MULTISAMPLE)
            GL11.glEnable(GL11.GL_POLYGON_SMOOTH)
            GL11.glEnable(GL11.GL_CLIP_PLANE0)
            true
        })

        scene.attach(0, "drawFakeOculus", { x: Int ->

            fbo!!.draw()
        })
    }

    fun getPreviewTexture(): FBO {
        return fbo!!
    }

    fun leftView(): Mat4 {
        return Mat4(camera.viewLeft()).transpose()
    }

    fun rightView(): Mat4 {
        return Mat4(camera.viewRight()).transpose()
    }

    fun leftProjectionMatrix(): Mat4 {
        return Mat4(camera.projectionMatrix(-1f)).transpose()
    }

    fun rightProjectionMatrix(): Mat4 {
        return Mat4(camera.projectionMatrix(1f)).transpose()
    }

    fun resetViewNow()
    {

    }

}
