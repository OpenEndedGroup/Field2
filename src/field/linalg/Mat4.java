package field.linalg;

import java.io.Serializable;
import java.nio.FloatBuffer;

/**
 * Created by marc on 3/12/14.
 */
public class Mat4 implements Serializable {
	private static final long serialVersionUID = 1L;

	public float m00, m01, m02, m03, m10, m11, m12, m13, m20, m21, m22, m23, m30, m31, m32, m33;

	/**
	 * Construct a new matrix, initialized to the identity.
	 */
	public Mat4() {
		super();
		setIdentity();
	}

	public Mat4(final Mat4 src) {
		super();
		load(src);
	}

	public Mat4(final float[] src) {
		super();
		load(src);
	}

	/**
	 * Constructs and initializes a Matrix4f from the quaternion,
	 * translation, and scale values; the scale is applied only to the
	 * rotational components of the matrix (upper 3x3) and not to the
	 * translational components.
	 *
	 * @param q1 the quaternion value representing the rotational
	 *           component (can be null)
	 * @param t1 the translational component of the matrix (can be null)
	 * @param s  the scale value applied to the rotational components
	 */
	public Mat4(Quat q1, Vec3 t1, float s) {
		setIdentity();

		if (s != 1 && q1 == null) scale(s);

		if (q1 != null) {
			m00 = (float) (s * (1.0 - 2.0 * q1.y * q1.y - 2.0 * q1.z * q1.z));
			m10 = (float) (s * (2.0 * (q1.x * q1.y + q1.w * q1.z)));
			m20 = (float) (s * (2.0 * (q1.x * q1.z - q1.w * q1.y)));

			m01 = (float) (s * (2.0 * (q1.x * q1.y - q1.w * q1.z)));
			m11 = (float) (s * (1.0 - 2.0 * q1.x * q1.x - 2.0 * q1.z * q1.z));
			m21 = (float) (s * (2.0 * (q1.y * q1.z + q1.w * q1.x)));

			m02 = (float) (s * (2.0 * (q1.x * q1.z + q1.w * q1.y)));
			m12 = (float) (s * (2.0 * (q1.y * q1.z - q1.w * q1.x)));
			m22 = (float) (s * (1.0 - 2.0 * q1.x * q1.x - 2.0 * q1.y * q1.y));
		}
		if (t1 != null) {
			m03 = t1.x;
			m13 = t1.y;
			m23 = t1.z;
		}
		m30 = 0.0f;
		m31 = 0.0f;
		m32 = 0.0f;
		m33 = 1.0f;

	}

	/**
	 * Returns a string representation of this matrix
	 */
	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append(m00).append(' ').append(m10).append(' ').append(m20).append(' ').append(m30).append('\n');
		buf.append(m01).append(' ').append(m11).append(' ').append(m21).append(' ').append(m31).append('\n');
		buf.append(m02).append(' ').append(m12).append(' ').append(m22).append(' ').append(m32).append('\n');
		buf.append(m03).append(' ').append(m13).append(' ').append(m23).append(' ').append(m33).append('\n');
		return buf.toString();
	}

	/**
	 * Set this Mat4 to be the identity matrix.
	 *
	 * @return this
	 */
	public Mat4 setIdentity() {
		return setIdentity(this);
	}

	/**
	 * Set the given Mat4 to be the identity matrix.
	 *
	 * @param m The Mat4 to set to the identity
	 * @return m
	 */
	public static Mat4 setIdentity(Mat4 m) {
		m.m00 = 1.0f;
		m.m01 = 0.0f;
		m.m02 = 0.0f;
		m.m03 = 0.0f;
		m.m10 = 0.0f;
		m.m11 = 1.0f;
		m.m12 = 0.0f;
		m.m13 = 0.0f;
		m.m20 = 0.0f;
		m.m21 = 0.0f;
		m.m22 = 1.0f;
		m.m23 = 0.0f;
		m.m30 = 0.0f;
		m.m31 = 0.0f;
		m.m32 = 0.0f;
		m.m33 = 1.0f;

		return m;
	}

	/**
	 * Set this Mat4 to 0.
	 *
	 * @return this
	 */
	public Mat4 setZero() {
		return setZero(this);
	}

	/**
	 * Set the given Mat4 to 0.
	 *
	 * @param m The Mat4 to set to 0
	 * @return m
	 */
	public static Mat4 setZero(Mat4 m) {
		m.m00 = 0.0f;
		m.m01 = 0.0f;
		m.m02 = 0.0f;
		m.m03 = 0.0f;
		m.m10 = 0.0f;
		m.m11 = 0.0f;
		m.m12 = 0.0f;
		m.m13 = 0.0f;
		m.m20 = 0.0f;
		m.m21 = 0.0f;
		m.m22 = 0.0f;
		m.m23 = 0.0f;
		m.m30 = 0.0f;
		m.m31 = 0.0f;
		m.m32 = 0.0f;
		m.m33 = 0.0f;

		return m;
	}

	/**
	 * Load from another Mat4
	 *
	 * @param src The source matrix
	 * @return this
	 */
	public Mat4 load(Mat4 src) {
		return load(src, this);
	}

	/**
	 * Load from another Mat4
	 *
	 * @param src The source matrix
	 * @return this
	 */
	public Mat4 load(float[] src) {
		int a = 0;
		m00 = src[a++];
		m01 = src[a++];
		m02 = src[a++];
		m03 = src[a++];
		m10 = src[a++];
		m11 = src[a++];
		m12 = src[a++];
		m13 = src[a++];
		m20 = src[a++];
		m21 = src[a++];
		m22 = src[a++];
		m23 = src[a++];
		m30 = src[a++];
		m31 = src[a++];
		m32 = src[a++];
		m33 = src[a++];
		return this;
	}

	/**
	 * Copy the source Mat4 to the destination matrix
	 *
	 * @param src  The source matrix
	 * @param dest The destination matrix, or null of a new one is to be created
	 * @return The copied matrix
	 */
	public static Mat4 load(Mat4 src, Mat4 dest) {
		if (dest == null) dest = new Mat4();
		dest.m00 = src.m00;
		dest.m01 = src.m01;
		dest.m02 = src.m02;
		dest.m03 = src.m03;
		dest.m10 = src.m10;
		dest.m11 = src.m11;
		dest.m12 = src.m12;
		dest.m13 = src.m13;
		dest.m20 = src.m20;
		dest.m21 = src.m21;
		dest.m22 = src.m22;
		dest.m23 = src.m23;
		dest.m30 = src.m30;
		dest.m31 = src.m31;
		dest.m32 = src.m32;
		dest.m33 = src.m33;

		return dest;
	}

	/**
	 * Load from a float buffer. The buffer stores the Mat4 in column major
	 * (OpenGL) order.
	 *
	 * @param buf A float buffer to read from
	 * @return this
	 */
	public Mat4 load(FloatBuffer buf) {

		m00 = buf.get();
		m01 = buf.get();
		m02 = buf.get();
		m03 = buf.get();
		m10 = buf.get();
		m11 = buf.get();
		m12 = buf.get();
		m13 = buf.get();
		m20 = buf.get();
		m21 = buf.get();
		m22 = buf.get();
		m23 = buf.get();
		m30 = buf.get();
		m31 = buf.get();
		m32 = buf.get();
		m33 = buf.get();

		return this;
	}

	/**
	 * Load from a float buffer. The buffer stores the Mat4 in row major
	 * (maths) order.
	 *
	 * @param buf A float buffer to read from
	 * @return this
	 */
	public Mat4 loadTranspose(FloatBuffer buf) {

		m00 = buf.get();
		m10 = buf.get();
		m20 = buf.get();
		m30 = buf.get();
		m01 = buf.get();
		m11 = buf.get();
		m21 = buf.get();
		m31 = buf.get();
		m02 = buf.get();
		m12 = buf.get();
		m22 = buf.get();
		m32 = buf.get();
		m03 = buf.get();
		m13 = buf.get();
		m23 = buf.get();
		m33 = buf.get();

		return this;
	}

	/**
	 * Store this Mat4 in a float buffer. The Mat4 is stored in column
	 * major (openGL) order.
	 *
	 * @param buf The buffer to store this Mat4 in
	 */
	public Mat4 store(FloatBuffer buf) {
		buf.put(m00);
		buf.put(m01);
		buf.put(m02);
		buf.put(m03);
		buf.put(m10);
		buf.put(m11);
		buf.put(m12);
		buf.put(m13);
		buf.put(m20);
		buf.put(m21);
		buf.put(m22);
		buf.put(m23);
		buf.put(m30);
		buf.put(m31);
		buf.put(m32);
		buf.put(m33);
		return this;
	}

	/**
	 * Store this Mat4 in a float buffer. The Mat4 is stored in row
	 * major (maths) order.
	 *
	 * @param buf The buffer to store this Mat4 in
	 */
	public Mat4 storeTranspose(FloatBuffer buf) {
		buf.put(m00);
		buf.put(m10);
		buf.put(m20);
		buf.put(m30);
		buf.put(m01);
		buf.put(m11);
		buf.put(m21);
		buf.put(m31);
		buf.put(m02);
		buf.put(m12);
		buf.put(m22);
		buf.put(m32);
		buf.put(m03);
		buf.put(m13);
		buf.put(m23);
		buf.put(m33);
		return this;
	}

	/**
	 * Store the rotation portion of this Mat4 in a float buffer. The Mat4 is stored in column
	 * major (openGL) order.
	 *
	 * @param buf The buffer to store this Mat4 in
	 */
	public Mat4 store3f(FloatBuffer buf) {
		buf.put(m00);
		buf.put(m01);
		buf.put(m02);
		buf.put(m10);
		buf.put(m11);
		buf.put(m12);
		buf.put(m20);
		buf.put(m21);
		buf.put(m22);
		return this;
	}

	/**
	 * Add two matrices together and place the result in a third matrix.
	 *
	 * @param left  The left source matrix
	 * @param right The right source matrix
	 * @param dest  The destination matrix, or null if a new one is to be created
	 * @return the destination matrix
	 */
	public static Mat4 add(Mat4 left, Mat4 right, Mat4 dest) {
		if (dest == null) dest = new Mat4();

		dest.m00 = left.m00 + right.m00;
		dest.m01 = left.m01 + right.m01;
		dest.m02 = left.m02 + right.m02;
		dest.m03 = left.m03 + right.m03;
		dest.m10 = left.m10 + right.m10;
		dest.m11 = left.m11 + right.m11;
		dest.m12 = left.m12 + right.m12;
		dest.m13 = left.m13 + right.m13;
		dest.m20 = left.m20 + right.m20;
		dest.m21 = left.m21 + right.m21;
		dest.m22 = left.m22 + right.m22;
		dest.m23 = left.m23 + right.m23;
		dest.m30 = left.m30 + right.m30;
		dest.m31 = left.m31 + right.m31;
		dest.m32 = left.m32 + right.m32;
		dest.m33 = left.m33 + right.m33;

		return dest;
	}

	/**
	 * Subtract the right Mat4 from the left and place the result in a third matrix.
	 *
	 * @param left  The left source matrix
	 * @param right The right source matrix
	 * @param dest  The destination matrix, or null if a new one is to be created
	 * @return the destination matrix
	 */
	public static Mat4 sub(Mat4 left, Mat4 right, Mat4 dest) {
		if (dest == null) dest = new Mat4();

		dest.m00 = left.m00 - right.m00;
		dest.m01 = left.m01 - right.m01;
		dest.m02 = left.m02 - right.m02;
		dest.m03 = left.m03 - right.m03;
		dest.m10 = left.m10 - right.m10;
		dest.m11 = left.m11 - right.m11;
		dest.m12 = left.m12 - right.m12;
		dest.m13 = left.m13 - right.m13;
		dest.m20 = left.m20 - right.m20;
		dest.m21 = left.m21 - right.m21;
		dest.m22 = left.m22 - right.m22;
		dest.m23 = left.m23 - right.m23;
		dest.m30 = left.m30 - right.m30;
		dest.m31 = left.m31 - right.m31;
		dest.m32 = left.m32 - right.m32;
		dest.m33 = left.m33 - right.m33;

		return dest;
	}

	/**
	 * Multiply the right Mat4 by the left and place the result in a third matrix.
	 *
	 * @param left  The left source matrix
	 * @param right The right source matrix
	 * @param dest  The destination matrix, or null if a new one is to be created
	 * @return the destination matrix
	 */
	public static Mat4 mul(Mat4 left, Mat4 right, Mat4 dest) {
		if (dest == null) dest = new Mat4();

		float m00 = left.m00 * right.m00 + left.m10 * right.m01 + left.m20 * right.m02 + left.m30 * right.m03;
		float m01 = left.m01 * right.m00 + left.m11 * right.m01 + left.m21 * right.m02 + left.m31 * right.m03;
		float m02 = left.m02 * right.m00 + left.m12 * right.m01 + left.m22 * right.m02 + left.m32 * right.m03;
		float m03 = left.m03 * right.m00 + left.m13 * right.m01 + left.m23 * right.m02 + left.m33 * right.m03;
		float m10 = left.m00 * right.m10 + left.m10 * right.m11 + left.m20 * right.m12 + left.m30 * right.m13;
		float m11 = left.m01 * right.m10 + left.m11 * right.m11 + left.m21 * right.m12 + left.m31 * right.m13;
		float m12 = left.m02 * right.m10 + left.m12 * right.m11 + left.m22 * right.m12 + left.m32 * right.m13;
		float m13 = left.m03 * right.m10 + left.m13 * right.m11 + left.m23 * right.m12 + left.m33 * right.m13;
		float m20 = left.m00 * right.m20 + left.m10 * right.m21 + left.m20 * right.m22 + left.m30 * right.m23;
		float m21 = left.m01 * right.m20 + left.m11 * right.m21 + left.m21 * right.m22 + left.m31 * right.m23;
		float m22 = left.m02 * right.m20 + left.m12 * right.m21 + left.m22 * right.m22 + left.m32 * right.m23;
		float m23 = left.m03 * right.m20 + left.m13 * right.m21 + left.m23 * right.m22 + left.m33 * right.m23;
		float m30 = left.m00 * right.m30 + left.m10 * right.m31 + left.m20 * right.m32 + left.m30 * right.m33;
		float m31 = left.m01 * right.m30 + left.m11 * right.m31 + left.m21 * right.m32 + left.m31 * right.m33;
		float m32 = left.m02 * right.m30 + left.m12 * right.m31 + left.m22 * right.m32 + left.m32 * right.m33;
		float m33 = left.m03 * right.m30 + left.m13 * right.m31 + left.m23 * right.m32 + left.m33 * right.m33;

		dest.m00 = m00;
		dest.m01 = m01;
		dest.m02 = m02;
		dest.m03 = m03;
		dest.m10 = m10;
		dest.m11 = m11;
		dest.m12 = m12;
		dest.m13 = m13;
		dest.m20 = m20;
		dest.m21 = m21;
		dest.m22 = m22;
		dest.m23 = m23;
		dest.m30 = m30;
		dest.m31 = m31;
		dest.m32 = m32;
		dest.m33 = m33;

		return dest;
	}

	/**
	 * Transform a Vector by a Mat4 and return the result in a destination
	 * vector.
	 *
	 * @param left  The left matrix
	 * @param right The right vector
	 * @param dest  The destination vector, or null if a new one is to be created
	 * @return the destination vector
	 */
	public static Vec4 transform(Mat4 left, Vec4 right, Vec4 dest) {
		if (dest == null) dest = new Vec4();

		float x = left.m00 * right.x + left.m10 * right.y + left.m20 * right.z + left.m30 * right.w;
		float y = left.m01 * right.x + left.m11 * right.y + left.m21 * right.z + left.m31 * right.w;
		float z = left.m02 * right.x + left.m12 * right.y + left.m22 * right.z + left.m32 * right.w;
		float w = left.m03 * right.x + left.m13 * right.y + left.m23 * right.z + left.m33 * right.w;

		dest.x = x;
		dest.y = y;
		dest.z = z;
		dest.w = w;

		return dest;
	}

	/**
	 * Transform a Vector by a Mat4 and return the result in a destination
	 * vector. This does the homogenous division by 'w' and the 'w' coordinate of 'right' is implied to be 1
	 *
	 * @param left  The left matrix
	 * @param right The right vector
	 * @param dest  The destination vector, or null if a new one is to be created
	 * @return the destination vector
	 */
	public static Vec3 transform(Mat4 left, Vec3 right, Vec3 dest) {
		if (dest == null) dest = new Vec3();

		float x = left.m00 * right.x + left.m10 * right.y + left.m20 * right.z + left.m30 * 1;
		float y = left.m01 * right.x + left.m11 * right.y + left.m21 * right.z + left.m31 * 1;
		float z = left.m02 * right.x + left.m12 * right.y + left.m22 * right.z + left.m32 * 1;
		float w = left.m03 * right.x + left.m13 * right.y + left.m23 * right.z + left.m33 * 1;

		dest.x = x/w;
		dest.y = y/w;
		dest.z = z/w;

		return dest;
	}


	/**
	 * Transform a Vector by a Mat4 and return the result in a destination
	 * vector. The 'w' coordinate of 'right' is implied to be 1
	 *
	 * @param left  The left matrix
	 * @param right The right vector
	 * @param dest  The destination vector, or null if a new one is to be created
	 * @return the destination vector
	 */
	public static Vec4 transform(Mat4 left, Vec3 right, Vec4 dest) {
		if (dest == null) dest = new Vec4();

		float x = left.m00 * right.x + left.m10 * right.y + left.m20 * right.z + left.m30 * 1;
		float y = left.m01 * right.x + left.m11 * right.y + left.m21 * right.z + left.m31 * 1;
		float z = left.m02 * right.x + left.m12 * right.y + left.m22 * right.z + left.m32 * 1;
		float w = left.m03 * right.x + left.m13 * right.y + left.m23 * right.z + left.m33 * 1;

		dest.x = x;
		dest.y = y;
		dest.z = z;
		dest.w = w;

		return dest;
	}

	/**
	 * Transpose this matrix
	 *
	 * @return this
	 */
	public Mat4 transpose() {
		return transpose(this);
	}

	/**
	 * Translate this matrix
	 *
	 * @param vec The vector to translate by
	 * @return this
	 */
	public Mat4 translate(Vec2 vec) {
		return translate(vec, this);
	}

	/**
	 * Translate this matrix
	 *
	 * @param vec The vector to translate by
	 * @return this
	 */
	public Mat4 translate(Vec3 vec) {
		return translate(vec, this);
	}

	/**
	 * Scales this matrix
	 *
	 * @param vec The vector to scale by
	 * @return this
	 */
	public Mat4 scale(Vec3 vec) {
		return scale(vec, this, this);
	}

	/**
	 * Scales this matrix
	 *
	 * @param s the scalar scale by
	 * @return this
	 */
	public Mat4 scale(float s) {
		this.m00 *= s;
		this.m01 *= s;
		this.m02 *= s;
		this.m03 *= s;
		this.m10 *= s;
		this.m11 *= s;
		this.m12 *= s;
		this.m13 *= s;
		this.m20 *= s;
		this.m21 *= s;
		this.m22 *= s;
		this.m23 *= s;
		return this;
	}

	/**
	 * Scales the source Mat4 and put the result in the destination matrix
	 *
	 * @param vec  The vector to scale by
	 * @param src  The source matrix
	 * @param dest The destination matrix, or null if a new Mat4 is to be created
	 * @return The scaled matrix
	 */
	public static Mat4 scale(Vec3 vec, Mat4 src, Mat4 dest) {
		if (dest == null) dest = new Mat4();
		dest.m00 = src.m00 * vec.x;
		dest.m01 = src.m01 * vec.x;
		dest.m02 = src.m02 * vec.x;
		dest.m03 = src.m03 * vec.x;
		dest.m10 = src.m10 * vec.y;
		dest.m11 = src.m11 * vec.y;
		dest.m12 = src.m12 * vec.y;
		dest.m13 = src.m13 * vec.y;
		dest.m20 = src.m20 * vec.z;
		dest.m21 = src.m21 * vec.z;
		dest.m22 = src.m22 * vec.z;
		dest.m23 = src.m23 * vec.z;
		return dest;
	}

	/**
	 * Rotates the Mat4 around the given axis the specified angle
	 *
	 * @param angle the angle, in radians.
	 * @param axis  The vector representing the rotation axis. Must be normalized.
	 * @return this
	 */
	public Mat4 rotate(float angle, Vec3 axis) {
		return rotate(angle, axis, this);
	}

	/**
	 * Rotates the Mat4 with the specified quaternion
	 *
	 * @param q the quaternion to rotate by.
	 * @return this
	 */
	public Mat4 rotate(Quat q) {
		Mat4 m = new Mat4(q, null, 1);
		mul(m, this, this);
		return this;
	}


	/**
	 * Rotates the Mat4 around the given axis the specified angle
	 *
	 * @param angle the angle, in radians.
	 * @param axis  The vector representing the rotation axis. Must be normalized.
	 * @param dest  The Mat4 to put the result, or null if a new Mat4 is to be created
	 * @return The rotated matrix
	 */
	public Mat4 rotate(float angle, Vec3 axis, Mat4 dest) {
		return rotate(angle, axis, this, dest);
	}

	/**
	 * Rotates the source Mat4 around the given axis the specified angle and
	 * put the result in the destination matrix.
	 *
	 * @param angle the angle, in radians.
	 * @param axis  The vector representing the rotation axis. Must be normalized.
	 * @param src   The Mat4 to rotate
	 * @param dest  The Mat4 to put the result, or null if a new Mat4 is to be created
	 * @return The rotated matrix
	 */
	public static Mat4 rotate(float angle, Vec3 axis, Mat4 src, Mat4 dest) {
		if (dest == null) dest = new Mat4();
		float c = (float) Math.cos(angle);
		float s = (float) Math.sin(angle);
		float oneminusc = 1.0f - c;
		float xy = axis.x * axis.y;
		float yz = axis.y * axis.z;
		float xz = axis.x * axis.z;
		float xs = axis.x * s;
		float ys = axis.y * s;
		float zs = axis.z * s;

		float f00 = axis.x * axis.x * oneminusc + c;
		float f01 = xy * oneminusc + zs;
		float f02 = xz * oneminusc - ys;
		// n[3] not used
		float f10 = xy * oneminusc - zs;
		float f11 = axis.y * axis.y * oneminusc + c;
		float f12 = yz * oneminusc + xs;
		// n[7] not used
		float f20 = xz * oneminusc + ys;
		float f21 = yz * oneminusc - xs;
		float f22 = axis.z * axis.z * oneminusc + c;

		float t00 = src.m00 * f00 + src.m10 * f01 + src.m20 * f02;
		float t01 = src.m01 * f00 + src.m11 * f01 + src.m21 * f02;
		float t02 = src.m02 * f00 + src.m12 * f01 + src.m22 * f02;
		float t03 = src.m03 * f00 + src.m13 * f01 + src.m23 * f02;
		float t10 = src.m00 * f10 + src.m10 * f11 + src.m20 * f12;
		float t11 = src.m01 * f10 + src.m11 * f11 + src.m21 * f12;
		float t12 = src.m02 * f10 + src.m12 * f11 + src.m22 * f12;
		float t13 = src.m03 * f10 + src.m13 * f11 + src.m23 * f12;
		dest.m20 = src.m00 * f20 + src.m10 * f21 + src.m20 * f22;
		dest.m21 = src.m01 * f20 + src.m11 * f21 + src.m21 * f22;
		dest.m22 = src.m02 * f20 + src.m12 * f21 + src.m22 * f22;
		dest.m23 = src.m03 * f20 + src.m13 * f21 + src.m23 * f22;
		dest.m00 = t00;
		dest.m01 = t01;
		dest.m02 = t02;
		dest.m03 = t03;
		dest.m10 = t10;
		dest.m11 = t11;
		dest.m12 = t12;
		dest.m13 = t13;
		return dest;
	}

	/**
	 * Translate this Mat4 and stash the result in another matrix
	 *
	 * @param vec  The vector to translate by
	 * @param dest The destination Mat4 or null if a new Mat4 is to be created
	 * @return the translated matrix
	 */
	public Mat4 translate(Vec3 vec, Mat4 dest) {
		return translate(vec, this, dest);
	}

	/**
	 * Translate the source Mat4 and stash the result in the destination matrix
	 *
	 * @param vec  The vector to translate by
	 * @param src  The source matrix
	 * @param dest The destination Mat4 or null if a new Mat4 is to be created
	 * @return The translated matrix
	 */
	public static Mat4 translate(Vec3 vec, Mat4 src, Mat4 dest) {
		if (dest == null) dest = new Mat4();

		dest.m30 += src.m00 * vec.x + src.m10 * vec.y + src.m20 * vec.z;
		dest.m31 += src.m01 * vec.x + src.m11 * vec.y + src.m21 * vec.z;
		dest.m32 += src.m02 * vec.x + src.m12 * vec.y + src.m22 * vec.z;
		dest.m33 += src.m03 * vec.x + src.m13 * vec.y + src.m23 * vec.z;

		return dest;
	}

	/**
	 * Translate this Mat4 and stash the result in another matrix
	 *
	 * @param vec  The vector to translate by
	 * @param dest The destination Mat4 or null if a new Mat4 is to be created
	 * @return the translated matrix
	 */
	public Mat4 translate(Vec2 vec, Mat4 dest) {
		return translate(vec, this, dest);
	}

	/**
	 * Translate the source Mat4 and stash the result in the destination matrix
	 *
	 * @param vec  The vector to translate by
	 * @param src  The source matrix
	 * @param dest The destination Mat4 or null if a new Mat4 is to be created
	 * @return The translated matrix
	 */
	public static Mat4 translate(Vec2 vec, Mat4 src, Mat4 dest) {
		if (dest == null) dest = new Mat4();

		dest.m30 += src.m00 * vec.x + src.m10 * vec.y;
		dest.m31 += src.m01 * vec.x + src.m11 * vec.y;
		dest.m32 += src.m02 * vec.x + src.m12 * vec.y;
		dest.m33 += src.m03 * vec.x + src.m13 * vec.y;

		return dest;
	}

	/**
	 * Transpose this Mat4 and place the result in another matrix
	 *
	 * @param dest The destination Mat4 or null if a new Mat4 is to be created
	 * @return the transposed matrix
	 */
	public Mat4 transpose(Mat4 dest) {
		return transpose(this, dest);
	}

	/**
	 * Transpose the source Mat4 and place the result in the destination matrix
	 *
	 * @param src  The source matrix
	 * @param dest The destination Mat4 or null if a new Mat4 is to be created
	 * @return the transposed matrix
	 */
	public static Mat4 transpose(Mat4 src, Mat4 dest) {
		if (dest == null) dest = new Mat4();
		float m00 = src.m00;
		float m01 = src.m10;
		float m02 = src.m20;
		float m03 = src.m30;
		float m10 = src.m01;
		float m11 = src.m11;
		float m12 = src.m21;
		float m13 = src.m31;
		float m20 = src.m02;
		float m21 = src.m12;
		float m22 = src.m22;
		float m23 = src.m32;
		float m30 = src.m03;
		float m31 = src.m13;
		float m32 = src.m23;
		float m33 = src.m33;

		dest.m00 = m00;
		dest.m01 = m01;
		dest.m02 = m02;
		dest.m03 = m03;
		dest.m10 = m10;
		dest.m11 = m11;
		dest.m12 = m12;
		dest.m13 = m13;
		dest.m20 = m20;
		dest.m21 = m21;
		dest.m22 = m22;
		dest.m23 = m23;
		dest.m30 = m30;
		dest.m31 = m31;
		dest.m32 = m32;
		dest.m33 = m33;

		return dest;
	}

	/**
	 * @return the determinant of the matrix
	 */
	public float determinant() {
		float f = m00 * ((m11 * m22 * m33 + m12 * m23 * m31 + m13 * m21 * m32) - m13 * m22 * m31 - m11 * m23 * m32 - m12 * m21 * m33);
		f -= m01 * ((m10 * m22 * m33 + m12 * m23 * m30 + m13 * m20 * m32) - m13 * m22 * m30 - m10 * m23 * m32 - m12 * m20 * m33);
		f += m02 * ((m10 * m21 * m33 + m11 * m23 * m30 + m13 * m20 * m31) - m13 * m21 * m30 - m10 * m23 * m31 - m11 * m20 * m33);
		f -= m03 * ((m10 * m21 * m32 + m11 * m22 * m30 + m12 * m20 * m31) - m12 * m21 * m30 - m10 * m22 * m31 - m11 * m20 * m32);
		return f;
	}

	/**
	 * Calculate the determinant of a 3x3 matrix
	 *
	 * @return result
	 */

	private static float determinant3x3(float t00, float t01, float t02, float t10, float t11, float t12, float t20, float t21, float t22) {
		return t00 * (t11 * t22 - t12 * t21) + t01 * (t12 * t20 - t10 * t22) + t02 * (t10 * t21 - t11 * t20);
	}

	/**
	 * Invert this matrix
	 *
	 * @return this if successful, null otherwise
	 */
	public Mat4 invert() {
		return invert(this, this);
	}

	/**
	 * Invert the source Mat4 and put the result in the destination
	 *
	 * @param src  The source matrix
	 * @param dest The destination matrix, or null if a new Mat4 is to be created
	 * @return The inverted Mat4 if successful, null otherwise
	 */
	public static Mat4 invert(Mat4 src, Mat4 dest) {
		float determinant = src.determinant();

		if (determinant != 0) {
			/*
			 * m00 m01 m02 m03
			 * m10 m11 m12 m13
			 * m20 m21 m22 m23
			 * m30 m31 m32 m33
			 */
			if (dest == null) dest = new Mat4();
			float determinant_inv = 1f / determinant;

			// first row
			float t00 = determinant3x3(src.m11, src.m12, src.m13, src.m21, src.m22, src.m23, src.m31, src.m32, src.m33);
			float t01 = -determinant3x3(src.m10, src.m12, src.m13, src.m20, src.m22, src.m23, src.m30, src.m32, src.m33);
			float t02 = determinant3x3(src.m10, src.m11, src.m13, src.m20, src.m21, src.m23, src.m30, src.m31, src.m33);
			float t03 = -determinant3x3(src.m10, src.m11, src.m12, src.m20, src.m21, src.m22, src.m30, src.m31, src.m32);
			// second row
			float t10 = -determinant3x3(src.m01, src.m02, src.m03, src.m21, src.m22, src.m23, src.m31, src.m32, src.m33);
			float t11 = determinant3x3(src.m00, src.m02, src.m03, src.m20, src.m22, src.m23, src.m30, src.m32, src.m33);
			float t12 = -determinant3x3(src.m00, src.m01, src.m03, src.m20, src.m21, src.m23, src.m30, src.m31, src.m33);
			float t13 = determinant3x3(src.m00, src.m01, src.m02, src.m20, src.m21, src.m22, src.m30, src.m31, src.m32);
			// third row
			float t20 = determinant3x3(src.m01, src.m02, src.m03, src.m11, src.m12, src.m13, src.m31, src.m32, src.m33);
			float t21 = -determinant3x3(src.m00, src.m02, src.m03, src.m10, src.m12, src.m13, src.m30, src.m32, src.m33);
			float t22 = determinant3x3(src.m00, src.m01, src.m03, src.m10, src.m11, src.m13, src.m30, src.m31, src.m33);
			float t23 = -determinant3x3(src.m00, src.m01, src.m02, src.m10, src.m11, src.m12, src.m30, src.m31, src.m32);
			// fourth row
			float t30 = -determinant3x3(src.m01, src.m02, src.m03, src.m11, src.m12, src.m13, src.m21, src.m22, src.m23);
			float t31 = determinant3x3(src.m00, src.m02, src.m03, src.m10, src.m12, src.m13, src.m20, src.m22, src.m23);
			float t32 = -determinant3x3(src.m00, src.m01, src.m03, src.m10, src.m11, src.m13, src.m20, src.m21, src.m23);
			float t33 = determinant3x3(src.m00, src.m01, src.m02, src.m10, src.m11, src.m12, src.m20, src.m21, src.m22);

			// transpose and divide by the determinant
			dest.m00 = t00 * determinant_inv;
			dest.m11 = t11 * determinant_inv;
			dest.m22 = t22 * determinant_inv;
			dest.m33 = t33 * determinant_inv;
			dest.m01 = t10 * determinant_inv;
			dest.m10 = t01 * determinant_inv;
			dest.m20 = t02 * determinant_inv;
			dest.m02 = t20 * determinant_inv;
			dest.m12 = t21 * determinant_inv;
			dest.m21 = t12 * determinant_inv;
			dest.m03 = t30 * determinant_inv;
			dest.m30 = t03 * determinant_inv;
			dest.m13 = t31 * determinant_inv;
			dest.m31 = t13 * determinant_inv;
			dest.m32 = t23 * determinant_inv;
			dest.m23 = t32 * determinant_inv;
			return dest;
		} else return null;
	}

	/**
	 * Negate this matrix
	 *
	 * @return this
	 */
	public Mat4 negate() {
		return negate(this);
	}

	/**
	 * Negate this Mat4 and place the result in a destination matrix.
	 *
	 * @param dest The destination matrix, or null if a new Mat4 is to be created
	 * @return the negated matrix
	 */
	public Mat4 negate(Mat4 dest) {
		return negate(this, dest);
	}

	/**
	 * Negate this Mat4 and place the result in a destination matrix.
	 *
	 * @param src  The source matrix
	 * @param dest The destination matrix, or null if a new Mat4 is to be created
	 * @return The negated matrix
	 */
	public static Mat4 negate(Mat4 src, Mat4 dest) {
		if (dest == null) dest = new Mat4();

		dest.m00 = -src.m00;
		dest.m01 = -src.m01;
		dest.m02 = -src.m02;
		dest.m03 = -src.m03;
		dest.m10 = -src.m10;
		dest.m11 = -src.m11;
		dest.m12 = -src.m12;
		dest.m13 = -src.m13;
		dest.m20 = -src.m20;
		dest.m21 = -src.m21;
		dest.m22 = -src.m22;
		dest.m23 = -src.m23;
		dest.m30 = -src.m30;
		dest.m31 = -src.m31;
		dest.m32 = -src.m32;
		dest.m33 = -src.m33;

		return dest;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Mat4)) return false;

		Mat4 mat4 = (Mat4) o;

		if (Float.compare(mat4.m00, m00) != 0) return false;
		if (Float.compare(mat4.m01, m01) != 0) return false;
		if (Float.compare(mat4.m02, m02) != 0) return false;
		if (Float.compare(mat4.m03, m03) != 0) return false;
		if (Float.compare(mat4.m10, m10) != 0) return false;
		if (Float.compare(mat4.m11, m11) != 0) return false;
		if (Float.compare(mat4.m12, m12) != 0) return false;
		if (Float.compare(mat4.m13, m13) != 0) return false;
		if (Float.compare(mat4.m20, m20) != 0) return false;
		if (Float.compare(mat4.m21, m21) != 0) return false;
		if (Float.compare(mat4.m22, m22) != 0) return false;
		if (Float.compare(mat4.m23, m23) != 0) return false;
		if (Float.compare(mat4.m30, m30) != 0) return false;
		if (Float.compare(mat4.m31, m31) != 0) return false;
		if (Float.compare(mat4.m32, m32) != 0) return false;
		if (Float.compare(mat4.m33, m33) != 0) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = (m00 != +0.0f ? Float.floatToIntBits(m00) : 0);
		result = 31 * result + (m01 != +0.0f ? Float.floatToIntBits(m01) : 0);
		result = 31 * result + (m02 != +0.0f ? Float.floatToIntBits(m02) : 0);
		result = 31 * result + (m03 != +0.0f ? Float.floatToIntBits(m03) : 0);
		result = 31 * result + (m10 != +0.0f ? Float.floatToIntBits(m10) : 0);
		result = 31 * result + (m11 != +0.0f ? Float.floatToIntBits(m11) : 0);
		result = 31 * result + (m12 != +0.0f ? Float.floatToIntBits(m12) : 0);
		result = 31 * result + (m13 != +0.0f ? Float.floatToIntBits(m13) : 0);
		result = 31 * result + (m20 != +0.0f ? Float.floatToIntBits(m20) : 0);
		result = 31 * result + (m21 != +0.0f ? Float.floatToIntBits(m21) : 0);
		result = 31 * result + (m22 != +0.0f ? Float.floatToIntBits(m22) : 0);
		result = 31 * result + (m23 != +0.0f ? Float.floatToIntBits(m23) : 0);
		result = 31 * result + (m30 != +0.0f ? Float.floatToIntBits(m30) : 0);
		result = 31 * result + (m31 != +0.0f ? Float.floatToIntBits(m31) : 0);
		result = 31 * result + (m32 != +0.0f ? Float.floatToIntBits(m32) : 0);
		result = 31 * result + (m33 != +0.0f ? Float.floatToIntBits(m33) : 0);
		return result;
	}
}