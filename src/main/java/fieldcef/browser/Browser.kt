package fieldcef.browser

import field.app.RunLoop
import field.graphics.*
import field.graphics.Window.MouseState
import field.graphics.util.KeyEventMapping
import field.linalg.Vec2
import field.utility.*
import fieldagent.Main
import fieldbox.boxes.*
import fieldbox.boxes.Box.FunctionOfBox
import fieldbox.boxes.Keyboard.*
import fieldbox.boxes.Mouse.*
import fieldbox.boxes.plugins.Chorder
import fieldbox.boxes.plugins.KeyboardFocus
import fieldbox.boxes.plugins.Planes
import fieldbox.boxes.plugins.RunCommand
import fieldbox.io.IO.Loaded
import fieldbox.ui.FieldBoxWindow
import fieldcef.browser.CefSystem.MessageCallback
import fieldcef.plugins.BrowserKeyboardHacks
import fielded.TextUtils
import fielded.boxbrowser.TransientCommands
import fielded.plugins.Out
import org.cef.browser.CefBrowser
import org.json.JSONObject
import org.json.JSONWriter
import java.awt.BasicStroke
import java.awt.Component
import java.awt.Point
import java.awt.Rectangle
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import java.io.StringWriter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.BiFunction
import java.util.function.Consumer
import java.util.function.Predicate
import java.util.function.Supplier

/**
 * The first look at embedded HTML. A Browser is a Field/Graphics texture map, a CEF browser of a fixed size, a quad for drawing it, a shader for drawing the quad, and some logic for generating and
 * transforming events. Chrome in a box.
 */
class Browser : Box(), Loaded {
    private val mapper = KeyEventMapping()

    @JvmField
    var browser: CefBrowser? = null
    protected var keyboardHacks: BrowserKeyboardHacks? = null
    protected var dirty = AtomicBoolean(false)
    protected var booted = true
    var dragOngoing = false
    var handlers: MutableList<Pair<Predicate<String>, Handler>> = ArrayList()
    var messages: Deque<Pair<String, Consumer<String?>>> = ArrayDeque()
    var bootQueue: MutableList<Runnable> = ArrayList()
    var navigation = Cached<Box, String, Void?>(
        { now: Box, nothing: Void? ->
            val u = now.properties.get(url)
            if (u != null) {
                Log.log("HTML") { "loading URL <$u>" }
//                println(" LOADING URL : $u")
                browser!!.loadURL(u)
            }
            null
        }) { box: Box -> box.properties.get(url) }
    var direct = Cached<Box, String, Void?>(
        { now: Box, nothing: Void? ->
            val u = now.properties.get(html)
            if (u != null) {
                Log.log("HTML") { "loading html" }
                throw IllegalArgumentException(" NOT IMPLEMENTED, loadString")
            }
            null
        }) { box: Box -> box.properties.get(html) }
    var again = 0
    var check = 10
    var hasRepainted = false
    var first = true
    var paused = false
    var ignore = false
    private var w = 0
    private var h = 0
    private var source: ByteBuffer? = null
    private var texture: Texture? = null
    private var q: BaseMesh? = null
    private var builder: MeshBuilder? = null
    private var drawing: Drawing? = null
    var geometry = Cached<Box, Any, Void?>(BiFunction { now: Box, nothing: Void? ->
        val r = now.properties.get(frame)
        var op = now.properties.getFloat(StandardFLineDrawing.opacity, 1f)
        op = Math.sqrt(op.toDouble()).toFloat()
        if (now.properties.isTrue(hidden, false)) {
            builder!!.open()
            builder!!.close()
            return@BiFunction null
        }
        builder!!.open()
        val ns = 30
        val Z = now.properties.getOr(depth) { 0f }.toFloat()

//        System.out.println(" frame for textedit geometry is "+r.x+" -> "+r.w);
        run {
            var x = 0
            while (x < ns) {
                var y = 0
                while (y < ns) {
                    val ax = (0f + x) / (ns - 1).toFloat()
                    val ay = (0f + y) / (ns - 1).toFloat()
                    builder!!.aux(1, ax * r.w / w, ay * (r.h + 0.5f) / h, op)
                    builder!!.v(r.x + ax * r.w, r.y + ay * r.h, Z)
                    y++
                }
                x++
            }
        }
        var x = 0
        while (x < ns - 1) {
            var y = 0
            while (y < ns - 1) {
                builder!!.e_quad((x + 1) * ns + y, x * ns + y, x * ns + y + 1, (x + 1) * ns + y + 1)
                y++
            }
            x++
        }
        builder!!.close()
        null
    }) { box: Box ->
        Quad(
            box.properties.getFloat(StandardFLineDrawing.opacity, 1f), drawing!!.scale, box.properties.get(
                frame
            ), box.properties.isTrue(hidden, false)
        )
    }
    private var shader: Shader? = null
    private var sourceView: ByteBuffer? = null
    private var window: FieldBoxWindow? = null
    private var root: Box? = null

    @Volatile
    private var damage: Rect? = Rect(0.0, 0.0, 0.0, 0.0)

    @JvmField
    var callbackOnNextReload: Runnable? = null
    val outCached: Any?
        get() {
            if (out_cached != null) return out_cached
            out_cached = find(Out.__out, both()).findAny()
                .orElseGet { null }
            return out_cached
        }

    fun attachToShader(s: Shader) {
//        s.attach(-2, "__rectupdate__", x -> {
//            Rect r = properties.get(Box.frame);
//            update(r.x, r.y, 1/*r.w/w*/);
//        });
        s.asMap_set("te", texture)
        s.attach(q)

//		this.properties.putToListMap(Callbacks.onDelete, (bx) -> {
//
//			if (bx != this) return null;
//
//			browser.close();
//			window.getCompositor()
//				.getLayer(properties.computeIfAbsent(FLineDrawing.layer, k -> "__main__"))
//				.getScene()
//				.detach(s);
//
//			return null;
//		});
    }

    var ezl = Options.dict().getFloat(Dict.Prop("extraZoomLevel"), 1f)
    override fun loaded() {

        println(" -- loaded -- happening now")

        Log.disable("cef.*")
        properties.computeIfAbsent(frame) { k: Dict.Prop<Rect?>? -> Rect(0.0, 0.0, 512.0, 512.0) }
        properties.put(name, "(browser)")
        properties.put(Planes.plane, "__always__")
        properties.put(Chorder.nox, true)
        shader = Shader()
        properties.putToListMap(Callbacks.onDelete, FunctionOfBox { bx: Box ->
            if (bx !== this) return@FunctionOfBox null
            browser!!.close(true)
            window!!.compositor
                .getLayer(properties.computeIfAbsent(FLineDrawing.layer) { k: Dict.Prop<String?>? -> "__main__" })
                .scene
                .detach(shader)
            null
        })
        properties.put(depth, -0.1f)
        root = this.find(Boxes.root, both())
            .findFirst()
            .orElseThrow { IllegalArgumentException(" did you call loaded without adding to the graph?") }
        window = root!!.first(Boxes.window)
            .orElseThrow { IllegalArgumentException(" can't draw a box hierarchy with no window to draw it in !") }
        drawing = root!!.first(Drawing.drawing)
            .orElseThrow { IllegalArgumentException(" can't install text-drawing into something without drawing support") }
        val rsf = window!!.getRetinaScaleFactor();
        w = properties.get(frame).w.toInt()
        h = properties.get(frame).h.toInt()
        println("CefSystem is making a browser: " + w + "x" + h + "x" + rsf + " -----------------------------")
        browser = CefSystem.cefSystem.makeBrowser(
            (w * rsf).toInt(),
            (h * rsf).toInt(),
            CefSystem.PaintCallback { popup: Boolean, dirty: Array<Rectangle>?, buffer: ByteBuffer?, w: Int, h: Int ->
                paint(
                    popup,
                    dirty!!,
                    buffer!!,
                    w,
                    h
                )
            },
            MessageCallback { id: Long, message: String?, reply: Consumer<String?>? -> message(id, message, reply) }) {
            try {
                if (callbackOnNextReload != null) {
                    callbackOnNextReload!!.run()
                }
            } catch (t: Throwable) {
                t.printStackTrace()
            } finally {
                callbackOnNextReload = null
            }
        }
        browser!!.setZoomLevel(ezl.toDouble())
        keyboardHacks = BrowserKeyboardHacks(browser!!)
        source = ByteBuffer.allocateDirect((w * rsf).toInt() * (h * rsf).toInt() * 4).order(ByteOrder.nativeOrder())
        source!!.position(0)
            .limit(source!!.capacity())
        sourceView = source!!.slice()
        texture = Texture(
            Texture.TextureSpecification.byte4(
                13,
                (w * rsf).toInt(),
                (h * rsf).toInt(),
                source,
                true
            )
        ).setIsDoubleBuffered(false)
        q = BaseMesh.triangleList(0, 0)
        builder = MeshBuilder(q)
        shader!!.addSource(
            Shader.Type.vertex, """#version 410
layout(location=0) in vec3 position;
layout(location=1) in vec3 tc;
out vec4 vtc;
//out float op;
uniform vec2 translation;
uniform vec2 scale;
uniform vec2 bounds;
//uniform float smoothing;
void main()
{
vec2 at = (scale.xy*position.xy+translation.xy)/bounds.xy * vec2(1.0, 1.0);
   gl_Position =  vec4(-1+at.x*2, 1-at.y*2, 0.5, 1.0);
   vtc = vec4(tc, 1.0);
}"""
        )
        shader!!.addSource(
            Shader.Type.fragment, """#version 410
layout(location=0) out vec4 _output;
in vec4 vtc;
//in vec4 col;

uniform sampler2D TE;

void main()
{
    _output = vec4(0,0,0,1);
//    _output.rgb += vec3(texture(TE, vtc.xy));
//    return; 
    
float g = 1.6;
	vec4 current = pow(texelFetch(TE, ivec2(vtc.xy*textureSize(TE,0)+0*vec2(0.5,0.5)), 0), vec4(g,g,g,1));
	current += 0.25*pow(texelFetch(TE, ivec2(vtc.xy*textureSize(TE,0)+0*vec2(0.5,0.5))+ivec2(1.0,0), 0), vec4(g,g,g,1));
	current += 0.25*pow(texelFetch(TE, ivec2(vtc.xy*textureSize(TE,0)+0*vec2(0.5,0.5))+ivec2(0,1.), 0), vec4(g,g,g,1));
	current += 0.25*pow(texelFetch(TE, ivec2(vtc.xy*textureSize(TE,0)+0*vec2(0.5,0.5))+ivec2(-1.,0), 0), vec4(g,g,g,1));
	current += 0.25*pow(texelFetch(TE, ivec2(vtc.xy*textureSize(TE,0)+0*vec2(0.5,0.5))+ivec2(0,-1.), 0), vec4(g,g,g,1));
current = current/2;
current = pow(current, vec4(1/g, 1/g, 1/g, 1));	float m = min(current.x, min(current.y, current.z));
float sat = 0.3;
	current.xyz = (current.xyz-vec3(m)*sat)/(1-sat);
float d = (current.x+current.y+current.z)/3;
current.xyz = pow(current.xyz, vec3(1.1));
	_output  = vec4(current.zyx,max(0.6, min(1, d*3))*current.w*vtc.z);
float e = 0.005;
//	 if (vtc.x<e || vtc.x>1-e || vtc.y<e || vtc.y>1-e*2) _output.w=0;
	 int ccx = ivec2(vtc.xy*textureSize(TE,0)).x;
	 int ccy = ivec2(vtc.xy*textureSize(TE,0)).y;
}"""
        )
        shader!!.asMap_set("translation", OffersUniform {
//            println("translation ${drawing!!.getTranslationRounded()}")
            drawing!!.getTranslationRounded()
        })
        shader!!.asMap_set("scale", OffersUniform {
//            println("scale ${drawing!!.getScale()}")
            drawing!!.getScale()
        })
        shader!!.asMap_set("bounds", OffersUniform {
//            println("bounds ${Vec2(Window.getCurrentWidth(), Window.getCurrentHeight())}")
            Vec2(Window.getCurrentWidth(), Window.getCurrentHeight())
        })
//        shader.attach(Uniform("translation") { drawing!!.getTranslationRounded() })
//        shader.attach(Uniform("scale") { drawing!!.getScale() })
//        shader.attach(Uniform("bounds") { Vec2(Window.getCurrentWidth(), Window.getCurrentHeight()) })
        shader!!.attach(-2, "__rectupdate__") { x: Int? ->
            val r = properties.get(frame)
            update(r.x, r.y, 1f)
        }
        shader!!.attach(q)
        shader!!.asMap_set("TE", texture)
        window!!.getCompositor()
            .getLayer(properties.computeIfAbsent(FLineDrawing.layer) { k: Dict.Prop<String?>? -> "__main__" })
            .scene
            .attach(shader)
        properties.putToMap(Boxes.insideRunLoop, "main.__updateSize__", Supplier {
            val r = properties.get(frame)
//            update(r.x, r.y, 1f)
            true
        })
        properties.putToMap(
            FLineDrawing.frameDrawing,
            "__outline__",
            Cached<Box, Any, FLine>(BiFunction { box: Box, previously: FLine? ->
                if (box.properties.isTrue(Mouse.isSticky, false)) return@BiFunction null
                var rect: Rect? = box.properties.get(frame) ?: return@BiFunction null
                val selected = box.properties.isTrue(Mouse.isSelected, false) || focus
                val f = FLine()
                rect = if (selected) rect!!.inset(-10f) else rect!!.inset(-0.5f)
                f.moveTo(rect.x.toDouble(), rect.y.toDouble())
                f.lineTo((rect.x + rect.w).toDouble(), rect.y.toDouble())
                f.lineTo((rect.x + rect.w).toDouble(), (rect.y + rect.h).toDouble())
                f.lineTo(rect.x.toDouble(), (rect.y + rect.h).toDouble())
                f.lineTo(rect.x.toDouble(), rect.y.toDouble())
                f.attributes.put(
                    StandardFLineDrawing.color,
                    if (selected) Vec4(0, 0, 0, -0.15f) else Vec4(0, 0, 0, 0.15f)
                )
                f.attributes.put(StandardFLineDrawing.filled, false)
                f.attributes.put(
                    StandardFLineDrawing.thicken,
                    BasicStroke(if (selected) 15.5f else 1.5f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER)
                )
                f.attributes.put(StandardFLineDrawing.stroked, true)
                f
            }) { box: Box ->
                Triple<Any?, Any?, Any?>(
                    box.properties.get(frame),
                    box.properties.isTrue(Mouse.isSelected, false),
                    focus
                )
            })

        // AWT's MouseEvent constructor throws an NPE unless you give it a component.
        val component: Component = object : Component() {
            override fun getLocationOnScreen(): Point {
                return Point(0, 0)
            }
        }
        properties.putToMap(Mouse.onMouseDown, "__browser__", OnMouseDown { e: Window.Event<MouseState>, button: Int ->
            Log.log("selection") {
                "is browser hidden ? " + properties.isTrue(
                    hidden, false
                ) + " " + this
            }
            val r = properties.get(frame)

//			if (!intersects(r, e)) return null;
            if (!intersects(r.inset(10f), e)) return@OnMouseDown null
            val gutterWidth = 50f
            if (intersects(Rect(r.x, r.y, gutterWidth, r.h), e)) return@OnMouseDown null
            if (properties.isTrue(hidden, false)) return@OnMouseDown null
            if (e.after.keyboardState.isSuperDown) return@OnMouseDown null
            e.properties.put(Window.consumed, true)
            val drawing = this.find(Drawing.drawing, both())
                .findFirst()
            val point = Vec2(e.after.mx, e.after.my)
            if (isSelected && properties.isTrue(Mouse.isSticky, false)) e.properties.put(Window.consumed, true) else {
//				setFocus(true);
                if (!properties.isTrue(Mouse.isSticky, false)) return@OnMouseDown null
            }
            browser!!.sendMouseEvent(
                MouseEvent(
                    component,
                    MouseEvent.MOUSE_PRESSED,
                    0,
                    MouseEvent.getMaskForButton(button + 1) or if (e.after.keyboardState.isAltDown) KeyEvent.ALT_DOWN_MASK else 0,
                    ((point.x - r.x).toInt() * rsf).toInt(),
                    ((point.y - r.y).toInt() * rsf).toInt(),
                    1,
                    false,
                    button + 1
                )
            )
            dragOngoing = true
            focus = true
            Dragger { e2: Window.Event<MouseState>, term: Boolean ->
                val point2 = Vec2(e2.after.mx, e2.after.my)
                e2.properties.put(Window.consumed, true)
                if (!term) {
                    browser!!.sendMouseEvent(
                        MouseEvent(
                            component,
                            MouseEvent.MOUSE_DRAGGED,
                            0,
                            MouseEvent.getMaskForButton(button + 1),
                            ((point2.x - r.x).toInt() * rsf).toInt(),
                            ((point2.y - r.y).toInt() * rsf).toInt(),
                            1,
                            false,
                            button + 1
                        )
                    )
                } else browser!!.sendMouseEvent(
                    MouseEvent(
                        component,
                        MouseEvent.MOUSE_RELEASED,
                        0,
                        MouseEvent.getMaskForButton(button + 1),
                        ((point2.x - r.x).toInt() * rsf).toInt(),
                        ((point2.y - r.y).toInt() * rsf).toInt(),
                        1,
                        false,
                        button + 1
                    )
                )
                dragOngoing = !term
                !term
            }
        })
        properties.putToMap(Mouse.onMouseMove, "__browser__", OnMouseMove { e: Window.Event<MouseState> ->
            if (dragOngoing) return@OnMouseMove null
            val r = properties.get(frame)
            if (!intersects(r, e)) return@OnMouseMove null
            if (properties.isTrue(hidden, false)) return@OnMouseMove null
            if (isSelected || focus);
            e.properties.put(Window.consumed, true)
            val drawing = this.find(Drawing.drawing, both())
                .findFirst()
            val point = Vec2(e.after.mx, e.after.my)
            browser!!.sendMouseEvent(
                MouseEvent(
                    component, MouseEvent.MOUSE_MOVED, 0, 0, ((point.x - r.x).toInt() * rsf).toInt(),
                    ((point.y - r.y).toInt() * rsf).toInt(), 0, false
                )
            )
            null
        })
        properties.putToMap(Mouse.onMouseScroll, "__browser__", OnMouseScroll { e: Window.Event<MouseState> ->
            val r = properties.get(frame)
            if (!intersects(r, e)) return@OnMouseScroll
            if (!isSelected && !focus) return@OnMouseScroll
            if (properties.isTrue(hidden, false)) return@OnMouseScroll
            e.properties.put(Window.consumed, true)
            val point = Vec2(e.after.mx, e.after.my)
            if (e.after.keyboardState.isAltDown) {
//                browser!!.setFocus(true)
                browser!!.sendMouseEvent(
                    MouseEvent(
                        component,
                        MouseEvent.MOUSE_PRESSED,
                        0,
                        MouseEvent.getMaskForButton(1) or if (e.after.keyboardState.isAltDown) KeyEvent.ALT_DOWN_MASK else 0,
                        ((point.x - r.x).toInt() * rsf).toInt(),
                        ((point.y - r.y).toInt() * rsf).toInt(),
                        1,
                        false,
                        1
                    )
                )
                if (e.after.dwheely > 0) {
                    find(RunCommand.runCommand, both()).findFirst()
                        .ifPresent { x: BiFunctionOfBoxAnd<String?, Boolean?> ->
                            val done = x.apply(root, "Increment Number")
                        }
                } else if (e.after.dwheely < 0) {
                    find(RunCommand.runCommand, both()).findFirst()
                        .ifPresent { x: BiFunctionOfBoxAnd<String?, Boolean?> ->
                            val done = x.apply(root, "Decrement Number")
                        }
                }
            }
            val dy = e.after.dwheely * -8
            browser!!.sendMouseWheelEvent(
                MouseWheelEvent(
                    component,
                    MouseWheelEvent.MOUSE_WHEEL,
                    0,
                    0,
                    ((point.x - r.x).toInt() * rsf).toInt(),
                    ((point.y - r.y).toInt() * rsf).toInt(),
                    ((point.x - r.x).toInt() * rsf * 4).toInt(),
                    ((point.y - r.y).toInt() * rsf).toInt(),
                    0,
                    false,
                    MouseWheelEvent.WHEEL_UNIT_SCROLL,
                    3,
                    dy.toInt(),
                    dy.toDouble()
                )
            )
            val dx = e.after.dwheel * 8
            browser!!.sendMouseWheelEvent(
                MouseWheelEvent(
                    component,
                    MouseWheelEvent.MOUSE_WHEEL,
                    KeyEvent.SHIFT_DOWN_MASK.toLong(),
                    0,
                    ((point.x - r.x).toInt() * rsf).toInt(),
                    ((point.y - r.y).toInt() * rsf).toInt(),
                    ((point.x - r.x).toInt() * rsf).toInt(),
                    ((point.y - r.y).toInt() * rsf).toInt(),
                    0,
                    false,
                    MouseWheelEvent.WHEEL_UNIT_SCROLL,
                    3,
                    dx.toInt(),
                    dx.toDouble()
                )
            )
        })
        properties.putToMap(
            Keyboard.onKeyDown,
            "__browser__",
            OnKeyDown { e: Window.Event<Window.KeyboardState>, k: Int ->

                System.out.println(" key down ! ${e.before} -> ${e.after}")

                //if (/*!isSelected() &&*/ !focussed) return null;
                if (!focus) return@OnKeyDown null
                if (properties.isTrue(hidden, false)) return@OnKeyDown null
                if (true) return@OnKeyDown keyboardHacks!!.onKeyDown(e, k)
//                println(" -- onkeydown $e $k")

//			Log.log("keyboard", "----- Key down :" + e + " " + k);
                var mod = if (e.after.isAltDown) KeyEvent.ALT_DOWN_MASK else 0
                mod = mod or if (e.after.isShiftDown) KeyEvent.SHIFT_DOWN_MASK else 0
                mod = mod or if (e.after.isControlDown) KeyEvent.CTRL_DOWN_MASK else 0
                mod = mod or if (e.after.isSuperDown) KeyEvent.META_DOWN_MASK else 0
                val fmod = mod
                val c = HashSet(e.after.charsDown.values)
                c.removeAll(e.before.charsDown.values)

//			Log.log("keyboard", "key down becomes char ? " + c);
                val translated = mapper.translateCode(k)
                var k = (//				Log.log("keyboard", "key code has a translation :"+translated);
                        translated ?: //				Log.log("keyboard", "skipping, assuming it will be a char");
//				if (c.size()==0)
                        return@OnKeyDown null)
                val ke = KeyEvent(component, KeyEvent.KEY_PRESSED, 0, mod, k, k.toChar())

//			Log.log("keyboard", "consuming keyboard event :"+e);
                e.properties.put(Window.consumed, true)

//			Log.log("keyboard", "Running key pressed and then released " + ke);
                browser!!.sendKeyEvent(ke)
                Drawing.dirty(this)
                val ke3 = KeyEvent(
                    component,
                    KeyEvent.KEY_RELEASED,
                    0,
                    fmod,
                    k,
                    KeyEvent.CHAR_UNDEFINED
                )
                browser!!.sendKeyEvent(ke3)
                val k2 = k
                Hold { e2: Window.Event<Window.KeyboardState?>, term: Boolean ->
                    if (term) {
                        Log.log("keyboard") { "actual up $e $k" }
                        val ke2 = KeyEvent(
                            component,
                            KeyEvent.KEY_RELEASED,
                            0,
                            fmod,
                            k,
                            KeyEvent.CHAR_UNDEFINED
                        )
                        browser!!.sendKeyEvent(ke2)
                        e2.properties.put(Window.consumed, true)
                        Drawing.dirty(this)
                    }
                    !term
                }
            })
        properties.putToMap(
            Keyboard.onCharTyped,
            "__browser__",
            OnCharTyped { e: Window.Event<Window.KeyboardState>, k: Char ->

//			if (!isSelected() && !focussed) return;
                if (!focus) return@OnCharTyped
                if (properties.isTrue(hidden, false)) return@OnCharTyped
                if (true) {
                    keyboardHacks!!.onCharDown(e, k)
                    return@OnCharTyped
                }

//			Log.log("keyboard", "---- CHAR TYPED:" + e + " " + k);
                var found: Int? = null
                val ii: Iterator<Int> = e.after.keysDown.iterator()
                while (ii.hasNext()) {
                    val g = ii.next()
                    if (mapper.isModifier(g)) continue
                    val code = mapper.translateCode(g)
                    if (code != null && (!e.before.keysDown.contains(g) || e.before.charsDown == e.after.charsDown)) {
                        found = code
                        break
                    }
                }
                var mod = if (e.after.isAltDown) KeyEvent.ALT_DOWN_MASK else 0
                mod = mod or if (e.after.isShiftDown) KeyEvent.SHIFT_DOWN_MASK else 0
                mod = mod or if (e.after.isControlDown) KeyEvent.CTRL_DOWN_MASK else 0
                mod = mod or if (e.after.isSuperDown) KeyEvent.META_DOWN_MASK else 0
                val fmod = mod

//			Log.log("keyboard", "mod :" + fmod);
//			Log.log("keyboard", "found translation?:"+found);
                e.properties.put(Window.consumed, true)
                if (found == null) {
//				if (mod==0)
//				{
                    Log.log("keyboard") { "sending char $k" }
                    val ke = KeyEvent(component, KeyEvent.KEY_TYPED, 0, mod, KeyEvent.VK_UNDEFINED, k)
                    Log.log("keyboard") { "awt event is $ke" }
                    browser!!.sendKeyEvent(ke)
                    //				}
//				else
//				{
//					KeyEvent ke = new KeyEvent(component, KeyEvent.KEY_PRESSED, 0, mod, k, k);
//					browser.sendKeyEvent(ke);
//					ke = new KeyEvent(component, KeyEvent.KEY_RELEASED, 0, mod, k, k);
//					browser.sendKeyEvent(ke);
//				}
                } else {
                    Log.log("keyboard") { "faking keypress instead because we found a translation" }
                    var ke = KeyEvent(component, KeyEvent.KEY_PRESSED, 0, mod, found, KeyEvent.CHAR_UNDEFINED)
                    browser!!.sendKeyEvent(ke)
                    ke = KeyEvent(component, KeyEvent.KEY_RELEASED, 0, mod, found, KeyEvent.CHAR_UNDEFINED)
                    browser!!.sendKeyEvent(ke)
                }
                Drawing.dirty(this)
            })
        properties.putToMap(Boxes.insideRunLoop, "main.__pullFocus__", Supplier {
            if (!isSelected && !focus) {
                keyboardFocus.disclaimFocus(this)
            } else if (properties.isTrue(hidden, false)) {
                keyboardFocus.disclaimFocus(this)
            } else {
                keyboardFocus.claimFocus(this)
            }
            true
        })
        properties.putToMap(Callbacks.onSelect, "__pullFocus__", FunctionOfBox { k: Box? ->
            if (k !is Browser) keyboardFocus.claimFocus(k)
            null
        })
        properties.putToMap(Callbacks.onDeselect, "__pullFocus__", FunctionOfBox { k: Box? ->
            if (k !is Browser) keyboardFocus.disclaimFocus(k)
            null
        })
    }

    var cachedFocus: KeyboardFocus? = null
    private val keyboardFocus: KeyboardFocus
        private get() = if (cachedFocus == null) find(KeyboardFocus._keyboardFocus, both()).findFirst()
            .get().also { cachedFocus = it } else cachedFocus!!
    private val isSelected: Boolean
        private get() = properties.isTrue(Mouse.isSelected, false)

    private fun intersects(r: Rect, e: Window.Event<MouseState>): Boolean {
        val drawing = this.find(Drawing.drawing, both())
            .findFirst()
        val point = drawing.map { x: Drawing -> x.windowSystemToDrawingSystem(Vec2(e.after.x, e.after.y)) }
            .orElseThrow { IllegalArgumentException(" can't mouse around something without drawing support (to provide coordinate system)") }
        return r.x < point.x && r.x + r.w > point.x && r.y < point.y && r.y + r.h > point.y
    }

    /**
     * called from some random thread, buffer only good for duration of call. ?
     */
    protected fun paint(popup: Boolean, dirty: Array<Rectangle>, buffer: ByteBuffer, w: Int, h: Int) {

//		System.out.println(" paint :" + buffer + " " + w + " " + h);
        if (dirty.size == 0) return
        sourceView!!.clear()
        buffer.clear()
        var x0 = w
        var x1 = 0
        var y0 = h
        var y1 = 0
        for (r in dirty) {
//            System.out.println(" -- "+r);
            if (r.x == 0 && r.y == 0 && r.width == w && r.height == h) {
                buffer.clear()
                sourceView!!.clear()
                sourceView!!.put(buffer)
                x0 = 0
                y0 = 0
                x1 = w
                y1 = h
            } else {
                for (y in r.y until r.y + r.height) {
                    buffer.limit(r.x * 4 + y * 4 * w + r.width * 4)
                    buffer.position(r.x * 4 + y * 4 * w)
                    sourceView!!.limit(r.x * 4 + y * 4 * w + r.width * 4)
                    sourceView!!.position(r.x * 4 + y * 4 * w)
                    sourceView!!.put(buffer)
                    sourceView!!.clear()
                    buffer.clear()

//                    for(int x=0;x<w;x++)
//                    {
//                        if (x%5==0)
//                            sourceView.put(y*4*w+x*4+1, (byte) 255);
//                    }
                }
                //                System.out.println("r= " + r);
                x0 = Math.min(x0, r.x)
                x1 = Math.max(x1, r.width + r.x)
                y0 = Math.min(y0, r.y)
                y1 = Math.max(y1, r.height + r.y)
            }
        }
        sourceView!!.clear()
        buffer.clear()


        // threading ?
        Drawing.dirty(this@Browser)
        root!!.properties.put(Drawing.needRepaint, true)
        window!!.requestRepaint()
        RunLoop.main.shouldSleep.add(this@Browser)
        damage = if (damage == null) Rect(
            x0.toDouble(),
            y0.toDouble(),
            (x1 - x0).toDouble(),
            (y1 - y0).toDouble()
        ) else damage!!.union(
            Rect(
                x0.toDouble(), y0.toDouble(), (x1 - x0).toDouble(), (y1 - y0).toDouble()
            )
        )

//        System.out.println(" damage :"+damage);
        this.dirty.set(true)

//        System.out.println(" clean exit :"+damage+" "+this.dirty.get());
    }

    protected fun message(id: Long, message: String?, reply: Consumer<String?>?) {
//        println(" -- message ! $id $message $reply")
        if (message != null && reply != null)
            synchronized(messages) { messages.add(Pair(message, reply)) }
    }

    fun addHandler(s: Predicate<String>, h: Handler) {
        handlers.add(Pair(s, h))
    }

    fun clearHandlers() {
        handlers.clear()
    }

    fun executeJavaScript(s: String?) {
        browser?.executeJavaScript(s, "", 0)
    }

    fun executeJavaScript_queued(s: String?) {
        if (ignore) return
        if (booted) {
            executeJavaScript(s)
        } else {
            bootQueue.add(Runnable { executeJavaScript(s) })
        }
    }

    fun pauseForBoot() {
        booted = false
    }

    fun pauseNow() {
        paused = true
    }

    fun unpauseNow() {
        paused = false
        ignore = false
    }

    fun pauseAndIgnoreNow() {
        paused = true
        ignore = true
    }

    fun finishBooting() {
        booted = true
        val bq: List<Runnable> = ArrayList(bootQueue)
        bootQueue.clear()
        bq.forEach(Consumer { x: Runnable -> x.run() })
    }

    protected fun update(x: Float, y: Float, scale: Float) {
        if (dirty.getAndSet(false) && damage != null) {
            if (check-- > 0) {
                if (Main.os != Main.OS.windows) browser!!.zoomLevel =
                    (ezl * window!!.retinaScaleFactor).toDouble() else {
                    browser!!.zoomLevel = ezl.toDouble()
                }
            }
            Log.log("cef.debug") { " texture was dirty, uploading " }

//            System.out.println(" calling upload on the texture 1  "+shader);
//            texture!!.upload(
//                source,
//                false,
//                damage!!.x.toInt(),
//                damage!!.y.toInt(),
//                (damage!!.w + damage!!.x).toInt(),
//                (1 + damage!!.h + damage!!.y).toInt()
//            )
            texture!!.forceUploadNow(source);
            Drawing.dirty(this)
            again = 1
            hasRepainted = true
            RunLoop.main.shouldSleep.add(this)
        } else if (again > 0 && damage != null) {
            Log.log("cef.debug") { " texture was dirty $again call, uploading " }

//            System.out.println(" calling upload on the texture 2 ");
//            texture!!.upload(
//                source,
//                false,
//                damage!!.x.toInt(),
//                damage!!.y.toInt(),
//                (damage!!.w + damage!!.x).toInt(),
//                (1 + damage!!.h + damage!!.y).toInt()
//            )
            texture!!.forceUploadNow(source);
            Drawing.dirty(this)
            RunLoop.main.shouldSleep.add(this)
            again--
        } else if (again == 0) {
            damage = null
            RunLoop.main.shouldSleep.remove(this)
        }


//		if (hasRepainted)
        run {
            navigation.apply(this)
            geometry.apply(this)
            direct.apply(this)
        }
        var m: ArrayList<Pair<String, Consumer<String?>>>
        synchronized(messages) {
            m = ArrayList(messages)
            messages.clear()
        }
        for (p in m) {
//            Log.log("cef.debug") { "dispatching message <" + p.first + ">" }
//            println(" -- dispatching message $p ${Thread.currentThread().name}")
//            println(".")
            val o = JSONObject(p.first)
            val address = o.getString("address")
            var payload = o["payload"]
            if (payload !is JSONObject) {
                val sw = StringWriter()
                val w = JSONWriter(sw)
                w.`object`()
                w.key("message")
                w.value(payload.toString() + "")
                w.endObject()
                payload = JSONObject(sw.toString())
            }
            if (!TransientCommands.transientCommands.handle(address, payload, p.second)) for (p2 in handlers) {
                if (p2.first.test(address)) {
                    Log.log("cef.debug") { "found handler" }
                    try {
                        p2.second.handle(address, payload, p.second)
                    } catch (t: Throwable) {
                        t.printStackTrace()
                    }
                }
            }
        }
    }

    var out_cached: Out? = null
    fun printHTML(text: String) {
        executeJavaScript_queued(
            "$(document.body).append('" + TextUtils.quoteNoOuter(
                text.replace(
                    "'",
                    "\""
                )
            ) + "');" + scrollDown()
        )
    }

    fun print(text: Any) {
        outCached
        var texts = ""
        texts = if (out_cached != null) {
            out_cached!!.convert(text)
        } else {
            text.toString() + ""
        }
        executeJavaScript_queued(
            "$(document.body).append('" + TextUtils.quoteNoOuter(
                texts.replace(
                    "'",
                    "\""
                )
            ) + "');" + scrollDown()
        )
    }

    fun println(text: Any) {
        outCached
        var texts = ""
        texts = if (out_cached != null) {
            out_cached!!.convert(text)
        } else {
            text.toString() + ""
        }
        executeJavaScript_queued(
            "$(document.body).append('" + TextUtils.quoteNoOuter(
                texts.replace(
                    "'",
                    "\""
                )
            ) + "<br>');" + scrollDown()
        )
    }

    fun clearAndPrint(text: Any) {
        clear()
        print(text)
    }

    fun printText(text: String) {
        executeJavaScript_queued(
            "$(document.body).append('<pre style=\"padding:3px;margin:3px;\">" + TextUtils.quoteNoOuter(
                text.replace("'", "\"")
            ) + "</pre>');" + scrollDown()
        )
    }

    fun clear() {
        executeJavaScript_queued("document.body.innerHTML=''")
    }

    private fun scrollDown(): String {
        return "document.body.scrollTop=document.body.scrollHeight"
    }

    var focus: Boolean
        get() = keyboardFocus.isFocused(this)
        set(f) {
            if (f != focus) Drawing.dirty(this)
//            browser!!.setFocus(f)
            if (f) {
                keyboardFocus.claimFocus(this)
            } else {
                keyboardFocus.disclaimFocus(this)
            }
        }

    fun injectCSS(css: String) {
        executeJavaScript_queued(
            """
                var css = document.createElement("style");
                css.type = "text/css";
                css.innerHTML = "$css";
                document.body.appendChild(css);
                """.trimIndent()
        )
    }

    fun interface Handler {
        fun handle(address: String?, payload: JSONObject?, reply: Consumer<String?>?)
    }

    fun reload() {
        browser!!.reload()
    }

    companion object {
        @JvmField
        val url = Dict.Prop<String>("url")//.type<String>()
            .toCanon<String>()
            .doc<String>("URL for the browser. Setting this will cause the browser to navigate, and repaint automatically")
        val html = Dict.Prop<String>("html")//.type<String>()
            .toCanon<Any>()
            .doc<String>("HTML for the browser. Setting this will cause the browser to reload it's contents from this string, and repaint automatically")
    }
}