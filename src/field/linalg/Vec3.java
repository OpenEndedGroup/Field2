package field.linalg;

import java.nio.FloatBuffer;

/**
 * Created by marc on 3/12/14.
 */
public class Vec3 {


	private static final long serialVersionUID = 1L;

	public float x, y, z;

	/**
	 * Constructor for Vec3.
	 */
	public Vec3() {
		super();
	}


	/**
	 * Constructor
	 */
	public Vec3(float x, float y, float z) {
		set(x, y, z);
	}

	public Vec3(Vec3 to) {
		set(to.x, to.y, to.z);
	}

	/* (non-Javadoc)
	 * @see org.lwjgl.util.vector.WritableVector2f#set(float, float)
	 */
	public void set(float x, float y) {
		this.x = x;
		this.y = y;
	}

	/* (non-Javadoc)
	 * @see org.lwjgl.util.vector.WritableVector3#set(float, float, float)
	 */
	public void set(float x, float y, float z) {
		this.x = x;
		this.y = y;
		this.z = z;
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
	public final float length() {
		return (float) Math.sqrt(lengthSquared());
	}

	/**
	 * @return the length squared of the vector
	 */
	public float lengthSquared() {
		return x * x + y * y + z * z;
	}

	/**
	 * Translate a vector
	 *
	 * @param x The translation in x
	 * @param y the translation in y
	 * @return this
	 */
	public Vec3 translate(float x, float y, float z) {
		this.x += x;
		this.y += y;
		this.z += z;
		return this;
	}

	/**
	 * Add a Vec3 to another Vec3 and place the result in a destination
	 * vector.
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
	 * Subtract a Vec3 from another Vec3 and place the result in a destination
	 * vector.
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
	 * Negate a vector
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
	 * Negate a Vec3 and place the result in a destination vector.
	 *
	 * @param dest The destination Vec3 or null if a new Vec3 is to be created
	 * @return the negated vector
	 */
	public Vec3 negate(Vec3 dest) {
		if (dest == null) dest = new Vec3();
		dest.x = -x;
		dest.y = -y;
		dest.z = -z;
		return dest;
	}


	/**
	 * Normalise this Vec3 and place the result in another vector.
	 *
	 * @param dest The destination vector, or null if a new Vec3 is to be created
	 * @return the normalised vector
	 */
	public Vec3 normalise(Vec3 dest) {
		float l = length();

		if (dest == null) dest = new Vec3(x / l, y / l, z / l);
		else dest.set(x / l, y / l, z / l);

		return dest;
	}

	/**
	 * Normalise this Vec3 inplace.
	 *
	 * @return this
	 */
	public Vec3 normalise() {
		float l = length();

		set(x / l, y / l, z / l);
		return this;
	}

	/**
	 * The dot product of two vectors is calculated as
	 * v1.x * v2.x + v1.y * v2.y + v1.z * v2.z
	 *
	 * @param left  The LHS vector
	 * @param right The RHS vector
	 * @return left dot right
	 */
	public static float dot(Vec3 left, Vec3 right) {
		return left.x * right.x + left.y * right.y + left.z * right.z;
	}

	/**
	 * Calculate the angle between two vectors, in radians
	 *
	 * @param a A vector
	 * @param b The other vector
	 * @return the angle between the two vectors, in radians
	 */
	public static float angle(Vec3 a, Vec3 b) {
		float dls = dot(a, b) / (a.length() * b.length());
		if (dls < -1f) dls = -1f;
		else if (dls > 1.0f) dls = 1.0f;
		return (float) Math.acos(dls);
	}

	/* (non-Javadoc)
	 * @see org.lwjgl.vector.Vector#load(FloatBuffer)
	 */
	public Vec3 load(FloatBuffer buf) {
		x = buf.get();
		y = buf.get();
		z = buf.get();
		return this;
	}

	/* (non-Javadoc)
	 * @see org.lwjgl.vector.Vector#scale(float)
	 */
	public Vec3 scale(float scale) {

		x *= scale;
		y *= scale;
		z *= scale;

		return this;

	}

	/* (non-Javadoc)
	 * @see org.lwjgl.vector.Vector#store(FloatBuffer)
	 */
	public Vec3 store(FloatBuffer buf) {

		buf.put(x);
		buf.put(y);
		buf.put(z);

		return this;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
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

	/* (Overrides)
	 * @see org.lwjgl.vector.ReadableVector3#getZ()
	 */
	public float getZ() {
		return z;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Vec3)) return false;

		Vec3 vec3 = (Vec3) o;

		if (Float.compare(vec3.x, x) != 0) return false;
		if (Float.compare(vec3.y, y) != 0) return false;
		if (Float.compare(vec3.z, z) != 0) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = (x != +0.0f ? Float.floatToIntBits(x) : 0);
		result = 31 * result + (y != +0.0f ? Float.floatToIntBits(y) : 0);
		result = 31 * result + (z != +0.0f ? Float.floatToIntBits(z) : 0);
		return result;
	}

	public double distanceFrom(Vec3 v) {
		return Math.sqrt((v.x-x)*(v.x-x)+(v.y-y)*(v.y-y)+(v.z-z)*(v.z-z));
	}


	/**
	 * returns Vec2(x, y);
	 */
	public Vec2 toVec2() {
		return new Vec2(x, y);
	}
}
