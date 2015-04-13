package field.graphics.csg;

/**
 * Heavily inspired by github:miho/JCSG
 */

/**
 * CSG.java
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

import field.graphics.FLine;
import field.graphics.FLinesAndJavaShapes;
import field.graphics.MeshBuilder;
import field.linalg.Vec3;
import quickhull3d.Point3d;
import quickhull3d.QuickHull3D;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Constructive Solid Geometry (CSG).
 * <p>
 * This implementation is a Java port of <a href="https://github.com/evanw/csg.js/">https://github.com/evanw/csg.js/</a> with some additional features
 * like polygon extrude, transformations etc. Thanks to the author for creating the CSG.js library.<br><br>
 * <p>
 * <b>Implementation Details</b>
 * <p>
 * All CSG operations are implemented in terms of two functions, {@link Node#clipTo(Node)} and {@link Node#invert()}, which remove parts of a BSP tree
 * inside another BSP tree and swap solid and empty space, respectively. To find the union of {@code a} and {@code b}, we want to remove everything in
 * {@code a} inside {@code b} and everything in {@code b} inside {@code a}, then combine polygons from {@code a} and {@code b} into one solid:
 * <p>
 * <blockquote><pre>
 *     a.clipTo(b);
 *     b.clipTo(a);
 *     a.build(b.allPolygons());
 * </pre></blockquote>
 * <p>
 * The only tricky part is handling overlapping coplanar polygons in both trees. The code above keeps both copies, but we need to keep them in one
 * tree and remove them in the other tree. To remove them from {@code b} we can clip the inverse of {@code b} against {@code a}. The code for union
 * now looks like this:
 * <p>
 * <blockquote><pre>
 *     a.clipTo(b);
 *     b.clipTo(a);
 *     b.invert();
 *     b.clipTo(a);
 *     b.invert();
 *     a.build(b.allPolygons());
 * </pre></blockquote>
 * <p>
 * Subtraction and intersection naturally follow from set operations. If union is {@code A | B}, differenceion is {@code A - B = ~(~A | B)} and
 * intersection is {@code A & B = ~(~A | ~B)} where {@code ~} is the complement operator.
 */
public class CSG {

	private List<Polygon> polygons;
	private static OptType defaultOptType = OptType.NONE;
	private OptType optType = null;

	private CSG() {
	}

	/**
	 * Constructs a CSG from a list of {@link Polygon} instances.
	 *
	 * @param polygons polygons
	 * @return a CSG instance
	 */
	public static CSG fromPolygons(List<Polygon> polygons) {

		CSG csg = new CSG();
		csg.polygons = polygons;

		return csg;
	}

	/**
	 * Constructs a CSG from the specified {@link Polygon} instances.
	 *
	 * @param polygons polygons
	 * @return a CSG instance
	 */
	public static CSG fromPolygons(Polygon... polygons) {
		return fromPolygons(Arrays.asList(polygons));
	}


	@Override
	public CSG clone() {
		CSG csg = new CSG();

		csg.setOptType(this.getOptType());

		Stream<Polygon> polygonStream;

		if (polygons.size() > 200) {
			polygonStream = polygons.parallelStream();
		} else {
			polygonStream = polygons.stream();
		}

		csg.polygons = polygonStream.map((Polygon p) -> p.clone())
					    .collect(Collectors.toList());

		return csg;
	}

	/**
	 * Returns the convex hull of this csg.
	 *
	 * @return the convex hull of this csg
	 */
	public CSG hull() {

		List<Vec3> points = new ArrayList<>();

		getPolygons().forEach((p) -> p.vertices.forEach((v) -> points.add(v.pos)));

		Point3d[] hullPoints = points.stream()
					     .map((vec) -> new Point3d(vec.x, vec.y, vec.z))
					     .toArray(Point3d[]::new);

		QuickHull3D hull = new QuickHull3D();
		hull.build(hullPoints);
		hull.triangulate();

		int[][] faces = hull.getFaces();

		List<Polygon> polygons = new ArrayList<>();

		List<Vec3> vertices = new ArrayList<>();

		for (int[] verts : faces) {

			for (int i : verts) {
				vertices.add(points.get(hull.getVertexPointIndices()[i]));
			}

			polygons.add(Polygon.fromPoints(vertices));

			vertices.clear();
		}

		return CSG.fromPolygons(polygons);
	}

	/**
	 * Returns the convex hull of this csg and the union of the specified csgs.
	 *
	 * @param csgs csgs
	 * @return the convex hull of this csg and the specified csgs
	 */
	public CSG hull(List<CSG> csgs) {

		CSG csgsUnion = new CSG();
		csgsUnion.optType = optType;
		csgsUnion.polygons = this.clone().polygons;

		csgs.stream()
		    .forEach((csg) -> {
			    csgsUnion.polygons.addAll(csg.clone().polygons);
		    });

		return csgsUnion.hull();

	}

	/**
	 * Returns the convex hull of this csg and the union of the specified csgs.
	 *
	 * @param csgs csgs
	 * @return the convex hull of this csg and the specified csgs
	 */
	public CSG hull(CSG... csgs) {

		return hull(Arrays.asList(csgs));
	}

	/**
	 * @return the polygons of this CSG
	 */
	public List<Polygon> getPolygons() {
		return polygons;
	}

	/**
	 * Defines the CSg optimization type.
	 *
	 * @param type optimization type
	 * @return this CSG
	 */
	public CSG optimization(OptType type) {
		this.setOptType(type);
		return this;
	}

	/**
	 * Return a new CSG solid representing the union of this csg and the specified csg.
	 * <p>
	 * <b>Note:</b> Neither this csg nor the specified csg are weighted.
	 * <p>
	 * <blockquote><pre>
	 *    A.union(B)
	 * <p>
	 *    +-------+            +-------+
	 *    |       |            |       |
	 *    |   A   |            |       |
	 *    |    +--+----+   =   |       +----+
	 *    +----+--+    |       +----+       |
	 *         |   B   |            |       |
	 *         |       |            |       |
	 *         +-------+            +-------+
	 * </pre></blockquote>
	 *
	 * @param csg other csg
	 * @return union of this csg and the specified csg
	 */
	public CSG union(CSG csg) {

		switch (getOptType()) {
			case CSG_BOUND:
				return _unionCSGBoundsOpt(csg);
			case POLYGON_BOUND:
				return _unionPolygonBoundsOpt(csg);
			default:
//                return _unionIntersectOpt(csg);
				return _unionNoOpt(csg);
		}
	}

	/**
	 * Return a new CSG solid representing the union of this csg and the specified csgs.
	 * <p>
	 * <b>Note:</b> Neither this csg nor the specified csg are weighted.
	 * <p>
	 * <blockquote><pre>
	 *    A.union(B)
	 * <p>
	 *    +-------+            +-------+
	 *    |       |            |       |
	 *    |   A   |            |       |
	 *    |    +--+----+   =   |       +----+
	 *    +----+--+    |       +----+       |
	 *         |   B   |            |       |
	 *         |       |            |       |
	 *         +-------+            +-------+
	 * </pre></blockquote>
	 *
	 * @param csgs other csgs
	 * @return union of this csg and the specified csgs
	 */
	public CSG union(List<CSG> csgs) {

		CSG result = this;

		for (CSG csg : csgs) {
			result = result.union(csg);
		}

		return result;
	}

	/**
	 * Return a new CSG solid representing the union of this csg and the specified csgs.
	 * <p>
	 * <b>Note:</b> Neither this csg nor the specified csg are weighted.
	 * <p>
	 * <blockquote><pre>
	 *    A.union(B)
	 * <p>
	 *    +-------+            +-------+
	 *    |       |            |       |
	 *    |   A   |            |       |
	 *    |    +--+----+   =   |       +----+
	 *    +----+--+    |       +----+       |
	 *         |   B   |            |       |
	 *         |       |            |       |
	 *         +-------+            +-------+
	 * </pre></blockquote>
	 *
	 * @param csgs other csgs
	 * @return union of this csg and the specified csgs
	 */
	public CSG union(CSG... csgs) {
		return union(Arrays.asList(csgs));
	}


	private CSG _unionCSGBoundsOpt(CSG csg) {
		System.err.println("WARNING: using " + CSG.OptType.NONE + " since other optimization types missing for union operation.");
		return _unionIntersectOpt(csg);
	}

	private CSG _unionPolygonBoundsOpt(CSG csg) {
		List<Polygon> inner = new ArrayList<>();
		List<Polygon> outer = new ArrayList<>();

		Bounds bounds = csg.getBounds();

		this.polygons.stream()
			     .forEach((p) -> {
				     if (bounds.intersects(p.getBounds())) {
					     inner.add(p);
				     } else {
					     outer.add(p);
				     }
			     });

		List<Polygon> allPolygons = new ArrayList<>();

		if (!inner.isEmpty()) {
			CSG innerCSG = CSG.fromPolygons(inner);

			allPolygons.addAll(outer);
			allPolygons.addAll(innerCSG._unionNoOpt(csg).polygons);
		} else {
			allPolygons.addAll(this.polygons);
			allPolygons.addAll(csg.polygons);
		}

		return CSG.fromPolygons(allPolygons)
			  .optimization(getOptType());
	}

	/**
	 * Optimizes for intersection. If csgs do not intersect create a new csg that consists of the polygon lists of this csg and the specified csg.
	 * In this case no further space partitioning is performed.
	 *
	 * @param csg csg
	 * @return the union of this csg and the specified csg
	 */
	private CSG _unionIntersectOpt(CSG csg) {
		boolean intersects = false;

		Bounds bounds = csg.getBounds();

		for (Polygon p : polygons) {
			if (bounds.intersects(p.getBounds())) {
				intersects = true;
				break;
			}
		}

		List<Polygon> allPolygons = new ArrayList<>();

		if (intersects) {
			return _unionNoOpt(csg);
		} else {
			allPolygons.addAll(this.polygons);
			allPolygons.addAll(csg.polygons);
		}

		return CSG.fromPolygons(allPolygons)
			  .optimization(getOptType());
	}

	private CSG _unionNoOpt(CSG csg) {
		Node a = new Node(this.clone().polygons);
		Node b = new Node(csg.clone().polygons);
		a.clipTo(b);
		b.clipTo(a);
		b.invert();
		b.clipTo(a);
		b.invert();
		a.build(b.allPolygons());
		return CSG.fromPolygons(a.allPolygons())
			  .optimization(getOptType());
	}

	/**
	 * Return a new CSG solid representing the difference of this csg and the specified csgs.
	 * <p>
	 * <b>Note:</b> Neither this csg nor the specified csgs are weighted.
	 * <p>
	 * <blockquote><pre>
	 * A.difference(B)
	 * <p>
	 * +-------+            +-------+
	 * |       |            |       |
	 * |   A   |            |       |
	 * |    +--+----+   =   |    +--+
	 * +----+--+    |       +----+
	 *      |   B   |
	 *      |       |
	 *      +-------+
	 * </pre></blockquote>
	 *
	 * @param csgs other csgs
	 * @return difference of this csg and the specified csgs
	 */
	public CSG difference(List<CSG> csgs) {

		if (csgs.isEmpty()) {
			return this.clone();
		}

		CSG csgsUnion = csgs.get(0);

		for (int i = 1; i < csgs.size(); i++) {
			csgsUnion = csgsUnion.union(csgs.get(i));
		}

		return difference(csgsUnion);
	}

	/**
	 * Return a new CSG solid representing the difference of this csg and the specified csgs.
	 * <p>
	 * <b>Note:</b> Neither this csg nor the specified csgs are weighted.
	 * <p>
	 * <blockquote><pre>
	 * A.difference(B)
	 * <p>
	 * +-------+            +-------+
	 * |       |            |       |
	 * |   A   |            |       |
	 * |    +--+----+   =   |    +--+
	 * +----+--+    |       +----+
	 *      |   B   |
	 *      |       |
	 *      +-------+
	 * </pre></blockquote>
	 *
	 * @param csgs other csgs
	 * @return difference of this csg and the specified csgs
	 */
	public CSG difference(CSG... csgs) {

		return difference(Arrays.asList(csgs));
	}

	/**
	 * Return a new CSG solid representing the difference of this csg and the specified csg.
	 * <p>
	 * <b>Note:</b> Neither this csg nor the specified csg are weighted.
	 * <p>
	 * <blockquote><pre>
	 * A.difference(B)
	 * <p>
	 * +-------+            +-------+
	 * |       |            |       |
	 * |   A   |            |       |
	 * |    +--+----+   =   |    +--+
	 * +----+--+    |       +----+
	 *      |   B   |
	 *      |       |
	 *      +-------+
	 * </pre></blockquote>
	 *
	 * @param csg other csg
	 * @return difference of this csg and the specified csg
	 */
	public CSG difference(CSG csg) {

		switch (getOptType()) {
			case CSG_BOUND:
				return _differenceCSGBoundsOpt(csg);
			case POLYGON_BOUND:
				return _differencePolygonBoundsOpt(csg);
			default:
				return _differenceNoOpt(csg);
		}
	}

	private CSG _differenceCSGBoundsOpt(CSG csg) {
		CSG b = csg;

		CSG a1 = this._differenceNoOpt(csg.getBounds()
						  .toCSG());
		CSG a2 = this.intersect(csg.getBounds()
					   .toCSG());

		return a2._differenceNoOpt(b)
			 ._unionIntersectOpt(a1)
			 .optimization(getOptType());
	}

	private CSG _differencePolygonBoundsOpt(CSG csg) {
		List<Polygon> inner = new ArrayList<>();
		List<Polygon> outer = new ArrayList<>();

		Bounds bounds = csg.getBounds();

		this.polygons.stream()
			     .forEach((p) -> {
				     if (bounds.intersects(p.getBounds())) {
					     inner.add(p);
				     } else {
					     outer.add(p);
				     }
			     });

		CSG innerCSG = CSG.fromPolygons(inner);

		List<Polygon> allPolygons = new ArrayList<>();
		allPolygons.addAll(outer);
		allPolygons.addAll(innerCSG._differenceNoOpt(csg).polygons);

		return CSG.fromPolygons(allPolygons)
			  .optimization(getOptType());
	}

	private CSG _differenceNoOpt(CSG csg) {

		Node a = new Node(this.clone().polygons);
		Node b = new Node(csg.clone().polygons);

		a.invert();
		a.clipTo(b);
		b.clipTo(a);
		b.invert();
		b.clipTo(a);
		b.invert();
		a.build(b.allPolygons());
		a.invert();

		CSG csgA = CSG.fromPolygons(a.allPolygons())
			      .optimization(getOptType());
		return csgA;
	}

	/**
	 * Return a new CSG solid representing the intersection of this csg and the specified csg.
	 * <p>
	 * <b>Note:</b> Neither this csg nor the specified csg are weighted.
	 * <p>
	 * <blockquote><pre>
	 *     A.intersect(B)
	 * <p>
	 *     +-------+
	 *     |       |
	 *     |   A   |
	 *     |    +--+----+   =   +--+
	 *     +----+--+    |       +--+
	 *          |   B   |
	 *          |       |
	 *          +-------+
	 * }
	 * </pre></blockquote>
	 *
	 * @param csg other csg
	 * @return intersection of this csg and the specified csg
	 */
	public CSG intersect(CSG csg) {

		Node a = new Node(this.clone().polygons);
		Node b = new Node(csg.clone().polygons);
		a.invert();
		b.clipTo(a);
		b.invert();
		a.clipTo(b);
		b.clipTo(a);
		a.build(b.allPolygons());
		a.invert();
		return CSG.fromPolygons(a.allPolygons())
			  .optimization(getOptType());
	}

	/**
	 * Return a new CSG solid representing the intersection of this csg and the specified csgs.
	 * <p>
	 * <b>Note:</b> Neither this csg nor the specified csgs are weighted.
	 * <p>
	 * <blockquote><pre>
	 *     A.intersect(B)
	 * <p>
	 *     +-------+
	 *     |       |
	 *     |   A   |
	 *     |    +--+----+   =   +--+
	 *     +----+--+    |       +--+
	 *          |   B   |
	 *          |       |
	 *          +-------+
	 * }Vector3d
	 * </pre></blockquote>
	 *
	 * @param csgs other csgs
	 * @return intersection of this csg and the specified csgs
	 */
	public CSG intersect(List<CSG> csgs) {

		if (csgs.isEmpty()) {
			return this.clone();
		}

		CSG csgsUnion = csgs.get(0);

		for (int i = 1; i < csgs.size(); i++) {
			csgsUnion = csgsUnion.union(csgs.get(i));
		}

		return intersect(csgsUnion);
	}

	/**
	 * Return a new CSG solid representing the intersection of this csg and the specified csgs.
	 * <p>
	 * <b>Note:</b> Neither this csg nor the specified csgs are weighted.
	 * <p>
	 * <blockquote><pre>
	 *     A.intersect(B)
	 * <p>
	 *     +-------+
	 *     |       |
	 *     |   A   |
	 *     |    +--+----+   =   +--+
	 *     +----+--+    |       +--+Vector3d
	 *          |   B   |
	 *          |       |
	 *          +-------+
	 * }
	 * </pre></blockquote>
	 *
	 * @param csgs other csgs
	 * @return intersection of this csg and the specified csgs
	 */
	public CSG intersect(CSG... csgs) {

		return intersect(Arrays.asList(csgs));
	}


	/**
	 * Returns the bounds of this csg.
	 *
	 * @return bouds of this csg
	 */
	public Bounds getBounds() {

		if (polygons.isEmpty()) {
			return new Bounds(new Vec3(), new Vec3());
		}

		double minX = Double.POSITIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		double minZ = Double.POSITIVE_INFINITY;

		double maxX = Double.NEGATIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;
		double maxZ = Double.NEGATIVE_INFINITY;

		for (Polygon p : getPolygons()) {

			for (int i = 0; i < p.vertices.size(); i++) {

				Vertex vert = p.vertices.get(i);

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

		} // end for polygon

		return new Bounds(new Vec3(minX, minY, minZ), new Vec3(maxX, maxY, maxZ));
	}

	/**
	 * @return the optType
	 */
	private OptType getOptType() {
		return optType != null ? optType : defaultOptType;
	}

	/**
	 * @param optType the optType to set
	 */
	public static void setDefaultOptType(OptType optType) {
		defaultOptType = optType;
	}

	/**
	 * @param optType the optType to set
	 */
	public void setOptType(OptType optType) {
		this.optType = optType;
	}

	public static enum OptType {

		CSG_BOUND,
		POLYGON_BOUND,
		NONE
	}


	public void toMeshBuilder(MeshBuilder builder) {
		try (MeshBuilder b = builder.open()) {
			this.polygons.stream()
				     .forEach(p -> b.nextContour(p.vertices.stream()
									   .map(x -> x.pos)
									   .collect(Collectors.toList())));
		}
	}

	public void toFLine(FLine f)
	{
		this.polygons.stream().forEach(p -> {
			f.data("ml*", p.vertices.stream().map(x -> x.pos).iterator());
		});
	}

	static public CSG fromFLine(FLine f)
	{
		List<FLine> segments = new FLinesAndJavaShapes().segment(f);
		return CSG.fromPolygons(segments.stream().map(l -> new FLinesAndJavaShapes().positions(l, 0.1f)).map(x->new Polygon(x.stream().map(p -> new Vertex(p, new Vec3(0,0,0))).collect(Collectors.toList()))).collect(Collectors.toList()));
	}


}