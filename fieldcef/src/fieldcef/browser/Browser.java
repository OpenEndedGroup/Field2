package fieldcef.browser;

import field.graphics.*;
import field.graphics.Window;
import field.linalg.Vec2;
import field.linalg.Vec4;
import field.utility.*;
import fieldbox.boxes.*;
import fieldbox.io.IO;
import fieldbox.ui.FieldBoxWindow;
import org.cef.browser.CefRendererBrowserBuffer;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static fieldbox.boxes.StandardFLineDrawing.*;

/**
 * The first look at embedded HTML. A Browser is a Field/Graphics texture map, a CEF browser of a fixed size, a quad for drawing it, a shader for
 * drawing the quad, and some logic for generating and transforming events. Chrome in a box.
 * <p>
 * TODO: make layer aware? TODO: deletion?
 */
public class Browser extends Box implements IO.Loaded {

	static public final Dict.Prop<String> url = new Dict.Prop<String>("url").type().toCannon().doc("URL for the browser. Setting this will cause the browser to nagivate, and repaint automatically");
	static public final Dict.Prop<String> html = new Dict.Prop<String>("html").type().toCannon().doc("HTML for the browser. Setting this will cause the browser to reload it's contents from this string, and repaint automatically");

	private int w;
	private int h;
	private ByteBuffer source;
	private Texture texture;
	private BaseMesh q;
	private MeshBuilder builder;
	private Shader shader;
	private CefRendererBrowserBuffer browser;
	private ByteBuffer sourceView;
	private FieldBoxWindow window;
	private Drawing drawing;
	private Box root;

	private final KeyEventMapping mapper = new KeyEventMapping();

	public Browser() {
	}

	public void loaded()
	{
		Log.disable("cef.*");

		this.properties.computeIfAbsent(Box.frame, (k) -> new Rect(0, 0, 512, 512));
		this.properties.put(Box.name, "(browser)");

		this.w = (int)this.properties.get(Box.frame).w;
		this.h = (int)this.properties.get(Box.frame).h;

		root = this.find(Boxes.root, both()).findFirst().orElseThrow(() -> new IllegalArgumentException(" did you call loaded without adding to the graph?"));

		window = root.first(Boxes.window)
			    .orElseThrow(() -> new IllegalArgumentException(" can't draw a box hierarchy with no window to draw it in !"));
		drawing = root.first(Drawing.drawing)
			    .orElseThrow(() -> new IllegalArgumentException(" can't install textdrawing into something without drawing support"));

		this.root = root;

		browser = CefSystem.cefSystem.makeBrowser(w, h, this::paint, this::message);

		source = ByteBuffer.allocateDirect(w * h * 4);
		source.position(0).limit(source.capacity());
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
			    "\tvec4 current = texture(te, vtc.xy,0);\n" +
			    "\t_output  = vec4(current.zyx,current.w*vtc.z);\n" +
			    "\n" +
			    "}");

		shader.connect(new Uniform<Vec2>("translation", () -> drawing.getTranslation()));
		shader.connect(new Uniform<Vec2>("scale", () -> drawing.getScale()));
		shader.connect(new Uniform<Vec2>("bounds", () -> new Vec2(Window.getCurrentWidth(), Window.getCurrentHeight())));

		shader.connect(q);
		shader.connect(texture);

		window.getCompositor().getLayer(properties.computeIfAbsent(FLineDrawing.layer, k -> "__main__")).getScene().connect(shader);

		this.properties.putToMap(Boxes.insideRunLoop, "main.__updateSize__", () -> {
			Rect r = properties.get(Box.frame);
			update(r.x, r.y, 1/*r.w/w*/);
			return true;
		});

		properties.putToMap(FLineDrawing.frameDrawing, "__outline__", new Cached<Box, Object, FLine>((box, previously) -> {
			Rect rect = box.properties.get(frame);
			if (rect == null) return null;

			boolean selected = box.properties.isTrue(Mouse.isSelected, false);

			FLine f = new FLine();
			if (selected) rect = rect.inset(-8f);
			else rect = rect.inset(-0.5f);

			f.moveTo(rect.x, rect.y);
			f.lineTo(rect.x + rect.w, rect.y);
			f.lineTo(rect.x + rect.w, rect.y + rect.h);
			f.lineTo(rect.x, rect.y + rect.h);
			f.lineTo(rect.x, rect.y);

			f.attributes.put(strokeColor, selected ? new Vec4(0, 0, 0, -1.0f) : new Vec4(0, 0, 0, 0.5f));

			f.attributes.put(thicken, new BasicStroke(selected ? 16 : 1.5f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));

			f.attributes.put(stroked, true);

			return f;
		}, (box) -> new Pair(box.properties.get(frame), box.properties.get(Mouse.isSelected))));

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

			e.properties.put(Window.consumed, true);

			browser.sendMouseEvent(new MouseEvent(component, MouseEvent.MOUSE_PRESSED, 0, MouseEvent
				    .getMaskForButton(button + 1), (int) (e.after.x - r.x), (int) (e.after.y - r.y), 1, false, button + 1));

			return (e2, term) -> {
				e2.properties.put(Window.consumed, true);
				if (!term) browser.sendMouseEvent(new MouseEvent(component, MouseEvent.MOUSE_DRAGGED, 0, MouseEvent
					    .getMaskForButton(button + 1), (int) (e.after.x - r.x), (int) (e.after.y - r.y), 1, false, button + 1));
				else browser.sendMouseEvent(new MouseEvent(component, MouseEvent.MOUSE_RELEASED, 0, MouseEvent
					    .getMaskForButton(button + 1), (int) (e.after.x - r.x), (int) (e.after.y - r.y), 1, false, button + 1));
				return !term;
			};
		});

		this.properties.putToList(Mouse.onMouseMove, (e) -> {

			Rect r = properties.get(Box.frame);
			if (!intersects(r, e)) return null;
			if (properties.isTrue(Box.hidden, false)) return null;

			e.properties.put(Window.consumed, true);

			browser.sendMouseEvent(new MouseEvent(component, MouseEvent.MOUSE_MOVED, 0, 0, (int) (e.after.x - r.x), (int) (e.after.y - r.y), 0, false));
			return null;
		});

		this.properties.putToList(Keyboard.onKeyDown, (e, k) -> {

			if (!isSelected()) return null;
			if (properties.isTrue(Box.hidden, false)) return null;

			Log.log("keyboard", "Key down :"+e+" "+k);

			int mod = (e.after.isAltDown() ? KeyEvent.ALT_DOWN_MASK : 0);
			mod |=(e.after.isShiftDown() ? KeyEvent.SHIFT_DOWN_MASK: 0);
			mod |=(e.after.isControlDown() ? KeyEvent.CTRL_DOWN_MASK: 0);
			mod |=(e.after.isSuperDown() ? KeyEvent.META_DOWN_MASK: 0);

			int fmod = mod;

			HashSet<Character> c = new HashSet<Character>(e.after.charsDown.values());
			c.removeAll(e.before.charsDown.values());

			Log.log("keyboard", "char ? "+c);


			Integer translated = mapper.translateCode(k);
			if (translated!=null)
				k = translated;

			int fk = k;

			KeyEvent ke = new KeyEvent(component, KeyEvent.KEY_PRESSED, 0, mod, fk, (char)fk);

			e.properties.put(Window.consumed, true);

			browser.sendKeyEvent(ke);
			Drawing.dirty(this);

			return (e2, term) -> {

				if (term)
				{
					Log.log("keyboard", "up "+e+" "+fk);
					KeyEvent ke2 = new KeyEvent(component, KeyEvent.KEY_RELEASED, 0, fmod, fk, (char)fk);
					browser.sendKeyEvent(ke2);
					e2.properties.put(Window.consumed, true);
					Drawing.dirty(this);
				}
				return !term;
			};
		});

		this.properties.putToList(Keyboard.onCharTyped, (e, k) -> {
			if (!isSelected()) return;
			if (properties.isTrue(Box.hidden, false)) return;

			Log.log("keyboard", "char typed:"+e+" "+k);

			int mod = (e.after.isAltDown() ? KeyEvent.ALT_DOWN_MASK : 0);
			mod |=(e.after.isShiftDown() ? KeyEvent.SHIFT_DOWN_MASK: 0);
			mod |=(e.after.isControlDown() ? KeyEvent.CTRL_DOWN_MASK: 0);
			mod |=(e.after.isSuperDown() ? KeyEvent.META_DOWN_MASK: 0);

			int fmod = mod;

			Log.log("keyboard", "mod :"+fmod);
			e.properties.put(Window.consumed, true);

			KeyEvent ke = new KeyEvent(component, KeyEvent.KEY_TYPED, 0, mod, KeyEvent.VK_UNDEFINED, k);

			browser.sendKeyEvent(ke);
			Drawing.dirty(this);
		});
	}

	private boolean isSelected() {
		return properties.isTrue(Mouse.isSelected, false);
	}

	private boolean intersects(Rect r, Window.Event<Window.MouseState> e) {
		return (r.x < e.after.x && r.x + r.w > e.after.x) && (r.y < e.after.y && r.y + r.h > e.after.y);
	}


	protected AtomicBoolean dirty = new AtomicBoolean(false);

	/**
	 * called from some random thread, buffer only good for duration of call.
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

		//sourceView.put(buffer);


		/*
		for (Rectangle r : dirty) {
			if (r.width<20 && r.height<20)
			{
				System.out.println(" dumping rect :"+r);
				for (int y = r.y; y < r.y + r.height; y++) {
					for(int x=r.x;x < r.x + r.width;x++)
					{
						boolean rc = (buffer.get(x * 4 + y * 4 * w + 0) & 0xff)>128;
						boolean g = (buffer.get(x * 4 + y * 4 * w + 1)& 0xff)>128;
						boolean b = (buffer.get(x * 4 + y * 4 * w + 2)& 0xff)>128;
						boolean a = (buffer.get(x * 4 + y * 4 * w + 3)& 0xff)>128;

						System.out.print((rc ? "@" : ".")+(g ? "@" : ".")+(b ? "@" : ".")+(a ? "@" : "."));
					}
					System.out.println(";");
				}
			}
		}
*/

		sourceView.clear();
		buffer.clear();

		this.dirty.set(true);

		// threading ?
		root.properties.put(Drawing.needRepaint, true);
		window.requestRepaint();

	}

	protected void message(long id, String message, Consumer<String> reply)
	{
		// here we need to define some functionality.

		// in particular set and get properties on this object

	}

	Cached<Box, Object, Void> geometry = new Cached<>((now, nothing) -> {

		Rect r = now.properties.get(Box.frame);
		float op = now.properties.getFloat(StandardFLineDrawing.opacity,1);
		if (now.properties.isTrue(FLineDrawing.hidden, false)){
			builder.open();
			builder.close();
			return null;
		}

		builder.open();
		builder.aux(5, 0, 0, op);
		builder.nextVertex(r.x, r.y, 0);
		builder.aux(5, r.w/w, 0, op);
		builder.nextVertex(r.x + r.w * 1, r.y, 0);
		builder.aux(5, r.w/w, r.h/h, op);
		builder.nextVertex(r.x + r.w * 1, r.y + r.h * 1, 0);
		builder.aux(5, 0, r.h/h, op);
		builder.nextVertex(r.x, r.y + r.h * 1, 0);
		builder.nextElement_quad(3, 2, 1, 0);
		builder.close();

		return null;
	}, (box) -> new Triple<>(box.properties.getFloat(StandardFLineDrawing.opacity, 1), box.properties.get(Box.frame), box.properties.isTrue(FLineDrawing.hidden, false)));

	Cached<Box, String, Void> navigation = new Cached<>((now, nothing) -> {
		String u = now.properties.get(url);

		if (u!=null)
			browser.loadURL(u);

		return null;
	}, (box) -> box.properties.get(url));

	Cached<Box, String, Void> direct = new Cached<>((now, nothing) -> {
		String u = now.properties.get(html);

		if (u!=null) {
			Log.log("HTML", "loading <"+u+">");
			browser.loadString(u, "http://localhost");
		}

		return null;
	}, (box) -> box.properties.get(html));


	boolean again = false;

	protected void update(float x, float y, float scale) {

		if (this.dirty.getAndSet(false)) {
			Log.log("cef.debug", " texture was dirty, uploading ");
			texture.upload(source, true);
			Drawing.dirty(this);
			again = true;
		}
		else if (again)
		{
			texture.upload(source, true);
			Drawing.dirty(this);
			again = false;
		}

		navigation.apply(this);
		geometry.apply(this);
		direct.apply(this);
	}

}
