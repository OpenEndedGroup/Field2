/*
 * (C) Copyright 2015 Kai Burjack

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
import java.text.DecimalFormat;
import java.text.NumberFormat;

/**
 * Represents a 3D rotation of a given radians about an axis represented as an unit 3D vector.
 *
 * @author Kai Burjack
 */
public class AxisAngle implements Externalizable {

	private static final long serialVersionUID = 1L;

	/**
	 * The angle in radians.
	 */
	public double angle;
	/**
	 * The x-component of the rotation axis.
	 */
	public double x;
	/**
	 * The y-component of the rotation axis.
	 */
	public double y;
	/**
	 * The z-component of the rotation axis.
	 */
	public double z;

	/**
	 * Create a new {@link AxisAngle} with zero rotation about <tt>(0, 0, 1)</tt>.
	 */
	public AxisAngle() {
		z = 1.0f;
	}

	/**
	 * Create a new {@link AxisAngle} with the same values of <code>a</code>.
	 *
	 * @param a the AngleAxis4f to copy the values from
	 */
	public AxisAngle(AxisAngle a) {
		x = a.x;
		y = a.y;
		z = a.z;
		angle = (angle < 0.0 ? 2.0 * Math.PI + angle % (2.0 * Math.PI) : angle) % (2.0 * Math.PI);
	}

	/**
	 * Create a new {@link AxisAngle} from the given {@link Quat}.
	 * <p>
	 * Reference: <a href= "http://www.euclideanspace.com/maths/geometry/rotations/conversions/quaternionToAngle/" >http://www.euclideanspace.com</a>
	 *
	 * @param q the quaternion from which to create the new AngleAxis4f
	 */
	public AxisAngle(Quat q) {
		double acos = Math.acos(q.w);
		double sqrt = Math.sqrt(1.0 - q.w * q.w);
		this.x = q.x / sqrt;
		this.y = q.y / sqrt;
		this.z = q.z / sqrt;
		this.angle = 2.0 * acos;
	}

	/**
	 * Create a new {@link AxisAngle} with the given values.
	 *
	 * @param angle the angle in radians
	 * @param x     the x-coordinate of the rotation axis
	 * @param y     the y-coordinate of the rotation axis
	 * @param z     the z-coordinate of the rotation axis
	 */
	public AxisAngle(double angle, double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.angle = (angle < 0.0 ? 2.0 * Math.PI + angle % (2.0 * Math.PI) : angle) % (2.0 * Math.PI);
	}

	/**
	 * Create a new {@link AxisAngle} with the given values.
	 *
	 * @param angle the angle in radians
	 * @param v     the rotation axis as a {@link Vec3}
	 */
	public AxisAngle(double angle, Vec3 v) {
		this(angle, v.x, v.y, v.z);
	}

	/**
	 * Set this {@link AxisAngle} to the values of <code>a</code>.
	 *
	 * @param a the AngleAxis4f to copy the values from
	 * @return this
	 */
	public AxisAngle set(AxisAngle a) {
		x = a.x;
		y = a.y;
		z = a.z;
		angle = (angle < 0.0 ? 2.0 * Math.PI + angle % (2.0 * Math.PI) : angle) % (2.0 * Math.PI);
		return this;
	}

	/**
	 * Set this {@link AxisAngle} to the given values.
	 *
	 * @param angle the angle in radians
	 * @param x     the x-coordinate of the rotation axis
	 * @param y     the y-coordinate of the rotation axis
	 * @param z     the z-coordinate of the rotation axis
	 * @return this
	 */
	public AxisAngle set(double angle, double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.angle = (angle < 0.0 ? 2.0 * Math.PI + angle % (2.0 * Math.PI) : angle) % (2.0 * Math.PI);
		return this;
	}

	/**
	 * Set this {@link AxisAngle} to the given values.
	 *
	 * @param angle the angle in radians
	 * @param v     the rotation axis as a {@link Vec3}
	 * @return this
	 */
	public AxisAngle set(double angle, Vec3 v) {
		return set(angle, v.x, v.y, v.z);
	}

	/**
	 * Set this {@link AxisAngle} to be equivalent to the given {@link Quat}.
	 *
	 * @param q the quaternion to set this AngleAxis4f from
	 * @return this
	 */
	public AxisAngle set(Quat q) {
		double acos = Math.acos(q.w);
		double sqrt = Math.sqrt(1.0 - q.w * q.w);
		this.x = q.x / sqrt;
		this.y = q.y / sqrt;
		this.z = q.z / sqrt;
		this.angle = 2.0f * acos;
		return this;
	}


	/**
	 * Set this {@link AxisAngle} to be equivalent to the rotation of the given {@link Mat3}.
	 *
	 * @param m the Mat3 to set this AngleAxis4f from
	 * @return this
	 */
	public AxisAngle set(Mat3 m) {
		double cos = (m.m00 + m.m11 + m.m22 - 1.0) * 0.5;
		x = m.m12 - m.m21;
		y = m.m20 - m.m02;
		z = m.m01 - m.m10;
		double sin = 0.5 * Math.sqrt(x * x + y * y + z * z);
		angle = Math.atan2(sin, cos);
		return this;
	}

	/**
	 * Set this {@link AxisAngle} to be equivalent to the rotational component of the given {@link Mat4}.
	 *
	 * @param m the Mat4 to set this AngleAxis4f from
	 * @return this
	 */
	public AxisAngle set(Mat4 m) {
		double cos = (m.m00 + m.m11 + m.m22 - 1.0) * 0.5;
		x = m.m12 - m.m21;
		y = m.m20 - m.m02;
		z = m.m01 - m.m10;
		double sin = 0.5 * Math.sqrt(x * x + y * y + z * z);
		angle = Math.atan2(sin, cos);
		return this;
	}

	/**
	 * Set the given {@link Quat} to be equivalent to this {@link AxisAngle} rotation.
	 *
	 * @param q the quaternion to set
	 * @return this
	 * @see Quat#set(AxisAngle)
	 */
	public AxisAngle get(Quat q) {
		q.set(this);
		return this;
	}

	/**
	 * Set the given {@link Mat4} to a rotation transformation equivalent to this {@link AxisAngle}.
	 *
	 * @param m the matrix to set
	 * @return this
	 * @see Mat4#set(AxisAngle)
	 */
	public AxisAngle get(Mat4 m) {
		m.set(this);
		return this;
	}

	/**
	 * Set the given {@link Mat3} to a rotation transformation equivalent to this {@link AxisAngle}.
	 *
	 * @param m the matrix to set
	 * @return this
	 * @see Mat3#set(AxisAngle)
	 */
	public AxisAngle get(Mat3 m) {
		m.set(this);
		return this;
	}

	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeDouble(angle);
		out.writeDouble(x);
		out.writeDouble(y);
		out.writeDouble(z);
	}

	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		angle = in.readDouble();
		x = in.readDouble();
		y = in.readDouble();
		z = in.readDouble();
	}

	/**
	 * Normalize the axis vector.
	 *
	 * @return this
	 */
	public AxisAngle normalize() {
		double length = Math.sqrt(x * x + y * y + z * z);
		x /= length;
		y /= length;
		z /= length;
		return this;
	}

	/**
	 * Increase the rotation angle by the given amount.
	 * <p>
	 * This method also takes care of wrapping around.
	 *
	 * @param ang the angle increase
	 * @return this
	 */
	public AxisAngle rotate(double ang) {
		angle += ang;
		angle = (angle < 0.0 ? 2.0 * Math.PI + angle % (2.0 * Math.PI) : angle) % (2.0 * Math.PI);
		return this;
	}

	/**
	 * Transform the given vector by the rotation transformation described by this {@link AxisAngle}.
	 *
	 * @param v the vector to transform
	 * @return this
	 */
	public AxisAngle transform(Vec3 v) {
		return transform(v, v);
	}

	/**
	 * Transform the given vector by the rotation transformation described by this {@link AxisAngle} and store the result in <code>dest</code>.
	 *
	 * @param v    the vector to transform
	 * @param dest will hold the result
	 * @return this
	 */
	public AxisAngle transform(Vec3 v, Vec3 dest) {
		double cos = Math.cos(angle);
		double sin = Math.sin(angle);
		double dot = x * v.x + y * v.y + z * v.z;
		dest.set(v.x * cos + sin * (y * v.z - z * v.y) + (1.0 - cos) * dot * x, v.y * cos + sin * (z * v.x - x * v.z) + (1.0 - cos) * dot * y,
			 v.z * cos + sin * (x * v.y - y * v.x) + (1.0 - cos) * dot * z);
		return this;
	}

	/**
	 * Transform the given vector by the rotation transformation described by this {@link AxisAngle}.
	 *
	 * @param v the vector to transform
	 * @return this
	 */
	public AxisAngle transform(Vec4 v) {
		return transform(v, v);
	}

	/**
	 * Transform the given vector by the rotation transformation described by this {@link AxisAngle} and store the result in <code>dest</code>.
	 *
	 * @param v    the vector to transform
	 * @param dest will hold the result
	 * @return this
	 */
	public AxisAngle transform(Vec4 v, Vec4 dest) {
		double cos = Math.cos(angle);
		double sin = Math.sin(angle);
		double dot = x * v.x + y * v.y + z * v.z;
		dest.set(v.x * cos + sin * (y * v.z - z * v.y) + (1.0 - cos) * dot * x, v.y * cos + sin * (z * v.x - x * v.z) + (1.0 - cos) * dot * y,
			 v.z * cos + sin * (x * v.y - y * v.x) + (1.0 - cos) * dot * z, dest.w);
		return this;
	}

	/**
	 * Return a string representation of this {@link AxisAngle}.
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
	 * Return a string representation of this {@link AxisAngle} by formatting the components with the given {@link NumberFormat}.
	 *
	 * @param formatter the {@link NumberFormat} used to format the vector components with
	 * @return the string representation
	 */
	public String toString(NumberFormat formatter) {
		return "(" + formatter.format(x) + formatter.format(y) + formatter.format(z) + " <|" + formatter.format(angle) + " )"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	public int hashCode() {
		final int prime = 31;
		long result = 1;
		double nangle = (angle < 0.0 ? 2.0 * Math.PI + angle % (2.0 * Math.PI) : angle) % (2.0 * Math.PI);
		result = prime * result + Double.doubleToLongBits(nangle);
		result = prime * result + Double.doubleToLongBits(x);
		result = prime * result + Double.doubleToLongBits(y);
		result = prime * result + Double.doubleToLongBits(z);
		return (int) (result ^ (result >>> 32));
	}

	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		AxisAngle other = (AxisAngle) obj;
		double nangle = (angle < 0.0 ? 2.0 * Math.PI + angle % (2.0 * Math.PI) : angle) % (2.0 * Math.PI);
		double nangleOther = (other.angle < 0.0 ? 2.0 * Math.PI + other.angle % (2.0 * Math.PI) : other.angle) % (2.0 * Math.PI);
		if (Double.doubleToLongBits(nangle) != Double.doubleToLongBits(nangleOther)) return false;
		if (Double.doubleToLongBits(x) != Double.doubleToLongBits(other.x)) return false;
		if (Double.doubleToLongBits(y) != Double.doubleToLongBits(other.y)) return false;
		return Double.doubleToLongBits(z) == Double.doubleToLongBits(other.z);
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
	 * Return the specified {@link Vec4}.
	 * <p>
	 * When using method chaining in a fluent interface style, this method can be used to switch the <i>context object</i>, on which further method invocations operate, to be the given vector.
	 *
	 * @param v the {@link Vec4} to return
	 * @return that vector
	 */
	public Vec4 with(Vec4 v) {
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
	 * Return the specified {@link Mat4}.
	 * <p>
	 * When using method chaining in a fluent interface style, this method can be used to switch the <i>context object</i>, on which further method invocations operate, to be the given matrix.
	 *
	 * @param m the {@link Mat4} to return
	 * @return that matrix
	 */
	public Mat4 with(Mat4 m) {
		return m;
	}

}
