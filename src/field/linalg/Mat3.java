package field.linalg;

import java.io.Serializable;
import java.nio.DoubleBuffer;

/**
 * Created by marc on 3/12/14.
 */
public class Mat3 implements Serializable {

	private static final long serialVersionUID = 1L;

	public double m00, m01, m02, m10, m11, m12, m20, m21, m22;

	/**
	 * Constructor for Mat3. Mat3 is initialised to the identity.
	 */
	public Mat3() {
		super();
		setIdentity();
	}

	/**
	 * Constructs and initializes a Matrix4f from the quaternion,
	 * translation, and scale values; the scale is applied only to the
	 * rotational components of the matrix (upper 3x3) and not to the
	 * translational components.
	 *
	 * @param q1 the quaternion value representing the rotational
	 *           component
	 * @param s  the scale value applied to the rotational components
	 */
	public Mat3(Quat q1, double s) {
		setIdentity();
		m00 = (double) (s * (1.0 - 2.0 * q1.y * q1.y - 2.0 * q1.z * q1.z));
		m10 = (double) (s * (2.0 * (q1.x * q1.y + q1.w * q1.z)));
		m20 = (double) (s * (2.0 * (q1.x * q1.z - q1.w * q1.y)));

		m01 = (double) (s * (2.0 * (q1.x * q1.y - q1.w * q1.z)));
		m11 = (double) (s * (1.0 - 2.0 * q1.x * q1.x - 2.0 * q1.z * q1.z));
		m21 = (double) (s * (2.0 * (q1.y * q1.z + q1.w * q1.x)));

		m02 = (double) (s * (2.0 * (q1.x * q1.z + q1.w * q1.y)));
		m12 = (double) (s * (2.0 * (q1.y * q1.z - q1.w * q1.x)));
		m22 = (double) (s * (1.0 - 2.0 * q1.x * q1.x - 2.0 * q1.y * q1.y));
	}

	/**
	 * Load from another matrix
	 *
	 * @param src The source matrix
	 * @return this
	 */
	public Mat3 load(Mat3 src) {
		return load(src, this);
	}

	/**
	 * Copy source Mat3 to destination matrix
	 *
	 * @param src  The source matrix
	 * @param dest The destination matrix, or null of a new Mat3 is to be created
	 * @return The copied matrix
	 */
	public static Mat3 load(Mat3 src, Mat3 dest) {
		if (dest == null) dest = new Mat3();

		dest.m00 = src.m00;
		dest.m10 = src.m10;
		dest.m20 = src.m20;
		dest.m01 = src.m01;
		dest.m11 = src.m11;
		dest.m21 = src.m21;
		dest.m02 = src.m02;
		dest.m12 = src.m12;
		dest.m22 = src.m22;

		return dest;
	}

	/**
	 * Load from a double buffer. The buffer stores the Mat3 in column major
	 * (OpenGL) order.
	 *
	 * @param buf A double buffer to read from
	 * @return this
	 */
	public Mat3 load(DoubleBuffer buf) {

		m00 = buf.get();
		m01 = buf.get();
		m02 = buf.get();
		m10 = buf.get();
		m11 = buf.get();
		m12 = buf.get();
		m20 = buf.get();
		m21 = buf.get();
		m22 = buf.get();

		return this;
	}

	/**
	 * Load from a double buffer. The buffer stores the Mat3 in row major
	 * (maths) order.
	 *
	 * @param buf A double buffer to read from
	 * @return this
	 */
	public Mat3 loadTranspose(DoubleBuffer buf) {

		m00 = buf.get();
		m10 = buf.get();
		m20 = buf.get();
		m01 = buf.get();
		m11 = buf.get();
		m21 = buf.get();
		m02 = buf.get();
		m12 = buf.get();
		m22 = buf.get();

		return this;
	}

	/**
	 * Store this Mat3 in a double buffer. The Mat3 is stored in column
	 * major (openGL) order.
	 *
	 * @param buf The buffer to store this Mat3 in
	 */
	public Mat3 store(DoubleBuffer buf) {
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
	 * Store this Mat3 in a double buffer. The Mat3 is stored in row
	 * major (maths) order.
	 *
	 * @param buf The buffer to store this Mat3 in
	 */
	public Mat3 storeTranspose(DoubleBuffer buf) {
		buf.put(m00);
		buf.put(m10);
		buf.put(m20);
		buf.put(m01);
		buf.put(m11);
		buf.put(m21);
		buf.put(m02);
		buf.put(m12);
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
	public static Mat3 add(Mat3 left, Mat3 right, Mat3 dest) {
		if (dest == null) dest = new Mat3();

		dest.m00 = left.m00 + right.m00;
		dest.m01 = left.m01 + right.m01;
		dest.m02 = left.m02 + right.m02;
		dest.m10 = left.m10 + right.m10;
		dest.m11 = left.m11 + right.m11;
		dest.m12 = left.m12 + right.m12;
		dest.m20 = left.m20 + right.m20;
		dest.m21 = left.m21 + right.m21;
		dest.m22 = left.m22 + right.m22;

		return dest;
	}

	/**
	 * Subtract the right Mat3 from the left and place the result in a third matrix.
	 *
	 * @param left  The left source matrix
	 * @param right The right source matrix
	 * @param dest  The destination matrix, or null if a new one is to be created
	 * @return the destination matrix
	 */
	public static Mat3 sub(Mat3 left, Mat3 right, Mat3 dest) {
		if (dest == null) dest = new Mat3();

		dest.m00 = left.m00 - right.m00;
		dest.m01 = left.m01 - right.m01;
		dest.m02 = left.m02 - right.m02;
		dest.m10 = left.m10 - right.m10;
		dest.m11 = left.m11 - right.m11;
		dest.m12 = left.m12 - right.m12;
		dest.m20 = left.m20 - right.m20;
		dest.m21 = left.m21 - right.m21;
		dest.m22 = left.m22 - right.m22;

		return dest;
	}

	/**
	 * Rotates this Mat3 with the specified quaternion
	 *
	 * @param q the quaternion to rotate by.
	 * @return this
	 */
	public Mat3 rotate(Quat q) {
		Mat3 m = new Mat3(q, 1);
		mul(m, this, this);
		return this;
	}


	/**
	 * Multiply the right Mat3 by the left and place the result in a third matrix.
	 *
	 * @param left  The left source matrix
	 * @param right The right source matrix
	 * @param dest  The destination matrix, or null if a new one is to be created
	 * @return the destination matrix
	 */
	public static Mat3 mul(Mat3 left, Mat3 right, Mat3 dest) {
		if (dest == null) dest = new Mat3();

		double m00 = left.m00 * right.m00 + left.m10 * right.m01 + left.m20 * right.m02;
		double m01 = left.m01 * right.m00 + left.m11 * right.m01 + left.m21 * right.m02;
		double m02 = left.m02 * right.m00 + left.m12 * right.m01 + left.m22 * right.m02;
		double m10 = left.m00 * right.m10 + left.m10 * right.m11 + left.m20 * right.m12;
		double m11 = left.m01 * right.m10 + left.m11 * right.m11 + left.m21 * right.m12;
		double m12 = left.m02 * right.m10 + left.m12 * right.m11 + left.m22 * right.m12;
		double m20 = left.m00 * right.m20 + left.m10 * right.m21 + left.m20 * right.m22;
		double m21 = left.m01 * right.m20 + left.m11 * right.m21 + left.m21 * right.m22;
		double m22 = left.m02 * right.m20 + left.m12 * right.m21 + left.m22 * right.m22;

		dest.m00 = m00;
		dest.m01 = m01;
		dest.m02 = m02;
		dest.m10 = m10;
		dest.m11 = m11;
		dest.m12 = m12;
		dest.m20 = m20;
		dest.m21 = m21;
		dest.m22 = m22;

		return dest;
	}

	/**
	 * Transform a Vector by a Mat3 and return the result in a destination
	 * vector.
	 *
	 * @param left  The left matrix
	 * @param right The right vector
	 * @param dest  The destination vector, or null if a new one is to be created
	 * @return the destination vector
	 */
	public static Vec3 transform(Mat3 left, Vec3 right, Vec3 dest) {
		if (dest == null) dest = new Vec3();

		double x = left.m00 * right.x + left.m10 * right.y + left.m20 * right.z;
		double y = left.m01 * right.x + left.m11 * right.y + left.m21 * right.z;
		double z = left.m02 * right.x + left.m12 * right.y + left.m22 * right.z;

		dest.x = x;
		dest.y = y;
		dest.z = z;

		return dest;
	}

	/**
	 * Transpose this matrix
	 *
	 * @return this
	 */
	public Mat3 transpose() {
		return transpose(this, this);
	}

	/**
	 * Transpose this Mat3 and place the result in another matrix
	 *
	 * @param dest The destination Mat3 or null if a new Mat3 is to be created
	 * @return the transposed matrix
	 */
	public Mat3 transpose(Mat3 dest) {
		return transpose(this, dest);
	}

	/**
	 * Transpose the source Mat3 and place the result into the destination matrix
	 *
	 * @param src  The source Mat3 to be transposed
	 * @param dest The destination Mat3 or null if a new Mat3 is to be created
	 * @return the transposed matrix
	 */
	public static Mat3 transpose(Mat3 src, Mat3 dest) {
		if (dest == null) dest = new Mat3();
		double m00 = src.m00;
		double m01 = src.m10;
		double m02 = src.m20;
		double m10 = src.m01;
		double m11 = src.m11;
		double m12 = src.m21;
		double m20 = src.m02;
		double m21 = src.m12;
		double m22 = src.m22;

		dest.m00 = m00;
		dest.m01 = m01;
		dest.m02 = m02;
		dest.m10 = m10;
		dest.m11 = m11;
		dest.m12 = m12;
		dest.m20 = m20;
		dest.m21 = m21;
		dest.m22 = m22;
		return dest;
	}

	/**
	 * @return the determinant of the matrix
	 */
	public double determinant() {
		double f = m00 * (m11 * m22 - m12 * m21) + m01 * (m12 * m20 - m10 * m22) + m02 * (m10 * m21 - m11 * m20);
		return f;
	}

	/**
	 * Returns a string representation of this matrix
	 */
	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append(m00).append(' ').append(m10).append(' ').append(m20).append(' ').append('\n');
		buf.append(m01).append(' ').append(m11).append(' ').append(m21).append(' ').append('\n');
		buf.append(m02).append(' ').append(m12).append(' ').append(m22).append(' ').append('\n');
		return buf.toString();
	}

	/**
	 * Invert this matrix
	 *
	 * @return this if successful, null otherwise
	 */
	public Mat3 invert() {
		return invert(this, this);
	}

	/**
	 * Invert the source Mat3 and put the result into the destination matrix
	 *
	 * @param src  The source Mat3 to be inverted
	 * @param dest The destination matrix, or null if a new one is to be created
	 * @return The inverted Mat3 if successful, null otherwise
	 */
	public static Mat3 invert(Mat3 src, Mat3 dest) {
		double determinant = src.determinant();

		if (determinant != 0) {
			if (dest == null) dest = new Mat3();
			 /* do it the ordinary way
			  *
			  * inv(A) = 1/det(A) * adj(T), where adj(T) = transpose(Conjugate Matrix)
			  *
			  * m00 m01 m02
			  * m10 m11 m12
			  * m20 m21 m22
			  */
			double determinant_inv = 1f / determinant;

			// get the conjugate matrix
			double t00 = src.m11 * src.m22 - src.m12 * src.m21;
			double t01 = -src.m10 * src.m22 + src.m12 * src.m20;
			double t02 = src.m10 * src.m21 - src.m11 * src.m20;
			double t10 = -src.m01 * src.m22 + src.m02 * src.m21;
			double t11 = src.m00 * src.m22 - src.m02 * src.m20;
			double t12 = -src.m00 * src.m21 + src.m01 * src.m20;
			double t20 = src.m01 * src.m12 - src.m02 * src.m11;
			double t21 = -src.m00 * src.m12 + src.m02 * src.m10;
			double t22 = src.m00 * src.m11 - src.m01 * src.m10;

			dest.m00 = t00 * determinant_inv;
			dest.m11 = t11 * determinant_inv;
			dest.m22 = t22 * determinant_inv;
			dest.m01 = t10 * determinant_inv;
			dest.m10 = t01 * determinant_inv;
			dest.m20 = t02 * determinant_inv;
			dest.m02 = t20 * determinant_inv;
			dest.m12 = t21 * determinant_inv;
			dest.m21 = t12 * determinant_inv;
			return dest;
		} else return null;
	}


	/**
	 * Negate this matrix
	 *
	 * @return this
	 */
	public Mat3 negate() {
		return negate(this);
	}

	/**
	 * Negate this Mat3 and place the result in a destination matrix.
	 *
	 * @param dest The destination matrix, or null if a new Mat3 is to be created
	 * @return the negated matrix
	 */
	public Mat3 negate(Mat3 dest) {
		return negate(this, dest);
	}

	/**
	 * Negate the source Mat3 and place the result in the destination matrix.
	 *
	 * @param src  The source matrix
	 * @param dest The destination matrix, or null if a new Mat3 is to be created
	 * @return the negated matrix
	 */
	public static Mat3 negate(Mat3 src, Mat3 dest) {
		if (dest == null) dest = new Mat3();

		dest.m00 = -src.m00;
		dest.m01 = -src.m02;
		dest.m02 = -src.m01;
		dest.m10 = -src.m10;
		dest.m11 = -src.m12;
		dest.m12 = -src.m11;
		dest.m20 = -src.m20;
		dest.m21 = -src.m22;
		dest.m22 = -src.m21;
		return dest;
	}

	/**
	 * Set this Mat3 to be the identity matrix.
	 *
	 * @return this
	 */
	public Mat3 setIdentity() {
		return setIdentity(this);
	}

	/**
	 * Set the Mat3 to be the identity matrix.
	 *
	 * @param m The Mat3 to be set to the identity
	 * @return m
	 */
	public static Mat3 setIdentity(Mat3 m) {
		m.m00 = 1.0f;
		m.m01 = 0.0f;
		m.m02 = 0.0f;
		m.m10 = 0.0f;
		m.m11 = 1.0f;
		m.m12 = 0.0f;
		m.m20 = 0.0f;
		m.m21 = 0.0f;
		m.m22 = 1.0f;
		return m;
	}

	/**
	 * Set this Mat3 to 0.
	 *
	 * @return this
	 */
	public Mat3 setZero() {
		return setZero(this);
	}

	/**
	 * Set the Mat3 matrix to 0.
	 *
	 * @param m The Mat3 to be set to 0
	 * @return m
	 */
	public static Mat3 setZero(Mat3 m) {
		m.m00 = 0.0f;
		m.m01 = 0.0f;
		m.m02 = 0.0f;
		m.m10 = 0.0f;
		m.m11 = 0.0f;
		m.m12 = 0.0f;
		m.m20 = 0.0f;
		m.m21 = 0.0f;
		m.m22 = 0.0f;
		return m;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Mat3)) return false;

		Mat3 mat3 = (Mat3) o;

		if (Double.compare(mat3.m00, m00) != 0) return false;
		if (Double.compare(mat3.m01, m01) != 0) return false;
		if (Double.compare(mat3.m02, m02) != 0) return false;
		if (Double.compare(mat3.m10, m10) != 0) return false;
		if (Double.compare(mat3.m11, m11) != 0) return false;
		if (Double.compare(mat3.m12, m12) != 0) return false;
		if (Double.compare(mat3.m20, m20) != 0) return false;
		if (Double.compare(mat3.m21, m21) != 0) return false;
		if (Double.compare(mat3.m22, m22) != 0) return false;

		return true;
	}

	@Override
	public int hashCode() {
		long result = (m00 != +0.0f ? Double.doubleToLongBits(m00) : 0);
		result = 31 * result + (m01 != +0.0f ? Double.doubleToLongBits(m01) : 0);
		result = 31 * result + (m02 != +0.0f ? Double.doubleToLongBits(m02) : 0);
		result = 31 * result + (m10 != +0.0f ? Double.doubleToLongBits(m10) : 0);
		result = 31 * result + (m11 != +0.0f ? Double.doubleToLongBits(m11) : 0);
		result = 31 * result + (m12 != +0.0f ? Double.doubleToLongBits(m12) : 0);
		result = 31 * result + (m20 != +0.0f ? Double.doubleToLongBits(m20) : 0);
		result = 31 * result + (m21 != +0.0f ? Double.doubleToLongBits(m21) : 0);
		result = 31 * result + (m22 != +0.0f ? Double.doubleToLongBits(m22) : 0);
		return (int)(result ^ (result >>> 32));
	}
}