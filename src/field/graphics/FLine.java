package field.graphics;

import field.linalg.Vec2;
import field.linalg.Vec3;
import field.utility.Curry;
import field.utility.Dict;
import field.utility.Rect;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Function;

import static field.graphics.FLinesAndJavaShapes.flineToJavaShape;
import static field.graphics.FLinesAndJavaShapes.javaShapeToFLine;

/**
 * Created by marc on 3/19/14.
 */
public class FLine {

	public FLine() {
	}

	public List<Node> nodes = new ArrayList<>();
	public Dict attributes = new Dict();
	private Map<Integer, String> auxProperties;

	long mod = 0;

	public Node node() {
		return nodes.get(nodes.size()-1);
	}


	public class Node {
		public final Vec3 to;
		public final Dict attributes = new Dict();

		transient float[][] flatAuxData;
		transient int[] flatAux;

		protected Node(Vec3 n) {
			this.to = new Vec3(n);
		}

		protected Node(float x, float y, float z) {
			this.to = new Vec3(x, y, z);
		}
	}

	public FLine add(Node n) {
		nodes.add(n);
		mod++;
		return this;
	}

	public void modify() {
		mod++;
	}

	public FLine moveTo(float x, float y) {
		return add(new MoveTo(x, y, 0));
	}

	public FLine lineTo(float x, float y) {
		return add(new LineTo(x, y, 0));
	}

	public FLine moveTo(float x, float y, float z) {
		return add(new MoveTo(x, y, z));
	}

	public FLine lineTo(float x, float y, float z) {
		return add(new LineTo(x, y, z));
	}

	public FLine cubicTo(float c1x, float c1y, float c2x, float c2y, float x, float y) {
		return add(new CubicTo(c1x, c1y, 0, c2x, c2y, 0, x, y, 0));
	}

	public FLine cubicTo(float c1x, float c1y, float c1z, float c2x, float c2y, float c2z, float x, float y, float z) {
		return add(new CubicTo(c1x, c1y, c1z, c2x, c2y, c2z, x, y, z));
	}

	public FLine rect(Rect r)
	{
		return rect(r.x, r.y, r.w, r.h);
	}

	public FLine rect(float x, float y, float w, float h)
	{
		this.moveTo(x, y);
		this.lineTo(x+w, y);
		this.lineTo(x + w, y + h);
		this.lineTo(x, y + h);
		return this.lineTo(x, y);
	}


	public class BookmarkCache {
		MeshBuilder.Bookmark start;
		MeshBuilder.Bookmark end;

		public BookmarkCache(MeshBuilder on) {
			start = on.bookmark();
			end = on.bookmark();
		}
	}

	WeakHashMap<MeshBuilder, BookmarkCache> cache = new WeakHashMap<>();

	public void clearCache() {
		cache.clear();
	}

	public void clearCache(MeshBuilder m) {
		cache.remove(m);
	}


	public boolean renderToPoints(MeshBuilder m, Curry.Function3<MeshAcceptor, Node, MoveTo, Node> moveTo, Curry.Function3<MeshAcceptor, Node, LineTo, Node> lineTo, Curry.Function3<MeshAcceptor, Node, CubicTo, Node> cubicTo) {

		BookmarkCache c = cache.computeIfAbsent(m, (k) -> new BookmarkCache(m));

		return m.skipTo(c.start, c.end, mod, () -> {

			flattenAuxProperties();
			m.open();
			try {
				Node a = null;
				for (int i = 0; i < nodes.size(); i++) {
					Node b = nodes.get(i);

					if (b instanceof MoveTo) {
						a = moveTo.apply(m, a, (MoveTo) b);
					} else {
						if (b instanceof LineTo) a = lineTo.apply(m, a, (LineTo) b);
						else if (b instanceof CubicTo) a = cubicTo.apply(m, a, (CubicTo) b);
						else throw new IllegalArgumentException(" unknown subclass ");
					}
				}
			} finally {
				m.close();
			}
		});
	}

	public boolean renderToPoints(MeshBuilder m, int fixedSizeForCubic) {
		return renderToPoints(m, this::renderMoveTo, this::renderLineTo, renderCubicTo(fixedSizeForCubic));
	}

	public boolean renderToLine(MeshBuilder m, Curry.Function3<MeshAcceptor, Node, MoveTo, Node> moveTo, Curry.Function3<MeshAcceptor, Node, LineTo, Node> lineTo, Curry.Function3<MeshAcceptor, Node, CubicTo, Node> cubicTo) {

		BookmarkCache c = cache.computeIfAbsent(m, (k) -> new BookmarkCache(m));

		return m.skipTo(c.start, c.end, mod, () -> {

			flattenAuxProperties();
			m.open();
			try {
				MeshBuilder.Bookmark start = null;
				// todo: AUX!

				Node a = null;
				for (int i = 0; i < nodes.size(); i++) {
					Node b = nodes.get(i);

					if (b instanceof MoveTo) {
						if (start != null) m.nextLine(start.at());
						a = moveTo.apply(m, a, (MoveTo) b);
						start = m.bookmark();
					} else {
						if (b instanceof LineTo) a = lineTo.apply(m, a, (LineTo) b);
						else if (b instanceof CubicTo) a = cubicTo.apply(m, a, (CubicTo) b);
						else throw new IllegalArgumentException(" unknown subclass ");
						if (start == null) start = m.bookmark();

					}
				}

				MeshBuilder.Bookmark end = m.bookmark();

				if (start != null && start.at() != end.at()) {
					m.nextLine(start.at());
				}
			} finally {
				m.close();
			}
		});
	}

	public boolean renderToLine(MeshBuilder m, int fixedSizeForCubic) {
		return renderToLine(m, this::renderMoveTo, this::renderLineTo, renderCubicTo(fixedSizeForCubic));
	}

	public boolean renderLineToMeshByStroking(MeshBuilder m, int fixedSizeForCubic, BasicStroke stroke) {
		BookmarkCache c = cache.computeIfAbsent(m, (k) -> new BookmarkCache(m));

		return m.skipTo(c.start, c.end, mod, () -> {
			Shape s = stroke.createStrokedShape(flineToJavaShape(this));
			FLine drawInstead = javaShapeToFLine(s);
			drawInstead.attributes.putAll(attributes);
			drawInstead.renderToMesh(m, fixedSizeForCubic);
		});
	}


	public boolean renderToMesh(MeshBuilder m, Curry.Function3<MeshAcceptor, Node, MoveTo, Node> moveTo, Curry.Function3<MeshAcceptor, Node, LineTo, Node> lineTo, Curry.Function3<MeshAcceptor, Node, CubicTo, Node> cubicTo) {

		BookmarkCache c = cache.computeIfAbsent(m, (k) -> new BookmarkCache(m));

		return m.skipTo(c.start, c.end, mod, () -> {

			MeshBuilder_tesselationSupport ts = m.getTessSupport();
			flattenAuxProperties();

			m.open();
			try {
				MeshBuilder.Bookmark start = null;

				Node a = null;
				ts.begin();

				for (int i = 0; i < nodes.size(); i++) {
					Node b = nodes.get(i);

					if (b instanceof MoveTo) {
						if (start != null) ts.endContour();
						ts.beginContour();
						a = moveTo.apply(ts, a, (MoveTo) b);
						start = m.bookmark();
					} else {
						if (b instanceof LineTo) a = lineTo.apply(ts, a, (LineTo) b);
						else if (b instanceof CubicTo) a = cubicTo.apply(ts, a, (CubicTo) b);
						else throw new IllegalArgumentException(" unknown subclass ");
						if (start == null) {
							start = m.bookmark();
							ts.beginContour();
							;
						}

					}
				}

				MeshBuilder.Bookmark end = m.bookmark();

				if (start != null && start.at() != end.at()) {
					ts.endContour();
					;
				}
				ts.end();
			} finally {
				m.close();
			}
		});
	}

	public void setAuxProperties(Map<Integer, String> propertiesToAuxChannels) {
		auxProperties = propertiesToAuxChannels;

	}

	private void flattenAuxProperties() {
		if (auxProperties == null || auxProperties.size() == 0) return;

		int[] flatAux = new int[auxProperties.size()];
		Dict.Prop[] flatAuxNames = new Dict.Prop[auxProperties.size()];
		int n = 0;
		for (Map.Entry<Integer, String> ii : auxProperties.entrySet()) {
			flatAux[n] = ii.getKey();
			flatAuxNames[n++] = new Dict.Prop(ii.getValue());
		}

		for (Node node : nodes) {
			node.flatAuxData = new float[flatAux.length][];
			node.flatAux = flatAux;
			for (int i = 0; i < flatAux.length; i++) {
				Object v = node.attributes.get(flatAuxNames[i]);
				if (v != null) node.flatAuxData[i] = Uniform.rewriteToFloatArray(v);
			}
		}

		float[][] previous = new float[flatAux.length][];
		int[] previousAt = new int[flatAux.length];
		for (int i = 0; i < nodes.size(); i++) {
			for (n = 0; n < flatAux.length; n++) {
				float[] a = nodes.get(i).flatAuxData[n];
				if ((a != null && previousAt[n] != i) || i == nodes.size() - 1) {
					fill(n, previousAt[n], i, nodes);
					previousAt[n] = i;
					previous[n] = a;
				}
			}
		}
	}

	private void fill(int slot, int start, int end, List<Node> target) {
		float[] a = target.get(start).flatAuxData[slot];
		float[] b = target.get(end).flatAuxData[slot];
		if (a == null && b == null) return;

		int dim;
		if (a == null) dim = b.length;
		else if (b == null) dim = a.length;
		else dim = Math.min(b.length, a.length);

		for (int i = start; i < end + 1; i++) {
			float alpha = (i - start) / ((float) end - start);
			float[] val = interpolate(alpha, a, b, dim);
			target.get(i).flatAuxData[slot] = val;
		}
	}

	private float[] interpolate(float alpha, float[] a, float[] b, int dim) {
		if (a == null) return b;
		if (b == null) return a;
		float[] r = new float[dim];
		for (int i = 0; i < r.length; i++)
			r[i] = a[i] * (1 - alpha) + b[i] * alpha;
		return r;
	}

	public boolean renderToMesh(MeshBuilder m, int fixedSizeForCubic) {
		return renderToMesh(m, this::renderMoveTo, this::renderLineTo, renderCubicTo(fixedSizeForCubic));
	}

	public Node renderMoveTo(MeshAcceptor m, Node from, MoveTo to) {
		if (to.flatAuxData != null) for (int i = 0; i < to.flatAuxData.length; i++) {
			int channel = to.flatAux[i];
			float[] value = to.flatAuxData[i];
			if (value != null && channel > 0) m.aux(channel, value);
		}
		m.nextVertex(to.to.x, to.to.y, to.to.z);
		return to;
	}

	public Node renderLineTo(MeshAcceptor m, Node from, LineTo to) {
		if (to.flatAuxData != null) for (int i = 0; i < to.flatAuxData.length; i++) {
			int channel = to.flatAux[i];
			float[] value = to.flatAuxData[i];
			if (value != null && channel > 0) m.aux(channel, value);
		}
		m.nextVertex(to.to.x, to.to.y, to.to.z);
		return to;
	}


	public Curry.Function3<MeshAcceptor, Node, CubicTo, Node> renderCubicTo(int fixedSize) {
		return (meshBuilder, from, to) -> {
			Vec3 o = new Vec3();

			for (int i = 0; i < fixedSize; i++) {
				float alpha = (i + 1f) / fixedSize;
				o = evaluateCubicFrame(from.to.x, from.to.y, from.to.z, to.c1.x, to.c1.y, to.c1.z, to.c2.x, to.c2.y, to.c2.z, to.to.x, to.to.y, to.to.z, alpha, o);

				if (from.flatAuxData != null) for (int j = 0; j < from.flatAuxData.length; j++) {
					int channel = from.flatAux[j];
					float[] a = from.flatAuxData[j];
					float[] b = to.flatAuxData[j];
					if (a==null && b==null)
						continue;
					float[] r = interpolate(alpha, a, b, a==null ? b.length : a.length);
					if (r != null && channel > 0) meshBuilder.aux(channel, r);
				}

				meshBuilder.nextVertex(o.x, o.y, o.z);
			}
			return to;
		};

	}


	public class MoveTo extends Node {

		public MoveTo(Vec3 to) {
			super(to);
		}

		public MoveTo(float x, float y, float z) {
			super(new Vec3(x, y, z));
		}

		public MoveTo(Vec2 to) {
			super(new Vec3(to.x, to.y, 0));
		}
	}

	public class LineTo extends Node {
		public LineTo(Vec3 to) {
			super(to);
		}

		public LineTo(Vec2 to) {
			super(to.x, to.y, 0);
		}

		public LineTo(float x, float y, float z) {
			super(x, y, z);
		}

	}

	public class CubicTo extends Node {
		public final Vec3 c1;
		public final Vec3 c2;

		public CubicTo(Vec3 c1, Vec3 c2, Vec3 to) {
			super(to);
			this.c1 = new Vec3(c1);
			this.c2 = new Vec3(c2);
		}

		public CubicTo(Vec2 c1, Vec2 c2, Vec2 to) {
			super(to.x, to.y, 0);
			this.c1 = new Vec3(c1.x, c1.y, 0);
			this.c2 = new Vec3(c2.x, c2.y, 0);
		}

		public CubicTo(float c1x, float c1y, float c1z, float c2x, float c2y, float c2z, float x, float y, float z) {
			super(x, y, z);
			this.c1 = new Vec3(c1x, c1y, c1z);
			this.c2 = new Vec3(c2x, c2y, c2z);
		}
	}

	static public Vec2 evaluateCubicFrame(float ax, float ay, float c1x, float c1y, float c2x, float c2y, float bx, float by, float alpha, Vec2 out) {
		if (out == null) out = new Vec2();

		float oma = 1 - alpha;
		float oma2 = oma * oma;
		float oma3 = oma2 * oma;
		float alpha2 = alpha * alpha;
		float alpha3 = alpha2 * alpha;

		out.x = ax * oma3 + 3 * c1x * alpha * oma2 + 3 * c2x * alpha2 * oma + bx * alpha3;
		out.y = ay * oma3 + 3 * c1y * alpha * oma2 + 3 * c2y * alpha2 * oma + by * alpha3;

		return out;
	}

	static public Vec3 evaluateCubicFrame(float ax, float ay, float az, float c1x, float c1y, float c1z, float c2x, float c2y, float c2z, float bx, float by, float bz, float alpha, Vec3 out) {
		if (out == null) out = new Vec3();

		float oma = 1 - alpha;
		float oma2 = oma * oma;
		float oma3 = oma2 * oma;
		float alpha2 = alpha * alpha;
		float alpha3 = alpha2 * alpha;

		out.x = ax * oma3 + 3 * c1x * alpha * oma2 + 3 * c2x * alpha2 * oma + bx * alpha3;
		out.y = ay * oma3 + 3 * c1y * alpha * oma2 + 3 * c2y * alpha2 * oma + by * alpha3;
		out.z = az * oma3 + 3 * c1z * alpha * oma2 + 3 * c2z * alpha2 * oma + bz * alpha3;

		return out;
	}

	public FLine byTransforming(Function<Vec3, Vec3> spaceTransform) {

		FLine f = new FLine();
		f.attributes.putAll(attributes);
		for(Node n : nodes)
		{
			if (n instanceof MoveTo)
				f.add(new MoveTo(spaceTransform.apply(n.to)));
			else if (n instanceof LineTo)
				f.add(new LineTo(spaceTransform.apply(n.to)));
			else if (n instanceof MoveTo)
				f.add(new CubicTo(spaceTransform.apply(((CubicTo)n).to),spaceTransform.apply(((CubicTo)n).c2),spaceTransform.apply(n.to)));
			f.nodes.get(f.nodes.size()-1).attributes.putAll(n.attributes);
		}
		return f;
	}


	public long getModCount() {
		return mod;
	}

}
