package field.graphics.csg;
/**
 * Polygon.java
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

import field.linalg.Vec3;
import org.poly2tri.Poly2Tri;
import org.poly2tri.geometry.polygon.PolygonPoint;
import org.poly2tri.triangulation.TriangulationPoint;
import org.poly2tri.triangulation.delaunay.DelaunayTriangle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Represents a convex polygon.
 * <p>
 * Each convex polygon has a {@code shared} property, disthich is shared betdisteen all polygons that are clones of each other or where split from the
 * same polygon. This can be used to define per-polygon properties (such as surface color).
 */
public final class Polygon {


	/**
	 * Polygon vertices
	 */
	public final List<Vertex> vertices;


	/**
	 * Plane defined by this polygon.
	 * <p>
	 * <b>Note:</b> uses first three vertices to define the plane.
	 */
	public final Plane plane;


	/**
	 * Decomposes the specified concave polygon into convex polygons.
	 *
	 * @param points the points that define the polygon
	 * @return the decomposed concave polygon (list of convex polygons)
	 */
	public static List<Polygon> fromConcavePoints(Vec3... points) {
		Polygon p = fromPoints(points);

		return concaveToConvex(p);
	}

	/**
	 * Decomposes the specified concave polygon into convex polygons.
	 *
	 * @param points the points that define the polygon
	 * @return the decomposed concave polygon (list of convex polygons)
	 */
	public static List<Polygon> fromConcavePoints(List<Vec3> points) {
		Polygon p = fromPoints(points);

		return concaveToConvex(p);
	}

	/**
	 * Constructor. Creates a new polygon that consists of the specified vertices.
	 * <p>
	 * <b>Note:</b> the vertices used to initialize a polygon must be coplanar and form a convex loop.
	 *
	 * @param vertices polygon vertices
	 */
	public Polygon(List<Vertex> vertices) {
		this.vertices = vertices;
		this.plane = Plane.createFromPoints(vertices.get(0).pos, vertices.get(1).pos, vertices.get(2).pos);
	}


	/**
	 * Constructor. Creates a new polygon that consists of the specified vertices.
	 * <p>
	 * <b>Note:</b> the vertices used to initialize a polygon must be coplanar and form a convex loop.
	 *
	 * @param vertices polygon vertices
	 */
	public Polygon(Vertex... vertices) {
		this(Arrays.asList(vertices));
	}

	@Override
	public Polygon clone() {
		List<Vertex> newVertices = new ArrayList<>();
		this.vertices.forEach((vertex) -> {
			newVertices.add(vertex.clone());
		});
		return new Polygon(newVertices);
	}

	/**
	 * Flips this polygon.
	 *
	 * @return this polygon
	 */
	public Polygon flip() {
		vertices.forEach((vertex) -> {
			vertex.flip();
		});
		Collections.reverse(vertices);

		plane.flip();

		return this;
	}

	/**
	 * Returns a flipped copy of this polygon.
	 * <p>
	 * <b>Note:</b> this polygon is not modified.
	 *
	 * @return a flipped copy of this polygon
	 */
	public Polygon flipped() {
		return clone().flip();
	}

	/**
	 * Translates this polygon.
	 *
	 * @param v the vector that defines the translation
	 * @return this polygon
	 */
	public Polygon translate(Vec3 v) {
		vertices.forEach((vertex) -> {
			vertex.pos = Vec3.add(vertex.pos, v, new Vec3());
		});

		Vec3 a = this.vertices.get(0).pos;
		Vec3 b = this.vertices.get(1).pos;
		Vec3 c = this.vertices.get(2).pos;

		this.plane.normal = Vec3.cross(Vec3.sub(b, a, new Vec3()), Vec3.sub(c, a, new Vec3()), new Vec3());

		return this;
	}

	/**
	 * Returns a translated copy of this polygon.
	 * <p>
	 * <b>Note:</b> this polygon is not modified
	 *
	 * @param v the vector that defines the translation
	 * @return a translated copy of this polygon
	 */
	public Polygon translated(Vec3 v) {
		return clone().translate(v);
	}


	/**
	 * Creates a polygon from the specified point list.
	 *
	 * @param points the points that define the polygon
	 * @return a polygon defined by the specified point list
	 */
	public static Polygon fromPoints(List<Vec3> points) {
		return fromPoints(points, null);
	}

	/**
	 * Creates a polygon from the specified points.
	 *
	 * @param points the points that define the polygon
	 * @return a polygon defined by the specified point list
	 */
	public static Polygon fromPoints(Vec3... points) {
		return fromPoints(Arrays.asList(points), null);
	}

	/**
	 * Creates a polygon from the specified point list.
	 *
	 * @param points the points that define the polygon
	 * @param plane  may be null
	 * @return a polygon defined by the specified point list
	 */
	private static Polygon fromPoints(List<Vec3> points, Plane plane) {

		Vec3 normal = (plane != null) ? plane.normal.clone() : new Vec3(0, 0, 0);

		List<Vertex> vertices = new ArrayList<>();

		for (Vec3 p : points) {
			Vec3 vec = p.clone();
			Vertex vertex = new Vertex(vec, normal);
			vertices.add(vertex);
		}

		return new Polygon(vertices);
	}


	public static List<Polygon> concaveToConvex(Polygon concave) {

		List<Polygon> result = new ArrayList<>();

		Vec3 normal = concave.vertices.get(0).normal.clone();

		boolean cw = !isCCW(concave);

		List<PolygonPoint> points = new ArrayList<>();

		for (Vertex v : concave.vertices) {
			PolygonPoint vp = new PolygonPoint(v.pos.x, v.pos.y, v.pos.z);
			points.add(vp);
		}

		org.poly2tri.geometry.polygon.Polygon p = new org.poly2tri.geometry.polygon.Polygon(points);

		Poly2Tri.triangulate(p);

		List<DelaunayTriangle> triangles = p.getTriangles();

		List<Vertex> triPoints = new ArrayList<>();

		for (DelaunayTriangle t : triangles) {

			int counter = 0;
			for (TriangulationPoint tp : t.points) {

				triPoints.add(new Vertex(new Vec3(tp.getX(), tp.getY(), tp.getZ()), normal));

				if (counter == 2) {
					if (!cw) {
						Collections.reverse(triPoints);
					}
					Polygon poly = new Polygon(triPoints);
					result.add(poly);
					counter = 0;

				} else {
					counter++;
				}
			}
		}

		return result;
	}

	/**
	 * Returns the bounds of this polygon.
	 *
	 * @return bouds of this polygon
	 */
	public Bounds getBounds() {
		double minX = Double.POSITIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		double minZ = Double.POSITIVE_INFINITY;

		double maxX = Double.NEGATIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;
		double maxZ = Double.NEGATIVE_INFINITY;

		for (int i = 0; i < vertices.size(); i++) {

			Vertex vert = vertices.get(i);

			if (vert.pos.x < minX) {
				minX = vert.pos.x;
			}
			if (vert.pos.y < minY) {
				minY = vert.pos.y;
			}
			if (vert.pos.z < minZ) {
				minZ = vert.pos.z;
			}

			if (vert.pos.x > maxX) {
				maxX = vert.pos.x;
			}
			if (vert.pos.y > maxY) {
				maxY = vert.pos.y;
			}
			if (vert.pos.z > maxZ) {
				maxZ = vert.pos.z;
			}

		} // end for vertices

		return new Bounds(new Vec3(minX, minY, minZ), new Vec3(maxX, maxY, maxZ));
	}

	public static boolean isCCW(Polygon polygon) {
		// thanks to Sepp Reiter for explaining me the algorithm!

		if (polygon.vertices.size() < 3) {
			throw new IllegalArgumentException("Only polygons with at least 3 vertices are supported!");
		}

		// search highest left vertex
		int highestLeftVertexIndex = 0;
		Vertex highestLeftVertex = polygon.vertices.get(0);
		for (int i = 0; i < polygon.vertices.size(); i++) {
			Vertex v = polygon.vertices.get(i);

			if (v.pos.y > highestLeftVertex.pos.y) {
				highestLeftVertex = v;
				highestLeftVertexIndex = i;
			} else if (v.pos.y == highestLeftVertex.pos.y && v.pos.x < highestLeftVertex.pos.x) {
				highestLeftVertex = v;
				highestLeftVertexIndex = i;
			}
		}

		// determine next and previous vertex indices
		int nextVertexIndex = (highestLeftVertexIndex + 1) % polygon.vertices.size();
		int prevVertexIndex = highestLeftVertexIndex - 1;
		if (prevVertexIndex < 0) {
			prevVertexIndex = polygon.vertices.size() - 1;
		}
		Vertex nextVertex = polygon.vertices.get(nextVertexIndex);
		Vertex prevVertex = polygon.vertices.get(prevVertexIndex);

		// edge 1
		double a1 = normalizedX(highestLeftVertex.pos, nextVertex.pos);

		// edge 2
		double a2 = normalizedX(highestLeftVertex.pos, prevVertex.pos);

		// select vertex with lowest x value
		int selectedVIndex;

		if (a2 > a1) {
			selectedVIndex = nextVertexIndex;
		} else {
			selectedVIndex = prevVertexIndex;
		}

		if (selectedVIndex == 0 && highestLeftVertexIndex == polygon.vertices.size() - 1) {
			selectedVIndex = polygon.vertices.size();
		}

		if (highestLeftVertexIndex == 0 && selectedVIndex == polygon.vertices.size() - 1) {
			highestLeftVertexIndex = polygon.vertices.size();
		}

		// indicates whether edge points from highestLeftVertexIndex towards
		// the sel index (ccw)
		return selectedVIndex > highestLeftVertexIndex;
	}

	private static double normalizedX(Vec3 v1, Vec3 v2) {

		double dx = v2.x - v1.x;
		double dy = v2.y - v1.y;

		double m = Math.sqrt(dx * dx + dy * dy);

		return dx / m;
	}
}