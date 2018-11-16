package trace.graphics

import field.app.RunLoop
import field.app.ThreadSync2
import field.graphics.*
import field.graphics.util.KeyboardCamera
import field.graphics.util.Saver
import field.graphics.util.SaverFBO
import field.graphics.util.onsheetui.get
import field.graphics.vr.OculusDrawTarget2
import field.linalg.*
import field.utility.*
import fieldbox.boxes.Box
import fieldbox.boxes.Drawing
import fieldbox.boxes.plugins.BoxDefaultCode
import fieldbox.boxes.plugins.GraphicsSupport
import fieldbox.boxes.plugins.ThreadSync2Feedback
import fieldbox.boxes.plugins.Viewport
import fieldbox.execution.Execution
import fieldbox.ui.GlfwCallbackDelegate
import fielded.RemoteEditor
import fieldlinker.AsMap
import fieldnashorn.annotations.HiddenInAutocomplete
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL12
import org.lwjgl.opengl.GL13
import org.lwjgl.opengl.GL32
import trace.graphics.remote.RemoteServer
import trace.graphics.remote.RemoteStageLayerHelper
import trace.input.Buttons
import trace.input.WebcamDriver
import trace.video.ImageCache
import trace.video.Syphon
import trace.video.TwinTextureCache
import java.io.File
import java.lang.reflect.Modifier
import java.util.*
import java.util.concurrent.Callable
import java.util.function.Consumer
import java.util.function.Supplier

/**
 * A stage is a place for drawing lines and planes that can quickly be put inside the canvas or in its own window
 *
 * stage.withName("bob").lines.hello = hello // done
 * stage.lines.hello = hello // done

 * stage.remap = `dest.x = src.x*2` ?
 */
class Stage(val w: Int, val h: Int) : AsMap {


    override fun asMap_new(a: Any?): Any {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun asMap_new(a: Any?, b: Any?): Any {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun asMap_getElement(element: Int): Any {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun asMap_set(p: String?, o: Any?): Any {
        throw IllegalArgumentException(" can't set '" + p + " on a stage")
    }

    override fun asMap_call(a: Any?, b: Any?): Any {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun asMap_delete(p: Any?): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    @HiddenInAutocomplete
    protected val knownNonProperties: Set<String> = computeKnownNonProperties()

    override fun asMap_isProperty(p: String?): Boolean {
        return !knownNonProperties.contains(p)
    }

    protected fun computeKnownNonProperties(): Set<String> {
        val r = LinkedHashSet<String>()
        val m = this.javaClass
                .methods
        for (mm in m)
            r.add(mm.name)
        val f = this.javaClass
                .fields
        for (ff in f)
            if (!Modifier.isStatic(ff.modifiers))
                r.add(ff.name)

        r.remove("children")
        r.remove("parents")

        return r
    }

    override fun asMap_get(p: String?): Any {
        throw IllegalArgumentException(" can't get '" + p + " from a stage")
    }

    override fun asMap_setElement(element: Int, o: Any?): Any {
        throw IllegalArgumentException(" can't set '" + element + " to a stage")
    }

    val fbo: FBO

    @JvmField
    @Documentation("The background color of the stage")
    var background = Vec4(0.5, 0.5, 0.5, 1.0)

    var window: Window? = null;
    var isOut = false;

    companion object {
        var stageNum: Int = 0

        @JvmStatic
        var rs = RemoteServer()
        var max_vertex = 60000;
        var max_element = 60000;
        var doRemote = true;
    }

    val thisStageNum: Int;


    @HiddenInAutocomplete
    val clear_vertex = BoxDefaultCode.findSource(this.javaClass, "clear_vertex")
    @HiddenInAutocomplete
    val clear_fragment = BoxDefaultCode.findSource(this.javaClass, "clear_fragment")

    @HiddenInAutocomplete
    val saver: SaverFBO;

    init {
        thisStageNum = stageNum++
        fbo = FBO(FBO.FBOSpecification.singleFloat16_depth(thisStageNum, w, h))

        val base = System.getProperty("user.home") + File.separatorChar + "Desktop" + File.separatorChar + "field_stage_recordings" + File.separatorChar

        var x = 1
        while (File(base + Saver.pad(x)).exists()) x++

        val prefix = File(base + Saver.pad(x))
        prefix.mkdirs()

        saver = SaverFBO(w, h, 4, prefix.absolutePath + File.separatorChar + "s_", fbo)

        val s = Shader()

        s.addSource(Shader.Type.vertex, clear_vertex);
        s.addSource(Shader.Type.fragment, clear_fragment);

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

        s.asMap_set("geom", planes)
        s.asMap_set("color", Supplier { background });
        fbo.scene.attach(-101, {
            if (STEREO)
                GL11.glEnable(GL11.GL_CLIP_PLANE0);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glDisable(GL11.GL_DEPTH_TEST)
            GL11.glDepthFunc(GL11.GL_LESS)
//            GL11.glEnable(GL32.GL_DEPTH_CLAMP)
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
//            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
            false
        })
        fbo.scene.attach("background_clear", s)
        fbo.scene.attach(-101, Scene.Perform {
            GL11.glClearDepth(1.0)
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT or GL11.GL_DEPTH_BUFFER_BIT);
            false
        })

        fbo.scene.attach(101, Scene.Perform {
            saver.update()
            true
        })

    }

    @JvmField
    var STEREO: Boolean = false

    @JvmField
    var VR: Boolean = false

    @JvmField
    var remap = ""

    @JvmField
    val groups = mutableMapOf<String, ShaderGroup>()


    @HiddenInAutocomplete
    val default_vertex = BoxDefaultCode.findSource(this.javaClass, "default_vertex")
    @HiddenInAutocomplete
    val default_fragment = BoxDefaultCode.findSource(this.javaClass, "default_fragment")

    @HiddenInAutocomplete
    val default_fragment_points = BoxDefaultCode.findSource(this.javaClass, "default_fragment_points")

    @HiddenInAutocomplete
    val default_geometry_lines = BoxDefaultCode.findSource(this.javaClass, "default_geometry_lines")
    @HiddenInAutocomplete
    val default_geometry_triangles = BoxDefaultCode.findSource(this.javaClass, "default_geometry_triangles")
    @HiddenInAutocomplete
    val default_geometry_points = BoxDefaultCode.findSource(this.javaClass, "default_geometry_points")

    @HiddenInAutocomplete
    val texture_vertex = BoxDefaultCode.findSource(this.javaClass, "texture_vertex")
    @HiddenInAutocomplete
    val texture_fragment = BoxDefaultCode.findSource(this.javaClass, "texture_fragment")
    @HiddenInAutocomplete
    val texture_fragment_points = BoxDefaultCode.findSource(this.javaClass, "texture_fragment_points")

    @HiddenInAutocomplete
    val texture_geometry_lines = BoxDefaultCode.findSource(this.javaClass, "texture_geometry_lines")
    @HiddenInAutocomplete
    val texture_geometry_triangles = BoxDefaultCode.findSource(this.javaClass, "texture_geometry_triangles")
    @HiddenInAutocomplete
    val texture_geometry_points = BoxDefaultCode.findSource(this.javaClass, "texture_geometry_points")

    @HiddenInAutocomplete
    val video_vertex = BoxDefaultCode.findSource(this.javaClass, "video_vertex")
    @HiddenInAutocomplete
    val video_fragment = BoxDefaultCode.findSource(this.javaClass, "video_fragment")
    @HiddenInAutocomplete
    val video_fragment_points = BoxDefaultCode.findSource(this.javaClass, "video_fragment_points")

    @HiddenInAutocomplete
    val video_geometry_lines = BoxDefaultCode.findSource(this.javaClass, "video_geometry_lines")
    @HiddenInAutocomplete
    val video_geometry_triangles = BoxDefaultCode.findSource(this.javaClass, "video_geometry_triangles")
    @HiddenInAutocomplete
    val video_geometry_points = BoxDefaultCode.findSource(this.javaClass, "video_geometry_points")

    @HiddenInAutocomplete
    val default_group = {
        val s = ShaderGroup("__default__")
        s.setShader(defaultShader(s))
        s
    }()


    inner class ShaderGroup(val name: String) {
        val line = BaseMesh.lineList(0, 0).setInstances({ if (OculusDrawTarget2.isVR != null || STEREO) 2 else 1 })
        val lineBuilder = MeshBuilder(line)
        val planes = BaseMesh.triangleList(0, 0).setInstances({ if (OculusDrawTarget2.isVR != null || STEREO) 2 else 1 })
        val planeBuilder = MeshBuilder(planes)
        val points = BaseMesh.pointList(0).setInstances({ if (OculusDrawTarget2.isVR != null || STEREO) 2 else 1 })
        val pointBuilder = MeshBuilder(points)
        val post = IdempotencyMap<Runnable>(Runnable::class.java);

        val remoteHelper = if (doRemote) RemoteStageLayerHelper(rs.s, max_vertex, max_element, -1, name) else null

        @JvmField
        @Documentation("list of `FLine` to draw, just like `_.lines` but it will appear on this Stage")
        val lines = IdempotencyMap<FLine>(FLine::class.java).configureResourceLimits<IdempotencyMap<FLine>>(300, "too many lines on a stage layer")

        var doTexture = false
        var textureFilename: String? = null
        var textureDimensions = Vec2(1, 1)

        @JvmField
        @Documentation("Camera translation. Starts at vec(0,0)")
        var translation = Vec2();

        @JvmField
        @Documentation("Camera 'scale'. Numbers >1 zoom in, Numbers <1 zoom out. Starts as vec(1,1).")
        var scale = Vec2(1.0, 1.0)

        @JvmField
        @Documentation("The opacity of this layer")
        var opacity = 1.0;

        @JvmField
        @Documentation("If this layer is a video layer this `time` represents the fraction through the video that we are. i.e. 0 is the very first frame and 1 is the very last frame.")
        var time = 0.0

        @JvmField
        @Documentation("Sets the scale of this box. The origin is in the bottom left, and 'bounds' is in the top right. Defaults to vec(100,100")
        var bounds = Vec2(100.0, 100.0)

        var shader: Triple<Shader?, Shader?, Shader?>? = null;

        @JvmField
        @Documentation("Sets any color remapping code that's applied to this layer")
        var colorRemap = "";

        @JvmField
        @Documentation("Sets any space remapping code that's applied to this layer")
        var spaceRemap = "";

        @JvmField
        @Documentation("How much is the camera rotated (in degrees)")
        var rotation = 0.0

        @JvmField
        @Documentation("How much is the camera rotated around the Y axis (in degrees)")
        var rotationY = 0.0

        @JvmField
        @Documentation("How much is the camera rotated around the Z axis (in degrees)")
        var rotationZ = 0.0

        @JvmField
        @Documentation("How much is left stereo texture shifted with respect to the right")
        var leftOffset = Vec3(0, 0, 0);

        var P = Mat4().identity()
        var V = Mat4().identity()

        var __camera = Camera()
        var camera: SimpleCamera

        @JvmField
        var is3D = STEREO;

        @JvmField
        var sides = 0;

        var keyboardCamera: KeyboardCamera? = null

        init {
            var s = __camera.state
            s.aspect = (w / h.toFloat());
            if (STEREO)
                s.aspect *= 0.5f;
            s.target = Vec3(0.0, 0.0, 0.0)
            s.up = Vec3(0.0, -1.0, 0.0)
            s.position = Vec3(0.0, 0.0, 2.0)
            __camera.state = s
            camera = SimpleCamera(__camera)

            if (STEREO) {
                keyboardCamera = KeyboardCamera(__camera, insideViewport!!, "" + name)
                keyboardCamera!!.standardMap()
            }

        }

        var vrDefaulted = false

        fun vrDefaults() {
            if (!vrDefaulted) {
                this.vrOptIn = 1.0f
                this.is3D = true
                this.bounds = Vec2(1.0, 1.0)
                this.translation = Vec2(-0.5, -0.5)
                var s = this.__camera.getState()
                val y = 1.6
                s.position = Vec3(0, y, -2)
                s.target = Vec3(0, y, 0)
                this.camera.setState(s)
                vrDefaulted = true
            }
        }

        fun bindPointShader(box: Box): Shader? {
            bindShaderToBox(name, box, 2)
            return shader!!.third!!
        }

        fun bindLineShader(box: Box): Shader? {
            bindShaderToBox(name, box, 1)
            return shader!!.second!!
        }

        fun bindTriangleShader(box: Box): Shader? {
            bindShaderToBox(name, box, 0)
            return shader!!.first!!
        }

        private var vrOptIn = 0f;

        val builders = mutableMapOf<String, kotlin.Pair<MeshBuilder, BaseMesh>>()


        fun lineBuilder(name: String): MeshBuilder {
            return builders.computeIfAbsent(name, {

                val geometry = BaseMesh.lineList(0, 0).setInstances({ if (OculusDrawTarget2.isVR != null || STEREO) 2 else 1 })
                val builder = MeshBuilder(geometry)

                shader!!.second!!.asMap_set(name, geometry)

                builder to geometry
            }).first
        }

        fun lineAdjBuilder(name: String): MeshBuilder {
            return builders.computeIfAbsent(name, {

                val geometry = BaseMesh.lineAdjecencyList(0, 0).setInstances({ if (OculusDrawTarget2.isVR != null || STEREO) 2 else 1 })
                val builder = MeshBuilder(geometry)

                shader!!.second!!.asMap_set(name, geometry)

                builder to geometry
            }).first
        }

        fun triangleBuilder(name: String): MeshBuilder {
            return builders.computeIfAbsent(name, {

                val geometry = BaseMesh.triangleList(0, 0).setInstances({ if (OculusDrawTarget2.isVR != null || STEREO) 2 else 1 })
                geometry.setInstances(2)
                val builder = MeshBuilder(geometry)

                shader!!.first!!.asMap_set(name, geometry)

                geometry.attach(-100, {
                    GL11.glEnable(GL11.GL_DEPTH_TEST)
                    GL11.glDepthFunc(GL11.GL_LESS)
                    GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT)
                    true
                })

                geometry.attach(100, {
                    GL11.glDisable(GL11.GL_DEPTH_TEST)
                    GL11.glDepthFunc(GL11.GL_LESS)
                    GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT)
                    true
                })

                builder to geometry
            }).first
        }

        fun pointBuilder(name: String): MeshBuilder {
            return builders.computeIfAbsent(name, {

                val geometry = BaseMesh.pointList(0).setInstances({ if (OculusDrawTarget2.isVR != null || STEREO) 2 else 1 })
                geometry.setInstances(2)
                val builder = MeshBuilder(geometry)

                shader!!.third!!.asMap_set(name, geometry)

                builder to geometry
            }).first
        }

        fun vrGazeDirection(): Vec3 {

            var m = Mat4(SimpleOculusTarget.o!!.rightView().get())
            var m2 = Mat4(__camera.view().get())
            m2 = m2.transpose()

            m = Mat4.mul(m, m2, Mat4())
            m.invert()

            var a = m.transform(Vec4(0.0, 0.0, 0.0, 1.0))
            var b = m.transform(Vec4(0.0, 0.0, -1.0, 1.0))
            var a2 = Vec3(a.x, a.y, a.z) * (1 / a.w)
            var b2 = Vec3(b.x, b.y, b.z) * (1 / b.w)

            return b2 - a2

        }

        fun vrViewerPosition(): Vec3 {

            var m = Mat4(SimpleOculusTarget.o!!.rightView().get())
            var m2 = Mat4(__camera.view().get())
            m2 = m2.transpose()

            m = Mat4.mul(m, m2, Mat4())
            m.invert()

            var a = m.transform(Vec4(0.0, 0.0, 0.0, 1.0))
            var b = m.transform(Vec4(0.0, 0.0, -1.0, 1.0))
            var a2 = Vec3(a.x, a.y, a.z) * (1 / a.w)
            var b2 = Vec3(b.x, b.y, b.z) * (1 / b.w)

            return a2

        }


        fun vrLeftHandPosition(): Vec3 {

            var at = SimpleOculusTarget.o!!.leftPosition + Vec3(0.0, 1.65, 0.0)
            var m2 = Mat4(__camera.view().get())
            m2 = m2.transpose()
            m2 = m2.invert()
            var al = m2.transform(Vec4(at, 1.0))

            println(al)
            return Vec3(al.x, al.y, al.z) * (1 / al.w)

        }

        fun vrLeftHandDirection(): Vec3 {

            val m = Mat4();
            m.identity()
            val o = SimpleOculusTarget.o!!.leftOrientation

            m.rotate(o)

            val base = m.transform(Vec4(0, 0, 0, 1))
            val end = m.transform(Vec4(0, 0, -1, 1))

            val dir = Vec3(end.x, end.y, end.z) * (1 / end.w) - Vec3(base.x, base.y, base.z) * (1 / base.w)


            var m2 = Mat4(__camera.view().get())
            m2 = m2.transpose()
            m2 = m2.invert()
            var al = m2.transform(Vec4(dir, 0.0))

            return Vec3(al.x, al.y, al.z);
        }


        fun vrRightHandDirection(): Vec3 {

            val m = Mat4();
            m.identity()
            val o = SimpleOculusTarget.o!!.rightOrientation

            m.rotate(o)

            val base = m.transform(Vec4(0, 0, 0, 1))
            val end = m.transform(Vec4(0, 0, -1, 1))

            val dir = Vec3(end.x, end.y, end.z) * (1 / end.w) - Vec3(base.x, base.y, base.z) * (1 / base.w)


            var m2 = Mat4(__camera.view().get())
            m2 = m2.transpose()
            m2 = m2.invert()
            var al = m2.transform(Vec4(dir, 0.0))

            return Vec3(al.x, al.y, al.z);
        }


        fun vrRightHandPosition(): Vec3 {

            var at = SimpleOculusTarget.o!!.rightPosition + Vec3(0.0, 1.65, 0.0)
            var m2 = Mat4(__camera.view().get())
            m2 = m2.transpose()
            m2 = m2.invert()
            var al = m2.transform(Vec4(at, 1.0))

            println(al)
            return Vec3(al.x, al.y, al.z) * (1 / al.w)

        }

        var layerIsDone = false

        fun finishLayer()
        {
            layerIsDone = true
        }


        fun setShader(shader: Triple<Shader?, Shader?, Shader?>): ShaderGroup {

            this.shader = shader

            if (shader.first != null)
                shader.first!!.asMap_set("_planes_", planes)
            if (shader.second != null)
                shader.second!!.asMap_set("_line_", line)
            if (shader.third != null)
                shader.third!!.asMap_set("_point_", points)

            listOf(shader.first, shader.second, shader.third).filter { it != null }.forEachIndexed { index, it ->

                it!!.asMap_set("translation", Supplier { translation })
                it.asMap_set("bounds", Supplier { bounds })
                it.asMap_set("reallyVR", Supplier<Float> {
                    if (OculusDrawTarget2.isVR != null) {
                        1f
                    } else {
                        0f
                    }
                })
                it.asMap_set("vrOptIn", Supplier<Float> {
                    vrOptIn
                })
                it.asMap_set("P", Supplier<Mat4> {
                    if (!is3D) {
                        Mat4().identity()
                    } else {
                        __camera.projectionMatrix()
                    }
                })
                it.asMap_set("V", Supplier<Mat4> {
                    if (!is3D) {
                        Mat4().identity()
                    } else {
                        __camera.view()

                    }
                })
                it.asMap_set("Pl", Supplier<Mat4> {
                    if (!is3D) {
                        Mat4().identity()
                    } else {
                        if (OculusDrawTarget2.isVR != null) {
                            OculusDrawTarget2.isVR!!.leftProjectionMatrix()
                        } else
                            __camera.projectionMatrix(-1f)
                    }
                })
                it.asMap_set("Vl", Supplier<Mat4> {
                    if (!is3D) {
                        Mat4().identity()
                    } else {
                        if (OculusDrawTarget2.isVR != null) {
                            OculusDrawTarget2.isVR!!.leftView()

                        } else
                            __camera.view(-1f)
                    }
                })
                it.asMap_set("Pr", Supplier<Mat4> {
                    if (!is3D) {
                        Mat4().identity()
                    } else {
                        if (OculusDrawTarget2.isVR != null) {
                            OculusDrawTarget2.isVR!!.rightProjectionMatrix()
                        } else
                            __camera.projectionMatrix(1f)
                    }
                })
                it.asMap_set("Vr", Supplier<Mat4> {
                    if (!is3D) {
                        Mat4().identity()
                    } else {
                        if (OculusDrawTarget2.isVR != null) {
                            OculusDrawTarget2.isVR!!.rightView()
                        } else
                            __camera.view(1f)
                    }
                })
                it.asMap_set("scale", Supplier { scale })
                it.asMap_set("sides", Supplier { sides })
                it.asMap_set("opacity", Supplier { opacity.toFloat() })
                it.asMap_set("rotator", Supplier { Vec2(Math.cos(Math.PI * rotation / 180), Math.sin(Math.PI * rotation / 180)) })

                it.asMap_set("isVR", Supplier {
                    if (OculusDrawTarget2.isVR != null || STEREO) 1f else -1f
                })

                it.asMap_set("leftOffset", Supplier<Vec3> { leftOffset })

                fbo.scene.attach(name + "_$index", it)
            }


            fbo.scene.attach(-100, "__doGeometry__" + name, Consumer<Int> {

                if (!layerIsDone)
                {

                    camera.update()

                    lineBuilder.open()
                    planeBuilder.open()
                    pointBuilder.open()
                    try {
                        lines.values.forEach {
                            if (doTexture)
                                it.addAuxProperties(4, StandardFLineDrawing.texCoord.name)
                            StandardFLineDrawing.dispatchLine(it, planeBuilder, lineBuilder, pointBuilder, Optional.empty(), "")
                        }
                    } finally {
                        pointBuilder.close()
                        planeBuilder.close()
                        lineBuilder.close()
                    }
                    remoteHelper?.update(this)
                }
                post.values.forEach { it.run() }
            })
            groups[name] = this
            return this
        }

        fun detatch() {
            fbo.scene.detach(name)
        }

        fun bakeTexture(f: FLine) {
            for (f in f.nodes) {
                f.asMap_set("texCoord", Vec2(f.to.x / bounds.x, f.to.y / bounds.y))
            }
        }

        fun bakeTexture(f: FLine, rotate1: Double, targetRect: Rect) {
            val c = f.center()

            val q = Quat().fromAxisAngleDeg(Vec3(0, 0, 1), rotate1)

            val f2 = f.byTransforming { q.transform(Vec3(it)) }

            val b = f2.bounds()

            f2.nodes.forEachIndexed { index, f ->

                val x0 = (Vec2(f.to.x - c.x, f.to.y - c.y))

                val x = (x0.x - b.x) / b.w
                val y = (x0.y - b.y) / b.h

                f.asMap_set("texCoord", Vec2(x * targetRect.w + targetRect.x, y * targetRect.h + targetRect.y))
            }
        }

        fun bakeTexture(f: FLine, sourceRect: Rect, targetRect: Rect) {
            val c = f.center()

            for (f in f.nodes) {

                val x0 = Vec2(f.to.x - c.x, f.to.y - c.y)

                val x = (x0.x - sourceRect.x) / sourceRect.w
                val y = (x0.y - sourceRect.y) / sourceRect.h

                f.asMap_set("texCoord", Vec2(x * targetRect.w + targetRect.x, y * targetRect.h + targetRect.y))
            }
        }

        var webcamDriver: WebcamDriver? = null

    }

    @JvmField
    val lines = default_group.lines

    private fun defaultShader(inside: ShaderGroup): Triple<Shader?, Shader?, Shader?> {
        val s1 = Shader()
        s1.addSource(Shader.Type.vertex, default_vertex)
        s1.addSource(Shader.Type.geometry, default_geometry_triangles)
        s1.addSource(Shader.Type.fragment, default_fragment)
        val s2 = Shader()
        s2.addSource(Shader.Type.vertex, default_vertex)
        s2.addSource(Shader.Type.geometry, default_geometry_lines)
        s2.addSource(Shader.Type.fragment, default_fragment)

        val s3 = Shader()
        s3.addSource(Shader.Type.vertex, default_vertex)
        s3.addSource(Shader.Type.geometry, default_geometry_points)
        s3.addSource(Shader.Type.fragment, default_fragment_points)

        return Triple(s1, s2, s3);
    }

    fun bindShaderToBox(name: String, box: Box, kind: Int) {
        if (groups[name] != null) {
            box.first(GraphicsSupport.bindShader, box.upwards()).ifPresent {
                if (kind == 0)
                    it.apply(box, groups[name]!!.shader!!.first)
                else if (kind == 1)
                    it.apply(box, groups[name]!!.shader!!.second)
                else if (kind == 2)
                    it.apply(box, groups[name]!!.shader!!.third)
            }
        } else
            throw IllegalArgumentException(" can't find a ShaderGroup called '$name'")
    }

    @HiddenInAutocomplete
    val show_vertex = BoxDefaultCode.findSource(this.javaClass, "show_vertex")
    @HiddenInAutocomplete
    val show_fragment = BoxDefaultCode.findSource(this.javaClass, "show_fragment")

    @HiddenInAutocomplete
    val show_vertex_stereo = BoxDefaultCode.findSource(this.javaClass, "show_vertex_stereo")
    @HiddenInAutocomplete
    val show_fragment_stereo = BoxDefaultCode.findSource(this.javaClass, "show_fragment_stereo")

    @HiddenInAutocomplete
    val show_vertex_vr = BoxDefaultCode.findSource(this.javaClass, "show_vertex_vr")
    @HiddenInAutocomplete
    val show_fragment_vr = BoxDefaultCode.findSource(this.javaClass, "show_fragment_vr")

    // todo: filename mapping

    @Documentation("Make a layer with a given texture map. Filename should be a jpeg image.")
    fun withTexture(filename: String): ShaderGroup {
        val n = groups.get(filename)
        if (n != null)
            return n

        if (groups.containsKey(filename)) {
            groups.remove(filename)!!.detatch()
        }

        val sg = ShaderGroup(filename)

        val s1 = Shader()
        s1.addSource(Shader.Type.vertex, texture_vertex)
        s1.addSource(Shader.Type.geometry, texture_geometry_triangles)
        s1.addSource(Shader.Type.fragment, ShaderPreprocessor().Preprocess(null, texture_fragment, Supplier {
            mutableMapOf("COLOR_REMAP" to sg.colorRemap, "SPACE_REMAP" to sg.spaceRemap)
        }))

        var tt = Texture(Texture.TextureSpecification.fromJpeg(3, filename, true))
        var tt2 = Texture(Texture.TextureSpecification.fromJpeg(4, filename, true))

        s1.asMap_set("source", tt)
        s1.asMap_set("source2", tt2)

        val s2 = Shader()
        s2.addSource(Shader.Type.vertex, texture_vertex)
        s2.addSource(Shader.Type.geometry, texture_geometry_lines)
        s2.addSource(Shader.Type.fragment, ShaderPreprocessor().Preprocess(null, texture_fragment, Supplier {
            mutableMapOf("COLOR_REMAP" to sg.colorRemap, "SPACE_REMAP" to sg.spaceRemap)
        }))

        s2.asMap_set("source", tt)
        s2.asMap_set("source2", tt2)

        val s3 = Shader()
        s3.addSource(Shader.Type.vertex, texture_vertex)
        s3.addSource(Shader.Type.geometry, texture_geometry_points)
        s3.addSource(Shader.Type.fragment, ShaderPreprocessor().Preprocess(null, texture_fragment_points, Supplier {
            mutableMapOf("COLOR_REMAP" to sg.colorRemap, "SPACE_REMAP" to sg.spaceRemap)
        }))

        s3.asMap_set("source", tt)
        s3.asMap_set("source2", tt2)

        try {
            val box = Execution.context.get().peek()
            s1.sources.get(Shader.Type.vertex)!!.setOnError(errorHandler(box, "color_remap"));
            s1.sources.get(Shader.Type.fragment)!!.setOnError(errorHandler(box, "color_remap"));
            s1.sources.get(Shader.Type.geometry)!!.setOnError(errorHandler(box, "color_remap"));
            s1.setOnError(errorHandler(box, "color_remap"));
            s2.sources.get(Shader.Type.vertex)!!.setOnError(errorHandler(box, "color_remap"));
            s2.sources.get(Shader.Type.fragment)!!.setOnError(errorHandler(box, "color_remap"));
            s2.sources.get(Shader.Type.geometry)!!.setOnError(errorHandler(box, "color_remap"));
            s2.setOnError(errorHandler(box, "color_remap"));
            s3.sources.get(Shader.Type.vertex)!!.setOnError(errorHandler(box, "color_remap"));
            s3.sources.get(Shader.Type.fragment)!!.setOnError(errorHandler(box, "color_remap"));
            s3.sources.get(Shader.Type.geometry)!!.setOnError(errorHandler(box, "color_remap"));
            s3.setOnError(errorHandler(box, "color_remap"));
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        sg.setShader(Triple(s1, s2, s3));
        sg.doTexture = true
        sg.textureFilename = filename
        sg.textureDimensions = Vec2(tt.specification.width, tt.specification.height)
        return sg
    }

    @Documentation("Make a layer with a given left-right stereo texture pair. Filenames should be a jpeg images.")
    fun withStereoTexture(filename: String, filename2: String): ShaderGroup {
        val n = groups.get(filename + "|" + filename2)
        if (n != null)
            return n

        if (groups.containsKey(filename + "|" + filename2)) {
            groups.remove(filename + "|" + filename2)!!.detatch()
        }

        val sg = ShaderGroup(filename + "|" + filename2)

        val s1 = Shader()
        s1.addSource(Shader.Type.vertex, texture_vertex)
        s1.addSource(Shader.Type.geometry, texture_geometry_triangles)
        s1.addSource(Shader.Type.fragment, ShaderPreprocessor().Preprocess(null, texture_fragment, Supplier {
            mutableMapOf("COLOR_REMAP" to sg.colorRemap, "SPACE_REMAP" to sg.spaceRemap)
        }))

        val tt = Texture(Texture.TextureSpecification.fromJpeg(3, filename, true))
        val tt2 = Texture(Texture.TextureSpecification.fromJpeg(4, filename2, true))
        s1.asMap_set("source", tt)
        s1.asMap_set("source2", tt2)

        val s2 = Shader()
        s2.addSource(Shader.Type.vertex, texture_vertex)
        s2.addSource(Shader.Type.geometry, texture_geometry_lines)
        s2.addSource(Shader.Type.fragment, ShaderPreprocessor().Preprocess(null, texture_fragment, Supplier {
            mutableMapOf("COLOR_REMAP" to sg.colorRemap, "SPACE_REMAP" to sg.spaceRemap)
        }))

        s2.asMap_set("source", tt)
        s2.asMap_set("source2", tt2)

        val s3 = Shader()
        s3.addSource(Shader.Type.vertex, texture_vertex)
        s3.addSource(Shader.Type.geometry, texture_geometry_points)
        s3.addSource(Shader.Type.fragment, ShaderPreprocessor().Preprocess(null, texture_fragment_points, Supplier {
            mutableMapOf("COLOR_REMAP" to sg.colorRemap, "SPACE_REMAP" to sg.spaceRemap)
        }))

        s3.asMap_set("source", tt)
        s3.asMap_set("source2", tt2)

        try {
            val box = Execution.context.get().peek()
            s1.sources.get(Shader.Type.vertex)!!.setOnError(errorHandler(box, "color_remap"));
            s1.sources.get(Shader.Type.fragment)!!.setOnError(errorHandler(box, "color_remap"));
            s1.sources.get(Shader.Type.geometry)!!.setOnError(errorHandler(box, "color_remap"));
            s1.setOnError(errorHandler(box, "color_remap"));
            s2.sources.get(Shader.Type.vertex)!!.setOnError(errorHandler(box, "color_remap"));
            s2.sources.get(Shader.Type.fragment)!!.setOnError(errorHandler(box, "color_remap"));
            s2.sources.get(Shader.Type.geometry)!!.setOnError(errorHandler(box, "color_remap"));
            s2.setOnError(errorHandler(box, "color_remap"));
            s3.sources.get(Shader.Type.vertex)!!.setOnError(errorHandler(box, "color_remap"));
            s3.sources.get(Shader.Type.fragment)!!.setOnError(errorHandler(box, "color_remap"));
            s3.sources.get(Shader.Type.geometry)!!.setOnError(errorHandler(box, "color_remap"));
            s3.setOnError(errorHandler(box, "color_remap"));
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        sg.setShader(Triple(s1, s2, s3));
        sg.doTexture = true
        sg.textureFilename = filename
        sg.textureDimensions = Vec2(tt.specification.width, tt.specification.height)
        return sg
    }

    @Documentation("Make a layer with a given texture map. Filename should be a jpeg image.")
    fun withWebcam(): ShaderGroup {
        val filename = "__webcam__"
        val n = groups.get(filename)
        if (n != null)
            return n

        if (groups.containsKey(filename)) {
            groups.remove(filename)!!.detatch()
        }

        val sg = ShaderGroup(filename)

        val s1 = Shader()
        s1.addSource(Shader.Type.vertex, texture_vertex)
        s1.addSource(Shader.Type.geometry, texture_geometry_triangles)
        s1.addSource(Shader.Type.fragment, ShaderPreprocessor().Preprocess(null, texture_fragment, Supplier {
            mutableMapOf("COLOR_REMAP" to sg.colorRemap, "SPACE_REMAP" to sg.spaceRemap)
        }))


        val s2 = Shader()
        s2.addSource(Shader.Type.vertex, texture_vertex)
        s2.addSource(Shader.Type.geometry, texture_geometry_lines)
        s2.addSource(Shader.Type.fragment, ShaderPreprocessor().Preprocess(null, texture_fragment, Supplier {
            mutableMapOf("COLOR_REMAP" to sg.colorRemap, "SPACE_REMAP" to sg.spaceRemap)
        }))


        val s3 = Shader()
        s3.addSource(Shader.Type.vertex, texture_vertex)
        s3.addSource(Shader.Type.geometry, texture_geometry_points)
        s3.addSource(Shader.Type.fragment, ShaderPreprocessor().Preprocess(null, texture_fragment_points, Supplier {
            mutableMapOf("COLOR_REMAP" to sg.colorRemap, "SPACE_REMAP" to sg.spaceRemap)
        }))

        val wd = WebcamDriver()
        val ts = wd.texture
        s1.asMap_set("source", ts)
        s2.asMap_set("source", ts)
        s3.asMap_set("source", ts)


        try {
            val box = Execution.context.get().peek()
            s1.sources.get(Shader.Type.vertex)!!.setOnError(errorHandler(box, "color_remap"));
            s1.sources.get(Shader.Type.fragment)!!.setOnError(errorHandler(box, "color_remap"));
            s1.sources.get(Shader.Type.geometry)!!.setOnError(errorHandler(box, "color_remap"));
            s1.setOnError(errorHandler(box, "color_remap"));
            s2.sources.get(Shader.Type.vertex)!!.setOnError(errorHandler(box, "color_remap"));
            s2.sources.get(Shader.Type.fragment)!!.setOnError(errorHandler(box, "color_remap"));
            s2.sources.get(Shader.Type.geometry)!!.setOnError(errorHandler(box, "color_remap"));
            s2.setOnError(errorHandler(box, "color_remap"));
            s3.sources.get(Shader.Type.vertex)!!.setOnError(errorHandler(box, "color_remap"));
            s3.sources.get(Shader.Type.fragment)!!.setOnError(errorHandler(box, "color_remap"));
            s3.sources.get(Shader.Type.geometry)!!.setOnError(errorHandler(box, "color_remap"));
            s3.setOnError(errorHandler(box, "color_remap"));
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        sg.setShader(Triple(s1, s2, s3));
        sg.doTexture = true
        sg.webcamDriver = wd;
        sg.post.put("texupdate", Runnable {
            println(" wd update")
            wd.update()
        })
        return sg
    }

    @Documentation("Make a layer that loads 'video' material from a stream of jpeg images in the given directory. These layers have an additional 'time' property that goes from 0 to 1 (from the start of the image sequence to the end).")
    fun withImageSequence(filename: String): ShaderGroup {
        val n = groups.get(filename)
        if (n != null)
            return n

        if (groups.containsKey(filename)) {
            groups.remove(filename)!!.detatch()
        }

        val sg = ShaderGroup(filename)

        val s1 = Shader()
        s1.addSource(Shader.Type.vertex, video_vertex)
        s1.addSource(Shader.Type.geometry, video_geometry_triangles)
        s1.addSource(Shader.Type.fragment, ShaderPreprocessor().Preprocess(null, video_fragment, Supplier {
            mutableMapOf("COLOR_REMAP" to sg.colorRemap, "SPACE_REMAP" to sg.spaceRemap)
        }))

        val s2 = Shader()
        s2.addSource(Shader.Type.vertex, video_vertex)
        s2.addSource(Shader.Type.geometry, video_geometry_lines)
        s2.addSource(Shader.Type.fragment, ShaderPreprocessor().Preprocess(null, video_fragment, Supplier {
            mutableMapOf("COLOR_REMAP" to sg.colorRemap, "SPACE_REMAP" to sg.spaceRemap)
        }))

        val s3 = Shader()
        s3.addSource(Shader.Type.vertex, video_vertex)
        s3.addSource(Shader.Type.geometry, video_geometry_points)
        s3.addSource(Shader.Type.fragment, ShaderPreprocessor().Preprocess(null, video_fragment_points, Supplier {
            mutableMapOf("COLOR_REMAP" to sg.colorRemap, "SPACE_REMAP" to sg.spaceRemap)
        }))

        val map = ImageCache.mapFromDirectory(filename, ".*.jpg")

        // todo image size
        if (map.length() == 0) throw IllegalArgumentException(" doesn't seem to be any .jpg files in directory $filename ?")

        val fn = map.apply(0);
        val dim = FastJPEG.j.dimensions(fn);

        val ic = ImageCache(dim[0], dim[1], 90, 40, map)
        val cache = TwinTextureCache(0, ic)
        cache.setPlaying(true)

        ImageCache.synchronous = false;

        cache.setTime(1.0)
//        cache.update()

        // todo, a lot more debug than this

        s1.asMap_set("T0", cache.textureA)
        s1.asMap_set("T1", cache.textureB)
        s1.asMap_set("alpha", Supplier { cache.getAlpha() })
        s2.asMap_set("T0", cache.textureA)
        s2.asMap_set("T1", cache.textureB)
        s2.asMap_set("alpha", Supplier { cache.getAlpha() })
        s3.asMap_set("T0", cache.textureA)
        s3.asMap_set("T1", cache.textureB)
        s3.asMap_set("alpha", Supplier { cache.getAlpha() })

        sg.setShader(Triple(s1, s2, s3))
        sg.doTexture = true
        sg.post.put("__dovideo__", Runnable {
            cache.setTime(map.length() * sg.time);
            cache.update()
        })

        try {
            val box = Execution.context.get().peek()
            s1.sources.get(Shader.Type.vertex)!!.setOnError(errorHandler(box, "color_remap"));
            s1.sources.get(Shader.Type.geometry)!!.setOnError(errorHandler(box, "color_remap"));
            s1.sources.get(Shader.Type.fragment)!!.setOnError(errorHandler(box, "color_remap"));
            s1.setOnError(errorHandler(box, "color_remap"));
            s2.sources.get(Shader.Type.vertex)!!.setOnError(errorHandler(box, "color_remap"));
            s2.sources.get(Shader.Type.geometry)!!.setOnError(errorHandler(box, "color_remap"));
            s2.sources.get(Shader.Type.fragment)!!.setOnError(errorHandler(box, "color_remap"));
            s2.setOnError(errorHandler(box, "color_remap"));
            s3.sources.get(Shader.Type.vertex)!!.setOnError(errorHandler(box, "color_remap"));
            s3.sources.get(Shader.Type.geometry)!!.setOnError(errorHandler(box, "color_remap"));
            s3.sources.get(Shader.Type.fragment)!!.setOnError(errorHandler(box, "color_remap"));
            s3.setOnError(errorHandler(box, "color_remap"));
        } catch (e: IndexOutOfBoundsException) {
            e.printStackTrace()
        }

        return sg
    }


    @Documentation("Make a layer with a particular name. If a layer already exists with this name, just look it up. Layers have cameras, opacities and color / space remappers.")
    fun withName(name: String): ShaderGroup {
        val n = groups.get(name)
        if (n != null)
            return n

        val g = ShaderGroup(name)
        g.setShader(defaultShader(g))
        return g;
    }

    // should texturing somehow be default?

    fun withStage(stage: Stage): ShaderGroup {
        val name = "__stage__" + System.identityHashCode(stage)
        val n = groups.get(name)
        if (n != null)
            return n

        if (groups.containsKey(name)) {
            groups.remove(name)!!.detatch()
        }

        val sg = ShaderGroup(name)
        val s1 = Shader()
        s1.addSource(Shader.Type.vertex, texture_vertex)
        s1.addSource(Shader.Type.geometry, texture_geometry_triangles)
        s1.addSource(Shader.Type.fragment, ShaderPreprocessor().Preprocess(null, texture_fragment, Supplier {
            mutableMapOf("COLOR_REMAP" to sg.colorRemap, "SPACE_REMAP" to sg.spaceRemap)
        }))
        s1.asMap_set("source", stage.fbo)
        val s2 = Shader()
        s2.addSource(Shader.Type.vertex, texture_vertex)
        s2.addSource(Shader.Type.geometry, texture_geometry_triangles)
        s2.addSource(Shader.Type.fragment, ShaderPreprocessor().Preprocess(null, texture_fragment, Supplier {
            mutableMapOf("COLOR_REMAP" to sg.colorRemap, "SPACE_REMAP" to sg.spaceRemap)
        }))
        s2.asMap_set("source", stage.fbo)

        sg.setShader(Triple(s1, s2, null))
        sg.doTexture = true

        try {
            val box = Execution.context.get().peek()
            s1.sources.get(Shader.Type.vertex)!!.setOnError(errorHandler(box, "color_remap"));
            s1.sources.get(Shader.Type.fragment)!!.setOnError(errorHandler(box, "color_remap"));
            s1.setOnError(errorHandler(box, "color_remap"));
            s2.sources.get(Shader.Type.vertex)!!.setOnError(errorHandler(box, "color_remap"));
            s2.sources.get(Shader.Type.fragment)!!.setOnError(errorHandler(box, "color_remap"));
            s2.setOnError(errorHandler(box, "color_remap"));
        } catch (e: IndexOutOfBoundsException) {
            e.printStackTrace()
        }

        return sg
    }


    private var insideViewport: Viewport? = null

    fun show(name: String, v: Viewport): Shader {
        insideViewport = v;
        return showScene(name, (v get Viewport.scene)!!, { isOut }, { v.properties.get(Box.frame).w / v.properties.get(Box.frame).h.toDouble() }, false)
    }


    private var masterShader: Shader? = null

    fun bindShowShaderToBox(box: Box): Shader {
        if (masterShader == null) throw IllegalArgumentException(" can't bind a show shader if the box isn't being shown ")
        box.first(GraphicsSupport.bindShader, box.upwards()).ifPresent {
            it.apply(box, masterShader!!)
        }

        return masterShader!!;
    }

    fun showScene(name: String, scene: Scene, disabled: () -> Boolean, asp: () -> Double, isFullscreen: Boolean): Shader {
        val s = Shader()

        if (isFullscreen && VR) {
            s.addSource(Shader.Type.vertex, ShaderPreprocessor().Preprocess(null, show_vertex_vr, Supplier {
                mutableMapOf("REMAP" to remap)
            }))
            s.addSource(Shader.Type.fragment, ShaderPreprocessor().Preprocess(null, show_fragment_vr, Supplier {
                mutableMapOf("REMAP" to remap)
            }))

        } else
            if (isFullscreen && STEREO) {
                s.addSource(Shader.Type.vertex, ShaderPreprocessor().Preprocess(null, show_vertex_stereo, Supplier {
                    mutableMapOf("REMAP" to remap)
                }))
                s.addSource(Shader.Type.fragment, ShaderPreprocessor().Preprocess(null, show_fragment_stereo, Supplier {
                    mutableMapOf("REMAP" to remap)
                }))

            } else {
                s.addSource(Shader.Type.vertex, ShaderPreprocessor().Preprocess(null, show_vertex, Supplier {
                    mutableMapOf("REMAP" to remap)
                }))
                s.addSource(Shader.Type.fragment, ShaderPreprocessor().Preprocess(null, show_fragment, Supplier {
                    mutableMapOf("REMAP" to remap)
                }))
            }

        s.asMap_set("disabled", Supplier { if (disabled.invoke()) 1f else 0f })

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
        s.asMap_set("aspect", Supplier { ((h) / w.toFloat()) * asp() });

        scene.asMap_set(name, s);
        scene.attach(-100, "__draw__" + name, Consumer<Int> {
            fbo.draw()
        })

        masterShader = s;

        return s;
    }

    @JvmField
    var keyboard: Buttons = Buttons()

    init {
        default_group.post.put("__keyboard__", keyboard)
    }

    fun popOut(): Shader? {
        if (isOut) {
            // toFront
            return null;
        }
        isOut = true;

        return ThreadSync2.callInMainThreadAndWait(Callable {
            if (window == null) {
                window = object : Window(0, 0, w / 2 - 1, h / 2 - 1, "Field / Stage $thisStageNum") {
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

                keyboard.attachToWindow(window!!)

            }

            showScene("default", window!!.scene, { false }, { window!!.width.toDouble() / window!!.height }, true)
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
    fun frame(): Boolean {
        redraw()
        return ThreadSync2Feedback.maybeYield();
    }

    fun errorHandler(b: Box, shader: String): Shader.iErrorHandler {
        return object : Shader.iErrorHandler {
            override fun beginError() {

            }

            override fun errorOnLine(line: Int, error: String) {
                b.first<java.util.function.Function<Box, Consumer<Pair<Int?, String>>>>(RemoteEditor.outputErrorFactory)
                        .orElse(java.util.function.Function { x -> Consumer { `is` -> System.err.println("error (without remote editor attached) :" + `is`) } })
                        .apply(b)
                        .accept(Pair(null, "Error on $shader reload: $error"))
            }

            override fun endError() {

            }

            override fun noError() {
                b.first<java.util.function.Function<Box, Consumer<String>>>(RemoteEditor.outputFactory)
                        .orElse(java.util.function.Function { x -> Consumer { `is` -> System.err.println("message (without remote editor attached) :" + `is`) } })
                        .apply(b)
                        .accept(shader + " reloaded correctly")
            }
        }
    }

    fun startSaving(): String {
        return saver.start()
    }


    fun startSaving(path : String): String {
        saver.setPrefix(path)
        saver.frameNumber = 0

        return saver.start()
    }

    fun stopSaving() {
        saver.stop()
    }


}

