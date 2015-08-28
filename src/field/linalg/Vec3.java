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
 * Contains the definition of a Vector comprising 3 doubles and associated transformations.
 *
 * @author Richard Greenlees
 * @author Kai Burjack
 */
public class Vec3 implements Externalizable, Supplier<Vec3> {

	private static final long serialVersionUID = 1L;

	/**
	 * The x component of the vector.
	 */
	public double x;
	/**
	 * The y component of the vector.
	 */
	public double y;
	/**
	 * The z component of the vector.
	 */
	public double z;

	/**
	 * Create a new {@link Vec3} with all components set to zero.
	 */
	public Vec3() {
	}

	/**
	 * Create a new {@link Vec3} and initialize all three components with the given value.
	 *
	 * @param d the value of all three components
	 */
	public Vec3(double d) {
		this(d, d, d);
	}

	/**
	 * Create a new {@link Vec3} with the given component values.
	 *
	 * @param x the value of x
	 * @param y the value of y
	 * @param z the value of z
	 */
	public Vec3(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	/**
	 * Create a new {@link Vec3} with the first two components from the given <code>v</code> and the given <code>z</code>
	 *
	 * @param v the {@link Vec2} to copy the values from
	 * @param z the z component
	 */
	public Vec3(Vec2 v, double z) {
		this.x = v.x;
		this.y = v.y;
		this.z = z;
	}

	/**
	 * Create a new {@link Vec3} whose values will be copied from the given vector.
	 *
	 * @param v provides the initial values for the new vector
	 */
	public Vec3(Vec3 v) {
		this.x = v.x;
		this.y = v.y;
		this.z = v.z;
	}


	/**
	 * Create a new {@link Vec3} and read this vector from the supplied {@link ByteBuffer} at the current buffer {@link ByteBuffer#position() position}.
	 * <p>
	 * This method will not increment the position of the given ByteBuffer.
	 * <p>
	 * If you want to specify the offset into the ByteBuffer at which the vector is read, you can use {@link #Vec3(int, ByteBuffer)}, taking the absolute position as parameter.
	 *
	 * @param buffer values will be read in <tt>x, y, z</tt> order
	 * @see #Vec3(int, ByteBuffer)
	 */
	public Vec3(ByteBuffer buffer) {
		this(buffer.position(), buffer);
	}

	/**
	 * Create a new {@link Vec3} and read this vector from the supplied {@link ByteBuffer} starting at the specified absolute buffer position/index.
	 * <p>
	 * This method will not increment the position of the given ByteBuffer.
	 *
	 * @param index  the absolute position into the ByteBuffer
	 * @param buffer values will be read in <tt>x, y, z</tt> order
	 */
	public Vec3(int index, ByteBuffer buffer) {
		x = buffer.getDouble(index);
		y = buffer.getDouble(index + 8);
		z = buffer.getDouble(index + 16);
	}

	/**
	 * Create a new {@link Vec3} and read this vector from the supplied {@link DoubleBuffer} at the current buffer {@link DoubleBuffer#position() position}.
	 * <p>
	 * This method will not increment the position of the given DoubleBuffer.
	 * <p>
	 * If you want to specify the offset into the DoubleBuffer at which the vector is read, you can use {@link #Vec3(int, DoubleBuffer)}, taking the absolute position as parameter.
	 *
	 * @param buffer values will be read in <tt>x, y, z</tt> order
	 * @see #Vec3(int, DoubleBuffer)
	 */
	public Vec3(DoubleBuffer buffer) {
		this(buffer.position(), buffer);
	}

	/**
	 * Create a new {@link Vec3} and read this vector from the supplied {@link DoubleBuffer} starting at the specified absolute buffer position/index.
	 * <p>
	 * This method will not increment the position of the given DoubleBuffer.
	 *
	 * @param index  the absolute position into the DoubleBuffer
	 * @param buffer values will be read in <tt>x, y, z</tt> order
	 */
	public Vec3(int index, DoubleBuffer buffer) {
		x = buffer.get(index);
		y = buffer.get(index + 1);
		z = buffer.get(index + 2);
	}


	/**
	 * Create a new {@link Vec3} and read this vector from the supplied {@link DoubleBuffer} at the current buffer {@link DoubleBuffer#position() position}.
	 * <p>
	 * This method will not increment the position of the given DoubleBuffer.
	 * <p>
	 * If you want to specify the offset into the DoubleBuffer at which the vector is read, you can use {@link #Vec3(int, DoubleBuffer)}, taking the absolute position as parameter.
	 *
	 * @param buffer values will be read in <tt>x, y, z</tt> order
	 * @see #Vec3(int, DoubleBuffer)
	 */
	public Vec3(FloatBuffer buffer) {
		this(buffer.position(), buffer);
	}

	/**
	 * Create a new {@link Vec3} and read this vector from the supplied {@link DoubleBuffer} starting at the specified absolute buffer position/index.
	 * <p>
	 * This method will not increment the position of the given DoubleBuffer.
	 *
	 * @param index  the absolute position into the DoubleBuffer
	 * @param buffer values will be read in <tt>x, y, z</tt> order
	 */
	public Vec3(int index, FloatBuffer buffer) {
		x = buffer.get(index);
		y = buffer.get(index + 1);
		z = buffer.get(index + 2);
	}

	/**
	 * Subtract <code>v2</code> from <code>v1</code> and store the result in <code>dest</code>.
	 *
	 * @param v1   the vector to subtract from
	 * @param v2   the vector to subtract
	 * @param dest will hold the result (automatically allocated if null)
	 * @return dest
	 */
	public static Vec3 sub(Vec3 v1, Vec3 v2, Vec3 dest) {
		if (dest == null) dest = new Vec3();
		dest.x = v1.x - v2.x;
		dest.y = v1.y - v2.y;
		dest.z = v1.z - v2.z;
		return dest;
	}

	/**
	 * Adds <code>v2</code> from <code>v1</code> and store the result in <code>dest</code>.
	 *
	 * @param v1   the vector to add to
	 * @param v2   the vector to add
	 * @param dest will hold the result (automatically allocated if null)
	 * @return dest
	 */
	public static Vec3 add(Vec3 v1, Vec3 v2, Vec3 dest) {
		if (dest == null) dest = new Vec3();
		dest.x = v1.x + v2.x;
		dest.y = v1.y + v2.y;
		dest.z = v1.z + v2.z;
		return dest;
	}

	/**
	 * returns x*w+y in dest
	 *
	 * @param x    the vector to add to
	 * @param w    the times 'w'
	 * @param y    add 'y'
	 * @param dest will hold the result (automatically allocated if null)
	 * @return dest
	 */
	public static Vec3 fma(Vec3 x, double w, Vec3 y, Vec3 dest) {

		if (dest == null) dest = new Vec3();
		dest.x = x.x * w + y.x;
		dest.y = x.y * w + y.y;
		dest.z = x.z * w + y.z;

		return dest;
	}

	/**
	 * Multiply <code>v</code> by the <code>scalar</code> value and store the result in <code>dest</code>.
	 *
	 * @param v      the vector to multiply
	 * @param scalar the scalar to multiply the given vector by
	 * @param dest   will hold the result
	 */
	public static Vec3 mul(Vec3 v, double scalar, Vec3 dest) {
		if (dest == null) dest = new Vec3();
		dest.x = v.x * scalar;
		dest.y = v.y * scalar;
		dest.z = v.z * scalar;
		return dest;
	}

	/**
	 * Calculate the cross product of a and b and store the result in <code>dest</code>.
	 *
	 * @param a    the first vec3
	 * @param v    the second vec3
	 * @param dest will hold the result
	 * @return dest
	 */
	public static Vec3 cross(Vec3 a, Vec3 v, Vec3 dest) {
		if (dest == null) dest = new Vec3();
		dest.set(a.y * v.z - a.z * v.y, a.z * v.x - a.x * v.z, a.x * v.y - a.y * v.x);
		return dest;
	}

	/**
	 * Return the dot product of a vector and another.
	 *
	 * @param a the first vector
	 * @param v the other vector
	 * @return the dot product
	 */
	public static double dot(Vec3 a, Vec3 v) {
		return a.x * v.x + a.y * v.y + a.z * v.z;
	}

	/**
	 * Linearly interpolate <code>a</code> and <code>b</code> using the given interpolation factor <code>t</code> and store the result in <code>dest</code>.
	 * <p>
	 * If <code>t</code> is <tt>0.0</tt> then the result is <code>this</code>. If the interpolation factor is <code>1.0</code> then the result is <code>other</code>.
	 *
	 * @param a    the first Vec3
	 * @param b    the second Vec3
	 * @param t    the interpolate (t=0 to 1)
	 * @param dest will hold the result
	 * @return this
	 */
	public static Vec3 lerp(Vec3 a, Vec3 b, double t, Vec3 dest) {
		if (dest == null) dest = new Vec3();
		dest.x = (1.0 - t) * a.x + t * b.x;
		dest.y = (1.0 - t) * a.y + t * b.y;
		dest.z = (1.0 - t) * a.z + t * b.z;
		return dest;
	}

	/**
	 * Set the first two components from the given <code>v</code> and the z component from the given <code>z</code>
	 *
	 * @param v the {@link Vec2} to copy the values from
	 * @param z the z component
	 * @return this
	 */
	public Vec3 set(Vec2 v, double z) {
		this.x = v.x;
		this.y = v.y;
		this.z = z;
		return this;
	}

	/**
	 * Set the x, y and z components to match the supplied vector.
	 *
	 * @param v the vector to set this vector's components from
	 * @return this
	 */
	public Vec3 set(Vec3 v) {
		x = v.x;
		y = v.y;
		z = v.z;
		return this;
	}

	/**
	 * Set the x, y, and z components to the supplied value.
	 *
	 * @param d the value of all three components
	 * @return this
	 */
	public Vec3 set(double d) {
		return set(d, d, d);
	}

	/**
	 * Set the x, y and z components to the supplied values.
	 *
	 * @param x the x component
	 * @param y the y component
	 * @param z the z component
	 * @return this
	 */
	public Vec3 set(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
		return this;
	}

	/**
	 * Read this vector from the supplied {@link ByteBuffer} at the current buffer {@link ByteBuffer#position() position}.
	 * <p>
	 * This method will not increment the position of the given ByteBuffer.
	 * <p>
	 * If you want to specify the offset into the ByteBuffer at which the vector is read, you can use {@link #set(int, ByteBuffer)}, taking the absolute position as parameter.
	 *
	 * @param buffer values will be read in <tt>x, y, z</tt> order
	 * @return this
	 * @see #set(int, ByteBuffer)
	 */
	public Vec3 set(ByteBuffer buffer) {
		return set(buffer.position(), buffer);
	}

	/**
	 * Read this vector from the supplied {@link ByteBuffer} starting at the specified absolute buffer position/index.
	 * <p>
	 * This method will not increment the position of the given ByteBuffer.
	 *
	 * @param index  the absolute position into the ByteBuffer
	 * @param buffer values will be read in <tt>x, y, z</tt> order
	 * @return this
	 */
	public Vec3 set(int index, ByteBuffer buffer) {
		x = buffer.getDouble(index);
		y = buffer.getDouble(index + 8);
		z = buffer.getDouble(index + 16);
		return this;
	}

	/**
	 * Read this vector from the supplied {@link DoubleBuffer} at the current buffer {@link DoubleBuffer#position() position}.
	 * <p>
	 * This method will not increment the position of the given DoubleBuffer.
	 * <p>
	 * If you want to specify the offset into the DoubleBuffer at which the vector is read, you can use {@link #set(int, DoubleBuffer)}, taking the absolute position as parameter.
	 *
	 * @param buffer values will be read in <tt>x, y, z</tt> order
	 * @return this
	 * @see #set(int, DoubleBuffer)
	 */
	public Vec3 set(DoubleBuffer buffer) {
		return set(buffer.position(), buffer);
	}

	/**
	 * Read this vector from the supplied {@link DoubleBuffer} starting at the specified absolute buffer position/index.
	 * <p>
	 * This method will not increment the position of the given DoubleBuffer.
	 *
	 * @param index  the absolute position into the DoubleBuffer
	 * @param buffer values will be read in <tt>x, y, z</tt> order
	 * @return this
	 */
	public Vec3 set(int index, DoubleBuffer buffer) {
		x = buffer.get(index);
		y = buffer.get(index + 1);
		z = buffer.get(index + 2);
		return this;
	}

	/**
	 * Store this vector into the supplied {@link ByteBuffer} at the current buffer {@link ByteBuffer#position() position}.
	 * <p>
	 * This method will not increment the position of the given ByteBuffer.
	 * <p>
	 * If you want to specify the offset into the ByteBuffer at which the vector is stored, you can use {@link #get(int, ByteBuffer)}, taking the absolute position as parameter.
	 *
	 * @param buffer will receive the values of this vector in <tt>x, y, z</tt> order
	 * @return the passed in buffer
	 * @see #get(int, ByteBuffer)
	 */
	public ByteBuffer get(ByteBuffer buffer) {
		return get(buffer.position(), buffer);
	}

	/**
	 * Store this vector into the supplied {@link ByteBuffer} starting at the specified absolute buffer position/index.
	 * <p>
	 * This method will not increment the position of the given ByteBuffer.
	 *
	 * @param index  the absolute position into the ByteBuffer
	 * @param buffer will receive the values of this vector in <tt>x, y, z</tt> order
	 * @return the passed in buffer
	 */
	public ByteBuffer get(int index, ByteBuffer buffer) {
		buffer.putDouble(index, x);
		buffer.putDouble(index + 8, y);
		buffer.putDouble(index + 16, z);
		return buffer;
	}

	/**
	 * Store this vector into the supplied {@link DoubleBuffer} at the current buffer {@link DoubleBuffer#position() position}.
	 * <p>
	 * This method will not increment the position of the given DoubleBuffer.
	 * <p>
	 * If you want to specify the offset into the DoubleBuffer at which the vector is stored, you can use {@link #get(int, DoubleBuffer)}, taking the absolute position as parameter.
	 *
	 * @param buffer will receive the values of this vector in <tt>x, y, z</tt> order
	 * @return the passed in buffer
	 * @see #get(int, DoubleBuffer)
	 */
	public DoubleBuffer get(DoubleBuffer buffer) {
		return get(buffer.position(), buffer);
	}

	/**
	 * Store this vector into the supplied {@link DoubleBuffer} starting at the specified absolute buffer position/index.
	 * <p>
	 * This method will not increment the position of the given DoubleBuffer.
	 *
	 * @param index  the absolute position into the DoubleBuffer
	 * @param buffer will receive the values of this vector in <tt>x, y, z</tt> order
	 * @return the passed in buffer
	 */
	public DoubleBuffer get(int index, DoubleBuffer buffer) {
		buffer.put(index, x);
		buffer.put(index + 1, y);
		buffer.put(index + 2, z);
		return buffer;
	}

	/**
	 * Subtract the supplied vector from this one.
	 *
	 * @param v the vector to subtract from this
	 * @return this
	 */
	public Vec3 sub(Vec3 v) {
		x -= v.x;
		y -= v.y;
		z -= v.z;
		return this;
	}

	/**
	 * Subtract the supplied vector from this one and store the result in <code>dest</code>.
	 *
	 * @param v    the vector to subtract from <code>this</code>
	 * @param dest will hold the result
	 * @return this
	 */
	public Vec3 sub(Vec3 v, Vec3 dest) {
		dest.x = x - v.x;
		dest.y = y - v.y;
		dest.z = z - v.z;
		return this;
	}

	/**
	 * Subtract <tt>(x, y, z)</tt> from this vector.
	 *
	 * @param x the x component to subtract
	 * @param y the y component to subtract
	 * @param z the z component to subtract
	 * @return this
	 */
	public Vec3 sub(double x, double y, double z) {
		this.x -= x;
		this.y -= y;
		this.z -= z;
		return this;
	}

	/**
	 * Subtract <tt>(x, y, z)</tt> from this vector and store the result in <code>dest</code>.
	 *
	 * @param x    the x component to subtract
	 * @param y    the y component to subtract
	 * @param z    the z component to subtract
	 * @param dest will hold the result
	 * @return this
	 */
	public Vec3 sub(double x, double y, double z, Vec3 dest) {
		dest.x = this.x - x;
		dest.y = this.y - y;
		dest.z = this.z - z;
		return this;
	}

	/**
	 * Add the supplied vector to this one.
	 *
	 * @param v the vector to add
	 * @return this
	 */
	public Vec3 add(Vec3 v) {
		x += v.x;
		y += v.y;
		z += v.z;
		return this;
	}

	/**
	 * Add the supplied vector to this one and store the result in <code>dest</code>.
	 *
	 * @param v    the vector to add
	 * @param dest will hold the result
	 * @return this
	 */
	public Vec3 add(Vec3 v, Vec3 dest) {
		dest.x = x + v.x;
		dest.y = y + v.y;
		dest.z = z + v.z;
		return this;
	}

	/**
	 * Increment the components of this vector by the given values.
	 *
	 * @param x the x component to add
	 * @param y the y component to add
	 * @param z the z component to add
	 * @return this
	 */
	public Vec3 add(double x, double y, double z) {
		this.x += x;
		this.y += y;
		this.z += z;
		return this;
	}

	/**
	 * Increment the components of this vector by the given values and store the result in <code>dest</code>.
	 *
	 * @param x    the x component to add
	 * @param y    the y component to add
	 * @param z    the z component to add
	 * @param dest will hold the result
	 * @return this
	 */
	public Vec3 add(double x, double y, double z, Vec3 dest) {
		dest.x = this.x + x;
		dest.y = this.y + y;
		dest.z = this.z + z;
		return this;
	}

	/**
	 * Add the component-wise multiplication of <code>a * b</code> to this vector.
	 *
	 * @param a the first multiplicand
	 * @param b the second multiplicand
	 * @return this
	 */
	public Vec3 fma(Vec3 a, Vec3 b) {
		x += a.x * b.x;
		y += a.y * b.y;
		z += a.z * b.z;
		return this;
	}

	/**
	 * Add the component-wise multiplication of <code>a * b</code> to this vector.
	 *
	 * @param a the first multiplicand
	 * @param b the second multiplicand
	 * @return this
	 */
	public Vec3 fma(double a, Vec3 b) {
		x += a * b.x;
		y += a * b.y;
		z += a * b.z;
		return this;
	}

	/**
	 * Add the component-wise multiplication of <code>a * b</code> to this vector.
	 *
	 * @param a the first multiplicand
	 * @param b the second multiplicand
	 * @return this
	 */
	public Vec3 fma(Vec3 b, double a) {
		x += a * b.x;
		y += a * b.y;
		z += a * b.z;
		return this;
	}

	/**
	 * Add the component-wise multiplication of <code>a * b</code> to this vector and store the result in <code>dest</code>.
	 *
	 * @param a    the first multiplicand
	 * @param b    the second multiplicand
	 * @param dest will hold the result
	 * @return this
	 */
	public Vec3 fma(Vec3 a, Vec3 b, Vec3 dest) {
		dest.x = x + a.x * b.x;
		dest.y = y + a.y * b.y;
		dest.z = z + a.z * b.z;
		return this;
	}

	/**
	 * Add the component-wise multiplication of <code>a * b</code> to this vector and store the result in <code>dest</code>.
	 *
	 * @param a    the first multiplicand
	 * @param b    the second multiplicand
	 * @param dest will hold the result
	 * @return this
	 */
	public Vec3 fma(double a, Vec3 b, Vec3 dest) {
		dest.x = x + a * b.x;
		dest.y = y + a * b.y;
		dest.z = z + a * b.z;
		return this;
	}

	/**
	 * Multiply this Vec3 component-wise by another Vec3.
	 *
	 * @param v the vector to multiply by
	 * @return this
	 */
	public Vec3 mul(Vec3 v) {
		x *= v.x;
		y *= v.y;
		z *= v.z;
		return this;
	}

	/**
	 * Multiply this Vec3 component-wise by another Vec3 and store the result in <code>dest</code>.
	 *
	 * @param v    the vector to multiply by
	 * @param dest will hold the result
	 * @return this
	 */
	public Vec3 mul(Vec3 v, Vec3 dest) {
		dest.x = x * v.x;
		dest.y = y * v.y;
		dest.z = z * v.z;
		return this;
	}

	/**
	 * Divide this Vec3 component-wise by another Vec3.
	 *
	 * @param v the vector to divide by
	 * @return this
	 */
	public Vec3 div(Vec3 v) {
		x /= v.x;
		y /= v.y;
		z /= v.z;
		return this;
	}

	/**
	 * Divide this Vec3 component-wise by another Vec3 and store the result in <code>dest</code>.
	 *
	 * @param v    the vector to divide by
	 * @param dest will hold the result
	 * @return this
	 */
	public Vec3 div(Vec3 v, Vec3 dest) {
		dest.x = x / v.x;
		dest.y = y / v.y;
		dest.z = z / v.z;
		return this;
	}

	/**
	 * Multiply this Vec3 by the given matrix <code>mat</code> and store the result in <code>dest</code>.
	 * <p>
	 * This method uses <tt>w=1.0</tt> as the fourth vector component.
	 *
	 * @param mat  the matrix to multiply this vector by
	 * @param dest will hold the result
	 * @return this
	 */
	public Vec3 mul(Mat4 mat, Vec3 dest) {
		if (this != dest) {
			dest.x = mat.m00 * x + mat.m10 * y + mat.m20 * z + mat.m30;
			dest.y = mat.m01 * x + mat.m11 * y + mat.m21 * z + mat.m31;
			dest.z = mat.m02 * x + mat.m12 * y + mat.m22 * z + mat.m32;
		} else {
			dest.set(mat.m00 * x + mat.m10 * y + mat.m20 * z + mat.m30, mat.m01 * x + mat.m11 * y + mat.m21 * z + mat.m31, mat.m02 * x + mat.m12 * y + mat.m22 * z + mat.m32);
		}
		return this;
	}

	/**
	 * Multiply this Vec3 by the given matrix <code>mat</code>.
	 * <p>
	 * This method uses <tt>w=1.0</tt> as the fourth vector component.
	 *
	 * @param mat the matrix to multiply this vector by
	 * @return this
	 */
	public Vec3 mul(Mat4 mat) {
		return mul(mat, this);
	}

	/**
	 * Multiply this Vec3 by the given matrix <code>mat</code>, perform perspective division and store the result in <code>dest</code>.
	 * <p>
	 * This method uses <tt>w=1.0</tt> as the fourth vector component.
	 * <p>
	 * This method differs from {@link #mul(Mat4, Vec3)} in that it also performs perspective division.
	 *
	 * @param mat  the matrix to multiply this vector by
	 * @param dest will hold the result
	 * @return this
	 * @see #mul(Mat4, Vec3)
	 */
	public Vec3 mulProject(Mat4 mat, Vec3 dest) {
		double w = mat.m03 * x + mat.m13 * y + mat.m23 * z + mat.m33;
		if (this != dest) {
			dest.x = (mat.m00 * x + mat.m10 * y + mat.m20 * z + mat.m30) / w;
			dest.y = (mat.m01 * x + mat.m11 * y + mat.m21 * z + mat.m31) / w;
			dest.z = (mat.m02 * x + mat.m12 * y + mat.m22 * z + mat.m32) / w;
		} else {
			dest.set((mat.m00 * x + mat.m10 * y + mat.m20 * z + mat.m30) / w, (mat.m01 * x + mat.m11 * y + mat.m21 * z + mat.m31) / w,
				 (mat.m02 * x + mat.m12 * y + mat.m22 * z + mat.m32) / w);
		}
		return this;
	}

	/**
	 * Multiply this Vec3 by the given matrix <code>mat</code>, perform perspective division.
	 * <p>
	 * This method uses <tt>w=1.0</tt> as the fourth vector component.
	 * <p>
	 * This method differs from {@link #mul(Mat4)} in that it also performs perspective division.
	 *
	 * @param mat the matrix to multiply this vector by
	 * @return this
	 * @see #mul(Mat4)
	 */
	public Vec3 mulProject(Mat4 mat) {
		return mulProject(mat, this);
	}

	/**
	 * Multiply this Vec3 by the given matrix <code>mat</code>.
	 *
	 * @param mat the matrix to multiply this vector by
	 * @return this
	 */
	public Vec3 mul(Mat3 mat) {
		return mul(mat, this);
	}

	/**
	 * Multiply <code>this</code> by the given matrix <code>mat</code> and store the result in <code>dest</code>.
	 *
	 * @param mat  the matrix to multiply this vector by
	 * @param dest will hold the result
	 * @return this
	 */
	public Vec3 mul(Mat3 mat, Vec3 dest) {
		if (this != dest) {
			dest.x = mat.m00 * x + mat.m10 * y + mat.m20 * z;
			dest.y = mat.m01 * x + mat.m11 * y + mat.m21 * z;
			dest.z = mat.m02 * x + mat.m12 * y + mat.m22 * z;
		} else {
			dest.set(mat.m00 * x + mat.m10 * y + mat.m20 * z, mat.m01 * x + mat.m11 * y + mat.m21 * z, mat.m02 * x + mat.m12 * y + mat.m22 * z);
		}
		return this;
	}

	/**
	 * Multiply this Vec3 by the given scalar value.
	 *
	 * @param scalar the scalar to multiply this vector by
	 * @return this
	 */
	public Vec3 mul(double scalar) {
		x *= scalar;
		y *= scalar;
		z *= scalar;
		return this;
	}

	/**
	 * Multiply this Vec3 by the given scalar value.
	 *
	 * @param scalar the scalar to multiply this vector by
	 * @return this
	 */
	public Vec3 scale(double scalar) {
		return mul(scalar);
	}

	/**
	 * Multiply this Vec3 by the given scalar value and store the result in <code>dest</code>.
	 *
	 * @param scalar the scalar factor
	 * @param dest   will hold the result
	 * @return this
	 */
	public Vec3 mul(double scalar, Vec3 dest) {
		dest.x = x * scalar;
		dest.y = y * scalar;
		dest.z = z * scalar;
		return this;
	}

	/**
	 * Multiply the components of this Vec3 by the given scalar values and store the result in <code>this</code>.
	 *
	 * @param x the x component to multiply this vector by
	 * @param y the y component to multiply this vector by
	 * @param z the z component to multiply this vector by
	 * @return this
	 */
	public Vec3 mul(double x, double y, double z) {
		this.x *= x;
		this.y *= y;
		this.z *= z;
		return this;
	}

	/**
	 * Multiply the components of this Vec3 by the given scalar values and store the result in <code>dest</code>.
	 *
	 * @param x    the x component to multiply this vector by
	 * @param y    the y component to multiply this vector by
	 * @param z    the z component to multiply this vector by
	 * @param dest will hold the result
	 * @return this
	 */
	public Vec3 mul(double x, double y, double z, Vec3 dest) {
		dest.x = this.x * x;
		dest.y = this.y * y;
		dest.z = this.z * z;
		return this;
	}

	/**
	 * Rotate this vector by the given quaternion <code>quat</code> and store the result in <code>this</code>.
	 *
	 * @param quat the quaternion to rotate this vector
	 * @return this
	 * @see Quat#transform(Vec3)
	 */
	public Vec3 rotate(Quat quat) {
		quat.transform(this, this);
		return this;
	}

	/**
	 * Rotate this vector by the given quaternion <code>quat</code> and store the result in <code>dest</code>.
	 *
	 * @param quat the quaternion to rotate this vector
	 * @param dest will hold the result
	 * @return this
	 * @see Quat#transform(Vec3)
	 */
	public Vec3 rotate(Quat quat, Vec3 dest) {
		quat.transform(this, dest);
		return this;
	}

	/**
	 * Divide this Vec3 by the given scalar value.
	 *
	 * @param scalar the scalar to divide this vector by
	 * @return this
	 */
	public Vec3 div(double scalar) {
		x /= scalar;
		y /= scalar;
		z /= scalar;
		return this;
	}

	/**
	 * Divide this Vec3 by the given scalar value and store the result in <code>dest</code>.
	 *
	 * @param scalar the scalar to divide this vector by
	 * @param dest   will hold the result
	 * @return this
	 */
	public Vec3 div(double scalar, Vec3 dest) {
		dest.x = x / scalar;
		dest.y = y / scalar;
		dest.z = z / scalar;
		return this;
	}

	/**
	 * Divide the components of this Vec3 by the given scalar values and store the result in <code>this</code>.
	 *
	 * @param x the x component to divide this vector by
	 * @param y the y component to divide this vector by
	 * @param z the z component to divide this vector by
	 * @return this
	 */
	public Vec3 div(double x, double y, double z) {
		this.x /= x;
		this.y /= y;
		this.z /= z;
		return this;
	}

	/**
	 * Divide the components of this Vec3 by the given scalar values and store the result in <code>dest</code>.
	 *
	 * @param x    the x component to divide this vector by
	 * @param y    the y component to divide this vector by
	 * @param z    the z component to divide this vector by
	 * @param dest will hold the result
	 * @return this
	 */
	public Vec3 div(double x, double y, double z, Vec3 dest) {
		dest.x = this.x / x;
		dest.y = this.y / y;
		dest.z = this.z / z;
		return this;
	}

	/**
	 * Return the length squared of this vector.
	 *
	 * @return the length squared
	 */
	public double lengthSquared() {
		return x * x + y * y + z * z;
	}

	/**
	 * Return the length of this vector.
	 *
	 * @return the length
	 */
	public double length() {
		return Math.sqrt(lengthSquared());
	}

	/**
	 * Normalize this vector.
	 *
	 * @return this
	 */
	public Vec3 normalize() {
		double d = length();
		x /= d;
		y /= d;
		z /= d;
		return this;
	}

	/**
	 * Normalize this vector and store the result in <code>dest</code>.
	 *
	 * @param dest will hold the result
	 * @return this
	 */
	public Vec3 normalize(Vec3 dest) {
		double d = length();
		dest.x = x / d;
		dest.y = y / d;
		dest.z = z / d;
		return this;
	}

	/**
	 * Set this vector to be the cross product of this and v2.
	 *
	 * @param v the other vector
	 * @return this
	 */
	public Vec3 cross(Vec3 v) {
		set(y * v.z - z * v.y, z * v.x - x * v.z, x * v.y - y * v.x);
		return this;
	}

	/**
	 * Set this vector to be the cross product of itself and <tt>(x, y, z)</tt>.
	 *
	 * @param x the x component of the other vector
	 * @param y the y component of the other vector
	 * @param z the z component of the other vector
	 * @return this
	 */
	public Vec3 cross(double x, double y, double z) {
		return set(this.y * z - this.z * y, this.z * x - this.x * z, this.x * y - this.y * x);
	}

	/**
	 * Calculate the cross product of this and v2 and store the result in <code>dest</code>.
	 *
	 * @param v    the other vector
	 * @param dest will hold the result
	 * @return this
	 */
	public Vec3 cross(Vec3 v, Vec3 dest) {
		dest.set(y * v.z - z * v.y, z * v.x - x * v.z, x * v.y - y * v.x);
		return this;
	}

	/**
	 * Compute the cross product of this vector and <tt>(x, y, z)</tt> and store the result in <code>dest</code>.
	 *
	 * @param x    the x component of the other vector
	 * @param y    the y component of the other vector
	 * @param z    the z component of the other vector
	 * @param dest will hold the result
	 * @return this
	 */
	public Vec3 cross(double x, double y, double z, Vec3 dest) {
		return dest.set(this.y * z - this.z * y, this.z * x - this.x * z, this.x * y - this.y * x);
	}

	/**
	 * Return the distance between this vector and <code>v</code>.
	 *
	 * @param v the other vector
	 * @return the distance
	 */
	public double distance(Vec3 v) {
		return Math.sqrt((v.x - this.x) * (v.x - this.x) + (v.y - this.y) * (v.y - this.y) + (v.z - this.z) * (v.z - this.z));
	}

	/**
	 * Return the distance between <code>this</code> vector and <tt>(x, y, z)</tt>.
	 *
	 * @param x the x component of the other vector
	 * @param y the y component of the other vector
	 * @param z the z component of the other vector
	 * @return the euclidean distance
	 */
	public double distance(double x, double y, double z) {
		return Math.sqrt((x - this.x) * (x - this.x) + (y - this.y) * (y - this.y) + (z - this.z) * (z - this.z));
	}

	/**
	 * Return the square of the distance between this vector and <code>v</code>.
	 *
	 * @param v the other vector
	 * @return the squared of the distance
	 */
	public double distanceSquared(Vec3 v) {
		return (v.x - this.x) * (v.x - this.x) + (v.y - this.y) * (v.y - this.y) + (v.z - this.z) * (v.z - this.z);
	}

	/**
	 * Return the square of the distance between <code>this</code> vector and <tt>(x, y, z)</tt>.
	 *
	 * @param x the x component of the other vector
	 * @param y the y component of the other vector
	 * @param z the z component of the other vector
	 * @return the square of the distance
	 */
	public double distanceSquared(double x, double y, double z) {
		return (x - this.x) * (x - this.x) + (y - this.y) * (y - this.y) + (z - this.z) * (z - this.z);
	}

	/**
	 * Return the dot product of this vector and the supplied vector.
	 *
	 * @param v the other vector
	 * @return the dot product
	 */
	public double dot(Vec3 v) {
		return x * v.x + y * v.y + z * v.z;
	}

	/**
	 * Return the dot product of this vector and the vector <tt>(x, y, z)</tt>.
	 *
	 * @param x the x component of the other vector
	 * @param y the y component of the other vector
	 * @param z the z component of the other vector
	 * @return the dot product
	 */
	public double dot(double x, double y, double z) {
		return this.x * x + this.y * y + this.z * z;
	}

	/**
	 * Return the cosine of the angle between <code>this</code> vector and the supplied vector. Use this instead of <tt>Math.cos(angle(v))</tt>.
	 *
	 * @param v the other vector
	 * @return the cosine of the angle
	 * @see #angle(Vec3)
	 */
	public double angleCos(Vec3 v) {
		double length1 = Math.sqrt(x * x + y * y + z * z);
		double length2 = Math.sqrt(v.x * v.x + v.y * v.y + v.z * v.z);
		double dot = x * v.x + y * v.y + z * v.z;
		return dot / (length1 * length2);
	}

	/**
	 * Return the angle between this vector and the supplied vector.
	 *
	 * @param v the other vector
	 * @return the angle, in radians
	 * @see #angleCos(Vec3)
	 */
	public double angle(Vec3 v) {
		double cos = angleCos(v);
		// This is because sometimes cos goes above 1 or below -1 because of lost precision
		cos = Math.min(cos, 1);
		cos = Math.max(cos, -1);
		return Math.acos(cos);
	}

	/**
	 * Set all components to zero.
	 *
	 * @return this
	 */
	public Vec3 zero() {
		this.x = 0.0;
		this.y = 0.0;
		this.z = 0.0;
		return this;
	}

	/**
	 * Return a string representation of this vector.
	 * <p>
	 * This method creates a new {@link DecimalFormat} on every invocation with the format string "<tt> 0.000E0;-</tt>".
	 *
	 * @return the string representation
	 */
	public String toString() {
		DecimalFormat formatter = new DecimalFormat(" 0.000E0;-"); //$NON-NLS-1$
		return toString(formatter).replaceAll("E(\\d+)", "E+$1"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Return a string representation of this vector by formatting the vector components with the given {@link NumberFormat}.
	 *
	 * @param formatter the {@link NumberFormat} used to format the vector components with
	 * @return the string representation
	 */
	public String toString(NumberFormat formatter) {
		return "(" + formatter.format(x) + " " + formatter.format(y) + " " + formatter.format(z) + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	}

	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeDouble(x);
		out.writeDouble(y);
		out.writeDouble(z);
	}

	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		x = in.readDouble();
		y = in.readDouble();
		z = in.readDouble();
	}

	/**
	 * Negate this vector.
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
	 * Negate this vector and store the result in <code>dest</code>.
	 *
	 * @param dest will hold the result
	 * @return this
	 */
	public Vec3 negate(Vec3 dest) {
		dest.x = -x;
		dest.y = -y;
		dest.z = -z;
		return this;
	}

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(x);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(y);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(z);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		Vec3 other = (Vec3) obj;
		if (Double.doubleToLongBits(x) != Double.doubleToLongBits(other.x)) return false;
		if (Double.doubleToLongBits(y) != Double.doubleToLongBits(other.y)) return false;
		if (Double.doubleToLongBits(z) != Double.doubleToLongBits(other.z)) return false;
		return true;
	}

	/**
	 * Reflect this vector about the given normal vector.
	 *
	 * @param normal the vector to reflect about
	 * @return this
	 */
	public Vec3 reflect(Vec3 normal) {
		double dot = this.dot(normal);
		x = x - 2.0 * dot * normal.x;
		y = y - 2.0 * dot * normal.y;
		z = z - 2.0 * dot * normal.z;
		return this;
	}

	/**
	 * Reflect this vector about the given normal vector.
	 *
	 * @param x the x component of the normal
	 * @param y the y component of the normal
	 * @param z the z component of the normal
	 * @return this
	 */
	public Vec3 reflect(double x, double y, double z) {
		double dot = this.dot(x, y, z);
		this.x = this.x - 2.0 * dot * x;
		this.y = this.y - 2.0 * dot * y;
		this.z = this.z - 2.0 * dot * z;
		return this;
	}

	/**
	 * Reflect this vector about the given normal vector and store the result in <code>dest</code>.
	 *
	 * @param normal the vector to reflect about
	 * @param dest   will hold the result
	 * @return this
	 */
	public Vec3 reflect(Vec3 normal, Vec3 dest) {
		double dot = this.dot(normal);
		dest.x = x - 2.0 * dot * normal.x;
		dest.y = y - 2.0 * dot * normal.y;
		dest.z = z - 2.0 * dot * normal.z;
		return this;
	}

	/**
	 * Reflect this vector about the given normal vector and store the result in <code>dest</code>.
	 *
	 * @param x    the x component of the normal
	 * @param y    the y component of the normal
	 * @param z    the z component of the normal
	 * @param dest will hold the result
	 * @return this
	 */
	public Vec3 reflect(double x, double y, double z, Vec3 dest) {
		double dot = this.dot(x, y, z);
		dest.x = this.x - 2.0 * dot * x;
		dest.y = this.y - 2.0 * dot * y;
		dest.z = this.z - 2.0 * dot * z;
		return this;
	}

	/**
	 * Compute the half vector between this and the other vector.
	 *
	 * @param other the other vector
	 * @return this
	 */
	public Vec3 half(Vec3 other) {
		return this.add(other)
			   .normalize();
	}

	/**
	 * Compute the half vector between this and the vector <tt>(x, y, z)</tt>.
	 *
	 * @param x the x component of the other vector
	 * @param y the y component of the other vector
	 * @param z the z component of the other vector
	 * @return this
	 */
	public Vec3 half(double x, double y, double z) {
		return this.add(x, y, z)
			   .normalize();
	}

	/**
	 * Compute the half vector between this and the other vector and store the result in <code>dest</code>.
	 *
	 * @param other the other vector
	 * @param dest  will hold the result
	 * @return this
	 */
	public Vec3 half(Vec3 other, Vec3 dest) {
		dest.set(this)
		    .add(other)
		    .normalize();
		return this;
	}

	/**
	 * Compute the half vector between this and the vector <tt>(x, y, z)</tt> and store the result in <code>dest</code>.
	 *
	 * @param x    the x component of the other vector
	 * @param y    the y component of the other vector
	 * @param z    the z component of the other vector
	 * @param dest will hold the result
	 * @return this
	 */
	public Vec3 half(double x, double y, double z, Vec3 dest) {
		dest.set(this)
		    .add(x, y, z)
		    .normalize();
		return this;
	}

	/**
	 * Compute a smooth-step (i.e. hermite with zero tangents) interpolation between <code>this</code> vector and the given vector <code>v</code> and store the result in <code>dest</code>.
	 *
	 * @param v    the other vector
	 * @param t    the interpolation factor, within <tt>[0..1]</tt>
	 * @param dest will hold the result
	 * @return this
	 */
	public Vec3 smoothStep(Vec3 v, double t, Vec3 dest) {
		dest.x = Interpolate.smoothStep(x, v.x, t);
		dest.y = Interpolate.smoothStep(y, v.y, t);
		dest.z = Interpolate.smoothStep(x, v.z, t);
		return this;
	}

	/**
	 * Compute a hermite interpolation between <code>this</code> vector and its associated tangent <code>t0</code> and the given vector <code>v</code> with its tangent <code>t1</code> and store
	 * the result in <code>dest</code>.
	 *
	 * @param t0   the tangent of <code>this</code> vector
	 * @param v1   the other vector
	 * @param t1   the tangent of the other vector
	 * @param t    the interpolation factor, within <tt>[0..1]</tt>
	 * @param dest will hold the result
	 * @return this
	 */
	public Vec3 hermite(Vec3 t0, Vec3 v1, Vec3 t1, double t, Vec3 dest) {
		dest.x = Interpolate.hermite(x, t0.x, v1.x, t1.x, t);
		dest.y = Interpolate.hermite(y, t0.y, v1.y, t1.y, t);
		dest.z = Interpolate.hermite(z, t0.z, v1.z, t1.z, t);
		return this;
	}

	/**
	 * Linearly interpolate <code>this</code> and <code>other</code> using the given interpolation factor <code>t</code> and store the result in <code>this</code>.
	 * <p>
	 * If <code>t</code> is <tt>0.0</tt> then the result is <code>this</code>. If the interpolation factor is <code>1.0</code> then the result is <code>other</code>.
	 *
	 * @param other the other vector
	 * @param t     the interpolation factor between 0.0 and 1.0.
	 * @return this
	 */
	public Vec3 lerp(Vec3 other, double t) {
		return lerp(this, other, t);
	}

	/**
	 * Linearly interpolate <code>a</code> and <code>b</code> using the given interpolation factor <code>t</code> and store the result in <code>this</code>.
	 * <p>
	 * If <code>t</code> is <tt>0.0</tt> then the result is <code>this</code>. If the interpolation factor is <code>1.0</code> then the result is <code>other</code>.
	 *
	 * @param a the first vector
	 * @param b the second vector
	 * @param t     the interpolation factor between 0.0 and 1.0
	 * @return this
	 */
	public Vec3 lerp(Vec3 a, Vec3 b, double t) {
		x = (1.0 - t) * a.x + t * b.x;
		y = (1.0 - t) * a.y + t * b.y;
		z = (1.0 - t) * a.z + t * b.z;
		return this;
	}

	/**
	 * Converts this Vec3 to a Vec4 (with w=0)
	 *
	 * @return Vec4(this.x, this.y, this.z, 0)
	 */
	public Vec4 toVec4() {
		return new Vec4(this.x, this.y, this.z, 0);
	}

	/**
	 * Converts this Vec3 to a Vec2
	 *
	 * @return Vec2(this.x, this.y)
	 */
	public Vec2 toVec2() {
		return new Vec2(this.x, this.y);
	}

	/**
	 * returns true if any component of this Double.isNaN
	 *
	 * @return is any component NaN ?
	 */
	public boolean isNaN() {
		return Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(z);
	}

	/**
	 * returns an independent copy of this object
	 */
	public Vec3 duplicate() {
		return new Vec3(this);
	}

	/**
	 * Sets this to be the component-wise minimum of this and another vector
	 *
	 * @param a the other vector
	 * @return this
	 */
	public Vec3 min(Vec3 a) {
		this.x = Math.min(a.x, this.x);
		this.y = Math.min(a.y, this.y);
		this.z = Math.min(a.z, this.z);
		return this;
	}

	/**
	 * Sets this to be the component-wise maximum of this and another vector
	 *
	 * @param a the other vector
	 * @return this
	 */
	public Vec3 max(Vec3 a) {
		this.x = Math.max(a.x, this.x);
		this.y = Math.max(a.y, this.y);
		this.z = Math.max(a.z, this.z);
		return this;
	}

	/**
	 * a Vec3 is a Supplier of type Vec3
	 *
	 * @return this
	 */
	public Vec3 get() {
		return this;
	}

	/**
	 * sets an element (0,1 or 2) of this vector to the specified value
	 *
	 * @param index the index
	 * @param value the value
	 * @return this
	 */
	public Vec3 set(int index, double value) {
		switch (index) {
			case 0:
				this.x = value;
				return this;
			case 1:
				this.y = value;
				return this;
			case 2:
				this.z = value;
				return this;
			default:
				throw new IndexOutOfBoundsException("" + index);
		}
	}

	/**
	 * get an element (0,1 or 2) of this vector to the specified value
	 *
	 * @param index the index
	 * @return this
	 */
	public double element(int index) {
		switch (index) {
			case 0:
				return this.x;
			case 1:
				return this.y;
			case 2:
				return this.z;
			default:
				throw new IndexOutOfBoundsException("" + index);
		}
	}

	/**
	 * returns the x component of this Vector
	 *
	 * @return x
	 */
	public double x() {
		return x;
	}

	/**
	 * sets the xy components of this Vector
	 *
	 * @return this
	 */
	public Vec3 xy(Vec2 v) {
		this.x = v.x;
		this.y = v.y;
		return this;
	}

	/**
	 * returns the xy components of this Vector
	 *
	 * @return xy
	 */
	public Vec2 xy() {
		return new Vec2(x, y);
	}

	/**
	 * sets the xz components of this Vector
	 *
	 * @return this
	 */
	public Vec3 xz(Vec2 v) {
		this.x = v.x;
		this.z = v.y;
		return this;
	}

	/**
	 * returns the xz components of this Vector
	 *
	 * @return xz
	 */
	public Vec2 xz() {
		return new Vec2(x, z);
	}

	/**
	 * sets the yz components of this Vector
	 *
	 * @return this
	 */
	public Vec3 yz(Vec2 v) {
		this.y = v.x;
		this.z = v.y;
		return this;
	}

	/**
	 * returns the xz components of this Vector
	 *
	 * @return xz
	 */
	public Vec2 yz() {
		return new Vec2(y, z);
	}


	/**
	 * sets the x component of this Vector
	 *
	 * @return this
	 */
	public Vec3 x(double v) {
		this.x = v;
		return this;
	}

	/**
	 * returns the y component of this Vector
	 *
	 * @return y
	 */
	public double y() {
		return y;
	}

	/**
	 * sets the y component of this Vector
	 *
	 * @return this
	 */
	public Vec3 y(double v) {
		this.y = v;
		return this;
	}

	/**
	 * returns the y component of this Vector
	 *
	 * @return y
	 */
	public double z() {
		return z;
	}

	/**
	 * sets the y component of this Vector
	 *
	 * @return this
	 */
	public Vec3 z(double v) {
		this.z = v;
		return this;
	}


}
