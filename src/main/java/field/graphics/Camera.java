package field.graphics;

import field.linalg.Mat4;
import field.linalg.Quat;
import field.linalg.Vec3;
import field.utility.Serializable_safe;

import java.io.Serializable;
import java.util.function.Function;

/**
 * A (Perspective and stereo capable) Camera class for Field graphics.
 * <p>
 * The state of a camera is kept in a separate, serializable, immutable class called "state". In contemporary OpenGL all a camera is is something that
 * can produce two Mat4's which were customarily multiplied together anyway. The view matrix, which rotates the world into the camera's
 * coordinate system and the projection matrix with projects that coordinate system into the screen's coordinate system. These are Mat4's not Mat3's,
 * even though we are 3d space because we can express projection matrices using homogeneous coordinates.
 * <p>
 * To tie this to OpenGL, conspire to load these matrices into shader uniforms.
 */
public class Camera {


	// inglorious vr hack
	static public float cameraScale = 1f;

	public void setCameraScale(float cameraScale) {
		Camera.cameraScale = cameraScale;
	}

	static public class State implements Serializable_safe {

		static final long serialVersionUID = -3003261492097861789L;

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
		 * the x - fustrum scale of the camera
		 */
		public float rx = 1;

		/**
		 * the y- fustrum scale of the camera
		 */
		public float ry = 1;

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

		/**
		 * artificially move the target with the eyes. Positive values of this push the effective neutral point back from the eyes
		 */
		public float io_target_shift = 0;

		public Vec3 ray() {
			return Vec3.sub(target, position, new Vec3());
		}

		public Vec3 ray(float stereoSide) {
			return Vec3.sub(target(stereoSide), position(stereoSide), new Vec3());
		}

		public Vec3 left() {
			return Vec3.cross(up, ray(), new Vec3()).normalize();
		}

		public Vec3 position() {
			return position;
		}

		public Vec3 up() {
			return up;
		}

		public Vec3 target() {
			return target(0);
		}

		public Vec3 target(float stereoSide) {

			Vec3 left = left();
			float d = (float) target.distance(position);
			float s = stereoSide * (io_disparity + io_disparity_per_distance * d) * io_target_shift;

			return new Vec3(target).fma(left, s);
		}

		public State copy() {
			State s = new State();
			s.io_disparity = this.io_disparity;
			s.io_disparity_per_distance = this.io_disparity_per_distance;
			s.io_frustra = this.io_frustra;
			s.aspect = this.aspect;
			s.far = this.far;
			s.near = this.near;
			s.fov = this.fov;
			s.rx = this.rx;
			s.ry = this.ry;
			s.sx = this.sx;
			s.sy = this.sy;
			s.target = new Vec3(target);
			s.position = new Vec3(position);
			s.up = new Vec3(up);
			return s;
		}

		@Override
		public String toString() {
			String basic = "camera p:"+position+" t:"+target+" u:"+up+" aspect:"+aspect+" fov:"+fov+" far:"+far+" near:"+near;

			if (io_disparity!=0)
				basic += " io_disparity:"+io_disparity;
			if (io_disparity_per_distance!=0)
				basic += " io_disparity_per_distance:"+io_disparity_per_distance;
			if (io_frustra!=0)
				basic += " io_frustra:"+io_frustra;

			if (rx!=1)
				basic += " rx:"+rx;
			if (ry!=1)
				basic += " ry:"+ry;
			if (sx!=0)
				basic += " sx:"+sx;
			if (sy!=0)
				basic += " sy:"+sy;

			return basic.trim();

		}

		public State orbitLeft(float r) {
			State s = copy();
			s.position = Vec3.add(target, new Quat().fromAxisAngleRad(up, r).transform(ray().mul(-1)), new Vec3());
			return s;
		}

		public State orbitUp(float r) {
			State s = copy();
			Quat q = new Quat().fromAxisAngleRad(left(), r);
			s.position = Vec3.add(target, q.transform(ray().mul(-1)), new Vec3());
			s.up = q.transform(up);
			return s;
		}

		public State roll(float r) {
			State s = copy();
			Quat q = new Quat().fromAxisAngleRad(ray(), r);
			s.up = q.transform(up);
			return s;
		}


		public State lookLeft(float r) {
			State s = copy();
			s.target = Vec3.add(position, new Quat().fromAxisAngleRad(up, r).transform(ray().mul(1)), new Vec3());
			return s;
		}

		public State lookUp(float r) {
			State s = copy();
			Quat q = new Quat().fromAxisAngleRad(left(), r);
			s.target = Vec3.add(position, q.transform(ray().mul(1)), new Vec3());
			s.up = q.transform(up);
			return s;
		}


		public State translateLeft(float r) {
			State s = copy();
			Vec3 left = left();
			left = left.normalize();
			left.mul((float) position.distance(target));
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
			left = left.normalize();
			left.mul((float) position.distance(target));
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
			left = left.normalize();
			left.mul((float) position.distance(target));
			s.position.x += left.x * r;
			s.position.y += left.y * r;
			s.position.z += left.z * r;
			return s;
		}

		public State translateUp(float r) {
			State s = copy();
			Vec3 left = up;
			left = left.normalize();
			left.mul((float) position.distance(target));
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
			float d = (float) target.distance(position);
			float s = stereoSide * (io_disparity + io_disparity_per_distance * d);
			return new Vec3(position.x + left.x * s, position.y + left.y * s, position.z + left.z * s);
		}

		public State zoomIn(float amount) {
			State s = copy();
			s.fov = Math.max(2, Math.min(179, s.fov + amount));
			return s;
		}

		public State zoomOut(float amount) {
			State s = copy();
			s.fov = Math.max(2, Math.min(179, s.fov - amount));
			return s;
		}

		public State interpolate(float amount, State towards)
		{
			State s = copy();

			s.position.lerp(towards.position, amount);
			s.target.lerp(towards.target, amount);
			s.up.lerp(towards.up, amount);

			s.fov = s.fov*(1-amount) + amount*towards.fov;
			s.io_disparity = s.io_disparity*(1-amount) + amount*towards.io_disparity;
			s.io_disparity_per_distance = s.io_disparity_per_distance*(1-amount) + amount*towards.io_disparity_per_distance;
			s.io_frustra = s.io_frustra*(1-amount) + amount*towards.io_frustra;
			s.aspect = s.aspect*(1-amount) + amount*towards.aspect;
			s.far = s.far*(1-amount) + amount*towards.far;
			s.near = s.near*(1-amount) + amount*towards.near;
			s.rx = s.rx*(1-amount) + amount*towards.rx;
			s.ry = s.ry*(1-amount) + amount*towards.ry;
			s.sx = s.sx*(1-amount) + amount*towards.sx;
			s.sy = s.sy*(1-amount) + amount*towards.sy;


			return s;

		}


	}

	State state = new State();

	public Mat4 projectionMatrix() {
		return projectionMatrix(0);
	}

	public Mat4 projectionMatrix(float stereoSide) {

		float R = (float) (state.near * Math.tan((Math.PI * state.fov / 180f) / 2) * state.aspect) * state.rx;
		float T = (float) (state.near * Math.tan((Math.PI * state.fov / 180f) / 2)) * state.ry;

		float right = -R + R * state.sx + state.io_frustra * stereoSide;
		float left = R + R * state.sx + state.io_frustra * stereoSide;
		float top = -T + T * state.sy;
		float bottom = T + T * state.sy;

		float A = (right + left) / (right - left);
		float B = (top + bottom) / (top - bottom);
		float C = -(state.far + state.near) / (state.far - state.near);
		float D = -(2 * state.far * state.near) / (state.far - state.near);

		float[] m = new float[16];

		m[0] = 2 * state.near / (right - left);
		m[5] = 2 * state.near / (top - bottom);
		m[10] = C;
		m[14] = -1;
		m[2] = A;
		m[6] = B;
		m[11] = D;

		return new Mat4(m);
	}

	/**
	 * Projection matrix that can blend between orthographic and projection
	 * <p>
	 * here state.near and state.far are measured from the look at point with +ve z pointing towards us. Therefore state.near>state.far for almost all applications.
	 * <p>
	 * Experimental
	 */
	public Mat4 projectionMatrix2(float half_width) {
		float[] m = new float[16];

		float hw = half_width * state.aspect;
		float hh = half_width;

		float iez = (float) Math.tan((state.fov * Math.PI / 90) / Math.min(hw, hh));

		m[0] = 1 / hw;
		m[5] = 1 / hh;
		m[8] = -state.sx / hw;
		m[9] = -state.sy / hh;
		m[10] = -(2 - (state.near + state.far) * iez) / (state.near - state.far);
		m[11] = -iez;
		m[14] = ((state.near + state.far) - 2 * state.near * state.far * iez) / (state.near - state.far);
		m[15] = 1;
		return new Mat4(m);
	}

	public Mat4 view() {
		return view(0);
	}

	public Mat4 view(float stereoSide) {

		Vec3 forward = state.ray(stereoSide).normalize();
		Vec3 up = state.up.normalize();

		/* Side = forward x up */
		Vec3 side = Vec3.cross(forward, up, new Vec3()).normalize();

		/* Recompute up as: up = side x forward */
		up = Vec3.cross(side, forward, new Vec3());

		float[] ret = new float[16];

		ret[0 * 4 + 0] = (float) side.x;
		ret[1 * 4 + 0] = (float) side.y;
		ret[2 * 4 + 0] = (float) side.z;

		ret[0 * 4 + 1] = (float) up.x;
		ret[1 * 4 + 1] = (float) up.y;
		ret[2 * 4 + 1] = (float) up.z;

		ret[0 * 4 + 2] = (float) -forward.x;
		ret[1 * 4 + 2] = (float) -forward.y;
		ret[2 * 4 + 2] = (float) -forward.z;

		ret[3 * 4 + 3] = 1;

		Mat4 m = new Mat4(ret);

		if (Math.abs(cameraScale-1.0)>1e-4) {
			m.translate(new Vec3(state.up).mul(1.67));
			m.scale(cameraScale);
			m.translate(new Vec3(state.up).mul(-1.67));
		}

//		Vec3 e = Mat4.transform(m, state.position(stereoSide), new Vec3());
//		ret[3 * 4 + 0] = (float) -e.x;
//		ret[3 * 4 + 1] = (float) -e.y;
//		ret[3 * 4 + 2] = (float) -e.z;

//		m.m30 = (float)-e.x;
//		m.m31 = (float)-e.y;
//		m.m32 = (float)-e.z;

		m.translate(new Vec3(state.position(stereoSide)).mul(-1));

		Mat4 q = new Mat4(m);



		q.transpose();


//		System.out.println(" model view is :" + new Mat4(q));
		return q;
	}

	public Mat4 viewLeft() {
		return view(-1);
	}

	public Mat4 viewRight() {
		return view(1);
	}

	public Mat4 viewCenter() {
		return view(0);
	}

	public void advanceState(Function<State, State> s) {
		state = s.apply(state);
	}

	public void setState(State s) {
		this.state = s;
	}

	public State getState() {
		return state;
	}

}
