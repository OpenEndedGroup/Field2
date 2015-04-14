package field.linalg;

import field.utility.Mutable;

import java.io.Serializable;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.util.function.Supplier;

/**
 * A class representing a 3-vector (both a position and direction in 3-space).
 */
public class Vec3 implements Supplier<Vec3>, Serializable, Mutable<Vec3> {

	private static final long serialVersionUID = 1L;

	public double x, y, z;

	/**
	 * Constructor for Vec3.
	 */
	public Vec3() {
		super();
	}


	/**
	 * Constructor
	 */
	public Vec3(double x, double y, double z) {
		set(x, y, z);
	}

	public Vec3(Vec3 to) {
		set(to.x, to.y, to.z);
	}

	public Vec3(FloatBuffer f) {
		set(f.get(), f.get(), f.get());
	}


	public Vec3(FloatBuffer f, int index)
	{
		set(f.get(index), f.get(index+1), f.get(index+2));
	}
	/**
	 * Add a Vec3 to another Vec3 and place the result in a destination vector.
	 *
	 * @param left  The LHS vector
	 * @param right The RHS vector
	 * @param dest  The destination vector, or null if a new Vec3 is to be created
	 * @return the sum of left and right in dest
	 */
	public static Vec3 add(Vec3 left, Vec3 right, Vec3 dest) {
		if (dest == null) return new Vec3(left.x + right.x, left.y + right.y, left.z + right.z);
		else {
			dest.set(left.x + right.x, left.y + right.y, left.z + right.z);
			return dest;
		}
	}

	/**
	 * Add a Vec3 to another Vec3 times a scalar and place the result in a destination vector.
	 *
	 * @param left  The LHS vector
	 * @param w     the weight
	 * @param right The RHS vector
	 * @param dest  The destination vector, or null if a new Vec3 is to be created
	 * @return the sum of left and right in dest
	 */
	public static Vec3 add(Vec3 left, double w, Vec3 right, Vec3 dest) {
		if (dest == null) return new Vec3(left.x + w * right.x, left.y + w * right.y, left.z + w * right.z);
		else {
			dest.set(left.x + w * right.x, left.y + w * right.y, left.z + w * right.z);
			return dest;
		}
	}

	/**
	 * Subtract a Vec3 from another Vec3 and place the result in a destination vector.
	 *
	 * @param left  The LHS vector
	 * @param right The RHS vector
	 * @param dest  The destination vector, or null if a new Vec3 is to be created
	 * @return left minus right in dest
	 */
	public static Vec3 sub(Vec3 left, Vec3 right, Vec3 dest) {
		if (dest == null) return new Vec3(left.x - right.x, left.y - right.y, left.z - right.z);
		else {
			dest.set(left.x - right.x, left.y - right.y, left.z - right.z);
			return dest;
		}
	}

	/**
	 * The cross product of two vectors.
	 *
	 * @param left  The LHS vector
	 * @param right The RHS vector
	 * @param dest  The destination result, or null if a new Vec3 is to be created
	 * @return left cross right
	 */
	public static Vec3 cross(Vec3 left, Vec3 right, Vec3 dest) {

		if (dest == null) dest = new Vec3();

		dest.set(left.y * right.z - left.z * right.y, right.x * left.z - right.z * left.x, left.x * right.y - left.y * right.x);

		return dest;
	}

	/**
	 * The dot product of two vectors is calculated as v1.x * v2.x + v1.y * v2.y + v1.z * v2.z
	 *
	 * @param left  The LHS vector
	 * @param right The RHS vector
	 * @return left dot right
	 */
	public static double dot(Vec3 left, Vec3 right) {
		return left.x * right.x + left.y * right.y + left.z * right.z;
	}

	/**
	 * Calculate the angle between two vectors, in radians
	 *
	 * @param a A vector
	 * @param b The other vector
	 * @return the angle between the two vectors, in radians
	 */
	public static double angle(Vec3 a, Vec3 b) {
		double dls = dot(a, b) / (a.length() * b.length());
		if (dls < -1f) dls = -1f;
		else if (dls > 1.0f) dls = 1.0f;
		return (double) Math.acos(dls);
	}

	/**
	 * blend two Vec3 to create a third. out can contain a pre-allocated return Vec3 or null
	 */

	static public Vec3 lerp(Vec3 a, Vec3 b, double alpha, Vec3 out) {
		if (out == null) out = new Vec3();

		out.x = a.x * (1 - alpha) + b.x * (alpha);
		out.y = a.y * (1 - alpha) + b.y * (alpha);
		out.z = a.z * (1 - alpha) + b.z * (alpha);

		return out;
	}

	public Vec3 set(double x, double y) {
		this.x = x;
		this.y = y;
		return this;
	}

	public Vec3 set(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
		return this;
	}

	/**
	 * Load from another Vec3
	 *
	 * @param src The source vector
	 * @return this
	 */
	public Vec3 set(Vec3 src) {
		x = src.getX();
		y = src.getY();
		z = src.getZ();
		return this;
	}

	/**
	 * @return the length of the vector
	 */
	public final double length() {
		return (double) Math.sqrt(lengthSquared());
	}

	/**
	 * @return the length squared of the vector
	 */
	public double lengthSquared() {
		return x * x + y * y + z * z;
	}

	/**
	 * add a vector
	 *
	 * @param x The translation in x
	 * @param y the translation in y
	 * @return this
	 */
	public Vec3 add(double x, double y, double z) {
		this.x += x;
		this.y += y;
		this.z += z;
		return this;
	}

	/**
	 * add a vector
	 *
	 * @param v the vector to add
	 * @return this
	 */
	public Vec3 add(Vec3 v) {
		this.x += v.x;
		this.y += v.y;
		this.z += v.z;
		return this;
	}

	/**
	 * add a vector times a scalar
	 *
	 * @param v the vector to add
	 * @return this
	 */
	public Vec3 add(Vec3 v, double w) {
		this.x += v.x * w;
		this.y += v.y * w;
		this.z += v.z * w;
		return this;
	}

	/**
	 * subtract a vector times a scalar
	 *
	 * @param v the vector to sub
	 * @return this
	 */
	public Vec3 sub(Vec3 v) {
		this.x -= v.x;
		this.y -= v.y;
		this.z -= v.z;
		return this;
	}

	/**
	 * element-size multiplication of a vector
	 *
	 * @param v the vector to mul
	 * @return this
	 */
	public Vec3 mul(Vec3 v) {
		this.x *= v.x;
		this.y *= v.y;
		this.z *= v.z;
		return this;
	}

	/**
	 * Negate this
	 *
	 * @return this
	 */
	public Vec3 negate() {
		x = -x;
		y = -y;
		z = -z;
		return this;
	}

	/**
	 * Normalise this Vec3 inplace.
	 *
	 * @return this
	 */
	public Vec3 normalise() {
		double l = length();

		set(x / l, y / l, z / l);
		return this;
	}

	/**
	 * load a value into this from the next three values in this Buffer
	 *
	 * @param buf
	 * @return
	 */
	public Vec3 load(FloatBuffer buf) {
		x = buf.get();
		y = buf.get();
		z = buf.get();
		return this;
	}

	/**
	 * load a value into this from the next three values in this Buffer
	 *
	 * @param buf
	 * @return
	 */
	public Vec3 load(DoubleBuffer buf) {
		x = buf.get();
		y = buf.get();
		z = buf.get();
		return this;
	}

	/**
	 * scales this Vec3 by 'scale'
	 *
	 * @param scale
	 * @return
	 */
	public Vec3 scale(double scale) {

		x *= scale;
		y *= scale;
		z *= scale;

		return this;

	}

	/**
	 * stores this into the next three values ofthis Buffer
	 *
	 * @param buf
	 * @return
	 */
	public Vec3 store(FloatBuffer buf) {

		buf.put((float) x);
		buf.put((float) y);
		buf.put((float) z);

		return this;
	}

	/**
	 * stores this into the next three values ofthis Buffer
	 *
	 * @param buf
	 * @return
	 */
	public Vec3 store(DoubleBuffer buf) {

		buf.put((float) x);
		buf.put((float) y);
		buf.put((float) z);

		return this;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder(64);

		sb.append("Vec3[");
		sb.append(x);
		sb.append(", ");
		sb.append(y);
		sb.append(", ");
		sb.append(z);
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
	 * Set X, return this
	 *
	 * @param x
	 */
	public final Vec3 setX(double x) {
		this.x = x;
		return this;
	}

	/**
	 * @return y
	 */
	public final double getY() {
		return y;
	}

	/**
	 * Set Y
	 *
	 * @param y
	 */
	public final Vec3 setY(double y) {
		this.y = y;
		return this;
	}

	public double getZ() {
		return z;
	}

	/**
	 * Set Z
	 *
	 * @param z
	 */
	public Vec3 setZ(double z) {
		this.z = z;
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Vec3)) return false;

		Vec3 vec3 = (Vec3) o;

		if (Double.compare(vec3.x, x) != 0) return false;
		if (Double.compare(vec3.y, y) != 0) return false;
		if (Double.compare(vec3.z, z) != 0) return false;

		return true;
	}

	@Override
	public int hashCode() {
		long result = (x != +0.0f ? Double.doubleToLongBits(x) : 0);
		result = 31 * result + (y != +0.0f ? Double.doubleToLongBits(y) : 0);
		result = 31 * result + (z != +0.0f ? Double.doubleToLongBits(z) : 0);
		return (int) (result ^ (result >>> 32));
	}

	/**
	 * returns the Euclidean distance between 'this' and 'v'
	 */
	public double distanceFrom(Vec3 v) {
		return Math.sqrt((v.x - x) * (v.x - x) + (v.y - y) * (v.y - y) + (v.z - z) * (v.z - z));
	}/**
	 * returns the Euclidean distance between 'this' and 'v' squared
	 */
	public double distanceFromSquared(Vec3 v) {
		return ((v.x - x) * (v.x - x) + (v.y - y) * (v.y - y) + (v.z - z) * (v.z - z));
	}

	/**
	 * returns Vec2(x, y);
	 */
	public Vec2 toVec2() {
		return new Vec2(x, y);
	}

	/**
	 * set this Vec3 to the blend of two Vec3
	 */
	public Vec3 lerp(Vec3 a, Vec3 b, double alpha) {
		this.x = a.x * (1 - alpha) + b.x * (alpha);
		this.y = a.y * (1 - alpha) + b.y * (alpha);
		this.z = a.z * (1 - alpha) + b.z * (alpha);

		return this;
	}

	/**
	 * copies this Vec3
	 */
	public Vec3 clone() {
		return new Vec3(this);
	}


	/**
	 * Dot product of this and another Vec3
	 */
	public double dot(Vec3 a) {
		return Vec3.dot(this, a);
	}

	@Override
	public Vec3 get() {
		return this;
	}

	/**
	 * returns true if any component of this vector is NaN
	 */
	public boolean isNaN() {
		return (x!=x || y!=y || z!=z);
	}

	/**
	 * set this to be the min of 's' and this
	 * @param s
	 * @return
	 */
	public Vec3 min(Vec3 s) {
		this.x = Math.min(this.x, s.x);
		this.y = Math.min(this.y, s.y);
		this.z = Math.min(this.z, s.z);
		return this;
	}

	/**
	 * set this to be the max of 's' and this
	 * @param s
	 * @return
	 */
	public Vec3 max(Vec3 s) {
		this.x = Math.max(this.x, s.x);
		this.y = Math.max(this.y, s.y);
		this.z = Math.max(this.z, s.z);
		return this;
	}

	@Override
	public Vec3 duplicate() {
		return new Vec3(this);
	}
}
