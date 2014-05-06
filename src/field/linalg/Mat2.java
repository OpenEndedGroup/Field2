package field.linalg;

import java.io.Serializable;
import java.nio.FloatBuffer;

/**
 * Created by marc on 3/12/14.
 */
public class Mat2 implements Serializable {

	private static final long serialVersionUID = 1L;

	public float m00, m01, m10, m11;

	/**
	 * Constructor for Mat2. The Mat2 is initialised to the identity.
	 */
	public Mat2() {
		setIdentity();
	}

	/**
	 * Constructor
	 */
	public Mat2(Mat2 src) {
		load(src);
	}

	/**
	 * Load from another matrix
	 *
	 * @param src The source matrix
	 * @return this
	 */
	public Mat2 load(Mat2 src) {
		return load(src, this);
	}

	/**
	 * Copy the source Mat2 to the destination matrix.
	 *
	 * @param src  The source matrix
	 * @param dest The destination matrix, or null if a new one should be created.
	 * @return The copied matrix
	 */
	public static Mat2 load(Mat2 src, Mat2 dest) {
		if (dest == null) dest = new Mat2();

		dest.m00 = src.m00;
		dest.m01 = src.m01;
		dest.m10 = src.m10;
		dest.m11 = src.m11;

		return dest;
	}

	/**
	 * Load from a float buffer. The buffer stores the Mat2 in column major
	 * (OpenGL) order.
	 *
	 * @param buf A float buffer to read from
	 * @return this
	 */
	public Mat2 load(FloatBuffer buf) {

		m00 = buf.get();
		m01 = buf.get();
		m10 = buf.get();
		m11 = buf.get();

		return this;
	}

	/**
	 * Load from a float buffer. The buffer stores the Mat2 in row major
	 * (mathematical) order.
	 *
	 * @param buf A float buffer to read from
	 * @return this
	 */
	public Mat2 loadTranspose(FloatBuffer buf) {

		m00 = buf.get();
		m10 = buf.get();
		m01 = buf.get();
		m11 = buf.get();

		return this;
	}

	/**
	 * Store this Mat2 in a float buffer. The Mat2 is stored in column
	 * major (openGL) order.
	 *
	 * @param buf The buffer to store this Mat2 in
	 */
	public Mat2 store(FloatBuffer buf) {
		buf.put(m00);
		buf.put(m01);
		buf.put(m10);
		buf.put(m11);
		return this;
	}

	/**
	 * Store this Mat2 in a float buffer. The Mat2 is stored in row
	 * major (maths) order.
	 *
	 * @param buf The buffer to store this Mat2 in
	 */
	public Mat2 storeTranspose(FloatBuffer buf) {
		buf.put(m00);
		buf.put(m10);
		buf.put(m01);
		buf.put(m11);
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
	public static Mat2 add(Mat2 left, Mat2 right, Mat2 dest) {
		if (dest == null) dest = new Mat2();

		dest.m00 = left.m00 + right.m00;
		dest.m01 = left.m01 + right.m01;
		dest.m10 = left.m10 + right.m10;
		dest.m11 = left.m11 + right.m11;

		return dest;
	}

	/**
	 * Subtract the right Mat2 from the left and place the result in a third matrix.
	 *
	 * @param left  The left source matrix
	 * @param right The right source matrix
	 * @param dest  The destination matrix, or null if a new one is to be created
	 * @return the destination matrix
	 */
	public static Mat2 sub(Mat2 left, Mat2 right, Mat2 dest) {
		if (dest == null) dest = new Mat2();

		dest.m00 = left.m00 - right.m00;
		dest.m01 = left.m01 - right.m01;
		dest.m10 = left.m10 - right.m10;
		dest.m11 = left.m11 - right.m11;

		return dest;
	}

	/**
	 * Multiply the right Mat2 by the left and place the result in a third matrix.
	 *
	 * @param left  The left source matrix
	 * @param right The right source matrix
	 * @param dest  The destination matrix, or null if a new one is to be created
	 * @return the destination matrix
	 */
	public static Mat2 mul(Mat2 left, Mat2 right, Mat2 dest) {
		if (dest == null) dest = new Mat2();

		float m00 = left.m00 * right.m00 + left.m10 * right.m01;
		float m01 = left.m01 * right.m00 + left.m11 * right.m01;
		float m10 = left.m00 * right.m10 + left.m10 * right.m11;
		float m11 = left.m01 * right.m10 + left.m11 * right.m11;

		dest.m00 = m00;
		dest.m01 = m01;
		dest.m10 = m10;
		dest.m11 = m11;

		return dest;
	}

	/**
	 * Transform a Vec2 by a Mat2 and return the result in a destination
	 * Vec2.
	 *
	 * @param left  The left matrix
	 * @param right The right Vec2
	 * @param dest  The destination Vec2, or null if a new one is to be created
	 * @return the destination Vec2
	 */
	public static Vec2 transform(Mat2 left, Vec2 right, Vec2 dest) {
		if (dest == null) dest = new Vec2();

		float x = left.m00 * right.x + left.m10 * right.y;
		float y = left.m01 * right.x + left.m11 * right.y;

		dest.x = x;
		dest.y = y;

		return dest;
	}

	/**
	 * Transpose this matrix
	 *
	 * @return this
	 */
	public Mat2 transpose() {
		return transpose(this);
	}

	/**
	 * Transpose this Mat2 and place the result in another matrix.
	 *
	 * @param dest The destination Mat2 or null if a new Mat2 is to be created
	 * @return the transposed matrix
	 */
	public Mat2 transpose(Mat2 dest) {
		return transpose(this, dest);
	}

	/**
	 * Transpose the source Mat2 and place the result in the destination matrix.
	 *
	 * @param src  The source Mat2 or null if a new Mat2 is to be created
	 * @param dest The destination Mat2 or null if a new Mat2 is to be created
	 * @return the transposed matrix
	 */
	public static Mat2 transpose(Mat2 src, Mat2 dest) {
		if (dest == null) dest = new Mat2();

		float m01 = src.m10;
		float m10 = src.m01;

		dest.m01 = m01;
		dest.m10 = m10;

		return dest;
	}

	/**
	 * Invert this matrix
	 *
	 * @return this if successful, null otherwise
	 */
	public Mat2 invert() {
		return invert(this, this);
	}

	/**
	 * Invert the source Mat2 and place the result in the destination matrix.
	 *
	 * @param src  The source Mat2 to be inverted
	 * @param dest The destination Mat2 or null if a new Mat2 is to be created
	 * @return The inverted matrix, or null if source can't be reverted.
	 */
	public static Mat2 invert(Mat2 src, Mat2 dest) {
		/*
		 *inv(A) = 1/det(A) * adj(A);
		 */

		float determinant = src.determinant();
		if (determinant != 0) {
			if (dest == null) dest = new Mat2();
			float determinant_inv = 1f / determinant;
			float t00 = src.m11 * determinant_inv;
			float t01 = -src.m01 * determinant_inv;
			float t11 = src.m00 * determinant_inv;
			float t10 = -src.m10 * determinant_inv;

			dest.m00 = t00;
			dest.m01 = t01;
			dest.m10 = t10;
			dest.m11 = t11;
			return dest;
		} else return null;
	}

	/**
	 * Returns a string representation of this matrix
	 */
	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append(m00).append(' ').append(m10).append(' ').append('\n');
		buf.append(m01).append(' ').append(m11).append(' ').append('\n');
		return buf.toString();
	}

	/**
	 * Negate this matrix
	 *
	 * @return this
	 */
	public Mat2 negate() {
		return negate(this);
	}

	/**
	 * Negate this Mat2 and stash the result in another matrix.
	 *
	 * @param dest The destination matrix, or null if a new Mat2 is to be created
	 * @return the negated matrix
	 */
	public Mat2 negate(Mat2 dest) {
		return negate(this, dest);
	}

	/**
	 * Negate the source Mat2 and stash the result in the destination matrix.
	 *
	 * @param src  The source Mat2 to be negated
	 * @param dest The destination matrix, or null if a new Mat2 is to be created
	 * @return the negated matrix
	 */
	public static Mat2 negate(Mat2 src, Mat2 dest) {
		if (dest == null) dest = new Mat2();

		dest.m00 = -src.m00;
		dest.m01 = -src.m01;
		dest.m10 = -src.m10;
		dest.m11 = -src.m11;

		return dest;
	}

	/**
	 * Set this Mat2 to be the identity matrix.
	 *
	 * @return this
	 */
	public Mat2 setIdentity() {
		return setIdentity(this);
	}

	/**
	 * Set the source Mat2 to be the identity matrix.
	 *
	 * @param src The Mat2 to set to the identity.
	 * @return The source matrix
	 */
	public static Mat2 setIdentity(Mat2 src) {
		src.m00 = 1.0f;
		src.m01 = 0.0f;
		src.m10 = 0.0f;
		src.m11 = 1.0f;
		return src;
	}

	/**
	 * Set this Mat2 to 0.
	 *
	 * @return this
	 */
	public Mat2 setZero() {
		return setZero(this);
	}

	public static Mat2 setZero(Mat2 src) {
		src.m00 = 0.0f;
		src.m01 = 0.0f;
		src.m10 = 0.0f;
		src.m11 = 0.0f;
		return src;
	}

	/* (non-Javadoc)
	 * @see org.lwjgl.Vec2.Matrix#determinant()
	 */
	public float determinant() {
		return m00 * m11 - m01 * m10;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Mat2)) return false;

		Mat2 mat2 = (Mat2) o;

		if (Float.compare(mat2.m00, m00) != 0) return false;
		if (Float.compare(mat2.m01, m01) != 0) return false;
		if (Float.compare(mat2.m10, m10) != 0) return false;
		if (Float.compare(mat2.m11, m11) != 0) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = (m00 != +0.0f ? Float.floatToIntBits(m00) : 0);
		result = 31 * result + (m01 != +0.0f ? Float.floatToIntBits(m01) : 0);
		result = 31 * result + (m10 != +0.0f ? Float.floatToIntBits(m10) : 0);
		result = 31 * result + (m11 != +0.0f ? Float.floatToIntBits(m11) : 0);
		return result;
	}
}
