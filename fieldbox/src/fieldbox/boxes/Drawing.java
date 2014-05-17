package fieldbox.boxes;

import field.graphics.*;
import field.linalg.Vec2;
import field.utility.Dict;
import field.utility.Rect;
import fieldbox.ui.FieldBoxWindow;
import field.graphics.Window;

import java.util.*;

import static field.utility.Util.closeable;

public class Drawing extends Box {

	public interface Drawer {
		public void draw(Drawing context);
	}

	static public final Dict.Prop<Collection<Drawer>> drawers = new Dict.Prop<>("drawers").toCannon();
	static public final Dict.Prop<Collection<Drawer>> glassDrawers = new Dict.Prop<>("glassDrawers").toCannon();
	static public final Dict.Prop<Drawing> drawing = new Dict.Prop<>("drawing").toCannon();
	static public final Dict.Prop<Drawing> glassDrawing = new Dict.Prop<>("glassDrawing").toCannon();
	static public final Dict.Prop<Boolean> dirty = new Dict.Prop<>("Boolean").toCannon();

	public class PerLayer
	{
		private MeshBuilder _mesh;
		private MeshBuilder _line;
		private MeshBuilder _point;
		private Shader shader;
	}

	private Vec2 translation = new Vec2(0, 0);
	private Vec2 scale = new Vec2(1, 1);
	private float opacity = 1f;

	Map<String, PerLayer> layerLocal = new LinkedHashMap<>();


	List<Bracketable> bracketableList = new ArrayList<>();

	public Drawing() {

	}


	public Box install(Box root)
	{
		return install(root, "__main__");
	}

	public Box install(Box root, String layerName)
	{
		FieldBoxWindow window = root.first(Boxes.window).orElseThrow(() -> new IllegalArgumentException(" can't draw a box hierarchy with no window to draw it in !"));

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
			    "	float f = mod(gl_FragCoord.x-gl_FragCoord.y,20)/20.0;\n"+
			    "	f = (sin(f*3.14*2)+1)/2;"+
			    "	f = (smoothstep(0.45, 0.55, f)+1)/2;"+
			    "	_output  = vec4(abs(vcolor.xyzw));\n" +
			    "	if (vcolor.w<0) _output.w *= f;"+
			    "	_output.w *= opacity;\n" +
			    "}");

		layer.shader.connect(new Uniform<Vec2>("translation", translation));
		layer.shader.connect(new Uniform<Vec2>("scale", scale));
		layer.shader.connect(new Uniform<Vec2>("bounds", () -> new Vec2(Window.getCurrentWidth(), Window.getCurrentHeight())));
		layer.shader.connect(new Uniform<Float>("opacity", () -> opacity));

		window.getCompositor().getLayer(layerName).getScene().connect(layer.shader);

		BaseMesh line = BaseMesh.lineList(1, 1);
		layer.shader.connect(line);
		BaseMesh mesh = BaseMesh.triangleList(1, 1);
		layer.shader.connect(mesh);
		BaseMesh point = BaseMesh.pointList(1);
		layer.shader.connect(point);

		layer._mesh = new MeshBuilder(mesh);
		layer._line = new MeshBuilder(line);
		layer._point = new MeshBuilder(point);

		bracketableList.add(layer._mesh);
		bracketableList.add(layer._line);
		bracketableList.add(layer._point);

		this.properties.put(drawing, this);

		if (layerName.equals("__main__"))
			graphicsContext.preQueue.add(() -> drawNow(root));
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
		if (isInsideDrawing())
			bracketable.open();
		return this;
	}

	public Vec2 windowSystemToDrawingSystem(Vec2 window) {
		float y = /*Window.getCurrentHeight() -*/ window.y;
		float x = window.x;

		x = x * scale.x;
		y = y * scale.y;
		x -= translation.x;
		y -= translation.y;

		return new Vec2(x, y);
	}

	public Vec2 windowSystemToDrawingSystemDelta(Vec2 windowDelta) {
		float y = /*-*/windowDelta.y;
		float x = windowDelta.x;

		x = x * scale.x;
		y = y * scale.y;

		return new Vec2(x, y);
	}

	boolean insideDrawing = false;

	public void drawNow(Box root) {
		Boolean q = root.properties.remove(dirty);
		if (q == null || !q) return;

		// javac and IDEA have a disagreement about the typing of this line
		try (AutoCloseable ignored = closeable(bracketableList)) {
			insideDrawing = true;
			root.find(drawers, root.both()).flatMap(x -> x.stream()).forEach(x -> x.draw(this));
		} catch (Exception e) {
			System.err.println(" exception thrown during drawing ");
			e.printStackTrace();
		}
		finally
		{
			insideDrawing = false;
		}
	}

	public boolean isInsideDrawing()
	{
		return insideDrawing;
	}

	static public void dirty(Box b) {
		b.find(Boxes.root, b.both()).findFirst().map(x -> x.properties.put(dirty, true));
		b.find(Boxes.window, b.both()).findFirst().ifPresent(x -> x.requestRepaint());
	}

	public Vec2 getScale() {
		return new Vec2(scale);
	}

	public Vec2 getTranslation() {
		return new Vec2(translation);
	}

	public void setTranslation(Box root, Vec2 t) {
		if (this.translation.distanceFrom(t)>1e-10) dirty(root);
		this.translation.set(t);
	}


	public void setOpacity(Box root, float v) {
		if (v!=this.opacity) dirty(root);
		this.opacity = v;
	}

	public float getOpacity()
	{
		return this.opacity;
	}

	public Rect getCurrentViewBounds(Box b)
	{
		FieldBoxWindow window = b.first(Boxes.window).get();
		return new Rect(-translation.x, -translation.y, window.getWidth()*scale.x, window.getHeight()*scale.y);
	}

}
