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

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.function.Supplier;

/**
 * Represents a 2D vector with double-precision.
 *
 * @author RGreenlees
 * @author Kai Burjack
 */
public class Vec2 implements Externalizable, Supplier<Vec2>, Mutable {

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
	 * Create a new {@link Vec2} and initialize its components to zero.
	 */
	public Vec2() {
	}

	public Vec2(List<Number> from)
	{
		this(from.get(0).doubleValue(), from.get(1).doubleValue());
	}

	public Vec2(List<Number> from, int offset)
	{
		this(from.get(offset).doubleValue(), from.get(offset+1).doubleValue());
	}

	/**
	 * Create a new {@link Vec2} and initialize both of its components with the given value.
	 *
	 * @param d the value of both components
	 */
	public Vec2(double d) {
		this(d, d);
	}

	/**
	 * Create a new {@link Vec2} and initialize its components to the given values.
	 *
	 * @param x the x value
	 * @param y the y value
	 */
	public Vec2(double x, double y) {
		this.x = x;
		this.y = y;
	}

	/**
	 * Create a new {@link Vec2} and initialize its components to the one of the given vector.
	 *
	 * @param v the {@link Vec2} to copy the values from
	 */
	public Vec2(Vec2 v) {
		x = v.x;
		y = v.y;
	}

	/**
	 * Create a new {@link Vec2} and read this vector from the supplied {@link ByteBuffer} at the current buffer {@link ByteBuffer#position() position}.
	 * <p>
	 * This method will not increment the position of the given ByteBuffer.
	 * <p>
	 * If you want to specify the offset into the ByteBuffer at which the vector is read, you can use {@link #Vec2(int, ByteBuffer)}, taking the absolute position as parameter.
	 *
	 * @param buffer values will be read in <tt>x, y</tt> order
	 * @see #Vec2(int, ByteBuffer)
	 */
	public Vec2(ByteBuffer buffer) {
		this(buffer.position(), buffer);
		buffer.position(buffer.position() + 2 * 8);
	}

	/**
	 * Create a new {@link Vec2} and read this vector from the supplied {@link ByteBuffer} starting at the specified absolute buffer position/index.
	 * <p>
	 * This method will not increment the position of the given ByteBuffer.
	 *
	 * @param index  the absolute position into the ByteBuffer
	 * @param buffer values will be read in <tt>x, y</tt> order
	 */
	public Vec2(int index, ByteBuffer buffer) {
		x = buffer.getDouble(index);
		y = buffer.getDouble(index + 8);
	}

	/**
	 * Create a new {@link Vec2} and read this vector from the supplied {@link DoubleBuffer} at the current buffer {@link DoubleBuffer#position() position}.
	 * <p>
	 * This method will not increment the position of the given DoubleBuffer.
	 * <p>
	 * If you want to specify the offset into the DoubleBuffer at which the vector is read, you can use {@link #Vec2(int, DoubleBuffer)}, taking the absolute position as parameter.
	 *
	 * @param buffer values will be read in <tt>x, y</tt> order
	 * @see #Vec2(int, DoubleBuffer)
	 */
	public Vec2(DoubleBuffer buffer) {
		this(buffer.position(), buffer);		buffer.position(buffer.position()+2);

	}

	/**
	 * Create a new {@link Vec2} and read this vector from the supplied {@link DoubleBuffer} starting at the specified absolute buffer position/index.
	 * <p>
	 * This method will not increment the position of the given DoubleBuffer.
	 *
	 * @param index  the absolute position into the DoubleBuffer
	 * @param buffer values will be read in <tt>x, y</tt> order
	 */
	public Vec2(int index, DoubleBuffer buffer) {
		x = buffer.get(index);
		y = buffer.get(index + 1);
	}


	/**
	 * Create a new {@link Vec2} and read this vector from the supplied {@link FloatBuffer} at the current buffer {@link FloatBuffer#position() position}.
	 * <p>
	 * This method will not increment the position of the given FloatBuffer.
	 * <p>
	 * If you want to specify the offset into the FloatBuffer at which the vector is read, you can use {@link #Vec2(int, FloatBuffer)}, taking the absolute position as parameter.
	 *
	 * @param buffer values will be read in <tt>x, y</tt> order
	 * @see #Vec2(int, DoubleBuffer)
	 */
	public Vec2(FloatBuffer buffer) {
		this(buffer.position(), buffer);		buffer.position(buffer.position()+2);

	}

	/**
	 * Create a new {@link Vec2} and read this vector from the supplied {@link FloatBuffer} starting at the specified absolute buffer position/index.
	 * <p>
	 * This method will not increment the position of the given FloatBuffer.
	 *
	 * @param index  the absolute position into the FloatBuffer
	 * @param buffer values will be read in <tt>x, y</tt> order
	 */
	public Vec2(int index, FloatBuffer buffer) {
		x = buffer.get(index);
		y = buffer.get(index + 1);
	}

	/**
	 * Store one perpendicular vector of <code>v</code> in <code>dest</code>.
	 *
	 * @param v    the vector to build one perpendicular vector of
	 * @param dest will hold the result
	 * @return dest
	 */
	public static Vec2 perpendicular(Vec2 v, Vec2 dest) {
		if (dest == null) dest = new Vec2();
		dest.x = v.y;
		dest.y = v.x * -1;
		return dest;
	}

	/**
	 * Subtract <code>b</code> from <code>a</code> and store the result in <code>dest</code>.
	 *
	 * @param a    the vector to subtract from
	 * @param b    the vector to subtract
	 * @param dest will hold the result
	 */
	public static Vec2 sub(Vec2 a, Vec2 b, Vec2 dest) {
		if (dest == null) dest = new Vec2();
		dest.x = a.x - b.x;
		dest.y = a.y - b.y;
		return dest;
	}

	/**
	 * Return the dot product of a vector and another.
	 *
	 * @param a the first vector
	 * @param v the other vector
	 * @return the dot product
	 */
	public static double dot(Vec2 a, Vec2 v) {
		return a.x * v.x + a.y * v.y;
	}

	/**
	 * Return the dot product of a vector and another.
	 *
	 * @param x
	 * @param y
	 * @return the dot product
	 */
	public double dot(double x, double y) {
		return this.x * x + this.y * y;
	}

	/**
	 * Add <code>a</code> to <code>b</code> and store the result in <code>dest</code>.
	 *
	 * @param a    the first addend
	 * @param b    the second addend
	 * @param dest will hold the result
	 */
	public static Vec2 add(Vec2 a, Vec2 b, Vec2 dest) {
		if (dest == null) dest = new Vec2();
		dest.x = a.x + b.x;
		dest.y = a.y + b.y;
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
	public static Vec2 fma(Vec2 x, float w, Vec2 y, Vec2 dest) {

		if (dest == null) dest = new Vec2();
		dest.x = x.x * w + y.x;
		dest.y = x.y * w + y.y;

		return dest;
	}

	/**
	 * Multiply <code>v</code> by the <code>scalar</code> value and store the result in <code>dest</code>.
	 *
	 * @param v      the vector to multiply
	 * @param scalar the scalar to multiply the given vector by
	 * @param dest   will hold the result
	 */
	public static Vec2 mul(Vec2 v, double scalar, Vec2 dest) {
		if (dest == null) dest = new Vec2();
		dest.x = v.x * scalar;
		dest.y = v.y * scalar;
		return dest;
	}

	/**
	 * Set the x and y components to the supplied value.
	 *
	 * @param d the value of both components
	 * @return this
	 */
	public Vec2 set(double d) {
		return set(d, d);
	}

	/**
	 * Set the x and y components to the supplied values.
	 *
	 * @param x the x value
	 * @param y the y value
	 * @return this
	 */
	public Vec2 set(double x, double y) {
		this.x = x;
		this.y = y;
		return this;
	}

	/**
	 * Set this {@link Vec2} to the values of v.
	 *
	 * @param v the vector to copy from
	 * @return this
	 */
	public Vec2 set(Vec2 v) {
		x = v.x;
		y = v.y;
		return this;
	}

	/**
	 * Read this vector from the supplied {@link ByteBuffer} at the current buffer {@link ByteBuffer#position() position}.
	 * <p>
	 * This method will not increment the position of the given ByteBuffer.
	 * <p>
	 * If you want to specify the offset into the ByteBuffer at which the vector is read, you can use {@link #set(int, ByteBuffer)}, taking the absolute position as parameter.
	 *
	 * @param buffer values will be read in <tt>x, y</tt> order
	 * @return this
	 * @see #set(int, ByteBuffer)
	 */
	public Vec2 set(ByteBuffer buffer) {
		return set(buffer.position(), buffer);
	}

	/**
	 * Read this vector from the supplied {@link ByteBuffer} starting at the specified absolute buffer position/index.
	 * <p>
	 * This method will not increment the position of the given ByteBuffer.
	 *
	 * @param index  the absolute position into the ByteBuffer
	 * @param buffer values will be read in <tt>x, y</tt> order
	 * @return this
	 */
	public Vec2 set(int index, ByteBuffer buffer) {
		x = buffer.getDouble(index);
		y = buffer.getDouble(index + 8);
		return this;
	}

	/**
	 * Read this vector from the supplied {@link DoubleBuffer} at the current buffer {@link DoubleBuffer#position() position}.
	 * <p>
	 * This method will not increment the position of the given DoubleBuffer.
	 * <p>
	 * If you want to specify the offset into the DoubleBuffer at which the vector is read, you can use {@link #set(int, DoubleBuffer)}, taking the absolute position as parameter.
	 *
	 * @param buffer values will be read in <tt>x, y</tt> order
	 * @return this
	 * @see #set(int, DoubleBuffer)
	 */
	public Vec2 set(DoubleBuffer buffer) {
		return set(buffer.position(), buffer);
	}

	/**
	 * Read this vector from the supplied {@link DoubleBuffer} starting at the specified absolute buffer position/index.
	 * <p>
	 * This method will not increment the position of the given DoubleBuffer.
	 *
	 * @param index  the absolute position into the DoubleBuffer
	 * @param buffer values will be read in <tt>x, y</tt> order
	 * @return this
	 */
	public Vec2 set(int index, DoubleBuffer buffer) {
		x = buffer.get(index);
		y = buffer.get(index + 1);
		return this;
	}

	/**
	 * Store this vector into the supplied {@link ByteBuffer} at the current buffer {@link ByteBuffer#position() position}.
	 * <p>
	 * This method will not increment the position of the given ByteBuffer.
	 * <p>
	 * If you want to specify the offset into the ByteBuffer at which the vector is stored, you can use {@link #get(int, ByteBuffer)}, taking the absolute position as parameter.
	 *
	 * @param buffer will receive the values of this vector in <tt>x, y</tt> order
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
	 * @param buffer will receive the values of this vector in <tt>x, y</tt> order
	 * @return the passed in buffer
	 */
	public ByteBuffer get(int index, ByteBuffer buffer) {
		buffer.putDouble(index, x);
		buffer.putDouble(index + 8, y);
		return buffer;
	}

	/**
	 * Store this vector into the supplied {@link DoubleBuffer} at the current buffer {@link DoubleBuffer#position() position}.
	 * <p>
	 * This method will not increment the position of the given DoubleBuffer.
	 * <p>
	 * If you want to specify the offset into the DoubleBuffer at which the vector is stored, you can use {@link #get(int, DoubleBuffer)}, taking the absolute position as parameter.
	 *
	 * @param buffer will receive the values of this vector in <tt>x, y</tt> order
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
	 * @param buffer will receive the values of this vector in <tt>x, y</tt> order
	 * @return the passed in buffer
	 */
	public DoubleBuffer get(int index, DoubleBuffer buffer) {
		buffer.put(index, x);
		buffer.put(index + 1, y);
		return buffer;
	}

	/**
	 * Set this vector to be one of its perpendicular vectors.
	 *
	 * @return this
	 */
	public Vec2 perpendicular() {
		return set(y, x * -1);
	}

	/**
	 * Subtract <code>v</code> from this vector.
	 *
	 * @param v the vector to subtract
	 * @return this
	 */
	public Vec2 sub(Vec2 v) {
		x -= v.x;
		y -= v.y;
		return this;
	}

	/**
	 * Subtract <tt>(x, y)</tt> from this vector.
	 *
	 * @param x the x component to subtract
	 * @param y the y component to subtract
	 * @return this
	 */
	public Vec2 sub(double x, double y) {
		this.x -= x;
		this.y -= y;
		return this;
	}

	/**
	 * Subtract <tt>(x, y)</tt> from this vector and store the result in <code>dest</code>.
	 *
	 * @param x    the x component to subtract
	 * @param y    the y component to subtract
	 * @param dest will hold the result
	 * @return this
	 */
	public Vec2 sub(double x, double y, Vec2 dest) {
		dest.x = this.x - x;
		dest.y = this.y - y;
		return this;
	}

	/**
	 * Subtract <code>v</code> from <code>this</code> vector and store the result in <code>dest</code>.
	 *
	 * @param v    the vector to subtract
	 * @param dest will hold the result
	 * @return this
	 */
	public Vec2 sub(Vec2 v, Vec2 dest) {
		dest.x = x - v.x;
		dest.y = y - v.y;
		return this;
	}

	/**
	 * Return the dot product of this vector and <code>v</code>.
	 *
	 * @param v the other vector
	 * @return the dot product
	 */
	public double dot(Vec2 v) {
		return x * v.x + y * v.y;
	}

	/**
	 * Return the angle between this vector and the supplied vector.
	 *
	 * @param v the other vector
	 * @return the angle, in radians
	 */
	public double angle(Vec2 v) {
		double dot = x * v.x + y * v.y;
		double det = x * v.y - y * v.x;
		return Math.atan2(det, dot);
	}

	/**
	 * Return the length of this vector.
	 *
	 * @return the length
	 */
	public double length() {
		return Math.sqrt((x * x) + (y * y));
	}

	/**
	 * Return the length squared of this vector.
	 *
	 * @return the length squared
	 */
	public double lengthSquared() {
		return ((x * x) + (y * y));
	}

	/**
	 * Return the distance between <code>this</code> and <code>v</code>.
	 *
	 * @param v the other vector
	 * @return the euclidean distance
	 */
	public double distance(Vec2 v) {
		return Math.sqrt((v.x - x) * (v.x - x) + (v.y - y) * (v.y - y));
	}

	/**
	 * Return the squared distance between <code>this</code> and <code>v</code>.
	 *
	 * @param v the other vector
	 * @return the euclidean distance
	 */
	public double distanceSquared(Vec2 v) {
		return ((v.x - x) * (v.x - x) + (v.y - y) * (v.y - y));
	}

	/**
	 * Normalize this vector.
	 *
	 * @return this
	 */
	public Vec2 normalize() {
		double length = Math.sqrt((x * x) + (y * y));
		x /= length;
		y /= length;
		return this;
	}

	/**
	 * Normalize this vector and store the result in <code>dest</code>.
	 *
	 * @param dest will hold the result
	 * @return this
	 */
	public Vec2 normalize(Vec2 dest) {
		double length = Math.sqrt((x * x) + (y * y));
		dest.x = x / length;
		dest.y = y / length;
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
	public Vec2 smoothStep(Vec2 v, double t, Vec2 dest) {
		dest.x = Interpolate.smoothStep(x, v.x, t);
		dest.y = Interpolate.smoothStep(y, v.y, t);
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
	public Vec2 hermite(Vec2 t0, Vec2 v1, Vec2 t1, double t, Vec2 dest) {
		dest.x = Interpolate.hermite(x, t0.x, v1.x, t1.x, t);
		dest.y = Interpolate.hermite(y, t0.y, v1.y, t1.y, t);
		return this;
	}


	/**
	 * Add <code>v</code> to this vector.
	 *
	 * @param v the vector to add
	 * @return this
	 */
	public Vec2 add(Vec2 v) {
		x += v.x;
		y += v.y;
		return this;
	}

	/**
	 * Add <code>(x, y)</code> to this vector.
	 *
	 * @param x the x component to add
	 * @param y the y component to add
	 * @return this
	 */
	public Vec2 add(double x, double y) {
		this.x += x;
		this.y += y;
		return this;
	}

	/**
	 * Add <code>(x, y)</code> to this vector and store the result in <code>dest</code>.
	 *
	 * @param x    the x component to add
	 * @param y    the y component to add
	 * @param dest will hold the result
	 * @return this
	 */
	public Vec2 add(double x, double y, Vec2 dest) {
		dest.x = this.x + x;
		dest.y = this.y + y;
		return this;
	}

	/**
	 * Add <code>v</code> to this vector and store the result in <code>dest</code>.
	 *
	 * @param v    the vector to add
	 * @param dest will hold the result
	 * @return this
	 */
	public Vec2 add(Vec2 v, Vec2 dest) {
		dest.x = x + v.x;
		dest.y = y + v.y;
		return this;
	}

	/**
	 * Set all components to zero.
	 *
	 * @return this
	 */
	public Vec2 zero() {
		this.x = 0.0;
		this.y = 0.0;
		return this;
	}

	/**
	 * Add the component-wise multiplication of <code>a * b</code> to this vector.
	 *
	 * @param a the first multiplicand
	 * @param b the second multiplicand
	 * @return this
	 */
	public Vec2 fma(Vec2 a, Vec2 b) {
		x += a.x * b.x;
		y += a.y * b.y;
		return this;
	}

	/**
	 * Add the component-wise multiplication of <code>a * b</code> to this vector.
	 *
	 * @param a the first multiplicand
	 * @param b the second multiplicand
	 * @return this
	 */
	public Vec2 fma(double a, Vec2 b) {
		x += a * b.x;
		y += a * b.y;
		return this;
	}

	/**
	 * Add the component-wise multiplication of <code>a * b</code> to this vector.
	 *
	 * @param a the first multiplicand
	 * @param b the second multiplicand
	 * @return this
	 */
	public Vec2 fma(Vec2 b, double a) {
		x += a * b.x;
		y += a * b.y;
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
	public Vec2 fma(Vec2 a, Vec2 b, Vec2 dest) {
		dest.x = x + a.x * b.x;
		dest.y = y + a.y * b.y;
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
	public Vec2 fma(double a, Vec2 b, Vec2 dest) {
		dest.x = x + a * b.x;
		dest.y = y + a * b.y;
		return this;
	}

	/**
	 * Multiply this Vec2 component-wise by another Vec2.
	 *
	 * @param v the vector to multiply by
	 * @return this
	 */
	public Vec2 mul(Vec2 v) {
		x *= v.x;
		y *= v.y;
		return this;
	}

	/**
	 * Multiply this Vec2 component-wise by another Vec2 and store the result in <code>dest</code>.
	 *
	 * @param v    the vector to multiply by
	 * @param dest will hold the result
	 * @return this
	 */
	public Vec2 mul(Vec2 v, Vec2 dest) {
		dest.x = x * v.x;
		dest.y = y * v.y;
		return this;
	}

	/**
	 * Divide this Vec2 component-wise by another Vec2.
	 *
	 * @param v the vector to divide by
	 * @return this
	 */
	public Vec2 div(Vec2 v) {
		x /= v.x;
		y /= v.y;
		return this;
	}

	/**
	 * Divide this Vec2 component-wise by another Vec2 and store the result in <code>dest</code>.
	 *
	 * @param v    the vector to divide by
	 * @param dest will hold the result
	 * @return this
	 */
	public Vec2 div(Vec2 v, Vec2 dest) {
		dest.x = x / v.x;
		dest.y = y / v.y;
		return this;
	}

	/**
	 * Multiply this Vec2 by the given matrix <code>mat</code> and store the result in <code>dest</code>.
	 * <p>
	 * This method uses <tt>w=1.0</tt> as the fourth vector component.
	 *
	 * @param mat  the matrix to multiply this vector by
	 * @param dest will hold the result
	 * @return this
	 */
	public Vec2 mul(Mat4 mat, Vec2 dest) {
		if (this != dest) {
			dest.x = mat.m00 * x + mat.m10 * y + mat.m30;
			dest.y = mat.m01 * x + mat.m11 * y + mat.m31;
		} else {
			dest.set(mat.m00 * x + mat.m10 * y + mat.m30, mat.m01 * x + mat.m11 * y + mat.m31);
		}
		return this;
	}

	/**
	 * Multiply this Vec2 by the given matrix <code>mat</code>.
	 * <p>
	 * This method uses <tt>w=1.0</tt> as the fourth vector component.
	 *
	 * @param mat the matrix to multiply this vector by
	 * @return this
	 */
	public Vec2 mul(Mat4 mat) {
		return mul(mat, this);
	}

	/**
	 * Multiply this Vec2 by the given matrix <code>mat</code>, perform perspective division and store the result in <code>dest</code>.
	 * <p>
	 * This method uses <tt>w=1.0</tt> as the fourth vector component.
	 * <p>
	 * This method differs from {@link #mul(Mat4, Vec2)} in that it also performs perspective division.
	 *
	 * @param mat  the matrix to multiply this vector by
	 * @param dest will hold the result
	 * @return this
	 * @see #mul(Mat4, Vec2)
	 */
	public Vec2 mulProject(Mat4 mat, Vec2 dest) {
		double w = mat.m03 * x + mat.m13 * y + mat.m33;
		if (this != dest) {
			dest.x = (mat.m00 * x + mat.m10 * y + mat.m30) / w;
			dest.y = (mat.m01 * x + mat.m11 * y + mat.m31) / w;
		} else {
			dest.set((mat.m00 * x + mat.m10 * y + mat.m30) / w, (mat.m01 * x + mat.m11 * y + mat.m31) / w);
		}
		return this;
	}

	/**
	 * Multiply this Vec2 by the given matrix <code>mat</code>, perform perspective division.
	 * <p>
	 * This method uses <tt>w=1.0</tt> as the fourth vector component.
	 * <p>
	 * This method differs from {@link #mul(Mat4)} in that it also performs perspective division.
	 *
	 * @param mat the matrix to multiply this vector by
	 * @return this
	 * @see #mul(Mat4)
	 */
	public Vec2 mulProject(Mat4 mat) {
		return mulProject(mat, this);
	}

	/**
	 * Multiply this Vec2 by the given matrix <code>mat</code>.
	 *
	 * @param mat the matrix to multiply this vector by
	 * @return this
	 */
	public Vec2 mul(Mat3 mat) {
		return mul(mat, this);
	}

	/**
	 * Multiply <code>this</code> by the given matrix <code>mat</code> and store the result in <code>dest</code>.
	 *
	 * @param mat  the matrix to multiply this vector by
	 * @param dest will hold the result
	 * @return this
	 */
	public Vec2 mul(Mat3 mat, Vec2 dest) {
		if (this != dest) {
			dest.x = mat.m00 * x + mat.m10 * y;
			dest.y = mat.m01 * x + mat.m11 * y;
		} else {
			dest.set(mat.m00 * x + mat.m10 * y, mat.m01 * x + mat.m11 * y);
		}
		return this;
	}

	/**
	 * Multiply this Vec2 by the given matrix <code>mat</code>.
	 *
	 * @param mat the matrix to multiply this vector by
	 * @return this
	 */
	public Vec2 mul(Mat2 mat) {
		return mul(mat, this);
	}

	/**
	 * Multiply <code>this</code> by the given matrix <code>mat</code> and store the result in <code>dest</code>.
	 *
	 * @param mat  the matrix to multiply this vector by
	 * @param dest will hold the result
	 * @return this
	 */
	public Vec2 mul(Mat2 mat, Vec2 dest) {
		if (this != dest) {
			dest.x = mat.m00 * x + mat.m10 * y;
			dest.y = mat.m01 * x + mat.m11 * y;
		} else {
			dest.set(mat.m00 * x + mat.m10 * y, mat.m01 * x + mat.m11 * y);
		}
		return this;
	}

	/**
	 * Multiply this Vec2 by the given scalar value.
	 *
	 * @param scalar the scalar to multiply this vector by
	 * @return this
	 */
	public Vec2 mul(double scalar) {
		x *= scalar;
		y *= scalar;
		return this;
	}

	/**
	 * Multiply this Vec2 by the given scalar value and store the result in <code>dest</code>.
	 *
	 * @param scalar the scalar factor
	 * @param dest   will hold the result
	 * @return this
	 */
	public Vec2 mul(double scalar, Vec2 dest) {
		dest.x = x * scalar;
		dest.y = y * scalar;
		return this;
	}

	/**
	 * Multiply the components of this Vec2 by the given scalar values and store the result in <code>this</code>.
	 *
	 * @param x the x component to multiply this vector by
	 * @param y the y component to multiply this vector by
	 * @return this
	 */
	public Vec2 mul(double x, double y) {
		this.x *= x;
		this.y *= y;
		return this;
	}

	/**
	 * Multiply the components of this Vec2 by the given scalar values and store the result in <code>dest</code>.
	 *
	 * @param x    the x component to multiply this vector by
	 * @param y    the y component to multiply this vector by
	 * @param dest will hold the result
	 * @return this
	 */
	public Vec2 mul(double x, double y, Vec2 dest) {
		dest.x = this.x * x;
		dest.y = this.y * y;
		return this;
	}

	/**
	 * Rotate this vector by the given quaternion <code>quat</code> and store the result in <code>this</code>.
	 *
	 * @param quat the quaternion to rotate this vector
	 * @return this
	 * @see Quat#transform(Vec2)
	 */
	public Vec2 rotate(Quat quat) {
		quat.transform(this, this);
		return this;
	}

	/**
	 * Rotate this vector by the given quaternion <code>quat</code> and store the result in <code>dest</code>.
	 *
	 * @param quat the quaternion to rotate this vector
	 * @param dest will hold the result
	 * @return this
	 * @see Quat#transform(Vec2)
	 */
	public Vec2 rotate(Quat quat, Vec2 dest) {
		quat.transform(this, dest);
		return this;
	}

	/**
	 * Divide this Vec2 by the given scalar value.
	 *
	 * @param scalar the scalar to divide this vector by
	 * @return this
	 */
	public Vec2 div(double scalar) {
		x /= scalar;
		y /= scalar;
		return this;
	}

	/**
	 * Divide this Vec2 by the given scalar value and store the result in <code>dest</code>.
	 *
	 * @param scalar the scalar to divide this vector by
	 * @param dest   will hold the result
	 * @return this
	 */
	public Vec2 div(double scalar, Vec2 dest) {
		dest.x = x / scalar;
		dest.y = y / scalar;
		return this;
	}

	/**
	 * Divide the components of this Vec2 by the given scalar values and store the result in <code>this</code>.
	 *
	 * @param x the x component to divide this vector by
	 * @param y the y component to divide this vector by
	 * @return this
	 */
	public Vec2 div(double x, double y) {
		this.x /= x;
		this.y /= y;
		return this;
	}

	/**
	 * Divide the components of this Vec2 by the given scalar values and store the result in <code>dest</code>.
	 *
	 * @param x    the x component to divide this vector by
	 * @param y    the y component to divide this vector by
	 * @param dest will hold the result
	 * @return this
	 */
	public Vec2 div(double x, double y, Vec2 dest) {
		dest.x = this.x / x;
		dest.y = this.y / y;
		return this;
	}

	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeDouble(x);
		out.writeDouble(y);
	}

	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		x = in.readDouble();
		y = in.readDouble();
	}

	/**
	 * Negate this vector.
	 *
	 * @return this
	 */
	public Vec2 negate() {
		x = -x;
		y = -y;
		return this;
	}

	/**
	 * Negate this vector and store the result in <code>dest</code>.
	 *
	 * @param dest will hold the result
	 * @return this
	 */
	public Vec2 negate(Vec2 dest) {
		dest.x = -x;
		dest.y = -y;
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
	public Vec2 lerp(Vec2 other, double t) {
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
	public Vec2 lerp(Vec2 a, Vec2 b, double t) {
		x = (1.0 - t) * a.x + t * b.x;
		y = (1.0 - t) * a.y + t * b.y;
		return this;
	}

	/**
	 * Linearly interpolate <code>a</code> and <code>b</code> using the given interpolation factor <code>t</code> and store the result in <code>dest</code>.
	 * <p>
	 * If <code>t</code> is <tt>0.0</tt> then the result is <code>this</code>. If the interpolation factor is <code>1.0</code> then the result is <code>other</code>.
	 *
	 * @param a    the first Vec2
	 * @param b    the second Vec2
	 * @param t    the interpolate (t=0 to 1)
	 * @param dest will hold the result
	 * @return this
	 */
	public static Vec2 lerp(Vec2 a, Vec2 b, double t, Vec2 dest) {
		if (dest == null) dest = new Vec2();
		dest.x = (1.0 - t) * a.x + t * b.x;
		dest.y = (1.0 - t) * a.y + t * b.y;
		return dest;
	}

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(x);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(y);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		Vec2 other = (Vec2) obj;
		if (Double.doubleToLongBits(x) != Double.doubleToLongBits(other.x)) return false;
		return Double.doubleToLongBits(y) == Double.doubleToLongBits(other.y);
	}


	/**
	 * Reflect this vector about the given normal vector.
	 *
	 * @param normal the vector to reflect about
	 * @return this
	 */
	public Vec2 reflect(Vec2 normal) {
		double dot = this.dot(normal);
		x = x - 2.0 * dot * normal.x;
		y = y - 2.0 * dot * normal.y;
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
	public Vec2 reflect(double x, double y, double z) {
		double dot = this.dot(x, y);
		this.x = this.x - 2.0 * dot * x;
		this.y = this.y - 2.0 * dot * y;
		return this;
	}

	/**
	 * Reflect this vector about the given normal vector and store the result in <code>dest</code>.
	 *
	 * @param normal the vector to reflect about
	 * @param dest   will hold the result
	 * @return this
	 */
	public Vec2 reflect(Vec2 normal, Vec2 dest) {
		double dot = this.dot(normal);
		dest.x = x - 2.0 * dot * normal.x;
		dest.y = y - 2.0 * dot * normal.y;
		return this;
	}

	/**
	 * Reflect this vector about the given normal vector and store the result in <code>dest</code>.
	 *
	 * @param x    the x component of the normal
	 * @param y    the y component of the normal
	 * @param dest will hold the result
	 * @return this
	 */
	public Vec2 reflect(double x, double y, Vec2 dest) {
		double dot = this.dot(x, y);
		dest.x = this.x - 2.0 * dot * x;
		dest.y = this.y - 2.0 * dot * y;
		return this;
	}

	/**
	 * Compute the half vector between this and the other vector.
	 *
	 * @param other the other vector
	 * @return this
	 */
	public Vec2 half(Vec2 other) {
		return this.add(other)
			   .normalize();
	}

	/**
	 * Compute the half vector between this and the vector <tt>(x, y, z)</tt>.
	 *
	 * @param x the x component of the other vector
	 * @param y the y component of the other vector
	 * @return this
	 */
	public Vec2 half(double x, double y) {
		return this.add(x, y)
			   .normalize();
	}

	/**
	 * Compute the half vector between this and the other vector and store the result in <code>dest</code>.
	 *
	 * @param other the other vector
	 * @param dest  will hold the result
	 * @return this
	 */
	public Vec2 half(Vec2 other, Vec2 dest) {
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
	public Vec2 half(double x, double y, double z, Vec2 dest) {
		dest.set(this)
		    .add(x, y)
		    .normalize();
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
		return "["+x+", "+y+"]";
//		DecimalFormat formatter = new DecimalFormat(" 0.000E0;-"); //$NON-NLS-1$
//		return toString(formatter).replaceAll("E(\\d+)", "E+$1"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Return a string representation of this vector by formatting the vector components with the given {@link NumberFormat}.
	 *
	 * @param formatter the {@link NumberFormat} used to format the vector components with
	 * @return the string representation
	 */
	public String toString(NumberFormat formatter) {
		return "(" + formatter.format(x) + " " + formatter.format(y) + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	/**
	 * Converts this Vec2 to a Vec3 (with z=0)
	 *
	 * @return Vec3(this.x, this.y, 0)
	 */
	public Vec3 toVec3() {
		return new Vec3(this, 0);
	}

	/**
	 * Converts this Vec2 to a Vec4 (with z=0, w=0)
	 *
	 * @return Vec4(this.x, this.y, 0, 0)
	 */
	public Vec4 toVec4() {
		return new Vec4(this, 0, 0);
	}

	/**
	 * returns true if any component of this Double.isNaN
	 *
	 * @return is any component NaN ?
	 */
	public boolean isNaN() {
		return Double.isNaN(x) || Double.isNaN(y);
	}

	/**
	 * returns an independent copy of this object
	 */
	public Vec2 duplicate() {
		return new Vec2(this);
	}

	/**
	 * Sets this to be the component-wise minimum of this and another vector
	 *
	 * @param a the other vector
	 * @return this
	 */
	public Vec2 min(Vec2 a) {
		this.x = Math.min(a.x, this.x);
		this.y = Math.min(a.y, this.y);
		return this;
	}

	/**
	 * Sets this to be the component-wise maximum of this and another vector
	 *
	 * @param a the other vector
	 * @return this
	 */
	public Vec2 max(Vec2 a) {
		this.x = Math.max(a.x, this.x);
		this.y = Math.max(a.y, this.y);
		return this;
	}

	/**
	 * a Vec2 is a Supplier of type Vec2
	 *
	 * @return this
	 */
	public Vec2 get() {
		return this;
	}

	/**
	 * sets an element (0, or 1) of this vector to the specified value
	 *
	 * @param index the index
	 * @param value the value
	 * @return this
	 */
	public Vec2 set(int index, double value) {
		switch (index) {
			case 0:
				this.x = value;
				return this;
			case 1:
				this.y = value;
				return this;
			default:
				throw new IndexOutOfBoundsException("" + index);
		}
	}

	/**
	 * get an element (0, or 1) of this vector to the specified value
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
			default:
				throw new IndexOutOfBoundsException("" + index);
		}
	}

	/** adds a uniformly distributed random number from -amount to amount to each dimension */
	public void noise(float amount)
	{
		x+= 2*amount*(Math.random()-0.5f);
		y+= 2*amount*(Math.random()-0.5f);
	}

}
