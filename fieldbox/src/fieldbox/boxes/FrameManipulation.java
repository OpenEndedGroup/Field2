package fieldbox.boxes;

import com.badlogic.jglfw.Glfw;
import field.graphics.FLine;
import field.linalg.Vec2;
import field.linalg.Vec4;
import field.utility.Dict;
import field.utility.Rect;
import field.graphics.Window;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Created by marc on 3/21/14.
 */
public class FrameManipulation extends Box implements Manipulation.OnMouseDown {
	public enum DragTarget {
		translate, left, top, right, bottom;
	}

	static public final Dict.Prop<Boolean> lockWidth = new Dict.Prop<>("lockWidth").type().toCannon();
	static public final Dict.Prop<Boolean> lockHeight= new Dict.Prop<>("lockHeight").type().toCannon();
	static public final Dict.Prop<Boolean> lockX= new Dict.Prop<>("lockX").type().toCannon();
	static public final Dict.Prop<Boolean> lockY= new Dict.Prop<>("lockY").type().toCannon();


	public FrameManipulation() {
		this.properties.putToList(Manipulation.onMouseDown, this);
	}

	@Override
	public Manipulation.Dragger onMouseDown(Window.Event<Window.MouseState> e, int button) {

		System.out.println(" consumed in frame manipulation ? "+e.properties.isTrue(Window.consumed,false));

		if (e.properties.isTrue(Window.consumed,false)) return null;
		if (button == 0 || button == 1) return button0(e);
		if (button == 2) return button2(e);
		return null;
	}

	public Manipulation.Dragger button2(Window.Event<Window.MouseState> e) {
		Optional<Drawing> drawing = this.find(Drawing.drawing, both()).findFirst();
		Vec2 point = drawing.map(x -> x.windowSystemToDrawingSystem(new Vec2(e.after.x, e.after.y))).orElseThrow(() -> new IllegalArgumentException(" cant mouse around something without drawing support (to provide coordinate system)"));

		Optional<Box> hit = breadthFirst(both()).filter(b -> frame(b) != null).filter(b -> frame(b).intersects(point)).sorted((a, b) -> Float.compare(order(frame(a)), order(frame(b)))).findFirst();

		if (!hit.isPresent()) {
			return (Manipulation.Dragger) (drag, termination) -> {
				Vec2 delta = new Vec2(drag.after.dx, drag.after.dy);
				Vec2 drawingDelta = drawing.map(x -> x.windowSystemToDrawingSystemDelta(delta)).orElseThrow(() -> new IllegalArgumentException(" cant mouse around something without drawing support (to provide coordinate system)"));

				System.out.println(" drawing delta is :" + drawingDelta);

				if (drawing.isPresent()) {
					Drawing d = drawing.get();
					Vec2 t = d.getTranslation();

					System.out.println(" translation is :" + t);

					t.translate(drawingDelta.x, drawingDelta.y);
					d.setTranslation(FrameManipulation.this, t);
				}
				return true;
			};
		}
		return null;
	}

	public Manipulation.Dragger button0(Window.Event<Window.MouseState> e) {
		Optional<Drawing> drawing = this.find(Drawing.drawing, both()).findFirst();
		Vec2 point = drawing.map(x -> x.windowSystemToDrawingSystem(new Vec2(e.after.x, e.after.y))).orElseThrow(() -> new IllegalArgumentException(" cant mouse around something without drawing support (to provide coordinate system)"));

		Optional<Box> hit = breadthFirst(both()).filter(b -> frame(b) != null).filter(b -> frame(b).intersects(point)).sorted((a, b) -> Float.compare(order(frame(a)), order(frame(b)))).findFirst();

		System.out.println(" hit for event :"+e.after.x+" "+e.after.y+" "+hit);

		return hit.map(hitBox -> {
			Drawing.dirty(hitBox);

			boolean shift = e.after.keyboardState.keysDown.contains(Glfw.GLFW_KEY_LEFT_SHIFT) ||e.after.keyboardState.keysDown.contains(Glfw.GLFW_KEY_RIGHT_SHIFT);
			boolean selected = hitBox.properties.isTrue(Manipulation.isSelected, false);
			Rect originalFrame = frame(hitBox);

			if (!shift) breadthFirst(both()).forEach(x -> x.properties.remove(Manipulation.isSelected));

			hitBox.properties.put(Manipulation.isSelected, true);

			if (hitBox.properties.isTrue(Manipulation.isSelected, false) && e.after.buttonsDown.contains(0)) {
				// what kind of drag might this be?

				List<DragTarget> targets = targetsFor(frame(hitBox), point, drawing.orElseThrow(() -> new IllegalArgumentException(" cant mouse around something without drawing support (to provide coordinate system)")).getScale(), hitBox.properties.isTrue(lockWidth, false), hitBox.properties.isTrue(lockHeight, false));

				feedback(hitBox, originalFrame, originalFrame, -1);
				Drawing.dirty(hitBox);

				return (Manipulation.Dragger) (drag, termination) -> {
					Vec2 delta = new Vec2(drag.after.dx, drag.after.dy);
					Vec2 drawingDelta = drawing.map(x -> x.windowSystemToDrawingSystemDelta(delta)).orElseThrow(() -> new IllegalArgumentException(" cant mouse around something without drawing support (to provide coordinate system)"));

					breadthFirst(both()).filter(x -> x.properties.isTrue(Manipulation.isSelected, false)).forEach(x -> {
						Rect r0 = frame(x);
						Rect r = transform(x, r0, targets, drawingDelta);
						x.properties.put(Manipulation.frame, r);
						feedback(hitBox, originalFrame, r, termination ? 60 : -1);
						Drawing.dirty(hitBox);
					});

					if (termination && frame(hitBox).equals(originalFrame) && selected) {
						hitBox.properties.put(Manipulation.isSelected, false);
						Drawing.dirty(hitBox);
					}

					return true;
				};
			}
			return null;
		}).orElseGet(() -> {

			if (!e.after.buttonsDown.contains(0)) return null;
			if (e.after.keyboardState.isAltDown()) return null;

			System.out.println(" nothing hit ");

			boolean shift = e.after.keyboardState.keysDown.contains(Glfw.GLFW_KEY_LEFT_SHIFT) ||e.after.keyboardState.keysDown.contains(Glfw.GLFW_KEY_RIGHT_SHIFT);
			if (!shift) breadthFirst(both()).forEach(x -> x.properties.remove(Manipulation.isSelected));

			Map<Box, Boolean> frozenAt = new LinkedHashMap<Box, Boolean>();
			breadthFirst(both()).forEach(x -> frozenAt.put(x, x.properties.isTrue(Manipulation.isSelected, false)));

			Drawing.dirty(this);

			Vec2 downAt = new Vec2(point);

			return (Manipulation.Dragger) (drag, termination) -> {
				Vec2 delta = new Vec2(drag.after.dx, drag.after.dy);

				System.out.println(" hello ? ");

				Vec2 drawingDelta = drawing.map(x -> x.windowSystemToDrawingSystemDelta(delta)).orElseThrow(() -> new IllegalArgumentException(" cant mouse around something without drawing support (to provide coordinate system)"));
				downAt.x += drawingDelta.x;
				downAt.y += drawingDelta.y;

				if (termination) this.properties.removeFromMap(FrameDrawer.frameDrawing, "__marquee__");
				else {
					breadthFirst(both()).forEach(x -> {
						Boolean b = frozenAt.get(x);

						Rect f = frame(x);
						if (f == null) return;

						if (f.intersects(new Rect(Math.min(downAt.x, point.x), Math.min(downAt.y, point.y), Math.max(downAt.x, point.x) - Math.min(downAt.x, point.x), Math.max(downAt.y, point.y) - Math.min(downAt.y, point.y)))) {
							x.properties.put(Manipulation.isSelected, true);
						} else {
							if (b == null || !b)
								x.properties.remove(Manipulation.isSelected);
						}
					});

					this.properties.putToMap(FrameDrawer.frameDrawing, "__marquee__", (box) -> {

						FLine m = new FLine();
						m.moveTo(point.x, point.y);
						m.lineTo(point.x, downAt.y);
						m.lineTo(downAt.x, downAt.y);
						m.lineTo(downAt.x, point.y);
						m.lineTo(point.x, point.y);

						m.attributes.put(FrameDrawer.thicken, new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
						m.attributes.put(FrameDrawer.strokeColor, new Vec4(1, 1, 1,0.2f));
						m.attributes.put(FrameDrawer.fillColor, new Vec4(1, 1, 1,0.3f));
						m.attributes.put(FrameDrawer.stroked, true);
						m.attributes.put(FrameDrawer.filled, true);
						m.attributes.put(FrameDrawer.pointed, false);

						return m;
					});
				}

				Drawing.dirty(this);
				return true;
			};
		});
	}

	protected Rect frame(Box hitBox) {
		return hitBox.properties.get(Manipulation.frame);
	}

	private Rect transform(Box b, Rect r, List<DragTarget> targets, Vec2 drawingDelta) {
		for (DragTarget d : targets) {

			Rect was = r;

			switch (d) {
				case bottom:
					r = new Rect(r.x, r.y, r.w, r.h + drawingDelta.y);
					break;
				case top:
					r = new Rect(r.x, r.y + drawingDelta.y, r.w, r.h - drawingDelta.y);
					break;
				case left:
					r = new Rect(r.x + drawingDelta.x, r.y, r.w - drawingDelta.x, r.h);
					break;
				case right:
					r = new Rect(r.x, r.y, r.w + drawingDelta.x, r.h);
					break;
				case translate:
					r = new Rect(r.x + drawingDelta.x, r.y + drawingDelta.y, r.w, r.h);
					break;
			}

			if (r.w < 20) r = new Rect(r.x, r.y, 20, r.h);
			if (r.h < 20) r = new Rect(r.x, r.y, r.w, 20);

			if (b.properties.isTrue(lockWidth, false))
				r = new Rect(r.x, r.y, was.w, r.h);
			if (b.properties.isTrue(lockHeight, false))
				r = new Rect(r.x, r.y, r.w, was.h);
			if (b.properties.isTrue(lockX, false))
				r = new Rect(was.x, r.y, r.w, r.h);
			if (b.properties.isTrue(lockY, false))
				r = new Rect(r.x, was.y, r.w, r.h);
		}
		return r;

	}

	private List<DragTarget> targetsFor(Rect rect, Vec2 point, Vec2 scale, boolean lockWidth, boolean lockHeight) {
		List<DragTarget> r = new ArrayList<>();

		float inset = 10;
		if (Math.abs(point.x - rect.x) < inset) r.add(DragTarget.left);
		if (Math.abs(point.x - rect.x - rect.w) < inset && !lockWidth) r.add(DragTarget.right);
		if (Math.abs(point.y - rect.y) < inset) r.add(DragTarget.top);
		if (Math.abs(point.y - rect.y - rect.h) < inset && !lockHeight) r.add(DragTarget.bottom);
		if (r.size() == 0) r.add(DragTarget.translate);

		return r;
	}

	protected float order(Rect r) {
		return Math.abs(r.w) + Math.abs(r.h);
	}

	protected void feedback(Box b, Rect r0, Rect r, int exp) {
		if (b.properties.getFromMap(FrameDrawer.frameDrawing, "__feedback__") != null || exp == -1 || true)
			b.properties.putToMap(FrameDrawer.frameDrawing, "__feedback__", FrameDrawer.expires(box -> {
				FLine f = new FLine();
				f.attributes.put(FrameDrawer.hasText, true);
				f.moveTo(r.x + r.w / 2, r.y + r.h + 14);
				List<String> text = new ArrayList<String>();
				List<Vec4> color = new ArrayList<Vec4>();


				text.add(String.format("%.0f", r.x));
				color.add(r.x == r0.x ? new Vec4(1, 1, 1,0.5f) : new Vec4(1, 1, 1,1));
				text.add(",");
				color.add(new Vec4(1, 1, 1,0.5f));
				text.add(String.format("%.0f", r.y));
				color.add(r.y == r0.y ? new Vec4(1, 1, 1,0.5f) : new Vec4(1, 1, 1,1));
				text.add(" ");
				color.add(new Vec4(1, 1, 1,0.5f));
				text.add(String.format("%.0f", r.w));
				color.add(r.w == r0.w ? new Vec4(1, 1, 1,0.5f) : new Vec4(1, 1, 1,1));
				text.add("x");
				color.add(new Vec4(1, 1, 1,0.5f));
				text.add(String.format("%.0f", r.h));
				color.add(r.h == r0.h ? new Vec4(1, 1, 1,0.5f) : new Vec4(1, 1, 1,1));


				f.nodes.get(f.nodes.size() - 1).attributes.put(FrameDrawer.textSpans, text);
				f.nodes.get(f.nodes.size() - 1).attributes.put(FrameDrawer.textColorSpans, color);
				return f;
			}, exp));
	}

}
