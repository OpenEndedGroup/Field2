package fieldbox.boxes.plugins;

import field.graphics.FLine;
import field.graphics.FLinesAndJavaShapes;
import field.graphics.StandardFLineDrawing;
import field.graphics.Window;
import field.linalg.Vec2;
import field.linalg.Vec3;
import field.linalg.Vec4;
import field.utility.Dict;
import field.utility.Pair;
import field.utility.Rect;
import fieldbox.boxes.*;
import fieldbox.io.IO;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.util.ArrayList;
import java.util.Optional;

import static field.graphics.StandardFLineDrawing.*;
import static fieldbox.boxes.FLineDrawing.frameDrawing;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_G;

/**
 * Adds: Hold down G to change the box-graph network. Also draws the current graph topology.
 * <p>
 * This is like Topology.java, but simpler, since the connections that it produces are not reified as new boxes.
 */
public class Dispatch extends Box implements Mouse.OnMouseDown, Mouse.OnMouseMove, Keyboard.OnKeyDown {

	static public final Dict.Prop<Boolean> shyConnections = new Dict.Prop<>("shyConnections").type()
		.toCanon()
		.doc("boxes with this property set to true only show connections when they are selected").set(IO.persistent, true);

	private final Box root;
	boolean on = false;

	long allFrameHashSalt = 0;

	public Dispatch(Box root) {
		this.root = root;
		this.properties.put(Planes.plane, "__always__");
		this.properties.putToMap(Mouse.onMouseDown, "__dispatch__", this);
		this.properties.putToMap(Mouse.onMouseMove, "__dispatch__", this);
		this.properties.putToMap(Keyboard.onKeyDown, "__dispatch__", this);

		this.properties.putToMap(FLineDrawing.frameDrawing, "__allTopology__", FrameChangedHash.getCached(this,
			(b, was) -> {

				FLine f = new FLine();

				breadthFirst(both()).filter(x -> x.properties.has(Box.frame))
					.filter(x -> !x.properties.isTrue(
						Box.hidden, false))
					.forEach(x -> {
						if (Planes.on(root, x) < 1) return;
						if (x.properties.isTrue(shyConnections, false))
							return;

						int n = 0;
						for (Box x2 : new ArrayList<>(x.children())) {
							if (x2.properties.has(Box.frame) && Planes.on(root, x2) >= 1 && (!x2.properties.isTrue(shyConnections, false))) {


								ensureBox(x, x2);

								if (n++ > 25)
									break; // TODO: indicate that we're giving up making boxes for things with more than 25 children
							}

						}

					});

				return f;

//		}, b -> allFrameHash()));
			}, () -> allFrameHashSalt));
		this.properties.putToMap(FLineDrawing.frameDrawing, "__allTopologySelected__", FrameChangedHash.getCached(this, (b, was) -> {

			FLine f = new FLine();

			breadthFirst(both()).filter(x -> x.properties.has(Box.frame))
				.filter(x -> !x.properties.isTrue(Box.hidden, false))
				.filter(x -> x.properties.isTrue(Mouse.isSelected, false))
				.forEach(x -> {
					if (x.disconnected) return;
					if (Planes.on(root, x) < 1) return;
					if (x instanceof DispatchBox) return;

					int n = 0;
					for (Box x2 : new ArrayList<>(x.children())) {
						if (x2 instanceof DispatchBox) continue;
						if (x2.properties.has(Box.frame) && x2.properties.has(Box.name) && x2.properties.get(Box.name).trim().length() > 0 && !x2.disconnected && Planes.on
							(root, x2) >= 1 && !x2.properties.isTrue(Box.hidden, false) && (x2.properties.isTrue(Mouse.isSelected, false) || !x2.properties.isTrue
							(shyConnections, false))) {
							float d1 = x.properties.getFloat(Box.depth, 0f);
							float d2 = x2.properties.getFloat(Box.depth, 0f);
							FLine m = arc(x.properties.get(Box.frame), x2.properties.get(Box.frame), d1, d2, true).first;
							if (f.nodes.size() == 0) f.attributes.putAll(m.attributes);
							f.nodes.addAll(m.nodes);
							if (n++ > 10)
								break; // TODO: indicate that we're giving up making boxes for things with more than 10 children
						}
					}

					for (Box x2 : new ArrayList<>(x.parents())) {
						if (x2 instanceof DispatchBox) continue;
						if (x2.properties.has(Box.frame) && x2.properties.has(Box.name) && x2.properties.get(Box.name).trim().length() > 0 && !x2.disconnected && Planes.on
							(root, x2) >= 1 && !x2.properties.isTrue(Box.hidden, false) && (x2.properties.isTrue(Mouse.isSelected, false) || !x2.properties.isTrue
							(shyConnections, false))) {
							float d1 = x.properties.getFloat(Box.depth, 0f);
							float d2 = x2.properties.getFloat(Box.depth, 0f);
							FLine m = arc(x2.properties.get(Box.frame), x.properties.get(Box.frame), d2, d1, true).first;
							if (f.nodes.size() == 0) f.attributes.putAll(m.attributes);
							f.nodes.addAll(m.nodes);
							if (n++ > 10)
								break; // TODO: indicate that we're giving up making boxes for things with more than 10 children
						}
					}
				});

			return f;

		}, () -> allFrameHashSalt));
	}


	@Override
	public Mouse.Dragger onMouseDown(Window.Event<Window.MouseState> e, int button) {
		if (button == 0) return button0(e);
		return null;
	}

	@Override
	public Keyboard.Hold onKeyDown(Window.Event<Window.KeyboardState> e, int key) {
//		System.out.println(" key down ");
		if (!e.after.keysDown.contains(GLFW_KEY_G)) {
			if (this.properties.removeFromMap(frameDrawing, "__ongoingDrag__") != null | this.properties.removeFromMap(frameDrawing, "__ongoingDrag__2") != null)
				Drawing.dirty(this);
			return null;
		}

		if (e.after.keysDown.equals(e.before.keysDown)) {
//			System.out.println(" -- nothing changed ");
			return null;
		}

		if (e.after.mouseState.buttonsDown.size() > 0) return null;

		Vec2 point = new Vec2(e.after.mouseState.mx, e.after.mouseState.my);
		Optional<Box> hit = breadthFirst(both()).filter(b -> frame(b) != null)
			.filter(x -> !x.properties.isTrue(Box.hidden, false))
			.filter(b -> frame(b).intersects(point))
			.sorted((a, b) -> Float.compare(order(frame(a)), order(frame(b))))
			.findFirst();

		if (!hit.isPresent()) {
//			System.out.println(" remove, no hit");
			if (this.properties.removeFromMap(frameDrawing, "__ongoingDrag__") != null | this.properties.removeFromMap(frameDrawing, "__ongoingDrag__2") != null)
				Drawing.dirty(this);
			return null;
		}

		showIncompleteDrag(hit.get(), point, true);

		return (e1, termination) -> {
			if (termination)
			if (this.properties.removeFromMap(frameDrawing, "__ongoingDrag__") != null | this.properties.removeFromMap(frameDrawing, "__ongoingDrag__2") != null)
				Drawing.dirty(this);
			return !termination;
		};
	}

	@Override
	public Mouse.Dragger onMouseMove(Window.Event<Window.MouseState> e) {

		if (!e.after.keyboardState.keysDown.contains(GLFW_KEY_G)) {
			if (this.properties.removeFromMap(frameDrawing, "__ongoingDrag__") != null | this.properties.removeFromMap(frameDrawing, "__ongoingDrag__2") != null)
				Drawing.dirty(this);
			return null;
		}

		if (e.after.buttonsDown.size() > 0) return null;

		Vec2 point = new Vec2(e.after.mx, e.after.my);
		Optional<Box> hit = breadthFirst(both()).filter(b -> frame(b) != null)
			.filter(x -> !x.properties.isTrue(Box.hidden, false))
			.filter(b -> frame(b).intersects(point))
			.sorted((a, b) -> Float.compare(order(frame(a)), order(frame(b))))
			.findFirst();

		if (!hit.isPresent()) {
			if (this.properties.removeFromMap(frameDrawing, "__ongoingDrag__") != null | this.properties.removeFromMap(frameDrawing, "__ongoingDrag__2") != null)
				Drawing.dirty(this);
			return null;
		}

		showIncompleteDrag(hit.get(), point, true);


		return null;
	}

	protected Mouse.Dragger button0(Window.Event<Window.MouseState> e) {
		if (!e.after.keyboardState.keysDown.contains(GLFW_KEY_G)) return null;

		Vec2 point = new Vec2(e.after.mx, e.after.my);

		Optional<Box> hit = breadthFirst(both()).filter(b -> frame(b) != null)
			.filter(x -> !x.properties.isTrue(Box.hidden, false))
			.filter(b -> frame(b).intersects(point))
			.sorted((a, b) -> Float.compare(order(frame(a)), order(frame(b))))
			.findFirst();

		if (hit.isPresent()) {
			e.properties.put(Window.consumed, true);

			Box origin = hit.get();

			return new Mouse.Dragger() {
				@Override
				public boolean update(Window.Event<Window.MouseState> e, boolean termination) {

					Vec2 point = new Vec2(e.after.mx, e.after.my);

					Optional<Box> hit = breadthFirst(both()).filter(x -> !x.properties.isTrue(Box.hidden, false))
						.filter(b -> frame(b) != null)
						.filter(b -> frame(b).intersects(point))
						.sorted((a, b) -> Float.compare(order(frame(a)), order(frame(b))))
						.findFirst();

					if (hit.isPresent() && hit.get() != origin) {
						showCompleteDrag(origin, hit.get());
						if (termination) {
							completeDrag(origin, hit.get());
						}
					} else {
						showIncompleteDrag(origin, point, false);
						if (termination) {
							Dispatch.this.properties.removeFromMap(frameDrawing, "__ongoingDrag__");
						}
					}


					return true;
				}


			};
		}
		if (this.properties.removeFromMap(frameDrawing, "__ongoingDrag__") != null | this.properties.removeFromMap(frameDrawing, "__ongoingDrag__2") != null)
			Drawing.dirty(this);

		return null;
	}

	protected void showIncompleteDrag(Box start, Vec2 to, boolean speculative) {
		if (!speculative)
			this.properties.putToMap(frameDrawing, "__ongoingDrag__", (box) -> {

				Rect f1 = frame(start);
				float d1 = start.properties.getFloat(depth, 0f);

				FLine m = arc(f1, new Rect(to.x - 10, to.y - 10, 20, 20), d1, 0, true).first;

				boolean selected = false;

				float o = -0.5f;

				m.attributes.put(StandardFLineDrawing.color, new Vec4(0, 0, 0, speculative ? 0.2f : 0.4f));

				return m;
			});
		this.properties.putToMap(frameDrawing, "__ongoingDrag__2", (box) -> {

			Rect f1 = frame(start).duplicate();
			f1 = f1.inset(-10);
			FLine m = new FLine().rect(f1);

			m.attributes.put(StandardFLineDrawing.color, new Vec4(1, 1, 1, speculative ? 0.2f : 0.4f));

			return m;
		});

		Drawing.dirty(this);
	}

	protected float order(Rect r) {
		return Math.abs(r.w) + Math.abs(r.h);
	}

	protected void completeDrag(Box start, Box box) {
		this.properties.removeFromMap(frameDrawing, "__ongoingDrag__");
		this.properties.removeFromMap(frameDrawing, "__ongoingDrag__2");

		ArrayList<Box> s = new ArrayList<>(box.parents());
		box.parents.clear();
		start.connect(box);
		box.parents.addAll(s);

		allFrameHashSalt++;

		Drawing.dirty(start);

		ensureBox(start, box);
	}

	private void ensureBox(Box start, Box box) {
		if ((start instanceof DispatchBox) || (box instanceof DispatchBox)) return;

		if (!root.breadthFirst(root.both()).filter(x -> x instanceof DispatchBox).filter(x -> ((DispatchBox) x).head() == start && ((DispatchBox) x).tail() == box).findAny().isPresent()) {
			if (!start.disconnected && !box.disconnected) {
				DispatchBox db = new DispatchBox(start, box);
				root.connect(db);
			}
		}
	}

	protected void showCompleteDrag(Box start, Box end) {
		this.properties.putToMap(frameDrawing, "__ongoingDrag__", (box) -> {

			Rect f1 = frame(start);
			Rect f2 = frame(end);

			float d1 = start.properties.getFloat(depth, 0f);
			float d2 = end.properties.getFloat(depth, 0f);

			FLine m = arc(f1, f2, d1, d2, true).first;

			m.attributes.put(StandardFLineDrawing.color, new Vec4(0, 0, 0, 0.4f));

			return m;
		});
		Drawing.dirty(this);
	}

	static protected Pair<FLine, Vec3> arc(Rect f1, Rect f2, float depth1, float depth2, boolean selected) {

//		if (false)
//		{
//			FLine f = new FLine();
//			f.moveTo(f1.x+f1.w/2, f1.y+f1.h/2);
//			f.lineTo(f2.x+f2.w/2, f2.y+f2.h/2);
//
//			f.attributes.put(fillColor, selected ? new Vec4(1, 1, 1, 0.5) : new Vec4(0.0, 0.0, 0.0, 0.05f));
//			f.attributes.put(strokeColor, selected ? new Vec4(1, 1, 1, 0.5) : new Vec4(0.0, 0.0, 0.0, 0.05f));
//			f.attributes.put(color, selected ? new Vec4(1, 1, 0, 0.5) : new Vec4(0.0, 0.0, 0.0, 0.05f));
//
//			if (!selected)
//				f.attributes.put(thicken, new BasicStroke(selected ? 13.25f : 5.5f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));
//			if (selected) f.attributes.put(filled, true);
//
//			return new Pair<>(f, new Vec2(f1.x+f1.w/2+f2.x+f2.w/2, f1.y+f1.h/2+f2.y+f2.h/2).mul(0.5));
//		}

		float inset = 0;
		Vec2[] a = new Vec2[]{new Vec2(f1.x + inset, f1.y + inset), new Vec2(f1.x + f1.w - inset, f1.y + f1.h - inset), new Vec2(f1.x + f1.w - inset, f1.y + inset), new Vec2(f1.x + inset,
			f1.y + f1.h - inset)};
		inset = 15;
//		Vec2[] b = new Vec2[]{new Vec2(f2.x + f2.w - inset, f2.y + inset), new Vec2(f2.x + f2.w - inset, f2.y + f2.h - inset), new Vec2(f2.x + inset, f2.y + f2.h - inset), new Vec2(f2.x +
// inset, f2.y + inset)};
		Vec2[] b = new Vec2[]{new Vec2(f2.x + f2.w / 2, f2.y + f2.h / 2 - inset), new Vec2(f2.x + f2.w / 2, f2.y + f2.h / 2 + inset)};//, new Vec2(f2.x + inset, f2.y + f2.h - inset), new
		// Vec2(f2.x + inset, f2.y + inset)};

		float d = Float.POSITIVE_INFINITY;
		int[] da = {0, 0};
		for (int x = 0; x < a.length; x++)
			for (int y = 0; y < b.length; y++) {
				float z = (float) a[x].distance(b[y]);
				if (z < d) {
					d = z;
					da[0] = x;
					da[1] = y;
				}
			}

		FLine f = new FLine();

		Vec2 normal = Vec2.sub(b[da[1]], a[da[0]], new Vec2());

		Vec2 tan = new Vec2(-normal.y, normal.x).normalize()
			.mul(-d * 0.15f);

//		if (normal.x > 0) tan.mul(-1);

		Vec2 midPoint1 = new Vec2(a[da[0]].x + b[da[1]].x, a[da[0]].y + b[da[1]].y).mul(1 / 2f)
			.add(tan.x, tan.y);
		Vec2 midPoint2 = new Vec2(a[da[0]].x + b[da[1]].x, a[da[0]].y + b[da[1]].y).mul(1 / 2f)
			.add(-tan.x, -tan.y);

		float d1 = 0;
		float d2 = 0;
		for (Vec2 vv : a) d1 += vv.distance(midPoint1);
		for (Vec2 vv : a) d2 += vv.distance(midPoint2);
		for (Vec2 vv : b) d1 += vv.distance(midPoint1);
		for (Vec2 vv : b) d2 += vv.distance(midPoint2);

//		if (d2 > d1) tan.mul(-1);

		Vec2 c1 = new Vec2(a[da[0]].x + normal.x * 1 / 3f + tan.x, a[da[0]].y + normal.y * 1 / 3f + tan.y);
		Vec2 c2 = new Vec2(a[da[0]].x + normal.x * 2 / 3f + tan.x, a[da[0]].y + normal.y * 2 / 3f + tan.y);

		f.moveTo(a[da[0]].x, a[da[0]].y, depth1);
		f.cubicTo(c1.x, c1.y, (depth1 * 2 + depth2) / 3, c2.x, c2.y, (depth1 * 1 + depth2 * 2) / 3, b[da[1]].x, b[da[1]].y, depth2);

		FLinesAndJavaShapes.Cursor c = new FLinesAndJavaShapes.Cursor(f, 0.5f);
		c.setT(0.5f);
		Vec2 midpoint = c.position().toVec2();


		c.setD(c.lengthD() / 2);
		Vec3 at = c.position();
		Area arrowA = null;

		Vec3 tang = c.tangentForward();
		if (tang != null) {
			tang.normalize();
			Vec3 norm = new Vec3(-tang.y, tang.x, tang.z);


//			FLine arrow = new FLine();
			FLine arrow = f;
			float sz = 5;
			float sz2 = -5;

			arrow.moveTo(at.x + norm.x * sz + tang.x * sz2, at.y + norm.y * sz + tang.y * sz2, (depth1 + depth2) / 2);
			arrow.lineTo(at.x, at.y, (depth1 + depth2) / 2);
			arrow.lineTo(at.x - norm.x * sz + tang.x * sz2, at.y - norm.y * sz + tang.y * sz2, (depth1 + depth2) / 2);


//			arrowA = new Area(new BasicStroke(5f).createStrokedShape(FLinesAndJavaShapes.flineToJavaShape(arrow)));
		}
//		Shape shape = FLinesAndJavaShapes.flineToJavaShape(f);
//		Area r1 = new Area(new BasicStroke(1.5f).createStrokedShape(shape));
//		{
//			FLine m = new FLine();
//			m.circle(a[da[0]].x, a[da[0]].y, 5);
//			Area r2 = new Area(FLinesAndJavaShapes.flineToJavaShape(m));
//
//			r1.add(r2);
//		}
//		{
//			FLine m = new FLine();
//			m.circle(b[da[1]].x, b[da[1]].y, 5);
//			Area r2 = new Area(FLinesAndJavaShapes.flineToJavaShape(m));
//
//			r1.add(r2);
//		}
//
//		if (arrowA != null) r1.add(arrowA);
//		f = FLinesAndJavaShapes.javaShapeToFLineFlat(r1, 0.1f, 3);

		f.attributes.put(fillColor, selected ? new Vec4(1, 1, 1, 0.35) : new Vec4(1, 0.5, 0.0, 0.25f));
		f.attributes.put(strokeColor, selected ? new Vec4(1, 1, 1, 0.35) : new Vec4(1.0, 0.5, 0.0, 0.25f));
		f.attributes.put(color, selected ? new Vec4(1, 1, 1, 0.35) : new Vec4(1.0, 0.5, 0.0, 0.25f));


//		if (!selected)
		f.attributes.put(thicken, new BasicStroke(selected ? 1.5f : 1.5f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));

		FLine fFinal = FLinesAndJavaShapes.javaShapeToFLine(FLinesAndJavaShapes.flineToJavaShape(f), f, new AffineTransform());

		fFinal.attributes.putAll(f.attributes);
		fFinal.attributes.remove(thicken);

		fFinal.circle(a[da[0]].x, a[da[0]].y, 5, depth1);
		fFinal.circle(b[da[1]].x, b[da[1]].y, 5, depth2);

		fFinal.attributes.put(filled, true);

		//		if (selected) f.attributes.put(filled, true);

		return new Pair<>(fFinal, new Vec3(midpoint.x, midpoint.y, (depth1 + depth2) / 2));
	}

	protected Rect frame(Box hitBox) {
		return hitBox.properties.get(frame);
	}

}
