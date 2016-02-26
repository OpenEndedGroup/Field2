package fieldbox.boxes;

import field.graphics.Window;
import field.linalg.Vec2;
import field.utility.Dict;
import field.utility.IdempotencyMap;
import field.utility.Rect;

import java.util.Map;

/**
 * Entry-point for File drops from GLFW into Field
 */
public class Drops {

	static public final Dict.Prop<Map<String, OnDrop>> onDrop = new Dict.Prop<>("onDrop").type()
											    .toCannon().autoConstructs(() -> new IdempotencyMap<OnDrop>(OnDrop.class));

	public void dispatch(Box root, Window.Event<Window.Drop> drop) {

		Box startAt = Intersects.startAt(drop.after.mouseState, root);

		System.out.println(" drop start at is :"+startAt);

		startAt.find(onDrop, startAt.upwards()).flatMap(x -> x.values()
								      .stream())
		       .forEach(x -> x.onDrop(drop));

		if (!drop.properties.isTrue(Window.consumed, false))
			startAt.find(onDrop, startAt.downwards())
			       .flatMap(x -> x.values().stream())
			       .forEach(x -> x.onDrop(drop));
	}

	private boolean intersectsWith(Box x, Vec2 position) {
		Rect f = x.properties.get(Box.frame);
		if (f == null) return false;
		return f.intersects(position.get());
	}

	private Box selection(Box root) {
		return root.breadthFirst(root.both())
			   .filter(x -> x.properties.isTrue(Mouse.isSelected, false) && x.properties.has(Box.frame) && x.properties.has(Box.name) && !x.properties.isTrue(Mouse.isSticky, false))
			   .findFirst()
			   .orElse(root);
	}

	public interface OnDrop {
		void onDrop(Window.Event<Window.Drop> drop);
	}


}
