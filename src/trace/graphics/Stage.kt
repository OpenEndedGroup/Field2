package trace.graphics

import field.app.RunLoop
import field.app.ThreadSync2
import field.graphics.*
import field.graphics.util.onsheetui.get
import field.linalg.Vec2
import field.linalg.Vec4
import field.utility.Documentation
import field.utility.IdempotencyMap
import fieldbox.boxes.Drawing
import fieldbox.boxes.plugins.BoxDefaultCode
import fieldbox.boxes.plugins.ThreadSync2Feedback
import fieldbox.boxes.plugins.Viewport
import fieldbox.ui.GlfwCallbackDelegate
import org.lwjgl.opengl.GL11
import java.util.*
import java.util.concurrent.Callable
import java.util.function.Consumer
import java.util.function.Supplier

/**
 * A stage is a place for drawing lines and planes that can quickly be put inside the canvas or in its own window
 */
class Stage(val w: Int, val h: Int) {
    val fbo: FBO

    @JvmField
    @Documentation("The background color of the stage")
    var background = Vec4(0.5, 0.5, 0.5, 1.0)

    var window: Window? = null;
    var isOut = false;

    companion object {
        var stageNum: Int = 0
    }

    val thisStageNum: Int;

    init {
        thisStageNum = stageNum++
        fbo = FBO(FBO.FBOSpecification.rgba(thisStageNum, w, h))

        fbo.scene.attach(-100, "__clear__", {

            GL11.glClearColor(background.x.toFloat(), background.y.toFloat(), background.z.toFloat(), background.w.toFloat())
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT)

            true
        });
    }

    val groups = mutableMapOf<String, ShaderGroup>()


    inner class ShaderGroup(val name: String, val shader: Shader) {
        val lines = BaseMesh.lineList(0, 0)
        val lineBuilder = MeshBuilder(lines)
        val planes = BaseMesh.triangleList(0, 0)
        val planeBuilder = MeshBuilder(planes)

        val l = IdempotencyMap<FLine>(FLine::class.java)

        var doTexture = false

        @JvmField
        @Documentation("Camera translation")
        var translation = Vec2();

        @JvmField
        @Documentation("Camera 'scale'. Numbers >1 zoom in, Numbers <1 zoom out")
        var scale = Vec2();

        init {
            shader.asMap_set("_line_", lines)
            shader.asMap_set("_planes_", planes)

            shader.asMap_set("translation", Supplier<Vec2> { Vec2(0.0, 0.0) })
            shader.asMap_set("bounds", Supplier<Vec2> { Vec2(100.0, 100.0) })
            shader.asMap_set("scale", Supplier<Vec2> { scale })
            shader.asMap_set("opacity", Supplier<Float> { 1.0f })

            fbo.scene.attach(name, shader)
            fbo.scene.attach(-100, "__doGeometry__", Consumer<Int> {

                // todo: mod caching?

                lineBuilder.open()
                planeBuilder.open()
                try {
                    l.values.forEach {
                        if (doTexture)
                            it.addAuxProperties(2, StandardFLineDrawing.texCoord.name)
                        System.out.println("---")
                        StandardFLineDrawing.dispatchLine(it, planeBuilder, lineBuilder, null, Optional.empty(), "")
                        System.out.println("---")
                    }
                } finally {
                    planeBuilder.close()
                    lineBuilder.close()
                }

            })
            groups[name] = this
        }

        fun detatch() {
            fbo.scene.detach(name)
        }
    }


    val default_vertex = BoxDefaultCode.findSource(this.javaClass, "default_vertex")
    val default_fragment = BoxDefaultCode.findSource(this.javaClass, "default_fragment")
    val default_group = ShaderGroup("__default__", defaultShader())

    val texture_vertex = BoxDefaultCode.findSource(this.javaClass, "texture_vertex")
    val texture_fragment = BoxDefaultCode.findSource(this.javaClass, "texture_fragment")

    private fun defaultShader(): Shader {
        val s = Shader()
        s.addSource(Shader.Type.vertex, default_vertex)
        s.addSource(Shader.Type.fragment, default_fragment)
        return s;
    }

    val show_vertex = BoxDefaultCode.findSource(this.javaClass, "show_vertex")
    val show_fragment = BoxDefaultCode.findSource(this.javaClass, "show_fragment")

    fun getDraw(): IdempotencyMap<FLine> {
        return default_group.l
    }

    // todo: filename mapping

    fun withTexture(filename: String): IdempotencyMap<FLine> {
        val n = groups.get(filename)
        if (n != null)
            return n.l

        if (groups.containsKey(filename)) {
            groups.remove(filename)!!.detatch()
        }

        val s = Shader()
        s.addSource(Shader.Type.vertex, texture_vertex)
        s.addSource(Shader.Type.fragment, texture_fragment)
        s.asMap_set("source", Texture(Texture.TextureSpecification.fromJpeg(3, filename, true)))

        // todo: automatically reload on change ?

        val sg = ShaderGroup(filename, s)
        sg.doTexture = true
        return sg.l
    }


    private var insideViewport: Viewport? = null

    fun show(name: String, v: Viewport) {
        insideViewport = v;
        showScene(name, (v get Viewport.scene)!!, { isOut })
    }

    fun showScene(name: String, scene: Scene, disabled: () -> Boolean) {
        val s = Shader()
        s.addSource(Shader.Type.vertex, show_vertex)
        s.addSource(Shader.Type.fragment, show_fragment)

        s.asMap_set("disabled", Supplier<Float> { if (disabled.invoke()) 1f else 0f })

        val planes = BaseMesh.triangleList(0, 0)
        val planeBuilder = MeshBuilder(planes)

        planeBuilder.open()
        planeBuilder.v(-1.0, -1.0, 0.0)
        planeBuilder.v(1.0, -1.0, 0.0)
        planeBuilder.v(1.0, 1.0, 0.0)
        planeBuilder.v(-1.0, 1.0, 0.0)
        planeBuilder.e(0, 1, 2)
        planeBuilder.e(0, 2, 3)
        planeBuilder.close()
        s.asMap_set("tex", fbo);
        s.asMap_set("geom", planes);

        scene.asMap_set(name, s);
        scene.attach(-100, "__draw__" + name, Consumer<Int> {
            fbo.draw()
        })

    }

    fun popOut() {
        if (isOut) {
            // toFront
            return;
        }
        isOut = true;

        ThreadSync2.callInMainThreadAndWait(Callable {
            if (window == null) {
                window = object : Window(0, 0, w - 1, h - 1, "Field / Stage $thisStageNum") {
                    override fun makeCallback(): GlfwCallback {

                        return object : GlfwCallbackDelegate(super.makeCallback()) {
                            override fun windowRefresh(l: Long) {
                                requestRepaint()
                            }

                            override fun windowClose(l: Long): Boolean {
                                popIn()
                                return true
                            }

                        }
                    }
                }

                window!!.setBounds(0, 0, w, h)
            }

            showScene("default", window!!.scene, { false })
        })


    }

    // can only be called by closing window
    private fun popIn() {
        window = null;
        isOut = false;
    }

    /**
     * reminds the window or viewport that it needs to update it's appearance
     */
    fun redraw() {
        if (isOut) {
            // assume continuous loop
        } else {
            if (insideViewport != null) {
                Drawing.dirty(insideViewport!!)
            }
        }
    }

    /**
     * marks the spot in the code where we wait for anything we've drawn to appear on the screen. Effectively the same as `_redraw(); _.wait()`
     */
    fun frame() {
        redraw()
        ThreadSync2Feedback.yield(null);
    }

}