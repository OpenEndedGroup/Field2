package field.linalg;

import java.nio.FloatBuffer;

/**
 * Created by marc on 3/12/14.
 */
public class Vec4 {

	private static final long serialVersionUID = 1L;

	public float x, y, z, w;

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
	public Vec4(float x, float y, float z, float w) {
		set(x, y, z, w);
	}

	/* (non-Javadoc)
	 * @see org.lwjgl.util.vector.WritableVector2f#set(float, float)
	 */
	public void set(float x, float y) {
		this.x = x;
		this.y = y;
	}

	/* (non-Javadoc)
	 * @see org.lwjgl.util.vector.WritableVector3f#set(float, float, float)
	 */
	public void set(float x, float y, float z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	/* (non-Javadoc)
	 * @see org.lwjgl.util.vector.WritableVector4#set(float, float, float, float)
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
	public float lengthSquared() {
		return x * x + y * y + z * z + w * w;
	}

	/**
	 * Translate a vector
	 *
	 * @param x The translation in x
	 * @param y the translation in y
	 * @return this
	 */
	public Vec4 translate(float x, float y, float z, float w) {
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
	public final float length() {
		return (float) Math.sqrt(lengthSquared());
	}

	/**
	 * Normalise this Vec4 and place the result in another vector.
	 *
	 * @param dest The destination vector, or null if a new Vec4 is to be created
	 * @return the normalised vector
	 */
	public Vec4 normalise(Vec4 dest) {
		float l = length();

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
	public static float dot(Vec4 left, Vec4 right) {
		return left.x * right.x + left.y * right.y + left.z * right.z + left.w * right.w;
	}

	/**
	 * Calculate the angle between two vectors, in radians
	 *
	 * @param a A vector
	 * @param b The other vector
	 * @return the angle between the two vectors, in radians
	 */
	public static float angle(Vec4 a, Vec4 b) {
		float dls = dot(a, b) / (a.length() * b.length());
		if (dls < -1f) dls = -1f;
		else if (dls > 1.0f) dls = 1.0f;
		return (float) Math.acos(dls);
	}

	/* (non-Javadoc)
	 * @see org.lwjgl.vector.Vector#load(FloatBuffer)
	 */
	public Vec4 load(FloatBuffer buf) {
		x = buf.get();
		y = buf.get();
		z = buf.get();
		w = buf.get();
		return this;
	}

	/* (non-Javadoc)
	 * @see org.lwjgl.vector.Vector#scale(float)
	 */
	public Vec4 scale(float scale) {
		x *= scale;
		y *= scale;
		z *= scale;
		w *= scale;
		return this;
	}

	/* (non-Javadoc)
	 * @see org.lwjgl.vector.Vector#store(FloatBuffer)
	 */
	public Vec4 store(FloatBuffer buf) {

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

	/* (Overrides)
	 * @see org.lwjgl.vector.ReadableVector3f#getZ()
	 */
	public float getW() {
		return w;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Vec4)) return false;

		Vec4 vec4 = (Vec4) o;

		if (Float.compare(vec4.w, w) != 0) return false;
		if (Float.compare(vec4.x, x) != 0) return false;
		if (Float.compare(vec4.y, y) != 0) return false;
		if (Float.compare(vec4.z, z) != 0) return false;

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

	public double distanceFrom(Vec4 v) {
		return Math.sqrt((v.x-x)*(v.x-x)+(v.y-y)*(v.y-y)+(v.z-z)*(v.z-z)+(v.w-w)*(v.w-w));
	}

}
