package field.linalg;

import java.io.Serializable;
import java.nio.FloatBuffer;

/**
 */

public class Vec2 implements Serializable {

	private static final long serialVersionUID = 1L;

	public float x, y;

	/**
	 * Constructor for Vector3f.
	 */
	public Vec2() {
		super();
	}

	/**
	 * Constructor
	 */
	public Vec2(Vec2 src) {
		set(src);
	}

	/**
	 * Constructor
	 */
	public Vec2(float x, float y) {
		set(x, y);
	}

	public Vec2(double x, double y)
	{
		set(x, y);
	}

	/**
	 * sets this Vec2 to be the value given by x,y
	 * @param x
	 * @param y
	 */
	public void set(float x, float y) {
		this.x = x;
		this.y = y;
	}

	/**
	 * sets this Vec2 to be the value given by x,y
	 * @param x
	 * @param y
	 */
	public void set(double x, double y) {
		this.x = (float)x;
		this.y = (float)y;
	}

	/**
	 * Load from another Vec2
	 *
	 * @param src The source vector
	 * @return this
	 */
	public Vec2 set(Vec2 src) {
		x = src.getX();
		y = src.getY();
		return this;
	}

	/**
	 * @return the length squared of the vector
	 */
	public float lengthSquared() {
		return x * x + y * y;
	}

	/**
	 * @return the length of the vector
	 */
	public final float length() {
		return (float) Math.sqrt(lengthSquared());
	}

	/**
	 * Normalise this vector
	 *
	 * @return this
	 */
	public final Vec2 normalise() {
		float len = length();
		if (len != 0.0f) {
			float l = 1.0f / len;
			return scale(l);
		}
		return this;
	}

	/**
	 * Translate a vector
	 *
	 * @param x The translation in x
	 * @param y the translation in y
	 * @return this
	 */
	public Vec2 translate(float x, float y) {
		this.x += x;
		this.y += y;
		return this;
	}

	/**
	 * Negate a vector
	 *
	 * @return this
	 */
	public Vec2 negate() {
		x = -x;
		y = -y;
		return this;
	}

	/**
	 * Negate a Vec2 and place the result in a destination vector.
	 *
	 * @param dest The destination Vec2 or null if a new Vec2 is to be created
	 * @return the negated vector
	 */
	public Vec2 negate(Vec2 dest) {
		if (dest == null) dest = new Vec2();
		dest.x = -x;
		dest.y = -y;
		return dest;
	}


	/**
	 * Normalise this Vec2 and place the result in another vector.
	 *
	 * @param dest The destination vector, or null if a new Vec2 is to be created
	 * @return the normalised vector
	 */
	public Vec2 normalise(Vec2 dest) {
		float l = length();

		if (dest == null) dest = new Vec2(x / l, y / l);
		else dest.set(x / l, y / l);

		return dest;
	}

	/**
	 * The dot product of two vectors is calculated as
	 * v1.x * v2.x + v1.y * v2.y + v1.z * v2.z
	 *
	 * @param left  The LHS vector
	 * @param right The RHS vector
	 * @return left dot right
	 */
	public static float dot(Vec2 left, Vec2 right) {
		return left.x * right.x + left.y * right.y;
	}


	/**
	 * Calculate the angle between two vectors, in radians
	 *
	 * @param a A vector
	 * @param b The other vector
	 * @return the angle between the two vectors, in radians
	 */
	public static float angle(Vec2 a, Vec2 b) {
		float dls = dot(a, b) / (a.length() * b.length());
		if (dls < -1f) dls = -1f;
		else if (dls > 1.0f) dls = 1.0f;
		return (float) Math.acos(dls);
	}

	/**
	 * Add a Vec2 to another Vec2 and place the result in a destination
	 * vector.
	 *
	 * @param left  The LHS vector
	 * @param right The RHS vector
	 * @param dest  The destination vector, or null if a new Vec2 is to be created
	 * @return the sum of left and right in dest
	 */
	public static Vec2 add(Vec2 left, Vec2 right, Vec2 dest) {
		if (dest == null) return new Vec2(left.x + right.x, left.y + right.y);
		else {
			dest.set(left.x + right.x, left.y + right.y);
			return dest;
		}
	}

	/**
	 * Subtract a Vec2 from another Vec2 and place the result in a destination
	 * vector.
	 *
	 * @param left  The LHS vector
	 * @param right The RHS vector
	 * @param dest  The destination vector, or null if a new Vec2 is to be created
	 * @return left minus right in dest
	 */
	public static Vec2 sub(Vec2 left, Vec2 right, Vec2 dest) {
		if (dest == null) return new Vec2(left.x - right.x, left.y - right.y);
		else {
			dest.set(left.x - right.x, left.y - right.y);
			return dest;
		}
	}

	/**
	 * Store this Vec2 in a FloatBuffer
	 *
	 * @param buf The buffer to store it in, at the current position
	 * @return this
	 */
	public Vec2 store(FloatBuffer buf) {
		buf.put(x);
		buf.put(y);
		return this;
	}

	/**
	 * Load this Vec2 from a FloatBuffer
	 *
	 * @param buf The buffer to load it from, at the current position
	 * @return this
	 */
	public Vec2 load(FloatBuffer buf) {
		x = buf.get();
		y = buf.get();
		return this;
	}

	/* (non-Javadoc)
	 * @see org.lwjgl.vector.Vector#scale(float)
	 */
	public Vec2 scale(float scale) {

		x *= scale;
		y *= scale;

		return this;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuilder sb = new StringBuilder(64);

		sb.append("Vec2[");
		sb.append(x);
		sb.append(", ");
		sb.append(y);
		sb.append(']');
		return sb.toString();
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

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Vec2)) return false;

		Vec2 vec2 = (Vec2) o;

		if (Float.compare(vec2.x, x) != 0) return false;
		if (Float.compare(vec2.y, y) != 0) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = (x != +0.0f ? Float.floatToIntBits(x) : 0);
		result = 31 * result + (y != +0.0f ? Float.floatToIntBits(y) : 0);
		return result;
	}

	public double distanceFrom(Vec2 v) {
		return Math.sqrt((v.x-x)*(v.x-x)+(v.y-y)*(v.y-y));
	}
}