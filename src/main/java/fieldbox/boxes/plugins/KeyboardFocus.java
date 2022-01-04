package fieldbox.boxes.plugins;

import field.graphics.FLine;
import field.graphics.StandardFLineDrawing;
import field.utility.Dict;
import field.utility.Rect;
import fieldbox.boxes.*;

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

/**
 * Draws a highlght for where keyboard focus is going to go
 */
public class KeyboardFocus extends Box {

	static public final Dict.Prop<KeyboardFocus> _keyboardFocus = new Dict.Prop<>("_keyboardFocus");
	private List<Box> focused = new ArrayList<Box>();

	public KeyboardFocus(Box root) {
		this.properties.put(_keyboardFocus, this);
		this.properties.put(Planes.plane, "__always__");

		this.properties.putToMap(FLineDrawing.frameDrawing, "__keyboardFocusRing__", (x) -> {

			if (focused == null) return new FLine();
			if (focused.size() == 0) return new FLine();
//			if (focused.get(focused.size() - 1) instanceof Browser) return new FLine();

			Rect f = focused.get(focused.size() - 1).properties.get(Box.frame);

			if (f == null) return new FLine();

			FLine fr = new FLine().rect(f.x, f.y, f.w, f.h);
			fr.attributes.put(StandardFLineDrawing.thicken, new BasicStroke(10));
			fr.attributes.put(StandardFLineDrawing.color, Colors.focusRing1);
			fr.attributes.put(FLineDrawing.layer, focused.get(focused.size() - 1).properties.getOr(FLineDrawing.layer, () -> "__main__"));

			Number d = focused.get(focused.size() - 1).properties.get(depth);


			if (d != null) {
				Number finalD = d;
				fr.nodes.forEach(v ->
					v.setZ(finalD.floatValue()));
			}

			return fr.bySubdividing().bySubdividing();
		});
		this.properties.putToMap(FLineDrawing.frameDrawing, "__keyboardFocusRing2__", (x) ->

		{

			if (focused == null) return new FLine();
			if (focused.size() == 0) return new FLine();
//			if (focused.get(focused.size() - 1) instanceof Browser) return new FLine();

			Rect f = focused.get(focused.size() - 1).properties.get(Box.frame);

			if (f == null) return new FLine();


			FLine fr = new FLine().rect(f.x - 5, f.y - 5, f.w + 10, f.h + 10);
			fr.attributes.put(StandardFLineDrawing.thicken, new BasicStroke(1));
			fr.attributes.put(StandardFLineDrawing.color, Colors.focusRing2);
			fr.attributes.put(FLineDrawing.layer, focused.get(focused.size() - 1).properties.getOr(FLineDrawing.layer, () -> "__main__"));
			Number d = focused.get(focused.size() - 1).properties.get(depth);


			if (d != null) {
				Number finalD = d;
				fr.nodes.forEach(v ->
					v.setZ(finalD.floatValue()));
			}

			return fr.bySubdividing().bySubdividing();
		});

		this.properties.putToMap(Callbacks.onDelete, "__keyboardFocusRing__", x ->

		{
			focused.remove(x);
			Drawing.dirty(KeyboardFocus.this);
			return null;
		});

		this.properties.putToMap(Boxes.insideRunLoop, "main.__checkfocusfordisconnect__", () ->

		{
			Iterator<Box> i = focused.iterator();
			boolean changed = false;
			while (i.hasNext()) {
				Box q = i.next();
				if (q.disconnected || (q.children().size() == 0 && q.parents().size() == 0)) {
					i.remove();
					changed = true;
				}
			}
			if (changed) {
				Drawing.dirty(KeyboardFocus.this);
			}
			return true;
		});
	}

	public boolean isFocused(Box b) {
		if (focused.size() == 0) return false;
		return focused.get(focused.size() - 1) == b;
	}


	public Optional<Box> getFocus() {
		if (focused.size() > 0) return Optional.of(focused.get(focused.size() - 1));
		return Optional.empty();
	}

	public void claimFocus(Box b) {
		List<Box> prev = new ArrayList<>(focused);

		focused.remove(b);
		focused.add(b);
		if (focused.size() > 10) focused.remove(0);

		if (!prev.equals(focused)) Drawing.dirty(this);

	}

	public void disclaimFocus(Box b) {
		if (focused.size() > 0 && focused.get(focused.size() - 1) == b) Drawing.dirty(this);
		focused.remove(b);
	}

}
