package field.utility;

import field.linalg.Vec2;

/**
 * A 2D Rectangle
 */
public class Rect implements Mutable<Rect> {

	public float x;
	public float y;
	public float w;
	public float h;

	public Rect(double x,  double y, double w, double h) {
		this.x = (float)x;
		this.y = (float)y;
		this.h = (float)h;
		this.w = (float)w;
	}

	public static Rect union(Rect r, Rect rect) {
		if (r == null)
			return rect;
		if (rect == null)
			return r;
		return r.union(rect);
	}

	public Rect union(Rect r) {
		if (r==null) return this;
		float minx = Math.min(r.x, x);
		float miny = Math.min(r.y, y);

		float maxx = Math.max(r.x + r.w, x + w);
		float maxy = Math.max(r.y + r.h, y + h);

		return new Rect(minx, miny, maxx - minx, maxy - miny);
	}

	public Rect translate(Vec2 by)
	{
		return new Rect(x+by.x, y+by.y, w, h);
	}

	public boolean intersects(Vec2 point) {
		return point.x>=x && point.x<=(x+w) && point.y>=y && point.y<=(y+h);
	}

	public boolean intersects(Rect r) {
		return (r.x<this.x+this.w && r.w+r.x>this.x && r.y<this.y+this.h && r.y+r.h>this.y);
	}


	@Override
	public String toString() {
		return "Rect{" +
			    "x=" + x +
			    ", y=" + y +
			    ", w=" + w +
			    ", h=" + h +
			    '}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Rect)) return false;

		Rect rect = (Rect) o;

		if (Float.compare(rect.h, h) != 0) return false;
		if (Float.compare(rect.w, w) != 0) return false;
		if (Float.compare(rect.x, x) != 0) return false;
		if (Float.compare(rect.y, y) != 0) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = (x != +0.0f ? Float.floatToIntBits(x) : 0);
		result = 31 * result + (y != +0.0f ? Float.floatToIntBits(y) : 0);
		result = 31 * result + (w != +0.0f ? Float.floatToIntBits(w) : 0);
		result = 31 * result + (h != +0.0f ? Float.floatToIntBits(h) : 0);
		return result;
	}

	public Rect inset(float by) {
		return new Rect(x+by, y+by, w-by*2, h-by*2);
	}

	public boolean intersectsX(float x) {
		return (x>=this.x && x<this.x+this.w);
	}

	public boolean inside(float start, float end) {
		return this.x>=start && this.x+this.w<end;
	}

	public Vec2 convert(double x, double y)
	{
		return new Vec2(this.x+x*this.w, this.y+y*this.h);
	}

	@Override
	public Rect duplicate() {
		return new Rect(x,y,w,h);
	}

	public double area()
	{
		return w*h;
	}

}
