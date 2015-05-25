package fieldbox.boxes.plugins;

import field.graphics.util.KeyEventMapping;
import field.utility.Dict;
import field.utility.IdempotencyMap;
import fieldbox.boxes.Box;
import fieldbox.boxes.Intersects;
import fieldbox.boxes.Keyboard;

import java.util.LinkedHashSet;
import java.util.Set;

public class KeyboardShortcuts extends Box {

	static protected final Dict.Prop<Boolean> __deltwith = new Dict.Prop<>("__deltwith");

	static public final Dict.Prop<IdempotencyMap<FunctionOfBox>> shortcut = new Dict.Prop<>("shortcut").type()
													   .toCannon()
													   .doc("A map for keyboard shortcuts. For example _.shortcut.ctrl_f = function(box){...}. In order to qualify as a keyboard shortcut, alt, ctrl or meta (aka command) must be pressed")
													   .autoConstructs(() -> new IdempotencyMap<>(Box.FunctionOfBox.class));


	public KeyboardShortcuts(Box root) {
		this.properties.getOrConstruct(shortcut); // make appear in autocomplete
		this.properties.putToMap(Keyboard.onKeyDown, "__shortcuts__", (k, e) -> {

			if (k.properties.isTrue(__deltwith, false)) {
				return null;
			}

			boolean alt = k.after.isAltDown();
			boolean ctrl = k.after.isControlDown();
			boolean shift = k.after.isShiftDown();
			boolean meta = k.after.isSuperDown();

			if (!(alt || ctrl || meta)) return null;

			// could be a keyboard shortcut;

			Box start = Intersects.startAt(k.after.mouseState, root);
			IdempotencyMap<FunctionOfBox> all = start.find(shortcut, start.upwards())
								 .reduce(new IdempotencyMap<FunctionOfBox>(FunctionOfBox.class), (a1, a2) -> {
									 IdempotencyMap<FunctionOfBox> q = new IdempotencyMap<>(FunctionOfBox.class);
									 q.putAll(a1);
									 q.putAll(a2);
									 return q;
								 });

			Set<Integer> c = new LinkedHashSet<>(k.after.keysDown);
			c.removeAll(k.before.keysDown);

			if (c.size() != 1) return null;


			all.entrySet()
			   .stream()
			   .filter(x -> {


				   if (!match(alt, "alt", x.getKey())) return false;
				   if (!match(ctrl, "ctrl", x.getKey())) return false;
				   if (!match(shift, "shift", x.getKey())) return false;
				   if (!(match(meta, "meta", x.getKey()) | match(meta, "command", x.getKey()))) return false;

				   for (int cc : c) {
					   String q = KeyEventMapping.lookup(cc);
					   if (q == null) return false;
					   String s = q.toLowerCase()
						       .replace("glfw_key_", "");

					   if (!x.getKey()
						 .contains("_" + s)) return false;
				   }

				   return true;

			   })
			   .map(x -> x.getValue())
			   .forEach(x -> {
				   k.properties.put(__deltwith, true);
				   x.apply(start);
			   });

			return null;
		});
	}

	private boolean match(boolean should, String substring, String inside) {
		return inside.contains(substring) == should;
	}


}
