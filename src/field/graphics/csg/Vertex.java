/**
 * Vertex.java
 *
 * Copyright 2014-2014 Michael Hoffer <info@michaelhoffer.de>. All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY Michael Hoffer <info@michaelhoffer.de> "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL Michael Hoffer <info@michaelhoffer.de> OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are
 * those of the authors and should not be interpreted as representing official
 * policies, either expressed or implied, of Michael Hoffer
 * <info@michaelhoffer.de>.
 */
package field.graphics.csg;

import field.linalg.Vec3;

import java.util.Objects;

/**
 * Represents a vertex of a polygon. This class provides {@link #normal} so
 * primitives can return a smooth vertex normal, but
 * {@link #normal} is not used anywhere else.
 */
public class Vertex {

	/**
	 * Vertex position.
	 */
	public Vec3 pos;

	/**
	 * Normal.
	 */
	public Vec3 normal;

	private double weight = 1.0;

	/**
	 * Constructor. Creates a vertex.
	 *
	 * @param pos position
	 * @param normal normal
	 */
	public Vertex(Vec3 pos, Vec3 normal) {
		this.pos = pos.duplicate();
		this.normal = normal.duplicate();
	}


	/**
	 * Constructor. Creates a vertex.
	 *
	 * @param pos position
	 * @param normal normal
	 * @param weight weight
	 */
	private Vertex(Vec3 pos, Vec3 normal, double weight) {
		this.pos = pos.duplicate();
		this.normal = normal.duplicate();
		this.weight = weight;
	}

	@Override
	public Vertex clone() {
		return new Vertex(pos.duplicate(), normal.duplicate(), weight);
	}

	/**
	 * Inverts all orientation-specific data. (e.g. vertex normal).
	 */
	public void flip() {
		normal.mul(-1);
	}

	/**
	 * Create a new vertex between this vertex and the specified vertex by
	 * linearly interpolating all properties using a parameter t.
	 *
	 * @param other vertex
	 * @param t interpolation parameter
	 * @return a new vertex between this and the specified vertex
	 */
	public Vertex interpolate(Vertex other, double t) {

		return new Vertex(Vec3.lerp(pos, other.pos, 1-t, new Vec3()), Vec3.lerp(normal, other.normal, 1-t, new Vec3()).normalize());

	}

	/**
	 * @return the weight
	 */
	public double getWeight() {
		return weight;
	}

	/**
	 * @param weight the weight to set
	 */
	public void setWeight(double weight) {
		this.weight = weight;
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 53 * hash + Objects.hashCode(this.pos);
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final Vertex other = (Vertex) obj;
		if (!Objects.equals(this.pos, other.pos)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return pos.toString();
	}


}