package fieldcef.browser;

import field.graphics.*;
import field.graphics.Window;
import field.graphics.util.KeyEventMapping;
import field.linalg.Vec2;
import field.linalg.Vec4;
import field.utility.*;
import fieldbox.boxes.*;
import fieldbox.io.IO;
import fieldbox.ui.FieldBoxWindow;
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
 * The first look at embedded HTML. A Browser is a Field/Graphics texture map, a CEF browser of a fixed size, a quad for drawing it, a shader for
 * drawing the quad, and some logic for generating and transforming events. Chrome in a box.
 * <p>
 * TODO: deletion?
 */
public class Browser extends Box implements IO.Loaded {

	static public final Dict.Prop<String> url = new Dict.Prop<String>("url").type()
										.toCannon()
										.doc("URL for the browser. Setting this will cause the browser to nagivate, and repaint automatically");
	static public final Dict.Prop<String> html = new Dict.Prop<String>("html").type()
										  .toCannon()
										  .doc("HTML for the browser. Setting this will cause the browser to reload it's contents from this string, and repaint automatically");

	private int w;
	private int h;
	private ByteBuffer source;
	private Texture texture;
	private BaseMesh q;
	private MeshBuilder builder;
	private Shader shader;
	public CefRendererBrowserBuffer browser;
	private ByteBuffer sourceView;
	private FieldBoxWindow window;
	private Drawing drawing;
	private Box root;

	private final KeyEventMapping mapper = new KeyEventMapping();

	boolean dragOngoing = false;

	public Browser() {
	}

	public void loaded() {
		Log.disable("cef.*");

		this.properties.computeIfAbsent(Box.frame, (k) -> new Rect(0, 0, 512, 512));
		this.properties.put(Box.name, "(browser)");

		this.w = (int) this.properties.get(Box.frame).w;
		this.h = (int) this.properties.get(Box.frame).h;

		root = this.find(Boxes.root, both())
			   .findFirst()
			   .orElseThrow(() -> new IllegalArgumentException(" did you call loaded without adding to the graph?"));

		window = root.first(Boxes.window)
			     .orElseThrow(() -> new IllegalArgumentException(" can't draw a box hierarchy with no window to draw it in !"));
		drawing = root.first(Drawing.drawing)
			      .orElseThrow(() -> new IllegalArgumentException(" can't install textdrawing into something without drawing support"));

		this.root = root;

		browser = CefSystem.cefSystem.makeBrowser(w, h, this::paint, this::message);

		source = ByteBuffer.allocateDirect(w * h * 4);
		source.position(0)
		      .limit(source.capacity());
		sourceView = source.slice();
		texture = new Texture(Texture.TextureSpecification.byte4(0, w, h, source, true));

		q = BaseMesh.triangleList(0, 0);
		builder = new MeshBuilder(q);

		shader = new Shader();
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


		// smoothing needs to be around 1 for font scales of 4 and 0.02 for font scales of 0.2

		shader.addSource(Shader.Type.fragment, "#version 410\n" +
			    "layout(location=0) out vec4 _output;\n" +
			    "in vec4 vtc;\n" +
			    "in vec4 col;\n" +
			    "uniform sampler2D te;\n" +
			    "\n" +
			    "void main()\n" +
			    "{\n" +
			    "\tvec4 current = texelFetch(te, ivec2(vtc.xy*textureSize(te,0)), 0);\n" +
			    "\t_output  = vec4(current.zyx,current.w*vtc.z);\n" +
			    "\t if (vtc.x==0 || vtc.x==1 || vtc.y==0 || vtc.y==1) _output.w=0;\n"+
			    "\n" +
			    "}");

		shader.attach(new Uniform<Vec2>("translation", () -> drawing.getTranslationRounded()));
		shader.attach(new Uniform<Vec2>("scale", () -> drawing.getScale()));
		shader.attach(new Uniform<Vec2>("bounds", () -> new Vec2(Window.getCurrentWidth(), Window.getCurrentHeight())));

		shader.attach(-2, x -> {
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
			Rect rect = box.properties.get(frame);
			if (rect == null) return null;

			boolean selected = box.properties.isTrue(Mouse.isSelected, false) || focussed;

			FLine f = new FLine();
			if (selected) rect = rect.inset(-8f);
			else rect = rect.inset(-0.5f);

			f.moveTo(rect.x, rect.y);
			f.lineTo(rect.x + rect.w, rect.y);
			f.lineTo(rect.x + rect.w, rect.y + rect.h);
			f.lineTo(rect.x, rect.y + rect.h);
			f.lineTo(rect.x, rect.y);

			f.attributes.put(color, selected ? new Vec4(0, 0, 0, -1.0f) : new Vec4(0, 0, 0, 0.15f));
			f.attributes.put(filled, false);
			f.attributes.put(thicken, new BasicStroke(selected ? 16 : 1.5f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));

			f.attributes.put(stroked, true);

			return f;
		}, (box) -> new Pair(box.properties.get(frame), box.properties.isTrue(Mouse.isSelected, false) || focussed)));

		// AWT's MouseEvent constructor throws an NPE unless you give it a component.
		Component component = new Component() {
			@Override
			public Point getLocationOnScreen() {
				return new Point(0, 0);
			}
		};

		this.properties.putToList(Mouse.onMouseDown, (e, button) -> {

			Rect r = properties.get(Box.frame);

			if (!intersects(r, e)) return null;

			if (properties.isTrue(Box.hidden, false)) return null;

			Optional<Drawing> drawing = this.find(Drawing.drawing, both())
							.findFirst();
			Vec2 point = drawing.map(x -> x.windowSystemToDrawingSystem(new Vec2(e.after.x, e.after.y)))
					    .orElseThrow(() -> new IllegalArgumentException(" can't mouse around something without drawing support (to provide coordinate system)"));


			if (isSelected())
				e.properties.put(Window.consumed, true);
			else
			{
//				setFocus(true);
			}

			browser.sendMouseEvent(new MouseEvent(component, MouseEvent.MOUSE_PRESSED, 0, MouseEvent.getMaskForButton(button + 1), (int) (point.x - r.x), (int) (point.y - r.y), 1, false, button + 1));

			dragOngoing = true;

			return (e2, term) -> {

				Vec2 point2 = drawing.map(x -> x.windowSystemToDrawingSystem(new Vec2(e2.after.x, e2.after.y)))
						    .orElseThrow(() -> new IllegalArgumentException(" can't mouse around something without drawing support (to provide coordinate system)"));

				System.out.println(" dragging .... "+point2+" "+term);

				if (isSelected())
					e2.properties.put(Window.consumed, true);

				if (!term) {
					browser.sendMouseEvent(new MouseEvent(component, MouseEvent.MOUSE_DRAGGED, 0, MouseEvent.getMaskForButton(button + 1), (int) (point2.x - r.x), (int) (point2.y - r.y), 1, false, button + 1));
				}
				else
					browser.sendMouseEvent(new MouseEvent(component, MouseEvent.MOUSE_RELEASED, 0, MouseEvent.getMaskForButton(button + 1), (int) (point2.x - r.x), (int) (point2.y - r.y), 1, false, button + 1));

				dragOngoing = !term;

				return !term;
			};
		});

		this.properties.putToList(Mouse.onMouseMove, (e) -> {

			if (dragOngoing) return null;

			Rect r = properties.get(Box.frame);
			if (!intersects(r, e)) return null;
			if (properties.isTrue(Box.hidden, false)) return null;

			if (isSelected() || focussed) ;
				e.properties.put(Window.consumed, true);

			Optional<Drawing> drawing = this.find(Drawing.drawing, both())
							.findFirst();
			Vec2 point = drawing.map(x -> x.windowSystemToDrawingSystem(new Vec2(e.after.x, e.after.y)))
					    .orElseThrow(() -> new IllegalArgumentException(" can't mouse around something without drawing support (to provide coordinate system)"));


			browser.sendMouseEvent(new MouseEvent(component, MouseEvent.MOUSE_MOVED, 0, 0, (int) (point.x - r.x), (int) (point.y - r.y), 0, false));
			return null;
		});

		this.properties.putToList(Mouse.onMouseScroll, (e) -> {

			Log.log("mouse", "scroll :"+e);

			Rect r = properties.get(Box.frame);
			if (!intersects(r, e)) return ;
			if (!isSelected() && !focussed) return ;
			if (properties.isTrue(Box.hidden, false)) return ;

			Log.log("mouse", "scroll going "+e);

			e.properties.put(Window.consumed, true);

			Optional<Drawing> drawing = this.find(Drawing.drawing, both())
							.findFirst();
			Vec2 point = drawing.map(x -> x.windowSystemToDrawingSystem(new Vec2(e.after.x, e.after.y)))
					    .orElseThrow(() -> new IllegalArgumentException(" can't mouse around something without drawing support (to provide coordinate system)"));


			Log.log("mouse", "scroll sending "+point);
			float dy = e.after.dwheely*8;
			browser.sendMouseWheelEvent(new MouseWheelEvent(component, MouseWheelEvent.MOUSE_WHEEL, 0, 0, (int) (point.x - r.x), (int) (point.y - r.y), (int) (point.x - r.x), (int) (point.y - r.y),  0, false, MouseWheelEvent.WHEEL_UNIT_SCROLL, 1, (int)dy, dy));
			float dx = e.after.dwheel*8;
			browser.sendMouseWheelEvent(new MouseWheelEvent(component, MouseWheelEvent.MOUSE_WHEEL, KeyEvent.SHIFT_DOWN_MASK,0, (int) (point.x - r.x), (int) (point.y - r.y), (int) (point.x - r.x), (int) (point.y - r.y), 0, false, MouseWheelEvent.WHEEL_UNIT_SCROLL, 1, (int)dx, dx));
		});

		this.properties.putToList(Keyboard.onKeyDown, (e, k) -> {

			if (!isSelected() && !focussed) return null;
			if (properties.isTrue(Box.hidden, false)) return null;

			Log.log("keyboard", "Key down :" + e + " " + k);

			int mod = (e.after.isAltDown() ? KeyEvent.ALT_DOWN_MASK : 0);
			mod |= (e.after.isShiftDown() ? KeyEvent.SHIFT_DOWN_MASK : 0);
			mod |= (e.after.isControlDown() ? KeyEvent.CTRL_DOWN_MASK : 0);
			mod |= (e.after.isSuperDown() ? KeyEvent.META_DOWN_MASK : 0);

			int fmod = mod;

			HashSet<Character> c = new HashSet<Character>(e.after.charsDown.values());
			c.removeAll(e.before.charsDown.values());

			Log.log("keyboard", "char ? " + c);


			Integer translated = mapper.translateCode(k);
			if (translated != null) {
				k = translated;
			}
			else {
				Log.log("keyboard", "skipping, assuming it will be a char");
			}

				int fk = k;

			KeyEvent ke = new KeyEvent(component, KeyEvent.KEY_PRESSED, 0, mod, fk, (char)fk);

			Log.log("consumption", "consuming keyboard event :"+e);
			e.properties.put(Window.consumed, true);

			browser.sendKeyEvent(ke);
			Drawing.dirty(this);

			KeyEvent ke3 = new KeyEvent(component, KeyEvent.KEY_RELEASED, 0, fmod, fk, KeyEvent.CHAR_UNDEFINED);
			browser.sendKeyEvent(ke3);

			return (e2, term) -> {

				if (term) {
					Log.log("keyboard", "up " + e + " " + fk);
					KeyEvent ke2 = new KeyEvent(component, KeyEvent.KEY_RELEASED, 0, fmod, fk, KeyEvent.CHAR_UNDEFINED);
					browser.sendKeyEvent(ke2);
					e2.properties.put(Window.consumed, true);
					Drawing.dirty(this);
				}
				return !term;
			};
		});

		this.properties.putToList(Keyboard.onCharTyped, (e, k) -> {
			if (!isSelected() && !focussed) return ;
			if (properties.isTrue(Box.hidden, false)) return ;

			Log.log("keyboard", "char typed:" + e + " " + k);


			Integer found = null;

			Iterator<Integer> ii = e.after.keysDown.iterator();
			while(ii.hasNext())
			{
				Integer g = ii.next();
				if (mapper.isModifier(g)) continue;
				Integer code = mapper.translateCode(g);
				if (code!=null && (!e.before.keysDown.contains(g) || (e.before.keysDown.equals(e.after.keysDown))))
				{
					found = code;
					break;
				}
			}

			int mod = (e.after.isAltDown() ? KeyEvent.ALT_DOWN_MASK : 0);
			mod |= (e.after.isShiftDown() ? KeyEvent.SHIFT_DOWN_MASK : 0);
			mod |= (e.after.isControlDown() ? KeyEvent.CTRL_DOWN_MASK : 0);
			mod |= (e.after.isSuperDown() ? KeyEvent.META_DOWN_MASK : 0);

			int fmod = mod;

			Log.log("keyboard", "mod :" + fmod);
			Log.log("keyboard", "found translation?:"+found);
			e.properties.put(Window.consumed, true);

			if (found==null) {
//				if (mod==0)
//				{
				Log.log("keyboard", "sending char "+k);
					KeyEvent ke = new KeyEvent(component, KeyEvent.KEY_TYPED, 0, mod, KeyEvent.VK_UNDEFINED, k);
					browser.sendKeyEvent(ke);
//				}
//				else
//				{
//					KeyEvent ke = new KeyEvent(component, KeyEvent.KEY_PRESSED, 0, mod, k, k);
//					browser.sendKeyEvent(ke);
//					ke = new KeyEvent(component, KeyEvent.KEY_RELEASED, 0, mod, k, k);
//					browser.sendKeyEvent(ke);
//				}
			}
			else
			{
				Log.log("keyboard", "faking keypress");
				KeyEvent ke = new KeyEvent(component, KeyEvent.KEY_PRESSED, 0, mod, found, KeyEvent.CHAR_UNDEFINED);
				browser.sendKeyEvent(ke);
				ke = new KeyEvent(component, KeyEvent.KEY_RELEASED, 0, mod, found, KeyEvent.CHAR_UNDEFINED);
				browser.sendKeyEvent(ke);
			}
			Drawing.dirty(this);
		});
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


	protected AtomicBoolean dirty = new AtomicBoolean(false);

	/**
	 * called from some random thread, buffer only good for duration of call. ?
	 */
	protected void paint(boolean popup, Rectangle[] dirty, ByteBuffer buffer, int w, int h) {
		sourceView.clear();
		buffer.clear();


		for (Rectangle r : dirty) {
			for (int y = r.y; y < r.y + r.height; y++) {
				buffer.limit(r.x * 4 + y * 4 * w + r.width * 4);
				buffer.position(r.x * 4 + y * 4 * w);
				sourceView.limit(r.x * 4 + y * 4 * w + r.width * 4);
				sourceView.position(r.x * 4 + y * 4 * w);
				sourceView.put(buffer);
			}
		}


		sourceView.clear();
		buffer.clear();

		this.dirty.set(true);

		// threading ?
		root.properties.put(Drawing.needRepaint, true);
		window.requestRepaint();

	}

	public interface Handler
	{
		public void handle(String address, JSONObject payload, Consumer<String> reply);
	}

	List<Pair<Predicate<String>, Handler>> handlers = new ArrayList<>();

	Deque<Pair<String, Consumer<String>>> messages = new ArrayDeque<>();

	protected void message(long id, String message, Consumer<String> reply) {
		synchronized (messages) {
			messages.add(new Pair<>(message, reply));
		}
	}

	public void addHandler(Predicate<String> s, Handler h)
	{
		handlers.add(new Pair<>(s, h));
	}

	public void clearHandlers()
	{

	}


	public void executeJavaScript(String s) {
		browser.executeJavaScript(s, "", 0);
	}

	Cached<Box, Object, Void> geometry = new Cached<>((now, nothing) -> {

		Rect r = now.properties.get(Box.frame);
		float op = now.properties.getFloat(StandardFLineDrawing.opacity, 1);
		if (now.properties.isTrue(FLineDrawing.hidden, false)) {
			builder.open();
			builder.close();
			return null;
		}

		builder.open();
		builder.aux(5, 0, 0, op);
		builder.nextVertex(r.x, r.y, 0);
		builder.aux(5, r.w / w, 0, op);
		builder.nextVertex(r.x + r.w * 1, r.y, 0);
		builder.aux(5, r.w / w, r.h / h, op);
		builder.nextVertex(r.x + r.w * 1, r.y + r.h * 1, 0);
		builder.aux(5, 0, r.h / h, op);
		builder.nextVertex(r.x, r.y + r.h * 1, 0);
		builder.nextElement_quad(3, 2, 1, 0);
		builder.close();

		return null;
	}, (box) -> new Triple<>(box.properties.getFloat(StandardFLineDrawing.opacity, 1), box.properties.get(Box.frame), box.properties.isTrue(FLineDrawing.hidden, false)));

	Cached<Box, String, Void> navigation = new Cached<>((now, nothing) -> {
		String u = now.properties.get(url);

		if (u != null) {
			Log.log("HTML", "loading URL <" + u + ">");
			browser.loadURL(u);
		}

		return null;
	}, (box) -> box.properties.get(url));

	Cached<Box, String, Void> direct = new Cached<>((now, nothing) -> {
		String u = now.properties.get(html);

		if (u != null) {
			Log.log("HTML", "loading html");
			browser.loadString(u, u);
		}

		return null;
	}, (box) -> box.properties.get(html));


	int again = 0;

	boolean hasRepainted = false;

	protected void update(float x, float y, float scale) {

		if (this.dirty.getAndSet(false)) {
			Log.log("cef.debug", " texture was dirty, uploading ");
			texture.upload(source, true);
			Drawing.dirty(this);
			again = 4;
			hasRepainted = true;
		} else if (again>0) {
			Log.log("cef.debug", " texture was dirty "+again+" call, uploading ");
			texture.upload(source, true);
			Drawing.dirty(this);
			again --;
		}

//		if (hasRepainted)
		{
			navigation.apply(this);
			geometry.apply(this);
			direct.apply(this);
		}

		ArrayList<Pair<String, Consumer<String>>> m;
		synchronized (messages)
		{
			m = new ArrayList<>(messages);
			messages.clear();
		}

		for(Pair<String, Consumer<String>> p : m)
		{
			Log.log("cef.debug", "dispatching message <" + p.first+ ">");

			JSONObject o = new JSONObject(p.first);
			String address = o.getString("address");

			Object payload = o.get("payload");

			if (!(payload instanceof JSONObject))
			{
				StringWriter sw = new StringWriter();
				JSONWriter w = new JSONWriter(sw);
				w.object();
				w.key("message");
				w.value(payload + "");
				w.endObject();
				payload = new JSONObject(sw.toString());
			}

			for (Pair<Predicate<String>, Handler> p2 : handlers) {
				if (p2.first.test(address)) {
					Log.log("cef.debug", "found handler");
					p2.second.handle(address, (JSONObject)payload, p.second);
				}
			}

		}

	}

	boolean focussed = false;

	public void setFocus(boolean f)
	{
		if (f!=focussed) Drawing.dirty(this);
		focussed = f;
		browser.setFocus(f);
	}


}
