package field.graphics.csg;

import field.linalg.Vec3;

public class Bounds {

	private final Vec3 center;
	private final Vec3 bounds;
	private final Vec3 min;
	private final Vec3 max;

	/**
	 * Constructor.
	 *
	 * @param min min x,y,z values
	 * @param max max x,y,z values
	 */
	public Bounds(Vec3 min, Vec3 max) {
		this.center = new Vec3(
			    (max.x + min.x) / 2,
			    (max.y + min.y) / 2,
			    (max.z + min.z) / 2);

		this.bounds = new Vec3(
			    Math.abs(max.x - min.x),
			    Math.abs(max.y - min.y),
			    Math.abs(max.z - min.z));

		this.min = min.clone();
		this.max = max.clone();

	}

	public CSG toCSG()
	{
		return new Cuboid(center, bounds).toCSG();
	}


	@Override
	public Bounds clone() {
		return new Bounds(min.clone(), max.clone());
	}

	/**
	 * Returns the position of the center.
	 *
	 * @return the center position
	 */
	public Vec3 getCenter() {
		return center;
	}

	/**
	 * Returns the bounds (width,height,depth).
	 *
	 * @return the bounds (width,height,depth)
	 */
	public Vec3 getBounds() {
		return bounds;
	}


	/**
	 * Indicates whether the specified vertex is contained within this bounding
	 * box (check includes box boundary).
	 *
	 * @param v vertex to check
	 * @return {@code true} if the vertex is contained within this bounding box;
	 * {@code false} otherwise
	 */
	public boolean contains(Vertex v) {
		return contains(v.pos);
	}

	/**
	 * Indicates whether the specified point is contained within this bounding
	 * box (check includes box boundary).
	 *
	 * @param v vertex to check
	 * @return {@code true} if the point is contained within this bounding box;
	 * {@code false} otherwise
	 */
	public boolean contains(Vec3 v) {
		boolean inX = min.x <= v.x && v.x <= max.x;
		boolean inY = min.y <= v.y && v.y <= max.y;
		boolean inZ = min.z <= v.z && v.z <= max.z;

		return inX && inY && inZ;
	}

	/**
	 * Indicates whether the specified polygon is contained within this bounding
	 * box (check includes box boundary).
	 *
	 * @param p polygon to check
	 * @return {@code true} if the polygon is contained within this bounding
	 * box; {@code false} otherwise
	 */
	public boolean contains(Polygon p) {
		return p.vertices.stream().allMatch(v -> contains(v));
	}

	/**
	 * Indicates whether the specified polygon intersects with this bounding box
	 * (check includes box boundary).
	 *
	 * @param p polygon to check
	 * @return {@code true} if the polygon intersects this bounding box;
	 * {@code false} otherwise
	 * @deprecated not implemented yet
	 */
	@Deprecated
	public boolean intersects(Polygon p) {
		throw new UnsupportedOperationException("Implementation missing!");
	}

	/**
	 * Indicates whether the specified bounding box intersects with this
	 * bounding box (check includes box boundary).
	 *
	 * @param b box to check
	 * @return {@code true} if the bounding box intersects this bounding box;
	 * {@code false} otherwise
	 */
	public boolean intersects(Bounds b) {

		if (b.getMin().x > this.getMax().x || b.getMax().x < this.getMin().x) {
			return false;
		}
		if (b.getMin().y > this.getMax().y || b.getMax().y < this.getMin().y) {
			return false;
		}
		if (b.getMin().z > this.getMax().z || b.getMax().z < this.getMin().z) {
			return false;
		}

		return true;

	}

	/**
	 * @return the min x,y,z values
	 */
	public Vec3 getMin() {
		return min;
	}

	/**
	 * @return the max x,y,z values
	 */
	public Vec3 getMax() {
		return max;
	}

	@Override
	public String toString() {
		return "[center: " + center + ", bounds: " + bounds + "]";
	}
}
