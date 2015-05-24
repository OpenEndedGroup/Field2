package fieldbox.boxes.plugins;

import field.graphics.FLine;
import field.graphics.StandardFLineDrawing;
import field.linalg.Vec4;
import field.utility.Dict;
import field.utility.Rect;
import fieldbox.boxes.Box;
import fieldbox.boxes.Callbacks;
import fieldbox.boxes.Drawing;
import fieldbox.boxes.FLineDrawing;
import fieldcef.browser.Browser;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Draws a highlght for where keyboard focus is going to go
 */
public class KeyboardFocus extends Box {

	static public final Dict.Prop<KeyboardFocus> _keyboardFocus = new Dict.Prop<>("_keyboardFocus");
	private List<Box> focused = new ArrayList<Box>();

	public KeyboardFocus(Box root) {
		this.properties.put(_keyboardFocus, this);

		this.properties.putToMap(FLineDrawing.frameDrawing, "__keyboardFocusRing__", (x) -> {

			if (focused == null) return new FLine();
			if (focused.size() == 0) return new FLine();
			if (focused.get(focused.size() - 1) instanceof Browser) return new FLine();

			Rect f = focused.get(focused.size() - 1).properties.get(Box.frame);

			if (f == null) return new FLine();


			FLine fr = new FLine().rect(f.x, f.y, f.w - 1, f.h - 1);
			fr.attributes.put(StandardFLineDrawing.thicken, new BasicStroke(3));
			fr.attributes.put(StandardFLineDrawing.color, new Vec4(0.1f, 0.6, 1.0, 0.7f));

			return fr;
		});

		this.properties.putToMap(Callbacks.onDelete,"__keyboardFocusRing__", x -> {focused.remove(x); return null;});
	}

	public boolean isFocused(Box b)
	{
		if (focused.size()==0) return false;
		return focused.get(focused.size()-1)==b;
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
