package field.graphics.gltf

import de.javagl.jgltf.model.GltfModel
import de.javagl.jgltf.model.GltfModels
import de.javagl.jgltf.model.io.GltfAsset
import de.javagl.jgltf.model.io.GltfAssetReader
import de.javagl.jgltf.viewer.AbstractGltfViewer
import de.javagl.jgltf.viewer.GlContext
import field.graphics.FullScreenWindow
import field.graphics.GraphicsContext
import field.graphics.Scene
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL12
import org.lwjgl.opengl.GL30
import java.io.File


class HelloGLTF {

    val g = FieldGLContext()

    inner public class Viewer(val f: FullScreenWindow?) : AbstractGltfViewer<FullScreenWindow?>() {

        override fun getRenderComponent(): FullScreenWindow? {
            return f
        }

        override fun getWidth(): Int {
            return f?.frameBufferWidth ?: 1024
        }

        override fun getHeight(): Int {
            return f?.frameBufferHeight ?: 1024
        }

        override fun triggerRendering() {
//            println(" -- trigger rendering called ... ??")
        }

        override fun getGlContext(): GlContext {
            return g
        }

        override fun prepareRender() {
//            println(" --prepare render called")
        }

        var va = -1
        override fun render() {

            renderGltfModels()
        }

        /*
        >>>>>>>>>>>>>>>>>
DISABLE 3042
DISABLE 2884
DISABLE 2929
DISABLE 32823
DISABLE 32926
DISABLE 3089
ENABLE 2929
ENABLE 2884
 <<<<<<<<<<<<<<<<<<<<<
         */

        var first = true
        fun renderNow() {
            try {
                println(" >>>>>>>>>>>>>>>>>")
//                GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS)
//                GL11.glPushClientAttrib(GL11.GL_CLIENT_ALL_ATTRIB_BITS)


                if (va == -1) {
                    va = GL30.glGenVertexArrays()
                }
                GL30.glBindVertexArray(va)
                try {
                    GraphicsContext.checkError { "before doRender" }
                    doRender()
                    GraphicsContext.checkError { "after doRender" }
                } finally {
                    GL30.glBindVertexArray(0)
                }

                GL11.glDisable(GL11.GL_CULL_FACE)
                GL11.glDisable(GL11.GL_DEPTH_TEST)
            }
            finally {
                println(" <<<<<<<<<<<<<<<<<<<<<")
//                GL11.glPopClientAttrib()
//                GL11.glPopAttrib()
            }
        }
    }

    fun buildViewer(s: Scene): Viewer {
        var v = Viewer(null)

        s.attach(Scene.Perform {
            v.renderNow()
            true
        })

        return v
    }

    fun read(n: String): Pair<GltfModel, Any> {

        val gltfAsset: GltfAsset = GltfAssetReader().read(File(n).toURI())
        val gltfModel = GltfModels.create(gltfAsset)
        val gltf = gltfAsset.gltf


        return gltfModel to gltf
    }

}