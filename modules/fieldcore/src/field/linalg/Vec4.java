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
import fieldnashorn.annotations.SafeToToString;

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
 * Contains the definition of a Vector comprising 4 doubles and associated transformations.
 *
 * @author Richard Greenlees
 * @author Kai Burjack
 */
public class Vec4 implements Externalizable, Supplier<Vec4>, Mutable {

	private static final long serialVersionUID = 1L;

	/**
	 * The x component of the vector.
	 */
	@SafeToToString
	public double x;
	/**
	 * The y component of the vector.
	 */
	@SafeToToString
	public double y;
	/**
	 * The z component of the vector.
	 */
	@SafeToToString
	public double z;
	/**
	 * The w component of the vector.
	 */
	@SafeToToString
	public double w = 1.0;

	/**
	 * Create a new {@link Vec4} of <code>(0, 0, 0, 1)</code>.
	 */
	public Vec4() {
	}

	/**
	 * Create a new {@link Vec4} with the same values as <code>v</code>.
	 *
	 * @param v the {@link Vec4} to copy the values from
	 */
	public Vec4(Vec4 v) {
		this.x = v.x;
		this.y = v.y;
		this.z = v.z;
		this.w = v.w;
	}

	/**
	 * Create a new {@link Vec4} with the first three components from the given <code>v</code> and the given <code>w</code>.
	 *
	 * @param v the {@link Vec3}
	 * @param w the w component
	 */
	public Vec4(Vec3 v, double w) {
		this.x = v.x;
		this.y = v.y;
		this.z = v.z;
		this.w = w;
	}

	/**
	 * Create a new {@link Vec4} with the first two components from the given <code>v</code> and the given <code>z</code> and <code>w</code>.
	 *
	 * @param v the {@link Vec2}
	 * @param z the z component
	 * @param w the w component
	 */
	public Vec4(Vec2 v, double z, double w) {
		this.x = v.x;
		this.y = v.y;
		this.z = z;
		this.w = w;
	}

	/**
	 * Create a new {@link Vec4} and initialize all four components with the given value.
	 *
	 * @param d the value of all four components
	 */
	public Vec4(double d) {
		this(d, d, d, d);
	}

	/**
	 * Create a new {@link Vec4} with the given component values.
	 *
	 * @param x the x component
	 * @param y the y component
	 * @param z the z component
	 * @param w the w component
	 */
	public Vec4(double x, double y, double z, double w) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.w = w;
	}

	/**
	 * Create a new {@link Vec4} and read this vector from the supplied {@link ByteBuffer} at the current buffer {@link ByteBuffer#position() position}.
	 * <p>
	 * This method will not increment the position of the given ByteBuffer.
	 * <p>
	 * If you want to specify the offset into the ByteBuffer at which the vector is read, you can use {@link #Vec4(int, ByteBuffer)}, taking the absolute position as parameter.
	 *
	 * @param buffer values will be read in <tt>x, y, z, w</tt> order
	 * @see #Vec4(int, ByteBuffer)
	 */
	public Vec4(ByteBuffer buffer) {
		this(buffer.position(), buffer);
		buffer.position(buffer.position()+8*4);
	}

	/**
	 * Create a new {@link Vec4} and read this vector from the supplied {@link ByteBuffer} starting at the specified absolute buffer position/index.
	 * <p>
	 * This method will not increment the position of the given ByteBuffer.
	 *
	 * @param index  the absolute position into the ByteBuffer
	 * @param buffer values will be read in <tt>x, y, z, w</tt> order
	 */
	public Vec4(int index, ByteBuffer buffer) {
		x = buffer.getDouble(index);
		y = buffer.getDouble(index + 8);
		z = buffer.getDouble(index + 16);
		w = buffer.getDouble(index + 24);
	}

	/**
	 * Create a new {@link Vec4} and read this vector from the supplied {@link DoubleBuffer} at the current buffer {@link DoubleBuffer#position() position}.
	 * <p>
	 * This method will not increment the position of the given DoubleBuffer.
	 * <p>
	 * If you want to specify the offset into the DoubleBuffer at which the vector is read, you can use {@link #Vec4(int, DoubleBuffer)}, taking the absolute position as parameter.
	 *
	 * @param buffer values will be read in <tt>x, y, z, w</tt> order
	 * @see #Vec4(int, DoubleBuffer)
	 */
	public Vec4(DoubleBuffer buffer) {
		this(buffer.position(), buffer);
		buffer.position(buffer.position()+4);
	}

	/**
	 * Create a new {@link Vec4} and read this vector from the supplied {@link DoubleBuffer} starting at the specified absolute buffer position/index.
	 * <p>
	 * This method will not increment the position of the given DoubleBuffer.
	 *
	 * @param index  the absolute position into the DoubleBuffer
	 * @param buffer values will be read in <tt>x, y, z, w</tt> order
	 */
	public Vec4(int index, DoubleBuffer buffer) {
		x = buffer.get(index);
		y = buffer.get(index + 1);
		z = buffer.get(index + 2);
		w = buffer.get(index + 3);
	}

	/**
	 * Create a new {@link Vec4} and read this vector from the supplied {@link FloatBuffer} at the current buffer {@link FloatBuffer#position() position}.
	 * <p>
	 * This method will not increment the position of the given FloatBuffer.
	 * <p>
	 * If you want to specify the offset into the FloatBuffer at which the vector is read, you can use {@link #Vec4(int, FloatBuffer)}, taking the absolute position as parameter.
	 *
	 * @param buffer values will be read in <tt>x, y, z, w</tt> order
	 * @see #Vec4(int, FloatBuffer)
	 */
	public Vec4(FloatBuffer buffer) {
		this(buffer.position(), buffer);
		buffer.position(buffer.position()+4);
	}

	/**
	 * Create a new {@link Vec4} and read this vector from the supplied {@link FloatBuffer} starting at the specified absolute buffer position/index.
	 * <p>
	 * This method will not increment the position of the given FloatBuffer.
	 *
	 * @param index  the absolute position into the FloatBuffer
	 * @param buffer values will be read in <tt>x, y, z, w</tt> order
	 */
	public Vec4(int index, FloatBuffer buffer) {
		x = buffer.get(index);
		y = buffer.get(index + 1);
		z = buffer.get(index + 2);
		w = buffer.get(index + 3);
	}

	/**
	 * Subtract <code>v2</code> from <code>v1</code> and store the result in <code>dest</code>.
	 *
	 * @param v1   the left operand
	 * @param v2   the right operand
	 * @param dest will hold the result (automatically allocated if null)
	 * @return dest
	 */
	public static Vec4 sub(Vec4 v1, Vec4 v2, Vec4 dest) {
		if (dest == null) dest = new Vec4();
		dest.x = v1.x - v2.x;
		dest.y = v1.y - v2.y;
		dest.z = v1.z - v2.z;
		dest.w = v1.w - v2.w;
		return dest;
	}

	/**
	 * Add <code>v2</code> to <code>v1</code> and store the result in <code>dest</code>.
	 *
	 * @param v1   the first addend
	 * @param v2   the second addend
	 * @param dest will hold the result
	 */
	public static Vec4 add(Vec4 v1, Vec4 v2, Vec4 dest) {
		if (dest == null) dest = new Vec4();
		dest.x = v1.x + v2.x;
		dest.y = v1.y + v2.y;
		dest.z = v1.z + v2.z;
		dest.w = v1.w + v2.w;
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
	public static Vec4 fma(Vec4 x, float w, Vec4 y, Vec4 dest) {

		if (dest == null) dest = new Vec4();
		dest.x = x.x * w + y.x;
		dest.y = x.y * w + y.y;
		dest.z = x.z * w + y.z;
		dest.w = x.w * w + y.w;

		return dest;
	}

	/**
	 * Return the dot product of a vector and another.
	 *
	 * @param a the first vector
	 * @param v the other vector
	 * @return the dot product
	 */
	public static double dot(Vec4 a, Vec4 v) {
		return a.x * v.x + a.y * v.y + a.z * v.z + a.w * v.w;
	}

	/**
	 * Set this {@link Vec4} to the values of the given <code>v</code>.
	 *
	 * @param v the vector whose values will be copied into this
	 * @return this
	 */
	public Vec4 set(Vec4 v) {
		this.x = v.x;
		this.y = v.y;
		this.z = v.z;
		this.w = v.w;
		return this;
	}

	/**
	 * Set the x, y, and z components of this to the components of <code>v</code> and the w component to <code>w</code>.
	 *
	 * @param v the {@link Vec3} to copy
	 * @param w the w component
	 * @return this
	 */
	public Vec4 set(Vec3 v, double w) {
		this.x = v.x;
		this.y = v.y;
		this.z = v.z;
		this.w = w;
		return this;
	}

	/**
	 * Set the x and y components from the given <code>v</code> and the z and w components to the given <code>z</code> and <code>w</code>.
	 *
	 * @param v the {@link Vec2}
	 * @param z the z component
	 * @param w the w component
	 * @return this
	 */
	public Vec4 set(Vec2 v, double z, double w) {
		this.x = v.x;
		this.y = v.y;
		this.z = z;
		this.w = w;
		return this;
	}

	/**
	 * Set the x, y, z, and w components to the supplied value.
	 *
	 * @param d the value of all four components
	 * @return this
	 */
	public Vec4 set(double d) {
		return set(d, d, d, d);
	}

	/**
	 * Set the x, y, z, and w components to the supplied values.
	 *
	 * @param x the x component
	 * @param y the y component
	 * @param z the z component
	 * @param w the w component
	 * @return this
	 */
	public Vec4 set(double x, double y, double z, double w) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.w = w;
		return this;
	}

	/**
	 * Read this vector from the supplied {@link ByteBuffer} at the current buffer {@link ByteBuffer#position() position}.
	 * <p>
	 * This method will not increment the position of the given ByteBuffer.
	 * <p>
	 * If you want to specify the offset into the ByteBuffer at which the vector is read, you can use {@link #set(int, ByteBuffer)}, taking the absolute position as parameter.
	 *
	 * @param buffer values will be read in <tt>x, y, z, w</tt> order
	 * @return this
	 * @see #set(int, ByteBuffer)
	 */
	public Vec4 set(ByteBuffer buffer) {
		return set(buffer.position(), buffer);
	}

	/**
	 * Read this vector from the supplied {@link ByteBuffer} starting at the specified absolute buffer position/index.
	 * <p>
	 * This method will not increment the position of the given ByteBuffer.
	 *
	 * @param index  the absolute position into the ByteBuffer
	 * @param buffer values will be read in <tt>x, y, z, w</tt> order
	 * @return this
	 */
	public Vec4 set(int index, ByteBuffer buffer) {
		x = buffer.getDouble(index);
		y = buffer.getDouble(index + 8);
		z = buffer.getDouble(index + 16);
		w = buffer.getDouble(index + 24);
		return this;
	}

	/**
	 * Read this vector from the supplied {@link DoubleBuffer} at the current buffer {@link DoubleBuffer#position() position}.
	 * <p>
	 * This method will not increment the position of the given DoubleBuffer.
	 * <p>
	 * If you want to specify the offset into the DoubleBuffer at which the vector is read, you can use {@link #set(int, DoubleBuffer)}, taking the absolute position as parameter.
	 *
	 * @param buffer values will be read in <tt>x, y, z, w</tt> order
	 * @return this
	 * @see #set(int, DoubleBuffer)
	 */
	public Vec4 set(DoubleBuffer buffer) {
		return set(buffer.position(), buffer);
	}

	/**
	 * Read this vector from the supplied {@link DoubleBuffer} starting at the specified absolute buffer position/index.
	 * <p>
	 * This method will not increment the position of the given DoubleBuffer.
	 *
	 * @param index  the absolute position into the DoubleBuffer
	 * @param buffer values will be read in <tt>x, y, z, w</tt> order
	 * @return this
	 */
	public Vec4 set(int index, DoubleBuffer buffer) {
		x = buffer.get(index);
		y = buffer.get(index + 1);
		z = buffer.get(index + 2);
		w = buffer.get(index + 3);
		return this;
	}

	/**
	 * Store this vector into the supplied {@link ByteBuffer} at the current buffer {@link ByteBuffer#position() position}.
	 * <p>
	 * This method will not increment the position of the given ByteBuffer.
	 * <p>
	 * If you want to specify the offset into the ByteBuffer at which the vector is stored, you can use {@link #get(int, ByteBuffer)}, taking the absolute position as parameter.
	 *
	 * @param buffer will receive the values of this vector in <tt>x, y, z, w</tt> order
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
	 * @param buffer will receive the values of this vector in <tt>x, y, z, w</tt> order
	 * @return the passed in buffer
	 */
	public ByteBuffer get(int index, ByteBuffer buffer) {
		buffer.putDouble(index, x);
		buffer.putDouble(index + 8, y);
		buffer.putDouble(index + 16, z);
		buffer.putDouble(index + 24, w);
		return buffer;
	}

	/**
	 * Store this vector into the supplied {@link DoubleBuffer} at the current buffer {@link DoubleBuffer#position() position}.
	 * <p>
	 * This method will not increment the position of the given DoubleBuffer.
	 * <p>
	 * If you want to specify the offset into the DoubleBuffer at which the vector is stored, you can use {@link #get(int, DoubleBuffer)}, taking the absolute position as parameter.
	 *
	 * @param buffer will receive the values of this vector in <tt>x, y, z, w</tt> order
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
	 * @param buffer will receive the values of this vector in <tt>x, y, z, w</tt> order
	 * @return the passed in buffer
	 */
	public DoubleBuffer get(int index, DoubleBuffer buffer) {
		buffer.put(index, x);
		buffer.put(index + 1, y);
		buffer.put(index + 2, z);
		buffer.put(index + 3, w);
		return buffer;
	}

	/**
	 * Subtract the supplied vector from this one.
	 *
	 * @param v the vector to subtract
	 * @return this
	 */
	public Vec4 sub(Vec4 v) {
		x -= v.x;
		y -= v.y;
		z -= v.z;
		w -= v.w;
		return this;
	}

	/**
	 * Subtract <tt>(x, y, z, w)</tt> from this.
	 *
	 * @param x the x component to subtract
	 * @param y the y component to subtract
	 * @param z the z component to subtract
	 * @param w the w component to subtract
	 * @return this
	 */
	public Vec4 sub(double x, double y, double z, double w) {
		this.x -= x;
		this.y -= y;
		this.z -= z;
		this.w -= w;
		return this;
	}

	/**
	 * Subtract <tt>(x, y, z, w)</tt> from this and store the result in <code>dest</code>.
	 *
	 * @param x    the x component to subtract
	 * @param y    the y component to subtract
	 * @param z    the z component to subtract
	 * @param w    the w component to subtract
	 * @param dest will hold the result
	 * @return this
	 */
	public Vec4 sub(double x, double y, double z, double w, Vec4 dest) {
		dest.x = this.x - x;
		dest.y = this.y - y;
		dest.z = this.z - z;
		dest.w = this.w - w;
		return this;
	}

	/**
	 * Add <tt>(x, y, z, w)</tt> to this.
	 *
	 * @param x the x component to subtract
	 * @param y the y component to subtract
	 * @param z the z component to subtract
	 * @param w the w component to subtract
	 * @return this
	 */
	public Vec4 add(double x, double y, double z, double w) {
		this.x += x;
		this.y += y;
		this.z += z;
		this.w += w;
		return this;
	}

	/**
	 * Add <tt>(x, y, z, w)</tt> to this and store the result in <code>dest</code>.
	 *
	 * @param x    the x component to subtract
	 * @param y    the y component to subtract
	 * @param z    the z component to subtract
	 * @param w    the w component to subtract
	 * @param dest will hold the result
	 * @return this
	 */
	public Vec4 add(double x, double y, double z, double w, Vec4 dest) {
		dest.x = this.x - x;
		dest.y = this.y - y;
		dest.z = this.z - z;
		dest.w = this.w - w;
		return this;
	}

	/**
	 * Add the supplied vector to this one.
	 *
	 * @param v the vector to add
	 * @return this
	 */
	public Vec4 add(Vec4 v) {
		x += v.x;
		y += v.y;
		z += v.z;
		w += v.w;
		return this;
	}

	/**
	 * Add the component-wise multiplication of <code>a * b</code> to this vector.
	 *
	 * @param a the first multiplicand
	 * @param b the second multiplicand
	 * @return this
	 */
	public Vec4 fma(Vec4 a, Vec4 b) {
		x += a.x * b.x;
		y += a.y * b.y;
		z += a.z * b.z;
		w += a.w * b.w;
		return this;
	}

	/**
	 * Add the component-wise multiplication of <code>a * b</code> to this vector.
	 *
	 * @param a the first multiplicand
	 * @param b the second multiplicand
	 * @return this
	 */
	public Vec4 fma(double a, Vec4 b) {
		x += a * b.x;
		y += a * b.y;
		z += a * b.z;
		w += a * b.w;
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
	public Vec4 fma(Vec4 a, Vec4 b, Vec4 dest) {
		dest.x = x + a.x * b.x;
		dest.y = y + a.y * b.y;
		dest.z = z + a.z * b.z;
		dest.w = w + a.w * b.w;
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
	public Vec4 fma(double a, Vec4 b, Vec4 dest) {
		dest.x = x + a * b.x;
		dest.y = y + a * b.y;
		dest.z = z + a * b.z;
		dest.w = w + a * b.w;
		return this;
	}

	/**
	 * Multiply this {@link Vec4} component-wise by the given {@link Vec4} and store the result in <code>dest</code>.
	 *
	 * @param v    the vector to multiply this by
	 * @param dest will hold the result
	 * @return this
	 */
	public Vec4 mul(Vec4 v, Vec4 dest) {
		dest.x = x * v.x;
		dest.y = y * v.y;
		dest.z = z * v.z;
		dest.w = w * v.w;
		return this;
	}

	/**
	 * Divide this {@link Vec4} component-wise by the given {@link Vec4}.
	 *
	 * @param v the vector to divide by
	 * @return this
	 */
	public Vec4 div(Vec4 v) {
		x /= v.x;
		y /= v.y;
		z /= v.z;
		z /= v.w;
		return this;
	}

	/**
	 * Divide this {@link Vec4} component-wise by the given {@link Vec4} and store the result in <code>dest</code>.
	 *
	 * @param v    the vector to divide this by
	 * @param dest will hold the result
	 * @return this
	 */
	public Vec4 div(Vec4 v, Vec4 dest) {
		dest.x = x / v.x;
		dest.y = y / v.y;
		dest.z = z / v.z;
		dest.w = w / v.w;
		return this;
	}

	/**
	 * Multiply this {@link Vec4} component-wise by the given {@link Vec4}.
	 *
	 * @param v the vector to multiply by
	 * @return this
	 */
	public Vec4 mul(Vec4 v) {
		x *= v.x;
		y *= v.y;
		z *= v.z;
		z *= v.w;
		return this;
	}

	/**
	 * Multiply this {@link Vec4} by the given matrix <code>mat</code>.
	 *
	 * @param mat the matrix to multiply by
	 * @return this
	 */
	public Vec4 mul(Mat4 mat) {
		return mul(mat, this);
	}

	/**
	 * Multiply this {@link Vec4} by the given matrix mat and store the result in <code>dest</code>.
	 *
	 * @param mat  the matrix to multiply <code>this</code> by
	 * @param dest will hold the result
	 * @return this
	 */
	public Vec4 mul(Mat4 mat, Vec4 dest) {
		if (this != dest) {
			dest.x = mat.m00 * x + mat.m10 * y + mat.m20 * z + mat.m30 * w;
			dest.y = mat.m01 * x + mat.m11 * y + mat.m21 * z + mat.m31 * w;
			dest.z = mat.m02 * x + mat.m12 * y + mat.m22 * z + mat.m32 * w;
			dest.w = mat.m03 * x + mat.m13 * y + mat.m23 * z + mat.m33 * w;
		} else {
			dest.set(mat.m00 * x + mat.m10 * y + mat.m20 * z + mat.m30 * w, mat.m01 * x + mat.m11 * y + mat.m21 * z + mat.m31 * w, mat.m02 * x + mat.m12 * y + mat.m22 * z + mat.m32 * w,
				 mat.m03 * x + mat.m13 * y + mat.m23 * z + mat.m33 * w);
		}
		return this;
	}

	/**
	 * Multiply this Vec4 by the given scalar value.
	 *
	 * @param scalar the scalar to multiply by
	 * @return this
	 */
	public Vec4 mul(double scalar) {
		x *= scalar;
		y *= scalar;
		z *= scalar;
		w *= scalar;
		return this;
	}

	/**
	 * Multiply this Vec4 by the given scalar value and store the result in <code>dest</code>.
	 *
	 * @param scalar the factor to multiply by
	 * @param dest   will hold the result
	 * @return this
	 */
	public Vec4 mul(double scalar, Vec4 dest) {
		x *= scalar;
		y *= scalar;
		z *= scalar;
		w *= scalar;
		return this;
	}

	/**
	 * Divide this Vec4 by the given scalar value.
	 *
	 * @param scalar the scalar to divide by
	 * @return this
	 */
	public Vec4 div(double scalar) {
		x /= scalar;
		y /= scalar;
		z /= scalar;
		w /= scalar;
		return this;
	}

	/**
	 * Divide this Vec4 by the given scalar value and store the result in <code>dest</code>.
	 *
	 * @param scalar the factor to divide by
	 * @param dest   will hold the result
	 * @return this
	 */
	public Vec4 div(double scalar, Vec4 dest) {
		x /= scalar;
		y /= scalar;
		z /= scalar;
		w /= scalar;
		return this;
	}

	/**
	 * Transform this vector by the given quaternion <code>quat</code> and store the result in <code>this</code>.
	 *
	 * @param quat the quaternion to transform this vector
	 * @return this
	 * @see Quat#transform(Vec4)
	 */
	public Vec4 rotate(Quat quat) {
		return rotate(quat, this);
	}

	/**
	 * Transform this vector by the given quaternion <code>quat</code> and store the result in <code>dest</code>.
	 *
	 * @param quat the quaternion to transform this vector
	 * @param dest will hold the result
	 * @return this
	 * @see Quat#transform(Vec4)
	 */
	public Vec4 rotate(Quat quat, Vec4 dest) {
		quat.transform(this, dest);
		return this;
	}

	/**
	 * Return the length squared of this vector.
	 *
	 * @return the length squared
	 */
	public double lengthSquared() {
		return x * x + y * y + z * z + w * w;
	}

	/**
	 * Return the length of this vector.
	 *
	 * @return the length
	 */
	@SafeToToString
	public double length() {
		return Math.sqrt(lengthSquared());
	}

	/**
	 * Normalizes this vector.
	 *
	 * @return this
	 */
	public Vec4 normalize() {
		double d = length();
		x /= d;
		y /= d;
		z /= d;
		w /= d;
		return this;
	}

	/**
	 * Normalizes this vector and store the result in <code>dest</code>.
	 *
	 * @param dest will hold the result
	 * @return this
	 */
	public Vec4 normalize(Vec4 dest) {
		double d = length();
		dest.x = x / d;
		dest.y = y / d;
		dest.z = z / d;
		dest.w = w / d;
		return this;
	}

	/**
	 * Normalize this vector by computing only the norm of <tt>(x, y, z)</tt>.
	 *
	 * @return this
	 */
	public Vec4 normalize3() {
		double d = Math.sqrt(x * x + y * y + z * z);
		x /= d;
		y /= d;
		z /= d;
		w /= d;
		return this;
	}

	/**
	 * Normalize this vector by computing only the norm of <tt>(x, y, z)</tt> and store the result in <code>dest</code>.
	 *
	 * @param dest will hold the result
	 * @return this
	 */
	public Vec4 normalize3(Vec4 dest) {
		double d = Math.sqrt(x * x + y * y + z * z);
		dest.x = x / d;
		dest.y = y / d;
		dest.z = z / d;
		dest.w = w / d;
		return this;
	}

	/**
	 * Return the distance between <code>this</code> vector and <code>v</code>.
	 *
	 * @param v the other vector
	 * @return the euclidean distance
	 */
	public double distance(Vec4 v) {
		return Math.sqrt((v.x - x) * (v.x - x) + (v.y - y) * (v.y - y) + (v.z - z) * (v.z - z) + (v.w - w) * (v.w - w));
	}

	/**
	 * Return the distance between <code>this</code> vector and <tt>(x, y, z, w)</tt>.
	 *
	 * @param x the x component of the other vector
	 * @param y the y component of the other vector
	 * @param z the z component of the other vector
	 * @param w the w component of the other vector
	 * @return the euclidean distance
	 */
	public double distance(double x, double y, double z, double w) {
		return Math.sqrt((x - this.x) * (x - this.x) + (y - this.y) * (y - this.y) + (z - this.z) * (z - this.z) + (w - this.w) * (w - this.w));
	}

	/**
	 * Compute the dot product (inner product) of this vector and <code>v</code>.
	 *
	 * @param v the other vector
	 * @return the dot product
	 */
	public double dot(Vec4 v) {
		return x * v.x + y * v.y + z * v.z + w * v.w;
	}

	/**
	 * Compute the dot product (inner product) of this vector and <tt>(x, y, z, w)</tt>.
	 *
	 * @param x the x component of the other vector
	 * @param y the y component of the other vector
	 * @param z the z component of the other vector
	 * @param w the w component of the other vector
	 * @return the dot product
	 */
	public double dot(double x, double y, double z, double w) {
		return this.x * x + this.y * y + this.z * z + this.w * w;
	}

	/**
	 * Return the cosine of the angle between this vector and the supplied vector.
	 * <p>
	 * Use this instead of <code>Math.cos(angle(v))</code>.
	 *
	 * @param v the other vector
	 * @return the cosine of the angle
	 * @see #angle(Vec4)
	 */
	public double angleCos(Vec4 v) {
		double length1 = Math.sqrt(x * x + y * y + z * z + w * w);
		double length2 = Math.sqrt(v.x * v.x + v.y * v.y + v.z * v.z + v.w * v.w);
		double dot = x * v.x + y * v.y + z * v.z + w * v.w;
		return dot / (length1 * length2);
	}

	/**
	 * Return the angle between this vector and the supplied vector.
	 *
	 * @param v the other vector
	 * @return the angle, in radians
	 * @see #angleCos(Vec4)
	 */
	public double angle(Vec4 v) {
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
	public Vec4 zero() {
		this.x = 0.0;
		this.y = 0.0;
		this.z = 0.0;
		this.w = 0.0;
		return this;
	}

	/**
	 * Negate this vector.
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
	 * Negate this vector and store the result in <code>dest</code>.
	 *
	 * @param dest will hold the result
	 * @return this
	 */
	public Vec4 negate(Vec4 dest) {
		dest.x = -x;
		dest.y = -y;
		dest.z = -z;
		dest.w = -w;
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
		return "(" + formatter.format(x) + " " + formatter.format(y) + " " + formatter.format(z) + " " + formatter.format(
			    w) + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
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

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(w);
		result = prime * result + (int) (temp ^ (temp >>> 32));
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
		Vec4 other = (Vec4) obj;
		if (Double.doubleToLongBits(w) != Double.doubleToLongBits(other.w)) return false;
		if (Double.doubleToLongBits(x) != Double.doubleToLongBits(other.x)) return false;
		if (Double.doubleToLongBits(y) != Double.doubleToLongBits(other.y)) return false;
		return Double.doubleToLongBits(z) == Double.doubleToLongBits(other.z);
	}

	/**
	 * Compute a smooth-step (i.e. hermite with zero tangents) interpolation between <code>this</code> vector and the given vector <code>v</code> and store the result in <code>dest</code>.
	 *
	 * @param v    the other vector
	 * @param t    the interpolation factor, within <tt>[0..1]</tt>
	 * @param dest will hold the result
	 * @return this
	 */
	public Vec4 smoothStep(Vec4 v, double t, Vec4 dest) {
		dest.x = Interpolate.smoothStep(x, v.x, t);
		dest.y = Interpolate.smoothStep(y, v.y, t);
		dest.z = Interpolate.smoothStep(x, v.z, t);
		dest.w = Interpolate.smoothStep(w, v.w, t);
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
	public Vec4 hermite(Vec4 t0, Vec4 v1, Vec4 t1, double t, Vec4 dest) {
		dest.x = Interpolate.hermite(x, t0.x, v1.x, t1.x, t);
		dest.y = Interpolate.hermite(y, t0.y, v1.y, t1.y, t);
		dest.z = Interpolate.hermite(z, t0.z, v1.z, t1.z, t);
		dest.w = Interpolate.hermite(z, t0.w, v1.w, t1.w, t);
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
	public Vec4 lerp(Vec4 other, double t) {
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
	public Vec4 lerp(Vec4 a, Vec4 b, double t) {
		x = (1.0 - t) * a.x + t * b.x;
		y = (1.0 - t) * a.y + t * b.y;
		z = (1.0 - t) * a.z + t * b.z;
		w = (1.0 - t) * a.w + t * b.w;
		return this;
	}


	/**
	 * Converts this Vec4 to a Vec3
	 *
	 * @return Vec3(this.x, this.y, this.z)
	 */
	public Vec3 toVec3() {
		return new Vec3(this.x, this.y, this.z);
	}

	/**
	 * Converts this Vec4 to a Vec2
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
		return Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(z) || Double.isNaN(w);
	}

	/**
	 * returns an independent copy of this object
	 */
	public Vec4 duplicate() {
		return new Vec4(this);
	}

	/**
	 * Sets this to be the component-wise minimum of this and another vector
	 *
	 * @param a the other vector
	 * @return this
	 */
	public Vec4 min(Vec4 a) {
		this.x = Math.min(a.x, this.x);
		this.y = Math.min(a.y, this.y);
		this.z = Math.min(a.z, this.z);
		this.w = Math.min(a.w, this.w);
		return this;
	}

	/**
	 * Sets this to be the component-wise maximum of this and another vector
	 *
	 * @param a the other vector
	 * @return this
	 */
	public Vec4 max(Vec4 a) {
		this.x = Math.max(a.x, this.x);
		this.y = Math.max(a.y, this.y);
		this.z = Math.max(a.z, this.z);
		this.w = Math.max(a.w, this.w);
		return this;
	}

	/**
	 * a Vec4 is a Supplier of type Vec4
	 *
	 * @return this
	 */
	public Vec4 get() {
		return this;
	}

	/**
	 * sets an element (0,1,2 or 3 ) of this vector to the specified value
	 *
	 * @param index the index
	 * @param value the value
	 * @return this
	 */
	public Vec4 set(int index, double value) {
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
			case 3:
				this.w = value;
				return this;
			default:
				throw new IndexOutOfBoundsException("" + index);
		}
	}

	/**
	 * get an element (0,1 2, or 3) of this vector to the specified value
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
			case 3:
				return this.w;
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
	public Vec4 xy(Vec2 v) {
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
	public Vec4 xz(Vec2 v) {
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
	public Vec4 yz(Vec2 v) {
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
	public Vec4 x(double v) {
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
	public Vec4 y(double v) {
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
	public Vec4 z(double v) {
		this.z = v;
		return this;
	}

	/** adds a uniformly distributed random number from -amount to amount to each dimension */
	public void noise(float amount)
	{
		x+= 2*amount*(Math.random()-0.5f);
		y+= 2*amount*(Math.random()-0.5f);
		z+= 2*amount*(Math.random()-0.5f);
		w+= 2*amount*(Math.random()-0.5f);
	}

}
