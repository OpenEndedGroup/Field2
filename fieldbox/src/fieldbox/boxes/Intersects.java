package fieldbox.boxes;

import field.graphics.Window;
import field.linalg.Vec2;
import field.utility.Rect;

import java.util.Optional;

/**
 * This is becoming a pattern: event processing starts either at a mouse over'd box, or a selected box, or the root
 */
public class Intersects {

	static public Box startAt(Window.Event<Window.MouseState> e, Box root)
	{
		Optional<Drawing> drawing = root.find(Drawing.drawing, root.both())
						.findFirst();
		Vec2 point = drawing.map(x -> x.windowSystemToDrawingSystem(new Vec2(e.after.x, e.after.y)))
				    .orElseThrow(() -> new IllegalArgumentException(" can't mouse around something without drawing support (to provide coordinate system)"));

		Optional<Box> hit = root.breadthFirst(root.both()).filter(b -> frame(b) != null)
							.filter(b -> !b.properties.isTrue(Box.hidden, false))
							.filter(b -> frame(b).intersects(point))
							.sorted((a, b) -> Float.compare(order(frame(a)), order(frame(b))))
							.findFirst();

		Box startAt = hit.orElseGet(() -> root.breadthFirst(root.both())
				  .filter(x -> x.properties.isTrue(Mouse.isSelected, false) && !x.properties.isTrue(Mouse.isSticky, false))
				  .findFirst()
				  .orElseGet(() -> root));

		return startAt;

	}

	static protected Rect frame(Box hitBox) {
		return hitBox.properties.get(Box.frame);
	}
	static protected float order(Rect r) {
		return Math.abs(r.w) + Math.abs(r.h);
	}


}
