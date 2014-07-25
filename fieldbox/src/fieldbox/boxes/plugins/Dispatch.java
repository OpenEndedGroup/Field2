package fieldbox.boxes.plugins;

import com.badlogic.jglfw.Glfw;
import field.graphics.FLine;
import field.graphics.FLinesAndJavaShapes;
import field.graphics.RunLoop;
import field.graphics.Window;
import field.linalg.Vec2;
import field.linalg.Vec4;
import field.utility.Cached;
import field.utility.Rect;
import fieldbox.boxes.*;

import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.util.Optional;

import static fieldbox.boxes.FLineDrawing.frameDrawing;
import static fieldbox.boxes.StandardFLineDrawing.*;

/**
 * Adds: Hold down G to change the box-graph network. Also draws the current graph topology.
 * <p>
 * This is like Topology.java, but simpler, since the connections that it produces are not reified as new boxes.
 */
public class Dispatch extends Box implements Mouse.OnMouseDown {


	private final Box root;
	boolean on = false;

	Object allFrameHash  = 0L;
	long allFrameHashAt = 0;
	long allFrameHashSalt = 0;

	public Dispatch(Box root) {
		this.root = root;
		this.properties.putToList(Mouse.onMouseDown, this);

		this.properties.putToMap(FLineDrawing.frameDrawing, "__allTopology__", new Cached<Box, Object, FLine>((b, was) -> {

			FLine f = new FLine();

			breadthFirst(both()).filter(x -> x.properties.has(Box.frame)).forEach(x -> {

				for (Box x2 : x.children()) {
					if (x2.properties.has(Box.frame)) {
						FLine m = arc(x.properties.get(Box.frame), x2.properties.get(Box.frame), false);
						if (f.nodes.size() == 0) f.attributes.putAll(m.attributes);
						f.nodes.addAll(m.nodes);
					}
				}

			});

			return f;

		}, b -> allFrameHash()));
		this.properties.putToMap(FLineDrawing.frameDrawing, "__allTopologySelected__", new Cached<Box, Object, FLine>((b, was) -> {

			FLine f = new FLine();

			breadthFirst(both()).filter(x -> x.properties.has(Box.frame)).filter(x -> x.properties.isTrue(Mouse.isSelected, false)).forEach(x -> {

				for (Box x2 : x.children()) {
					if (x2.properties.has(Box.frame)) {
						FLine m = arc(x.properties.get(Box.frame), x2.properties.get(Box.frame), true);
						if (f.nodes.size() == 0) f.attributes.putAll(m.attributes);
						f.nodes.addAll(m.nodes);
					}
				}

			});

			return f;

		}, b -> allFrameHash()));
	}

	private Object allFrameHash() {
		if (allFrameHashAt== RunLoop.tick) return allFrameHash;
		allFrameHash = breadthFirst(both()).filter(x -> x.properties.has(Box.frame))
			    .reduce(0L, (w, frame) -> 31 * w + allFrameHashSalt+(frame.properties.isTrue(Mouse.isSelected, false) ? 1 : 0) + frame.properties.get(Box.frame).hashCode(), (x, y) -> 31 * x + y);

		allFrameHashAt = RunLoop.tick;
		return allFrameHash;
	}

	@Override
	public Mouse.Dragger onMouseDown(Window.Event<Window.MouseState> e, int button) {
		if (button == 0) return button0(e);
		return null;
	}

	protected Mouse.Dragger button0(Window.Event<Window.MouseState> e) {
		if (!e.after.keyboardState.keysDown.contains(Glfw.GLFW_KEY_G)) return null;

		Optional<Drawing> drawing = this.find(Drawing.drawing, both()).findFirst();
		Vec2 point = drawing.map(x -> x.windowSystemToDrawingSystem(new Vec2(e.after.x, e.after.y)))
			    .orElseThrow(() -> new IllegalArgumentException(" cant mouse around something without drawing support (to provide coordinate system)"));

		Optional<Box> hit = breadthFirst(both()).filter(b -> frame(b) != null).filter(b -> frame(b).intersects(point))
			    .sorted((a, b) -> Float.compare(order(frame(a)), order(frame(b)))).findFirst();

		if (hit.isPresent()) {
			e.properties.put(Window.consumed, true);

			Box origin = hit.get();

			return new Mouse.Dragger() {
				@Override
				public boolean update(Window.Event<Window.MouseState> e, boolean termination) {

					Optional<Drawing> drawing = Dispatch.this.find(Drawing.drawing, both()).findFirst();
					Vec2 point = drawing.map(x -> x.windowSystemToDrawingSystem(new Vec2(e.after.x, e.after.y)))
						    .orElseThrow(() -> new IllegalArgumentException(" cant mouse around something without drawing support (to provide coordinate system)"));

					Optional<Box> hit = breadthFirst(both()).filter(b -> frame(b) != null).filter(b -> frame(b).intersects(point))
						    .sorted((a, b) -> Float.compare(order(frame(a)), order(frame(b)))).findFirst();

					if (hit.isPresent()) {
						showCompleteDrag(origin, hit.get());
						if (termination) {
							completeDrag(origin, hit.get());
						}
					} else {
						showIncompleteDrag(origin, point);
						if (termination) {
							Dispatch.this.properties.removeFromMap(frameDrawing, "__ongoingDrag__");
						}
					}


					return true;
				}


			};
		}
		return null;
	}

	protected void showIncompleteDrag(Box start, Vec2 to) {
		this.properties.putToMap(frameDrawing, "__ongoingDrag__", (box) -> {

			Rect f1 = frame(start);

			FLine m = arc(f1, new Rect(to.x - 10, to.y - 10, 20, 20), true);

			boolean selected = false;

			float o = -0.5f;

			m.attributes.put(StandardFLineDrawing.color, new Vec4(0, 0, 0, 0.4f));

			return m;
		});

		Drawing.dirty(this);
	}

	protected float order(Rect r) {
		return Math.abs(r.w) + Math.abs(r.h);
	}

	protected void completeDrag(Box start, Box box) {
		this.properties.removeFromMap(frameDrawing, "__ongoingDrag__");

		start.connect(box);
		allFrameHashAt = 0;
		allFrameHash = 0;
		allFrameHashSalt++;

		Drawing.dirty(start);
	}

	protected void showCompleteDrag(Box start, Box end) {
		this.properties.putToMap(frameDrawing, "__ongoingDrag__", (box) -> {

			Rect f1 = frame(start);
			Rect f2 = frame(end);

			FLine m = arc(f1, f2, true);

			m.attributes.put(StandardFLineDrawing.color, new Vec4(0, 0, 0, 0.4f));

			return m;
		});
		Drawing.dirty(this);
	}

	private FLine arc(Rect f1, Rect f2, boolean selected) {

		float inset = 0;
		Vec2[] a = new Vec2[]{new Vec2(f1.x + inset, f1.y + inset), new Vec2(f1.x + f1.w - inset, f1.y + f1.h - inset)};
		inset = 15;
		Vec2[] b = new Vec2[]{new Vec2(f2.x + f2.w - inset, f2.y + inset), new Vec2(f2.x + f2.w - inset, f2.y + f2.h - inset), new Vec2(f2.x + inset, f2.y + f2.h - inset)};

		float d = Float.POSITIVE_INFINITY;
		int[] da = {0, 0};
		for (int x = 0; x < a.length; x++)
			for (int y = 0; y < b.length; y++) {
				float z = (float) a[x].distanceFrom(b[y]);
				if (z < d) {
					d = z;
					da[0] = x;
					da[1] = y;
				}
			}

		FLine f = new FLine();
		f.moveTo(a[da[0]].x, a[da[0]].y);

		Vec2 normal = Vec2.sub(b[da[1]], a[da[0]], new Vec2());

		Vec2 tan = new Vec2(-normal.y, normal.x).normalise().scale(-d * 0.15f);
//		if (normal.x > 0) tan.scale(-1);

		Vec2 midPoint1 = new Vec2(a[da[0]].x+b[da[1]].x, a[da[0]].y+b[da[1]].y).scale(1/2f).translate(tan.x, tan.y);
		Vec2 midPoint2 = new Vec2(a[da[0]].x+b[da[1]].x, a[da[0]].y+b[da[1]].y).scale(1/2f).translate(-tan.x, -tan.y);

		float d1 = 0;
		float d2 = 0;
		for(Vec2 vv : a) d1+=vv.distanceFrom(midPoint1);
		for(Vec2 vv : a) d2+=vv.distanceFrom(midPoint2);
		for(Vec2 vv : b) d1+=vv.distanceFrom(midPoint1);
		for(Vec2 vv : b) d2+=vv.distanceFrom(midPoint2);

		if (d2>d1)
			tan.scale(-1);

		Vec2 c1 = new Vec2(a[da[0]].x + normal.x * 1 / 3f + tan.x, a[da[0]].y + normal.y * 1 / 3f + tan.y);
		Vec2 c2 = new Vec2(a[da[0]].x + normal.x * 2 / 3f + tan.x, a[da[0]].y + normal.y * 2 / 3f + tan.y);

		f.moveTo(a[da[0]].x, a[da[0]].y);
		f.cubicTo(c1.x, c1.y, c2.x, c2.y, b[da[1]].x, b[da[1]].y);

		float o = 0.5f;

		GeneralPath shape = FLinesAndJavaShapes.flineToJavaShape(f);
		Area r1 = new Area(new BasicStroke(1.5f).createStrokedShape(shape));

		FLine m = new FLine();
		m.circle(a[da[0]].x, a[da[0]].y, 5);
		m.circle(b[da[1]].x, b[da[1]].y, 5);
		Area r2 = new Area(FLinesAndJavaShapes.flineToJavaShape(m));

		r1.add(r2);
		f = FLinesAndJavaShapes.javaShapeToFLine(r1);

		f.attributes.put(fillColor, selected ? new Vec4(0, 0, 0, 1.0f * o) : new Vec4(0, 0, 0, 0.25f * o));
		f.attributes.put(strokeColor, selected ? new Vec4(0, 0, 0, 0.15f * o) : new Vec4(0, 0, 0, 0.1f * o));
		f.attributes.put(thicken, new BasicStroke(selected ? 2.25f : 1.5f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));

		f.attributes.put(filled, true);
		f.attributes.put(stroked, true);

		return f;
	}

	protected Rect frame(Box hitBox) {
		return hitBox.properties.get(frame);
	}

}
