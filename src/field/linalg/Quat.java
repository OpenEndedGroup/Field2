package field.linalg;

import java.io.Serializable;
import java.nio.FloatBuffer;

/**
 * Created by marc on 3/12/14.
 */
public class Quat implements Serializable {

	private static final long serialVersionUID = 1L;

	public float x, y, z, w;

	/**
	 * C'tor. The quaternion will be initialized to the identity.
	 */
	public Quat() {
		super();
		setIdentity();
	}

	/**
	 * C'tor
	 *
	 * @param src
	 */
	public Quat(Vec4 src) {
		set(src);
	}

	/**
	 * C'tor
	 *
	 * @param src
	 */
	public Quat(Quat src) {
		set(src);
	}

	/**
	 * C'tor
	 */
	public Quat(float x, float y, float z, float w) {
		set(x, y, z, w);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.lwjgl.util.vector.WritableVector2f#set(float, float)
	 */
	public void set(float x, float y) {
		this.x = x;
		this.y = y;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.lwjgl.util.vector.WritableVector3f#set(float, float, float)
	 */
	public void set(float x, float y, float z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.lwjgl.util.vector.WritableVector4#set(float, float, float,
	 *      float)
	 */
	public void set(float x, float y, float z, float w) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.w = w;
	}

	/**
	 * Load from another Vec4
	 *
	 * @param src The source vector
	 * @return this
	 */
	public Quat set(Vec4 src) {
		x = src.getX();
		y = src.getY();
		z = src.getZ();
		w = src.getW();
		return this;
	}

	/**
	 * Load from another Quat
	 *
	 * @param src The source vector
	 * @return this
	 */
	public Quat set(Quat src) {
		x = src.getX();
		y = src.getY();
		z = src.getZ();
		w = src.getW();
		return this;
	}

	/**
	 * Set this quaternion to the multiplication identity.
	 *
	 * @return this
	 */
	public Quat setIdentity() {
		return setIdentity(this);
	}

	/**
	 * Set the given quaternion to the multiplication identity.
	 *
	 * @param q The quaternion
	 * @return q
	 */
	public static Quat setIdentity(Quat q) {
		q.x = 0;
		q.y = 0;
		q.z = 0;
		q.w = 1;
		return q;
	}

	/**
	 * @return the length of the vector
	 */
	public final float length() {
		return (float) Math.sqrt(lengthSquared());
	}

	/**
	 * @return the length squared of the quaternion
	 */
	public float lengthSquared() {
		return x * x + y * y + z * z + w * w;
	}

	/**
	 * Normalise the source quaternion and place the result in another quaternion.
	 *
	 * @param src  The source quaternion
	 * @param dest The destination quaternion, or null if a new quaternion is to be
	 *             created
	 * @return The normalised quaternion
	 */
	public static Quat normalise(Quat src, Quat dest) {
		float inv_l = 1f / src.length();

		if (dest == null) dest = new Quat();

		dest.set(src.x * inv_l, src.y * inv_l, src.z * inv_l, src.w * inv_l);

		return dest;
	}

	/**
	 * Normalise this quaternion and place the result in another quaternion.
	 *
	 * @param dest The destination quaternion, or null if a new quaternion is to be
	 *             created
	 * @return the normalised quaternion
	 */
	public Quat normalise(Quat dest) {
		return normalise(this, dest);
	}

	/**
	 * The dot product of two quaternions
	 *
	 * @param left  The LHS quat
	 * @param right The RHS quat
	 * @return left dot right
	 */
	public static float dot(Quat left, Quat right) {
		return left.x * right.x + left.y * right.y + left.z * right.z + left.w * right.w;
	}

	/**
	 * Calculate the conjugate of this quaternion and put it into the given one
	 *
	 * @param dest The quaternion which should be set to the conjugate of this
	 *             quaternion
	 */
	public Quat negate(Quat dest) {
		return negate(this, dest);
	}

	/**
	 * Calculate the conjugate of this quaternion and put it into the given one
	 *
	 * @param src  The source quaternion
	 * @param dest The quaternion which should be set to the conjugate of this
	 *             quaternion
	 */
	public static Quat negate(Quat src, Quat dest) {
		if (dest == null) dest = new Quat();

		dest.x = -src.x;
		dest.y = -src.y;
		dest.z = -src.z;
		dest.w = src.w;

		return dest;
	}

	/**
	 * Calculate the conjugate of this quaternion
	 */
	public Quat negate() {
		return negate(this, this);
	}

	/* (non-Javadoc)
	 * @see org.lwjgl.util.vector.Vector#load(java.nio.FloatBuffer)
	 */
	public Quat load(FloatBuffer buf) {
		x = buf.get();
		y = buf.get();
		z = buf.get();
		w = buf.get();
		return this;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.lwjgl.vector.Vector#scale(float)
	 */
	public Quat scale(float scale) {
		return scale(scale, this, this);
	}

	/**
	 * Scale the source quaternion by scale and put the result in the destination
	 *
	 * @param scale The amount to scale by
	 * @param src   The source quaternion
	 * @param dest  The destination quaternion, or null if a new quaternion is to be created
	 * @return The scaled quaternion
	 */
	public static Quat scale(float scale, Quat src, Quat dest) {
		if (dest == null) dest = new Quat();
		dest.x = src.x * scale;
		dest.y = src.y * scale;
		dest.z = src.z * scale;
		dest.w = src.w * scale;
		return dest;
	}

	/* (non-Javadoc)
	 * @see org.lwjgl.util.vector.ReadableVector#store(java.nio.FloatBuffer)
	 */
	public Quat store(FloatBuffer buf) {
		buf.put(x);
		buf.put(y);
		buf.put(z);
		buf.put(w);

		return this;
	}

	/**
	 * @return x
	 */
	public final float getX() {
		return x;
	}

	/**
	 * @return y
	 */
	public final float getY() {
		return y;
	}

	/**
	 * Set X
	 *
	 * @param x
	 */
	public final void setX(float x) {
		this.x = x;
	}

	/**
	 * Set Y
	 *
	 * @param y
	 */
	public final void setY(float y) {
		this.y = y;
	}

	/**
	 * Set Z
	 *
	 * @param z
	 */
	public void setZ(float z) {
		this.z = z;
	}

	/*
	 * (Overrides)
	 *
	 * @see org.lwjgl.vector.ReadableVector3f#getZ()
	 */
	public float getZ() {
		return z;
	}

	/**
	 * Set W
	 *
	 * @param w
	 */
	public void setW(float w) {
		this.w = w;
	}

	/*
	 * (Overrides)
	 *
	 * @see org.lwjgl.vector.ReadableVector3f#getW()
	 */
	public float getW() {
		return w;
	}

	public String toString() {
		return "Quat: " + x + " " + y + " " + z + " " + w;
	}


	/**
	 * Returns a vector that is 'v' rotated by this quaternion
	 * @param v the Vec3 to rotate
	 * @return a new Vec3
	 */

	public Vec3 rotate(Vec3 v)
	{
		return Mat4.transform(new Mat4(this, null, 1), v, new Vec3());
	}

	/**
	 * Sets the value of this quaternion to the quaternion product of
	 * quaternions left and right (this = left * right). Note that this is safe
	 * for aliasing (e.g. this can be left or right).
	 *
	 * @param left  the first quaternion
	 * @param right the second quaternion
	 */
	public static Quat mul(Quat left, Quat right, Quat dest) {
		if (dest == null) dest = new Quat();
		dest.set(left.x * right.w + left.w * right.x + left.y * right.z - left.z * right.y, left.y * right.w + left.w * right.y + left.z * right.x - left.x * right.z, left.z * right.w + left.w * right.z + left.x * right.y - left.y * right.x, left.w * right.w - left.x * right.x - left.y * right.y - left.z * right.z);
		return dest;
	}

	/**
	 * Multiplies quaternion left by the inverse of quaternion right and places
	 * the value into this quaternion. The value of both argument quaternions is
	 * preservered (this = left * right^-1).
	 *
	 * @param left  the left quaternion
	 * @param right the right quaternion
	 */
	public static Quat mulInverse(Quat left, Quat right, Quat dest) {
		float n = right.lengthSquared();
		// zero-div may occur.
		n = (n == 0.0 ? n : 1 / n);
		// store on stack once for aliasing-safty
		if (dest == null) dest = new Quat();
		dest.set((left.x * right.w - left.w * right.x - left.y * right.z + left.z * right.y) * n, (left.y * right.w - left.w * right.y - left.z * right.x + left.x * right.z) * n, (left.z * right.w - left.w * right.z - left.x * right.y + left.y * right.x) * n, (left.w * right.w + left.x * right.x + left.y * right.y + left.z * right.z) * n);

		return dest;
	}

	/**
	 * Sets the value of this quaternion to the equivalent rotation of the
	 * Axis-Angle argument.
	 *
	 * @param a1 the axis-angle: (x,y,z) is the axis and w is the angle
	 */
	public final Quat setFromAxisAngle(Vec4 a1) {
		x = a1.x;
		y = a1.y;
		z = a1.z;
		float n = (float) Math.sqrt(x * x + y * y + z * z);
		// zero-div may occur.
		float s = (float) (Math.sin(0.5 * a1.w) / n);
		x *= s;
		y *= s;
		z *= s;
		w = (float) Math.cos(0.5 * a1.w);
		return this;
	}

	/**
	 * Sets the value of this quaternion to the equivalent rotation of the
	 * Axis-Angle argument.
	 *
	 * @param a1 the axis-angle: (x,y,z) is the axis and a is the angle
	 */
	public final Quat setFromAxisAngle(Vec3 a1, float a) {
		x = a1.x;
		y = a1.y;
		z = a1.z;
		float n = (float) Math.sqrt(x * x + y * y + z * z);
		// zero-div may occur.
		float s = (float) (Math.sin(0.5 * a) / n);
		x *= s;
		y *= s;
		z *= s;
		w = (float) Math.cos(0.5 * a);
		return this;
	}

	/**
	 * Sets the value of this quaternion using the rotational component of the
	 * passed matrix.
	 *
	 * @param m The matrix
	 * @return this
	 */
	public final Quat setFromMatrix(Mat4 m) {
		return setFromMatrix(m, this);
	}

	/**
	 * Sets the value of the source quaternion using the rotational component of the
	 * passed matrix.
	 *
	 * @param m The source matrix
	 * @param q The destination quaternion, or null if a new quaternion is to be created
	 * @return q
	 */
	public static Quat setFromMatrix(Mat4 m, Quat q) {
		return q.setFromMat(m.m00, m.m01, m.m02, m.m10, m.m11, m.m12, m.m20, m.m21, m.m22);
	}

	/**
	 * Sets the value of this quaternion using the rotational component of the
	 * passed matrix.
	 *
	 * @param m The source matrix
	 */
	public final Quat setFromMatrix(Mat3 m) {
		return setFromMatrix(m, this);
	}

	/**
	 * Sets the value of the source quaternion using the rotational component of the
	 * passed matrix.
	 *
	 * @param m The source matrix
	 * @param q The destination quaternion, or null if a new quaternion is to be created
	 * @return q
	 */
	public static Quat setFromMatrix(Mat3 m, Quat q) {
		return q.setFromMat(m.m00, m.m01, m.m02, m.m10, m.m11, m.m12, m.m20, m.m21, m.m22);
	}

	/**
	 * Private method to perform the matrix-to-quaternion conversion
	 */
	private Quat setFromMat(float m00, float m01, float m02, float m10, float m11, float m12, float m20, float m21, float m22) {

		float s;
		float tr = m00 + m11 + m22;
		if (tr >= 0.0) {
			s = (float) Math.sqrt(tr + 1.0);
			w = s * 0.5f;
			s = 0.5f / s;
			x = (m21 - m12) * s;
			y = (m02 - m20) * s;
			z = (m10 - m01) * s;
		} else {
			float max = Math.max(Math.max(m00, m11), m22);
			if (max == m00) {
				s = (float) Math.sqrt(m00 - (m11 + m22) + 1.0);
				x = s * 0.5f;
				s = 0.5f / s;
				y = (m01 + m10) * s;
				z = (m20 + m02) * s;
				w = (m21 - m12) * s;
			} else if (max == m11) {
				s = (float) Math.sqrt(m11 - (m22 + m00) + 1.0);
				y = s * 0.5f;
				s = 0.5f / s;
				z = (m12 + m21) * s;
				x = (m01 + m10) * s;
				w = (m02 - m20) * s;
			} else {
				s = (float) Math.sqrt(m22 - (m00 + m11) + 1.0);
				z = s * 0.5f;
				s = 0.5f / s;
				x = (m20 + m02) * s;
				y = (m12 + m21) * s;
				w = (m10 - m01) * s;
			}
		}
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Quat)) return false;

		Quat that = (Quat) o;

		if (Float.compare(that.w, w) != 0) return false;
		if (Float.compare(that.x, x) != 0) return false;
		if (Float.compare(that.y, y) != 0) return false;
		if (Float.compare(that.z, z) != 0) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = (x != +0.0f ? Float.floatToIntBits(x) : 0);
		result = 31 * result + (y != +0.0f ? Float.floatToIntBits(y) : 0);
		result = 31 * result + (z != +0.0f ? Float.floatToIntBits(z) : 0);
		result = 31 * result + (w != +0.0f ? Float.floatToIntBits(w) : 0);
		return result;
	}

}
