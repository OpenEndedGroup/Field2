package trace.util;

import field.linalg.Vec3;
import field.utility.Triple;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;


public class TCB3 {

	boolean dirty = true;

	public class Node {
		protected double x;
		protected Vec3 v;
		private double subX = System.identityHashCode(this);
		private int n;

		public Vec3 tO, tI;

		public Node(double x, Vec3 v) {
			this.x = x;
			this.v = new Vec3(v);
			dirty = true;
			TCB3.this.n.add(this);
		}

		public void setSubX(double sub_x) {
			this.subX = subX;
		}

		public Function<Triple<Node, Node, Node>, Void> refresh;

		@Override
		public String toString() {
			return v + "@" + x + "(" + tI + "->" + tO + "):" + n;
		}

		public void setV(Vec3 f) {
			if (!v.equals(f)) {
				v.set(f);
				dirty = true;
			}
		}

		public Vec3 getV() {
			return v;
		}

		public double getX() {
			return x;
		}
	}

	public void respaceByDistance() {
		double d = 0;
		for (int i = 0; i < n.size(); i++) {
			n.get(i).x = d;
			if (i < n.size() - 1)
				d += n.get(i).v.distance(n.get(i + 1).v);
		}
		dirty = true;
	}

	public Node newNode(double x, Vec3 y) {
		Node n = new Node(x, y);
		return n;
	}

	public Function<Triple<Node, Node, Node>, Void> tcb(final double it, final double ic, final double ib) {
		return new Function<Triple<Node, Node, Node>, Void>() {

			double t = it;
			double c = ic;
			double b = ib;

			@Override
			public Void apply(Triple<Node, Node, Node> in) {

				Vec3 d1 = in.third == null ? new Vec3() : (new Vec3(in.third.v).sub(in.second.v)).scale((float) ((1 - t) * (1 + c) * (1 - b)));
				Vec3 d2 = in.first == null ? new Vec3() : (new Vec3(in.second.v).sub(in.first.v)).scale((float) ((1 - t) * (1 - c) * (1 + b)));

				Vec3 e1 = in.third == null ? new Vec3() : (new Vec3(in.third.v).sub(in.second.v)).scale((float) ((1 - t) * (1 - c) * (1 - b)));
				Vec3 e2 = in.first == null ? new Vec3() : (new Vec3(in.second.v).sub(in.first.v)).scale((float) ((1 - t) * (1 + c) * (1 + b)));

				in.second.tI = (new Vec3(d1).add(d2)).scale(0.5f);
				in.second.tO = (new Vec3(e1).add(e2)).scale(0.5f);

				if (in.first != null && in.third != null)
					in.second.tI.scale((float) (2 * (in.second.x - in.first.x) / (in.second.x - in.first.x + in.third.x - in.second.x)));
				if (in.first != null && in.third != null)
					in.second.tO.scale((float) (2 * (in.third.x - in.second.x) / (in.second.x - in.first.x + in.third.x - in.second.x)));

				return null;
			}
		};
	}

	public void tcbAll() {
		dirty = true;
		for (Node nn : n)
			nn.refresh = tcb(0, 0, 0);
	}

	public List<Node> n = new ArrayList<>();

	public Vec3 evaluateFrame(Node left, Node right, double s, Vec3 storage) {
		if (storage == null)
			storage = new Vec3();
		if (left == null && right == null)
			throw new NullPointerException();
		if (left == null)
			return storage.set(right.v);
		if (right == null)
			return storage.set(left.v);
		double h0 = 2 * s * s * s - 3 * s * s + 1;
		double h1 = -2 * s * s * s + 3 * s * s;
		double h2 = s * s * s - 2 * s * s + s;
		double h3 = s * s * s - s * s;

		storage.scale(0).fma(left.v, h0).fma(right.v, h1).fma(left.tO, h2).fma(right.tI, h3);

		return storage;
		// double v = left.v * h0 + right.v * h1 + left.tO * h2 +
		// right.tI * h3;
		// return v;
	}

	Comparator<Object> finder = new Comparator<Object>() {
		@Override
		public int compare(Object o1, Object o2) {
			double v1 = o1 instanceof Node ? ((Node) o1).x : ((Number) o1).doubleValue();
			double v2 = o2 instanceof Node ? ((Node) o2).x : ((Number) o2).doubleValue();
			return Double.compare(v1, v2);
		}
	};

	public Triple<Node, Node, Double> bracket(double x) {
		clean();
		int index = Collections.binarySearch((List) n, (Double) x, finder);
		if (index >= 0)
			return new Triple<>(n.get(index), null, 0.0);
		if (-index - 1 >= n.size())
			return new Triple<>(null, n.get(n.size() - 1), 0.0);

		return new Triple<>(n.get(safeIndex(-index - 2)), n.get(safeIndex(-index - 1)), alpha(x, n.get(safeIndex(-index - 2)), n.get(safeIndex(-index - 1))));
	}

	private int safeIndex(int i) {
		if (i<0) i = 0;
		if (i>=n.size()) i = n.size()-1;
		return i;
	}

	private double alpha(double x, Node a, Node b) {
		if (a.x == b.x)
			return 0.5f;
		return (x - a.x) / (b.x - a.x);
	}

	public void clean() {
		if (!dirty)
			return;

		Collections.sort(n, new Comparator<Node>() {
			public int compare(Node o1, Node o2) {
				if (o1.x == o2.x)
					return Double.compare(o1.subX, o2.subX);
				return Double.compare(o1.x, o2.x);
			}
		});

		for (int i = 0; i < n.size(); i++)
			n.get(i).n = i;

		for (int i = 0; i < n.size(); i++) {
			Node nn = n.get(i);
			if (nn.refresh == null)
				continue;
			Node left = i > 0 ? n.get(i - 1) : null;
			Node right = i < n.size() - 1 ? n.get(i + 1) : null;
//			System.out.println("@ " + i + " " + left + " " + nn + " " + right);
			nn.refresh.apply(new Triple<>(left, nn, right));
		}

		dirty = false;
	}

	public double duration() {
		clean();
		return n.get(n.size() - 1).x;
	}

	public Vec3 evaluate(double a, Vec3 out) {
		Triple<Node, Node, Double> q = this.bracket(a);

		if (q == null) {
			System.out.println(" could not bracket <" + a + "> inside <" + this.n + ">");
			return null;
		}

		return evaluateFrame(q.first, q.second, q.third.doubleValue(), out);
	}

	static public TCB3 fromPoints(List<Vec3> a, float t, float c, float b) {
		TCB3 tcb = new TCB3();
		int index = 0;
		for (Vec3 vv : a) {
			Node n = tcb.newNode(index++, vv);
			n.refresh = tcb.tcb(t, c, b);
		}
		return tcb;
	}

	static public TCB3 fromPoints(TCB3 refresh, List<Vec3> a, float t, float c, float b) {
		if (refresh == null || refresh.n.size() != a.size())
			return fromPoints(a, t, c, b);
		return refreshContents(refresh, a);
	}

	static public TCB3 refreshContents(TCB3 into, List<Vec3> a) {

		if (into.n.size() != a.size())
			throw new IllegalArgumentException(into.n.size() + " != " + a.size());
		int index = 0;
		for (Vec3 vv : a) {
			Node n = into.n.get(index++);
			n.setV(vv);
		}
		return into;
	}

	public TCB3 copy() {

		TCB3 r = new TCB3();
		for(Node nn : n)
		{
			Node n2 = r.newNode(nn.x, new Vec3(nn.v));
			n2.refresh = nn.refresh;
		}
		r.dirty = true;
		r.clean();
		return r;

	}

}
