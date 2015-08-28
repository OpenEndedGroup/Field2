package field.graphics;

import field.linalg.Quat;
import field.linalg.Vec2;
import field.linalg.Vec3;
import field.utility.*;
import fieldbox.boxes.Box;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.univariate.BrentOptimizer;
import org.apache.commons.math3.optim.univariate.SearchInterval;
import org.apache.commons.math3.optim.univariate.UnivariateObjectiveFunction;
import org.apache.commons.math3.optim.univariate.UnivariatePointValuePair;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.util.*;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Utility classes for manipulating and traversing FLines.
 * <p>
 * In addition to the tools that exploit the ability to round trip the geometry (but not the attributes) of 2d FLines to java.awt.geom classes (and thus gain quite a bit of 2d computational geometry
 * for no cost), the star here is the Cursor class which solves some key problems in traversing FLines by equal units of distance (rather than node by node). *
 */
public class FLinesAndJavaShapes {

	private static final double angle = Math.PI / 4.0;
	private static final double a = 1.0 - Math.cos(angle);
	private static final double b = Math.tan(angle);
	private static final double c = Math.sqrt(1.0 + b * b) - 1 + a;
	private static final double cv = 4.0 / 3.0 * a * b / c;
	private static final double acv = (1.0 - cv) / 2.0;
	// For each array:
	//     4 values for each point {v0, v1, v2, v3}:
	//         point = (x + v0 * w + v1 * arcWidth,
	//                  y + v2 * h + v3 * arcHeight);
	private static double ctrlpts[][]
		    = {{0.0, 0.0, 0.0, 0.5}, {0.0, 0.0, 1.0, -0.5}, {0.0, 0.0, 1.0, -acv, 0.0, acv, 1.0, 0.0, 0.0, 0.5, 1.0, 0.0}, {1.0, -0.5, 1.0, 0.0}, {1.0, -acv, 1.0, 0.0, 1.0, 0.0, 1.0, -acv, 1.0, 0.0, 1.0, -0.5}, {1.0, 0.0, 0.0, 0.5}, {1.0, 0.0, 0.0, acv, 1.0, -acv, 0.0, 0.0, 1.0, -0.5, 0.0, 0.0}, {0.0, 0.5, 0.0, 0.0}, {0.0, acv, 0.0, 0.0, 0.0, 0.0, 0.0, acv, 0.0, 0.0, 0.0, 0.5}, {},};

	static public Shape flineToJavaShape(FLine f) {

		if (f.attributes.has(StandardFLineDrawing.thicken)) {
			return f.attributes.get(StandardFLineDrawing.thicken).createStrokedShape(_flineToJavaShape(f));
		}

		return _flineToJavaShape(f);
	}

	static public Shape flineToJavaShape_notThickened(FLine f) {

		return _flineToJavaShape(f);
	}



	private static GeneralPath _flineToJavaShape(FLine f) {
		GeneralPath p = new GeneralPath();

		for (FLine.Node n : f.nodes) {
			if (n instanceof FLine.MoveTo) p.moveTo(n.to.x, n.to.y);
			else if (n instanceof FLine.LineTo) p.lineTo(n.to.x, n.to.y);
			else if (n instanceof FLine.CubicTo) p.curveTo(((FLine.CubicTo) n).c1.x, ((FLine.CubicTo) n).c1.y, ((FLine.CubicTo) n).c2.x, ((FLine.CubicTo) n).c2.y, n.to.x, n.to.y);

		}
		return p;

	}

	static public Shape flineToJavaShape(Collection<FLine> f)
	{
		GeneralPath p = new GeneralPath();


		for (FLine ff : f) {
			p.append(flineToJavaShape(ff), false);
		}
		return p;
	}

	static public Vec2 evaluateCubicFrame(Vec2 a, Vec2 c1, Vec2 c2, Vec2 b, double alpha, Vec2 out) {
		double oma = 1 - alpha;
		double oma2 = oma * oma;
		double oma3 = oma2 * oma;
		double alpha2 = alpha * alpha;
		double alpha3 = alpha2 * alpha;

		out.x = a.x * oma3 + 3 * c1.x * alpha * oma2 + 3 * c2.x * alpha2 * oma + b.x * alpha3;
		out.y = a.y * oma3 + 3 * c1.y * alpha * oma2 + 3 * c2.y * alpha2 * oma + b.y * alpha3;

		return out;
	}

	/**
	 * returns both the evaluation (x) and it's derivative (y)
	 */

	static public Vec2 evaluateCubicFrame(double a, double c1, double c2, double b, double alpha) {
		double oma = 1 - alpha;
		double oma2 = oma * oma;
		double oma3 = oma2 * oma;
		double alpha2 = alpha * alpha;
		double alpha3 = alpha2 * alpha;

		Vec2 out = new Vec2();
		out.x = a * oma3 + 3 * c1 * alpha * oma2 + 3 * c2 * alpha2 * oma + b * alpha3;

		oma3 = -3 * (1 - alpha) * (1 - alpha);
		double alphaoma2 = 3 * alpha * alpha - 4 * alpha + 1;
		double alpha2oma = alpha * (2 - 3 * alpha);
		alpha3 = 3 * alpha * alpha;

		out.y = a * oma3 + 3 * c1 * alphaoma2 + 3 * c2 * alpha2oma + b * alpha3;

		return out;
	}

	/**
	 * returns both the derivative (x) and it's second derivative (y)
	 */

	static public Vec2 evaluateDCubicFrame(double a, double c1, double c2, double b, double alpha) {

		Vec2 out = new Vec2();

		double oma3 = -3 * (1 - alpha) * (1 - alpha);
		double alphaoma2 = 3 * alpha * alpha - 4 * alpha + 1;
		double alpha2oma = alpha * (2 - 3 * alpha);
		double alpha3 = 3 * alpha * alpha;

		out.x = a * oma3 + 3 * c1 * alphaoma2 + 3 * c2 * alpha2oma + b * alpha3;


		oma3 = -2 + 2 * alpha;
		alphaoma2 = 6 * alpha - 4;
		alpha2oma = 2 - 6 * alpha;
		alpha3 = 6 * alpha;

		out.y = a * oma3 + 3 * c1 * alphaoma2 + 3 * c2 * alpha2oma + b * alpha3;

		return out;
	}

	static public Vec3 evaluateCubicFrame(Vec3 a, Vec3 c1, Vec3 c2, Vec3 b, double alpha, Vec3 out) {
		double oma = 1 - alpha;
		double oma2 = oma * oma;
		double oma3 = oma2 * oma;
		double alpha2 = alpha * alpha;
		double alpha3 = alpha2 * alpha;

		out.x = a.x * oma3 + 3 * c1.x * alpha * oma2 + 3 * c2.x * alpha2 * oma + b.x * alpha3;
		out.y = a.y * oma3 + 3 * c1.y * alpha * oma2 + 3 * c2.y * alpha2 * oma + b.y * alpha3;
		out.z = a.z * oma3 + 3 * c1.z * alpha * oma2 + 3 * c2.z * alpha2 * oma + b.z * alpha3;

		return out;
	}

	static public Vec3 evaluateDCubicFrame(Vec3 a, Vec3 c1, Vec3 c2, Vec3 b, double alpha, Vec3 out) {
		double oma3 = -3 * (1 - alpha) * (1 - alpha);
		double alphaoma2 = 3 * alpha * alpha - 4 * alpha + 1;
		double alpha2oma = alpha * (2 - 3 * alpha);
		double alpha3 = 3 * alpha * alpha;

		out.x = a.x * oma3 + 3 * c1.x * alphaoma2 + 3 * c2.x * alpha2oma + b.x * alpha3;
		out.y = a.y * oma3 + 3 * c1.y * alphaoma2 + 3 * c2.y * alpha2oma + b.y * alpha3;
		out.z = a.z * oma3 + 3 * c1.z * alphaoma2 + 3 * c2.z * alpha2oma + b.z * alpha3;

		return out;
	}

	static public Vec3 evaluateDDCubicFrame(Vec3 a, Vec3 c1, Vec3 c2, Vec3 b, double alpha, Vec3 out) {
		double oma3 = 6 * (1 - alpha);
		double alphaoma2 = 6 * alpha - 4;
		double alpha2oma = (2 - 6 * alpha);
		double alpha3 = 6 * alpha;

		out.x = a.x * oma3 + 3 * c1.x * alphaoma2 + 3 * c2.x * alpha2oma + b.x * alpha3;
		out.y = a.y * oma3 + 3 * c1.y * alphaoma2 + 3 * c2.y * alpha2oma + b.y * alpha3;
		out.z = a.z * oma3 + 3 * c1.z * alphaoma2 + 3 * c2.z * alpha2oma + b.z * alpha3;

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


	/**
	 * mutates c1 and c2
	 */
	static public void splitCubicFrame(Vec2 a, Vec2 c1, Vec2 c2, Vec2 b, double alpha, Vec2 c12, Vec2 m, Vec2 c21, Vec2 tmp) {

		tmp.lerp(c1, c2, alpha);

		evaluateCubicFrame(a, c1, c2, b, alpha, m);

		c1.lerp(a, c1, alpha);
		c12.lerp(c1, tmp, alpha);

		c2.lerp(c2, b, alpha);
		c21.lerp(tmp, c2, alpha);
	}

	/**
	 * mutates c1 and c2
	 */
	static public void splitCubicFrame(Vec3 a, Vec3 c1, Vec3 c2, Vec3 b, double alpha, Vec3 c12, Vec3 m, Vec3 c21, Vec3 tmp) {

		tmp.lerp(c1, c2, alpha);

		evaluateCubicFrame(a, c1, c2, b, alpha, m);

		c1.lerp(a, c1, alpha);
		c12.lerp(c1, tmp, alpha);

		c2.lerp(c2, b, alpha);
		c21.lerp(tmp, c2, alpha);
	}

	static public FLine javaShapeToFLine(Shape f) {
		return javaShapeToFLine(f, AffineTransform.getTranslateInstance(0,0));
	}

	static public FLine javaShapeToFLine(Shape f, AffineTransform at) {
		PathIterator pi = f.getPathIterator(at);
		float[] cc = new float[6];
		Vec2 lastAt = null;
		Vec2 lastMoveTo = null;

		FLine in = new FLine();
		while (!pi.isDone()) {
			int ty = pi.currentSegment(cc);
			if (ty == PathIterator.SEG_CLOSE) {
				if (lastMoveTo != null && lastAt.distance(lastMoveTo) > 1e-6) in.lineTo(lastMoveTo.x, lastMoveTo.y);
				lastAt = null;
			} else if (ty == PathIterator.SEG_CUBICTO) {
				if (lastAt == null || (Math.abs(lastAt.x - cc[4]) + Math.abs(lastAt.y - cc[5]) > 1e-15)) in.cubicTo(cc[0], cc[1], cc[2], cc[3], cc[4], cc[5]);
				if (lastAt == null) lastAt = new Vec2(cc[4], cc[5]);
				else {
					lastAt.x = cc[4];
					lastAt.y = cc[5];
				}
			} else if (ty == PathIterator.SEG_LINETO) {
				if (lastAt == null || (Math.abs(lastAt.x - cc[0]) + Math.abs(lastAt.y - cc[1]) > 1e-15)) in.lineTo(cc[0], cc[1]);
				if (lastAt == null) lastAt = new Vec2(cc[0], cc[1]);
				else {
					lastAt.x = cc[0];
					lastAt.y = cc[1];
				}
			} else if (ty == PathIterator.SEG_MOVETO) {
				if (lastAt == null || (Math.abs(lastAt.x - cc[0]) + Math.abs(lastAt.y - cc[1]) > 1e-15)) in.moveTo(cc[0], cc[1]);

				lastMoveTo = new Vec2(cc[0], cc[1]);

				if (lastAt == null) lastAt = new Vec2(cc[0], cc[1]);
				else {
					lastAt.x = cc[0];
					lastAt.y = cc[1];
				}
			} else if (ty == PathIterator.SEG_QUADTO) {
				if (lastAt == null || (Math.abs(lastAt.x - cc[2]) + Math.abs(lastAt.y - cc[3]) > 1e-15))
					in.cubicTo((cc[0] - lastAt.x) * (2 / 3f) + lastAt.x, (cc[1] - lastAt.y) * (2 / 3f) + lastAt.y, (cc[0] - cc[2]) * (2 / 3f) + cc[2],
						   (cc[1] - cc[3]) * (2 / 3f) + cc[3], cc[2], cc[3]);

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

	static public List<Vec3> samplePoints(FLine f, float distance)
	{
		Cursor cc = f.cursor();
		ArrayList<Vec3> r = new ArrayList<>();
		double D = 0;
		while(D<cc.lengthD())
		{
			r.add(cc.position());
			cc.setD(D+=distance);
		}
		return r;
	}

	static public FLine drawRoundedRectInto(FLine into, double x, double y, double w, double h, double r) {
		into.moveTo(x + ctrlpts[0][0] * w + ctrlpts[0][1] * r, y + ctrlpts[0][2] * h + ctrlpts[0][3] * r);
		into.lineTo(x + ctrlpts[1][0] * w + ctrlpts[1][1] * r, y + ctrlpts[1][2] * h + ctrlpts[1][3] * r);
		into.cubicTo(x + ctrlpts[2][0] * w + ctrlpts[2][1] * r, y + ctrlpts[2][2] * h + ctrlpts[2][3] * r, x + ctrlpts[2][4] * w + ctrlpts[2][5] * r, y + ctrlpts[2][6] * h + ctrlpts[2][7] * r,
			     x + ctrlpts[2][8] * w + ctrlpts[2][9] * r, y + ctrlpts[2][10] * h + ctrlpts[2][11] * r);
		into.lineTo(x + ctrlpts[3][0] * w + ctrlpts[3][1] * r, y + ctrlpts[3][2] * h + ctrlpts[3][3] * r);
		into.cubicTo(x + ctrlpts[4][0] * w + ctrlpts[4][1] * r, y + ctrlpts[4][2] * h + ctrlpts[4][3] * r, x + ctrlpts[4][4] * w + ctrlpts[4][5] * r, y + ctrlpts[4][6] * h + ctrlpts[4][7] * r,
			     x + ctrlpts[4][8] * w + ctrlpts[4][9] * r, y + ctrlpts[4][10] * h + ctrlpts[4][11] * r);
		into.lineTo(x + ctrlpts[5][0] * w + ctrlpts[5][1] * r, y + ctrlpts[5][2] * h + ctrlpts[5][3] * r);
		into.cubicTo(x + ctrlpts[6][0] * w + ctrlpts[6][1] * r, y + ctrlpts[6][2] * h + ctrlpts[6][3] * r, x + ctrlpts[6][4] * w + ctrlpts[6][5] * r, y + ctrlpts[6][6] * h + ctrlpts[6][7] * r,
			     x + ctrlpts[6][8] * w + ctrlpts[6][9] * r, y + ctrlpts[6][10] * h + ctrlpts[6][11] * r);
		into.lineTo(x + ctrlpts[7][0] * w + ctrlpts[7][1] * r, y + ctrlpts[7][2] * h + ctrlpts[7][3] * r);
		into.cubicTo(x + ctrlpts[8][0] * w + ctrlpts[8][1] * r, y + ctrlpts[8][2] * h + ctrlpts[8][3] * r, x + ctrlpts[8][4] * w + ctrlpts[8][5] * r, y + ctrlpts[8][6] * h + ctrlpts[8][7] * r,
			     x + ctrlpts[8][8] * w + ctrlpts[8][9] * r, y + ctrlpts[8][10] * h + ctrlpts[8][11] * r);
		return into;
	}

	/**
	 * returns the 't' (in the cursor sense) that's closest to this Vec3
	 */
	static public double closestT(FLine to, Vec3 point) {
		double best = Double.POSITIVE_INFINITY;
		double tt = 0;
		for (int i = 1; i < to.nodes.size(); i++) {
			FLine.Node n1 = to.nodes.get(i);
			FLine.Node n0 = to.nodes.get(i - 1);
			if (n1 instanceof FLine.MoveTo) continue;

			if (n1 instanceof FLine.LineTo) {
				Vec3 dir = new Vec3(n1.to).sub(n0.to);
				if (dir.length() == 0) {
					double dd = n1.to.distance(point);
					if (dd <= best) {
						best = dd;

						tt = (i - 1);
					}
				} else {

					double len = dir.length();
					dir.mul(1 / len);
					double d = dir.dot(new Vec3(point).sub(n0.to));
					if (d < 0) d = 0;
					if (d > len) d = len;
					Vec3 at = new Vec3(n0.to).fma(dir, d);
					double dd = at.distance(point);
					if (dd < best) {
						best = dd;
						tt = d + (i - 1);
					}
				}
			} else {
				FLine.CubicTo c = (FLine.CubicTo) n1;
				double t = new CubicSegment3(n0.to, c.c1, c.c2, c.to).closestToPoint(point);
				double dd = evaluateCubicFrame(n0.to, c.c1, c.c2, c.to, t, new Vec3()).distance(point);
				if (dd < best) {
					best = dd;
					tt = t + (i - 1);
				}
			}

		}
		return tt;
	}

	/**
	 * returns the c1, c2 control points for a cubic segement to go through p0@0, p1@u, p2@v and p3@ t=1.0
	 */
	public Pair<Vec3, Vec3> fromFourPoints(Vec3 p0, float u, Vec3 p1, float v, Vec3 p2, Vec3 p3) {

		double a = 0.0, b = 0.0, c = 0.0, d = 0.0, det = 0.0;
		Vec3 q1 = new Vec3();
		Vec3 q2 = new Vec3();

		if ((u <= 0.0) || (u >= 1.0) || (v <= 0.0) || (v >= 1.0) || (u >= v)) return null;

		a = 3 * (1 - u) * (1 - u) * u;
		b = 3 * (1 - u) * u * u;
		c = 3 * (1 - v) * (1 - v) * v;
		d = 3 * (1 - v) * v * v;
		det = a * d - b * c;
		if (det == 0.0) return null;

		Pair<Vec3, Vec3> r = new Pair<>(new Vec3(), new Vec3());

		q1.x = p1.x - ((1 - u) * (1 - u) * (1 - u) * p0.x + u * u * u * p3.x);
		q1.y = p1.y - ((1 - u) * (1 - u) * (1 - u) * p0.y + u * u * u * p3.y);
		q1.z = p1.z - ((1 - u) * (1 - u) * (1 - u) * p0.z + u * u * u * p3.z);

		q2.x = p2.x - ((1 - v) * (1 - v) * (1 - v) * p0.x + v * v * v * p3.x);
		q2.y = p2.y - ((1 - v) * (1 - v) * (1 - v) * p0.y + v * v * v * p3.y);
		q2.z = p2.z - ((1 - v) * (1 - v) * (1 - v) * p0.z + v * v * v * p3.z);

		r.first.x = d * q1.x - b * q2.x;
		r.first.y = d * q1.y - b * q2.y;
		r.first.z = d * q1.z - b * q2.z;
		r.first.x /= det;
		r.first.y /= det;
		r.first.z /= det;

		r.second.x = (-c) * q1.x + a * q2.x;
		r.second.y = (-c) * q1.y + a * q2.y;
		r.second.z = (-c) * q1.z + a * q2.z;
		r.second.x /= det;
		r.second.y /= det;
		r.second.z /= det;

		return r;
	}

	public Supplier<FLine> relativeTo(FLine f, Box box, Vec2 at) {

		Vec2 q = box.properties.get(Box.frame)
				       .convert(at.x, at.y);

		FLine template = f.byTransforming(x -> new Vec3(x.x - q.x, x.y - q.y, x.z));

		Cached<Box, Vec2, FLine> c = new Cached<>((x, prev) -> {
			Vec2 p = x.properties.get(Box.frame)
					     .convert(at.x, at.y);

			return template.byTransforming(m -> new Vec3(m.x + p.x, m.y + p.y, m.z));

		}, x -> x.properties.get(Box.frame)
				    .convert(at.x, at.y));


		return () -> c.apply(box);
	}

	// builds a segment out that goes a->b->c->d. b and d are assumed to start and end at the start and end of a and c; a and b are run forward, c and d are run backwards
	public FLine rail(FLine out, FLine a, int ax, FLine b, int bx, FLine c, int cx, boolean cflip, FLine d, int dx, boolean dflip) {
		if (ax == 0) throw new IllegalArgumentException("can't indicate segment with 0");
		if (bx == 0) throw new IllegalArgumentException("can't indicate segment with 0");
		if (cx == 0) throw new IllegalArgumentException("can't indicate segment with 0");
		if (dx == 0) throw new IllegalArgumentException("can't indicate segment with 0");

		if (ax >= a.nodes.size()) throw new IllegalArgumentException("over end of line");
		if (bx >= b.nodes.size()) throw new IllegalArgumentException("over end of line");
		if (cx >= c.nodes.size()) throw new IllegalArgumentException("over end of line");
		if (dx >= d.nodes.size()) throw new IllegalArgumentException("over end of line");


		Vec3 aStart = a.nodes.get(ax - 1).to;

		Vec3 aEnd = a.nodes.get(ax).to;

		Vec3 bStart = b.nodes.get(bx - 1).to;
		Vec3 bEnd = b.nodes.get(bx).to;

		Vec3 cStart = c.nodes.get(cx).to;
		Vec3 cEnd = c.nodes.get(cx - 1).to;

		Vec3 dStart = d.nodes.get(dx).to;
		Vec3 dEnd = d.nodes.get(dx - 1).to;

		out.moveTo(aStart);
		out.copyTo(a.nodes.get(ax));
		out.copyTo(b.nodes.get(bx));
		if (cflip) out.copyToFlipped(c.nodes.get(cx), cEnd);
		else out.copyTo(c.nodes.get(cx));
		if (dflip) out.copyToFlipped(d.nodes.get(dx), dEnd);
		else out.copyTo(d.nodes.get(dx));

		return out;
	}

	public FLine rail(FLine a, FLine b) {
		if (a.nodes.size() != b.nodes.size()) throw new IllegalArgumentException(" lines do not have same number of nodes ");

		FLine out = new FLine();

		for (int i = 1; i < a.nodes.size(); i++) {
			rail(out, a, i, new FLine().moveTo(a.nodes.get(i).to)
						   .lineTo(b.nodes.get(i).to), 1, b, i, true, new FLine().moveTo(b.nodes.get(i - 1).to)
													 .lineTo(a.nodes.get(i - 1).to), 1, false);
		}
		return out;
	}

	/**
	 * we rotate xy so that 'z' axis moves along control
	 */

	public FLine extrude(FLine control, FLine xy, Vec3 z) {

		FLine out = new FLine();

		Cursor controlCusor = new Cursor(control, 0.1f);
		Quat q2 = new Quat().rotateTo(controlCusor.setT(0)
							  .tangent(), z);

		xy = xy.byTransforming(x -> q2.transform(new Vec3(x))
					      .add(control.nodes.get(0).to));

//		out.add(xy);

		for (int i = 1; i < control.nodes.size(); i++) {
			final int fi = i;

			Quat q1 = new Quat().rotateTo(controlCusor.setT(i)
								  .tangent(), controlCusor.setT(i - 1)
											  .tangent());


			FLine nxy = xy.byTransforming(x -> q1.transform(new Vec3(x).sub(control.nodes.get(fi - 1).to))
							     .add(control.nodes.get(fi).to));

			for (int x = 1; x < nxy.nodes.size(); x++) {

				// translate the control segment so that it starts and ends at the right place

				Vec3 s1 = xy.nodes.get(x - 1).to;
				Vec3 e1 = nxy.nodes.get(x - 1).to;

				FLine seg = new FLine().moveTo(control.nodes.get(i - 1).to)
						       .copyTo(control.nodes.get(i));


				Vec3 s2 = xy.nodes.get(x).to;
				Vec3 e2 = nxy.nodes.get(x).to;

				FLine seg1 = seg.byFixingEndpointsTo(s1, e1);
				FLine seg2 = seg.byFixingEndpointsTo(s2, e2);

				rail(out, seg1, 1, nxy, x, seg2, 1, true, xy, x, true);


			}

			xy = nxy;

//			out.add(xy);

		}

		return out;

	}

	/**
	 * segments this along .moveTo's
	 *
	 * @param f
	 * @return
	 */
	public List<FLine> segment(FLine f) {
		List<FLine> q = new ArrayList<>();
		FLine current = null;
		for (FLine.Node n : f.nodes) {
			if (n instanceof FLine.MoveTo) {
				current = new FLine();
				q.add(current);
			}
			current.copyTo(n);
		}
		return q;
	}

	/**
	 * A list of low curvature positions out of a FLine
	 */
	public List<Vec3> positions(FLine f, float flatness) {
		Cursor c = new Cursor(f, flatness);
		List<PathFlattener.Mapping> m = c.getPathFlattener()
						 .getMappings();
		List<Vec3> o = m.stream()
				.map(x -> x.start)
				.collect(Collectors.toList());

		o.add(m.get(m.size() - 1).end);

		return o;
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
		protected int index;
		protected double alpha;

		public Cursor(FLine on, float tol) {
			this.on = on;
			this.tol = tol;
			pAt = on.getModCount();
		}

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
			try {
				if (segmentIsCubic()) return new Vec3[]{new Vec3(on.nodes.get(clamp(index)).to), new Vec3(((FLine.CubicTo) on.nodes.get(clamp(index + 1))).c1), new Vec3(
					    ((FLine.CubicTo) on.nodes.get(clamp(index + 1))).c2), new Vec3(((FLine.CubicTo) on.nodes.get(clamp(index + 1))).to)};
				else if (segmentIsLinear()) return new Vec3[]{new Vec3(on.nodes.get(clamp(index)).to), new Vec3(on.nodes.get(clamp(index + 1)).to)};
				else if (segmentIsMove()) return new Vec3[]{new Vec3(on.nodes.get(clamp(index)).to)};
				else throw new IllegalStateException(" segment not implemented ");
			} finally {
				index++;
			}
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
				return evaluateCubicFrame(on.nodes.get(clamp(index)).to, ((FLine.CubicTo) on.nodes.get(clamp(index + 1))).c1, ((FLine.CubicTo) on.nodes.get(clamp(index + 1))).c2,
							  ((FLine.CubicTo) on.nodes.get(clamp(index + 1))).to, alpha, new Vec3());
			} else throw new IllegalStateException(" segment not implemented ");
		}

		public Vec3 tangentForward() {
			if (segmentIsMove()) {
				return null;
			} else if (segmentIsLinear()) {
				return Vec3.sub(on.nodes.get(clamp(index + 1)).to, on.nodes.get(clamp(index)).to, new Vec3());
			} else if (segmentIsCubic()) {
				return evaluateDCubicFrame(on.nodes.get(clamp(index)).to, ((FLine.CubicTo) on.nodes.get(clamp(index + 1))).c1, ((FLine.CubicTo) on.nodes.get(clamp(index + 1))).c2,
							   ((FLine.CubicTo) on.nodes.get(clamp(index + 1))).to, alpha, new Vec3());
			} else throw new IllegalStateException(" segment not implemented ");
		}

		public Vec3 tangentBackward() {
			if (alpha != 0) return tangentForward();
			if (index == 0) return null;

			index--;
			alpha = 1;
			try {
				return tangentForward();
			} finally {
				index++;
				alpha = 0;
			}
		}

		public Vec3 accelerationForward() {
			if (segmentIsMove()) {
				return null;
			} else if (segmentIsLinear()) {
				return new Vec3();
			} else if (segmentIsCubic()) {
				return evaluateDDCubicFrame(on.nodes.get(clamp(index)).to, ((FLine.CubicTo) on.nodes.get(clamp(index + 1))).c1, ((FLine.CubicTo) on.nodes.get(clamp(index + 1))).c2,
							    ((FLine.CubicTo) on.nodes.get(clamp(index + 1))).to, alpha, new Vec3());
			} else throw new IllegalStateException(" segment not implemented ");
		}

		public Vec3 accelerationBackward() {
			if (alpha != 0) return accelerationForward();
			if (index == 0) return null;

			index--;
			alpha = 1;
			try {
				return accelerationForward();
			} finally {
				index++;
				alpha = 0;
			}
		}

		public Vec3 binormalForward() {
			Vec3 a = accelerationForward();
			Vec3 t = tangentForward();

			if (a == null || t == null) return null;

			return Vec3.cross(t, a, new Vec3());

		}

		public Vec3 binormalBackward() {
			Vec3 a = accelerationBackward();
			Vec3 t = tangentBackward();

			if (a == null || t == null) return null;

			return Vec3.cross(t, a, new Vec3());
		}

		public Vec3 binormal() {
			Vec3 a = acceleration();
			Vec3 t = tangent();

			if (a == null || t == null) return null;

			return Vec3.cross(t, a, new Vec3());
		}

		public Vec3 normalForward() {
			Vec3 a = binormalForward();
			Vec3 t = tangentForward();

			if (a == null || t == null) return null;

			return Vec3.cross(t, a, new Vec3());

		}

		public Vec3 normalBackward() {
			Vec3 a = binormalBackward();
			Vec3 t = tangentBackward();

			if (a == null || t == null) return null;

			return Vec3.cross(t, a, new Vec3());
		}

		public Vec3 normal() {
			Vec3 a = binormal();
			Vec3 t = tangent();

			if (a == null || t == null) return null;

			return Vec3.cross(t, a, new Vec3());
		}


		public Vec2 normal2()
		{
			Vec3 t = tangent();
			Vec2 t2 = t==null ? null : new Vec2(t.y, -t.x);

			if (t2==null)
				System.out.println("NORMAL2 failed :"+this.on.nodes+" / "+this.alpha);

			return t2;
		}

		/**
		 * this is a vector along the normal of the right length. i.e, you can draw this circle and it will be tangent at this point
		 */
		public Vec3 radiusOfCurvature() {
			if (segmentIsCubic()) {
				Vec3 p0 = on.nodes.get(clamp(index)).to;
				Vec3 p1 = ((FLine.CubicTo) on.nodes.get(clamp(index + 1))).c1;
				Vec3 p2 = ((FLine.CubicTo) on.nodes.get(clamp(index + 1))).c2;
				Vec3 p3 = ((FLine.CubicTo) on.nodes.get(clamp(index + 1))).to;

				CubicSegment3 c = new CubicSegment3(p0, p1, p2, p3);
				return new Vec3(normal()).normalize()
							 .mul(c.R(alpha));

			} else return null;
		}


		public CubicSegment3 currentSegment() {
			if (segmentIsCubic()) {
				Vec3 p0 = on.nodes.get(clamp(index)).to;
				Vec3 p1 = ((FLine.CubicTo) on.nodes.get(clamp(index + 1))).c1;
				Vec3 p2 = ((FLine.CubicTo) on.nodes.get(clamp(index + 1))).c2;
				Vec3 p3 = ((FLine.CubicTo) on.nodes.get(clamp(index + 1))).to;

				CubicSegment3 c = new CubicSegment3(p0, p1, p2, p3);
				return c;

			} else return null;
		}

		public Vec3 acceleration() {
			if (alpha != 0) return accelerationForward();
			Vec3 t1 = accelerationBackward();
			Vec3 t2 = accelerationForward();

			if (t1 != null && t1.isNaN()) t1 = null;
			if (t2 != null && t2.isNaN()) t2 = null;

			if (t1 == null && t2 == null) return null;

			Vec3 t = new Vec3(t1 == null ? t2 : t1).add(t2 == null ? t1 : t2)
							       .mul(0.5f);
			return t.isNaN() ? null : t;
		}

		public Vec3 tangent() {
			if (alpha != 0) return tangentForward();
			Vec3 t1 = tangentBackward();
			Vec3 t2 = tangentForward();

			if (t1 != null && t1.isNaN()) t1 = null;
			if (t2 != null && t2.isNaN()) t2 = null;

			if (t1 == null && t2 == null) return null;

			Vec3 t = new Vec3(t1 == null ? t2 : t1).add(t2 == null ? t1 : t2)
							       .mul(0.5f);
			return t.isNaN() ? null : t;
		}

		/**
		 * returns a list of points where dx, dy, or dz is zero, inside segments
		 */
		public List<Double> extremalInnerPoints() {
			List<Double> q = new ArrayList<>();
			for (int i = 1; i < on.nodes.size(); i++) {
				if (on.nodes.get(i) instanceof FLine.CubicTo) {

					int fi = i;
					CubicSegment3 c = new CubicSegment3(on, i);
					List<Double> z1 = c.findDXZeroRoots();
					List<Double> z2 = c.findDYZeroRoots();
					List<Double> z3 = c.findDZZeroRoots();
					z1.forEach(x -> q.add(fi + x - 1));
					z2.forEach(x -> q.add(fi + x - 1));
					z3.forEach(x -> q.add(fi + x - 1));
				}
			}
			Collections.sort(q);
			if (q.size() == 0) return q;

			Double o = q.get(0);
			for (int i = 1; i < q.size(); i++) {
				if (Math.abs(q.get(i) - o) < 1e-3) {
					q.remove(i);
					i--;
				}
			}
			return q;
		}

		/**
		 * returns a list of places where tangentForward doesn't equal tangentBackward
		 */
		public List<Double> tangentBreaks() {
			return extremalOuterPoints(p -> p.first.distance(p.second) > 1e-3);
		}

		/**
		 * returns a list of places where tangent's change dx, dy or dz
		 */
		public List<Double> extremalOuterPoints() {
			return this.extremalOuterPoints(
				    p -> Math.signum(p.first.x) != Math.signum(p.second.x) || Math.signum(p.first.y) != Math.signum(p.second.y) || Math.signum(p.first.z) != Math.signum(p.second.z));
		}

		public List<Double> inflectionPointsXY() {
			ArrayList<Double> q = new ArrayList<>();
			for (int i = 1; i < on.nodes.size(); i++) {
				int fi = i;
				FLine.Node nn = on.nodes.get(i);
				if (nn instanceof FLine.CubicTo) {
					CubicSegment3 c = new CubicSegment3(on, i);
					List<Double> d = c.inflectionPointsXY();

					d.forEach(x -> q.add(x + fi - 1));
				}
			}
			return q;
		}


		protected List<Double> extremalOuterPoints(Predicate<Pair<Vec3, Vec3>> p) {
			double was = getT();
			List<Double> q = new ArrayList<>();
			try {

				for (int i = 0; i < lengthT(); i++) {

					setT(i);
					Vec3 b = tangentBackward();
					Vec3 f = tangentForward();
					if (b == null && f != null) q.add(i + 0.0);
					else if (b != null && f == null) q.add(i + 0.0);
					else if (b == null && f == null) {

					} else {
						if (p.test(new Pair<>(b, f))) q.add(i + 0.0);
					}
				}
			} finally {
				setT(was);
			}
			return q;
		}

		/**
		 * returns new FLine's one on each side of the current cursor
		 */

		public Pair<FLine, FLine> split() {

			if (alpha == 0 && index == 0) return new Pair<>(null, on.duplicate());
			if (alpha == 1 && index == on.nodes.size() - 2) return new Pair<>(on.duplicate(), null);
			if (alpha == 0 && index == on.nodes.size() - 1) return new Pair<>(on.duplicate(), null);

			FLine left = new FLine();
			FLine right = new FLine();

			for (int i = 0; i < index + 1; i++) {
				left.copyTo(on.nodes.get(i));
			}

			if (segmentIsMove()) {

			} else if (segmentIsLinear()) {
				left.lineTo(position());
				right.moveTo(position());
			} else if (segmentIsCubic()) {
				Vec3 a = new Vec3(on.nodes.get(clamp(index)).to);
				Vec3 c1 = new Vec3(((FLine.CubicTo) on.nodes.get(clamp(index + 1))).c1);
				Vec3 c2 = new Vec3(((FLine.CubicTo) on.nodes.get(clamp(index + 1))).c2);
				Vec3 b = new Vec3(((FLine.CubicTo) on.nodes.get(clamp(index + 1))).to);

				Vec3 c12 = new Vec3();
				Vec3 m = new Vec3();
				Vec3 c21 = new Vec3();
				Vec3 t = new Vec3();

				splitCubicFrame(a, c1, c2, b, alpha, c12, m, c21, t);

				left.cubicTo(c1, c12, m);
				right.moveTo(m);
				right.cubicTo(c21, c2, b);
			}
			for (int i = index + 2; i < on.nodes.size(); i++) {
				right.copyTo(on.nodes.get(i));
			}

			return new Pair<>(left, right);
		}


		/**
		 * splits on a list of D positions
		 */
		public List<FLine> splitD(List<Double> d) {
			double was = getT();
			try {
				FLine right = on;
				double soFar = 0;

				Cursor cursor = this;
				List<FLine> f = new ArrayList<>();
				for (int i = 0; i < d.size(); i++) {

					double at = d.get(i) - soFar;
					cursor.setD(at);
					Pair<FLine, FLine> s = cursor.split();

					f.add(s.first);
					soFar = at;
					right = s.second;
					cursor = new Cursor(right, 0.1f);
				}
				;
				f.add(right);
				return f;
			} finally {
				setT(was);
			}
		}

		/**
		 * splits on a list of D positions
		 */
		public List<FLine> splitT(List<Double> t) {
			double was = getT();
			try {
				List<Double> d = new ArrayList<>(t.size());
				for (Double tt : t) {
					setT(tt);
					d.add(getD());
				}
				return splitD(d);
			} finally {
				setT(was);
			}
		}

		/**
		 * mutates this line, introducing a node here. Note this will increment lengthT and possibly t, but this cursor will still be in the same actual .position() This returns the added node
		 * or null if we are already at a control point or a break in the line
		 */
		public FLine.Node segment() {
			if (alpha == 0) return null;

			if (segmentIsMove()) {
				return null;
			} else if (segmentIsLinear()) {
				FLine.LineTo s = on.new LineTo(position());
				on.nodes.add(index + 1, s);
				alpha = 0;
				index++;
				return on.nodes.get(index + 1);
			} else if (segmentIsCubic()) {
				Vec3 a = new Vec3(on.nodes.get(clamp(index)).to);
				Vec3 c1 = new Vec3(((FLine.CubicTo) on.nodes.get(clamp(index + 1))).c1);
				Vec3 c2 = new Vec3(((FLine.CubicTo) on.nodes.get(clamp(index + 1))).c2);
				Vec3 b = new Vec3(((FLine.CubicTo) on.nodes.get(clamp(index + 1))).to);

				Vec3 c12 = new Vec3();
				Vec3 m = new Vec3();
				Vec3 c21 = new Vec3();
				Vec3 t = new Vec3();

				splitCubicFrame(a, c1, c2, b, alpha, c12, m, c21, t);

				((FLine.CubicTo) on.nodes.get(clamp(index + 1))).c1.set(c1);
				((FLine.CubicTo) on.nodes.get(clamp(index + 1))).c2.set(c12);
				((FLine.CubicTo) on.nodes.get(clamp(index + 1))).to.set(m);

				on.nodes.add(index + 2, on.new CubicTo(c21, c2, b));

				p = null;
				alpha = 0;
				index++;
				return on.nodes.get(index + 1);
			}

			return null;
		}

		/**
		 * length in node units.
		 *
		 * @return
		 */
		public float lengthT() {
			return on.nodes.size() - 1;
		}

		/**
		 * length in distance
		 */
		public float lengthD() {
			return (float) getPathFlattener().length();
		}

		/**
		 * get current position as distance along line
		 */
		public double getD() {
			return getPathFlattener().dotToLength(index + alpha);
		}

		/**
		 * set the current position as a distance along this line. Returns position in node.t format
		 */
		public Cursor setD(double d) {
			double q = getPathFlattener().lengthToDot(d);
			index = (int) q;
			alpha = (float) (q - index);
			return this;
		}

		/**
		 * Get current position in node.t format (so 0 is the start of the line, 1 is the first node 1.5 is roughly (but certainly not exactly) half-way between the first and second node, and
		 * so on. The key trouble here is that while evalLinear(x,y, alpha) parameterizes linear segments evently, evalCubic(x, c1, c2, y, alpha) certainly doesn't and alpha=0.5 doesn't mean
		 * half-way along in terms of distance. If you can about even distribution use the much slower but accurate setD and getD.
		 */
		public double getT() {
			return index + alpha;
		}

		/**
		 * set the current position as a distance along this line. Returns position in node.t format
		 */
		public Cursor setT(double q) {
			index = (int) q;
			alpha = (q - index);
			return this;
		}

		protected PathFlattener getPathFlattener() {
			if (p == null) return p = new PathFlattener(this.on, tol);
			return p;
		}

		private int clamp(int index) {
			if (index >= on.nodes.size()) return on.nodes.size() - 1;
			if (index < 0) return 0;
			return index;
		}

	}

	static public Vec2 intersectTwoLineSegments(Vec2 l1a, Vec2 l1d, Vec2 l2a, Vec2 l2d)
	{
		double c = l1d.x*l2d.y-l1d.y*l2d.x;

		if (c==0) return null; // colinear

		Vec2 across = new Vec2(l2a).sub(l1a);

		double t = (across.x*l2d.y-across.y*l2d.x)/c;
		double u = (across.x*l1d.y-across.y*l1d.x)/c;

		if (t<0 || t>1 || u<0 || u>1) return null;

		return new Vec2(l1a).fma((float) t, l1d);

	}

	/**
	 * helper class for a 2d cubic segment, for the things that we can only reasonably do in 2d (like intersections).
	 */
	public static class CubicSegment3 {
		public Vec3 a, b, c, d;

		public CubicSegment3(Vec3 a, Vec3 b, Vec3 c, Vec3 d) {
			this.a = new Vec3(a);
			this.b = new Vec3(b);
			this.c = new Vec3(c);
			this.d = new Vec3(d);
		}

		public CubicSegment3(FLine f, int index) {

			this.a = new Vec3(f.nodes.get(index - 1).to);
			this.b = new Vec3(((FLine.CubicTo) f.nodes.get(index)).c1);
			this.c = new Vec3(((FLine.CubicTo) f.nodes.get(index)).c2);
			this.d = new Vec3(((FLine.CubicTo) f.nodes.get(index)).to);
		}

		public List<Vec3> intersect(Vec3 x1, Vec3 x2) {
			CubicSegment3 c2 = rotateToX(x1, x2);
			return c2.findYZeroRoots()
				 .stream()
				 .filter(x -> Math.abs(evaluateCubicFrame(c2.a.z, c2.b.z, c2.c.z, c2.d.z, x).x) < 1e-5)
				 .map(x -> evaluateCubicFrame(a, b, c, d, x, new Vec3()))
				 .collect(Collectors.toList());

			// filter out things passed the end of x1->x2?
		}

		/**
		 * rotates so that x1->x2 is along the x axis
		 */
		public CubicSegment3 rotateToX(Vec3 x1, Vec3 x2) {
			Vec3 dir = Vec3.sub(x2, x1, new Vec3());
			Quat q = new Quat().rotateTo(new Vec3(1, 0, 0), dir);

			x1 = q.transform(x1);
			x2 = q.transform(x2);

			Vec3 a2 = Vec3.sub(q.transform(a), x1, new Vec3());
			Vec3 b2 = Vec3.sub(q.transform(b), x1, new Vec3());
			Vec3 c2 = Vec3.sub(q.transform(c), x1, new Vec3());
			Vec3 d2 = Vec3.sub(q.transform(d), x1, new Vec3());
			return new CubicSegment3(a2, b2, c2, d2);
		}

		public Cuboid boundingBox() {
			List<Double> y = findDYZeroRoots();
			List<Double> x = findDXZeroRoots();
			List<Double> z = findDZZeroRoots();

			Vec3 min = new Vec3(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
			Vec3 max = new Vec3(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);

			{
				Vec3 s = evaluateCubicFrame(a, b, c, d, 0, new Vec3());
				min.min(s);
				max.max(s);
			}
			{
				Vec3 s = evaluateCubicFrame(a, b, c, d, 1, new Vec3());
				min.min(s);
				max.max(s);
			}
			for (double yy : y) {
				Vec3 s = evaluateCubicFrame(a, b, c, d, yy, new Vec3());
				min.min(s);
				max.max(s);
			}
			for (double yy : x) {
				Vec3 s = evaluateCubicFrame(a, b, c, d, yy, new Vec3());
				min.min(s);
				max.max(s);
			}
			for (double yy : z) {
				Vec3 s = evaluateCubicFrame(a, b, c, d, yy, new Vec3());
				min.min(s);
				max.max(s);
			}

			return new Cuboid(min.x, min.y, min.z, max.x - min.x, max.y - min.y, max.z - min.z);
		}

		public Pair<CubicSegment3, CubicSegment3> split(float alpha) {
			Vec3 c12 = new Vec3();
			Vec3 c21 = new Vec3();
			Vec3 m = new Vec3();

			CubicSegment3 s1 = new CubicSegment3(a, b, c, d);
			CubicSegment3 s2 = new CubicSegment3(a, b, c, d);

			splitCubicFrame(s1.a, s1.b, s2.c, s2.d, alpha, c12, m, c21, new Vec3());

			s1.c = c12;
			s1.d = m;
			s2.a = new Vec3(m);
			s2.b = c21;

			return new Pair<>(s1, s2);
		}

		public String toString() {
			return "c[" + a + "," + b + "," + c + "," + d + "]";
		}

		public List<Pair<CubicSegment3, CubicSegment3>> intersection(CubicSegment3 with, float sz) {

			List<CubicSegment3> a = new ArrayList<>();
			a.add(this);
			List<CubicSegment3> b = new ArrayList<>();
			b.add(with);

			List<Pair<CubicSegment3, CubicSegment3>> out = new ArrayList<>();

			int gen = 0;
			while (a.size() > 0 && b.size() > 0 && gen < 20) {
				Map<CubicSegment3, Cuboid> cache = new LinkedHashMap<>();

				Set<CubicSegment3> na = new LinkedHashSet<>();
				Set<CubicSegment3> nb = new LinkedHashSet<>();

				o:
				for (CubicSegment3 aa : a) {
					Cuboid baa = aa.boundingBox();
					Iterator<CubicSegment3> ib = b.iterator();
					while (ib.hasNext()) {
						CubicSegment3 nib = ib.next();

						Cuboid bnn = cache.computeIfAbsent(nib, (x) -> nib.boundingBox());

						Log.log("intersection", "checking " + baa + " / " + bnn + "");


						if (baa.intersects(bnn)) {

							Log.log("intersection", " -- intersect");

							if (Math.max(baa.w, baa.h) < sz && Math.max(bnn.w, bnn.h) < sz) {
								ib.remove();
								out.add(new Pair<>(aa, nib));
							} else {
								na.add(aa);
								nb.add(nib);

							}
							//continue o;
						}
					}

					Log.log("intersection", "nothing intersects with " + baa);
				}

				a.clear();
				b.clear();

				for (CubicSegment3 c : na) {
					Cuboid r = cache.computeIfAbsent(c, (x) -> c.boundingBox());
					if (Math.max(r.w, r.y) < sz) a.add(c);
					else {
						Pair<CubicSegment3, CubicSegment3> lr = c.split(0.5f);
						a.add(lr.first);
						a.add(lr.second);
					}

				}

				for (CubicSegment3 c : nb) {
					Cuboid r = cache.computeIfAbsent(c, (x) -> c.boundingBox());
					if (Math.max(r.w, r.y) < sz) b.add(c);
					else {
						Pair<CubicSegment3, CubicSegment3> lr = c.split(0.5f);
						b.add(lr.first);
						b.add(lr.second);
					}

				}

				gen++;
			}

			if (gen == 20) Log.log("intersection", "warning: bailed");

			return out;

		}


		public List<Double> findYZeroRoots() {
			int subdiv = 20;

			List<Double> dd = new ArrayList<>();

			for (int i = 0; i < subdiv; i++) {
				double alphaStart = (i - 0.75f) / subdiv;
				double alphaEnd = (i + 0.75f) / subdiv;
				double yStart = evaluateCubicFrame(a.y, b.y, c.y, d.y, alphaStart).x;
				double yEnd = evaluateCubicFrame(a.y, b.y, c.y, d.y, alphaEnd).x;
				if (Math.signum(yStart) != Math.signum(yEnd)) {
					Double t = findYZeroRoot_bracketed(alphaStart, alphaEnd);
					if (t != null) dd.add(t);
				}
			}

			for (int i = 0; i < dd.size(); i++) {
				for (int j = i + 1; j < dd.size(); j++) {
					if (Math.abs(dd.get(i)
						       .doubleValue() - dd.get(j)
									  .doubleValue()) < 1e-6) {
						dd.remove(j);
						j--;
					}
				}
			}

			// filter dd for repeated roots

			return dd;
		}

		private Double findYZeroRoot_bracketed(double yStart, double yEnd) {

			double t = (yStart + yEnd) / 2;

			for (int i = 0; i < 10; i++) {
				Vec2 D = evaluateCubicFrame(a.y, b.y, c.y, d.y, t);
				double dt = D.x / D.y;
				t = t - dt;
				if (Math.abs(dt) < 1e-5) {
					return t;
				}
			}

			if (t >= yStart && t <= yEnd && evaluateCubicFrame(a.y, b.y, c.y, d.y, t).x < 1e-6) {
				return t;
			}

			return null;
		}

		public List<Double> findXZeroRoots() {
			int subdiv = 20;

			List<Double> dd = new ArrayList<>();

			for (int i = 0; i < subdiv; i++) {
				double alphaStart = (i - 0.75f) / subdiv;
				double alphaEnd = (i + 0.75f) / subdiv;
				double yStart = evaluateCubicFrame(a.x, b.x, c.x, d.x, alphaStart).x;
				double yEnd = evaluateCubicFrame(a.x, b.x, c.x, d.x, alphaEnd).x;
				if (Math.signum(yStart) != Math.signum(yEnd)) {
					Double t = findXZeroRoot_bracketed(alphaStart, alphaEnd);
					if (t != null) dd.add(t);
				}
			}

			for (int i = 0; i < dd.size(); i++) {
				for (int j = i + 1; j < dd.size(); j++) {
					if (Math.abs(dd.get(i)
						       .doubleValue() - dd.get(j)
									  .doubleValue()) < 1e-6) {
						dd.remove(j);
						j--;
					}
				}
			}

			// filter dd for repeated roots

			return dd;
		}

		private Double findXZeroRoot_bracketed(double yStart, double yEnd) {

			double t = (yStart + yEnd) / 2;

			for (int i = 0; i < 10; i++) {
				Vec2 D = evaluateCubicFrame(a.x, b.x, c.x, d.x, t);
				double dt = D.x / D.y;
				t = t - dt;
				if (Math.abs(dt) < 1e-5) {
					return t;
				}
			}

			if (t >= yStart && t <= yEnd && evaluateCubicFrame(a.x, b.x, c.x, d.x, t).x < 1e-6) {
				return t;
			}

			return null;
		}

		public List<Double> findDYZeroRoots() {
			int subdiv = 20;

			List<Double> dd = new ArrayList<>();

			for (int i = 0; i < subdiv; i++) {
				double alphaStart = (i - 0.75f) / subdiv;
				double alphaEnd = (i + 0.75f) / subdiv;
				double yStart = evaluateCubicFrame(a.y, b.y, c.y, d.y, alphaStart).y;
				double yEnd = evaluateCubicFrame(a.y, b.y, c.y, d.y, alphaEnd).y;
				if (Math.signum(yStart) != Math.signum(yEnd)) {
					Double t = findDYZeroRoot_bracketed(alphaStart, alphaEnd);
					if (t != null) dd.add(t);
				}
			}

			for (int i = 0; i < dd.size(); i++) {
				for (int j = i + 1; j < dd.size(); j++) {
					if (Math.abs(dd.get(i)
						       .doubleValue() - dd.get(j)
									  .doubleValue()) < 1e-6) {
						dd.remove(j);
						j--;
					}
				}
			}

			// filter dd for repeated roots

			return dd;
		}

		private Double findDYZeroRoot_bracketed(double yStart, double yEnd) {

			double t = (yStart + yEnd) / 2;

			for (int i = 0; i < 10; i++) {
				Vec2 D = evaluateDCubicFrame(a.y, b.y, c.y, d.y, t);
				double dt = D.x / D.y;
				t = t - dt;
				if (Math.abs(dt) < 1e-5) {
					return t;
				}
			}

			if (t >= yStart && t <= yEnd && evaluateDCubicFrame(a.y, b.y, c.y, d.y, t).x < 1e-6) {
				return t;
			}

			return null;
		}

		public List<Double> findDXZeroRoots() {
			int subdiv = 20;

			List<Double> dd = new ArrayList<>();

			for (int i = 0; i < subdiv; i++) {
				double alphaStart = (i - 0.75f) / subdiv;
				double alphaEnd = (i + 0.75f) / subdiv;
				double yStart = evaluateCubicFrame(a.x, b.x, c.x, d.x, alphaStart).y;
				double yEnd = evaluateCubicFrame(a.x, b.x, c.x, d.x, alphaEnd).y;
				if (Math.signum(yStart) != Math.signum(yEnd)) {
					Double t = findDXZeroRoot_bracketed(alphaStart, alphaEnd);
					if (t != null) dd.add(t);
				}
			}

			for (int i = 0; i < dd.size(); i++) {
				for (int j = i + 1; j < dd.size(); j++) {
					if (Math.abs(dd.get(i)
						       .doubleValue() - dd.get(j)
									  .doubleValue()) < 1e-6) {
						dd.remove(j);
						j--;
					}
				}
			}

			// filter dd for repeated roots

			return dd;
		}

		private Double findDXZeroRoot_bracketed(double yStart, double yEnd) {

			double t = (yStart + yEnd) / 2;

			for (int i = 0; i < 10; i++) {
				Vec2 D = evaluateDCubicFrame(a.x, b.x, c.x, d.x, t);
				double dt = D.x / D.y;
				t = t - dt;
				if (Math.abs(dt) < 1e-5) {
					return t;
				}
			}

			if (t >= yStart && t <= yEnd && evaluateDCubicFrame(a.x, b.x, c.x, d.x, t).x < 1e-6) {
				return t;
			}

			return null;
		}


		public List<Double> findDZZeroRoots() {
			int subdiv = 20;

			List<Double> dd = new ArrayList<>();

			for (int i = 0; i < subdiv; i++) {
				double alphaStart = (i - 0.75f) / subdiv;
				double alphaEnd = (i + 0.75f) / subdiv;
				double yStart = evaluateCubicFrame(a.z, b.z, c.z, d.z, alphaStart).y;
				double yEnd = evaluateCubicFrame(a.z, b.z, c.z, d.z, alphaEnd).y;
				if (Math.signum(yStart) != Math.signum(yEnd)) {
					Double t = findDZZeroRoot_bracketed(alphaStart, alphaEnd);
					if (t != null) dd.add(t);
				}
			}

			for (int i = 0; i < dd.size(); i++) {
				for (int j = i + 1; j < dd.size(); j++) {
					if (Math.abs(dd.get(i)
						       .doubleValue() - dd.get(j)
									  .doubleValue()) < 1e-6) {
						dd.remove(j);
						j--;
					}
				}
			}

			// filter dd for repeated roots

			return dd;
		}

		private Double findDZZeroRoot_bracketed(double yStart, double yEnd) {

			double t = (yStart + yEnd) / 2;

			for (int i = 0; i < 10; i++) {
				Vec2 D = evaluateDCubicFrame(a.z, b.z, c.z, d.z, t);
				double dt = D.x / D.y;
				t = t - dt;
				if (Math.abs(dt) < 1e-5) {
					return t;
				}
			}

			if (t >= yStart && t <= yEnd && evaluateDCubicFrame(a.z, b.z, c.z, d.z, t).x < 1e-6) {
				return t;
			}

			return null;
		}

		public Vec3 osculating(double alpha) {
			;

			return Vec3.cross(D(alpha), DD(alpha), new Vec3());
		}

		public Vec3 D(double alpha) {

			Vec3 a = new Vec3(this.b).sub(this.a);
			Vec3 b = new Vec3(this.c).fma(this.b, -2)
						 .add(this.a);
			Vec3 c = new Vec3(this.d).fma(this.c, -3)
						 .fma(this.b, 3)
						 .fma(this.a, -1);

			Vec3 D = new Vec3().fma(a, 3)
					   .fma(b, 6 * alpha)
					   .fma(c, 3 * alpha * alpha);

			return D;
		}


		public Vec3 DD(double alpha) {

			Vec3 a = new Vec3(this.b).sub(this.c);
			Vec3 b = new Vec3(this.c).fma(this.b, -2)
						 .add(this.a);
			Vec3 c = new Vec3(this.d).fma(this.c, -3)
						 .fma(this.b, 3)
						 .fma(this.a, -1);

			Vec3 DD = new Vec3().fma(b, 6)
					    .fma(c, 6 * alpha);

			return DD;
		}

		public double R(double alpha) {

			Vec3 D = D(alpha);
			Vec3 DD = DD(alpha);

			return Math.pow(D.x * D.x + D.y * D.y + D.z * D.z, 3 / 2f) / Vec3.cross(D, DD, new Vec3())
											 .length();
		}

		public List<Double> inflectionPointsXY() {

			double ax = -a.x + 3 * b.x - 3 * c.x + d.x;
			double bx = 3 * a.x - 6 * b.x + 3 * c.x;
			double cx = -3 * a.x + 3 * b.x;
			double dx = a.x;

			double ay = -a.y + 3 * b.y - 3 * c.y + d.y;
			double by = 3 * a.y - 6 * b.y + 3 * c.y;
			double cy = -3 * a.y + 3 * b.y;
			double dy = a.y;

			double az = -a.z + 3 * b.z - 3 * c.z + d.z;
			double bz = 3 * a.z - 6 * b.z + 3 * c.z;
			double cz = -3 * a.z + 3 * b.z;
			double dz = a.z;

			double cuspxy = -(ay * cx - ax * cy) / (2 * (ay * bx - ax * by));
			double disxy = cuspxy * cuspxy - (by * cx - bx * cy) / (3 * (ay * bx - ax * by));


			if (disxy < 0) {
				return Collections.EMPTY_LIST;
			} else if (disxy == 0) {
				return Collections.singletonList(cuspxy);
			} else {
				double d0 = cuspxy + Math.sqrt(disxy);
				double d1 = cuspxy - Math.sqrt(disxy);

				List<Double> dd = new ArrayList<>();
				dd.add(d0);
				dd.add(d1);
				return dd;

			}


		}

		public List<Double> inflectionPointsXZ() {

			double ax = -a.x + 3 * b.x - 3 * c.x + d.x;
			double bx = 3 * a.x - 6 * b.x + 3 * c.x;
			double cx = -3 * a.x + 3 * b.x;
			double dx = a.x;

//			double ay = -a.y+3*b.y-3*c.y+d.y;
//			double by = 3*a.y-6*b.y+3*c.y;
//			double cy = -3*a.y+3*b.y;
//			double dy = a.y;

			double ay = -a.z + 3 * b.z - 3 * c.z + d.z;
			double by = 3 * a.z - 6 * b.z + 3 * c.z;
			double cy = -3 * a.z + 3 * b.z;
			double dy = a.z;

			double cuspxy = -(ay * cx - ax * cy) / (2 * (ay * bx - ax * by));
			double disxy = cuspxy * cuspxy - (by * cx - bx * cy) / (3 * (ay * bx - ax * by));


			if (disxy < 0) {
				return Collections.EMPTY_LIST;
			} else if (disxy == 0) {
				return Collections.singletonList(cuspxy);
			} else {
				double d0 = cuspxy + Math.sqrt(disxy);
				double d1 = cuspxy - Math.sqrt(disxy);

				List<Double> dd = new ArrayList<>();
				dd.add(d0);
				dd.add(d1);
				return dd;

			}
		}

		public List<Double> inflectionPointsYZ() {

//			double ax = -a.x+3*b.x-3*c.x+d.x;
//			double bx = 3*a.x-6*b.x+3*c.x;
//			double cx = -3*a.x+3*b.x;
//			double dx = a.x;

			double ay = -a.y + 3 * b.y - 3 * c.y + d.y;
			double by = 3 * a.y - 6 * b.y + 3 * c.y;
			double cy = -3 * a.y + 3 * b.y;
			double dy = a.y;

			double ax = -a.z + 3 * b.z - 3 * c.z + d.z;
			double bx = 3 * a.z - 6 * b.z + 3 * c.z;
			double cx = -3 * a.z + 3 * b.z;
			double dx = a.z;

			double cuspxy = -(ay * cx - ax * cy) / (2 * (ay * bx - ax * by));
			double disxy = cuspxy * cuspxy - (by * cx - bx * cy) / (3 * (ay * bx - ax * by));


			if (disxy < 0) {
				return Collections.EMPTY_LIST;
			} else if (disxy == 0) {
				return Collections.singletonList(cuspxy);
			} else {
				double d0 = cuspxy + Math.sqrt(disxy);
				double d1 = cuspxy - Math.sqrt(disxy);

				List<Double> dd = new ArrayList<>();
				dd.add(d0);
				dd.add(d1);
				return dd;

			}


		}

		public double closestToPoint(Vec3 v) {

			// right now we split this segment into two pieces to reduce the changes of getting stuck in a local minimum. We ought to split at inflection points instead

			BrentOptimizer o = new BrentOptimizer(1e-4, 1e-4);
			UnivariatePointValuePair p1 = o.optimize(new UnivariateObjectiveFunction((x) -> evaluateCubicFrame(a, b, c, d, x, new Vec3()).distance(v)),
								 new SearchInterval(0, 0.5, 0.25), GoalType.MINIMIZE, new MaxEval(200));
			UnivariatePointValuePair p2 = o.optimize(new UnivariateObjectiveFunction((x) -> evaluateCubicFrame(a, b, c, d, x, new Vec3()).distance(v)),
								 new SearchInterval(0.5, 1, 0.75), GoalType.MINIMIZE, new MaxEval(200));

			if (p1.getValue() < p2.getValue()) return p1.getPoint();
			else return p2.getPoint();

		}

	}

	/**
	 * helper class for a 2d cubic segment, for the things that we can only reasonably do in 2d (like intersections).
	 */
	public class CubicSegment2 {
		public Vec2 a, b, c, d;

		public CubicSegment2(Vec2 a, Vec2 b, Vec2 c, Vec2 d) {
			this.a = new Vec2(a);
			this.b = new Vec2(b);
			this.c = new Vec2(c);
			this.d = new Vec2(d);
		}

		public CubicSegment2(Vec3 a, Vec3 b, Vec3 c, Vec3 d) {
			this.a = a.toVec2();
			this.b = b.toVec2();
			this.c = c.toVec2();
			this.d = d.toVec2();
		}

		public List<Vec2> intersect(Vec2 x1, Vec2 x2) {
			CubicSegment2 c2 = rotateToX(x1, x2);
			return c2.findYZeroRoots()
				 .stream()
				 .map(x -> evaluateCubicFrame(a, b, c, d, x, new Vec2()))
				 .collect(Collectors.toList());

			// filter out things passed the end of x1->x2?
		}

		/**
		 * rotates so that x1->x2 is along the x axis
		 */
		public CubicSegment2 rotateToX(Vec2 x1, Vec2 x2) {
			Vec2 dir = Vec2.sub(x2, x1, new Vec2());
			Quat q = new Quat().rotateTo(new Vec3(1, 0, 0), dir.toVec3());

			x1 = q.transform(x1);
			x2 = q.transform(x2);

			Vec2 a2 = Vec2.sub(q.transform(a)
					    , x1, new Vec2());
			Vec2 b2 = Vec2.sub(q.transform(b)
					    , x1, new Vec2());
			Vec2 c2 = Vec2.sub(q.transform(c)
					    , x1, new Vec2());
			Vec2 d2 = Vec2.sub(q.transform(d)
					    , x1, new Vec2());
			return new CubicSegment2(a2, b2, c2, d2);
		}

		public Rect boundingBox() {
			List<Double> y = findDYZeroRoots();
			List<Double> x = findDXZeroRoots();

			Vec2 min = new Vec2(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
			Vec2 max = new Vec2(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);

			{
				Vec2 s = evaluateCubicFrame(a, b, c, d, 0, new Vec2());
				min.min(s);
				max.max(s);
			}
			{
				Vec2 s = evaluateCubicFrame(a, b, c, d, 1, new Vec2());
				min.min(s);
				max.max(s);
			}
			for (double yy : y) {
				Vec2 s = evaluateCubicFrame(a, b, c, d, yy, new Vec2());
				min.min(s);
				max.max(s);
			}
			for (double yy : x) {
				Vec2 s = evaluateCubicFrame(a, b, c, d, yy, new Vec2());
				min.min(s);
				max.max(s);
			}

			return new Rect(min.x, min.y, max.x - min.x, max.y - min.y);
		}

		public Pair<CubicSegment2, CubicSegment2> split(float alpha) {
			Vec2 c12 = new Vec2();
			Vec2 c21 = new Vec2();
			Vec2 m = new Vec2();

			CubicSegment2 s1 = new CubicSegment2(a, b, c, d);
			CubicSegment2 s2 = new CubicSegment2(a, b, c, d);

			splitCubicFrame(s1.a, s1.b, s2.c, s2.d, alpha, c12, m, c21, new Vec2());

			s1.c = c12;
			s1.d = m;
			s2.a = new Vec2(m);
			s2.b = c21;

			return new Pair<>(s1, s2);
		}

		public List<Pair<CubicSegment2, CubicSegment2>> intersection(CubicSegment2 with, float sz) {

			List<CubicSegment2> a = new ArrayList<>();
			a.add(this);
			List<CubicSegment2> b = new ArrayList<>();
			b.add(with);

			List<Pair<CubicSegment2, CubicSegment2>> out = new ArrayList<>();

			int gen = 0;
			while (a.size() > 0 && b.size() > 0 && gen < 20) {
				Map<CubicSegment2, Rect> cache = new LinkedHashMap<>();

				Set<CubicSegment2> na = new LinkedHashSet<>();
				Set<CubicSegment2> nb = new LinkedHashSet<>();

				o:
				for (CubicSegment2 aa : a) {
					Rect baa = aa.boundingBox();
					Iterator<CubicSegment2> ib = b.iterator();
					while (ib.hasNext()) {
						CubicSegment2 nib = ib.next();

						Rect bnn = cache.computeIfAbsent(nib, (x) -> nib.boundingBox());

						Log.log("intersection", "checking " + baa + " / " + bnn + "");


						if (baa.intersects(bnn)) {

							Log.log("intersection", " -- intersect");

							if (Math.max(baa.w, baa.h) < sz && Math.max(bnn.w, bnn.h) < sz) {
								ib.remove();
								out.add(new Pair<>(aa, nib));
							} else {
								na.add(aa);
								nb.add(nib);

							}
							//continue o;
						}
					}

					Log.log("intersection", "nothing intersects with " + baa);
				}

				a.clear();
				b.clear();

				for (CubicSegment2 c : na) {
					Rect r = cache.computeIfAbsent(c, (x) -> c.boundingBox());
					if (Math.max(r.w, r.y) < sz) a.add(c);
					else {
						Pair<CubicSegment2, CubicSegment2> lr = c.split(0.5f);
						a.add(lr.first);
						a.add(lr.second);
					}

				}

				for (CubicSegment2 c : nb) {
					Rect r = cache.computeIfAbsent(c, (x) -> c.boundingBox());
					if (Math.max(r.w, r.y) < sz) b.add(c);
					else {
						Pair<CubicSegment2, CubicSegment2> lr = c.split(0.5f);
						b.add(lr.first);
						b.add(lr.second);
					}

				}

				gen++;
			}

			if (gen == 20) Log.log("intersection", "warning: bailed");

			return out;

		}


		public List<Double> findYZeroRoots() {
			int subdiv = 20;

			List<Double> dd = new ArrayList<>();

			for (int i = 0; i < subdiv; i++) {
				double alphaStart = (i - 0.75f) / subdiv;
				double alphaEnd = (i + 0.75f) / subdiv;
				double yStart = evaluateCubicFrame(a.y, b.y, c.y, d.y, alphaStart).x;
				double yEnd = evaluateCubicFrame(a.y, b.y, c.y, d.y, alphaEnd).x;
				if (Math.signum(yStart) != Math.signum(yEnd)) {
					Double t = findYZeroRoot_bracketed(alphaStart, alphaEnd);
					if (t != null) dd.add(t);
				}
			}

			for (int i = 0; i < dd.size(); i++) {
				for (int j = i + 1; j < dd.size(); j++) {
					if (Math.abs(dd.get(i)
						       .doubleValue() - dd.get(j)
									  .doubleValue()) < 1e-6) {
						dd.remove(j);
						j--;
					}
				}
			}

			// filter dd for repeated roots

			return dd;
		}

		private Double findYZeroRoot_bracketed(double yStart, double yEnd) {

			double t = (yStart + yEnd) / 2;

			for (int i = 0; i < 10; i++) {
				Vec2 D = evaluateCubicFrame(a.y, b.y, c.y, d.y, t);
				double dt = D.x / D.y;
				t = t - dt;
				if (Math.abs(dt) < 1e-5) {
					return t;
				}
			}

			if (t >= yStart && t <= yEnd && evaluateCubicFrame(a.y, b.y, c.y, d.y, t).x < 1e-6) {
				return t;
			}

			return null;
		}

		public List<Double> findXZeroRoots() {
			int subdiv = 20;

			List<Double> dd = new ArrayList<>();

			for (int i = 0; i < subdiv; i++) {
				double alphaStart = (i - 0.75f) / subdiv;
				double alphaEnd = (i + 0.75f) / subdiv;
				double yStart = evaluateCubicFrame(a.x, b.x, c.x, d.x, alphaStart).x;
				double yEnd = evaluateCubicFrame(a.x, b.x, c.x, d.x, alphaEnd).x;
				if (Math.signum(yStart) != Math.signum(yEnd)) {
					Double t = findXZeroRoot_bracketed(alphaStart, alphaEnd);
					if (t != null) dd.add(t);
				}
			}

			for (int i = 0; i < dd.size(); i++) {
				for (int j = i + 1; j < dd.size(); j++) {
					if (Math.abs(dd.get(i)
						       .doubleValue() - dd.get(j)
									  .doubleValue()) < 1e-6) {
						dd.remove(j);
						j--;
					}
				}
			}

			// filter dd for repeated roots

			return dd;
		}

		private Double findXZeroRoot_bracketed(double yStart, double yEnd) {

			double t = (yStart + yEnd) / 2;

			for (int i = 0; i < 10; i++) {
				Vec2 D = evaluateCubicFrame(a.x, b.x, c.x, d.x, t);
				double dt = D.x / D.y;
				t = t - dt;
				if (Math.abs(dt) < 1e-5) {
					return t;
				}
			}

			if (t >= yStart && t <= yEnd && evaluateCubicFrame(a.x, b.x, c.x, d.x, t).x < 1e-6) {
				return t;
			}

			return null;
		}

		public List<Double> findDYZeroRoots() {
			int subdiv = 20;

			List<Double> dd = new ArrayList<>();

			for (int i = 0; i < subdiv; i++) {
				double alphaStart = (i - 0.75f) / subdiv;
				double alphaEnd = (i + 0.75f) / subdiv;
				double yStart = evaluateCubicFrame(a.y, b.y, c.y, d.y, alphaStart).y;
				double yEnd = evaluateCubicFrame(a.y, b.y, c.y, d.y, alphaEnd).y;
				if (Math.signum(yStart) != Math.signum(yEnd)) {
					Double t = findDYZeroRoot_bracketed(alphaStart, alphaEnd);
					if (t != null) dd.add(t);
				}
			}

			for (int i = 0; i < dd.size(); i++) {
				for (int j = i + 1; j < dd.size(); j++) {
					if (Math.abs(dd.get(i)
						       .doubleValue() - dd.get(j)
									  .doubleValue()) < 1e-6) {
						dd.remove(j);
						j--;
					}
				}
			}

			// filter dd for repeated roots

			return dd;
		}

		private Double findDYZeroRoot_bracketed(double yStart, double yEnd) {

			double t = (yStart + yEnd) / 2;

			for (int i = 0; i < 10; i++) {
				Vec2 D = evaluateDCubicFrame(a.y, b.y, c.y, d.y, t);
				double dt = D.x / D.y;
				t = t - dt;
				if (Math.abs(dt) < 1e-5) {
					return t;
				}
			}

			if (t >= yStart && t <= yEnd && evaluateDCubicFrame(a.y, b.y, c.y, d.y, t).x < 1e-6) {
				return t;
			}

			return null;
		}

		public List<Double> findDXZeroRoots() {
			int subdiv = 20;

			List<Double> dd = new ArrayList<>();

			for (int i = 0; i < subdiv; i++) {
				double alphaStart = (i - 0.75f) / subdiv;
				double alphaEnd = (i + 0.75f) / subdiv;
				double yStart = evaluateCubicFrame(a.x, b.x, c.x, d.x, alphaStart).y;
				double yEnd = evaluateCubicFrame(a.x, b.x, c.x, d.x, alphaEnd).y;
				if (Math.signum(yStart) != Math.signum(yEnd)) {
					Double t = findDXZeroRoot_bracketed(alphaStart, alphaEnd);
					if (t != null) dd.add(t);
				}
			}

			for (int i = 0; i < dd.size(); i++) {
				for (int j = i + 1; j < dd.size(); j++) {
					if (Math.abs(dd.get(i)
						       .doubleValue() - dd.get(j)
									  .doubleValue()) < 1e-6) {
						dd.remove(j);
						j--;
					}
				}
			}

			// filter dd for repeated roots

			return dd;
		}

		private Double findDXZeroRoot_bracketed(double yStart, double yEnd) {

			double t = (yStart + yEnd) / 2;

			for (int i = 0; i < 10; i++) {
				Vec2 D = evaluateDCubicFrame(a.x, b.x, c.x, d.x, t);
				double dt = D.x / D.y;
				t = t - dt;
				if (Math.abs(dt) < 1e-5) {
					return t;
				}
			}

			if (t >= yStart && t <= yEnd && evaluateDCubicFrame(a.x, b.x, c.x, d.x, t).x < 1e-6) {
				return t;
			}

			return null;
		}

		public double closestToPoint(Vec2 v) {

			// right now we split this segment into two pieces to reduce the changes of getting stuck in a local minimum.

			BrentOptimizer o = new BrentOptimizer(1e-4, 1e-4);
			UnivariatePointValuePair p1 = o.optimize(new UnivariateObjectiveFunction((x) -> evaluateCubicFrame(a, b, c, d, x, new Vec2()).distance(v)),
								 new SearchInterval(0, 0.5, 0.25), GoalType.MINIMIZE);
			UnivariatePointValuePair p2 = o.optimize(new UnivariateObjectiveFunction((x) -> evaluateCubicFrame(a, b, c, d, x, new Vec2()).distance(v)),
								 new SearchInterval(0.5, 1, 0.75), GoalType.MINIMIZE);

			if (p1.getValue() < p2.getValue()) return p1.getPoint();
			else return p2.getPoint();

		}

	}

}
