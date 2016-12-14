/**
 * Cube.java
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

import java.util.ArrayList;
import java.util.List;

/**
 * An axis-aligned solid cuboid defined by {@code center} and {@code dimensions}.
 *
 * @author Michael Hoffer &lt;info@michaelhoffer.de&gt;
 */
public class Cuboid implements Primitive {

	/**
	 * Center of this cube.
	 */
	private Vec3 center;
	/**
	 * Cube dimensions.
	 */
	private Vec3 dimensions;

	/**
	 * Constructor. Creates a new cube with center {@code [0, 0, 0]} and dimensions {@code [1, 1, 1]}.
	 */
	public Cuboid() {
		center = new Vec3(0, 0, 0);
		dimensions = new Vec3(1, 1, 1);
	}

	/**
	 * Constructor. Creates a new cube with center {@code [0, 0, 0]} and dimensions {@code [size, size, size]}.
	 *
	 * @param size size
	 */
	public Cuboid(double size) {
		center = new Vec3(0, 0, 0);
		dimensions = new Vec3(size, size, size);
	}

	/**
	 * Constructor. Creates a new cuboid with the specified center and dimensions.
	 *
	 * @param center     center of the cuboid
	 * @param dimensions dimensions
	 */
	public Cuboid(Vec3 center, Vec3 dimensions) {
		this.center = center;
		this.dimensions = dimensions;
	}

	/**
	 * Constructor. Creates a new cuboid with center {@code [0, 0, 0]} and with the specified dimensions.
	 *
	 * @param w width
	 * @param h height
	 * @param d depth
	 */
	public Cuboid(double w, double h, double d) {
		this(new Vec3(), new Vec3(w, h, d));
	}

	public List<Polygon> toPolygons() {

		int[][][] a = {
			    // position     // normal
			    {{0, 4, 6, 2}, {-1, 0, 0}}, {{1, 3, 7, 5}, {+1, 0, 0}}, {{0, 1, 5, 4}, {0, -1, 0}}, {{2, 6, 7, 3}, {0, +1, 0}}, {{0, 2, 3, 1}, {0, 0, -1}}, {{4, 5, 7, 6}, {0, 0, +1}}};
		List<Polygon> polygons = new ArrayList<>();
		for (int[][] info : a) {
			List<Vertex> vertices = new ArrayList<>();
			for (int i : info[0]) {
				Vec3 pos = new Vec3(center.x + dimensions.x * (1 * Math.min(1, i & 1) - 0.5), center.y + dimensions.y * (1 * Math.min(1, i & 2) - 0.5), center.z + dimensions.z * (1 * Math.min(1, i & 4) - 0.5));
				vertices.add(new Vertex(pos, new Vec3((double) info[1][0], (double) info[1][1], (double) info[1][2])));
			}
			polygons.add(new Polygon(vertices));
		}


		return polygons;
	}

	/**
	 * @return the center
	 */
	public Vec3 getCenter() {
		return center;
	}

	/**
	 * @param center the center to set
	 */
	public void setCenter(Vec3 center) {
		this.center = center;
	}

	/**
	 * @return the dimensions
	 */
	public Vec3 getDimensions() {
		return dimensions;
	}

	/**
	 * @param dimensions the dimensions to set
	 */
	public void setDimensions(Vec3 dimensions) {
		this.dimensions = dimensions;
	}

}
