package field.utility;

import field.linalg.Vec2;
import field.linalg.Vec3;

/**
 * A 3d rectangle
 */
public class Cuboid implements Mutable<Cuboid> {

	public float x;
	public float y;
	public float z;
	public float w;
	public float h;
	public float d;

	public Cuboid(double x, double y, double z, double w, double h, double d) {
		this.x = (float) x;
		this.y = (float) y;
		this.h = (float) h;
		this.w = (float) w;
		this.d = (float) d;
		this.z = (float) z;
	}

	public static Cuboid union(Cuboid r, Cuboid rect) {
		if (r == null) return rect;
		if (rect == null) return r;
		return r.union(rect);
	}

	public Cuboid union(Cuboid r) {
		if (r == null) return this;
		float minx = Math.min(r.x, x);
		float miny = Math.min(r.y, y);
		float minz = Math.min(r.z, z);

		float maxx = Math.max(r.x + r.w, x + w);
		float maxy = Math.max(r.y + r.h, y + h);
		float maxz = Math.max(r.z + r.d, z + d);

		return new Cuboid(minx, miny, minz, maxx - minx, maxy - miny, maxz - minz);
	}

	public Cuboid translate(Vec2 by) {
		return new Cuboid(x + by.x, y + by.y, z, w, h, d);
	}

	public Cuboid translate(Vec3 by) {
		return new Cuboid(x + by.x, y + by.y, z + by.z, w, h, d);
	}

	public boolean intersects(Vec3 point) {
		return point.x >= x && point.x <= (x + w) && point.y >= y && point.y <= (y + h) && point.z >= z && point.z <= (z + d);
	}

	public boolean intersects(Cuboid r) {
		return (r.x < this.x + this.w && r.w + r.x > this.x && r.y < this.y + this.h && r.y + r.h > this.y && r.z < this.z + this.d && r.z + r.d > this.z);
	}


	@Override
	public String toString() {
		return "Cuboid{" +
			    "x=" + x +
			    ", y=" + y +
			    ", z=" + z +
			    ", w=" + w +
			    ", h=" + h +
			    ", d=" + d +
			    '}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Cuboid)) return false;

		Cuboid rect = (Cuboid) o;

		if (Float.compare(rect.h, h) != 0) return false;
		if (Float.compare(rect.w, w) != 0) return false;
		if (Float.compare(rect.x, x) != 0) return false;
		if (Float.compare(rect.y, y) != 0) return false;

		if (Float.compare(rect.z, d) != 0) return false;
		if (Float.compare(rect.d, z) != 0) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = (x != +0.0f ? Float.floatToIntBits(x) : 0);
		result = 31 * result + (y != +0.0f ? Float.floatToIntBits(y) : 0);
		result = 31 * result + (w != +0.0f ? Float.floatToIntBits(w) : 0);
		result = 31 * result + (h != +0.0f ? Float.floatToIntBits(h) : 0);
		result = 31 * result + (w != +0.0f ? Float.floatToIntBits(z) : 0);
		result = 31 * result + (h != +0.0f ? Float.floatToIntBits(d) : 0);
		return result;
	}

	public Cuboid inset(float by) {
		return new Cuboid(x + by, y + by, z + by, w - by * 2, h - by * 2, d - by * 2);
	}

	public boolean intersectsX(float x) {
		return (x >= this.x && x < this.x + this.w);
	}

	public boolean inside(float start, float end) {
		return this.x >= start && this.x + this.w < end;
	}

	public Vec3 convert(double x, double y, double z) {
		return new Vec3(this.x + x * this.w, this.y + y * this.h, this.z + z * this.d);
	}

	@Override
	public Cuboid duplicate() {
		return new Cuboid(x, y, z, w, h, d);
	}

}

