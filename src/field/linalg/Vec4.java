package field.linalg;

import java.nio.DoubleBuffer;

/**
 * A class representing a 4-vector (useful for colors and homogeneous coordinates)
 */
public class Vec4 {

	private static final long serialVersionUID = 1L;

	public double x, y, z, w;

	/**
	 * Constructor for Vec4.
	 */
	public Vec4() {
		super();
	}

	/**
	 * Constructor
	 */
	public Vec4(Vec4 src) {
		set(src);
	}

	/**
	 * Constructor
	 */
	public Vec4(double x, double y, double z, double w) {
		set(x, y, z, w);
	}

	public void set(double x, double y) {
		this.x = x;
		this.y = y;
	}

	public void set(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public void set(double x, double y, double z, double w) {
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
	public Vec4 set(Vec4 src) {
		x = src.getX();
		y = src.getY();
		z = src.getZ();
		w = src.getW();
		return this;
	}

	/**
	 * @return the length squared of the vector
	 */
	public double lengthSquared() {
		return x * x + y * y + z * z + w * w;
	}

	/**
	 * Translate a vector
	 *
	 * @param x The translation in x
	 * @param y the translation in y
	 * @return this
	 */
	public Vec4 translate(double x, double y, double z, double w) {
		this.x += x;
		this.y += y;
		this.z += z;
		this.w += w;
		return this;
	}

	/**
	 * Add a Vec4 to another Vec4 and place the result in a destination
	 * vector.
	 *
	 * @param left  The LHS vector
	 * @param right The RHS vector
	 * @param dest  The destination vector, or null if a new Vec4 is to be created
	 * @return the sum of left and right in dest
	 */
	public static Vec4 add(Vec4 left, Vec4 right, Vec4 dest) {
		if (dest == null)
			return new Vec4(left.x + right.x, left.y + right.y, left.z + right.z, left.w + right.w);
		else {
			dest.set(left.x + right.x, left.y + right.y, left.z + right.z, left.w + right.w);
			return dest;
		}
	}

	/**
	 * Subtract a Vec4 from another Vec4 and place the result in a destination
	 * vector.
	 *
	 * @param left  The LHS vector
	 * @param right The RHS vector
	 * @param dest  The destination vector, or null if a new Vec4 is to be created
	 * @return left minus right in dest
	 */
	public static Vec4 sub(Vec4 left, Vec4 right, Vec4 dest) {
		if (dest == null)
			return new Vec4(left.x - right.x, left.y - right.y, left.z - right.z, left.w - right.w);
		else {
			dest.set(left.x - right.x, left.y - right.y, left.z - right.z, left.w - right.w);
			return dest;
		}
	}


	/**
	 * Negate a vector
	 *
	 * @return this
	 */
	public Vec4 negate() {
		x = -x;
		y = -y;
		z = -z;
		w = -w;
		return this;
	}

	/**
	 * Negate a Vec4 and place the result in a destination vector.
	 *
	 * @param dest The destination Vec4 or null if a new Vec4 is to be created
	 * @return the negated vector
	 */
	public Vec4 negate(Vec4 dest) {
		if (dest == null) dest = new Vec4();
		dest.x = -x;
		dest.y = -y;
		dest.z = -z;
		dest.w = -w;
		return dest;
	}

	/**
	 * @return the length of the vector
	 */
	public final double length() {
		return (double) Math.sqrt(lengthSquared());
	}

	/**
	 * Normalise this Vec4 and place the result in another vector.
	 *
	 * @param dest The destination vector, or null if a new Vec4 is to be created
	 * @return the normalised vector
	 */
	public Vec4 normalise(Vec4 dest) {
		double l = length();

		if (dest == null) dest = new Vec4(x / l, y / l, z / l, w / l);
		else dest.set(x / l, y / l, z / l, w / l);

		return dest;
	}

	/**
	 * The dot product of two vectors is calculated as
	 * v1.x * v2.x + v1.y * v2.y + v1.z * v2.z + v1.w * v2.w
	 *
	 * @param left  The LHS vector
	 * @param right The RHS vector
	 * @return left dot right
	 */
	public static double dot(Vec4 left, Vec4 right) {
		return left.x * right.x + left.y * right.y + left.z * right.z + left.w * right.w;
	}

	/**
	 * Calculate the angle between two vectors, in radians
	 *
	 * @param a A vector
	 * @param b The other vector
	 * @return the angle between the two vectors, in radians
	 */
	public static double angle(Vec4 a, Vec4 b) {
		double dls = dot(a, b) / (a.length() * b.length());
		if (dls < -1f) dls = -1f;
		else if (dls > 1.0f) dls = 1.0f;
		return (double) Math.acos(dls);
	}

	public Vec4 load(DoubleBuffer buf) {
		x = buf.get();
		y = buf.get();
		z = buf.get();
		w = buf.get();
		return this;
	}

	public Vec4 scale(double scale) {
		x *= scale;
		y *= scale;
		z *= scale;
		w *= scale;
		return this;
	}

	public Vec4 store(DoubleBuffer buf) {

		buf.put(x);
		buf.put(y);
		buf.put(z);
		buf.put(w);

		return this;
	}

	public String toString() {
		return "Vec4: " + x + " " + y + " " + z + " " + w;
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

	/**
	 * Set Z
	 *
	 * @param z
	 */
	public void setZ(double z) {
		this.z = z;
	}


	/* (Overrides)
	 * @see org.lwjgl.vector.ReadableVector3f#getZ()
	 */
	public double getZ() {
		return z;
	}

	/**
	 * Set W
	 *
	 * @param w
	 */
	public void setW(double w) {
		this.w = w;
	}

	public double getW() {
		return w;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Vec4)) return false;

		Vec4 vec4 = (Vec4) o;

		if (Double.compare(vec4.w, w) != 0) return false;
		if (Double.compare(vec4.x, x) != 0) return false;
		if (Double.compare(vec4.y, y) != 0) return false;
		if (Double.compare(vec4.z, z) != 0) return false;

		return true;
	}

	@Override
	public int hashCode() {
		long result = (x != +0.0f ? Double.doubleToLongBits(x) : 0);
		result = 31 * result + (y != +0.0f ? Double.doubleToLongBits(y) : 0);
		result = 31 * result + (z != +0.0f ? Double.doubleToLongBits(z) : 0);
		result = 31 * result + (w != +0.0f ? Double.doubleToLongBits(w) : 0);
		return (int)(result ^ (result >>> 32));
	}

	public double distanceFrom(Vec4 v) {
		return Math.sqrt((v.x-x)*(v.x-x)+(v.y-y)*(v.y-y)+(v.z-z)*(v.z-z)+(v.w-w)*(v.w-w));
	}

}
