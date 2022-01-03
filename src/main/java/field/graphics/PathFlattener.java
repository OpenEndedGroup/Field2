package field.graphics;

import field.linalg.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


public class PathFlattener {

	private final FLine c;
	private final FLinesAndJavaShapes.Cursor cursor;

	public class Mapping {
		public double dotStart; // node.t
		public double dotEnd; // node.t
		public Vec3 start;
		public Vec3 end;
		public double cumulativeDistanceAtEnd;

		@Override
		public String toString() {
			return dotStart+"->"+dotEnd+" > "+cumulativeDistanceAtEnd;
		}
	}

	List<Mapping> mappings = new ArrayList<Mapping>();
	private final double tol;

	public PathFlattener(FLine c, float tol) {
		this.c = c;
		this.tol = tol;
		this.cursor = new FLinesAndJavaShapes.Cursor(c, tol);

		int index = 0;


		while (cursor.hasNext()) {
			Vec3[] v = cursor.next();
//			System.out.println(" cursor :"+ Arrays.asList(v)+" "+index);
			if (v.length==4) {
				emitCubicFrame(index, index+1, v[0], v[1], v[2], v[3], 0);
			} else if (v.length==2) {
				emitLinearFrame(index, index+1, v[0], v[1]);
			}
			index++;
		}
	}

	public List<Mapping> getMappings() {
		return mappings;
	}

	Comparator searchDistance = new Comparator() {
		public int compare(Object o1, Object o2) {
			double f1 = o1 instanceof Number ? ((Number) o1).doubleValue() : ((Mapping) o1).cumulativeDistanceAtEnd;
			double f2 = o2 instanceof Number ? ((Number) o2).doubleValue() : ((Mapping) o2).cumulativeDistanceAtEnd;
			return Double.compare(f1, f2);
		}
	};

	Comparator searchDot = new Comparator() {
		public int compare(Object o1, Object o2) {
			double f1 = o1 instanceof Number ? ((Number) o1).doubleValue() : ((Mapping) o1).dotEnd;
			double f2 = o2 instanceof Number ? ((Number) o2).doubleValue() : ((Mapping) o2).dotEnd;
			return Double.compare(f1, f2);
		}
	};

	public double length() {
		if (mappings.size()<1) return 0;
		return mappings.get(mappings.size() - 1).cumulativeDistanceAtEnd;
	}

	public List<Mapping> getMappingSublist(double length)
	{
		int found = Collections.binarySearch((List) mappings, new Double(length), searchDistance);
		if (found >= 0)
			return mappings.subList(0, found+1);
		return mappings.subList(0, -found-1+1);
	}


	public double lengthToDot(double length) {
		if (length==0) return 0;
		if (mappings.size()==0) return 0;

		int found = Collections.binarySearch((List) mappings, new Double(length), searchDistance);
		if (found >= 0)
			return mappings.get(found).dotEnd;

		int leftOf = -found - 1;
		int rightOf = leftOf - 1;
		if (leftOf > mappings.size() - 1)
			return mappings.get(mappings.size() - 1).dotEnd;

		double l1 = rightOf >= 0 ? mappings.get(rightOf).cumulativeDistanceAtEnd : 0;
		double l2 = mappings.get(leftOf).cumulativeDistanceAtEnd;

		if (l2 == l1)
			return mappings.get(leftOf).dotEnd;
		double x = (length - l1) / (l2 - l1);
		double de = mappings.get(leftOf).dotStart * (1 - x) + x * mappings.get(leftOf).dotEnd;

		return de;
	}

	public double dotToLength(double dot) {

		int found = Collections.binarySearch((List) mappings, new Double(dot), searchDot);

		if (found >= 0)
			return mappings.get(found).cumulativeDistanceAtEnd;

		int leftOf = -found - 1;
		int rightOf = leftOf - 1;

		if (leftOf > mappings.size() - 1)
			return mappings.get(mappings.size() - 1).cumulativeDistanceAtEnd;


		double l1 = mappings.get(leftOf).dotStart;
		double l2 = mappings.get(leftOf).dotEnd;

		if (l2 == l1)
			return mappings.get(rightOf).cumulativeDistanceAtEnd;
		double x = (dot - l1) / (l2 - l1);
		double de = (rightOf>=0 ? mappings.get(rightOf).cumulativeDistanceAtEnd : 0) * (1 - x) + x * mappings.get(leftOf).cumulativeDistanceAtEnd;

		return de;
	}

	private void emitLinearFrame(double dotStart, double dotEnd, Vec3 a, Vec3 b) {
		Mapping m = new Mapping();
		m.start = a;
		m.end = b;
		m.dotStart = dotStart;
		m.dotEnd = dotEnd;
		if (mappings.size() == 0)
			m.cumulativeDistanceAtEnd = b.distance(a);
		else
			m.cumulativeDistanceAtEnd = b.distance(a) + mappings.get(mappings.size() - 1).cumulativeDistanceAtEnd;
		mappings.add(m);
	}

	Vec3 tmp = new Vec3();

	static int maxSubDiv = 10;
	private void emitCubicFrame(double dotStart, double dotEnd, Vec3 a, Vec3 c1, Vec3 c2, Vec3 b, int sub) {

		if (sub>maxSubDiv)
		{
			System.err.println("warning: maxSubDiv reached :"+dotStart+" "+dotEnd+" "+a+" "+c1+" "+c2+" "+b+" -> "+flatnessFor(a, c1, c2, b));
			return;
		}
		double f = flatnessFor(a, c1, c2, b);
		if (f > tol) {
			Vec3 c12 = new Vec3();
			Vec3 c21 = new Vec3();
			Vec3 m = new Vec3();

			FLinesAndJavaShapes.splitCubicFrame(a, c1 = new Vec3(c1), c2 = new Vec3(c2), b, 0.5f, c12, m, c21, tmp);

			double mp = dotStart + (dotEnd - dotStart) * 0.5f;

			emitCubicFrame(dotStart, mp, a, c1, c12, m, sub+1);
			emitCubicFrame(mp, dotEnd, m, c21, c2, b, sub+1);
		} else {
			emitLinearFrame(dotStart, dotEnd, a, b);
		}
	}

	private double flatnessFor(Vec3 a, Vec3 c1, Vec3 c2, Vec3 b) {
		//	return (double) CubicCurve2D.getFlatness(a.x, a.y, c1.x, c1.y, c2.x, c2.y, b.x, b.y);
		double f1 = FLinesAndJavaShapes.ptSegDistSq3(a.x, a.y, a.z, b.x, b.y, b.z, c1.x, c1.y, c1.z);
		double f2 = FLinesAndJavaShapes.ptSegDistSq3(a.x, a.y, a.z, b.x, b.y, b.z, c2.x, c2.y, c2.z);
		return Math.sqrt(Math.max(f1, f2));
	}

}
