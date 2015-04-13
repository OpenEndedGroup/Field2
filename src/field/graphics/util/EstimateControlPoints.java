package field.graphics.util;

import field.graphics.FLinesAndJavaShapes;
import field.linalg.Vec2;
import field.linalg.Vec3;
import field.utility.Log;
import field.utility.Triple;
import org.apache.commons.math3.optim.*;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.BOBYQAOptimizer;
import org.apache.commons.math3.optim.univariate.BrentOptimizer;
import org.apache.commons.math3.optim.univariate.SearchInterval;
import org.apache.commons.math3.optim.univariate.UnivariateObjectiveFunction;
import org.apache.commons.math3.optim.univariate.UnivariatePointValuePair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * Created by marc on 1/18/15.
 */
public class EstimateControlPoints {

	public List<FLinesAndJavaShapes.CubicSegment3> curves = new ArrayList<>();

	public EstimateControlPoints() {

	}

	static public Result standardFit(Vec3[] q, FLinesAndJavaShapes.CubicSegment3 initialEstimate) {
		EstimateControlPoints e = new EstimateControlPoints();
		Result r = null;

		for (int i = 0; i < 4; i++) {
			FLinesAndJavaShapes.CubicSegment3 m = initialEstimate;
			if (initialEstimate == null) {
				m = e.estimate(q, e.averageError(q, 10), 20);
			} else {
			}
			FLinesAndJavaShapes.CubicSegment3 c = e.reestimate(m, q, e.averageError(q));

			Triple<Integer, Double, Double> t = e.maxErrorAt(q, c);
			if (r == null || t.third < r.maxDistance) {
				r = new Result(c, t.third, t.second, t.first);
			}
		}
		return r;
	}

	static public Result standardFit2(Vec3[] q, FLinesAndJavaShapes.CubicSegment3 initialEstimate) {
		EstimateControlPoints e = new EstimateControlPoints();
		Result r = null;

		for (int i = 0; i < 4; i++) {
			FLinesAndJavaShapes.CubicSegment3 m = initialEstimate;
			if (initialEstimate == null) {
				m = e.estimate(q, e.averageError(q, 10), 20);
			} else {
			}
			FLinesAndJavaShapes.CubicSegment3 c = e.reestimate2(m, q, e.averageError(q));

			Triple<Integer, Double, Double> t = e.maxErrorAt(q, c);
			if (r == null || t.third < r.maxDistance) {
				r = new Result(c, t.third, t.second, t.first);
			}
		}
		return r;
	}

	public Triple<Double, Double, Vec3> maximizeAlongCurve(FLinesAndJavaShapes.CubicSegment3 c, Function<Vec3, Double> m, double from, double to) {
		Vec3 o = new Vec3();
		double best = Double.NEGATIVE_INFINITY;
		double initial = 0;
		int samples = 50;
		for (int i = 1; i < samples - 1; i++) {
			double alpha = from+(to-from)*i / (samples - 1f);
			FLinesAndJavaShapes.evaluateCubicFrame(c.a, c.b, c.c, c.d, alpha, o);
			double score = m.apply(o);
			if (score > best) {
				best = score;
				initial = alpha;
			}
		}


		BrentOptimizer opt = new BrentOptimizer(1e-4, 1e-4);
		UnivariatePointValuePair v = opt.optimize(GoalType.MAXIMIZE, new MaxEval(200), new MaxIter(200), new SearchInterval(from, to, initial), new UnivariateObjectiveFunction(x -> {
			FLinesAndJavaShapes.evaluateCubicFrame(c.a, c.b, c.c, c.d, x, o);
			double val = m.apply(o);
			return val;
		}));

		return new Triple<>(v.getPoint(), v.getValue(), FLinesAndJavaShapes.evaluateCubicFrame(c.a, c.b, c.c, c.d, v.getPoint(), o));
	}

	public Function<Vec3, Double> distanceToData(Vec3[] d) {
		return x -> {
			double t = Double.POSITIVE_INFINITY;
			for (Vec3 vv : d) {
				double s = vv.distanceFromSquared(x);
				if (s<t)
					t = s;
			}
			return t;
		};
	}

	public <T> Function<T, Double> negate(Function<T, Double> w) {
		return x -> -w.apply(x);
	}

	public Function<Vec3, Double> negateAndSlope(Function<Vec3, Double> w, Vec3 from, double slope) {
		return x -> -w.apply(x)+from.distanceFrom(x)*slope;
	}



	public FLinesAndJavaShapes.CubicSegment3 estimate(Vec3[] p) {
		curves = new ArrayList<>();

		Vec3 p0 = p[0];
		Vec3 p3 = p[p.length - 1];

		Vec3 ac1 = new Vec3();
		Vec3 ac2 = new Vec3();
		int num = 0;

		for (int i = 1; i < p.length - 1; i++) {
			Vec3 p1 = p[i];
			double d01 = p1.distanceFrom(p0);
			double d31 = p1.distanceFrom(p3);
			for (int j = i + 1; j < p.length - 1; j++) {
				Vec3 p2 = p[j];

				double d02 = p2.distanceFrom(p0);
				double d32 = p2.distanceFrom(p3);

				double d12 = p1.distanceFrom(p2);
				FLinesAndJavaShapes.CubicSegment3 c = null;
				if (d01 + d32 < d31 + d02) {
					// p0, p1, p2, p3
					double u = d01 / (d01 + d32 + d12);
					double v = (d01 + d12) / (d01 + d32 + d12);

					c = CurveThroughFourPoints.interpolate(p0, u, p1, v, p2, p3);
				} else {
					// p0, p2, p1, p3
					double u = d02 / (d01 + d32 + d12);
					double v = (d02 + d12) / (d01 + d32 + d12);

					c = CurveThroughFourPoints.interpolate(p0, u, p2, v, p1, p3);
				}

				curves.add(c);
				ac1.add(c.b);
				ac2.add(c.c);
				num++;


			}
		}


		return new FLinesAndJavaShapes.CubicSegment3(p0, ac1.scale(1.0 / num), ac2.scale(1.0 / num), p3);

	}

	public FLinesAndJavaShapes.CubicSegment3 estimate(Vec3[] p, Function<FLinesAndJavaShapes.CubicSegment3, Double> weight) {
		curves = new ArrayList<>();

		Vec3 p0 = p[0];
		Vec3 p3 = p[p.length - 1];

		Vec3 ac1 = new Vec3();
		Vec3 ac2 = new Vec3();
		double num = 0;

		for (int i = 1; i < p.length - 1; i++) {
			Vec3 p1 = p[i];
			double d01 = p1.distanceFrom(p0);
			double d31 = p1.distanceFrom(p3);
			for (int j = i + 1; j < p.length - 1; j++) {
				Vec3 p2 = p[j];

				double d02 = p2.distanceFrom(p0);
				double d32 = p2.distanceFrom(p3);

				double d12 = p1.distanceFrom(p2);
				FLinesAndJavaShapes.CubicSegment3 c = null;
				if (d01 + d32 < d31 + d02) {
					// p0, p1, p2, p3
					double u = d01 / (d01 + d32 + d12);
					double v = (d01 + d12) / (d01 + d32 + d12);

					c = CurveThroughFourPoints.interpolate(p0, u, p1, v, p2, p3);
				} else {
					// p0, p2, p1, p3
					double u = d02 / (d01 + d32 + d12);
					double v = (d02 + d12) / (d01 + d32 + d12);

					c = CurveThroughFourPoints.interpolate(p0, u, p2, v, p1, p3);
				}

				curves.add(c);
				double w = weight.apply(c);
				ac1.add(c.b.scale(w));
				ac2.add(c.c.scale(w));
				num += w;
			}
		}


		return new FLinesAndJavaShapes.CubicSegment3(p0, ac1.scale(1.0 / num), ac2.scale(1.0 / num), p3);
	}

	public FLinesAndJavaShapes.CubicSegment3 reestimate(FLinesAndJavaShapes.CubicSegment3 initial, Vec3[] p, Function<FLinesAndJavaShapes.CubicSegment3, Double> weight) {

		BOBYQAOptimizer optimizer = new BOBYQAOptimizer(7, 50, 3);

		double[] dd = new double[]{initial.b.x, initial.b.y, initial.c.x, initial.c.y};

		double scale = initial.a.distanceFrom(initial.b) + initial.b.distanceFrom(initial.c) + initial.c.distanceFrom(initial.d);

		PointValuePair s = optimizer.optimize(new MaxEval(2000), GoalType.MINIMIZE, new InitialGuess(dd), new ObjectiveFunction(x -> {

			Vec3 C1 = new Vec3(x[0], x[1], 0);
			Vec3 C2 = new Vec3(x[2], x[3], 0);

			double sc = initial.a.distanceFrom(C1) + initial.d.distanceFrom(C2) + C1.distanceFrom(C2);

			FLinesAndJavaShapes.CubicSegment3 seg = new FLinesAndJavaShapes.CubicSegment3(initial.a, C1, C2, initial.d);
			Double w = weight.apply(seg) + sc;

//			Log.log("max", w);
			return w;

		}), new SimpleBounds(new double[]{dd[0] - scale, dd[1] - scale, dd[2] - scale, dd[3] - scale}, new double[]{dd[0] + scale, dd[1] + scale, dd[2] + scale, dd[3] + scale}));

		double[] x = s.getPoint();
		return new FLinesAndJavaShapes.CubicSegment3(initial.a, new Vec3(x[0], x[1], 0), new Vec3(x[2], x[3], 0), initial.d);

	}

	public FLinesAndJavaShapes.CubicSegment3 reestimate2(FLinesAndJavaShapes.CubicSegment3 initial, Vec3[] p, Function<FLinesAndJavaShapes.CubicSegment3, Double> weight) {

		BOBYQAOptimizer optimizer = new BOBYQAOptimizer(18, 50, 3);

		double[] dd = new double[]{initial.b.x, initial.b.y, initial.c.x, initial.c.y, initial.a.x, initial.a.y, initial.d.x, initial.d.y};

		double scale = initial.a.distanceFrom(initial.b) + initial.b.distanceFrom(initial.c) + initial.c.distanceFrom(initial.d);

		PointValuePair s = optimizer.optimize(new MaxEval(2000), GoalType.MINIMIZE, new InitialGuess(dd), new ObjectiveFunction(x -> {

			Vec3 C1 = new Vec3(x[0], x[1], 0);
			Vec3 C2 = new Vec3(x[2], x[3], 0);
			Vec3 P1 = new Vec3(x[4], x[5], 0);
			Vec3 P2 = new Vec3(x[6], x[7], 0);

			double sc = P1.distanceFrom(C1) + P2.distanceFrom(C2) + C1.distanceFrom(C2);

			FLinesAndJavaShapes.CubicSegment3 seg = new FLinesAndJavaShapes.CubicSegment3(P1, C1, C2, P2);
			Double w = weight.apply(seg) + sc;

//			Log.log("max", w);
			return w;

		}), new SimpleBounds(new double[]{dd[0] - scale, dd[1] - scale, dd[2] - scale, dd[3] - scale, dd[4] - scale, dd[5] - scale, dd[6] - scale, dd[7] - scale},
				     new double[]{dd[0] + scale, dd[1] + scale, dd[2] + scale, dd[3] + scale, dd[4] + scale, dd[5] + scale, dd[6] + scale, dd[7] + scale}));

		double[] x = s.getPoint();
		return new FLinesAndJavaShapes.CubicSegment3(new Vec3(x[4], x[5], 0), new Vec3(x[0], x[1], 0), new Vec3(x[2], x[3], 0), new Vec3(x[6], x[7], 0));

	}

	public FLinesAndJavaShapes.CubicSegment3 estimate(Vec3[] p, Function<FLinesAndJavaShapes.CubicSegment3, Double> weight, int samples) {
		curves = new ArrayList<>();

		Vec3 p0 = p[0];
		Vec3 p3 = p[p.length - 1];

		Vec3 ac1 = new Vec3();
		Vec3 ac2 = new Vec3();
		double num = 0;

		for (int zz = 0; zz < samples; zz++)
//		for(int i=1;i<p.length-1;i++)
		{
			int i = (int) (1 + (p.length - 2) * Math.random());
			int j = (int) (1 + (p.length - 2) * Math.random());
			Vec3 p1 = p[i];
			double d01 = p1.distanceFrom(p0);
			double d31 = p1.distanceFrom(p3);
			Vec3 p2 = p[j];

			double d02 = p2.distanceFrom(p0);
			double d32 = p2.distanceFrom(p3);

			double d12 = p1.distanceFrom(p2);
			FLinesAndJavaShapes.CubicSegment3 c = null;
			if (d01 + d32 < d31 + d02) {
				// p0, p1, p2, p3
				double u = d01 / (d01 + d32 + d12);
				double v = (d01 + d12) / (d01 + d32 + d12);

				c = CurveThroughFourPoints.interpolate(p0, u, p1, v, p2, p3);
			} else {
				// p0, p2, p1, p3
				double u = d01 / (d01 + d32 + d12);
				double v = (d01 + d12) / (d01 + d32 + d12);

				c = CurveThroughFourPoints.interpolate(p0, u, p2, v, p1, p3);
			}

			if (c == null) continue;

			curves.add(c);
			double w = weight.apply(c);
			ac1.add(c.b.scale(w));
			ac2.add(c.c.scale(w));
			num += w;
		}


		return new FLinesAndJavaShapes.CubicSegment3(p0, ac1.scale(1.0 / num), ac2.scale(1.0 / num), p3);
	}

	public Function<FLinesAndJavaShapes.CubicSegment3, Double> averageError(Vec3[] data) {
		return (c) -> {

			double e = 0;
			for (Vec3 vv : data) {
				NearestPoint n = new NearestPoint();
				n.NearestPointOnCurve(vv.toVec2(), c);
				e += n.getLastDistance();
			}
			return e;
		};
	}

	public Function<FLinesAndJavaShapes.CubicSegment3, Double> averageError(Vec3[] data, int samples) {
		return (c) -> {

			double e = 0;
			for (int i = 0; i < samples; i++) {
				Vec3 vv = data[((int) (1 + (data.length - 2) * Math.random()))];
				NearestPoint n = new NearestPoint();
				n.NearestPointOnCurve(vv.toVec2(), c);
				e += n.getLastDistance() * n.getLastDistance();
			}
			return 1 / (1e-8 + e * e);
		};
	}

	public Function<FLinesAndJavaShapes.CubicSegment3, Double> medianError(Vec3[] data, int samples) {
		return (c) -> {

			List<Double> e = new ArrayList<>();
			for (int i = 0; i < samples; i++) {
				Vec3 vv = data[((int) (1 + (data.length - 2) * Math.random()))];
				NearestPoint n = new NearestPoint();
				n.NearestPointOnCurve(vv.toVec2(), c);
				e.add(n.getLastDistance());
			}
			Collections.sort(e);
			return 1 / (1e-8 + Math.pow(e.get(e.size() / 2), 4));
		};
	}

	public Function<FLinesAndJavaShapes.CubicSegment3, Double> maxError(Vec3[] data, int samples) {
		return (c) -> {

			double e = Double.NEGATIVE_INFINITY;
			for (int i = 0; i < samples; i++) {
				Vec3 vv = data[((int) (1 + (data.length - 2) * Math.random()))];
				NearestPoint n = new NearestPoint();
				n.NearestPointOnCurve(vv.toVec2(), c);
				if (n.getLastDistance() > e) e = n.getLastDistance();
			}
			return 1 / (1e-8 + e * e * e * e);
		};
	}

	public Triple<Integer, Double, Double> maxErrorAt(Vec3[] data, FLinesAndJavaShapes.CubicSegment3 c) {
		double e = Double.NEGATIVE_INFINITY;
		int ii = 0;
		for (int i = 0; i < data.length; i++) {
			Vec3 vv = data[i];
			NearestPoint n = new NearestPoint();
			n.NearestPointOnCurve(vv.toVec2(), c);
			if (n.getLastDistance() > e) {
				e = n.getLastDistance();
				ii = i;
			}
		}

		NearestPoint np = new NearestPoint();
		Vec2 ca = np.NearestPointOnCurve(data[ii].toVec2(), c);


		return new Triple<>(ii, np.getLastT(), e);
	}

	static public class Result {
		public FLinesAndJavaShapes.CubicSegment3 best;
		public double maxDistance;
		public double maxDistanceAtT;
		public int maxDistanceAtI;

		public Result(FLinesAndJavaShapes.CubicSegment3 c, double maxDistance, double maxDistanceAtT, int maxDistanceAtI) {
			this.best = c;
			this.maxDistance = maxDistance;
			this.maxDistanceAtT = maxDistanceAtT;
			this.maxDistanceAtI = maxDistanceAtI;
		}
	}


}
