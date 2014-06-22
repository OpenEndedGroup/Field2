package fieldbox.boxes.plugins;

import field.graphics.Window;
import field.linalg.Vec2;
import fieldbox.boxes.Box;
import fieldbox.boxes.Drawing;
import fieldbox.boxes.Mouse;

/**
 * Created by marc on 6/21/14.
 */
public class Scrolling extends Box {

	public Scrolling(Box root_unused) {
		this.properties.putToList(Mouse.onMouseScroll, e -> {
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
