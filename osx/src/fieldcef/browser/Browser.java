package fieldcef.browser;

import field.app.RunLoop;
import field.graphics.*;
import field.graphics.Window;
import field.graphics.util.KeyEventMapping;
import field.linalg.Vec2;
import field.linalg.Vec4;
import field.utility.*;
import fieldagent.Main;
import fieldbox.boxes.*;
import fieldbox.boxes.plugins.Chorder;
import fieldbox.boxes.plugins.KeyboardFocus;
import fieldbox.boxes.plugins.Planes;
import fieldbox.io.IO;
import fieldbox.ui.FieldBoxWindow;
import fieldcef.plugins.BrowserKeyboardHacks;
import fielded.TextUtils;
import fielded.boxbrowser.TransientCommands;
import fielded.plugins.Out;
import org.cef.browser.CefRendererBrowserBuffer;
import org.json.JSONObject;
import org.json.JSONWriter;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static field.graphics.StandardFLineDrawing.*;

/**
 * The first look at embedded HTML. A Browser is a Field/Graphics texture map, a CEF browser of a fixed size, a quad for drawing it, a shader for drawing the quad, and some logic for generating and
 * transforming events. Chrome in a box.
 * <p>
 */
public class Browser extends Box implements IO.Loaded {

	static public final Dict.Prop<String> url = new Dict.Prop<String>("url").type()
		.toCanon()
		.doc("URL for the browser. Setting this will cause the browser to navigate, and repaint automatically");
	static public final Dict.Prop<String> html = new Dict.Prop<String>("html").type()
		.toCanon()
		.doc("HTML for the browser. Setting this will cause the browser to reload it's contents from this string, and repaint automatically");
	private final KeyEventMapping mapper = new KeyEventMapping();
	public CefRendererBrowserBuffer browser;
	protected BrowserKeyboardHacks keyboardHacks;
	protected AtomicBoolean dirty = new AtomicBoolean(false);
	protected boolean booted = true;
	boolean dragOngoing = false;
	List<Pair<Predicate<String>, Handler>> handlers = new ArrayList<>();
	Deque<Pair<String, Consumer<String>>> messages = new ArrayDeque<>();
	List<Runnable> bootQueue = new ArrayList<>();
	Cached<Box, String, Void> navigation = new Cached<>((now, nothing) -> {
		String u = now.properties.get(url);

		if (u != null) {
			Log.log("HTML", () -> "loading URL <" + u + ">");
			browser.loadURL(u);
		}

		return null;
	}, (box) -> box.properties.get(url));
	Cached<Box, String, Void> direct = new Cached<>((now, nothing) -> {
		String u = now.properties.get(html);

		if (u != null) {
			Log.log("HTML", () -> "loading html");
			browser.loadString(u, u);
		}

		return null;
	}, (box) -> box.properties.get(html));
	int again = 0;
	int check = 10;
	boolean hasRepainted = false;
	boolean first = true;
	boolean paused = false;
	boolean ignore = false;
	private int w;
	private int h;
	private ByteBuffer source;
	private Texture texture;
	private BaseMesh q;
	private MeshBuilder builder;
	private Drawing drawing;

	Cached<Box, Object, Void> geometry = new Cached<>((now, nothing) -> {

		Rect r = now.properties.get(Box.frame);

		float op = now.properties.getFloat(StandardFLineDrawing.opacity, 1);

		op = (float) Math.sqrt(op);

		if (now.properties.isTrue(FLineDrawing.hidden, false)) {
			builder.open();
			builder.close();
			return null;
		}

		builder.open();

		int ns = 30;

		float sx = (float) drawing.getScale().x;
		float sy = (float) drawing.getScale().y;

		for (int x = 0; x < ns; x++) {
			for (int y = 0; y < ns; y++) {
				float ax = (0.f + x) / (float) (ns - 1);
				float ay = (0.f + y) / (float) (ns - 1);

				builder.aux(5, ax * sx * r.w / w, ay * sy * (r.h + 0.5f) / h, op);
				builder.v(r.x + ax * r.w, r.y + ay * r.h, 0);
			}
		}

		for (int x = 0; x < ns - 1; x++) {
			for (int y = 0; y < ns - 1; y++) {
				builder.e_quad((x + 1) * ns + y, x * ns + y, x * ns + y + 1, (x + 1) * ns + y + 1);
			}
		}

		builder.close();

		return null;
	}, (box) -> new Quad<>(box.properties.getFloat(StandardFLineDrawing.opacity, 1), drawing.getScale(), box.properties.get(Box.frame), box.properties.isTrue(FLineDrawing.hidden, false)));

	private Shader shader;
	private ByteBuffer sourceView;
	private FieldBoxWindow window;
	private Box root;
	private Rect damage = new Rect(0, 0, 0, 0);
	public Runnable callbackOnNextReload = null;
	private Object outCached;
	private long charTypedAt;

	public Browser() {
	}

	public void attachToShader(Shader s) {
		s.attach(-2, "__rectupdate__", x -> {
			Rect r = properties.get(Box.frame);
			update(r.x, r.y, 1/*r.w/w*/);
		});

		s.asMap_set("te", texture);
		s.attach(q);

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

	public void loaded() {
		Log.disable("cef.*");

		this.properties.computeIfAbsent(Box.frame, (k) -> new Rect(0, 0, 512, 512));
		this.properties.put(Box.name, "(browser)");
		this.properties.put(Planes.plane, "__always__");
		this.properties.put(Chorder.nox, true);
		Shader shader = new Shader();

		this.properties.putToListMap(Callbacks.onDelete, (bx) -> {

			if (bx != this) return null;

			browser.close();
			window.getCompositor()
				.getLayer(properties.computeIfAbsent(FLineDrawing.layer, k -> "__main__"))
				.getScene()
				.detach(shader);

			return null;
		});

		this.w = (int) this.properties.get(Box.frame).w;
		this.h = (int) this.properties.get(Box.frame).h;

		root = this.find(Boxes.root, both())
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException(" did you call loaded without adding to the graph?"));

		window = root.first(Boxes.window)
			.orElseThrow(() -> new IllegalArgumentException(" can't draw a box hierarchy with no window to draw it in !"));
		drawing = root.first(Drawing.drawing)
			.orElseThrow(() -> new IllegalArgumentException(" can't install text-drawing into something without drawing support"));


		float rsf = window.getRetinaScaleFactor();

		System.out.println("CefSystem is initializing a browser at " + w + "x" + h);

		browser = CefSystem.cefSystem.makeBrowser((int) (w * rsf), (int) (h * rsf), this::paint, this::message, () -> {
			try {
				if (callbackOnNextReload != null) {
					callbackOnNextReload.run();
				}
			} catch (Throwable t) {
				t.printStackTrace();

			} finally {
				callbackOnNextReload = null;
			}

		});


		keyboardHacks = new BrowserKeyboardHacks(browser);
		source = ByteBuffer.allocateDirect(((int) (w * rsf) * ((int) (h * rsf)) * 4));
		source.position(0)
			.limit(source.capacity());
		sourceView = source.slice();
		texture = new Texture(Texture.TextureSpecification.byte4(0, (int) (w * rsf), (int) (h * rsf), source, false)).setIsDoubleBuffered(false);

		q = BaseMesh.triangleList(0, 0);
		builder = new MeshBuilder(q);

		shader.addSource(Shader.Type.vertex, "#version 410\n" +
			"layout(location=0) in vec3 position;\n" +
			"layout(location=5) in vec4 tc;\n" +
			"out vec4 vtc;\n" +
			"out float op;\n" +

			"uniform vec2 translation;\n" +
			"uniform vec2 scale;\n" +
			"uniform vec2 bounds;\n" +
			"uniform float smoothing;\n" +

			"void main()\n" +
			"{\n" +
			"	vec2 at = (scale.xy*position.xy+translation.xy)/bounds.xy;\n" +
			"   gl_Position =  vec4(-1+at.x*2, 1-at.y*2, 0.5, 1.0);\n" +
			"   vtc =tc;\n" +
			"}");


		shader.addSource(Shader.Type.fragment, "#version 410\n" +
			"layout(location=0) out vec4 _output;\n" +
			"in vec4 vtc;\n" +
			"in vec4 col;\n" +
			"uniform sampler2D te;\n" +
			"\n" +
			"void main()\n" +
			"{\n" +
			"ivec2 TC = ivec2(vtc.xy*textureSize(te,0));\n" +
			"float g = 1.6;\n" +
			"\tvec4 current = pow(texelFetch(te, ivec2(vtc.xy*textureSize(te,0)+vec2(0.25,0.25)), 0), vec4(g,g,g,1));\n" +
			"\tcurrent += 0.25*pow(texelFetch(te, ivec2(vtc.xy*textureSize(te,0)+vec2(0.25,0.25))+ivec2(1.0,0), 0), vec4(g,g,g,1));\n" +
			"\tcurrent += 0.25*pow(texelFetch(te, ivec2(vtc.xy*textureSize(te,0)+vec2(0.25,0.25))+ivec2(0,1.), 0), vec4(g,g,g,1));\n" +
			"\tcurrent += 0.25*pow(texelFetch(te, ivec2(vtc.xy*textureSize(te,0)+vec2(0.25,0.25))+ivec2(-1.,0), 0), vec4(g,g,g,1));\n" +
			"\tcurrent += 0.25*pow(texelFetch(te, ivec2(vtc.xy*textureSize(te,0)+vec2(0.25,0.25))+ivec2(0,-1.), 0), vec4(g,g,g,1));\n" +
			"current = current/2;\n" +
			"current = pow(current, vec4(1/g, 1/g, 1/g, 1));" +
			"\tfloat m = min(current.x, min(current.y, current.z));\n" +
			"float sat = 0.2;\n" +
			"\tcurrent.xyz = (current.xyz-vec3(m)*sat)/(1-sat);\n" +
			"float d = (current.x+current.y+current.z)/3;\n" +
			"current.xyz = pow(current.xyz, vec3(1.25));\n" +
			"\t_output  = vec4(current.zyx,max(0.4, min(1, d*8))*current.w*vtc.z);\n" +
			"float ee = 0.001f;\n" +
			"\t if (vtc.x<ee || vtc.x>1-ee || vtc.y<ee || vtc.y>1-ee) _output.w=0;\n" +
//			"\t _output=vec4(current.xyz,1);\n" +
//			"_output+=vec4(TC.x%15<1 ? 1 : 0, 0, 0, 1)*0.5;\n" +
			"}");


		shader.attach(new Uniform<Vec2>("translation", () -> drawing.getTranslationRounded()));
		shader.attach(new Uniform<Vec2>("scale", () -> drawing.getScale()));
		shader.attach(new Uniform<Vec2>("bounds", () -> new Vec2(Window.getCurrentWidth(), Window.getCurrentHeight())));

		shader.attach(-2, "__rectupdate__", x -> {
			Rect r = properties.get(Box.frame);
			update(r.x, r.y, 1/*r.w/w*/);
		});

		shader.attach(q);
		shader.attach(texture);

		window.getCompositor()
			.getLayer(properties.computeIfAbsent(FLineDrawing.layer, k -> "__main__"))
			.getScene()
			.attach(shader);

		this.properties.putToMap(Boxes.insideRunLoop, "main.__updateSize__", () -> {
			Rect r = properties.get(Box.frame);
			update(r.x, r.y, 1/*r.w/w*/);
			return true;
		});

		properties.putToMap(FLineDrawing.frameDrawing, "__outline__", new Cached<Box, Object, FLine>((box, previously) -> {
			if (box.properties.isTrue(Mouse.isSticky, false)) return null;

			Rect rect = box.properties.get(frame);
			if (rect == null) return null;

			boolean selected = box.properties.isTrue(Mouse.isSelected, false) || getFocus();

			FLine f = new FLine();
			if (selected) rect = rect.inset(-10f);
			else rect = rect.inset(-0.5f);

			f.moveTo(rect.x, rect.y);
			f.lineTo(rect.x + rect.w, rect.y);
			f.lineTo(rect.x + rect.w, rect.y + rect.h);
			f.lineTo(rect.x, rect.y + rect.h);
			f.lineTo(rect.x, rect.y);

			f.attributes.put(color, selected ? new Vec4(0, 0, 0, -0.15f) : new Vec4(0, 0, 0, 0.15f));
			f.attributes.put(filled, false);
			f.attributes.put(thicken, new BasicStroke(selected ? 15.5f : 1.5f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));

			f.attributes.put(stroked, true);

			return f;
		}, (box) -> new Triple(box.properties.get(frame), box.properties.isTrue(Mouse.isSelected, false), getFocus())));

		// AWT's MouseEvent constructor throws an NPE unless you give it a component.
		Component component = new Component() {
			@Override
			public Point getLocationOnScreen() {
				return new Point(0, 0);
			}
		};


		this.properties.putToMap(Mouse.onMouseDown, "__browser__", (e, button) -> {

			Log.log("selection", () -> "is browser hidden ? " + properties.isTrue(Box.hidden, false) + " " + this);

			Rect r = properties.get(Box.frame);

//			if (!intersects(r, e)) return null;

			if (!intersects(r.inset(10), e)) return null;

			float gutterWidth = 0;

			if (properties.isTrue(Box.hidden, false)) return null;

			if (e.after.keyboardState.isSuperDown()) return null;

			if (intersects(new Rect(r.x, r.y, gutterWidth, r.h), e))
				return null;

			Optional<Drawing> drawing = this.find(Drawing.drawing, both())
				.findFirst();
			Vec2 point = new Vec2(e.after.mx, e.after.my);

			if (isSelected() && properties.isTrue(Mouse.isSticky, false))
				e.properties.put(Window.consumed, !intersects(new Rect(r.x, r.y, gutterWidth, r.h), e));
			else {
//				setFocus(true);
				if (!properties.isTrue(Mouse.isSticky, false)) return null;
			}

			Vec2 SC = drawing.get().getScale();

			browser.sendMouseEvent(
				new MouseEvent(component, MouseEvent.MOUSE_PRESSED, 0, MouseEvent.getMaskForButton(button + 1) | (e.after.keyboardState.isAltDown() ? KeyEvent.ALT_DOWN_MASK : 0),
					(int) ((int) (point.x - r.x) * rsf * SC.x), (int) ((int) (point.y - r.y) * rsf * SC.x), 1, false, button + 1));

			dragOngoing = true;

			setFocus(true);

			return (e2, term) -> {

				Vec2 point2 = new Vec2(e2.after.mx, e2.after.my);

				if (e2 != e)
					e2.properties.put(Window.consumed, true);

				if (!term) {
					browser.sendMouseEvent(new MouseEvent(component, MouseEvent.MOUSE_DRAGGED, 0, MouseEvent.getMaskForButton(button + 1),
						((int) ((int) (point2.x - r.x) * rsf * SC.x)), ((int) ((int) (point2.y - r.y) * rsf * SC.x)), 1, false,
						button + 1));
				} else browser.sendMouseEvent(
					new MouseEvent(component, MouseEvent.MOUSE_RELEASED, 0, MouseEvent.getMaskForButton(button + 1), ((int) ((int) (point2.x - r.x) * rsf * SC.x)),
						((int) ((int) (point2.y - r.y) * rsf * SC.x)), 1, false, button + 1));

				dragOngoing = !term;

				return !term;
			};
		});

		this.properties.putToMap(Mouse.onMouseMove, "__browser__", (e) -> {

			if (dragOngoing) return null;

			Rect r = properties.get(Box.frame);
			if (!intersects(r, e)) return null;
			if (properties.isTrue(Box.hidden, false)) return null;

			if (isSelected() || getFocus()) ;
			e.properties.put(Window.consumed, true);

			Optional<Drawing> drawing = this.find(Drawing.drawing, both())
				.findFirst();
			Vec2 point = new Vec2(e.after.mx, e.after.my);
			Vec2 SC = drawing.get().getScale();


			browser.sendMouseEvent(new MouseEvent(component, MouseEvent.MOUSE_MOVED, 0, 0, ((int) ((int) (point.x - r.x) * rsf * SC.x)),
				((int) ((int) (point.y - r.y) * rsf * SC.y)), 0, false));
			return null;
		});

		this.properties.putToMap(Mouse.onMouseScroll, "__browser__", (e) -> {
			Rect r = properties.get(Box.frame);
			if (!intersects(r, e)) return;
			if (!isSelected() && !getFocus()) return;
			if (properties.isTrue(Box.hidden, false)) return;


			e.properties.put(Window.consumed, true);


			Vec2 point = new Vec2(e.after.mx, e.after.my);


			float dy = e.after.dwheely * 8;
			browser.sendMouseWheelEvent(new MouseWheelEvent(component, MouseWheelEvent.MOUSE_WHEEL, 0, 0, ((int) ((int) (point.x - r.x) * rsf)),
				((int) ((int) (point.y - r.y) * rsf)), ((int) ((int) (point.x - r.x) * rsf * 4)),
				((int) ((int) (point.y - r.y) * rsf)), 0, false, MouseWheelEvent.WHEEL_UNIT_SCROLL, 1, (int) dy, dy));
			float dx = e.after.dwheel * 8;
			browser.sendMouseWheelEvent(new MouseWheelEvent(component, MouseWheelEvent.MOUSE_WHEEL, 0, KeyEvent.SHIFT_DOWN_MASK, ((int) ((int) (point.x - r.x) * rsf)),
				((int) ((int) (point.y - r.y) * rsf)), ((int) ((int) (point.x - r.x) * rsf)),
				((int) ((int) (point.y - r.y) * rsf)), 0, false, MouseWheelEvent.WHEEL_UNIT_SCROLL, 1, (int) dx, dx));
		});

		this.properties.putToMap(Keyboard.onKeyDown, "__browser__", (e, k) -> {

			//if (/*!isSelected() &&*/ !focussed) return null;
			if (!getFocus()) return null;
			if (properties.isTrue(Box.hidden, false)) return null;

			if (true) {
				charTypedAt = System.currentTimeMillis();

				return keyboardHacks.onKeyDown(e, k);
			}

//			Log.log("keyboard", "----- Key down :" + e + " " + k);

			int mod = (e.after.isAltDown() ? KeyEvent.ALT_DOWN_MASK : 0);
			mod |= (e.after.isShiftDown() ? KeyEvent.SHIFT_DOWN_MASK : 0);
			mod |= (e.after.isControlDown() ? KeyEvent.CTRL_DOWN_MASK : 0);
			mod |= (e.after.isSuperDown() ? KeyEvent.META_DOWN_MASK : 0);

			int fmod = mod;

			HashSet<Character> c = new HashSet<Character>(e.after.charsDown.values());
			c.removeAll(e.before.charsDown.values());

//			Log.log("keyboard", "key down becomes char ? " + c);


			Integer translated = mapper.translateCode(k);
			if (translated != null) {
//				Log.log("keyboard", "key code has a translation :"+translated);
				k = translated;
			} else {
//				Log.log("keyboard", "skipping, assuming it will be a char");
//				if (c.size()==0)
				return null;
			}

			int fk = k;

			KeyEvent ke = new KeyEvent(component, KeyEvent.KEY_PRESSED, 0, mod, fk, (char) fk);

//			Log.log("keyboard", "consuming keyboard event :"+e);
			e.properties.put(Window.consumed, true);

//			Log.log("keyboard", "Running key pressed and then released " + ke);

			browser.sendKeyEvent(ke);
			Drawing.dirty(this);

			KeyEvent ke3 = new KeyEvent(component, KeyEvent.KEY_RELEASED, 0, fmod, fk, KeyEvent.CHAR_UNDEFINED);
			browser.sendKeyEvent(ke3);

			int k2 = k;

			return (e2, term) -> {

				if (term) {
					Log.log("keyboard", () -> "actual up " + e + " " + fk);
					KeyEvent ke2 = new KeyEvent(component, KeyEvent.KEY_RELEASED, 0, fmod, fk, KeyEvent.CHAR_UNDEFINED);
					browser.sendKeyEvent(ke2);
					e2.properties.put(Window.consumed, true);
					Drawing.dirty(this);
				}
				return !term;
			};
		});

		this.properties.putToMap(Keyboard.onCharTyped, "__browser__", (e, k) -> {


//			if (!isSelected() && !focussed) return;
			if (!getFocus()) return;
			if (properties.isTrue(Box.hidden, false)) return;

			if (true) {
				charTypedAt = System.currentTimeMillis();
				keyboardHacks.onCharDown(e, k);
				return;
			}

//			Log.log("keyboard", "---- CHAR TYPED:" + e + " " + k);


			Integer found = null;

			Iterator<Integer> ii = e.after.keysDown.iterator();
			while (ii.hasNext()) {
				Integer g = ii.next();
				if (mapper.isModifier(g)) continue;
				Integer code = mapper.translateCode(g);
				if (code != null && (!e.before.keysDown.contains(g) || (e.before.charsDown.equals(e.after.charsDown)))) {
					found = code;
					break;
				}
			}

			int mod = (e.after.isAltDown() ? KeyEvent.ALT_DOWN_MASK : 0);
			mod |= (e.after.isShiftDown() ? KeyEvent.SHIFT_DOWN_MASK : 0);
			mod |= (e.after.isControlDown() ? KeyEvent.CTRL_DOWN_MASK : 0);
			mod |= (e.after.isSuperDown() ? KeyEvent.META_DOWN_MASK : 0);

			int fmod = mod;

//			Log.log("keyboard", "mod :" + fmod);
//			Log.log("keyboard", "found translation?:"+found);
			e.properties.put(Window.consumed, true);

			charTypedAt = System.currentTimeMillis();
			if (found == null) {
//				if (mod==0)
//				{
				Log.log("keyboard", () -> "sending char " + k);
				KeyEvent ke = new KeyEvent(component, KeyEvent.KEY_TYPED, 0, mod, KeyEvent.VK_UNDEFINED, k);
				Log.log("keyboard", () -> "awt event is " + ke);
				browser.sendKeyEvent(ke);
//				}
//				else
//				{
//					KeyEvent ke = new KeyEvent(component, KeyEvent.KEY_PRESSED, 0, mod, k, k);
//					browser.sendKeyEvent(ke);
//					ke = new KeyEvent(component, KeyEvent.KEY_RELEASED, 0, mod, k, k);
//					browser.sendKeyEvent(ke);
//				}
			} else {
				Log.log("keyboard", () -> "faking keypress instead because we found a translation");
				KeyEvent ke = new KeyEvent(component, KeyEvent.KEY_PRESSED, 0, mod, found, KeyEvent.CHAR_UNDEFINED);
				browser.sendKeyEvent(ke);
				ke = new KeyEvent(component, KeyEvent.KEY_RELEASED, 0, mod, found, KeyEvent.CHAR_UNDEFINED);
				browser.sendKeyEvent(ke);
			}
//			System.out.print("k");
			Drawing.dirty(this);
		});

		this.properties.putToMap(Boxes.insideRunLoop, "main.__pullFocus__", () -> {

			if (!isSelected() && !getFocus()) {
				getKeyboardFocus().disclaimFocus(this);
			} else if (properties.isTrue(Box.hidden, false)) {
				getKeyboardFocus().disclaimFocus(this);
			} else {

				getKeyboardFocus().claimFocus(this);
			}
			return true;
		});

		this.properties.putToMap(Callbacks.onSelect, "__pullFocus__", (k) -> {
			if (!(k instanceof Browser)) getKeyboardFocus().claimFocus((Box) k);
			return null;
		});

		this.properties.putToMap(Callbacks.onDeselect, "__pullFocus__", (k) -> {
			if (!(k instanceof Browser)) getKeyboardFocus().disclaimFocus((Box) k);
			return null;
		});
	}

	KeyboardFocus cachedFocus = null;

	private KeyboardFocus getKeyboardFocus() {
		return cachedFocus == null ? cachedFocus = find(KeyboardFocus._keyboardFocus, both()).findFirst()
			.get() : cachedFocus;
	}

	private boolean isSelected() {
		return properties.isTrue(Mouse.isSelected, false);
	}

	private boolean intersects(Rect r, Window.Event<Window.MouseState> e) {

		Optional<Drawing> drawing = this.find(Drawing.drawing, both())
			.findFirst();
		Vec2 point = drawing.map(x -> x.windowSystemToDrawingSystem(new Vec2(e.after.x, e.after.y)))
			.orElseThrow(() -> new IllegalArgumentException(" can't mouse around something without drawing support (to provide coordinate system)"));

		return (r.x < point.x && r.x + r.w > point.x) && (r.y < point.y && r.y + r.h > point.y);
	}

	/**
	 * called from some random thread, buffer only good for duration of call. ?
	 */
	protected void paint(boolean popup, Rectangle[] dirty, ByteBuffer buffer, int w, int h) {
//		System.out.println(" started copy to buffer 1 " + (System.currentTimeMillis() - charTypedAt) + "ms ");

//		System.out.println(" paint :" + buffer + " " + w + " " + h);

		sourceView.clear();
		buffer.clear();

		int x0 = w;
		int x1 = 0;
		int y0 = h;
		int y1 = 0;

		for (Rectangle r: dirty) {
//            System.out.println(" -- "+r);

			if (r.x == 0 && r.y == 0 && r.width == w && r.height == h) {
				buffer.clear();
				sourceView.clear();
				sourceView.put(buffer);
				x0 = 0;
				y0 = 0;
				x1 = w;
				y1 = h;
			} else {

				for (int y = r.y; y < r.y + r.height; y++) {
					buffer.limit(r.x * 4 + y * 4 * w + r.width * 4);
					buffer.position(r.x * 4 + y * 4 * w);
					sourceView.limit(r.x * 4 + y * 4 * w + r.width * 4);
					sourceView.position(r.x * 4 + y * 4 * w);
					sourceView.put(buffer);

					sourceView.clear();
					buffer.clear();

//                    for(int x=0;x<w;x++)
//                    {
//                        if (x%5==0)
//                            sourceView.put(y*4*w+x*4+1, (byte) 255);
//                    }


				}
//                System.out.println("r= " + r);
				x0 = Math.min(x0, r.x);
				x1 = Math.max(x1, r.width + r.x);
				y0 = Math.min(y0, r.y);
				y1 = Math.max(y1, r.height + r.y);
			}
		}


		sourceView.clear();
		buffer.clear();


		// threading ?
		Drawing.dirty(Browser.this);
		root.properties.put(Drawing.needRepaint, true);
		window.requestRepaint();
		RunLoop.main.shouldSleep.add(Browser.this);

		if (damage == null) damage = new Rect(x0, y0, x1 - x0, y1 - y0);
		else damage = damage.union(new Rect(x0, y0, x1 - x0, y1 - y0));

		this.dirty.set(true);


//		System.out.println(" finished copy to buffer 1 " + (System.currentTimeMillis() - charTypedAt) + "ms ");
	}

	protected void message(long id, String message, Consumer<String> reply) {
		synchronized (messages) {
			messages.add(new Pair<>(message, reply));
		}
	}

	public void addHandler(Predicate<String> s, Handler h) {
		handlers.add(new Pair<>(s, h));
	}

	public void clearHandlers() {
		handlers.clear();
	}

	public void executeJavaScript(String s) {
		browser.executeJavaScript(s, "", 0);
	}

	public void executeJavaScript_queued(String s) {
		if (ignore) return;

		if (booted) {
			executeJavaScript(s);
		} else {
			bootQueue.add(() -> executeJavaScript(s));
		}
	}

	public void pauseForBoot() {
		booted = false;
	}

	public void pauseNow() {
		paused = true;
	}

	public void unpauseNow() {
		paused = false;
		ignore = false;
	}

	public void pauseAndIgnoreNow() {
		paused = true;
		ignore = true;
	}

	public void finishBooting() {
		booted = true;
		List<Runnable> bq = new ArrayList<>(bootQueue);
		bootQueue.clear();
		bq.forEach(x -> x.run());
	}

	static public float baseZoomLevel = Options.dict().getFloat(new Dict.Prop<Number>("baseZoomLevel"), 2);

	static public void setBaseZoomLevel(float z) {
		baseZoomLevel = z;
	}

	float lastZoom = baseZoomLevel;

	protected void update(float x, float y, float scale) {

//		System.out.println(" inside update for browser ");


		if (this.dirty.getAndSet(false) && damage != null) {
			if (check-- > 0 || lastZoom != baseZoomLevel) {
				if (Main.os != Main.OS.windows)
					browser.setZoomLevel(baseZoomLevel * window.getRetinaScaleFactor());
				else
					browser.setZoomLevel(baseZoomLevel * 2);
				lastZoom = baseZoomLevel;
			}
			Log.log("cef.debug", () -> " texture was dirty, uploading ");

//			if (GraphicsContext.getContext() != null)
//				texture.forceUploadNow(source);
//			else
			texture.upload(source, false, (int) damage.x, (int) damage.y, (int) (damage.w + damage.x), (int) (1 + damage.h + damage.y));

//			System.out.print("<P1>");
			Drawing.dirty(this);
			again = 1;
			hasRepainted = true;
			RunLoop.main.shouldSleep.add(this);
		} else if (again > 0 && damage != null) {
			Log.log("cef.debug", () -> " texture was dirty " + again + " call, uploading ");
//			if (GraphicsContext.getContext() != null)
//				texture.forceUploadNow(source);
//			else
			texture.upload(source, false, (int) damage.x, (int) damage.y, (int) (damage.w + damage.x), (int) (1 + damage.h + damage.y));

//			System.out.print("<p2>");
			Drawing.dirty(this);
			RunLoop.main.shouldSleep.add(this);
			again--;
		} else if (again == 0) {
			damage = null;
			RunLoop.main.shouldSleep.remove(this);
		}


//		if (hasRepainted)
		{
			navigation.apply(this);
			geometry.apply(this);
			direct.apply(this);
		}


		ArrayList<Pair<String, Consumer<String>>> m;
		synchronized (messages) {
			m = new ArrayList<>(messages);
			messages.clear();
		}

		for (Pair<String, Consumer<String>> p: m) {
			Log.log("cef.debug", () -> "dispatching message <" + p.first + ">");

			JSONObject o = new JSONObject(p.first);
			String address = o.getString("address");

			Object payload = o.get("payload");

			if (!(payload instanceof JSONObject)) {
				StringWriter sw = new StringWriter();
				JSONWriter w = new JSONWriter(sw);
				w.object();
				w.key("message");
				w.value(payload + "");
				w.endObject();
				payload = new JSONObject(sw.toString());
			}

			if (!TransientCommands.transientCommands.handle(address, (JSONObject) payload, p.second))
				for (Pair<Predicate<String>, Handler> p2: handlers) {
					if (p2.first.test(address)) {
						Log.log("cef.debug", () -> "found handler");

						try {
							p2.second.handle(address, (JSONObject) payload, p.second);
						} catch (Throwable t) {
							t.printStackTrace();
						}
					}
				}

		}

	}

	Out out_cached = null;


	public void printHTML(String text) {
		executeJavaScript_queued("$(document.body).append('" + TextUtils.quoteNoOuter(text.replace("'", "\"")) + "');" + scrollDown());
	}

	public void print(Object text) {

		getOutCached();
		String texts = "";

		if (out_cached != null) {
			texts = out_cached.convert(text);
		} else {
			texts = text + "";
		}

		executeJavaScript_queued("$(document.body).append('" + TextUtils.quoteNoOuter(texts.replace("'", "\"")) + "');" + scrollDown());
	}

	public void println(Object text) {

		getOutCached();
		String texts = "";

		if (out_cached != null) {
			texts = out_cached.convert(text);
		} else {
			texts = text + "";
		}

		executeJavaScript_queued("$(document.body).append('" + TextUtils.quoteNoOuter(texts.replace("'", "\"")) + "<br>');" + scrollDown());
	}

	public void clearAndPrint(Object text) {
		clear();
		print(text);
	}


	public void printText(String text) {
		executeJavaScript_queued("$(document.body).append('<pre style=\"padding:3px;margin:3px;\">" + TextUtils.quoteNoOuter(text.replace("'", "\"")) + "</pre>');" + scrollDown());
	}

	public void clear() {
		executeJavaScript_queued("document.body.innerHTML=''");
	}

	private String scrollDown() {
		return "document.body.scrollTop=document.body.scrollHeight";
	}

	public boolean getFocus() {
		return getKeyboardFocus().isFocused(this);
	}

	public void setFocus(boolean f) {
		if (f != getFocus()) {
//			System.out.print("<");
			Drawing.dirty(this);
		}

		browser.setFocus(f);
		if (f) {
			getKeyboardFocus().claimFocus(this);
		} else {
			getKeyboardFocus().disclaimFocus(this);

		}
	}

	public void injectCSS(String css) {
		executeJavaScript_queued("var css = document.createElement(\"style\");\n" +
			"css.type = \"text/css\";\n" +
			"css.innerHTML = \"" + css + "\";\n" +
			"document.body.appendChild(css);");
	}

	public Out getOutCached() {
		if (out_cached != null) return out_cached;

		out_cached = find(Out.__out, both()).findAny()
			.orElseGet(() -> null);

		return out_cached;
	}

	public interface Handler {
		void handle(String address, JSONObject payload, Consumer<String> reply);
	}


	public void reload() {
		browser.reload();
	}

}
