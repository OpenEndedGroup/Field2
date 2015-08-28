package fieldbox.boxes.plugins;

import field.linalg.Vec2;
import field.utility.Log;
import fieldbox.boxes.Box;
import fieldbox.boxes.Drawing;
import fieldbox.boxes.FrameManipulation;
import fieldbox.boxes.Mouse;
import fieldbox.ui.FieldBoxWindow;

/**
 * Adds mouse wheel (and, thus two-finger drag on OS X) pan support to the canvas
 */
public class Scrolling extends Box {

	public Scrolling(Box root_unused) {
		this.properties.putToMap(Mouse.onMouseScroll, "__scrolling__", e -> {

			if (e.properties.isTrue(FieldBoxWindow.consumed, false)) return;

			Log.log("scrolling", "not consumed");
			if (e.after.dwheel != 0 || e.after.dwheely != 0) {
				this.find(Drawing.drawing, this.both())
				    .findFirst()
				    .ifPresent(x -> {

					    System.err.println(" a :"+e.after.keyboardState+" "+e.after.mods);

					    if (e.after.keyboardState.isShiftDown()) {
						    Vec2 t = new Vec2(x.getScale());

						    Vec2 d = new Vec2(e.after.mx, e.after.my);

						    double sc = x.getScale().x;

						    t.x = t.y = t.y * Math.pow(2, e.after.dwheely / 50f);

						    x.setScale(this, t);

						    double r = -(t.x/sc-1);

						    x.setTranslation(this, x.getTranslation().add(d.x*r, d.y*r));
					    } else {
						    Vec2 t = x.getTranslation();
						    t.x += e.after.dwheel * 8;
						    t.y += e.after.dwheely * 8;

						    x.setTranslation(this, t);
					    }

					    FrameManipulation.continueTranslationFeedback(root_unused, false);
				    });
			}
		});
	}


}
