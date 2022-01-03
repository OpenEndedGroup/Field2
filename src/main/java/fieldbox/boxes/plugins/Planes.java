package fieldbox.boxes.plugins;

import field.linalg.Vec2;
import field.utility.Dict;
import field.utility.Rect;
import fieldbox.boxes.Box;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Function;

/**
 * Plugin for rendering the stuff _above_ root visible and editable. Perhaps this replaces "disconnected" ?
 */
public class Planes extends Box {

	static public field.utility.Dict.Prop<String> plane = new Dict.Prop<>("plane").type().toCanon().doc("comma separated list of planes that this box is a member of. A _plane_ is a group of boxes inside a document that you want to view together. By default only boxes that aren't in any plane (where this property is unset or blank) are shown");

	static public field.utility.Dict.Prop<Function<Box, Number>> selectPlane = new Dict.Prop<>("selectPlane").type().toCanon().doc("set at the root level to determine which planes are visible and interactive at any one time");

	static public double on(Box root, Box test) {
		Function<Box, Number> selector = root.properties.getOr(selectPlane, () -> null);
		if (selector == null) {
			String p = test.properties.get(plane);
			return (((p == null || p.trim().length() == 0) && test.properties.has(Box.frame) && test.properties.has(Box.name)) || (p!=null && p.trim().contains("__always__"))) ? 1 : 0;
		} else {
			return selector.apply(test).doubleValue();
		}
	}

	static public Function<Box, Number> getSelector(Box root)
	{
		return root.properties.getOr(selectPlane, () -> null);
	}

	static public double on(Box test, Function<Box, Number> selector) {
		if (selector == null) {
			String p = test.properties.get(plane);
			return (((p == null || p.trim().length() == 0) && test.properties.has(Box.frame) && test.properties.has(Box.name)) || (p!=null && p.trim().contains("__always__"))) ? 1 : 0;
		} else {
			return selector.apply(test).doubleValue();
		}
	}

	static public void clear(Box root) {
		root.properties.remove(selectPlane);
	}

	static public void show(Box root, String contains) {
		root.properties.put(selectPlane, x -> {
			String p = x.properties.get(plane);
			if (p == null) return 0;
			if (p.contains("__always__")) return 1;
			String n = x.properties.get(Box.name);
			if (n == null) return 0;
			Rect f = x.properties.get(Box.frame);
			if (f == null) return 0;
			return p.contains(contains) ? 1 : 0;
		});
	}

	static public LinkedHashMap<String, List<Box>> automaticallyLayout(Vec2 offset, Box root, String contains) {
		LinkedHashMap<String, List<Box>> m = new LinkedHashMap<>();
		root.breadthFirst(root.both()).forEach(x -> {
			String p = x.properties.get(plane);
			if (p != null && p.contains(contains)) {
				m.computeIfAbsent(p, k -> new ArrayList<Box>()).add(x);
			}
		});
		ArrayList<String> a = new ArrayList<>(m.keySet());
		a.sort(String.CASE_INSENSITIVE_ORDER);
		double y = offset.y;
		for (String aa : a) {
			List<Box> q = m.get(aa);
			double x = offset.x;
			for (Box qq : q) {
				qq.properties.put(Box.frame, new Rect(x, y, 150, 50));
				if (qq.properties.get(Box.name) == null) {
					qq.properties.put(Box.name, qq + "'");
				}
				x += 160;
			}
			y += 80;
		}
		return m;
	}

}
