package field.graphics.csg;

import field.linalg.Vec3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by marc on 10/2/14.
 */
public class Cylinder implements Primitive {

	private Vec3 start;
	private Vec3 end;
	private double radius;
	private int numSlices;


	/**
	 * Constructor. Creates a new cylinder with center {@code [0,0,0]} and
	 * ranging from {@code [0,-0.5,0]} to {@code [0,0.5,0]}, i.e.
	 * {@code size = 1}.
	 */
	public Cylinder() {
		this.start = new Vec3(0, -0.5, 0);
		this.end = new Vec3(0, 0.5, 0);
		this.radius = 1;
		this.numSlices = 16;
	}

	/**
	 * Constructor. Creates a cylinder ranging from {@code start} to {@code end}
	 * with the specified {@code radius}. The resolution of the tessellation can
	 * be controlled with {@code numSlices}.
	 *
	 * @param start cylinder start
	 * @param end cylinder end
	 * @param radius cylinder radius
	 * @param numSlices number of slices (used for tessellation)
	 */
	public Cylinder(Vec3 start, Vec3 end, double radius, int numSlices) {
		this.start = start;
		this.end = end;
		this.radius = radius;
		this.numSlices = numSlices;
	}

	/**
	 * Constructor. Creates a cylinder ranging from {@code [0,0,0]} to {@code [0,0,height]}
	 * with the specified {@code radius} and {@code height}. The resolution of the tessellation can
	 * be controlled with {@code numSlices}.
	 *
	 * @param radius cylinder radius
	 * @param height cylinder height
	 * @param numSlices number of slices (used for tessellation)
	 */
	public Cylinder(double radius, double height, int numSlices) {
		this.start = new Vec3();
		this.end = new Vec3(0,0,height);
		this.radius = radius;
		this.numSlices = numSlices;
	}

	@Override
	public List<Polygon> toPolygons() {
		final Vec3 s = getStart();
		Vec3 e = getEnd();
		final Vec3 ray = Vec3.sub(e, s, new Vec3());
		final Vec3 axisZ = new Vec3(ray).normalise();
		boolean isY = (Math.abs(axisZ.y) > 0.5);
		final Vec3 axisX = Vec3.cross(new Vec3(isY ? 1 : 0, !isY ? 1 : 0, 0), axisZ, new Vec3()).normalise();
		final Vec3 axisY = Vec3.cross(axisX, axisZ, new Vec3()).normalise();
		Vertex startV = new Vertex(s, new Vec3(axisZ).scale(-1));
		Vertex endV = new Vertex(e, new Vec3(axisZ).normalise());
		List<Polygon> polygons = new ArrayList<>();

		for (int i = 0; i < numSlices; i++) {
			double t0 = i / (double) numSlices, t1 = (i + 1) / (double) numSlices;
			polygons.add(new Polygon(Arrays.asList(startV, cylPoint(axisX, axisY, axisZ, ray, s, radius, 0, t0, -1), cylPoint(axisX, axisY, axisZ, ray, s, radius, 0, t1, -1))
			));
			polygons.add(new Polygon(Arrays.asList(
				    cylPoint(axisX, axisY, axisZ, ray, s, radius, 0, t1, 0),
				    cylPoint(axisX, axisY, axisZ, ray, s, radius, 0, t0, 0),
				    cylPoint(axisX, axisY, axisZ, ray, s, radius, 1, t0, 0),
				    cylPoint(axisX, axisY, axisZ, ray, s, radius, 1, t1, 0))			));
			polygons.add(new Polygon(
						Arrays.asList(
							    endV,
							    cylPoint(axisX, axisY, axisZ, ray, s, radius, 1, t1, 1),
							    cylPoint(axisX, axisY, axisZ, ray, s, radius, 1, t0, 1))				    )
			);
		}

		return polygons;
	}

	private Vertex cylPoint(
		    Vec3 axisX, Vec3 axisY, Vec3 axisZ, Vec3 ray, Vec3 s,
		    double r, double stack, double slice, double normalBlend) {
		double angle = slice * Math.PI * 2;
		Vec3 out = Vec3.add(new Vec3(axisX).scale(Math.cos(angle)), (new Vec3(axisY).scale(Math.sin(angle))), new Vec3());
		Vec3 pos = Vec3.add(Vec3.add(s, new Vec3(ray).scale(stack), new Vec3()), new Vec3(out).scale(r), new Vec3());
		Vec3 normal = Vec3.add(new Vec3(out).scale(1.0 - Math.abs(normalBlend)), new Vec3(axisZ).scale(normalBlend), new Vec3());
		return new Vertex(pos, normal);
	}

	/**
	 * @return the start
	 */
	public Vec3 getStart() {
		return start;
	}

	/**
	 * @param start the start to set
	 */
	public void setStart(Vec3 start) {
		this.start = start;
	}

	/**
	 * @return the end
	 */
	public Vec3 getEnd() {
		return end;
	}

	/**
	 * @param end the end to set
	 */
	public void setEnd(Vec3 end) {
		this.end = end;
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
	 * @return the number of slices
	 */
	public int getNumSlices() {
		return numSlices;
	}

	/**
	 * @param numSlices the number of slices to set
	 */
	public void setNumSlices(int numSlices) {
		this.numSlices = numSlices;
	}


}