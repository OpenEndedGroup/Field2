package trace.graphics

//import trace.input.KinectDriver
//import trace.input.WebcamDriver
//import trace.util.FLineToSVG
import field.app.ThreadSync2
import field.graphics.*
import field.graphics.util.KeyboardCamera
import field.graphics.util.Saver
import field.graphics.util.SaverFBO
import field.graphics.util.onsheetui.get
import field.linalg.*
import field.utility.*
import fieldbox.boxes.Box
import fieldbox.boxes.Drawing
import fieldbox.boxes.plugins.*
import fieldbox.execution.Execution
import fieldbox.ui.GlfwCallbackDelegate
import fielded.RemoteEditor
import fieldlinker.AsMap
import fieldnashorn.annotations.HiddenInAutocomplete
import org.lwjgl.opengl.GL11
import trace.graphics.remote.RemoteServer
import trace.graphics.remote.RemoteStageLayerHelper
import trace.input.Buttons
import trace.input.WebcamDriver3
import trace.input.SimpleMouse
import trace.util.LinePiper
import trace.video.ImageCache
import trace.video.SimpleHead
import trace.video.TwinTextureCache
import java.io.File
import java.lang.reflect.Modifier
import java.nio.ByteBuffer
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
        val m = this.javaClass.methods
        for (mm in m) r.add(mm.name)
        val f = this.javaClass.fields
        for (ff in f) if (!Modifier.isStatic(ff.modifiers)) r.add(ff.name)

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

    @JvmField
    val fbo: FBO

    @JvmField
    @Documentation("The background color of the stage")
    var background = Vec4(0.5, 0.5, 0.5, 1.0)

    @JvmField
    var window: Window? = null
    var isOut = false

    companion object {
        var stageNum: Int = 0

        @JvmField
        var VR: Boolean = false

        @JvmStatic
        var rs: RemoteServer? = null

        init {
            try {
                rs = RemoteServer()
            } catch (e: Throwable) {
                println(" -- error, can't start RemoteServer. No AR for you then --")
            }
        }

        var max_vertex = 60000
        var max_element = 60000
        var doRemote = false
    }

    val thisStageNum: Int


    @HiddenInAutocomplete
    val clear_vertex = BoxDefaultCode.findSource(this.javaClass, "clear_vertex")

    @HiddenInAutocomplete
    val clear_fragment = BoxDefaultCode.findSource(this.javaClass, "clear_fragment")

    @HiddenInAutocomplete
    val saver: SaverFBO

    val LATENCY = 1


    init {
        thisStageNum = stageNum++
//        fbo = FBOStack(FBO.FBOSpecification.singleFloat16_depth(thisStageNum, w, h), LATENCY)
        fbo = FBO(FBO.FBOSpecification.singleFloat16(thisStageNum, w, h))

        val base =
            System.getProperty("user.home") + File.separatorChar + "Desktop" + File.separatorChar + "field_stage_recordings" + File.separatorChar

        var x = 1
        while (File(base + Saver.pad(x)).exists()) x++

        val prefix = File(base + Saver.pad(x))
        prefix.mkdirs()

        saver = SaverFBO(w, h, 4, prefix.absolutePath + File.separatorChar + "s_") { fbo }

        val s = Shader()

        s.addSource(Shader.Type.vertex, clear_vertex)
        s.addSource(Shader.Type.fragment, clear_fragment)

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
        s.asMap_set("color", Supplier { background })

        s.attach(100, {

            GL11.glColorMask(false, false, false, true);
            GL11.glClearColor(0f, 0f, 0f, 0f);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT or GL11.GL_DEPTH_BUFFER_BIT);
            GL11.glColorMask(true, true, true, true);
//            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)

            true
        })

        s.attach(-99, {

            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
//            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);

            true
        })

        var tick = 0

        fbo.scene.attach(-101) {
            if (STEREO || SimpleOculusTarget.isVR()) GL11.glEnable(GL11.GL_CLIP_PLANE0)
            GL11.glEnable(GL11.GL_BLEND)
            GL11.glDisable(GL11.GL_DEPTH_TEST)
            GL11.glDepthFunc(GL11.GL_LESS)
//            GL11.glEnable(GL32.GL_DEPTH_CLAMP)
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
//            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);

//            if (tick++ < LATENCY*8)
//                GL11.glClear(GL11.GL_COLOR_BUFFER_BIT or GL11.GL_DEPTH_BUFFER_BIT)

            false

//            tick++ < LATENCY

        }
        fbo.scene.attach("background_clear", s)
        fbo.scene.attach(-101, Scene.Perform {
            GL11.glClearDepth(1.0)
//            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT or GL11.GL_DEPTH_BUFFER_BIT)
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
        val line = BaseMesh.lineList(0, 0).setInstances { if (SimpleOculusTarget.isVR() != null || STEREO) 2 else 1 }
        val lineBuilder = MeshBuilder(line)
        val planes = BaseMesh.triangleList(0, 0).setInstances { if (SimpleOculusTarget.isVR() || STEREO) 2 else 1 }
        val planeBuilder = MeshBuilder(planes)
        val points = BaseMesh.pointList(0).setInstances { if (SimpleOculusTarget.isVR() || STEREO) 2 else 1 }
        val pointBuilder = MeshBuilder(points)
        val post = IdempotencyMap<Runnable>(Runnable::class.java)

        val remoteHelper = if (doRemote) RemoteStageLayerHelper(rs!!.s, max_vertex, max_element, -1, name) else null

        val lines = IdempotencyMap<FLine>(FLine::class.java).configureResourceLimits<IdempotencyMap<FLine>>(
            1000,
            "too many lines on a stage layer"
        )

        @JvmField
        @Documentation("list of `FLine` to draw, just like `_.lines` but it will appear on this Stage")
        val __lines =
            LinePiper<IdempotencyMap<FLine>>(lines, { IdempotencyMap(FLine::class.java) }, { it.duplicate() }, LATENCY)

        var doTexture = false
        var textureFilename: String? = null
        var textureDimensions = Vec2(1, 1)

        @JvmField
        @Documentation("Camera translation. Starts at vec(0,0)")
        var translation = Vec2()

        @JvmField
        @Documentation("Camera 'scale'. Numbers >1 zoom in, Numbers <1 zoom out. Starts as vec(1,1).")
        var scale = Vec2(1.0, 1.0)

        @JvmField
        @Documentation("The opacity of this layer")
        var opacity = 1.0

        @JvmField
        @Documentation("If this layer is a video layer this `time` represents the fraction through the video that we are. i.e. 0 is the very first frame and 1 is the very last frame.")
        var time = 0.0

        var frameSet = false
        var _frame = 0
        fun setFrame(f: Int) {
            _frame = f
            frameSet = true
        }

        var imageBytes: (() -> ByteBuffer?)? = null
        var imageBytesPerPixel = 3
        var imageDimensions = 0 to 0

        var vrButtons: SimpleButtons? = if (SimpleOculusTarget.isVR()) SimpleButtons(SimpleOculusTarget.o2!!) else null

        @JvmField
        @Documentation("Sets the scale of this box. The origin is in the bottom left, and 'bounds' is in the top right. Defaults to vec(100,100")
        var bounds = Vec2(100.0, 100.0)

        var shader: Triple<Shader?, Shader?, Shader?>? = null

        @JvmField
        @Documentation("Sets any color remapping code that's applied to this layer")
        var colorRemap = ""

        @JvmField
        @Documentation("Color to multiply this video by")
        var colorMul = Vec3(1, 1, 1)

        @JvmField
        @Documentation("Color to add to this video")
        var colorAdd = Vec3(0, 0, 0)

        @JvmField
        @Documentation("Sets any space remapping code that's applied to this layer")
        var spaceRemap = ""

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
        var leftOffset = Vec3(0, 0, 0)


        @JvmField
        @Documentation("Additional motion blur")
        var blur = Vec3(0, 0, 0)

        var P = Mat4().identity()
        var V = Mat4().identity()

        var __camera = Camera()
        var camera: SimpleCamera

        @JvmField
        var is3D = STEREO

        @JvmField
        var sides = 0

        var keyboardCamera: KeyboardCamera? = null

        init {
            val s = __camera.state
            s.aspect = (w / h.toFloat())
            if (STEREO) s.aspect *= 0.5f
            s.target = Vec3(0.0, 0.0, 0.0)
            s.up = Vec3(0.0, -1.0, 0.0)
            s.position = Vec3(0.0, 0.0, 2.0)
            __camera.state = s
            camera = SimpleCamera(__camera)

            if (STEREO) {
                keyboardCamera = KeyboardCamera(__camera, insideViewport!!, "" + name)
                keyboardCamera!!.standardMap()
            }

            if (SimpleOculusTarget.isVR()) {
                vrDefaults()
            }

        }

//        fun toSVG(fn: String) {
//            val f = FLineToSVG(fn)
//            lines.values.forEach {
//                f.addLine(it, this)
//            }
//            f.save()
//        }


        var vrDefaulted = false

        fun vrDefaults() {
            if (!vrDefaulted) {
                this.vrOptIn = 1.0f
                this.is3D = true
                this.bounds = Vec2(1.0, 1.0)
                this.translation = Vec2(-0.5, -0.5)
                val s = this.__camera.getState()
                val y = 1.6
                s.position = Vec3(0, y, -2)
                s.target = Vec3(0, y, 0)
                this.camera.setState(s)
                vrDefaulted = true
            }
        }

        fun makeKeyboardCamera() {
            keyboardCamera = KeyboardCamera(__camera, insideViewport!!, "" + name)
            keyboardCamera!!.standardMap()
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

        fun sampleImage(x: Double, y: Double): Vec3 {
            if (imageBytes == null) return Vec3()
            val b = imageBytes?.invoke()

            val px = Math.max(0, Math.min(imageDimensions.first - 1, (x * imageDimensions.first).toInt()))
            val py = Math.max(0, Math.min(imageDimensions.second - 1, (y * imageDimensions.second).toInt()))

            val off = 3 * py * imageDimensions.first + 3 * px
            return Vec3(
                (b!!.get(off + 0).toInt() and 0xff) / 255.0,
                (b!!.get(off + 1).toInt() and 0xff) / 255.0,
                (b!!.get(off + 2).toInt() and 0xff) / 255.0
            )
        }

        private var vrOptIn = 0f

        val builders = mutableMapOf<String, kotlin.Pair<MeshBuilder, BaseMesh>>()


        fun lineBuilder(name: String): MeshBuilder {
            return builders.computeIfAbsent(name) {

                val geometry =
                    BaseMesh.lineList(0, 0).setInstances { if (SimpleOculusTarget.isVR() || STEREO) 2 else 1 }
                val builder = MeshBuilder(geometry)

                shader!!.second!!.asMap_set(name, geometry)

                builder to geometry
            }.first
        }

        fun lineAdjBuilder(name: String): MeshBuilder {
            return builders.computeIfAbsent(name) {

                val geometry =
                    BaseMesh.lineAdjecencyList(0, 0).setInstances { if (SimpleOculusTarget.isVR() || STEREO) 2 else 1 }
                val builder = MeshBuilder(geometry)

                shader!!.second!!.asMap_set(name, geometry)

                builder to geometry
            }.first
        }

        var rawTriangles = IdempotencyMap<MeshBuilder>(MeshBuilder::class.java).setAutoconstruct { triangleBuilder(it) }
        var rawLines = IdempotencyMap<MeshBuilder>(MeshBuilder::class.java).setAutoconstruct { lineBuilder(it) }
        var rawPoints = IdempotencyMap<MeshBuilder>(MeshBuilder::class.java).setAutoconstruct { pointBuilder(it) }


        fun triangleBuilder(name: String): MeshBuilder {
            return builders.computeIfAbsent(name) {

                val geometry =
                    BaseMesh.triangleList(0, 0).setInstances { if (SimpleOculusTarget.isVR() || STEREO) 2 else 1 }
                geometry.setInstances(2)
                val builder = MeshBuilder(geometry)

                builder.open()
                builder.aux(1, 0.3f, 0.6f, 0.8f, 1f)
                builder.close()

                shader!!.first!!.asMap_set(name, geometry)

                geometry.attach(-100) {
//                    GL11.glEnable(GL11.GL_DEPTH_TEST)
                    GL11.glDepthFunc(GL11.GL_LESS)
                    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);

//                    GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT)
                    true
                }

                geometry.attach(100) {
                    GL11.glDisable(GL11.GL_DEPTH_TEST)
                    GL11.glDepthFunc(GL11.GL_LESS)
//                    GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT)
                    true
                }

                builder to geometry
            }.first
        }

        fun pointBuilder(name: String): MeshBuilder {
            return builders.computeIfAbsent(name) {

                val geometry = BaseMesh.pointList(0).setInstances { if (SimpleOculusTarget.isVR() || STEREO) 2 else 1 }
                geometry.setInstances(2)
                val builder = MeshBuilder(geometry)

                shader!!.third!!.asMap_set(name, geometry)

                builder to geometry
            }.first
        }

        fun vrGazeDirection(): Vec3 {
            if (SimpleOculusTarget.o != null) {

                var m = Mat4(SimpleOculusTarget.o!!.rightView().get())
                var m2 = Mat4(__camera.view().get())
                m2 = m2.transpose()

                m = Mat4.mul(m, m2, Mat4())
                m.invert()

                val a = m.transform(Vec4(0.0, 0.0, 0.0, 1.0))
                val b = m.transform(Vec4(0.0, 0.0, -1.0, 1.0))
                val a2 = Vec3(a.x, a.y, a.z) * (1 / a.w)
                val b2 = Vec3(b.x, b.y, b.z) * (1 / b.w)

                return b2 - a2
            } else if (SimpleOculusTarget.o2 != null) {
                var m = Mat4(SimpleOculusTarget.o2!!.cameraInterface().view(-1f))
                m.transpose()
                var m2 = Mat4(__camera.view().get())
                m2 = m2.transpose()

                m = Mat4.mul(m, m2, Mat4())
                m.invert()

                val a = m.transform(Vec4(0.0, 0.0, 0.0, 1.0))
                val b = m.transform(Vec4(0.0, 0.0, -1.0, 1.0))
                val a2 = Vec3(a.x, a.y, a.z) * (1 / a.w)
                var b2 = Vec3(b.x, b.y, b.z) * (1 / b.w)

                return b2 - a2
            }

            return Vec3()
        }

        fun vrViewerUp(): Vec3 {
            if (SimpleOculusTarget.o != null) {

                var m = Mat4(SimpleOculusTarget.o!!.rightView().get())
                var m2 = Mat4(__camera.view().get())
                m2 = m2.transpose()

                m = Mat4.mul(m, m2, Mat4())
                m.invert()

                val a = m.transform(Vec4(0.0, 0.0, 0.0, 1.0))
                val b = m.transform(Vec4(0.0, 1.0, 0.0, 1.0))
                val a2 = Vec3(a.x, a.y, a.z) * (1 / a.w)
                val b2 = Vec3(b.x, b.y, b.z) * (1 / b.w)

                return b2 - a2
            } else if (SimpleOculusTarget.o2 != null) {
                var m = Mat4(SimpleOculusTarget.o2!!.cameraInterface().view(-1f))
                m.transpose()
                var m2 = Mat4(__camera.view().get())
                m2 = m2.transpose()

                m = Mat4.mul(m, m2, Mat4())
                m.invert()

                val a = m.transform(Vec4(0.0, 0.0, 0.0, 1.0))
                val b = m.transform(Vec4(0.0, 1.0, 0.0, 1.0))
                val a2 = Vec3(a.x, a.y, a.z) * (1 / a.w)
                var b2 = Vec3(b.x, b.y, b.z) * (1 / b.w)

                return b2 - a2
            }

            return Vec3()
        }

        fun vrViewerPosition(): Vec3 {

            if (SimpleOculusTarget.o != null) {
                var m = Mat4(SimpleOculusTarget.o!!.rightView().get())
                var m2 = Mat4(__camera.view().get())
                m2 = m2.transpose()

                m = Mat4.mul(m, m2, Mat4())
                m.invert()

                val a = m.transform(Vec4(0.0, 0.0, 0.0, 1.0))
                val b = m.transform(Vec4(0.0, 0.0, -1.0, 1.0))
                val a2 = Vec3(a.x, a.y, a.z) * (1 / a.w)
                var b2 = Vec3(b.x, b.y, b.z) * (1 / b.w)

                return a2
            } else if (SimpleOculusTarget.o2 != null) {
                var m = Mat4(SimpleOculusTarget.o2!!.cameraInterface().view(-1f))
                m.transpose()
                var m2 = Mat4(__camera.view().get())
                m2 = m2.transpose()

                m = Mat4.mul(m, m2, Mat4())
                m.invert()

                val a = m.transform(Vec4(0.0, 0.0, 0.0, 1.0))
                val b = m.transform(Vec4(0.0, 0.0, -1.0, 1.0))
                val a2 = Vec3(a.x, a.y, a.z) * (1 / a.w)
                var b2 = Vec3(b.x, b.y, b.z) * (1 / b.w)

                return a2
            }
            return Vec3()
        }


        fun vrLeftHandPosition(): Vec3 {

            if (SimpleOculusTarget.o != null) {
                val at = SimpleOculusTarget.o!!.leftPosition + Vec3(0.0, 1.65, 0.0)
                var m2 = Mat4(__camera.view().get())
                m2 = m2.transpose()
                m2 = m2.invert()
                val al = m2.transform(Vec4(at, 1.0))

                println(al)
                return Vec3(al.x, al.y, al.z) * (1 / al.w)
            } else if (SimpleOculusTarget.o2 != null) {
//                println("hand : ${SimpleOculusTarget.o2!!.hand1}")

                val at = Vec3(
                    SimpleOculusTarget.o2!!.hand1.m03,
                    SimpleOculusTarget.o2!!.hand1.m13,
                    SimpleOculusTarget.o2!!.hand1.m23
                );// + Vec3(0.0, 1.65, 0.0)
                var m2 = Mat4(__camera.view().get())
                m2 = m2.transpose()
                m2 = m2.invert()
                val al = m2.transform(Vec4(at, 1.0))

                println(al)
                return Vec3(al.x, al.y, al.z) * (1 / al.w)
            }
            return Vec3()
        }

        fun vrLeftHandDirection(): Vec3 {
            if (SimpleOculusTarget.o != null) {

                val m = Mat4()
                m.identity()
                val o = SimpleOculusTarget.o!!.leftOrientation

                m.rotate(o)

                val base = m.transform(Vec4(0, 0, 0, 1))
                val end = m.transform(Vec4(0, 0, -1, 1))

                val dir = Vec3(end.x, end.y, end.z) * (1 / end.w) - Vec3(base.x, base.y, base.z) * (1 / base.w)


                var m2 = Mat4(__camera.view().get())
                m2 = m2.transpose()
                m2 = m2.invert()
                val al = m2.transform(Vec4(dir, 0.0))

                return Vec3(al.x, al.y, al.z)
            } else if (SimpleOculusTarget.o2 != null) {


                val m = Mat4(SimpleOculusTarget.o2!!.hand1).transpose()

                val base = m.transform(Vec4(0, 0, 0, 1))
                val end = m.transform(Vec4(0, 0, -1, 1))

                val dir = Vec3(end.x, end.y, end.z) * (1 / end.w) - Vec3(base.x, base.y, base.z) * (1 / base.w)


                var m2 = Mat4(__camera.view().get())
                m2 = m2.transpose()
                m2 = m2.invert()
                val al = m2.transform(Vec4(dir, 0.0))

                return Vec3(al.x, al.y, al.z)

            }
            return Vec3()
        }

        fun vrRightHandDirection(): Vec3 {
            if (SimpleOculusTarget.o != null) {

                val m = Mat4()
                m.identity()
                val o = SimpleOculusTarget.o!!.rightOrientation

                m.rotate(o)

                val base = m.transform(Vec4(0, 0, 0, 1))
                val end = m.transform(Vec4(0, 0, -1, 1))

                val dir = Vec3(end.x, end.y, end.z) * (1 / end.w) - Vec3(base.x, base.y, base.z) * (1 / base.w)


                var m2 = Mat4(__camera.view().get())
                m2 = m2.transpose()
                m2 = m2.invert()
                val al = m2.transform(Vec4(dir, 0.0))

                return Vec3(al.x, al.y, al.z)
            } else if (SimpleOculusTarget.o2 != null) {

                val m = Mat4(SimpleOculusTarget.o2!!.hand2).transpose()

                val base = m.transform(Vec4(0, 0, 0, 1))
                val end = m.transform(Vec4(0, 0, -1, 1))

                val dir = Vec3(end.x, end.y, end.z) * (1 / end.w) - Vec3(base.x, base.y, base.z) * (1 / base.w)


                var m2 = Mat4(__camera.view().get())
                m2 = m2.transpose()
                m2 = m2.invert()
                val al = m2.transform(Vec4(dir, 0.0))

                return Vec3(al.x, al.y, al.z)

            }
            return Vec3()
        }

        fun vrRightHandButtons(): Map<String, Number> {
            if (SimpleOculusTarget.o != null) {
                val v = SimpleOculusTarget.o!!.rightInputState.Buttons()
                var x = 1
                var n = 1
                var r = mutableMapOf<String, Number>()
                while (x < 256) {
                    if ((v and x) != 0) {
                        r.put("button_$n", 1.0)
                    }
                    x = x * 2
                }
                return r
            } else if (SimpleOculusTarget.o2 != null) {

                return SimpleOculusTarget.o2!!.buttons.axesMap

            }
            return emptyMap()
        }


        fun vrLeftHandButtons(): Map<String, Number> {
            if (SimpleOculusTarget.o != null) {
                val v = SimpleOculusTarget.o!!.rightInputState.Buttons()
                var x = 1
                var n = 1
                var r = mutableMapOf<String, Number>()
                while (x < 256) {
                    if ((v and x) != 0) {
                        r.put("button_$n", 1.0)
                    }
                    x = x * 2
                }
                return r
            } else if (SimpleOculusTarget.o2 != null) {

                return SimpleOculusTarget.o2!!.buttons.axesMap

            }
            return emptyMap()
        }


        fun vrRightHandPosition(): Vec3 {

            if (SimpleOculusTarget.o != null) {
                val at = SimpleOculusTarget.o!!.rightPosition + Vec3(0.0, 1.65, 0.0)
                var m2 = Mat4(__camera.view().get())
                m2 = m2.transpose()
                m2 = m2.invert()
                val al = m2.transform(Vec4(at, 1.0))

                println(al)
                return Vec3(al.x, al.y, al.z) * (1 / al.w)
            } else if (SimpleOculusTarget.o2 != null) {
//                println("hand : ${SimpleOculusTarget.o2!!.hand1}")

                val at = Vec3(
                    SimpleOculusTarget.o2!!.hand2.m03,
                    SimpleOculusTarget.o2!!.hand2.m13,
                    SimpleOculusTarget.o2!!.hand2.m23
                );// + Vec3(0.0, 1.65, 0.0)
                var m2 = Mat4(__camera.view().get())
                m2 = m2.transpose()
                m2 = m2.invert()
                val al = m2.transform(Vec4(at, 1.0))

                println(al)
                return Vec3(al.x, al.y, al.z) * (1 / al.w)
            }
            return Vec3()


        }

        var layerIsDone = false

        fun finishLayer() {
            layerIsDone = true
        }


        fun setShader(shader: Triple<Shader?, Shader?, Shader?>): ShaderGroup {

            this.shader = shader

            if (shader.first != null) shader.first!!.asMap_set("_planes_", planes)
            if (shader.second != null) shader.second!!.asMap_set("_line_", line)
            if (shader.third != null) shader.third!!.asMap_set("_point_", points)

            listOf(shader.first, shader.second, shader.third).filter { it != null }.forEachIndexed { index, it ->

                it!!.asMap_set("translation", Supplier { translation })
                it.asMap_set("bounds", Supplier { bounds })
                it.asMap_set("reallyVR", Supplier<Float> {
                    if (SimpleOculusTarget.isVR()) {
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
                        if (SimpleOculusTarget.isVR()) {
                            SimpleOculusTarget.leftProjectionMatrix()

                        } else __camera.projectionMatrix(-1f)
                    }
                })
                it.asMap_set("Vl", Supplier<Mat4> {
                    if (!is3D) {
                        Mat4().identity()
                    } else {
                        if (SimpleOculusTarget.isVR()) {
                            SimpleOculusTarget.leftViewMatrix()

                        } else __camera.view(-1f)
                    }
                })
                it.asMap_set("Pr", Supplier<Mat4> {
                    if (!is3D) {
                        Mat4().identity()
                    } else {
                        if (SimpleOculusTarget.isVR()) {
                            SimpleOculusTarget.rightProjectionMatrix()
                        } else __camera.projectionMatrix(1f)
                    }
                })
                it.asMap_set("Vr", Supplier<Mat4> {
                    if (!is3D) {
                        Mat4().identity()
                    } else {
                        if (SimpleOculusTarget.isVR()) {
                            SimpleOculusTarget.rightViewMatrix()
                        } else __camera.view(1f)
                    }
                })
                it.asMap_set("scale", Supplier { scale })
                it.asMap_set("sides", Supplier { sides })
                it.asMap_set("opacity", Supplier { opacity.toFloat() })
                it.asMap_set(
                    "rotator",
                    Supplier { Vec2(Math.cos(Math.PI * rotation / 180), Math.sin(Math.PI * rotation / 180)) })

                it.asMap_set("isVR", Supplier {
                    if (SimpleOculusTarget.isVR() || STEREO) 1f else -1f
                })

                it.asMap_set("leftOffset", Supplier<Vec3> { leftOffset })

                fbo.scene.attach(name + "_$index", it)
            }


            fbo.scene.attach(-100, "__doGeometry__" + name) {

                if (!layerIsDone) {

                    camera.update()

                    lineBuilder.open()
                    planeBuilder.open()
                    pointBuilder.open()
                    try {
                        __lines.tailUpdate().values.forEach {
                            if (doTexture) it.addAuxProperties(4, StandardFLineDrawing.texCoord.name)
                            //it.addAuxProperties(3, FLineTo)
                            StandardFLineDrawing.dispatchLine(
                                it,
                                planeBuilder,
                                lineBuilder,
                                pointBuilder,
                                Optional.empty(),
                                ""
                            )
                        }
                    } finally {
                        pointBuilder.close()
                        planeBuilder.close()
                        lineBuilder.close()
                    }
                    remoteHelper?.update(this)
                }
                post.values.forEach { it.run() }
            }
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

        fun addTexture(unit: Int, name: String, layer: ShaderGroup) {
            val texture = layer.shader!!.second!!.lookup("source") as Texture

            val texture2 = texture.viewWithDifferentUnit(unit)

            shader?.first?.asMap_set(name, texture2)
            shader?.second?.asMap_set(name, texture2)
            shader?.third?.asMap_set(name, texture2)
        }

        //        var webcamDriver: WebcamDriver? = null
        var fileMap: ImageCache.FileMap? = null
        lateinit var head: SimpleHead.Frame

        var texture: Texture? = null



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

        return Triple(s1, s2, s3)
    }

    fun bindShaderToBox(name: String, box: Box, kind: Int) {
        if (groups[name] != null) {
            box.first(GraphicsSupport.bindShader, box.upwards()).ifPresent {
                if (kind == 0) it.apply(box, groups[name]!!.shader!!.first)
                else if (kind == 1) it.apply(box, groups[name]!!.shader!!.second)
                else if (kind == 2) it.apply(box, groups[name]!!.shader!!.third)
            }
        } else throw IllegalArgumentException(" can't find a ShaderGroup called '$name'")
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

    @Documentation("Make a layer with a given texture map. Filename should be a jpeg image.")
    fun withTexture(filename: String): ShaderGroup {
        val n = groups.get(filename)
        if (n != null) return n

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

        val tt = Texture(Texture.TextureSpecification.fromJpeg(3, filename, true))
        val tt2 = Texture(Texture.TextureSpecification.fromJpeg(4, filename, true))

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
            s1.sources.get(Shader.Type.vertex)!!.setOnError(errorHandler(box, "color_remap"))
            s1.sources.get(Shader.Type.fragment)!!.setOnError(errorHandler(box, "color_remap"))
            s1.sources.get(Shader.Type.geometry)!!.setOnError(errorHandler(box, "color_remap"))
            s1.setOnError(errorHandler(box, "color_remap"))
            s2.sources.get(Shader.Type.vertex)!!.setOnError(errorHandler(box, "color_remap"))
            s2.sources.get(Shader.Type.fragment)!!.setOnError(errorHandler(box, "color_remap"))
            s2.sources.get(Shader.Type.geometry)!!.setOnError(errorHandler(box, "color_remap"))
            s2.setOnError(errorHandler(box, "color_remap"))
            s3.sources.get(Shader.Type.vertex)!!.setOnError(errorHandler(box, "color_remap"))
            s3.sources.get(Shader.Type.fragment)!!.setOnError(errorHandler(box, "color_remap"))
            s3.sources.get(Shader.Type.geometry)!!.setOnError(errorHandler(box, "color_remap"))
            s3.setOnError(errorHandler(box, "color_remap"))
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        sg.imageBytes = { tt.specification.pixels };
        sg.imageDimensions = tt.specification.width to tt.specification.height
        sg.imageBytesPerPixel = 3

        sg.setShader(Triple(s1, s2, s3))
        sg.doTexture = true
        sg.textureFilename = filename
        sg.textureDimensions = Vec2(tt.specification.width, tt.specification.height)
        return sg
    }

//    fun withKinectDepth(): ShaderGroup {
//        return withTexture(KinectDriver.texture!!, "kinect")
//    }

    @JvmOverloads
    fun withTexture(t: TextureFromFloatBuffer, name: String? = null): ShaderGroup {
        return withTexture(t.tex, name)
    }

    @JvmOverloads
    fun withTexture(t: Texture, name: String? = null): ShaderGroup {

        val filename = name ?: "" + System.identityHashCode(t)

        val n = groups.get(filename)
        if (n != null) return n

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

        val tt = t
        val tt2 = t

        s1.asMap_set("source", tt)
        s1.asMap_set("source2", OffersUniform { tt2.specification.unit })

        val s2 = Shader()
        s2.addSource(Shader.Type.vertex, texture_vertex)
        s2.addSource(Shader.Type.geometry, texture_geometry_lines)
        s2.addSource(Shader.Type.fragment, ShaderPreprocessor().Preprocess(null, texture_fragment, Supplier {
            mutableMapOf("COLOR_REMAP" to sg.colorRemap, "SPACE_REMAP" to sg.spaceRemap)
        }))

        s2.asMap_set("source", tt)
        s2.asMap_set("source2", OffersUniform { tt2.specification.unit })

        val s3 = Shader()
        s3.addSource(Shader.Type.vertex, texture_vertex)
        s3.addSource(Shader.Type.geometry, texture_geometry_points)
        s3.addSource(Shader.Type.fragment, ShaderPreprocessor().Preprocess(null, texture_fragment_points, Supplier {
            mutableMapOf("COLOR_REMAP" to sg.colorRemap, "SPACE_REMAP" to sg.spaceRemap)
        }))

        s3.asMap_set("source", tt)
        s3.asMap_set("source2", OffersUniform { tt2.specification.unit })

        try {
            val box = Execution.context.get().peek()
            s1.sources.get(Shader.Type.vertex)!!.setOnError(errorHandler(box, "color_remap"))
            s1.sources.get(Shader.Type.fragment)!!.setOnError(errorHandler(box, "color_remap"))
            s1.sources.get(Shader.Type.geometry)!!.setOnError(errorHandler(box, "color_remap"))
            s1.setOnError(errorHandler(box, "color_remap"))
            s2.sources.get(Shader.Type.vertex)!!.setOnError(errorHandler(box, "color_remap"))
            s2.sources.get(Shader.Type.fragment)!!.setOnError(errorHandler(box, "color_remap"))
            s2.sources.get(Shader.Type.geometry)!!.setOnError(errorHandler(box, "color_remap"))
            s2.setOnError(errorHandler(box, "color_remap"))
            s3.sources.get(Shader.Type.vertex)!!.setOnError(errorHandler(box, "color_remap"))
            s3.sources.get(Shader.Type.fragment)!!.setOnError(errorHandler(box, "color_remap"))
            s3.sources.get(Shader.Type.geometry)!!.setOnError(errorHandler(box, "color_remap"))
            s3.setOnError(errorHandler(box, "color_remap"))
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        sg.setShader(Triple(s1, s2, s3))
        sg.doTexture = true
        sg.texture = t
        sg.textureFilename = filename
        sg.textureDimensions = Vec2(tt.specification.width, tt.specification.height)
        return sg
    }

    @Documentation("Make a layer with a given left-right stereo texture pair. Filenames should be a jpeg images.")
    fun withStereoTexture(filename: String, filename2: String): ShaderGroup {
        val n = groups.get(filename + "|" + filename2)
        if (n != null) return n

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
            s1.sources.get(Shader.Type.vertex)!!.setOnError(errorHandler(box, "color_remap"))
            s1.sources.get(Shader.Type.fragment)!!.setOnError(errorHandler(box, "color_remap"))
            s1.sources.get(Shader.Type.geometry)!!.setOnError(errorHandler(box, "color_remap"))
            s1.setOnError(errorHandler(box, "color_remap"))
            s2.sources.get(Shader.Type.vertex)!!.setOnError(errorHandler(box, "color_remap"))
            s2.sources.get(Shader.Type.fragment)!!.setOnError(errorHandler(box, "color_remap"))
            s2.sources.get(Shader.Type.geometry)!!.setOnError(errorHandler(box, "color_remap"))
            s2.setOnError(errorHandler(box, "color_remap"))
            s3.sources.get(Shader.Type.vertex)!!.setOnError(errorHandler(box, "color_remap"))
            s3.sources.get(Shader.Type.fragment)!!.setOnError(errorHandler(box, "color_remap"))
            s3.sources.get(Shader.Type.geometry)!!.setOnError(errorHandler(box, "color_remap"))
            s3.setOnError(errorHandler(box, "color_remap"))
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        sg.setShader(Triple(s1, s2, s3))
        sg.doTexture = true
        sg.textureFilename = filename
        sg.textureDimensions = Vec2(tt.specification.width, tt.specification.height)
        return sg
    }

    @Documentation("Make a layer with a given texture map. Filename should be a jpeg image.")
    fun withWebcam(): ShaderGroup {
        val filename = "__webcam__"
        val n = groups.get(filename)
        if (n != null) return n

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

        val wd = WebcamDriver3(0)
        val ts = WebcamDriver3.texture
        s1.asMap_set("source", ts)
        s2.asMap_set("source", ts)
        s3.asMap_set("source", ts)


        try {
            val box = Execution.context.get().peek()
            s1.sources.get(Shader.Type.vertex)!!.setOnError(errorHandler(box, "color_remap"))
            s1.sources.get(Shader.Type.fragment)!!.setOnError(errorHandler(box, "color_remap"))
            s1.sources.get(Shader.Type.geometry)!!.setOnError(errorHandler(box, "color_remap"))
            s1.setOnError(errorHandler(box, "color_remap"))
            s2.sources.get(Shader.Type.vertex)!!.setOnError(errorHandler(box, "color_remap"))
            s2.sources.get(Shader.Type.fragment)!!.setOnError(errorHandler(box, "color_remap"))
            s2.sources.get(Shader.Type.geometry)!!.setOnError(errorHandler(box, "color_remap"))
            s2.setOnError(errorHandler(box, "color_remap"))
            s3.sources.get(Shader.Type.vertex)!!.setOnError(errorHandler(box, "color_remap"))
            s3.sources.get(Shader.Type.fragment)!!.setOnError(errorHandler(box, "color_remap"))
            s3.sources.get(Shader.Type.geometry)!!.setOnError(errorHandler(box, "color_remap"))
            s3.setOnError(errorHandler(box, "color_remap"))
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        sg.imageBytes = { WebcamDriver3.storage };
        sg.imageDimensions = WebcamDriver3.w to WebcamDriver3.h
        sg.imageBytesPerPixel = 3

        sg.setShader(Triple(s1, s2, s3))
        sg.doTexture = true
//        sg.webcamDriver = wd
        sg.post.put("texupdate", Runnable {
            wd.update()
        })
        return sg
    }

    @Documentation(
        "Make a layer that loads 'video' material from a stream of jpeg images in the given directory. These layers have an additional 'time' property that goes from 0 to 1 (from the start of the image sequence to the end)."
    )
    fun withImageSequence(filename: String): ShaderGroup {
        val n = groups.get(filename)
        if (n != null) return n

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

        val map = try {
            ImageCache.mapFromDirectory(filename, ".*.jpg")

        } catch (n: NullPointerException) {
            if (!File(filename).exists()) throw java.lang.IllegalArgumentException(" problem loading jpgs from directory `$filename`, that directory doesn't exist")
            if (!File(filename).isDirectory) throw java.lang.IllegalArgumentException(" problem loading jpgs from directory `$filename`, that isn't a directory")

            throw java.lang.IllegalArgumentException(" problem loading jpgs from directory `$filename`")
        }
        // todo image size
        if (map.length() == 0) throw IllegalArgumentException(" doesn't seem to be any .jpg files in directory $filename ?")

        val fn = map.apply(0)
        val dim = FastJPEG.j.dimensions(fn)

        val ic = ImageCache(dim[0], dim[1], 300, 40, map)

        val cache = TwinTextureCache(0, ic)
        cache.setPlaying(true)

        if (!SimpleOculusTarget.isVR())
            ImageCache.synchronous = true

        cache.setTime(1.0)
//        cache.update()

        s1.asMap_set("T0", cache.textureA)
        s1.asMap_set("T1", cache.textureB)
        s1.asMap_set("alpha", Supplier { cache.getAlpha() })
        s2.asMap_set("T0", cache.textureA)
        s2.asMap_set("T1", cache.textureB)
        s2.asMap_set("alpha", Supplier { cache.getAlpha() })
        s3.asMap_set("T0", cache.textureA)
        s3.asMap_set("T1", cache.textureB)
        s3.asMap_set("alpha", Supplier { cache.getAlpha() })

        s1.asMap_set("blur", Supplier { sg.blur })
        s2.asMap_set("blur", Supplier { sg.blur })
        s3.asMap_set("blur", Supplier { sg.blur })

        s1.asMap_set("colorAdd", Supplier { sg.colorAdd })
        s2.asMap_set("colorAdd", Supplier { sg.colorAdd })
        s3.asMap_set("colorAdd", Supplier { sg.colorAdd })
        s1.asMap_set("colorMul", Supplier { sg.colorMul })
        s2.asMap_set("colorMul", Supplier { sg.colorMul })
        s3.asMap_set("colorMul", Supplier { sg.colorMul })

        sg.setShader(Triple(s1, s2, s3))

        sg.fileMap = map

        sg.doTexture = true
        sg.post.put("__dovideo__", Runnable {

            if (sg.frameSet) {
                sg.frameSet = false
                sg.time = sg._frame / map.length().toDouble()
                cache.setTime(sg._frame.toDouble())
            } else cache.setTime(map.length() * sg.time)

//            println(" cache update at time ${cache.time}")

            cache.update()
        })

        sg.imageBytes = { cache.lastBytes }
        sg.imageDimensions = cache.textureA.specification.width to cache.textureA.specification.height
        sg.imageBytesPerPixel = 3


        try {
            val box = Execution.context.get().peek()
            s1.sources.get(Shader.Type.vertex)!!.setOnError(errorHandler(box, "color_remap"))
            s1.sources.get(Shader.Type.geometry)!!.setOnError(errorHandler(box, "color_remap"))
            s1.sources.get(Shader.Type.fragment)!!.setOnError(errorHandler(box, "color_remap"))
            s1.setOnError(errorHandler(box, "color_remap"))
            s2.sources.get(Shader.Type.vertex)!!.setOnError(errorHandler(box, "color_remap"))
            s2.sources.get(Shader.Type.geometry)!!.setOnError(errorHandler(box, "color_remap"))
            s2.sources.get(Shader.Type.fragment)!!.setOnError(errorHandler(box, "color_remap"))
            s2.setOnError(errorHandler(box, "color_remap"))
            s3.sources.get(Shader.Type.vertex)!!.setOnError(errorHandler(box, "color_remap"))
            s3.sources.get(Shader.Type.geometry)!!.setOnError(errorHandler(box, "color_remap"))
            s3.sources.get(Shader.Type.fragment)!!.setOnError(errorHandler(box, "color_remap"))
            s3.setOnError(errorHandler(box, "color_remap"))
        } catch (e: IndexOutOfBoundsException) {
            e.printStackTrace()
        }

//        if (File(filename.replace(".dir", ".simpleHead")).exists()) {
//            val reader = SimpleHead.reader(filename.replace(".dir", ".simpleHead"))
//            sg.head = reader(0.0)
//
//            sg.post.put("__dovideo2__", Runnable {
//                println(" -- reader update at time ${cache.time / map.length()}")
//                sg.head = reader((cache.time / map.length()) % 1)
//            })
//
//        }

        return sg
    }


    @Documentation("Make a layer with a particular name. If a layer already exists with this name, just look it up. Layers have cameras, opacities and color / space remappers.")
    fun withName(name: String): ShaderGroup {
        val n = groups.get(name)
        if (n != null) return n

        val g = ShaderGroup(name)
        g.setShader(defaultShader(g))
        return g
    }

    // should texturing somehow be default?

    fun withStage(stage: Stage): ShaderGroup {
        val name = "__stage__" + System.identityHashCode(stage)
        val n = groups.get(name)
        if (n != null) return n

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
            s1.sources.get(Shader.Type.vertex)!!.setOnError(errorHandler(box, "color_remap"))
            s1.sources.get(Shader.Type.fragment)!!.setOnError(errorHandler(box, "color_remap"))
            s1.setOnError(errorHandler(box, "color_remap"))
            s2.sources.get(Shader.Type.vertex)!!.setOnError(errorHandler(box, "color_remap"))
            s2.sources.get(Shader.Type.fragment)!!.setOnError(errorHandler(box, "color_remap"))
            s2.setOnError(errorHandler(box, "color_remap"))
        } catch (e: IndexOutOfBoundsException) {
            e.printStackTrace()
        }

        return sg
    }


    private var insideViewport: Viewport? = null

    fun show(name: String, v: Viewport): Shader {
        insideViewport = v
        return showScene(
            name,
            (v get Viewport.scene)!!,
            { isOut },
            { v.properties.get(Box.frame).w / v.properties.get(Box.frame).h.toDouble() },
            false
        )
    }


    private var masterShader: Shader? = null

    fun bindShowShaderToBox(box: Box): Shader {
        if (masterShader == null) throw IllegalArgumentException(" can't bind a show shader if the box isn't being shown ")
        box.first(GraphicsSupport.bindShader, box.upwards()).ifPresent {
            it.apply(box, masterShader!!)
        }

        return masterShader!!
    }

    fun showScene(
        name: String,
        scene: Scene,
        disabled: () -> Boolean,
        asp: () -> Double,
        isFullscreen: Boolean
    ): Shader {
        val s = Shader()

        if (isFullscreen && VR) {
            s.addSource(Shader.Type.vertex, ShaderPreprocessor().Preprocess(null, show_vertex_vr, Supplier {
                mutableMapOf("REMAP" to remap)
            }))
            s.addSource(Shader.Type.fragment, ShaderPreprocessor().Preprocess(null, show_fragment_vr, Supplier {
                mutableMapOf("REMAP" to remap)
            }))

        } else if (isFullscreen && STEREO) {
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
        s.asMap_set("tex", fbo)
        s.asMap_set("geom", planes)
        s.asMap_set("aspect", Supplier { ((h) / w.toFloat()) * asp() })

        scene.asMap_set(name, s)
        scene.attach(-100, "__draw__" + name, Consumer<Int> {
            fbo.draw()
        })

        masterShader = s

        return s
    }

    @JvmField
    var keyboard: Buttons = Buttons()

    init {
        default_group.post.put("__keyboard__", keyboard)
    }

    @JvmField
    val mouse = SimpleMouse()

    @JvmOverloads
    fun popOut(useFullScreenWindow: Boolean = false): Shader? {
        if (isOut) {
            // toFront
            return null
        }
        isOut = true

        return ThreadSync2.callInMainThreadAndWait(Callable {
            if (window == null) {
                window = object : Window(
                    0,
                    0,
                    w / 2 - 1,
                    h / 2 - 1,
                    if (useFullScreenWindow) null else "Field / Stage $thisStageNum"
                ) {
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

                window!!.addMouseHandler(mouse)

                window!!.setBounds(0, 0, w, h)

                keyboard.attachToWindow(window!!)

            }

            showScene("default", window!!.scene, { false }, { window!!.width.toDouble() / window!!.height }, true)
        })
    }

    // can only be called by closing window
    private fun popIn() {
        window = null
        isOut = false
    }


    /**
     * reminds the window or viewport that it needs to update it's appearance
     */
    fun redraw() {
        if (isOut) {
            // assume continuous loop
        } else {
            if (insideViewport != null) {
                Drawing.dirty(insideViewport!!, LATENCY)
            }
        }
    }

    var previousFrameAt = 0L

    /**
     * marks the spot in the code where we wait for anything we've drawn to appear on the screen. Effectively the same as `_redraw(); _.wait()`
     */
    fun frame(): Boolean {

        redraw()
        val r = ThreadSync2Feedback.maybeYield()
        previousFrameAt = System.currentTimeMillis()
        return r
    }


    fun frame(t: Double): Boolean {
        val now = previousFrameAt
        redraw()
        Threading.waitSafely()
        while ((System.currentTimeMillis() - now) / 1000.0 < t) {
            Threading.waitSafely()
        }
        previousFrameAt = System.currentTimeMillis()
        return t > 0
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
                    .apply(b).accept(shader + " reloaded correctly")
            }
        }
    }

    fun startSaving(): String {
        return saver.start()
    }


    fun startSaving(path: String): String {
        saver.setPrefix(path)
        saver.frameNumber = 0

        return saver.start()
    }

    fun stopSaving() {
        saver.stop()
    }


}

