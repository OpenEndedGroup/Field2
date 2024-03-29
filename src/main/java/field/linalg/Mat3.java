/*
 * (C) Copyright 2015 Richard Greenlees

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in
 all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 THE SOFTWARE.

 */
package field.linalg;

import field.utility.Mutable;
import field.utility.Serializable_safe;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.function.Supplier;

/**
 * Contains the definition of a 3x3 Matrix of doubles, and associated functions to transform it. The matrix is column-major to match OpenGL's interpretation, and it looks like this:
 * <p>
 * m00  m10  m20<br> m01  m11  m21<br> m02  m12  m22<br>
 *
 * @author Richard Greenlees
 * @author Kai Burjack
 */
public class Mat3 implements Externalizable, Supplier<Mat3>, Mutable, Serializable_safe {

	private static final long serialVersionUID = 1L;

	public double m00, m10, m20;
	public double m01, m11, m21;
	public double m02, m12, m22;

	/**
	 * Create a new {@link Mat3} and initialize it to {@link #identity() identity}.
	 */
	public Mat3() {
		super();
		identity();
	}

	/**
	 * Create a new {@link Mat3} and initialize it with the values from the given matrix.
	 *
	 * @param mat the matrix to initialize this matrix with
	 */
	public Mat3(Mat3 mat) {
		m00 = mat.m00;
		m01 = mat.m01;
		m02 = mat.m02;
		m10 = mat.m10;
		m11 = mat.m11;
		m12 = mat.m12;
		m20 = mat.m20;
		m21 = mat.m21;
		m22 = mat.m22;
	}

	/**
	 * Create a new {@link Mat3} and make it a copy of the upper left 3x3 of the given {@link Mat4}.
	 *
	 * @param mat the {@link Mat4} to copy the values from
	 */
	public Mat3(Mat4 mat) {
		m00 = mat.m00;
		m01 = mat.m01;
		m02 = mat.m02;
		m10 = mat.m10;
		m11 = mat.m11;
		m12 = mat.m12;
		m20 = mat.m20;
		m21 = mat.m21;
		m22 = mat.m22;
	}

	/**
	 * Create a new {@link Mat3} and make it a copy of the given Mat2; the bottom right entry is given the value "m22"
	 *
	 * @param mat the {@link Mat2} to copy the values from
	 * @param m22 the bottom right value of the resulting Mat3
	 */
	public Mat3(Mat2 mat, double m22) {
		m00 = mat.m00;
		m01 = mat.m01;
		m10 = mat.m10;
		m11 = mat.m11;
		this.m22 = m22;
	}

	/**
	 * Create a new {@link Mat3} and initialize its elements with the given values.
	 *
	 * @param m00 the value of m00
	 * @param m01 the value of m01
	 * @param m02 the value of m02
	 * @param m10 the value of m10
	 * @param m11 the value of m11
	 * @param m12 the value of m12
	 * @param m20 the value of m20
	 * @param m21 the value of m21
	 * @param m22 the value of m22
	 */
	public Mat3(double m00, double m01, double m02, double m10, double m11, double m12, double m20, double m21, double m22) {
		this.m00 = m00;
		this.m01 = m01;
		this.m02 = m02;
		this.m10 = m10;
		this.m11 = m11;
		this.m12 = m12;
		this.m20 = m20;
		this.m21 = m21;
		this.m22 = m22;
	}

	/**
	 * Set the values in this matrix to the ones in m.
	 *
	 * @param m the matrix whose values will be copied
	 * @return this
	 */
	public Mat3 set(Mat3 m) {
		m00 = m.m00;
		m01 = m.m01;
		m02 = m.m02;
		m10 = m.m10;
		m11 = m.m11;
		m12 = m.m12;
		m20 = m.m20;
		m21 = m.m21;
		m22 = m.m22;
		return this;
	}

	/**
	 * Set the elements of this matrix to the upper left 3x3 of the given {@link Mat4}.
	 *
	 * @param mat the {@link Mat4} to copy the values from
	 * @return this
	 */
	public Mat3 set(Mat4 mat) {
		m00 = mat.m00;
		m01 = mat.m01;
		m02 = mat.m02;
		m10 = mat.m10;
		m11 = mat.m11;
		m12 = mat.m12;
		m20 = mat.m20;
		m21 = mat.m21;
		m22 = mat.m22;
		return this;
	}

	/**
	 * Set this matrix to be equivalent to the rotation specified by the given {@link AxisAngle}.
	 *
	 * @param axisAngle the {@link AxisAngle}
	 * @return this
	 */
	public Mat3 set(AxisAngle axisAngle) {
		double x = axisAngle.x;
		double y = axisAngle.y;
		double z = axisAngle.z;
		double angle = axisAngle.angle;
		double n = Math.sqrt(x * x + y * y + z * z);
		x /= n;
		y /= n;
		z /= n;
		double c = Math.cos(angle);
		double s = Math.sin(angle);
		double omc = 1.0 - c;
		m00 = c + x * x * omc;
		m11 = c + y * y * omc;
		m22 = c + z * z * omc;
		double tmp1 = x * y * omc;
		double tmp2 = z * s;
		m10 = tmp1 - tmp2;
		m01 = tmp1 + tmp2;
		tmp1 = x * z * omc;
		tmp2 = y * s;
		m20 = tmp1 + tmp2;
		m02 = tmp1 - tmp2;
		tmp1 = y * z * omc;
		tmp2 = x * s;
		m21 = tmp1 - tmp2;
		m12 = tmp1 + tmp2;
		return this;
	}

	/**
	 * Set this matrix to a rotation equivalent to the given quaternion.
	 *
	 * @param q the quaternion
	 * @return this
	 * @see Quat#get(Mat3)
	 */
	public Mat3 set(Quat q) {
		q.get(this);
		return this;
	}

	public Mat3(double[] m) {
		set(m);
	}


	/**
	 * Multiply this matrix by the supplied matrix. This matrix will be the left one.
	 * <p>
	 * If <code>M</code> is <code>this</code> matrix and <code>R</code> the <code>right</code> matrix, then the new matrix will be <code>M * R</code>. So when transforming a vector <code>v</code>
	 * with the new matrix by using <code>M * R * v</code>, the transformation of the right matrix will be applied first!
	 *
	 * @param right the right operand
	 * @return this
	 */
	public Mat3 mul(Mat3 right) {
		return mul(right, this);
	}

	/**
	 * Multiply this matrix by the supplied matrix and store the result in <code>dest</code>. This matrix will be the left one.
	 * <p>
	 * If <code>M</code> is <code>this</code> matrix and <code>R</code> the <code>right</code> matrix, then the new matrix will be <code>M * R</code>. So when transforming a vector <code>v</code>
	 * with the new matrix by using <code>M * R * v</code>, the transformation of the right matrix will be applied first!
	 *
	 * @param right the right operand
	 * @param dest  will hold the result
	 * @return this
	 */
	public Mat3 mul(Mat3 right, Mat3 dest) {
		if (this != dest && right != dest) {
			dest.m00 = m00 * right.m00 + m10 * right.m01 + m20 * right.m02;
			dest.m01 = m01 * right.m00 + m11 * right.m01 + m21 * right.m02;
			dest.m02 = m02 * right.m00 + m12 * right.m01 + m22 * right.m02;
			dest.m10 = m00 * right.m10 + m10 * right.m11 + m20 * right.m12;
			dest.m11 = m01 * right.m10 + m11 * right.m11 + m21 * right.m12;
			dest.m12 = m02 * right.m10 + m12 * right.m11 + m22 * right.m12;
			dest.m20 = m00 * right.m20 + m10 * right.m21 + m20 * right.m22;
			dest.m21 = m01 * right.m20 + m11 * right.m21 + m21 * right.m22;
			dest.m22 = m02 * right.m20 + m12 * right.m21 + m22 * right.m22;
		} else {
			dest.set(m00 * right.m00 + m10 * right.m01 + m20 * right.m02, m01 * right.m00 + m11 * right.m01 + m21 * right.m02, m02 * right.m00 + m12 * right.m01 + m22 * right.m02,
				 m00 * right.m10 + m10 * right.m11 + m20 * right.m12, m01 * right.m10 + m11 * right.m11 + m21 * right.m12, m02 * right.m10 + m12 * right.m11 + m22 * right.m12,
				 m00 * right.m20 + m10 * right.m21 + m20 * right.m22, m01 * right.m20 + m11 * right.m21 + m21 * right.m22, m02 * right.m20 + m12 * right.m21 + m22 * right.m22);
		}
		return this;
	}

	/**
	 * Set the values within this matrix to the supplied double values. The result looks like this:
	 * <p>
	 * m00, m10, m20<br> m01, m11, m21<br> m02, m12, m22<br>
	 *
	 * @param m00 the new value of m00
	 * @param m01 the new value of m01
	 * @param m02 the new value of m02
	 * @param m10 the new value of m10
	 * @param m11 the new value of m11
	 * @param m12 the new value of m12
	 * @param m20 the new value of m20
	 * @param m21 the new value of m21
	 * @param m22 the new value of m22
	 * @return this
	 */
	public Mat3 set(double m00, double m01, double m02, double m10, double m11, double m12, double m20, double m21, double m22) {
		this.m00 = m00;
		this.m01 = m01;
		this.m02 = m02;
		this.m10 = m10;
		this.m11 = m11;
		this.m12 = m12;
		this.m20 = m20;
		this.m21 = m21;
		this.m22 = m22;
		return this;
	}

	/**
	 * Set the values in this matrix based on the supplied double array. The result looks like this:
	 * <p>
	 * 0, 3, 6<br> 1, 4, 7<br> 2, 5, 8<br>
	 * <p>
	 * Only uses the first 9 values, all others are ignored.
	 *
	 * @param m the array to read the matrix values from
	 * @return this
	 */
	public Mat3 set(double m[]) {
		m00 = m[0];
		m01 = m[1];
		m02 = m[2];
		m10 = m[3];
		m11 = m[4];
		m12 = m[5];
		m20 = m[6];
		m21 = m[7];
		m22 = m[8];
		return this;
	}

	/**
	 * Set the values in this matrix based on the supplied double array. The result looks like this:
	 * <p>
	 * 0, 3, 6<br> 1, 4, 7<br> 2, 5, 8<br>
	 * <p>
	 * Only uses the first 9 values, all others are ignored
	 *
	 * @param m the array to read the matrix values from
	 * @return this
	 */
	public Mat3 set(float m[]) {
		m00 = m[0];
		m01 = m[1];
		m02 = m[2];
		m10 = m[3];
		m11 = m[4];
		m12 = m[5];
		m20 = m[6];
		m21 = m[7];
		m22 = m[8];
		return this;
	}

	/**
	 * Return the determinant of this matrix.
	 *
	 * @return the determinant
	 */
	public double determinant() {
		return m00 * (m11 * m22 - m12 * m21) - m01 * (m10 * m22 - m12 * m20) + m02 * (m01 * m21 - m11 * m20);
	}

	/**
	 * Invert this matrix.
	 *
	 * @return this
	 */
	public Mat3 invert() {
		return invert(this);
	}

	/**
	 * Invert <code>this</code> matrix and store the result in <code>dest</code>.
	 *
	 * @param dest will hold the result
	 * @return this
	 */
	public Mat3 invert(Mat3 dest) {
		double s = determinant();
		if (s == 0.0) {
			dest.set(this);
			return this;
		}
		s = 1.0 / s;
		if (this != dest) {
			dest.m00 = ((m11 * m22) - (m21 * m12)) * s;
			dest.m01 = -((m01 * m22) - (m21 * m02)) * s;
			dest.m02 = ((m01 * m12) - (m11 * m02)) * s;
			dest.m10 = -((m10 * m22) - (m20 * m12)) * s;
			dest.m11 = ((m00 * m22) - (m20 * m02)) * s;
			dest.m12 = -((m00 * m12) - (m10 * m02)) * s;
			dest.m20 = ((m10 * m21) - (m20 * m11)) * s;
			dest.m21 = -((m00 * m21) - (m20 * m01)) * s;
			dest.m22 = ((m00 * m11) - (m10 * m01)) * s;
		} else {
			dest.set(((m11 * m22) - (m21 * m12)) * s, -((m01 * m22) - (m21 * m02)) * s, ((m01 * m12) - (m11 * m02)) * s, -((m10 * m22) - (m20 * m12)) * s, ((m00 * m22) - (m20 * m02)) * s,
				 -((m00 * m12) - (m10 * m02)) * s, ((m10 * m21) - (m20 * m11)) * s, -((m00 * m21) - (m20 * m01)) * s, ((m00 * m11) - (m10 * m01)) * s);
		}
		return this;
	}

	/**
	 * Transpose this matrix.
	 *
	 * @return this
	 */
	public Mat3 transpose() {
		return transpose(this);
	}

	/**
	 * Transpose <code>this</code> matrix and store the result in <code>dest</code>.
	 *
	 * @param dest will hold the result
	 * @return this
	 */
	public Mat3 transpose(Mat3 dest) {
		if (this != dest) {
			dest.m00 = m00;
			dest.m01 = m10;
			dest.m02 = m20;
			dest.m10 = m01;
			dest.m11 = m11;
			dest.m12 = m21;
			dest.m20 = m02;
			dest.m21 = m12;
			dest.m22 = m22;
		} else {
			dest.set(m00, m10, m20, m01, m11, m21, m02, m12, m22);
		}
		return this;
	}

	/**
	 * Return a string representation of this matrix.
	 * <p>
	 * This method creates a new {@link DecimalFormat} on every invocation with the format string "<tt>  0.000E0; -</tt>".
	 *
	 * @return the string representation
	 */
	public String toString() {
		DecimalFormat formatter = new DecimalFormat("  0.000E0; -"); //$NON-NLS-1$
		return toString(formatter).replaceAll("E(\\d+)", "E+$1"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Return a string representation of this matrix by formatting the matrix elements with the given {@link NumberFormat}.
	 *
	 * @param formatter the {@link NumberFormat} used to format the matrix values with
	 * @return the string representation
	 */
	public String toString(NumberFormat formatter) {
		return formatter.format(m00) + formatter.format(m10) + formatter.format(m20) + "\n" //$NON-NLS-1$
			    + formatter.format(m01) + formatter.format(m11) + formatter.format(m21) + "\n" //$NON-NLS-1$
			    + formatter.format(m02) + formatter.format(m12) + formatter.format(m22) + "\n"; //$NON-NLS-1$
	}

	/**
	 * Get the current values of <code>this</code> matrix and store them into <code>dest</code>.
	 * <p>
	 * This is the reverse method of {@link #set(Mat3)} and allows to obtain intermediate calculation results when chaining multiple transformations.
	 *
	 * @param dest the destination matrix
	 * @return the passed in destination
	 * @see #set(Mat3)
	 */
	public Mat3 get(Mat3 dest) {
		return dest.set(this);
	}

	/**
	 * Get the current values of <code>this</code> matrix and store the represented rotation into the given {@link AxisAngle}.
	 *
	 * @param dest the destination {@link AxisAngle}
	 * @return the passed in destination
	 * @see AxisAngle#set(Mat3)
	 */
	public AxisAngle get(AxisAngle dest) {
		return dest.set(this);
	}

	/**
	 * Get the current values of <code>this</code> matrix and store the represented rotation into the given {@link Quat}.
	 *
	 * @param dest the destination {@link Quat}
	 * @return the passed in destination
	 * @see Quat#set(Mat3)
	 */
	public Quat get(Quat dest) {
		return dest.set(this);
	}

	/**
	 * Store this matrix into the supplied {@link DoubleBuffer} at the current buffer {@link DoubleBuffer#position() position} using column-major order.
	 * <p>
	 * This method will not increment the position of the given DoubleBuffer.
	 * <p>
	 * If you want to specify the offset into the DoubleBuffer} at which the matrix is stored, you can use {@link #get(int, DoubleBuffer)}, taking the absolute position as parameter.
	 *
	 * @param buffer will receive the values of this matrix in column-major order at its current position
	 * @return the passed in buffer
	 * @see #get(int, DoubleBuffer)
	 */
	public DoubleBuffer get(DoubleBuffer buffer) {
		return get(buffer.position(), buffer);
	}

	/**
	 * Store this matrix into the supplied {@link DoubleBuffer} starting at the specified absolute buffer position/index using column-major order.
	 * <p>
	 * This method will not increment the position of the given {@link DoubleBuffer}.
	 *
	 * @param index  the absolute position into the {@link DoubleBuffer}
	 * @param buffer will receive the values of this matrix in column-major order
	 * @return the passed in buffer
	 */
	public DoubleBuffer get(int index, DoubleBuffer buffer) {
		buffer.put(index, m00);
		buffer.put(index + 1, m01);
		buffer.put(index + 2, m02);
		buffer.put(index + 3, m10);
		buffer.put(index + 4, m11);
		buffer.put(index + 5, m12);
		buffer.put(index + 6, m20);
		buffer.put(index + 7, m21);
		buffer.put(index + 8, m22);
		return buffer;
	}

	/**
	 * Store this matrix in column-major order into the supplied {@link FloatBuffer} at the current buffer {@link FloatBuffer#position() position}.
	 * <p>
	 * This method will not increment the position of the given FloatBuffer.
	 * <p>
	 * If you want to specify the offset into the FloatBuffer at which the matrix is stored, you can use {@link #get(int, FloatBuffer)}, taking the absolute position as parameter.
	 * <p>
	 * Please note that due to this matrix storing double values those values will potentially lose precision when they are converted to float values before being put into the given FloatBuffer.
	 *
	 * @param buffer will receive the values of this matrix in column-major order at its current position
	 * @return the passed in buffer
	 * @see #get(int, FloatBuffer)
	 */
	public FloatBuffer get(FloatBuffer buffer) {
		return get(buffer.position(), buffer);
	}

	/**
	 * Store this matrix in column-major order into the supplied {@link FloatBuffer} starting at the specified absolute buffer position/index.
	 * <p>
	 * This method will not increment the position of the given FloatBuffer.
	 * <p>
	 * Please note that due to this matrix storing double values those values will potentially lose precision when they are converted to float values before being put into the given FloatBuffer.
	 *
	 * @param index  the absolute position into the FloatBuffer
	 * @param buffer will receive the values of this matrix in column-major order
	 * @return the passed in buffer
	 */
	public FloatBuffer get(int index, FloatBuffer buffer) {
		buffer.put(index, (float) m00);
		buffer.put(index + 1, (float) m01);
		buffer.put(index + 2, (float) m02);
		buffer.put(index + 3, (float) m10);
		buffer.put(index + 4, (float) m11);
		buffer.put(index + 5, (float) m12);
		buffer.put(index + 6, (float) m20);
		buffer.put(index + 7, (float) m21);
		buffer.put(index + 8, (float) m22);
		return buffer;
	}

	/**
	 * Store this matrix in column-major order into the supplied {@link ByteBuffer} at the current buffer {@link ByteBuffer#position() position}.
	 * <p>
	 * This method will not increment the position of the given ByteBuffer.
	 * <p>
	 * If you want to specify the offset into the ByteBuffer at which the matrix is stored, you can use {@link #get(int, ByteBuffer)}, taking the absolute position as parameter.
	 *
	 * @param buffer will receive the values of this matrix in column-major order at its current position
	 * @return the passed in buffer
	 * @see #get(int, ByteBuffer)
	 */
	public ByteBuffer get(ByteBuffer buffer) {
		return get(buffer.position(), buffer);
	}

	/**
	 * Store this matrix in column-major order into the supplied {@link ByteBuffer} starting at the specified absolute buffer position/index.
	 * <p>
	 * This method will not increment the position of the given ByteBuffer.
	 *
	 * @param index  the absolute position into the ByteBuffer
	 * @param buffer will receive the values of this matrix in column-major order
	 * @return the passed in buffer
	 */
	public ByteBuffer get(int index, ByteBuffer buffer) {
		buffer.putDouble(index + 8 * 0, m00);
		buffer.putDouble(index + 8 * 1, m01);
		buffer.putDouble(index + 8 * 2, m02);
		buffer.putDouble(index + 8 * 3, m10);
		buffer.putDouble(index + 8 * 4, m11);
		buffer.putDouble(index + 8 * 5, m12);
		buffer.putDouble(index + 8 * 6, m20);
		buffer.putDouble(index + 8 * 7, m21);
		buffer.putDouble(index + 8 * 8, m22);
		return buffer;
	}


	/**
	 * Store this matrix into the supplied double array in column-major order.
	 *
	 * @param arr    the array to write the matrix values into
	 * @param offset the offset into the array
	 * @return the passed in array
	 */
	public float[] get(float[] arr, int offset) {
		arr[offset + 0] = (float) m00;
		arr[offset + 1] = (float) m01;
		arr[offset + 2] = (float) m02;
		arr[offset + 3] = (float) m10;
		arr[offset + 4] = (float) m11;
		arr[offset + 5] = (float) m12;
		arr[offset + 6] = (float) m20;
		arr[offset + 7] = (float) m21;
		arr[offset + 8] = (float) m22;
		return arr;
	}

	/**
	 * Store this matrix into the supplied double array in column-major order.
	 *
	 * @param arr    the array to write the matrix values into
	 * @param offset the offset into the array
	 * @return the passed in array
	 */
	public double[] get(double[] arr, int offset) {
		arr[offset + 0] = m00;
		arr[offset + 1] = m01;
		arr[offset + 2] = m02;
		arr[offset + 3] = m10;
		arr[offset + 4] = m11;
		arr[offset + 5] = m12;
		arr[offset + 6] = m20;
		arr[offset + 7] = m21;
		arr[offset + 8] = m22;
		return arr;
	}

	/**
	 * Set the values of this matrix by reading 9 double values from the given {@link DoubleBuffer} in column-major order, starting at its current position.
	 * <p>
	 * The DoubleBuffer is expected to contain the values in column-major order.
	 * <p>
	 * The position of the DoubleBuffer will not be changed by this method.
	 *
	 * @param buffer the DoubleBuffer to read the matrix values from in column-major order
	 * @return this
	 */
	public Mat3 set(DoubleBuffer buffer) {
		int pos = buffer.position();
		m00 = buffer.get(pos);
		m01 = buffer.get(pos + 1);
		m02 = buffer.get(pos + 2);
		m10 = buffer.get(pos + 3);
		m11 = buffer.get(pos + 4);
		m12 = buffer.get(pos + 5);
		m20 = buffer.get(pos + 6);
		m21 = buffer.get(pos + 7);
		m22 = buffer.get(pos + 8);
		return this;
	}

	/**
	 * Set all the values within this matrix to 0.
	 *
	 * @return this
	 */
	public Mat3 zero() {
		m00 = 0.0;
		m01 = 0.0;
		m02 = 0.0;
		m10 = 0.0;
		m11 = 0.0;
		m12 = 0.0;
		m20 = 0.0;
		m21 = 0.0;
		m22 = 0.0;
		return this;
	}

	/**
	 * Set this matrix to the identity.
	 *
	 * @return this
	 */
	public Mat3 identity() {
		m00 = 1.0;
		m01 = 0.0;
		m02 = 0.0;
		m10 = 0.0;
		m11 = 1.0;
		m12 = 0.0;
		m20 = 0.0;
		m21 = 0.0;
		m22 = 1.0;
		return this;
	}

	/**
	 * Set this matrix to be a simple scale matrix, which scales all axes uniformly by the given factor.
	 * <p>
	 * The resulting matrix can be multiplied against another transformation matrix to obtain an additional scaling.
	 * <p>
	 * If you want to post-multiply a scaling transformation directly to a matrix, you can use {@link #scale(double) scale()} instead.
	 *
	 * @param factor the scale factor in x, y and z
	 * @return this
	 * @see #scale(double)
	 */
	public Mat3 scaling(double factor) {
		m00 = factor;
		m01 = 0.0;
		m02 = 0.0;
		m10 = 0.0;
		m11 = factor;
		m12 = 0.0;
		m20 = 0.0;
		m21 = 0.0;
		m22 = factor;
		return this;
	}

	/**
	 * Set this matrix to be a simple scale matrix.
	 *
	 * @param x the scale in x
	 * @param y the scale in y
	 * @param z the scale in z
	 * @return this
	 */
	public Mat3 scaling(double x, double y, double z) {
		m00 = x;
		m01 = 0.0;
		m02 = 0.0;
		m10 = 0.0;
		m11 = y;
		m12 = 0.0;
		m20 = 0.0;
		m21 = 0.0;
		m22 = z;
		return this;
	}

	/**
	 * Set this matrix to be a simple scale matrix which scales the base axes by <tt>xyz.x</tt>, <tt>xyz.y</tt> and <tt>xyz.z</tt> respectively.
	 * <p>
	 * The resulting matrix can be multiplied against another transformation matrix to obtain an additional scaling.
	 * <p>
	 * In order to post-multiply a scaling transformation directly to a matrix use {@link #scale(Vec3) scale()} instead.
	 *
	 * @param xyz the scale in x, y and z respectively
	 * @return this
	 * @see #scale(Vec3)
	 */
	public Mat3 scaling(Vec3 xyz) {
		m00 = xyz.x;
		m01 = 0.0;
		m02 = 0.0;
		m10 = 0.0;
		m11 = xyz.y;
		m12 = 0.0;
		m20 = 0.0;
		m21 = 0.0;
		m22 = xyz.z;
		return this;
	}

	/**
	 * Apply scaling to the this matrix by scaling the base axes by the given <tt>xyz.x</tt>, <tt>xyz.y</tt> and <tt>xyz.z</tt> factors, respectively and store the result in <code>dest</code>.
	 * <p>
	 * If <code>M</code> is <code>this</code> matrix and <code>S</code> the scaling matrix, then the new matrix will be <code>M * S</code>. So when transforming a vector <code>v</code> with the
	 * new matrix by using <code>M * S * v</code> , the scaling will be applied first!
	 *
	 * @param xyz  the factors of the x, y and z component, respectively
	 * @param dest will hold the result
	 * @return this
	 */
	public Mat3 scale(Vec3 xyz, Mat3 dest) {
		return scale(xyz.x, xyz.y, xyz.z, dest);
	}

	/**
	 * Apply scaling to this matrix by scaling the base axes by the given <tt>xyz.x</tt>, <tt>xyz.y</tt> and <tt>xyz.z</tt> factors, respectively.
	 * <p>
	 * If <code>M</code> is <code>this</code> matrix and <code>S</code> the scaling matrix, then the new matrix will be <code>M * S</code>. So when transforming a vector <code>v</code> with the
	 * new matrix by using <code>M * S * v</code>, the scaling will be applied first!
	 *
	 * @param xyz the factors of the x, y and z component, respectively
	 * @return this
	 */
	public Mat3 scale(Vec3 xyz) {
		return scale(xyz.x, xyz.y, xyz.z, this);
	}

	/**
	 * Apply scaling to this matrix by scaling the base axes by the given x, y and z factors and store the result in <code>dest</code>.
	 * <p>
	 * If <code>M</code> is <code>this</code> matrix and <code>S</code> the scaling matrix, then the new matrix will be <code>M * S</code>. So when transforming a vector <code>v</code> with the
	 * new matrix by using <code>M * S * v</code> , the scaling will be applied first!
	 *
	 * @param x    the factor of the x component
	 * @param y    the factor of the y component
	 * @param z    the factor of the z component
	 * @param dest will hold the result
	 * @return this
	 */
	public Mat3 scale(double x, double y, double z, Mat3 dest) {
		// scale matrix elements:
		// m00 = x, m11 = y, m22 = z
		// all others = 0
		dest.m00 = m00 * x;
		dest.m01 = m01 * x;
		dest.m02 = m02 * x;
		dest.m10 = m10 * y;
		dest.m11 = m11 * y;
		dest.m12 = m12 * y;
		dest.m20 = m20 * z;
		dest.m21 = m21 * z;
		dest.m22 = m22 * z;
		return this;
	}

	/**
	 * Apply scaling to this matrix by scaling the base axes by the given x, y and z factors.
	 * <p>
	 * If <code>M</code> is <code>this</code> matrix and <code>S</code> the scaling matrix, then the new matrix will be <code>M * S</code>. So when transforming a vector <code>v</code> with the
	 * new matrix by using <code>M * S * v</code> , the scaling will be applied first!
	 *
	 * @param x the factor of the x component
	 * @param y the factor of the y component
	 * @param z the factor of the z component
	 * @return this
	 */
	public Mat3 scale(double x, double y, double z) {
		return scale(x, y, z, this);
	}

	/**
	 * Apply scaling to this matrix by uniformly scaling all base axes by the given <code>xyz</code> factor and store the result in <code>dest</code>.
	 * <p>
	 * If <code>M</code> is <code>this</code> matrix and <code>S</code> the scaling matrix, then the new matrix will be <code>M * S</code>. So when transforming a vector <code>v</code> with the
	 * new matrix by using <code>M * S * v</code> , the scaling will be applied first!
	 *
	 * @param xyz  the factor for all components
	 * @param dest will hold the result
	 * @return this
	 * @see #scale(double, double, double, Mat3)
	 */
	public Mat3 scale(double xyz, Mat3 dest) {
		return scale(xyz, xyz, xyz, dest);
	}

	/**
	 * Apply scaling to this matrix by uniformly scaling all base axes by the given <code>xyz</code> factor.
	 * <p>
	 * If <code>M</code> is <code>this</code> matrix and <code>S</code> the scaling matrix, then the new matrix will be <code>M * S</code>. So when transforming a vector <code>v</code> with the
	 * new matrix by using <code>M * S * v</code> , the scaling will be applied first!
	 *
	 * @param xyz the factor for all components
	 * @return this
	 * @see #scale(double, double, double)
	 */
	public Mat3 scale(double xyz) {
		return scale(xyz, xyz, xyz);
	}

	/**
	 * Set this matrix to a rotation matrix which rotates the given radians about a given axis.
	 * <p>
	 * The resulting matrix can be multiplied against another transformation matrix to obtain an additional rotation.
	 * <p>
	 * If you want to post-multiply a rotation transformation directly to a matrix, you can use {@link #rotate(double, Vec3) rotate()} instead.
	 *
	 * @param angle the angle in radians
	 * @param axis  the axis to rotate about (needs to be {@link Vec3#normalize() normalized})
	 * @return this
	 * @see #rotate(double, Vec3)
	 */
	public Mat3 rotation(double angle, Vec3 axis) {
		return rotation(angle, axis.x, axis.y, axis.z);
	}

	/**
	 * Set this matrix to a rotation transformation using the given {@link AxisAngle}.
	 * <p>
	 * The resulting matrix can be multiplied against another transformation matrix to obtain an additional rotation.
	 * <p>
	 * In order to apply the rotation transformation to an existing transformation, use {@link #rotate(AxisAngle) rotate()} instead.
	 * <p>
	 * Reference: <a href="http://en.wikipedia.org/wiki/Rotation_matrix#Axis_and_angle">http://en.wikipedia.org</a>
	 *
	 * @param axisAngle the {@link AxisAngle} (needs to be {@link AxisAngle#normalize() normalized})
	 * @return this
	 * @see #rotate(AxisAngle)
	 */
	public Mat3 rotation(AxisAngle axisAngle) {
		return rotation(axisAngle.angle, axisAngle.x, axisAngle.y, axisAngle.z);
	}

	/**
	 * Set this matrix to a rotation matrix which rotates the given radians about a given axis.
	 * <p>
	 * The axis described by the three components needs to be a unit vector.
	 * <p>
	 * The resulting matrix can be multiplied against another transformation matrix to obtain an additional rotation.
	 * <p>
	 * In order to apply the rotation transformation to an existing transformation, use {@link #rotate(double, double, double, double) rotate()} instead.
	 * <p>
	 * Reference: <a href="http://en.wikipedia.org/wiki/Rotation_matrix#Rotation_matrix_from_axis_and_angle">http://en.wikipedia.org</a>
	 *
	 * @param angle the angle in radians
	 * @param x     the x-component of the rotation axis
	 * @param y     the y-component of the rotation axis
	 * @param z     the z-component of the rotation axis
	 * @return this
	 * @see #rotate(double, double, double, double)
	 */
	public Mat3 rotation(double angle, double x, double y, double z) {
		double cos = Math.cos(angle);
		double sin = Math.sin(angle);
		double C = 1.0 - cos;
		m00 = cos + x * x * C;
		m10 = x * y * C - z * sin;
		m20 = x * z * C + y * sin;
		m01 = y * x * C + z * sin;
		m11 = cos + y * y * C;
		m21 = y * z * C - x * sin;
		m02 = z * x * C - y * sin;
		m12 = z * y * C + x * sin;
		m22 = cos + z * z * C;
		return this;
	}

	/**
	 * Set this matrix to a rotation transformation about the X axis.
	 * <p>
	 * Reference: <a href="http://en.wikipedia.org/wiki/Rotation_matrix#Basic_rotations">http://en.wikipedia.org</a>
	 *
	 * @param ang the angle in radians
	 * @return this
	 */
	public Mat3 rotationX(double ang) {
		double cos = Math.cos(ang);
		double sin = Math.sin(ang);
		m00 = 1.0;
		m01 = 0.0;
		m02 = 0.0;
		m10 = 0.0;
		m11 = cos;
		m12 = sin;
		m20 = 0.0;
		m21 = -sin;
		m22 = cos;
		return this;
	}

	/**
	 * Set this matrix to a rotation transformation about the Y axis.
	 * <p>
	 * Reference: <a href="http://en.wikipedia.org/wiki/Rotation_matrix#Basic_rotations">http://en.wikipedia.org</a>
	 *
	 * @param ang the angle in radians
	 * @return this
	 */
	public Mat3 rotationY(double ang) {
		double cos = Math.cos(ang);
		double sin = Math.sin(ang);
		m00 = cos;
		m01 = 0.0;
		m02 = -sin;
		m10 = 0.0;
		m11 = 1.0;
		m12 = 0.0;
		m20 = sin;
		m21 = 0.0;
		m22 = cos;
		return this;
	}

	/**
	 * Set this matrix to a rotation transformation about the Z axis.
	 * <p>
	 * Reference: <a href="http://en.wikipedia.org/wiki/Rotation_matrix#Basic_rotations">http://en.wikipedia.org</a>
	 *
	 * @param ang the angle in radians
	 * @return this
	 */
	public Mat3 rotationZ(double ang) {
		double cos = Math.cos(ang);
		double sin = Math.sin(ang);
		m00 = cos;
		m01 = sin;
		m02 = 0.0;
		m10 = -sin;
		m11 = cos;
		m12 = 0.0;
		m20 = 0.0;
		m21 = 0.0;
		m22 = 0.0;
		return this;
	}

	/**
	 * Set this matrix to the rotation transformation of the given {@link Quat}.
	 * <p>
	 * The resulting matrix can be multiplied against another transformation matrix to obtain an additional rotation.
	 * <p>
	 * In order to apply the rotation transformation to an existing transformation, use {@link #rotate(Quat) rotate()} instead.
	 * <p>
	 * Reference: <a href="http://en.wikipedia.org/wiki/Rotation_matrix#Quaternion">http://en.wikipedia.org</a>
	 *
	 * @param quat the {@link Quat}
	 * @return this
	 * @see #rotate(Quat)
	 */
	public Mat3 rotation(Quat quat) {
		double q00 = 2.0 * quat.x * quat.x;
		double q11 = 2.0 * quat.y * quat.y;
		double q22 = 2.0 * quat.z * quat.z;
		double q01 = 2.0 * quat.x * quat.y;
		double q02 = 2.0 * quat.x * quat.z;
		double q03 = 2.0 * quat.x * quat.w;
		double q12 = 2.0 * quat.y * quat.z;
		double q13 = 2.0 * quat.y * quat.w;
		double q23 = 2.0 * quat.z * quat.w;

		m00 = 1.0 - q11 - q22;
		m01 = q01 + q23;
		m02 = q02 - q13;
		m10 = q01 - q23;
		m11 = 1.0 - q22 - q00;
		m12 = q12 + q03;
		m20 = q02 + q13;
		m21 = q12 - q03;
		m22 = 1.0 - q11 - q00;

		return this;
	}

	/**
	 * Transform the given vector by this matrix.
	 *
	 * @param v the vector to transform
	 * @return this
	 */
	public Mat3 transform(Vec3 v) {
		v.mul(this);
		return this;
	}

	/**
	 * Transform the given vector by this matrix and store the result in <code>dest</code>.
	 *
	 * @param v    the vector to transform
	 * @param dest will hold the result
	 * @return this
	 */
	public Mat3 transform(Vec3 v, Vec3 dest) {
		v.mul(this, dest);
		return this;
	}

	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeDouble(m00);
		out.writeDouble(m01);
		out.writeDouble(m02);
		out.writeDouble(m10);
		out.writeDouble(m11);
		out.writeDouble(m12);
		out.writeDouble(m20);
		out.writeDouble(m21);
		out.writeDouble(m22);
	}

	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		m00 = in.readDouble();
		m01 = in.readDouble();
		m02 = in.readDouble();
		m10 = in.readDouble();
		m11 = in.readDouble();
		m12 = in.readDouble();
		m20 = in.readDouble();
		m21 = in.readDouble();
		m22 = in.readDouble();
	}

	/**
	 * Apply rotation about the X axis to this matrix by rotating the given amount of radians and store the result in <code>dest</code>.
	 * <p>
	 * If <code>M</code> is <code>this</code> matrix and <code>R</code> the rotation matrix, then the new matrix will be <code>M * R</code>. So when transforming a vector <code>v</code> with the
	 * new matrix by using <code>M * R * v</code> , the rotation will be applied first!
	 * <p>
	 * Reference: <a href="http://en.wikipedia.org/wiki/Rotation_matrix#Basic_rotations">http://en.wikipedia.org</a>
	 *
	 * @param ang  the angle in radians
	 * @param dest will hold the result
	 * @return this
	 */
	public Mat3 rotateX(double ang, Mat3 dest) {
		double cos = Math.cos(ang);
		double sin = Math.sin(ang);
		double rm11 = cos;
		double rm21 = -sin;
		double rm12 = sin;
		double rm22 = cos;

		// add temporaries for dependent values
		double nm10 = m10 * rm11 + m20 * rm12;
		double nm11 = m11 * rm11 + m21 * rm12;
		double nm12 = m12 * rm11 + m22 * rm12;
		// set non-dependent values directly
		dest.m20 = m10 * rm21 + m20 * rm22;
		dest.m21 = m11 * rm21 + m21 * rm22;
		dest.m22 = m12 * rm21 + m22 * rm22;
		// set other values
		dest.m10 = nm10;
		dest.m11 = nm11;
		dest.m12 = nm12;
		dest.m00 = m00;
		dest.m01 = m01;
		dest.m02 = m02;

		return this;
	}

	/**
	 * Apply rotation about the X axis to this matrix by rotating the given amount of radians.
	 * <p>
	 * If <code>M</code> is <code>this</code> matrix and <code>R</code> the rotation matrix, then the new matrix will be <code>M * R</code>. So when transforming a vector <code>v</code> with the
	 * new matrix by using <code>M * R * v</code> , the rotation will be applied first!
	 * <p>
	 * Reference: <a href="http://en.wikipedia.org/wiki/Rotation_matrix#Basic_rotations">http://en.wikipedia.org</a>
	 *
	 * @param ang the angle in radians
	 * @return this
	 */
	public Mat3 rotateX(double ang) {
		return rotateX(ang, this);
	}

	/**
	 * Apply rotation about the Y axis to this matrix by rotating the given amount of radians and store the result in <code>dest</code>.
	 * <p>
	 * If <code>M</code> is <code>this</code> matrix and <code>R</code> the rotation matrix, then the new matrix will be <code>M * R</code>. So when transforming a vector <code>v</code> with the
	 * new matrix by using <code>M * R * v</code> , the rotation will be applied first!
	 * <p>
	 * Reference: <a href="http://en.wikipedia.org/wiki/Rotation_matrix#Basic_rotations">http://en.wikipedia.org</a>
	 *
	 * @param ang  the angle in radians
	 * @param dest will hold the result
	 * @return this
	 */
	public Mat3 rotateY(double ang, Mat3 dest) {
		double cos = Math.cos(ang);
		double sin = Math.sin(ang);
		double rm00 = cos;
		double rm20 = sin;
		double rm02 = -sin;
		double rm22 = cos;

		// add temporaries for dependent values
		double nm00 = m00 * rm00 + m20 * rm02;
		double nm01 = m01 * rm00 + m21 * rm02;
		double nm02 = m02 * rm00 + m22 * rm02;
		// set non-dependent values directly
		dest.m20 = m00 * rm20 + m20 * rm22;
		dest.m21 = m01 * rm20 + m21 * rm22;
		dest.m22 = m02 * rm20 + m22 * rm22;
		// set other values
		dest.m00 = nm00;
		dest.m01 = nm01;
		dest.m02 = nm02;
		dest.m10 = m10;
		dest.m11 = m11;
		dest.m12 = m12;

		return this;
	}

	/**
	 * Apply rotation about the Y axis to this matrix by rotating the given amount of radians.
	 * <p>
	 * If <code>M</code> is <code>this</code> matrix and <code>R</code> the rotation matrix, then the new matrix will be <code>M * R</code>. So when transforming a vector <code>v</code> with the
	 * new matrix by using <code>M * R * v</code> , the rotation will be applied first!
	 * <p>
	 * Reference: <a href="http://en.wikipedia.org/wiki/Rotation_matrix#Basic_rotations">http://en.wikipedia.org</a>
	 *
	 * @param ang the angle in radians
	 * @return this
	 */
	public Mat3 rotateY(double ang) {
		return rotateY(ang, this);
	}

	/**
	 * Apply rotation about the Z axis to this matrix by rotating the given amount of radians and store the result in <code>dest</code>.
	 * <p>
	 * If <code>M</code> is <code>this</code> matrix and <code>R</code> the rotation matrix, then the new matrix will be <code>M * R</code>. So when transforming a vector <code>v</code> with the
	 * new matrix by using <code>M * R * v</code> , the rotation will be applied first!
	 * <p>
	 * Reference: <a href="http://en.wikipedia.org/wiki/Rotation_matrix#Basic_rotations">http://en.wikipedia.org</a>
	 *
	 * @param ang  the angle in radians
	 * @param dest will hold the result
	 * @return this
	 */
	public Mat3 rotateZ(double ang, Mat3 dest) {
		double cos = Math.cos(ang);
		double sin = Math.sin(ang);
		double rm00 = cos;
		double rm10 = -sin;
		double rm01 = sin;
		double rm11 = cos;

		// add temporaries for dependent values
		double nm00 = m00 * rm00 + m10 * rm01;
		double nm01 = m01 * rm00 + m11 * rm01;
		double nm02 = m02 * rm00 + m12 * rm01;
		// set non-dependent values directly
		dest.m10 = m00 * rm10 + m10 * rm11;
		dest.m11 = m01 * rm10 + m11 * rm11;
		dest.m12 = m02 * rm10 + m12 * rm11;
		// set other values
		dest.m00 = nm00;
		dest.m01 = nm01;
		dest.m02 = nm02;
		dest.m20 = m20;
		dest.m21 = m21;
		dest.m22 = m22;

		return this;
	}

	/**
	 * Apply rotation about the Z axis to this matrix by rotating the given amount of radians.
	 * <p>
	 * If <code>M</code> is <code>this</code> matrix and <code>R</code> the rotation matrix, then the new matrix will be <code>M * R</code>. So when transforming a vector <code>v</code> with the
	 * new matrix by using <code>M * R * v</code> , the rotation will be applied first!
	 * <p>
	 * Reference: <a href="http://en.wikipedia.org/wiki/Rotation_matrix#Basic_rotations">http://en.wikipedia.org</a>
	 *
	 * @param ang the angle in radians
	 * @return this
	 */
	public Mat3 rotateZ(double ang) {
		return rotateZ(ang, this);
	}

	/**
	 * Apply rotation to this matrix by rotating the given amount of radians about the given axis specified as x, y and z components.
	 * <p>
	 * If <code>M</code> is <code>this</code> matrix and <code>R</code> the rotation matrix, then the new matrix will be <code>M * R</code>. So when transforming a vector <code>v</code> with the
	 * new matrix by using <code>M * R * v</code> , the rotation will be applied first!
	 * <p>
	 * Reference: <a href="http://en.wikipedia.org/wiki/Rotation_matrix#Rotation_matrix_from_axis_and_angle">http://en.wikipedia.org</a>
	 *
	 * @param ang the angle in radians
	 * @param x   the x component of the axis
	 * @param y   the y component of the axis
	 * @param z   the z component of the axis
	 * @return this
	 */
	public Mat3 rotate(double ang, double x, double y, double z) {
		return rotate(ang, x, y, z, this);
	}

	/**
	 * Apply rotation to this matrix by rotating the given amount of radians about the given axis specified as x, y and z components, and store the result in <code>dest</code>.
	 * <p>
	 * If <code>M</code> is <code>this</code> matrix and <code>R</code> the rotation matrix, then the new matrix will be <code>M * R</code>. So when transforming a vector <code>v</code> with the
	 * new matrix by using <code>M * R * v</code> , the rotation will be applied first!
	 * <p>
	 * Reference: <a href="http://en.wikipedia.org/wiki/Rotation_matrix#Rotation_matrix_from_axis_and_angle">http://en.wikipedia.org</a>
	 *
	 * @param ang  the angle in radians
	 * @param x    the x component of the axis
	 * @param y    the y component of the axis
	 * @param z    the z component of the axis
	 * @param dest will hold the result
	 * @return this
	 */
	public Mat3 rotate(double ang, double x, double y, double z, Mat3 dest) {
		double s = Math.sin(ang);
		double c = Math.cos(ang);
		double C = 1.0 - c;

		// rotation matrix elements:
		// m30, m31, m32, m03, m13, m23 = 0
		double rm00 = x * x * C + c;
		double rm01 = y * x * C + z * s;
		double rm02 = z * x * C - y * s;
		double rm10 = x * y * C - z * s;
		double rm11 = y * y * C + c;
		double rm12 = z * y * C + x * s;
		double rm20 = x * z * C + y * s;
		double rm21 = y * z * C - x * s;
		double rm22 = z * z * C + c;

		// add temporaries for dependent values
		double nm00 = m00 * rm00 + m10 * rm01 + m20 * rm02;
		double nm01 = m01 * rm00 + m11 * rm01 + m21 * rm02;
		double nm02 = m02 * rm00 + m12 * rm01 + m22 * rm02;
		double nm10 = m00 * rm10 + m10 * rm11 + m20 * rm12;
		double nm11 = m01 * rm10 + m11 * rm11 + m21 * rm12;
		double nm12 = m02 * rm10 + m12 * rm11 + m22 * rm12;
		// set non-dependent values directly
		dest.m20 = m00 * rm20 + m10 * rm21 + m20 * rm22;
		dest.m21 = m01 * rm20 + m11 * rm21 + m21 * rm22;
		dest.m22 = m02 * rm20 + m12 * rm21 + m22 * rm22;
		// set other values
		dest.m00 = nm00;
		dest.m01 = nm01;
		dest.m02 = nm02;
		dest.m10 = nm10;
		dest.m11 = nm11;
		dest.m12 = nm12;

		return this;
	}

	/**
	 * Apply the rotation transformation of the given {@link Quat} to this matrix.
	 * <p>
	 * If <code>M</code> is <code>this</code> matrix and <code>Q</code> the rotation matrix obtained from the given quaternion, then the new matrix will be <code>M * Q</code>. So when transforming
	 * a vector <code>v</code> with the new matrix by using <code>M * Q * v</code>, the quaternion rotation will be applied first!
	 * <p>
	 * In order to set the matrix to a rotation transformation without post-multiplying, use {@link #rotation(Quat)}.
	 * <p>
	 * Reference: <a href="http://en.wikipedia.org/wiki/Rotation_matrix#Quaternion">http://en.wikipedia.org</a>
	 *
	 * @param quat the {@link Quat}
	 * @return this
	 * @see #rotation(Quat)
	 */
	public Mat3 rotate(Quat quat) {
		return rotate(quat, this);
	}

	/**
	 * Apply the rotation transformation of the given {@link Quat} to this matrix and store the result in <code>dest</code>.
	 * <p>
	 * If <code>M</code> is <code>this</code> matrix and <code>Q</code> the rotation matrix obtained from the given quaternion, then the new matrix will be <code>M * Q</code>. So when transforming
	 * a vector <code>v</code> with the new matrix by using <code>M * Q * v</code>, the quaternion rotation will be applied first!
	 * <p>
	 * In order to set the matrix to a rotation transformation without post-multiplying, use {@link #rotation(Quat)}.
	 * <p>
	 * Reference: <a href="http://en.wikipedia.org/wiki/Rotation_matrix#Quaternion">http://en.wikipedia.org</a>
	 *
	 * @param quat the {@link Quat}
	 * @param dest will hold the result
	 * @return this
	 * @see #rotation(Quat)
	 */
	public Mat3 rotate(Quat quat, Mat3 dest) {
		double q00 = 2.0 * quat.x * quat.x;
		double q11 = 2.0 * quat.y * quat.y;
		double q22 = 2.0 * quat.z * quat.z;
		double q01 = 2.0 * quat.x * quat.y;
		double q02 = 2.0 * quat.x * quat.z;
		double q03 = 2.0 * quat.x * quat.w;
		double q12 = 2.0 * quat.y * quat.z;
		double q13 = 2.0 * quat.y * quat.w;
		double q23 = 2.0 * quat.z * quat.w;

		double rm00 = 1.0 - q11 - q22;
		double rm01 = q01 + q23;
		double rm02 = q02 - q13;
		double rm10 = q01 - q23;
		double rm11 = 1.0 - q22 - q00;
		double rm12 = q12 + q03;
		double rm20 = q02 + q13;
		double rm21 = q12 - q03;
		double rm22 = 1.0 - q11 - q00;

		double nm00 = m00 * rm00 + m10 * rm01 + m20 * rm02;
		double nm01 = m01 * rm00 + m11 * rm01 + m21 * rm02;
		double nm02 = m02 * rm00 + m12 * rm01 + m22 * rm02;
		double nm10 = m00 * rm10 + m10 * rm11 + m20 * rm12;
		double nm11 = m01 * rm10 + m11 * rm11 + m21 * rm12;
		double nm12 = m02 * rm10 + m12 * rm11 + m22 * rm12;
		dest.m20 = m00 * rm20 + m10 * rm21 + m20 * rm22;
		dest.m21 = m01 * rm20 + m11 * rm21 + m21 * rm22;
		dest.m22 = m02 * rm20 + m12 * rm21 + m22 * rm22;
		dest.m00 = nm00;
		dest.m01 = nm01;
		dest.m02 = nm02;
		dest.m10 = nm10;
		dest.m11 = nm11;
		dest.m12 = nm12;

		return this;
	}

	/**
	 * Apply a rotation transformation, rotating about the given {@link AxisAngle}, to this matrix.
	 * <p>
	 * If <code>M</code> is <code>this</code> matrix and <code>A</code> the rotation matrix obtained from the given {@link AxisAngle}, then the new matrix will be <code>M * A</code>. So when
	 * transforming a vector <code>v</code> with the new matrix by using <code>M * A * v</code>, the {@link AxisAngle} rotation will be applied first!
	 * <p>
	 * In order to set the matrix to a rotation transformation without post-multiplying, use {@link #rotation(AxisAngle)}.
	 * <p>
	 * Reference: <a href="http://en.wikipedia.org/wiki/Rotation_matrix#Axis_and_angle">http://en.wikipedia.org</a>
	 *
	 * @param axisAngle the {@link AxisAngle} (needs to be {@link AxisAngle#normalize() normalized})
	 * @return this
	 * @see #rotate(double, double, double, double)
	 * @see #rotation(AxisAngle)
	 */
	public Mat3 rotate(AxisAngle axisAngle) {
		return rotate(axisAngle.angle, axisAngle.x, axisAngle.y, axisAngle.z);
	}

	/**
	 * Apply a rotation transformation, rotating about the given {@link AxisAngle} and store the result in <code>dest</code>.
	 * <p>
	 * If <code>M</code> is <code>this</code> matrix and <code>A</code> the rotation matrix obtained from the given {@link AxisAngle}, then the new matrix will be <code>M * A</code>. So when
	 * transforming a vector <code>v</code> with the new matrix by using <code>M * A * v</code>, the {@link AxisAngle} rotation will be applied first!
	 * <p>
	 * In order to set the matrix to a rotation transformation without post-multiplying, use {@link #rotation(AxisAngle)}.
	 * <p>
	 * Reference: <a href="http://en.wikipedia.org/wiki/Rotation_matrix#Axis_and_angle">http://en.wikipedia.org</a>
	 *
	 * @param axisAngle the {@link AxisAngle} (needs to be {@link AxisAngle#normalize() normalized})
	 * @param dest      will hold the result
	 * @return this
	 * @see #rotate(double, double, double, double)
	 * @see #rotation(AxisAngle)
	 */
	public Mat3 rotate(AxisAngle axisAngle, Mat3 dest) {
		return rotate(axisAngle.angle, axisAngle.x, axisAngle.y, axisAngle.z, dest);
	}

	/**
	 * Apply a rotation transformation, rotating the given radians about the specified axis, to this matrix.
	 * <p>
	 * If <code>M</code> is <code>this</code> matrix and <code>A</code> the rotation matrix obtained from the given angle and axis, then the new matrix will be <code>M * A</code>. So when
	 * transforming a vector <code>v</code> with the new matrix by using <code>M * A * v</code>, the axis-angle rotation will be applied first!
	 * <p>
	 * In order to set the matrix to a rotation transformation without post-multiplying, use {@link #rotation(double, Vec3)}.
	 * <p>
	 * Reference: <a href="http://en.wikipedia.org/wiki/Rotation_matrix#Axis_and_angle">http://en.wikipedia.org</a>
	 *
	 * @param angle the angle in radians
	 * @param axis  the rotation axis (needs to be {@link Vec3#normalize() normalized})
	 * @return this
	 * @see #rotate(double, double, double, double)
	 * @see #rotation(double, Vec3)
	 */
	public Mat3 rotate(double angle, Vec3 axis) {
		return rotate(angle, axis.x, axis.y, axis.z);
	}

	/**
	 * Apply a rotation transformation, rotating the given radians about the specified axis and store the result in <code>dest</code>.
	 * <p>
	 * If <code>M</code> is <code>this</code> matrix and <code>A</code> the rotation matrix obtained from the given axis and angle, then the new matrix will be <code>M * A</code>. So when
	 * transforming a vector <code>v</code> with the new matrix by using <code>M * A * v</code>, the axis-angle rotation will be applied first!
	 * <p>
	 * In order to set the matrix to a rotation transformation without post-multiplying, use {@link #rotation(double, Vec3)}.
	 * <p>
	 * Reference: <a href="http://en.wikipedia.org/wiki/Rotation_matrix#Axis_and_angle">http://en.wikipedia.org</a>
	 *
	 * @param angle the angle in radians
	 * @param axis  the rotation axis (needs to be {@link Vec3#normalize() normalized})
	 * @param dest  will hold the result
	 * @return this
	 * @see #rotate(double, double, double, double)
	 * @see #rotation(double, Vec3)
	 */
	public Mat3 rotate(double angle, Vec3 axis, Mat3 dest) {
		return rotate(angle, axis.x, axis.y, axis.z, dest);
	}

	/**
	 * Get the row at the given <code>row</code> index, starting with <code>0</code>.
	 *
	 * @param row  the row index in <tt>[0..2]</tt>
	 * @param dest will hold the row components
	 * @return the passed in destination
	 * @throws IndexOutOfBoundsException if <code>row</code> is not in <tt>[0..2]</tt>
	 */
	public Vec3 getRow(int row, Vec3 dest) throws IndexOutOfBoundsException {
		switch (row) {
			case 0:
				dest.x = m00;
				dest.y = m10;
				dest.z = m20;
				break;
			case 1:
				dest.x = m01;
				dest.y = m11;
				dest.z = m21;
				break;
			case 2:
				dest.x = m02;
				dest.y = m12;
				dest.z = m22;
				break;
			default:
				throw new IndexOutOfBoundsException();
		}

		return dest;
	}

	/**
	 * Get the column at the given <code>column</code> index, starting with <code>0</code>.
	 *
	 * @param column the column index in <tt>[0..2]</tt>
	 * @param dest   will hold the column components
	 * @return the passed in destination
	 * @throws IndexOutOfBoundsException if <code>column</code> is not in <tt>[0..2]</tt>
	 */
	public Vec3 getColumn(int column, Vec3 dest) throws IndexOutOfBoundsException {
		switch (column) {
			case 0:
				dest.x = m00;
				dest.y = m01;
				dest.z = m02;
				break;
			case 1:
				dest.x = m10;
				dest.y = m11;
				dest.z = m12;
				break;
			case 2:
				dest.x = m20;
				dest.y = m21;
				dest.z = m22;
				break;
			default:
				throw new IndexOutOfBoundsException();
		}

		return dest;
	}

	/**
	 * Compute a normal matrix from <code>this</code> matrix and store it into <code>dest</code>.
	 * <p>
	 * Please note that, if <code>this</code> is an orthogonal matrix or a matrix whose columns are orthogonal vectors, then this method need to be invoked, since in that case <code>this</code>
	 * itself is its normal matrix. In this case, use {@link #set(Mat3)} to set a given Mat3 to this matrix.
	 *
	 * @param dest will hold the result
	 * @return this
	 * @see #set(Mat3)
	 */
	public Mat3 normal(Mat3 dest) {
		double det = determinant();
		double s = 1.0 / det;
	/* Invert and transpose in one go */
		dest.set((m11 * m22 - m21 * m12) * s, -(m10 * m22 - m20 * m12) * s, (m10 * m21 - m20 * m11) * s, -(m01 * m22 - m21 * m02) * s, (m00 * m22 - m20 * m02) * s,
			 -(m00 * m21 - m20 * m01) * s, (m01 * m12 - m11 * m02) * s, -(m00 * m12 - m10 * m02) * s, (m00 * m11 - m10 * m01) * s);
		return this;
	}

	/**
	 * Apply a rotation transformation to this matrix to make <code>-z</code> point along <code>dir</code>.
	 * <p>
	 * If <code>M</code> is <code>this</code> matrix and <code>L</code> the lookalong rotation matrix, then the new matrix will be <code>M * L</code>. So when transforming a vector <code>v</code>
	 * with the new matrix by using <code>M * L * v</code>, the lookalong rotation transformation will be applied first!
	 * <p>
	 * In order to set the matrix to a lookalong transformation without post-multiplying it, use {@link #setLookAlong(Vec3, Vec3) setLookAlong()}.
	 *
	 * @param dir the direction in space to look along
	 * @param up  the direction of 'up'
	 * @return this
	 * @see #lookAlong(double, double, double, double, double, double)
	 * @see #setLookAlong(Vec3, Vec3)
	 */
	public Mat3 lookAlong(Vec3 dir, Vec3 up) {
		return lookAlong(dir.x, dir.y, dir.z, up.x, up.y, up.z, this);
	}

	/**
	 * Apply a rotation transformation to this matrix to make <code>-z</code> point along <code>dir</code> and store the result in <code>dest</code>.
	 * <p>
	 * If <code>M</code> is <code>this</code> matrix and <code>L</code> the lookalong rotation matrix, then the new matrix will be <code>M * L</code>. So when transforming a vector <code>v</code>
	 * with the new matrix by using <code>M * L * v</code>, the lookalong rotation transformation will be applied first!
	 * <p>
	 * In order to set the matrix to a lookalong transformation without post-multiplying it, use {@link #setLookAlong(Vec3, Vec3) setLookAlong()}.
	 *
	 * @param dir  the direction in space to look along
	 * @param up   the direction of 'up'
	 * @param dest will hold the result
	 * @return this
	 * @see #lookAlong(double, double, double, double, double, double)
	 * @see #setLookAlong(Vec3, Vec3)
	 */
	public Mat3 lookAlong(Vec3 dir, Vec3 up, Mat3 dest) {
		return lookAlong(dir.x, dir.y, dir.z, up.x, up.y, up.z, dest);
	}

	/**
	 * Apply a rotation transformation to this matrix to make <code>-z</code> point along <code>dir</code> and store the result in <code>dest</code>.
	 * <p>
	 * If <code>M</code> is <code>this</code> matrix and <code>L</code> the lookalong rotation matrix, then the new matrix will be <code>M * L</code>. So when transforming a vector <code>v</code>
	 * with the new matrix by using <code>M * L * v</code>, the lookalong rotation transformation will be applied first!
	 * <p>
	 * In order to set the matrix to a lookalong transformation without post-multiplying it, use {@link #setLookAlong(double, double, double, double, double, double) setLookAlong()}
	 *
	 * @param dirX the x-coordinate of the direction to look along
	 * @param dirY the y-coordinate of the direction to look along
	 * @param dirZ the z-coordinate of the direction to look along
	 * @param upX  the x-coordinate of the up vector
	 * @param upY  the y-coordinate of the up vector
	 * @param upZ  the z-coordinate of the up vector
	 * @param dest will hold the result
	 * @return this
	 * @see #setLookAlong(double, double, double, double, double, double)
	 */
	public Mat3 lookAlong(double dirX, double dirY, double dirZ, double upX, double upY, double upZ, Mat3 dest) {
		// Normalize direction
		double dirLength = Math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
		double dirnX = dirX / dirLength;
		double dirnY = dirY / dirLength;
		double dirnZ = dirZ / dirLength;
		// right = direction x up
		double rightX, rightY, rightZ;
		rightX = dirnY * upZ - dirnZ * upY;
		rightY = dirnZ * upX - dirnX * upZ;
		rightZ = dirnX * upY - dirnY * upX;
		// normalize right
		double rightLength = Math.sqrt(rightX * rightX + rightY * rightY + rightZ * rightZ);
		rightX /= rightLength;
		rightY /= rightLength;
		rightZ /= rightLength;
		// up = right x direction
		double upnX = rightY * dirnZ - rightZ * dirnY;
		double upnY = rightZ * dirnX - rightX * dirnZ;
		double upnZ = rightX * dirnY - rightY * dirnX;

		// calculate right matrix elements
		double rm00 = rightX;
		double rm01 = upnX;
		double rm02 = -dirnX;
		double rm10 = rightY;
		double rm11 = upnY;
		double rm12 = -dirnY;
		double rm20 = rightZ;
		double rm21 = upnZ;
		double rm22 = -dirnZ;

		// perform optimized matrix multiplication
		// introduce temporaries for dependent results
		double nm00 = m00 * rm00 + m10 * rm01 + m20 * rm02;
		double nm01 = m01 * rm00 + m11 * rm01 + m21 * rm02;
		double nm02 = m02 * rm00 + m12 * rm01 + m22 * rm02;
		double nm10 = m00 * rm10 + m10 * rm11 + m20 * rm12;
		double nm11 = m01 * rm10 + m11 * rm11 + m21 * rm12;
		double nm12 = m02 * rm10 + m12 * rm11 + m22 * rm12;
		dest.m20 = m00 * rm20 + m10 * rm21 + m20 * rm22;
		dest.m21 = m01 * rm20 + m11 * rm21 + m21 * rm22;
		dest.m22 = m02 * rm20 + m12 * rm21 + m22 * rm22;
		// set the rest of the matrix elements
		dest.m00 = nm00;
		dest.m01 = nm01;
		dest.m02 = nm02;
		dest.m10 = nm10;
		dest.m11 = nm11;
		dest.m12 = nm12;

		return this;
	}

	/**
	 * Apply a rotation transformation to this matrix to make <code>-z</code> point along <code>dir</code>.
	 * <p>
	 * If <code>M</code> is <code>this</code> matrix and <code>L</code> the lookalong rotation matrix, then the new matrix will be <code>M * L</code>. So when transforming a vector <code>v</code>
	 * with the new matrix by using <code>M * L * v</code>, the lookalong rotation transformation will be applied first!
	 * <p>
	 * In order to set the matrix to a lookalong transformation without post-multiplying it, use {@link #setLookAlong(double, double, double, double, double, double) setLookAlong()}
	 *
	 * @param dirX the x-coordinate of the direction to look along
	 * @param dirY the y-coordinate of the direction to look along
	 * @param dirZ the z-coordinate of the direction to look along
	 * @param upX  the x-coordinate of the up vector
	 * @param upY  the y-coordinate of the up vector
	 * @param upZ  the z-coordinate of the up vector
	 * @return this
	 * @see #setLookAlong(double, double, double, double, double, double)
	 */
	public Mat3 lookAlong(double dirX, double dirY, double dirZ, double upX, double upY, double upZ) {
		return lookAlong(dirX, dirY, dirZ, upX, upY, upZ, this);
	}

	/**
	 * Set this matrix to a rotation transformation to make <code>-z</code> point along <code>dir</code>.
	 * <p>
	 * In order to apply the lookalong transformation to any previous existing transformation, use {@link #lookAlong(Vec3, Vec3)}.
	 *
	 * @param dir the direction in space to look along
	 * @param up  the direction of 'up'
	 * @return this
	 * @see #setLookAlong(Vec3, Vec3)
	 * @see #lookAlong(Vec3, Vec3)
	 */
	public Mat3 setLookAlong(Vec3 dir, Vec3 up) {
		return setLookAlong(dir.x, dir.y, dir.z, up.x, up.y, up.z);
	}

	/**
	 * Set this matrix to a rotation transformation to make <code>-z</code> point along <code>dir</code>.
	 * <p>
	 * In order to apply the lookalong transformation to any previous existing transformation, use {@link #lookAlong(double, double, double, double, double, double) lookAlong()}
	 *
	 * @param dirX the x-coordinate of the direction to look along
	 * @param dirY the y-coordinate of the direction to look along
	 * @param dirZ the z-coordinate of the direction to look along
	 * @param upX  the x-coordinate of the up vector
	 * @param upY  the y-coordinate of the up vector
	 * @param upZ  the z-coordinate of the up vector
	 * @return this
	 * @see #setLookAlong(double, double, double, double, double, double)
	 * @see #lookAlong(double, double, double, double, double, double)
	 */
	public Mat3 setLookAlong(double dirX, double dirY, double dirZ, double upX, double upY, double upZ) {
		// Normalize direction
		double dirLength = Math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
		double dirnX = dirX / dirLength;
		double dirnY = dirY / dirLength;
		double dirnZ = dirZ / dirLength;
		// right = direction x up
		double rightX, rightY, rightZ;
		rightX = dirnY * upZ - dirnZ * upY;
		rightY = dirnZ * upX - dirnX * upZ;
		rightZ = dirnX * upY - dirnY * upX;
		// normalize right
		double rightLength = Math.sqrt(rightX * rightX + rightY * rightY + rightZ * rightZ);
		rightX /= rightLength;
		rightY /= rightLength;
		rightZ /= rightLength;
		// up = right x direction
		double upnX = rightY * dirnZ - rightZ * dirnY;
		double upnY = rightZ * dirnX - rightX * dirnZ;
		double upnZ = rightX * dirnY - rightY * dirnX;

		m00 = rightX;
		m01 = upnX;
		m02 = -dirnX;
		m10 = rightY;
		m11 = upnY;
		m12 = -dirnY;
		m20 = rightZ;
		m21 = upnZ;
		m22 = -dirnZ;

		return this;
	}

	/**
	 * Return the specified {@link Mat3}.
	 * <p>
	 * When using method chaining in a fluent interface style, this method can be used to switch the <i>context object</i>, on which further method invocations operate, to be the given matrix.
	 *
	 * @param m the {@link Mat3} to return
	 * @return that matrix
	 */
	public Mat3 with(Mat3 m) {
		return m;
	}

	/**
	 * Return the specified {@link Vec3}.
	 * <p>
	 * When using method chaining in a fluent interface style, this method can be used to switch the <i>context object</i>, on which further method invocations operate, to be the given vector.
	 *
	 * @param v the {@link Vec3} to return
	 * @return that vector
	 */
	public Vec3 with(Vec3 v) {
		return v;
	}

	/**
	 * Return the specified {@link Quat}.
	 * <p>
	 * When using method chaining in a fluent interface style, this method can be used to switch the <i>context object</i>, on which further method invocations operate, to be the given
	 * quaternion.
	 *
	 * @param q the {@link Quat} to return
	 * @return that quaternion
	 */
	public Quat with(Quat q) {
		return q;
	}

	/**
	 * Return the specified {@link AxisAngle}.
	 * <p>
	 * When using method chaining in a fluent interface style, this method can be used to switch the <i>context object</i>, on which further method invocations operate, to be the given {@link
	 * AxisAngle}.
	 *
	 * @param a the {@link AxisAngle} to return
	 * @return that quaternion
	 */
	public AxisAngle with(AxisAngle a) {
		return a;
	}


	/**
	 * a Mat3 is a Supplier of type Mat3
	 *
	 * @return this
	 */
	public Mat3 get() {
		return this;
	}

	@Override
	public Mutable duplicate() {
		return new Mat3(this);
	}
}
