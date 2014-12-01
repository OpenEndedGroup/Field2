package field.graphics.csg;

import java.util.List;

/**
 * A primitive geometry.
 *
 * @author Michael Hoffer &lt;info@michaelhoffer.de&gt;
 */
public interface Primitive {

	/**
	 * Returns the polygons that define this primitive.
	 *
	 * <b>Note:</b> this method computes the polygons each time this method is
	 * called. The polygons can be cached inside a {@link CSG} object.
	 *
	 * @return al list of polygons that define this primitive
	 */
	public List<Polygon> toPolygons();

	/**
	 * Returns this primitive as {@link CSG}.
	 *
	 * @return this primitive as {@link CSG}
	 */
	public default CSG toCSG() {
		return CSG.fromPolygons(toPolygons());
	}

}