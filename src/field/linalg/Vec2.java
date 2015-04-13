package field.linalg;

import fieldnashorn.annotations.SafeToToString;

import java.io.Serializable;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.util.function.Supplier;

/**
 * A class representing a 2-vector (both a position and direction in 2-space).
 */
public class Vec2 implements Supplier<Vec2>, Serializable {

	private static final long serialVersionUID = 1L;


	@SafeToToString
	public double x;
	@SafeToToString
	public double y;

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
	public Vec2(double x, double y) {
		set(x, y);
	}

	public Vec2(FloatBuffer f) {
		set(f.get(), f.get());
	}

	public Vec2(FloatBuffer f, int index) {
		set(f.get(index), f.get(index + 1));
	}

	/**
	 * The dot product of two vectors is calculated as v1.x * v2.x + v1.y * v2.y + v1.z * v2.z
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
	 * Add a Vec2 to another Vec2 and place the result in a destination vector.
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
	 * Add a Vec2 times a scalar to another Vec2 and place the result in a destination vector. Returns left + w*right
	 *
	 * @param left  The LHS vector
	 * @param right The RHS vector
	 * @param dest  The destination vector, or null if a new Vec2 is to be created
	 * @return the sum of left and right in dest
	 */
	public static Vec2 add(Vec2 left, float w, Vec2 right, Vec2 dest) {
		if (dest == null) return new Vec2(left.x + right.x, left.y + right.y);
		else {
			dest.set(left.x + w * right.x, left.y + w * right.y);
			return dest;
		}
	}

	/**
	 * Subtract a Vec2 from another Vec2 and place the result in a destination vector.
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
	 * blend two Vec2 to create a third. out can contain a pre-allocated return Vec2 or null
	 */

	static public Vec2 lerp(Vec2 a, Vec2 b, double alpha, Vec2 out) {
		if (out == null) out = new Vec2();

		out.x = a.x * (1 - alpha) + b.x * (alpha);
		out.y = a.y * (1 - alpha) + b.y * (alpha);

		return out;
	}

	/**
	 * sets this Vec2 to be the value given by x,y
	 *
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
	@SafeToToString
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
	 * add a vector
	 *
	 * @param x The translation in x
	 * @param y the translation in y
	 * @return this
	 */
	public Vec2 add(double x, double y) {
		this.x += x;
		this.y += y;
		return this;
	}

	/**
	 * add a vector
	 *
	 * @param v the vec2 to add
	 * @return this
	 */
	public Vec2 add(Vec2 v) {
		this.x += v.x;
		this.y += v.y;
		return this;
	}

	/**
	 * add a vector times a scalar
	 *
	 * @param v the vec2 to add
	 * @return this
	 */
	public Vec2 add(Vec2 v, float w) {
		this.x += v.x * w;
		this.y += v.y * w;
		return this;
	}

	/**
	 * subtracts a vector
	 *
	 * @param v the vec2 to add
	 * @return this
	 */
	public Vec2 sub(Vec2 v) {
		this.x -= v.x;
		this.y -= v.y;
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
	 * The dot product of two vectors is calculated as v1.x * v2.x + v1.y * v2.y + v1.z * v2.z
	 *
	 * @return this dot right
	 */
	public double dot(Vec2 right) {
		return dot(this, right);
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

	public Vec2 scale(double scale) {

		x *= scale;
		y *= scale;

		return this;
	}

	@SafeToToString
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
	@SafeToToString
	public final double getX() {
		return x;
	}

	/**
	 * Set X
	 *
	 * @param x
	 */
	public final Vec2 setX(double x) {
		this.x = x;
		return this;
	}

	/**
	 * @return y
	 */
	@SafeToToString
	public final double getY() {
		return y;

	}

	/**
	 * Set Y
	 *
	 * @param y
	 */
	public final Vec2 setY(double y) {
		this.y = y;
		return this;
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
		return (int) (result ^ (result >>> 32));
	}

	/**
	 * returns the Euclidean distance between 'this' and 'v'
	 */
	public double distanceFrom(Vec2 v) {
		return Math.sqrt((v.x - x) * (v.x - x) + (v.y - y) * (v.y - y));
	}

	/**
	 * returns the Euclidean distance between 'this' and 'v' squared
	 */
	public double distanceFromSquared(Vec2 v) {
		return ((v.x - x) * (v.x - x) + (v.y - y) * (v.y - y) );
	}

	/**
	 * set this Vec2 to the blend of two Vec2
	 */
	public Vec2 lerp(Vec2 a, Vec2 b, double alpha) {
		this.x = a.x * (1 - alpha) + b.x * (alpha);
		this.y = a.y * (1 - alpha) + b.y * (alpha);

		return this;
	}

	@Override
	@SafeToToString
	public Vec2 get() {
		return this;
	}

	/**
	 * returns this Vec2 as a Vec3 (z=0)
	 *
	 * @return
	 */
	@SafeToToString
	public Vec3 toVec3() {
		return new Vec3(x, y, 0);
	}

	/**
	 * returns this Vec2 as a Vec3
	 *
	 * @return
	 */
	@SafeToToString
	public Vec3 toVec3(double z) {
		return new Vec3(x, y, z);
	}

	/**
	 * replaces this with the element-wise min of this and 's'
	 */
	public Vec2 min(Vec2 s) {
		x = Math.min(s.x, x);
		y = Math.min(s.y, y);
		return this;
	}

	/**
	 * replaces this with the element-wise min of this and 's'
	 */
	public Vec2 max(Vec2 s) {
		x = Math.max(s.x, x);
		y = Math.max(s.y, y);
		return this;
	}

	/**
	 * is any component NaN?
	 *
	 * @return
	 */
	public boolean isNaN() {
		return Double.isNaN(x) || Double.isNaN(y);

	}


}