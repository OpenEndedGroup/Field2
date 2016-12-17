package field.graphics.csg;

import field.linalg.Vec3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Extrudes concave and convex polygons.
 *
 * @author Michael Hoffer &lt;info@michaelhoffer.de&gt;
 */
public class Extrude {

	private Extrude() {
		throw new AssertionError("Don't instantiate me!", null);
	}

	/**
	 * Extrudes the specified path (convex or concave polygon without holes or
	 * intersections, specified in CCW) into the specified direction.
	 *
	 * @param dir direction
	 * @param points path (convex or concave polygon without holes or
	 * intersections)
	 *
	 * @return a CSG object that consists of the extruded polygon
	 */
	public static CSG points(Vec3 dir, Vec3... points) {

		return extrude(dir, Polygon.fromPoints(toCCW(Arrays.asList(points))));
	}

	/**
	 * Extrudes the specified path (convex or concave polygon without holes or
	 * intersections, specified in CCW) into the specified direction.
	 *
	 * @param dir direction
	 * @param points path (convex or concave polygon without holes or
	 * intersections)
	 *
	 * @return a CSG object that consists of the extruded polygon
	 */
	public static CSG points(Vec3 dir, List<Vec3> points) {

		List<Vec3> newList = new ArrayList<>(points);

		return extrude(dir, Polygon.fromPoints(toCCW(newList)));
	}

	private static CSG extrude(Vec3 dir, Polygon polygon1) {
		List<Polygon> newPolygons = new ArrayList<>();

		if (dir.z<0) {
			throw new IllegalArgumentException("z < 0 currently not supported for extrude: " + dir);
		}

		newPolygons.addAll(Polygon.concaveToConvex(polygon1));
		Polygon polygon2 = polygon1.translated(dir);

		int numvertices = polygon1.vertices.size();
		for (int i = 0; i < numvertices; i++) {

			int nexti = (i + 1) % numvertices;

			Vec3 bottomV1 = polygon1.vertices.get(i).pos;
			Vec3 topV1 = polygon2.vertices.get(i).pos;
			Vec3 bottomV2 = polygon1.vertices.get(nexti).pos;
			Vec3 topV2 = polygon2.vertices.get(nexti).pos;

			List<Vec3> pPoints = Arrays.asList(bottomV2, topV2, topV1, bottomV1);

			newPolygons.add(Polygon.fromPoints(pPoints));

		}

		polygon2 = polygon2.flipped();
		List<Polygon> topPolygons = Polygon.concaveToConvex(polygon2);

		newPolygons.addAll(topPolygons);

		return CSG.fromPolygons(newPolygons);

	}

	static List<Vec3> toCCW(List<Vec3> points) {

		List<Vec3> result = new ArrayList<>(points);

		if (!Polygon.isCCW(Polygon.fromPoints(result))) {
			Collections.reverse(result);
		}

		return result;
	}

	static List<Vec3> toCW(List<Vec3> points) {

		List<Vec3> result = new ArrayList<>(points);

		if (Polygon.isCCW(Polygon.fromPoints(result))) {
			Collections.reverse(result);
		}

		return result;
	}

}