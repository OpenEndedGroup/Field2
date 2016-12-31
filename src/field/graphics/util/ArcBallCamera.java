package field.graphics.util;

import field.graphics.Camera;
import field.graphics.Window;
import field.linalg.Mat4;
import field.linalg.Quat;
import field.linalg.Vec3;
import field.utility.Rect;
import fieldbox.boxes.Box;
import fieldbox.boxes.Drawing;
import fieldbox.boxes.Mouse;

/**
 * Created by marc on 12/30/16.
 */
public class ArcBallCamera {

	private final Camera target;

	public class Down {
		Vec3 from;
		Camera.State fromState;
		Mat4 fromView;
	}

	public ArcBallCamera(Camera target) {
		this.target = target;
	}

	public void install(Box b) {
		b.properties.putToMap(Mouse.onMouseDown, "__arcball", (e, button) -> {

			Rect rr = b.properties.get(Box.frame);
			double x = (e.after.x - rr.x) / rr.w;
			double y = (e.after.y - rr.y) / rr.h;

			if (b.properties.isTrue(Mouse.isSelected, false) && x > 0 && x < 1 && y > 0 && y < 1) {
				e.properties.put(Window.consumed, true);
			} else
				return null;

			x = 2 * x - 1;
			y = 2 * y - 1;

			y = -y;

			Down down = down(x, y);
			return (e2, end) -> {

				e2.properties.put(Window.consumed, true);

				Rect rr2 = b.properties.get(Box.frame);
				double x2 = (e2.after.x - rr2.x) / rr.w;
				double y2 = (e2.after.y - rr2.y) / rr.h;

				x2 = 2 * x2 - 1;
				y2 = 2 * y2 - 1;
				y2 = -y2;

				Camera.State state = drag(down, x2, y2);

				target.setState(state);

				Drawing.dirty(b);

				return !end;
			};
		});
	}


	public static Vec3 projectToSphere(double r, double x, double y, Vec3 v) {
		if (v == null) v = new Vec3();
		double d = Math.sqrt(x * x + y * y);
		double z;
		if (d < r * 0.70710678118654752440) {
	    /* Inside sphere */
			z = Math.sqrt(r * r - d * d);
		} else {
	    /* On hyperbola */
			double t = r / 1.41421356237309504880;
			z = t * t / d;
		}
		return v.set(x, y, z);
	}

	public Down down(double ndc_x, double ndc_y) {
		Down d = new Down();

		d.from = projectToSphere(1, ndc_x, ndc_y, new Vec3());
		d.fromState = target.getState();
		d.fromView = target.view();
		return d;
	}

	public Camera.State drag(Down d, double ndc_x, double ndc_y) {
		Vec3 to = projectToSphere(1, ndc_x, ndc_y, new Vec3());

		Vec3 f = new Vec3();
		d.fromView.transform(d.from, f);
		Vec3 t = new Vec3();
		d.fromView.transform(to, t);

		Quat q = new Quat().rotateTo(f, t);
		Vec3 np = q.transform(new Vec3(d.fromState.position).sub(d.fromState.target)).add(d.fromState.target);
		Vec3 u = q.transform(new Vec3(d.fromState.up));
		np = q.transform(new Vec3(np).sub(d.fromState.target)).add(d.fromState.target);
		u = q.transform(new Vec3(u));

		Camera.State outState = d.fromState.copy();
		outState.position = np;
		outState.up = u;
		return outState;
	}


}


