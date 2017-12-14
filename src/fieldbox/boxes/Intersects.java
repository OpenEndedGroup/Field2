package fieldbox.boxes;

import field.graphics.Window;
import field.linalg.Vec2;
import field.utility.Rect;
import fieldbox.boxes.plugins.Planes;
import fieldcef.plugins.TextEditor;

import java.util.Optional;

/**
 * This is becoming a pattern: event processing starts either at a mouse over'd box, or a selected box, or the root
 */
public class Intersects {

	static public Box startAt(Window.MouseState e, Box root) {
		Vec2 point = e == null ? null : new Vec2(e.mx, e.my);

		Optional<Box> hit = point == null ? Optional.empty() : root.breadthFirst(root.both())
			.filter(b -> frame(b) != null)
			.filter(b -> !b.properties.isTrue(Box.hidden, false))
			.filter(b -> point == null || frame(b).intersects(point))
			.filter(x -> !x.properties.isTrue(Mouse.isSticky, false))
			.filter(x -> Planes.on(root, x)>=1)
			.sorted((a, b) -> Float.compare(order(frame(a), a instanceof TextEditor), order(frame(b), b instanceof TextEditor)))
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

	static protected float order(Rect r, boolean textEditor) {
		return Math.abs(r.w) + Math.abs(r.h) - (textEditor ? 1000 : 0);
	}


}
