package fieldbox.boxes.plugins;

import com.badlogic.jglfw.Glfw;
import field.graphics.FLine;
import field.graphics.Window;
import field.linalg.Vec2;
import field.linalg.Vec4;
import field.utility.Rect;
import fieldbox.boxes.Box;
import fieldbox.boxes.Drawing;
import fieldbox.boxes.FLineDrawing;
import fieldbox.boxes.Mouse;

import java.awt.*;
import java.util.Optional;

/**
 * Adds: Hold down T to connect elements with other elements.
 * <p>
 * The elements added are TopologyBox, which are subclasses of Box that have a default drawer that draws arcs between
 * endpoints. This is more proof of concept that anything else. The interpretation of this "topology" is completely up
 * to the code in the boxes themselves.
 */
public class Topology extends Box implements Mouse.OnMouseDown {

	private final Box root;
	boolean on = false;

	public Topology(Box root) {
		this.root = root;
		this.properties.putToList(Mouse.onMouseDown, this);
	}

	@Override
	public Mouse.Dragger onMouseDown(Window.Event<Window.MouseState> e, int button) {
		if (button == 0) return button0(e);
		return null;
	}

	protected Mouse.Dragger button0(Window.Event<Window.MouseState> e) {
		if (!e.after.keyboardState.keysDown.contains(Glfw.GLFW_KEY_T)) return null;

		Optional<Drawing> drawing = this.find(Drawing.drawing, both()).findFirst();
		Vec2 point = drawing.map(x -> x.windowSystemToDrawingSystem(new Vec2(e.after.x, e.after.y))).orElseThrow(() -> new IllegalArgumentException(" cant mouse around something without drawing support (to provide coordinate system)"));

		Optional<Box> hit = breadthFirst(both()).filter(b -> frame(b) != null).filter(b -> frame(b).intersects(point)).sorted((a, b) -> Float.compare(order(frame(a)), order(frame(b)))).findFirst();

		if (hit.isPresent()) {
			e.properties.put(Window.consumed, true);

			Box origin = hit.get();

			return new Mouse.Dragger() {
				@Override
				public boolean update(Window.Event<Window.MouseState> e, boolean termination) {

					Optional<Drawing> drawing = Topology.this.find(Drawing.drawing, both()).findFirst();
					Vec2 point = drawing.map(x -> x.windowSystemToDrawingSystem(new Vec2(e.after.x, e.after.y))).orElseThrow(() -> new IllegalArgumentException(" cant mouse around something without drawing support (to provide coordinate system)"));

					Optional<Box> hit = breadthFirst(both()).filter(b -> frame(b) != null).filter(b -> frame(b).intersects(point)).sorted((a, b) -> Float.compare(order(frame(a)), order(frame(b)))).findFirst();

					if (hit.isPresent()) {
						showCompleteDrag(origin, hit.get());
						if (termination) {
							completeDrag(origin, hit.get());
						}
					} else {
						showIncompleteDrag(origin, point);
						if (termination) {
							Topology.this.properties.removeFromMap(FLineDrawing.frameDrawing, "__ongoingDrag__");
						}
					}


					return true;
				}


			};
		}
		return null;
	}

	protected void showIncompleteDrag(Box start, Vec2 to) {
		System.out.println(" incomplete drag ");
		this.properties.putToMap(FLineDrawing.frameDrawing, "__ongoingDrag__", (box) -> {

			Rect f1 = frame(start);

			FLine m = new FLine();
			m.moveTo(f1.x, f1.y);
			m.lineTo(to.x, to.y);

			m.attributes.put(FLineDrawing.thicken, new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			m.attributes.put(FLineDrawing.strokeColor, new Vec4(1, 0, 0, 1f));

			return m;
		});
		Drawing.dirty(this);
	}

	protected float order(Rect r) {
		return Math.abs(r.w) + Math.abs(r.h);
	}

	protected void completeDrag(Box start, Box box) {
		System.out.println(" Drag completed ");
		this.properties.removeFromMap(FLineDrawing.frameDrawing, "__ongoingDrag__");
		Drawing.dirty(this);
	}

	protected void showCompleteDrag(Box start, Box end) {
		System.out.println(" complete drag ");
		this.properties.putToMap(FLineDrawing.frameDrawing, "__ongoingDrag__", (box) -> {

			Rect f1 = frame(start);
			Rect f2 = frame(end);

			FLine m = new FLine();
			m.moveTo(f1.x, f1.y);
			m.lineTo(f2.x, f2.y);

			m.attributes.put(FLineDrawing.thicken, new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			m.attributes.put(FLineDrawing.strokeColor, new Vec4(1, 0, 0, 1f));

			return m;
		});
		Drawing.dirty(this);
	}

	protected Rect frame(Box hitBox) {
		return hitBox.properties.get(frame);
	}

}
