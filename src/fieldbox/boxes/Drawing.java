package fieldbox.boxes;

import field.app.RunLoop;
import field.graphics.*;
import field.linalg.Vec2;
import field.linalg.Vec4;
import field.utility.Dict;
import field.utility.Rect;
import field.utility.Util;
import fieldbox.io.IO;
import fieldbox.ui.FieldBoxWindow;
import fieldnashorn.annotations.HiddenInAutocomplete;

import java.util.*;
import java.util.stream.Collectors;

import static field.graphics.StandardFLineDrawing.*;
import static field.utility.Util.closeable;
import static fieldbox.boxes.FLineDrawing.*;

/**
 * Fundamental drawing support for Field.
 * <p>
 * For each layer on the screen this box manages the primary OpenGL drawing resources (the MeshBuilder for meshes, lines and points) and their shaders. All drawing ultimately goes through these
 * MeshBuilders.
 * <p>
 * Drawing for the FieldBoxWindow is done lazily --- unless somebody requests it through calling Drawing.dirty(box), or occasionally the windowing system asks for it (on damage or resize), repainting
 * will not happen during the RunLoop.
 * <p>
 * This class also maintains the current relationships between window coordinates (aka mouse coordinates, aka pixels) and OpenGL coordinates (aka Box's frames aka drawing coordinates). The
 * transformation between for geometry happens entirely in the OpenGL shaders here and the math is duplicated in convenience functions.
 * <p>
 * This class is the low level drawing plumbing FieldBox (Boxes) to talk to the Field graphics system (MeshBuilder). For drawing that you can use from Boxes, see FLineDrawing
 */
public class Drawing extends Box implements DrawingInterface {

	static public final Dict.Prop<Collection<Drawer>> drawers = new Dict.Prop<>("drawers").type()
		.toCanon()
		.doc("a collection of things that will draw inside the OpenGL paint context. Currently FrameDrawer & FLineInteraction plug into the window at this low level");
	static public final Dict.Prop<Collection<Drawer>> lateDrawers = new Dict.Prop<>("lateDrawers").type()
		.toCanon()
		.doc("a collection of things that will draw inside the OpenGL paint context, after everything else has drawn. Viewport plugs in at this level.");
	static public final Dict.Prop<Drawing> drawing = new Dict.Prop<>("drawing").type()
		.toCanon()
		.doc("the Drawing plugin")
		.set(Dict.readOnly, true);

	static public final Dict.Prop<Boolean> needRepaint = new Dict.Prop<>("_needRepaint").type()
		.toCanon();
	static public final Dict.Prop<Vec2> windowSpace = new Dict.Prop<>("windowSpace").type()
		.toCanon()
		.doc("set to make this box stick to the viewport when it pans around. The value is a <code>Vec2(x,y)</code>. <code>x=0, y=0</code> pins the top left of this box to the top left of the window; similarly <code>x=1, y=1</code> pins the top left of this box to the bottom right. ");

	static public final Dict.Prop<Vec2> windowScale = new Dict.Prop<>("windowScale").type()
		.toCanon()
		.doc("like `windowSpace` but changes the width and height of the box to maintain a position in space for the lower right corner of a box. Combine with `_.windowSpace`.");

	static {
		IO.persist(windowSpace);
		IO.persist(windowScale);
	}

	public float displayZ;

	Map<String, PerLayer> layerLocal = new LinkedHashMap<>();
	List<Bracketable> bracketableList = new ArrayList<>();
	boolean insideDrawing = false;
	Vec2 lastDimensions = null;
	Vec2 nextDimensions = null;
	private Vec2 translation = new Vec2(0, 0);
	private Vec2 translationNext = null;
	private Vec2 scale = new Vec2(1.f, 1.f);
	private Vec2 scaleNext = null;
	private Vec2 boxScale = new Vec2(1, 1);
	private float opacity = 1f;

	public Drawing() {
		this.properties.putToMap(IO.onPreparingToSave, "__checkWindowSpace__", () -> {
			System.out.println(" handling window space boxes ");

			Map<Box, Rect> oldFrames = new LinkedHashMap<>();

			this.breadthFirstAll(this.allDownwardsFrom()).forEach( x -> {
				if (x.properties.has(frame))
					oldFrames.put(x, x.properties.get(frame).duplicate());
			});

			updateWindowSpaceBoxes(translation, new Vec2(0,0), lastDimensions, lastDimensions, scale, new Vec2(1,1));

			this.properties.putToMap(IO.onFinishingSaving, "__checkWindowSpace__", () -> {
				RunLoop.main.delayTicks(() -> {
					System.out.println(" undoing window space boxes ");
					this.breadthFirstAll(this.allDownwardsFrom()).forEach(x -> {
						if (oldFrames.containsKey(x))
							x.properties.put(frame, oldFrames.get(x));
					});
					oldFrames.clear();
					Drawing.dirty(this);
				}, 1);
			});

		});
	}

	static public boolean intersects(Window.Event<?> event, Box box) {
		Rect frame = box.properties.get(Box.frame);
		if (frame == null) return false;

		Optional<Drawing> drawing = box.find(Drawing.drawing, box.both())
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
	 * call this to request a redraw at the next available animation cycle
	 */
	static public void dirty(Box b) {
		// which layer needs updating?
		String layerName = b.properties.getOr(FLineDrawing.layer, () -> "__main__");

		dirty(b, layerName);
	}

	static public void dirty(Box b, String explicitLayerName) {
		b.find(Boxes.root, b.both())
			.findFirst()
			.map(x -> x.properties.put(needRepaint, true));

		if (explicitLayerName.endsWith(".fast"))
			explicitLayerName = explicitLayerName.substring(0, explicitLayerName.length() - ".fast".length());

		String finalExplicitLayerName = explicitLayerName;
		b.find(Boxes.window, b.both())
			.findFirst()
			.ifPresent(x -> {
				x.requestRepaint();
				x.getCompositor()
					.getLayer(finalExplicitLayerName).dirty();
			});

	}

	/**
	 * call this to request another 'n' redraws at the next and subsequent available animation cycle
	 */
	static public void dirty(Box b, int n) {
		dirty(b);

		RunLoop.main.nTimes(() -> dirty(b), n);
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
		}, dur, 0.05f));
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
			"uniform float displayZ;\n" +
			"void main()\n" +
			"{\n" +
			"	vec2 at = (scale.xy*(position.xy+vec2(0.5,0.5))+translation.xy)/bounds.xy;\n" +
			"   gl_Position =  vec4(-1+at.x*2+displayZ*position.z, 1-at.y*2, 0.5, 1.0);\n" +
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
		layer.shader.attach(new Uniform<Vec2>("scale", () -> new Vec2(scale.x * boxScale.x, scale.y * boxScale.y)));
		layer.shader.attach(new Uniform<Vec2>("bounds", () -> new Vec2(Window.getCurrentWidth(), Window.getCurrentHeight())));
		layer.shader.attach(new Uniform<Float>("opacity", () -> opacity));
		layer.shader.attach(new Uniform<Float>("displayZ", () -> displayZ));

		layer.pointShader = new Shader();

		layer.pointShader.addSource(Shader.Type.vertex, "#version 410\n" +
			"layout(location=0) in vec3 position;\n" +
			"layout(location=1) in vec4 color;\n" +
			"layout(location=2) in vec2 pointControl;\n" +
			"out vec4 vcolor_q;\n" +
			"out vec2 pc_q;\n" +
			"uniform vec2 translation;\n" +
			"uniform vec2 scale;\n" +
			"uniform vec2 bounds;\n" +
			"uniform float displayZ;\n" +
			"void main()\n" +
			"{\n" +
			"	vec2 at = (scale.xy*(position.xy+vec2(0.5,0.5))+translation.xy)/bounds.xy;\n" +
			"   gl_Position =  vec4(-1+at.x*2+displayZ*position.z, 1-at.y*2, 0.5, 1.0);\n" +
			"   vcolor_q = color;\n" +
			"   pc_q= pointControl;\n" +
			"}");

		layer.pointShader.addSource(Shader.Type.geometry, "#version 410\n" +
			"layout(points) in;\n" +
			"layout(triangle_strip, max_vertices=4) out;\n" +
			"in vec4[] vcolor_q;\n" +
			"in vec2[] pc_q;\n" +
			"out vec4 vcolor;\n" +
			"out vec2 tc;\n" +
			"out vec2 pc;\n" +
			"uniform vec2 bounds;\n" +
			"void main()\n" +
			"{\n" +
			"float s1 = (pc_q[0].x+2)/bounds.x; float s2 = s1*bounds.x/bounds.y;\n" +
			"vcolor = vcolor_q[0];\n" +
			"pc = pc_q[0]\n;" +
			"tc = vec2(-1,-1);\n" +
			"gl_Position = gl_in[0].gl_Position+vec4(-s1, -s2, 0, 0)*gl_in[0].gl_Position.w;\n" +
			"EmitVertex();\n" +
			"vcolor = vcolor_q[0];\n" +
			"tc = vec2(1,-1);\n" +
			"gl_Position = gl_in[0].gl_Position+vec4(s1, -s2, 0, 0)*gl_in[0].gl_Position.w;\n" +
			"EmitVertex();\n" +
			"vcolor = vcolor_q[0];\n" +
			"tc = vec2(-1,1);\n" +
			"gl_Position = gl_in[0].gl_Position+vec4(-s1, s2, 0, 0)*gl_in[0].gl_Position.w;\n" +
			"EmitVertex();\n" +
			"vcolor = vcolor_q[0];\n" +
			"tc = vec2(1,1);\n" +
			"gl_Position = gl_in[0].gl_Position+vec4(s1, s2, 0, 0)*gl_in[0].gl_Position.w;\n" +
			"EmitVertex();\n" +
			"EndPrimitive();\n" +
			"}");

		layer.pointShader.addSource(Shader.Type.fragment, "#version 410\n" +
			"layout(location=0) out vec4 _output;\n" +
			"in vec4 vcolor;\n" +
			"in vec2 tc;\n" +
			"uniform float opacity; \n" +
			"void main()\n" +
			"{\n" +
			"	float f = mod(gl_FragCoord.x-gl_FragCoord.y,20)/20.0;\n" +
			"	f = (sin(f*3.14*2)+1)/2;\n" +
			"	f = (smoothstep(0.45, 0.55, f)+1)/2;\n" +
			"	_output  = vec4(abs(vcolor.xyzw)*smoothstep(0.1, 0.2, (1-(length(tc.xy)))));\n" +
			"	if (vcolor.w<0) _output.w *= f;" +
			"	_output.w *= opacity;\n" +
			"}");

		layer.pointShader.attach(new Uniform<Vec2>("translation", this::getTranslationRounded));
		layer.pointShader.attach(new Uniform<Vec2>("scale", () -> new Vec2(scale.x * boxScale.x, scale.y * boxScale.y)));
		layer.pointShader.attach(new Uniform<Vec2>("bounds", () -> new Vec2(Window.getCurrentWidth(), Window.getCurrentHeight())));
		layer.pointShader.attach(new Uniform<Float>("opacity", () -> opacity));
		layer.pointShader.attach(new Uniform<Float>("displayZ", () -> displayZ));


		window.getCompositor()
			.getLayer(layerName)
			.getScene()
			.attach(layer.shader);
		window.getCompositor()
			.getLayer(layerName)
			.getScene()
			.attach(layer.pointShader);

		/*
		BaseMesh mesh = BaseMesh.triangleList(1, 1);
		layer.shader.attach(mesh);
		BaseMesh line = BaseMesh.lineList(1, 1);
		layer.shader.attach(line);
		BaseMesh point = BaseMesh.pointList(1);
		layer.pointShader.attach(point);

		layer._mesh = new MeshBuilder(mesh);
		layer._line = new MeshBuilder(line);
		layer._point = new MeshBuilder(point);

		bracketableList.add(layer._mesh);
		bracketableList.add(layer._line);
		bracketableList.add(layer._point);

		BaseMesh fastMesh = BaseMesh.triangleList(1, 1);
		layer.shader.attach(fastMesh);
		BaseMesh fastLine = BaseMesh.lineList(1, 1);
		layer.shader.attach(fastLine);
		BaseMesh fastPoint = BaseMesh.pointList(1);
		layer.pointShader.attach(fastPoint);

		layer._fastMesh = new MeshBuilder(fastMesh);
		layer._fastLine = new MeshBuilder(fastLine);
		layer._fastPoint = new MeshBuilder(fastPoint);

		bracketableList.add(layer._fastMesh);
		bracketableList.add(layer._fastLine);
		bracketableList.add(layer._fastPoint);
		*/

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

		String subName = "";
		if (layerName.contains(".")) {
			layerName = layerName.substring(0, layerName.lastIndexOf("."));
			subName = layerName.substring(layerName.lastIndexOf(".")+1);
		}

		PerLayer l = layerLocal.get(layerName);
		MeshBuilder ll = l._line.computeIfAbsent(subName, k -> {
			BaseMesh line = BaseMesh.lineList(1, 1);
			l.shader.attach(line);

			MeshBuilder b = new MeshBuilder(line);
			bracketableList.add(b);

			b.open();

			return b;
		});
		if (ll.isOpen()) return ll;
		throw new IllegalArgumentException(" graphics resource (line) isn't open, are you trying to draw outside of your drawing method?");
	}

	public MeshBuilder getMesh(String layerName) {
		String subName = "";
		if (layerName.contains(".")) {
			layerName = layerName.substring(0, layerName.lastIndexOf("."));
			subName = layerName.substring(layerName.lastIndexOf(".")+1);
		}

		PerLayer l = layerLocal.get(layerName);
		MeshBuilder ll = l._mesh.computeIfAbsent(subName, k -> {
			BaseMesh line = BaseMesh.triangleList(1, 1);
			l.shader.attach(line);

			MeshBuilder b = new MeshBuilder(line);
			bracketableList.add(b);

			b.open();

			return b;
		});
		if (ll.isOpen()) return ll;
		throw new IllegalArgumentException(" graphics resource (mesh) isn't open, are you trying to draw outside of your drawing method?");
	}

	public MeshBuilder getPoints(String layerName) {

		String subName = "";
		if (layerName.contains(".")) {
			layerName = layerName.substring(0, layerName.lastIndexOf("."));
			subName = layerName.substring(layerName.lastIndexOf(".")+1);
		}

		PerLayer l = layerLocal.get(layerName);
		MeshBuilder ll = l._point.computeIfAbsent(subName, k -> {
			BaseMesh line = BaseMesh.pointList(1);
			l.pointShader.attach(line);

			MeshBuilder b = new MeshBuilder(line);
			bracketableList.add(b);

			b.open();

			return b;
		});
		if (ll.isOpen()) return ll;
		throw new IllegalArgumentException(" graphics resource (point) isn't open, are you trying to draw outside of your drawing method?");
	}

	public DrawingInterface addBracketable(Bracketable bracketable) {
		bracketableList.add(bracketable);
		if (isInsideDrawing()) bracketable.open();
		return this;
	}

	/**
	 * to convert between event / mouse / pixel coordinates and OpenGL / Box / Drawing coordinates.
	 */
	public Vec2 windowSystemToDrawingSystem(Vec2 window) {
		double y = /*Window.getCurrentHeight() -*/ window.y;
		double x = window.x;

		x = x / scale.x;
		y = y / scale.y;
		x -= translation.x / scale.x;
		y -= translation.y / scale.y;

		return new Vec2(x, y);
	}

	/**
	 * to convert between OpenGL / Box / Drawing coordinates and event / mouse / pixel coordinates.
	 */
	public Vec2 drawingSystemToWindowSystem(Vec2 window) {
		return drawingSystemToWindowSystem(window, translation, scale);
	}


	/**
	 * to convert between OpenGL / Box / Drawing coordinates and event / mouse / pixel coordinates.
	 */
	public Vec2 drawingSystemToWindowSystem(Vec2 window, Vec2 translation, Vec2 scale) {
		double y = window.y;
		double x = window.x;

		x = x * scale.x * boxScale.x;
		y = y * scale.y * boxScale.y;
		x += translation.x;
		y += translation.y;

		return new Vec2(x, y);
	}

	/**
	 * to convert between event / mouse / pixel coordinates and OpenGL / Box / Drawing coordinates.
	 */
	protected Vec2 windowSystemToDrawingSystemNext(Vec2 window, Vec2 translationNext, Vec2 scaleNext) {
		double y = /*Window.getCurrentHeight() -*/ window.y;
		double x = window.x;

		x = x / scaleNext.x;
		y = y / scaleNext.y;
		x -= translationNext.x / scaleNext.x;
		y -= translationNext.y / scaleNext.y;

		return new Vec2(x, y);
	}

	/**
	 * to convert between OpenGL / Box / Drawing coordinates and event / mouse / pixel coordinates.
	 */
	protected Vec2 drawingSystemToWindowSystemNext(Vec2 window, Vec2 translationNext, Vec2 scaleNext) {
		double y = window.y;
		double x = window.x;

		x = x * (scaleNext == null ? scale : scaleNext).x * boxScale.x;
		y = y * (scaleNext == null ? scale : scaleNext).y * boxScale.y;
		x += (translationNext == null ? translation : translationNext).x;
		y += (translationNext == null ? translation : translationNext).y;

		return new Vec2(x, y);
	}

	/**
	 * to convert between event / mouse / pixel coordinates and OpenGL / Box / Drawing delta's.
	 */
	public Vec2 windowSystemToDrawingSystemDelta(Vec2 windowDelta) {
		return windowSystemToDrawingSystemDelta(windowDelta, translation, scale);
	}
	/**
	 * to convert between event / mouse / pixel coordinates and OpenGL / Box / Drawing delta's.
	 */
	public Vec2 windowSystemToDrawingSystemDelta(Vec2 windowDelta, Vec2 translation, Vec2 scale) {
		double y = /*-*/windowDelta.y;
		double x = windowDelta.x;

		x = x / (scale.x * boxScale.x);
		y = y / (scale.y * boxScale.y);

		return new Vec2(x, y);
	}

	/**
	 * to convert between event / mouse / pixel coordinates and OpenGL / Box / Drawing delta's.
	 */
	public Vec2 windowSystemToDrawingSystemDeltaNext(Vec2 windowDelta) {
		double y = /*-*/windowDelta.y;
		double x = windowDelta.x;

		x = x / ((scaleNext == null ? scale : scaleNext).x * boxScale.x);
		y = y / ((scaleNext == null ? scale : scaleNext).y * boxScale.y);

		return new Vec2(x, y);
	}

	long drawCount = 0;

	@HiddenInAutocomplete
	public long getDrawCount() {
		return drawCount;
	}


	@HiddenInAutocomplete
	public boolean freezeDraw = false;

	public void drawNow(Box root) {
		Boolean q = root.properties.remove(needRepaint);
		if (q == null || !q) return;

		if (freezeDraw) return;

		drawCount++;


		find(Boxes.window, both()).findFirst()
			.ifPresent(x -> {
				nextDimensions = new Vec2(x.getWidth(), x.getHeight());
			});

		if (translationNext != null || (lastDimensions != null && !Util.safeEq(lastDimensions, nextDimensions)) || scaleNext != null) {
			updateWindowSpaceBoxes(translation, translationNext == null ? translation : translationNext, lastDimensions == null ? nextDimensions : lastDimensions, nextDimensions, scale, scaleNext == null ? scale : scaleNext);

			translation.x = (translationNext == null ? translation : translationNext).x;
			translation.y = (translationNext == null ? translation : translationNext).y;
			translationNext = null;

			scale.x = (scaleNext == null ? scale : scaleNext).x;
			scale.y = (scaleNext == null ? scale : scaleNext).y;
			scaleNext = null;

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

	private void updateWindowSpaceBoxes(Vec2 t, Vec2 tn, Vec2 d, Vec2 dn, Vec2 s, Vec2 sn) {
		this.breadthFirst(both())
			.filter(x -> x.properties.get(windowSpace) != null)
			.forEach(box -> {

				Vec2 v = box.properties.get(windowSpace);

				Rect f = box.properties.get(Box.frame);
				f = new Rect(f.x, f.y, f.w, f.h);

				float bx = f.x + f.w;
				float by = f.y + f.h;

				Vec2 at = new Vec2(f.x, f.y);
				Vec2 delta = deltaToStabalize(d, dn, v, at, t, tn, s, sn);
				f.x -= delta.x;
				f.y -= delta.y;

				Vec2 v2 = box.properties.get(windowScale);
				if (v2 != null) {

					at = new Vec2(bx, by);
					delta = deltaToStabalize(d, dn, v2, at, t, tn, s, sn);

					bx -= delta.x;
					by -= delta.y;

					f.w = bx - f.x;
					f.h = by - f.y;
				}


				box.properties.put(Box.frame, f);
			});
	}

	private Vec2 deltaToStabalize(Vec2 dimensions, Vec2 dimensionsNext, Vec2 v, Vec2 at, Vec2 t, Vec2 tn, Vec2 s, Vec2 sn) {
		Vec2 tl_win = drawingSystemToWindowSystem(at, t, s);
		Vec2 tl_winN = drawingSystemToWindowSystemNext(at, tn, sn);
		Vec2 delta = new Vec2(tl_winN).sub(tl_win).sub(new Vec2(dimensionsNext.x * v.x - dimensions.x * v.x, dimensionsNext.y * v.y - dimensions.y * v.y));
		delta = windowSystemToDrawingSystemDelta(delta, tn, sn);
		return delta;
	}

	/**
	 * are we currently inside a repaint. Specifically, are we inside a valid OpenGL context? There are certain OpenGL calls that are only valid inside a valid context.
	 */
	public boolean isInsideDrawing() {
		return insideDrawing;
	}

	public Vec2 getScale() {
		return new Vec2(scale);
	}

	/**
	 * Returns the shader (for lines and meshes) that's currently shading the main layer of this window
	 */
	public Shader getShader() {
		return getShader("__main__");
	}

	/**
	 * Returns the shader (for lines and meshes) that's currently shading a specific ayer of this window
	 */
	public Shader getShader(String layerName) {
		PerLayer layer = layerLocal.computeIfAbsent(layerName, (k) -> new PerLayer());
		return layer.shader;
	}

	public Vec2 getTranslation() {
		return new Vec2(translation);
	}

	public Vec2 getTranslationRounded() {
		return new Vec2((int) translation.x, (int) translation.y);
	}

	/**
	 * sets the translation of the canvas. Note, we defer actually handing this off to the graphics system (or getTranslation) until the next draw cycle. This way we do not get repaints during
	 * which the transformation changes half way through.
	 */
	public void setTranslation(Box root, Vec2 t) {
		if (this.translation.distance(t) > 1e-10) {
			dirty(root);
			dirty(root, "glass");
		}
		this.translationNext = new Vec2(t);
	}

	/**
	 * sets the translation of the canvas. Note, we defer actually handing this off to the graphics system (or getTranslation) until the next draw cycle. This way we do not get repaints during
	 * which the transformation changes half way through.
	 */
	public void setScale(Box root, Vec2 t) {
		if (this.scale.distance(t) > 1e-10) {
			dirty(root);
			dirty(root, "glass");
		}
		this.scaleNext = new Vec2(t);
	}

	/**
	 * returns the current draw coordinates that are visible inside the window
	 */
	public Rect getCurrentViewBounds(Box b) {
		FieldBoxWindow window = b.first(Boxes.window, b.both())
			.get();

		Vec2 tl = windowSystemToDrawingSystem(new Vec2(0, 0));
		Vec2 br = windowSystemToDrawingSystem(new Vec2(window.getWidth(), window.getHeight()));


		return new Rect(tl.x, tl.y, br.x - tl.x, br.y - tl.y);
	}

	public interface Drawer {
		void draw(DrawingInterface context);
	}

	public class PerLayer {
		private Map<String, MeshBuilder> _mesh = new LinkedHashMap<>();
		private Map<String, MeshBuilder> _line= new LinkedHashMap<>();
		private Map<String, MeshBuilder> _point= new LinkedHashMap<>();

		private Shader shader;
		private Shader pointShader;
	}
}
