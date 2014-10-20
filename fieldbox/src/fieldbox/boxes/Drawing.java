package fieldbox.boxes;

import field.graphics.*;
import field.linalg.Vec2;
import field.linalg.Vec4;
import field.utility.Dict;
import field.utility.Rect;
import field.utility.Util;
import fieldbox.io.IO;
import fieldbox.ui.FieldBoxWindow;

import java.util.*;
import java.util.stream.Collectors;

import static field.utility.Util.closeable;
import static fieldbox.boxes.FLineDrawing.*;
import static field.graphics.StandardFLineDrawing.*;

/**
 * Fundamental drawing support for Field.
 * <p>
 * For each layer on the screen this box manages the primary OpenGL drawing resources (the MeshBuilder for meshes, lines and points) and their
 * shaders. All drawing ultimately goes through these MeshBuilders.
 * <p>
 * Drawing for the FieldBoxWindow is done lazily --- unless somebody requests it through calling Drawing.dirty(box), or occasionally the windowing
 * system asks for it (on damage or resize), repainting will not happen during the animation. cycle.
 * <p>
 * This class also maintains the current relationships between window coordinates (aka mouse coordinates, aka pixels) and OpenGL coordinates (aka
 * Box's frames aka drawing coordinates). The transformation between for geometry happens entirely in the OpenGL shaders here and the math is
 * duplicated in convenience functions.
 * <p>
 * This class is the low level drawing plumbing FieldBox (Boxes) to talk to the Field graphics system (MeshBuilder). For drawing that you can use from
 * Boxes, see FrameDrawer
 */
public class Drawing extends Box {

	public interface Drawer {
		public void draw(Drawing context);
	}

	static public final Dict.Prop<Collection<Drawer>> drawers = new Dict.Prop<>("drawers").type()
											      .toCannon()
											      .doc("a collection of things that will draw inside the OpenGL paint context. Currently FrameDrawer & FLineInteraction plug into the window at this low level");
	static public final Dict.Prop<Collection<Drawer>> lateDrawers = new Dict.Prop<>("lateDrawers").type()
												      .toCannon()
												      .doc("a collection of things that will draw inside the OpenGL paint context, after everything else has drawn. Viewport plugs in at this level.");
	static public final Dict.Prop<Collection<Drawer>> glassDrawers = new Dict.Prop<>("glassDrawers").type()
													.toCannon()
													.doc("a collection of things that will draw inside the OpenGL paint context. Currently FrameDrawer & FLineInteraction plug into the window at this low level");
	static public final Dict.Prop<Drawing> drawing = new Dict.Prop<>("drawing").type()
										   .toCannon()
										   .doc("the Drawing plugin");
	static public final Dict.Prop<Boolean> needRepaint = new Dict.Prop<>("_needRepaint").type()
											    .toCannon();
	static public final Dict.Prop<Vec2> windowSpace = new Dict.Prop<>("windowSpace").type()
											.toCannon()
											.doc("set to make this box stick to the viewport when it pans around. The value is a <code>Vec2(x,y)</code>. <code>x=0, y=0</code> pins the top left of this box to the top left of the window; similarly <code>x=1, y=1</code> pins the bottom right. ");

	static {
		IO.persist(windowSpace);
	}

	public class PerLayer {
		private MeshBuilder _mesh;
		private MeshBuilder _line;
		private MeshBuilder _point;
		private Shader shader;
	}

	private Vec2 translation = new Vec2(0, 0);
	private Vec2 translationNext = null;
	private Vec2 scale = new Vec2(1, 1);
	private float opacity = 1f;

	Map<String, PerLayer> layerLocal = new LinkedHashMap<>();

	List<Bracketable> bracketableList = new ArrayList<>();

	public Drawing() {

	}

	public Box install(Box root) {
		return install(root, "__main__");
	}

	public Box install(Box root, String layerName) {
		FieldBoxWindow window = root.first(Boxes.window)
					    .orElseThrow(() -> new IllegalArgumentException(" can't draw a box hierarchy with no window to draw it in !"));

		GraphicsContext graphicsContext = window.getGraphicsContext();

		PerLayer layer = layerLocal.computeIfAbsent(layerName, (k) -> new PerLayer());

		layer.shader = new Shader();

		layer.shader.addSource(Shader.Type.vertex, "#version 410\n" +
			    "layout(location=0) in vec3 position;\n" +
			    "layout(location=1) in vec4 color;\n" +
			    "out vec4 vcolor;\n" +
			    "uniform vec2 translation;\n" +
			    "uniform vec2 scale;\n" +
			    "uniform vec2 bounds;\n" +
			    "void main()\n" +
			    "{\n" +
			    "	vec2 at = (scale.xy*(position.xy+vec2(0.5,0.5))+translation.xy)/bounds.xy;\n" +
			    "   gl_Position =  vec4(-1+at.x*2, 1-at.y*2, 0.5, 1.0);\n" +
			    "   vcolor = color;\n" +
			    "}");

		layer.shader.addSource(Shader.Type.fragment, "#version 410\n" +
			    "layout(location=0) out vec4 _output;\n" +
			    "in vec4 vcolor;\n" +
			    "uniform float opacity; \n" +
			    "void main()\n" +
			    "{\n" +
			    "	float f = mod(gl_FragCoord.x-gl_FragCoord.y,20)/20.0;\n" +
			    "	f = (sin(f*3.14*2)+1)/2;" +
			    "	f = (smoothstep(0.45, 0.55, f)+1)/2;" +
			    "	_output  = vec4(abs(vcolor.xyzw));\n" +
			    "	if (vcolor.w<0) _output.w *= f;" +
			    "	_output.w *= opacity;\n" +
			    "}");

		layer.shader.attach(new Uniform<Vec2>("translation", this::getTranslationRounded));
		layer.shader.attach(new Uniform<Vec2>("scale", scale));
		layer.shader.attach(new Uniform<Vec2>("bounds", () -> new Vec2(Window.getCurrentWidth(), Window.getCurrentHeight())));
		layer.shader.attach(new Uniform<Float>("opacity", () -> opacity));

		window.getCompositor()
		      .getLayer(layerName)
		      .getScene()
		      .attach(layer.shader);

		BaseMesh line = BaseMesh.lineList(1, 1);
		layer.shader.attach(line);
		BaseMesh mesh = BaseMesh.triangleList(1, 1);
		layer.shader.attach(mesh);
		BaseMesh point = BaseMesh.pointList(1);
		layer.shader.attach(point);

		layer._mesh = new MeshBuilder(mesh);
		layer._line = new MeshBuilder(line);
		layer._point = new MeshBuilder(point);

		bracketableList.add(layer._mesh);
		bracketableList.add(layer._line);
		bracketableList.add(layer._point);

		this.properties.put(drawing, this);

		if (layerName.equals("__main__")) {
			graphicsContext.preQueue.add(() -> drawNow(root));
			window.getCompositor()
			      .getLayer(layerName)
			      .getScene()
			      .attach(100, (x) -> lateDrawNow(root));
		}

		return this;
	}


	public MeshBuilder getLine() {
		return getLine("__main__");
	}

	public MeshBuilder getMesh() {
		return getMesh("__main__");
	}

	public MeshBuilder getPoints() {
		return getPoints("__main__");
	}


	public MeshBuilder getLine(String layerName) {
		if (layerLocal.get(layerName)._line.isOpen()) return layerLocal.get(layerName)._line;
		throw new IllegalArgumentException(" graphics resource (line) isn't open, are you trying to draw outside of your drawing method?");
	}

	public MeshBuilder getMesh(String layerName) {
		if (layerLocal.get(layerName)._mesh.isOpen()) return layerLocal.get(layerName)._mesh;
		throw new IllegalArgumentException(" graphics resource (mesh) isn't open, are you trying to draw outside of your drawing method?");
	}

	public MeshBuilder getPoints(String layerName) {
		if (layerLocal.get(layerName)._point.isOpen()) return layerLocal.get(layerName)._point;
		throw new IllegalArgumentException(" graphics resource (point) isn't open, are you trying to draw outside of your drawing method?");
	}

	public Drawing addBracketable(Bracketable bracketable) {
		bracketableList.add(bracketable);
		if (isInsideDrawing()) bracketable.open();
		return this;
	}


	static public boolean intersects(Window.Event<?> event, Box box) {
		Rect frame = (Rect) box.properties.get(Box.frame);
		if (frame == null) return false;

		Optional<Drawing> drawing = (Optional<Drawing>) box.find(Drawing.drawing, box.both())
								   .findFirst();
		if (!drawing.isPresent()) return false;

		Object o = event.after;
		if (o instanceof Window.HasPosition) {
			Optional<Vec2> o2 = ((Window.HasPosition) o).position();
			if (!o2.isPresent()) return false;

			return frame.intersects(drawing.get()
						       .windowSystemToDrawingSystem(o2.get()));
		}

		return false;
	}

	/**
	 * to convert between event / mouse / pixel coordinates and OpenGL / Box / Drawing coordinates.
	 */
	public Vec2 windowSystemToDrawingSystem(Vec2 window) {
		float y = /*Window.getCurrentHeight() -*/ window.y;
		float x = window.x;

		x = x * scale.x;
		y = y * scale.y;
		x -= translation.x;
		y -= translation.y;

		return new Vec2(x, y);
	}

	/**
	 * to convert between OpenGL / Box / Drawing coordinates and event / mouse / pixel coordinates.
	 */
	public Vec2 drawingSystemToWindowSystem(Vec2 window) {
		float y = window.y;
		float x = window.x;

		x += translation.x;
		y += translation.y;
		x = x / scale.x;
		y = y / scale.y;

		return new Vec2(x, y);
	}

	/**
	 * to convert between event / mouse / pixel coordinates and OpenGL / Box / Drawing delta's.
	 */
	public Vec2 windowSystemToDrawingSystemDelta(Vec2 windowDelta) {
		float y = /*-*/windowDelta.y;
		float x = windowDelta.x;

		x = x * scale.x;
		y = y * scale.y;

		return new Vec2(x, y);
	}

	boolean insideDrawing = false;
	Vec2 lastDimensions = null;
	Vec2 nextDimensions = null;

	public void drawNow(Box root) {
		Boolean q = root.properties.remove(needRepaint);
		if (q == null || !q) return;

		find(Boxes.window, both()).findFirst()
					  .ifPresent(x -> {
						  nextDimensions = new Vec2(x.getWidth(), x.getHeight());
					  });

		if (translationNext != null || (lastDimensions != null && !Util.safeEq(lastDimensions, nextDimensions))) {
			updateWindowSpaceBoxes(translation, translationNext == null ? translation : translationNext, lastDimensions == null ? nextDimensions : lastDimensions, nextDimensions);

			System.out.println(" translation :" + translation + " " + translationNext);

			translation.x = (translationNext == null ? translation : translationNext).x;
			translation.y = (translationNext == null ? translation : translationNext).y;
			translationNext = null;
		}

		try (AutoCloseable ignored = closeable(bracketableList)) {
			insideDrawing = true;
			root.find(drawers, root.both())
			    .collect(Collectors.toList())
			    .stream()
			    .flatMap(x -> x.stream()).collect(Collectors.toList()) // avoid concurrent modification
				    .stream()
				    .forEach(x -> x.draw(this));
		} catch (Exception e) {
			System.err.println(" exception thrown during drawing ");
			e.printStackTrace();
		} finally {
			insideDrawing = false;
		}

		find(Boxes.window, both()).findFirst()
					  .ifPresent(x -> {
						  lastDimensions = new Vec2(x.getWidth(), x.getHeight());
					  });


	}

	private void lateDrawNow(Box root) {
		try {
			insideDrawing = true;
			root.find(lateDrawers, root.both())
			    .collect(Collectors.toList())
			    .stream()
			    .flatMap(x -> x.stream()).collect(Collectors.toList()) // avoid concurrent modification
				    .stream()
				    .forEach(x -> x.draw(this));
		} catch (Exception e) {
			System.err.println(" exception thrown during drawing ");
			e.printStackTrace();
		} finally {
			insideDrawing = false;
		}
	}


	private void updateWindowSpaceBoxes(Vec2 translation, Vec2 translationNext, Vec2 dimensions, Vec2 dimensionsNext) {
		this.breadthFirst(both())
		    .filter(x -> x.properties.get(windowSpace) != null)
		    .forEach(box -> {

			    Vec2 v = box.properties.get(windowSpace);

			    Rect f = box.properties.get(Box.frame);
			    f = new Rect(f.x, f.y, f.w, f.h);

			    System.out.println(" translation :" + translation + " " + translationNext + " " + dimensions + " " + dimensionsNext);


			    f.x = f.x + (translation.x + dimensionsNext.x * v.x) - (translationNext.x + dimensions.x * v.x);
			    f.y = f.y + (translation.y + dimensionsNext.y * v.y) - (translationNext.y + dimensions.y * v.y);
			    box.properties.put(Box.frame, f);
		    });
	}

	/**
	 * are we currently inside a repaint. Specifically, are we inside a valid OpenGL context? There are certain OpenGL calls that are only valid
	 * inside a valid context.
	 */
	public boolean isInsideDrawing() {
		return insideDrawing;
	}

	/**
	 * call this to request a redraw at the next available animation cycle
	 */
	static public void dirty(Box b) {
		b.find(Boxes.root, b.both())
		 .findFirst()
		 .map(x -> x.properties.put(needRepaint, true));
		b.find(Boxes.window, b.both())
		 .findFirst()
		 .ifPresent(x -> x.requestRepaint());
	}

	/**
	 * call this to request another 'n' redraws at the next and subsequent available animation cycle
	 */
	static public void dirty(Box b, int n) {
		b.find(Boxes.root, b.both())
		 .findFirst()
		 .map(x -> x.properties.put(needRepaint, true));
		b.find(Boxes.window, b.both())
		 .findFirst()
		 .ifPresent(x -> x.requestRepaint());
		RunLoop.main.nTimes(() -> dirty(b), n);
	}

	public Vec2 getScale() {
		return new Vec2(scale);
	}

	public Vec2 getTranslation() {
		return new Vec2(translation);
	}

	public Vec2 getTranslationRounded() {
		return new Vec2((int) translation.x, (int) translation.y);
	}

	/**
	 * sets the translation of the canvas. Note, we defer actually handing this off to the graphics system (or getTranslation) until the next draw
	 * cycle. This way we do not get repaints during which the transformation changes half way through.
	 */
	public void setTranslation(Box root, Vec2 t) {
		if (this.translation.distanceFrom(t) > 1e-10) dirty(root);
		this.translationNext = new Vec2(t);
	}

	/**
	 * returns the current draw coordinates that are visible inside the window
	 */
	public Rect getCurrentViewBounds(Box b) {
		FieldBoxWindow window = b.first(Boxes.window, b.both())
					 .get();
		return new Rect(-translation.x, -translation.y, window.getWidth() * scale.x, window.getHeight() * scale.y);
	}

	/**
	 * puts some text on the screen for a certain number of animation cycles. TODO --- really ought to be certain number of seconds, not cycles.
	 */
	static public void notify(String text, Box from, int dur) {
		from.properties.putToMap(frameDrawing, "__notificationText__", expires(box -> {

			Drawing d = from.first(drawing, from.both())
					.get();
			Rect view = d.getCurrentViewBounds(from);
			FLine f = new FLine();
			f.moveTo(view.x + view.w / 2, view.y + view.h / 2 - 10);
			f.nodes.get(0).attributes.put(StandardFLineDrawing.text, text);
			f.nodes.get(0).attributes.put(textScale, 4);
			f.attributes.put(color, new Vec4(1, 1, 1, 1f));
			f.attributes.put(hasText, true);
			f.attributes.put(layer, "glass2");

			return f;
		}, (int) (dur), 0.05f));
		from.properties.putToMap(frameDrawing, "__notificationMask__", expires(box -> {

			Drawing d = from.first(drawing, from.both())
					.get();
			Rect view = d.getCurrentViewBounds(from);
			FLine f = new FLine();
			int w = 20;
			int h = 60;
			f.rect(view.x - w, view.y + view.h / 2 - h - 10, view.w + w * 2, h + 25);
			f.attributes.put(color, new Vec4(0, 0, 0, -0.8f));
			f.attributes.put(layer, "glass2");
			f.attributes.put(filled, true);
			return f;
		}, dur, 0.05f));

		if (from != null) Drawing.dirty(from);

	}
}
