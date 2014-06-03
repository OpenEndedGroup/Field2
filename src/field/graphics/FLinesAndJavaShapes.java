package field.graphics;

import field.linalg.Vec2;
import field.linalg.Vec3;

import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.util.Iterator;

/** Utility classes for manipulating and traversing FLines.
 *
 * In addition to the tools that exploit the ability to round trip the geometry (but not the attributes) of 2d FLines to java.awt.geom classes (and thus gain quite a bit of 2d computational geometry for no cost), the star here is the Cursor class which solves some key problems in traversing FLines by equal units of distance (rather than node by node). **/
public class FLinesAndJavaShapes {

	static public Shape flineToJavaShape(FLine f) {
		GeneralPath p = new GeneralPath();

		for (FLine.Node n : f.nodes) {
			if (n instanceof FLine.MoveTo) p.moveTo(n.to.x, n.to.y);
			else if (n instanceof FLine.LineTo) p.lineTo(n.to.x, n.to.y);
			else if (n instanceof FLine.CubicTo)
				p.curveTo(((FLine.CubicTo) n).c1.x, ((FLine.CubicTo) n).c1.y, ((FLine.CubicTo) n).c2.x, ((FLine.CubicTo) n).c2.y, n.to.x, n.to.y);

		}
		return p;
	}

	static public Vec2 evaluateCubicFrame(Vec2 a, Vec2 c1, Vec2 c2, Vec2 b, float alpha, Vec2 out) {
		float oma = 1 - alpha;
		float oma2 = oma * oma;
		float oma3 = oma2 * oma;
		float alpha2 = alpha * alpha;
		float alpha3 = alpha2 * alpha;

		out.x = a.x * oma3 + 3 * c1.x * alpha * oma2 + 3 * c2.x * alpha2 * oma + b.x * alpha3;
		out.y = a.y * oma3 + 3 * c1.y * alpha * oma2 + 3 * c2.y * alpha2 * oma + b.y * alpha3;

		return out;
	}

	static public Vec3 evaluateCubicFrame(Vec3 a, Vec3 c1, Vec3 c2, Vec3 b, float alpha, Vec3 out) {
		float oma = 1 - alpha;
		float oma2 = oma * oma;
		float oma3 = oma2 * oma;
		float alpha2 = alpha * alpha;
		float alpha3 = alpha2 * alpha;

		out.x = a.x * oma3 + 3 * c1.x * alpha * oma2 + 3 * c2.x * alpha2 * oma + b.x * alpha3;
		out.y = a.y * oma3 + 3 * c1.y * alpha * oma2 + 3 * c2.y * alpha2 * oma + b.y * alpha3;
		out.z = a.z * oma3 + 3 * c1.z * alpha * oma2 + 3 * c2.z * alpha2 * oma + b.z * alpha3;

		return out;
	}

	public static double ptSegDistSq3(double x1, double y1, double z1, double x2, double y2, double z2, double px, double py, double pz) {
		x2 -= x1;
		y2 -= y1;
		z2 -= z1;
		px -= x1;
		py -= y1;
		pz -= z1;
		double dotprod = px * x2 + py * y2 + pz * z2;
		double projlenSq;
		if (dotprod <= 0.0) {
			projlenSq = 0.0;
		} else {
			px = x2 - px;
			py = y2 - py;
			pz = z2 - pz;
			dotprod = px * x2 + py * y2 + pz * z2;
			if (dotprod <= 0.0) {
				projlenSq = 0.0;
			} else {
				projlenSq = dotprod * dotprod / (x2 * x2 + y2 * y2 + z2 * z2);
			}
		}
		double lenSq = px * px + py * py + pz * pz - projlenSq;
		if (lenSq < 0) {
			lenSq = 0;
		}
		return lenSq;
	}

	static public void splitCubicFrame(Vec2 a, Vec2 c1, Vec2 c2, Vec2 b, float alpha, Vec2 c12, Vec2 m, Vec2 c21, Vec2 tmp) {

		tmp.lerp(c1, c2, alpha);

		evaluateCubicFrame(a, c1, c2, b, alpha, m);

		c1.lerp(a, c1, alpha);
		c12.lerp(c1, tmp, alpha);

		c2.lerp(c2, b, alpha);
		c21.lerp(tmp, c2, alpha);
	}

	static public void splitCubicFrame3(Vec3 a, Vec3 c1, Vec3 c2, Vec3 b, float alpha, Vec3 c12, Vec3 m, Vec3 c21, Vec3 tmp) {

		tmp.lerp(c1, c2, alpha);

		evaluateCubicFrame(a, c1, c2, b, alpha, m);

		c1.lerp(a, c1, alpha);
		c12.lerp(c1, tmp, alpha);

		c2.lerp(c2, b, alpha);
		c21.lerp(tmp, c2, alpha);
	}


	static public FLine javaShapeToFLine(Shape f) {
		PathIterator pi = f.getPathIterator(null);
		float[] cc = new float[6];
		Vec2 lastAt = null;
		Vec2 lastMoveTo = null;

		FLine in = new FLine();
		while (!pi.isDone()) {
			int ty = pi.currentSegment(cc);
			if (ty == PathIterator.SEG_CLOSE) {
				if (lastMoveTo != null && lastAt.distanceFrom(lastMoveTo) > 1e-6)
					in.lineTo(lastMoveTo.x, lastMoveTo.y);
				lastAt = null;
			} else if (ty == PathIterator.SEG_CUBICTO) {
				if (lastAt == null || (Math.abs(lastAt.x - cc[4]) + Math.abs(lastAt.y - cc[5]) > 1e-15))
					in.cubicTo(cc[0], cc[1], cc[2], cc[3], cc[4], cc[5]);
				if (lastAt == null) lastAt = new Vec2(cc[4], cc[5]);
				else {
					lastAt.x = cc[4];
					lastAt.y = cc[5];
				}
			} else if (ty == PathIterator.SEG_LINETO) {
				if (lastAt == null || (Math.abs(lastAt.x - cc[0]) + Math.abs(lastAt.y - cc[1]) > 1e-15))
					in.lineTo(cc[0], cc[1]);
				if (lastAt == null) lastAt = new Vec2(cc[0], cc[1]);
				else {
					lastAt.x = cc[0];
					lastAt.y = cc[1];
				}
			} else if (ty == PathIterator.SEG_MOVETO) {
				if (lastAt == null || (Math.abs(lastAt.x - cc[0]) + Math.abs(lastAt.y - cc[1]) > 1e-15))
					in.moveTo(cc[0], cc[1]);

				lastMoveTo = new Vec2(cc[0], cc[1]);

				if (lastAt == null) lastAt = new Vec2(cc[0], cc[1]);
				else {
					lastAt.x = cc[0];
					lastAt.y = cc[1];
				}
			} else if (ty == PathIterator.SEG_QUADTO) {
				if (lastAt == null || (Math.abs(lastAt.x - cc[2]) + Math.abs(lastAt.y - cc[3]) > 1e-15))
					in.cubicTo((cc[0] - lastAt.x) * (2 / 3f) + lastAt.x, (cc[1] - lastAt.y) * (2 / 3f) + lastAt.y, (cc[0] - cc[2]) * (2 / 3f) + cc[2], (cc[1] - cc[3]) * (2 / 3f) + cc[3], cc[2], cc[3]);

				if (lastAt == null) lastAt = new Vec2(cc[2], cc[3]);
				else {
					lastAt.x = cc[2];
					lastAt.y = cc[3];
				}

			}

			pi.next();

		}
		return in;
	}


	static public FLine insetShape(FLine f, float amount) {
		Shape s = flineToJavaShape(f);
		Shape ss = new BasicStroke(amount, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND).createStrokedShape(s);

		Area sa = new Area(s);
		Area ssa = new Area(ss);
		sa.subtract(ssa);
		return javaShapeToFLine(sa);
	}


	/**
	 * A class for navigating FLines either by distance "D" or by node "T". Incomplete, needs port of pathflattener3 for distance calculations.
	 */
	static public class Cursor implements Iterator<Vec3[]> {
		protected final FLine on;
		private final float tol;

		// lazy inited
		protected PathFlattener p = null;

		protected long pAt;

		public Cursor(FLine on, float tol) {
			this.on = on;
			this.tol = tol;
			pAt = on.getModCount();
		}

		protected int index;
		protected float alpha;

		public boolean segmentIsLinear() {
			return on.nodes.get(clamp(index + 1)) instanceof FLine.LineTo;
		}

		public boolean segmentIsCubic() {
			return on.nodes.get(clamp(index + 1)) instanceof FLine.CubicTo;
		}

		public boolean segmentIsMove() {
			return on.nodes.get(clamp(index + 1)) instanceof FLine.MoveTo;
		}

		public boolean hasNext() {
			return index < on.nodes.size() - 1;
		}

		public Vec3[] next() {
			if (segmentIsCubic())
				return new Vec3[]{new Vec3(on.nodes.get(clamp(index)).to), new Vec3(((FLine.CubicTo) on.nodes.get(clamp(index + 1))).c1), new Vec3(((FLine.CubicTo) on.nodes.get(clamp(index + 1))).c2), new Vec3(((FLine.CubicTo) on.nodes.get(clamp(index + 1))).to)};
			else if (segmentIsLinear())
				return new Vec3[]{new Vec3(on.nodes.get(clamp(index)).to), new Vec3(on.nodes.get(clamp(index + 1)).to)};
			else if (segmentIsMove()) return new Vec3[]{new Vec3(on.nodes.get(clamp(index)).to)};
			else throw new IllegalStateException(" segment not implemented ");

		}

		public void reset() {
			index = 0;
		}

		public Vec3 position() {
			if (segmentIsMove()) {
				if (alpha < 0.5) return new Vec3(on.nodes.get(clamp(index)).to);
				else return new Vec3(on.nodes.get(clamp(index + 1)).to);
			} else if (segmentIsLinear()) {
				return Vec3.lerp(on.nodes.get(clamp(index)).to, on.nodes.get(clamp(index + 1)).to, alpha, null);
			} else if (segmentIsCubic()) {
				return evaluateCubicFrame(on.nodes.get(clamp(index)).to, ((FLine.CubicTo) on.nodes.get(clamp(index + 1))).c1, ((FLine.CubicTo) on.nodes.get(clamp(index + 1))).c2, ((FLine.CubicTo) on.nodes.get(clamp(index + 1))).to, alpha, new Vec3());
			} else throw new IllegalStateException(" segment not implemented ");
		}

		/**
		 * length in node units.
		 *
		 * @return
		 */
		public float lengthT() {
			return on.nodes.size();
		}

		/**
		 * length in distance
		 */
		public float lengthD()
		{
			return (float)getPathFlattener().length();
		}

		/** get current position as distance along line */
		public float getD()
		{
			return (float) getPathFlattener().dotToLength(index+alpha);
		}

		/** Get current position in node.t format (so 0 is the start of the line, 1 is the first node 1.5 is roughly (but certainly not exactly) half-way between the first and second node, and so on.
		 * The key trouble here is that while evalLinear(x,y, alpha) parameterizes linear segments evently, evalCubic(x, c1, c2, y, alpha) certainly doesn't and alpha=0.5 doesn't mean half-way along in terms of distance. If you can about even distribution use the much slower but accurate setD and getD. */
		public float getT()
		{
			return index+alpha;
		}

		/** set the current position as a distance along this line. Returns position in node.t format*/
		public float setD(float d)
		{
			double q = getPathFlattener().lengthToDot(d);
			index = (int)q;
			alpha = (float)(q-index);
			return index+alpha;
		}

		/** set the current position as a distance along this line. Returns position in node.t format*/
		public float setT(float q)
		{
			index = (int)q;
			alpha = (float)(q-index);
			return index+alpha;
		}

		protected PathFlattener getPathFlattener()
		{
			if (p==null) return p = new PathFlattener(this.on, tol);
			return p;
		}

		private int clamp(int index) {
			if (index >= on.nodes.size()) return on.nodes.size() - 1;
			if (index < 0) return 0;
			return index;
		}

	}

}
