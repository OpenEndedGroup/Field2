package field.linalg;

import java.io.Serializable;
import java.nio.DoubleBuffer;

/**
 * A class representing a 2-vector (both a position and direction in 2-space).
 */
public class Vec2 implements Serializable {

	private static final long serialVersionUID = 1L;

	public double x, y;

	/**
	 * Constructor for Vector2f.
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
	public Vec2(double x, double y)
	{
		set(x, y);
	}

	/**
	 * sets this Vec2 to be the value given by x,y
	 * @param x
	 * @param y
	 */
	public void set(double x, double y) {
		this.x = x;
		this.y = y;
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
	public double lengthSquared() {
		return x * x + y * y;
	}

	/**
	 * @return the length of the vector
	 */
	public final double length() {
		return (double) Math.sqrt(lengthSquared());
	}

	/**
	 * Normalise this vector
	 *
	 * @return this
	 */
	public final Vec2 normalise() {
		double len = length();
		if (len != 0.0f) {
			double l = 1.0f / len;
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
	public Vec2 translate(double x, double y) {
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
		double l = length();

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
	public static double dot(Vec2 left, Vec2 right) {
		return left.x * right.x + left.y * right.y;
	}


	/**
	 * Calculate the angle between two vectors, in radians
	 *
	 * @param a A vector
	 * @param b The other vector
	 * @return the angle between the two vectors, in radians
	 */
	public static double angle(Vec2 a, Vec2 b) {
		double dls = dot(a, b) / (a.length() * b.length());
		if (dls < -1f) dls = -1f;
		else if (dls > 1.0f) dls = 1.0f;
		return (double) Math.acos(dls);
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
	 * Store this Vec2 in a doubleBuffer
	 *
	 * @param buf The buffer to store it in, at the current position
	 * @return this
	 */
	public Vec2 store(DoubleBuffer buf) {
		buf.put(x);
		buf.put(y);
		return this;
	}

	/**
	 * Load this Vec2 from a doubleBuffer
	 *
	 * @param buf The buffer to load it from, at the current position
	 * @return this
	 */
	public Vec2 load(DoubleBuffer buf) {
		x = buf.get();
		y = buf.get();
		return this;
	}

	/* (non-Javadoc)
	 * @see org.lwjgl.vector.Vector#scale(double)
	 */
	public Vec2 scale(double scale) {

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
	public final double getX() {
		return x;
	}

	/**
	 * @return y
	 */
	public final double getY() {
		return y;
	}

	/**
	 * Set X
	 *
	 * @param x
	 */
	public final void setX(double x) {
		this.x = x;
	}

	/**
	 * Set Y
	 *
	 * @param y
	 */
	public final void setY(double y) {
		this.y = y;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Vec2)) return false;

		Vec2 vec2 = (Vec2) o;

		if (Double.compare(vec2.x, x) != 0) return false;
		if (Double.compare(vec2.y, y) != 0) return false;

		return true;
	}

	@Override
	public int hashCode() {
		long result = (x != +0.0f ? Double.doubleToLongBits(x) : 0);
		result = 31 * result + (y != +0.0f ? Double.doubleToLongBits(y) : 0);
		return (int)(result ^ (result >>> 32));
	}

	public double distanceFrom(Vec2 v) {
		return Math.sqrt((v.x-x)*(v.x-x)+(v.y-y)*(v.y-y));
	}

	/**
	 * blend two Vec2 to create a third. out can contain a pre-allocated return Vec2 or null
	 */

	static public Vec2 lerp(Vec2 a, Vec2 b, double alpha, Vec2 out)
	{
		if (out==null) out = new Vec2();

		out.x = a.x*alpha+b.x*(1-alpha);
		out.y = a.y*alpha+b.y*(1-alpha);

		return out;
	}


	/**
	 * set this Vec2 to the blend of two Vec2
	 */
	public Vec2 lerp(Vec2 a, Vec2 b, double alpha)
	{
		this.x = a.x*alpha+b.x*(1-alpha);
		this.y = a.y*alpha+b.y*(1-alpha);

		return this;
	}
}