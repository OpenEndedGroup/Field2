package field.graphics;

import field.linalg.Mat4;
import field.utility.Rect;

import java.util.function.Supplier;

/**
 * Orthographic version of Camera
 */
public class OrthoCamera implements Supplier<Mat4> {

	static public class State {
		public final float left, top, right, bottom, znear, zfar;
		Mat4 m = null;

		public State(float left, float top, float right, float bottom, float znear, float zfar) {
			this.left = left;
			this.top = top;
			this.right = right;
			this.bottom = bottom;
			this.znear = znear;
			this.zfar = zfar;
		}
		public State() {
			this.left = 0;
			this.top = 0;
			this.right = 1;
			this.bottom = 1;
			this.znear = 0.01f;
			this.zfar = 100f;
		}


		public Mat4 toProjection() {
			if (m == null) {
				m = new Mat4();
				float tx = (float) (-(right + left) / (right - left));
				float ty = (float) (-(top + bottom) / (top - bottom));
				float tz = (float) (-(zfar + znear) / (zfar - znear));

				m.m00 = (float) (2 / (right - left));
				m.m11 = (float) (2 / (top - bottom));
				m.m22 = (float) (-2 / (zfar - znear));
				m.m33 = 1;
				m.m03 = tx;
				m.m13 = ty;
				m.m23 = tz;
			}
			return m;
		}

		public State withLeft(float left)
		{
			return new State(left, top, right, bottom,znear, zfar);
		}

		public State withRight(float right)
		{
			return new State(left, top, right, bottom,znear, zfar);
		}

		public State withTop(float top)
		{
			return new State(left, top, right, bottom,znear, zfar);
		}

		public State withBottom(float bottom)
		{
			return new State(left, top, right, bottom,znear, zfar);
		}

		public State withBounds(Rect bounds)
		{
			return new State(bounds.x, bounds.y, bounds.x+bounds.w, bounds.y+bounds.h, znear, zfar);
		}

		public State withSize(int w, int h)
		{
			return new State(left, top, left+w, top+h, znear, zfar);
		}

	}

	State state = null;

	public OrthoCamera(float left, float top, float right, float bottom, float znear, float zfar) {
		state = new State(left, top, right, bottom, znear, zfar);
	}

	public OrthoCamera(float left, float top, float right, float bottom) {
		state = new State(left, top, right, bottom, 0.01f, 100);
	}

	public State getState()
	{
		return state;
	}

	public Mat4 get() {
		return new Mat4(state.toProjection());
	}

}
