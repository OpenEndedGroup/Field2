package field.graphics.csg;

import field.linalg.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * A solid sphere.
 *
 * Tthe tessellation along the longitude and latitude directions can be
 * controlled via the {@link #numSlices} and {@link #numStacks} parameters.
 *
 * @author Michael Hoffer &lt;info@michaelhoffer.de&gt;
 */
public class Sphere implements Primitive {

	private Vec3 center;
	private double radius;
	private int numSlices;
	private int numStacks;

	/**
	 * Constructor. Creates a sphere with radius 1, 16 slices and 8 stacks and
	 * center [0,0,0].
	 *
	 */
	public Sphere() {
		init();
	}

	/**
	 * Constructor. Creates a sphere with the specified radius, 16 slices and 8
	 * stacks and center [0,0,0].
	 *
	 * @param radius sphare radius
	 */
	public Sphere(double radius) {
		init();
		this.radius = radius;
	}

	/**
	 * Constructor. Creates a sphere with the specified radius, number of slices
	 * and stacks.
	 *
	 * @param radius sphare radius
	 * @param numSlices number of slices
	 * @param numStacks number of stacks
	 */
	public Sphere(double radius, int numSlices, int numStacks) {
		init();
		this.radius = radius;
		this.numSlices = numSlices;
		this.numStacks = numStacks;
	}

	/**
	 * Constructor. Creates a sphere with the specified center, radius, number
	 * of slices and stacks.
	 *
	 * @param center center of the sphere
	 * @param radius sphere radius
	 * @param numSlices number of slices
	 * @param numStacks number of stacks
	 */
	public Sphere(Vec3 center, double radius, int numSlices, int numStacks) {
		this.center = center;
		this.radius = radius;
		this.numSlices = numSlices;
		this.numStacks = numStacks;
	}

	private void init() {
		center = new Vec3(0, 0, 0);
		radius = 1;
		numSlices = 16;
		numStacks = 8;
	}

	private Vertex sphereVertex(Vec3 c, double r, double theta, double phi) {
		theta *= Math.PI * 2;
		phi *= Math.PI;
		Vec3 dir = new Vec3(
			    Math.cos(theta) * Math.sin(phi),
			    Math.cos(phi),
			    Math.sin(theta) * Math.sin(phi)
		);
		return new Vertex(Vec3.add(c, r, dir, new Vec3()), dir);
	}

	@Override
	public List<Polygon> toPolygons() {
		List<Polygon> polygons = new ArrayList<>();

		for (int i = 0; i < numSlices; i++) {
			for (int j = 0; j < numStacks; j++) {
				final List<Vertex> vertices = new ArrayList<>();

				vertices.add(
					    sphereVertex(center, radius, i / (double) numSlices,
							j / (double) numStacks)
				);
				if (j > 0) {
					vertices.add(
						    sphereVertex(center, radius, (i + 1) / (double) numSlices,
								j / (double) numStacks)
					);
				}
				if (j < numStacks - 1) {
					vertices.add(
						    sphereVertex(center, radius, (i + 1) / (double) numSlices,
								(j + 1) / (double) numStacks)
					);
				}
				vertices.add(
					    sphereVertex(center, radius, i / (double) numSlices,
							(j + 1) / (double) numStacks)
				);
				polygons.add(new Polygon(vertices));
			}
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
	 * @return the radius
	 */
	public double getRadius() {
		return radius;
	}

	/**
	 * @param radius the radius to set
	 */
	public void setRadius(double radius) {
		this.radius = radius;
	}

	/**
	 * @return the numSlices
	 */
	public int getNumSlices() {
		return numSlices;
	}

	/**
	 * @param numSlices the numSlices to set
	 */
	public void setNumSlices(int numSlices) {
		this.numSlices = numSlices;
	}

	/**
	 * @return the numStacks
	 */
	public int getNumStacks() {
		return numStacks;
	}

	/**
	 * @param numStacks the numStacks to set
	 */
	public void setNumStacks(int numStacks) {
		this.numStacks = numStacks;
	}

}