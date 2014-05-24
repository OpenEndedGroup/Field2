package field.graphics;

import field.linalg.Mat4;
import field.linalg.Quat;
import field.linalg.Vec3;

import java.io.Serializable;
import java.util.function.Function;

/**
 * A (Perspective and stereo capable) Camera class for Field graphics.
 * <p>
 * The state of a camera is kept in a separate, serializable, immutable class called "state". In contemporary OpenGL all a camera is is something that
 * can produce two Mat4's which were customarily multiplied together anyway. The ModelView matrix, which rotates the world into the camera's
 * coordinate system and the projection matrix with projects that coordinate system into the screen's coordinate system. These are Mat4's not Mat3's,
 * even though we are 3d space because we can express projection matrices using homogeneous coordinates.
 *
 * To tie this to OpenGL, conspire to load these matrices into shader uniforms.
 */
public class Camera {

	static public class State implements Serializable {


		/**
		 * the position of the camera
		 */
		public Vec3 position = new Vec3(0, 0, -10);

		/**
		 * the target or lookAt position of the camera
		 */
		public Vec3 target = new Vec3(0, 0, 0);

		/**
		 * the up direction of the camera
		 */
		public Vec3 up = new Vec3(0, 1, 0);

		/**
		 * the field of view of the camera (in degrees)
		 */
		public float fov = 45;

		/**
		 * the near clipping plane of the camera
		 */
		public float near = 0.1f;

		/**
		 * the far clipping plane of the camera
		 */
		public float far = 1000;

		/**
		 * the horizontal shift of the camera
		 */
		public float sx = 0;

		/**
		 * the vertical shift of the camera
		 */
		public float sy = 0;

		/**
		 * the fustrum scale of the camera
		 */
		public float rx = 1;

		/**
		 * the aspect ratio of the camera
		 */
		public float aspect = 1;

		/**
		 * stereo interocculur fustrum shift
		 */
		public float io_frustra = 0;

		/**
		 * stereo interocculur position (left) shift
		 */
		public float io_disparity = 0;

		/**
		 * stereo interocculur position (left) shift, per distance to lookat
		 */
		public float io_disparity_per_distance = 0;

		public Vec3 ray() {
			return Vec3.sub(target, position, new Vec3());
		}

		public Vec3 ray(float stereoSide) {
			return Vec3.sub(target, position(stereoSide), new Vec3());
		}

		public Vec3 left() {
			return Vec3.cross(up, ray(), new Vec3()).normalise(new Vec3());
		}

		public Vec3 position() {
			return position(0);
		}

		public Vec3 target() {
			return target;
		}

		public State copy() {
			State s = new State();
			s.io_disparity = this.io_disparity;
			s.io_frustra = this.io_frustra;
			s.aspect = this.aspect;
			s.far = this.far;
			s.near = this.near;
			s.fov = this.fov;
			s.rx = this.rx;
			s.sx = this.sx;
			s.sy = this.sy;
			s.target = new Vec3(target);
			s.position = new Vec3(position);
			s.up = new Vec3(up);
			return s;
		}

		public State orbitLeft(float r) {
			State s = copy();
			s.position = Vec3.add(target, new Quat().setFromAxisAngle(up, r).rotate(ray().scale(-1)), new Vec3());
			return s;
		}

		public State orbitUp(float r) {
			State s = copy();
			Quat q = new Quat().setFromAxisAngle(left(), r);
			s.position = Vec3.add(target, q.rotate(ray().scale(-1)), new Vec3());
			s.up = q.rotate(up);
			return s;
		}

		public State lookLeft(float r) {
			State s = copy();
			s.target = Vec3.add(position, new Quat().setFromAxisAngle(up, r).rotate(ray().scale(1)), new Vec3());
			return s;
		}

		public State lookUp(float r) {
			State s = copy();
			Quat q = new Quat().setFromAxisAngle(left(), r);
			s.target = Vec3.add(position, q.rotate(ray().scale(1)), new Vec3());
			s.up = q.rotate(up);
			return s;
		}


		public State translateLeft(float r) {
			State s = copy();
			Vec3 left = left();
			left = left.normalise();
			left.scale((float) position.distanceFrom(target));
			s.position.x += left.x * r;
			s.position.y += left.y * r;
			s.position.z += left.z * r;
			s.target.x += left.x * r;
			s.target.y += left.y * r;
			s.target.z += left.z * r;
			return s;
		}

		public State translateIn(float r) {
			State s = copy();
			Vec3 left = ray();
			left = left.normalise();
			left.scale((float) position.distanceFrom(target));
			s.position.x += left.x * r;
			s.position.y += left.y * r;
			s.position.z += left.z * r;
			s.target.x += left.x * r;
			s.target.y += left.y * r;
			s.target.z += left.z * r;
			return s;
		}

		public State dollyIn(float r) {
			State s = copy();
			Vec3 left = ray();
			left = left.normalise();
			left.scale((float) position.distanceFrom(target));
			s.position.x += left.x * r;
			s.position.y += left.y * r;
			s.position.z += left.z * r;
			return s;
		}

		public State translateUp(float r) {
			State s = copy();
			Vec3 left = up;
			left = left.normalise();
			left.scale((float) position.distanceFrom(target));
			s.position.x += left.x * r;
			s.position.y += left.y * r;
			s.position.z += left.z * r;
			s.target.x += left.x * r;
			s.target.y += left.y * r;
			s.target.z += left.z * r;
			return s;
		}

		public Vec3 position(float stereoSide) {
			Vec3 left = left();
			float d = (float) target.distanceFrom(position);
			float s = stereoSide * (io_disparity + io_disparity_per_distance * d);
			return new Vec3(position.x + left.x * s, position.y + left.y * s, position.z + left.z * s);
		}

	}

	State state = new State();

	public Mat4 projectionMatrix() {
		return projectionMatrix(0);
	}

	public Mat4 projectionMatrix(float stereoSide) {

		float R = (float) (state.near * Math.tan((Math.PI * state.fov / 180f) / 2) * state.aspect) * state.rx;
		float T = (float) (state.near * Math.tan((Math.PI * state.fov / 180f) / 2)) * state.rx;

		float right = -R + R * state.sx + state.io_frustra * stereoSide;
		float left = R + R * state.sx + state.io_frustra * stereoSide;
		float top = -T + T * state.sy;
		float bottom = T + T * state.sy;

		float A = (right + left) / (right - left);
		float B = (top + bottom) / (top - bottom);
		float C = -(state.far + state.near) / (state.far - state.near);
		float D = -(2 * state.far * state.near) / (state.far - state.near);

		float[] m = new float[16];

		m[0] = (float) (2 * state.near) / (right - left);
		m[5] = (float) (2 * state.near) / (top - bottom);
		m[10] = C;
		m[14] = -1;
		m[2] = A;
		m[6] = B;
		m[11] = D;

		System.out.println(" projection is :" + new Mat4(m));
		return new Mat4(m);
	}

	public Mat4 modelView() {
		return modelView(0);
	}

	public Mat4 modelView(float stereoSide) {
		Vec3 forward = state.ray(stereoSide).normalise(new Vec3());
		Vec3 up = state.up.normalise(new Vec3());

		/* Side = forward x up */
		Vec3 side = Vec3.cross(forward, up, new Vec3()).normalise(new Vec3());

		/* Recompute up as: up = side x forward */
		up = Vec3.cross(side, forward, new Vec3());

		float[] ret = new float[16];

		ret[0 * 4 + 0] = side.x;
		ret[1 * 4 + 0] = side.y;
		ret[2 * 4 + 0] = side.z;

		ret[0 * 4 + 1] = up.x;
		ret[1 * 4 + 1] = up.y;
		ret[2 * 4 + 1] = up.z;

		ret[0 * 4 + 2] = -forward.x;
		ret[1 * 4 + 2] = -forward.y;
		ret[2 * 4 + 2] = -forward.z;

		ret[3 * 4 + 3] = 1;

		Mat4 m = new Mat4(ret);
		Vec3 e = Mat4.transform(m, state.position(stereoSide), new Vec3());
		ret[3 * 4 + 0] = -e.x;
		ret[3 * 4 + 1] = -e.y;
		ret[3 * 4 + 2] = -e.z;

		Mat4 q = new Mat4(ret);
		q.transpose();
		System.out.println(" model view is :" + new Mat4(q));
		return q;
	}

	public Mat4 modelViewLeft()
	{
		return modelView(-1);
	}

	public Mat4 modelViewRight()
	{
		return modelView(1);
	}

	public Mat4 modelViewCenter()
	{
		return modelView(0);
	}

	public void advanceState(Function<State, State> s) {
		state = s.apply(state);
	}

	public void setState(State s) {
		this.state = state;
	}

	public State getState() {
		return state;
	}

}
