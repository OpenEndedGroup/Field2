package fieldbox.boxes.plugins;

import field.linalg.Vec2;
import field.utility.Log;
import fieldbox.boxes.Box;
import fieldbox.boxes.Drawing;
import fieldbox.boxes.Mouse;
import fieldbox.ui.FieldBoxWindow;

/**
 * Adds mouse wheel (and, thus two-finger drag on OS X) pan support to the canvas
 */
public class Scrolling extends Box {

	public Scrolling(Box root_unused) {
		this.properties.putToList(Mouse.onMouseScroll, e -> {

			if (e.properties.isTrue(FieldBoxWindow.consumed, false)) return;

			Log.log("scrolling", "not consumed");
			if (e.after.dwheel != 0 || e.after.dwheely != 0) {
				this.find(Drawing.drawing, this.both()).findFirst().ifPresent(x -> {
					Vec2 t = x.getTranslation();
					t.x += e.after.dwheel*4;
					t.y += e.after.dwheely*4;

					x.setTranslation(this, t);
				});
			}
		});
	}


}
