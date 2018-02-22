package trace.graphics

import field.app.RunLoop
import field.app.ThreadSync2
import field.graphics.*
import field.graphics.util.Saver
import field.graphics.util.SaverFBO
import field.graphics.util.onsheetui.get
import field.linalg.Mat4
import field.linalg.Vec2
import field.linalg.Vec4
import field.utility.Documentation
import field.utility.IdempotencyMap
import field.utility.Pair
import field.utility.ShaderPreprocessor
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
        fbo = FBO(FBO.FBOSpecification.singleFloat16(thisStageNum, w, h))

        val base = System.getProperty("user.home") + File.separatorChar + "Desktop"+File.separatorChar+"field_stage_recordings" + File.separatorChar

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
        s.asMap_set("color", Supplier<Vec4> { background });
        fbo.scene.attach(-101, {
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            false
        })
        fbo.scene.attach("background_clear", s)
        fbo.scene.attach(-101, Scene.Perform {
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
            false
        })

        fbo.scene.attach(101, Scene.Perform {
            saver.update()
            true
        })

    }


    @JvmField
    var remap = ""

    @JvmField
    val groups = mutableMapOf<String, ShaderGroup>()


    @HiddenInAutocomplete
    val default_vertex = BoxDefaultCode.findSource(this.javaClass, "default_vertex")
    @HiddenInAutocomplete
    val default_fragment = BoxDefaultCode.findSource(this.javaClass, "default_fragment")
    @HiddenInAutocomplete
    val default_group = {
        val s = ShaderGroup("__default__")
        s.setShader(defaultShader(s))
        s
    }()

    @HiddenInAutocomplete
    val texture_vertex = BoxDefaultCode.findSource(this.javaClass, "texture_vertex")
    @HiddenInAutocomplete
    val texture_fragment = BoxDefaultCode.findSource(this.javaClass, "texture_fragment")

    @HiddenInAutocomplete
    val syphon_vertex = BoxDefaultCode.findSource(this.javaClass, "syphon_vertex")
    @HiddenInAutocomplete
    val syphon_fragment = BoxDefaultCode.findSource(this.javaClass, "syphon_fragment")

    @HiddenInAutocomplete
    val video_vertex = BoxDefaultCode.findSource(this.javaClass, "video_vertex")
    @HiddenInAutocomplete
    val video_fragment = BoxDefaultCode.findSource(this.javaClass, "video_fragment")


    inner class ShaderGroup(val name: String) {
        val line = BaseMesh.lineList(0, 0)
        val lineBuilder = MeshBuilder(line)
        val planes = BaseMesh.triangleList(0, 0)
        val planeBuilder = MeshBuilder(planes)
        val post = IdempotencyMap<Runnable>(Runnable::class.java);

        @JvmField
        @Documentation("list of `FLine` to draw, just like `_.lines` but it will appear on this Stage")
        val lines = IdempotencyMap<FLine>(FLine::class.java).configureResourceLimits<IdempotencyMap<FLine>>(300, "too many lines on a stage layer")

        var doTexture = false

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

        var shader: Shader? = null;

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

        var P = Mat4().identity()
        var V = Mat4().identity()


        fun setShader(shader: Shader): ShaderGroup {
            shader.asMap_set("_line_", line)
            shader.asMap_set("_planes_", planes)

            shader.asMap_set("translation", Supplier<Vec2> { translation })
            shader.asMap_set("bounds", Supplier<Vec2> { bounds })
            shader.asMap_set("P", Supplier<Mat4> { P })
            shader.asMap_set("V", Supplier<Mat4> { V })
            shader.asMap_set("scale", Supplier<Vec2> { scale })
            shader.asMap_set("opacity", Supplier<Float> { opacity.toFloat() })
            shader.asMap_set("rotator", Supplier<Vec2> { Vec2(Math.cos(Math.PI * rotation / 180), Math.sin(Math.PI * rotation / 180)) })

            fbo.scene.attach(name, shader)
            fbo.scene.attach(-100, "__doGeometry__" + name, Consumer<Int> {

                // todo: mod caching?

                lineBuilder.open()
                planeBuilder.open()
                try {
                    lines.values.forEach {
                        if (doTexture)
                            it.addAuxProperties(2, StandardFLineDrawing.texCoord.name)
//                        System.out.println(">---")
                        StandardFLineDrawing.dispatchLine(it, planeBuilder, lineBuilder, null, Optional.empty(), "")
//                        System.out.println("--- > " + line)
                    }
                } finally {
                    planeBuilder.close()
                    lineBuilder.close()
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

    }


    @JvmField
    val lines = default_group.lines

    private fun defaultShader(inside: ShaderGroup): Shader {
        val s = Shader()
        s.addSource(Shader.Type.vertex, default_vertex)
        s.addSource(Shader.Type.fragment, default_fragment)
        return s;
    }

    fun bindShaderToBox(name: String, box: Box) {
        if (groups[name] != null) {
            box.first(GraphicsSupport.bindShader, box.upwards()).ifPresent {
                it.apply(box, groups[name]!!.shader)
            }
        } else
            throw IllegalArgumentException(" can't find a ShaderGroup called '$name'")
    }

    @HiddenInAutocomplete
    val show_vertex = BoxDefaultCode.findSource(this.javaClass, "show_vertex")
    @HiddenInAutocomplete
    val show_fragment = BoxDefaultCode.findSource(this.javaClass, "show_fragment")


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

        val s = Shader()
        s.addSource(Shader.Type.vertex, texture_vertex)
        s.addSource(Shader.Type.fragment, ShaderPreprocessor().Preprocess(null, texture_fragment, Supplier {
            mutableMapOf("COLOR_REMAP" to sg.colorRemap, "SPACE_REMAP" to sg.spaceRemap)
        }))

        s.asMap_set("source", Texture(Texture.TextureSpecification.fromJpeg(3, filename, true)))

        try {
            val box = Execution.context.get().peek()
            s.sources.get(Shader.Type.vertex)!!.setOnError(errorHandler(box, "color_remap"));
            s.sources.get(Shader.Type.fragment)!!.setOnError(errorHandler(box, "color_remap"));
            s.setOnError(errorHandler(box, "color_remap"));
        } catch (e: IndexOutOfBoundsException) {
            e.printStackTrace()
        }

        sg.setShader(s);
        sg.doTexture = true
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

        val s = Shader()
        val sg = ShaderGroup(filename)
        s.addSource(Shader.Type.vertex, video_vertex)
        s.addSource(Shader.Type.fragment, ShaderPreprocessor().Preprocess(null, video_fragment, Supplier {
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

        s.asMap_set("T0", cache.textureA)
        s.asMap_set("T1", cache.textureB)
        s.asMap_set("alpha", Supplier { cache.getAlpha() })

        sg.setShader(s)
        sg.doTexture = true
        sg.post.put("__dovideo__", Runnable {
            cache.setTime(map.length() * sg.time);
            cache.update()
        })

        try {
            val box = Execution.context.get().peek()
            s.sources.get(Shader.Type.vertex)!!.setOnError(errorHandler(box, "color_remap"));
            s.sources.get(Shader.Type.fragment)!!.setOnError(errorHandler(box, "color_remap"));
            s.setOnError(errorHandler(box, "color_remap"));
        } catch (e: IndexOutOfBoundsException) {
            e.printStackTrace()
        }

        return sg
    }

    @Documentation("Make a layer that streams realtime video material using the Syphon protocol")
    fun withVideo(): ShaderGroup {
        val filename = "__syphon__"
        val n = groups.get(filename)
        if (n != null)
            return n

        if (groups.containsKey(filename)) {
            groups.remove(filename)!!.detatch()
        }
        val sg = ShaderGroup(filename)

        val s = Shader()
        s.addSource(Shader.Type.vertex, syphon_vertex)
        s.addSource(Shader.Type.fragment, ShaderPreprocessor().Preprocess(null, syphon_fragment, Supplier {
            mutableMapOf("COLOR_REMAP" to sg.colorRemap, "SPACE_REMAP" to sg.spaceRemap)
        }))
        val syphon = Syphon(7)

        s.asMap_set("source", syphon)

        sg.setShader(s)
        sg.doTexture = true

        try {
            val box = Execution.context.get().peek()
            s.sources.get(Shader.Type.vertex)!!.setOnError(errorHandler(box, "color_remap"));
            s.sources.get(Shader.Type.fragment)!!.setOnError(errorHandler(box, "color_remap"));
            s.setOnError(errorHandler(box, "color_remap"));
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
        val s = Shader()
        s.addSource(Shader.Type.vertex, texture_vertex)
        s.addSource(Shader.Type.fragment, ShaderPreprocessor().Preprocess(null, texture_fragment, Supplier {
            mutableMapOf("COLOR_REMAP" to sg.colorRemap, "SPACE_REMAP" to sg.spaceRemap)
        }))
        s.asMap_set("source", stage.fbo)

        sg.setShader(s)
        sg.doTexture = true

        try {
            val box = Execution.context.get().peek()
            s.sources.get(Shader.Type.vertex)!!.setOnError(errorHandler(box, "color_remap"));
            s.sources.get(Shader.Type.fragment)!!.setOnError(errorHandler(box, "color_remap"));
            s.setOnError(errorHandler(box, "color_remap"));
        } catch (e: IndexOutOfBoundsException) {
            e.printStackTrace()
        }

        return sg
    }


    private var insideViewport: Viewport? = null

    fun show(name: String, v: Viewport): Shader {
        insideViewport = v;
        return showScene(name, (v get Viewport.scene)!!, { isOut }, { v.properties.get(Box.frame).w / v.properties.get(Box.frame).h.toDouble() })
    }


    fun showScene(name: String, scene: Scene, disabled: () -> Boolean, asp: () -> Double): Shader {
        val s = Shader()

        s.addSource(Shader.Type.vertex, ShaderPreprocessor().Preprocess(null, show_vertex, Supplier {
            mutableMapOf<String, String>("REMAP" to remap)
        }))
        s.addSource(Shader.Type.fragment, ShaderPreprocessor().Preprocess(null, show_fragment, Supplier {
            mutableMapOf<String, String>("REMAP" to remap)
        }))

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
        s.asMap_set("aspect", Supplier<Double> { asp() });

        scene.asMap_set(name, s);
        scene.attach(-100, "__draw__" + name, Consumer<Int> {
            fbo.draw()
        })

        return s;
    }

    fun popOut(): Shader? {
        if (isOut) {
            // toFront
            return null;
        }
        isOut = true;

        return ThreadSync2.callInMainThreadAndWait(Callable {
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

            showScene("default", window!!.scene, { false }, { window!!.width.toDouble() / window!!.height })
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
    fun frame() :Boolean {
        redraw()
        return ThreadSync2Feedback.maybeYield();
    }

    fun errorHandler(b: Box, shader: String): Shader.iErrorHandler {
        return object : Shader.iErrorHandler {
            override fun beginError() {

            }

            override fun errorOnLine(line: Int, error: String) {
                b.first<java.util.function.Function<Box, Consumer<Pair<Int?, String>>>>(RemoteEditor.outputErrorFactory)
                        .orElse(java.util.function.Function<Box, Consumer<Pair<Int?, String>>> { x -> Consumer<Pair<Int?, String>> { `is` -> System.err.println("error (without remote editor attached) :" + `is`) } })
                        .apply(b)
                        .accept(Pair(null, "Error on $shader reload: $error"))
            }

            override fun endError() {

            }

            override fun noError() {
                b.first<java.util.function.Function<Box, Consumer<String>>>(RemoteEditor.outputFactory)
                        .orElse(java.util.function.Function<Box, Consumer<String>> { x -> Consumer<String> { `is` -> System.err.println("message (without remote editor attached) :" + `is`) } })
                        .apply(b)
                        .accept(shader + " reloaded correctly")
            }
        }
    }

    fun startSaving(): String {
        return saver.start()
    }

    fun stopSaving() {
        saver.stop()
    }


}

