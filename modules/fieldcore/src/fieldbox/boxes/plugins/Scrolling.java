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
		this.properties.put(Planes.plane, "__always__");
		this.properties.putToMap(Mouse.onMouseScroll, "__scrolling__", e -> {

			if (e.properties.isTrue(FieldBoxWindow.consumed, false)) return;

			Log.log("scrolling", () -> "not consumed");
			if (e.after.dwheel != 0 || e.after.dwheely != 0) {
				this.find(Drawing.drawing, this.both())
					.findFirst()
					.ifPresent(x -> {
						if (e.after.keyboardState.isShiftDown()) {
							Vec2 t = new Vec2(x.getScale());

							Vec2 dm = new Vec2(e.after.mx, e.after.my);

							Vec2 d= new Vec2(e.after.x, e.after.y);


							double sc = x.getScale().x;

							t.x = t.y = t.y * Math.pow(2, e.after.dwheely / 50f);

							t.x = Math.max(0.1f, Math.min(10, t.x));
							t.y = Math.max(0.1f, Math.min(10, t.y));

							x.setScale(this, t);

							double r = (t.x / sc);

							Vec2 trans;
							{
								System.out.println(" start at :"+dm);
								Vec2 dm2 = windowSystemToDrawingSystem(d, t, x.getTranslation());
								System.out.println(" d to d " + dm + " -> " + dm2);
								x.setTranslation(this, trans = x.getTranslation().add(new Vec2(dm).sub(dm2).mul(-t.x)));
								Vec2 dm3= windowSystemToDrawingSystem(d, t, trans);
								System.out.println(" final check" + dm + " -> " + dm3);
							}
//							for (int i = 0; i < 10; i++) {
//								Vec2 dm2 = windowSystemToDrawingSystem(d, t, trans);
//								System.out.println(" iterate d to d " + dm + " -> " + dm2);
//								x.setTranslation(this, trans = x.getTranslation().add(new Vec2(dm).sub(dm2).mul(-0.1f)));
//							}

//						    x.setTranslation(this, x.getTranslation().scale(r));
						} else {
							Vec2 t = x.getTranslation();

							double sc = x.getScale().x;

							t.x += e.after.dwheel * 8;
							t.y += e.after.dwheely * 8;

							x.setTranslation(this, t);
						}

						FrameManipulation.continueTranslationFeedback(Scrolling.this, false);
					});
			}
		});
	}

	/**
	 * to convert between event / mouse / pixel coordinates and OpenGL / Box / Drawing coordinates.
	 */
	public Vec2 windowSystemToDrawingSystem(Vec2 window, Vec2 scale, Vec2 translation) {
		double y = window.y;
		double x = window.x;

		x = x / scale.x;
		y = y / scale.y;
		x -= translation.x / scale.x;
		y -= translation.y / scale.y;

		return new Vec2(x, y);
	}

	/**
	 * to convert between OpenGL / Box / Drawing coordinates and event / mouse / pixel coordinates.
	 */
	public Vec2 drawingSystemToWindowSystem(Vec2 window, Vec2 scale, Vec2 translation) {
		double y = window.y;
		double x = window.x;

		x = x * scale.x;
		y = y * scale.y;
		x += translation.x;
		y += translation.y;

		return new Vec2(x, y);
	}


}
