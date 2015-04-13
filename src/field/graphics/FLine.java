package field.graphics;

import field.dynalink.beans.StaticClass;
import field.linalg.*;
import field.nashorn.api.scripting.ScriptUtils;
import field.nashorn.internal.objects.ScriptFunctionImpl;
import field.nashorn.internal.runtime.ConsString;
import field.nashorn.internal.runtime.ScriptObject;
import field.nashorn.internal.runtime.linker.JavaAdapterFactory;
import field.utility.*;
import fieldbox.boxes.Box;
import fieldlinker.Linker;
import fieldnashorn.annotations.HiddenInAutocomplete;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.awt.*;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import static field.graphics.FLinesAndJavaShapes.flineToJavaShape;
import static field.graphics.FLinesAndJavaShapes.javaShapeToFLine;

/**
 * FLine is our main high level geometry container for lines, meshes and lists of points.
 * <p>
 * FLines consist of a list of drawing instructions, or nodes: line.moveTo(...).lineTo(...).cubicTo(...).lineTo(...).moveTo(...).lineTo(...) .etc
 * <p>
 * This is essentially the common postscript / pdf / java2d drawing model with a few key differences and refinements.
 * <p>
 * 1. the drawing instructions are in 3d. 2d (the z=0 plane) is a special case 2. attributes can be associated with both the line itself and with individual nodes. 3. per-node attributes that are
 * present in some places on the line and absent in others are interpolated 4. the structure is freely mutable, although changes to attributes are not automatically tracked (call line.modify() to
 * cause an explicit un-caching of this line). 5. the caching of the flattening of this line into MeshBuilder data (ready for OpenGL) cascades into MeshBuilder's cache structure.
 * <p>
 * So, we have three levels of caching in total: FLine caches whether or not the geometry has changed at all, MeshBuilder caches whether or not there's any point sending anything to the OpenGL
 * underlying Buffers or whether this piece of geometry can be skipped, and finally individual ArrayBuffers can elect to skip the upload to OpenGL. This means that static geometry is extremely cheap
 * to draw, and dynamic geometry that has the same number of vertices is relatively cheap, hence we use a constant subdivision policy by default (rather than a textbook recursive subdivision strategy)
 * for cubic splines.
 * <p>
 * We expect to have nicer interfaces to FLine and to utility classes supporting geometric operations in dynamic languages inside Field
 * <p>
 * For the code where properties inside attributes are given semantics look at FieldBox / FrameDrawer
 * <p>
 * TODO: port FrameDrawer's guts back out into something freestanding for the broader graphics system. It was a nice place to grow it, but it's more general than that, and we need to be able to attach
 * FLines to MeshBuilders in general
 */
public class FLine implements Supplier<FLine>, Linker.AsMap {

	public List<Node> nodes = new ArrayList<>();
	public Dict attributes = new Dict();
	transient protected Set<String> knownNonProperties;
	long mod = 0;
	WeakHashMap<MeshBuilder, BookmarkCache> cache = new WeakHashMap<>();
	WeakHashMap<MeshBuilder, BookmarkCache> cache_thickening = new WeakHashMap<>();
	private Map<Integer, String> auxProperties;

	public FLine() {
	}

	@HiddenInAutocomplete
	static public Vec2 evaluateCubicFrame(double ax, double ay, double c1x, double c1y, double c2x, double c2y, double bx, double by, double alpha, Vec2 out) {
		if (out == null) out = new Vec2();

		double oma = 1 - alpha;
		double oma2 = oma * oma;
		double oma3 = oma2 * oma;
		double alpha2 = alpha * alpha;
		double alpha3 = alpha2 * alpha;

		out.x = ax * oma3 + 3 * c1x * alpha * oma2 + 3 * c2x * alpha2 * oma + bx * alpha3;
		out.y = ay * oma3 + 3 * c1y * alpha * oma2 + 3 * c2y * alpha2 * oma + by * alpha3;

		return out;
	}

	@HiddenInAutocomplete
	static public Vec3 evaluateCubicFrame(double ax, double ay, double az, double c1x, double c1y, double c1z, double c2x, double c2y, double c2z, double bx, double by, double bz, double alpha, Vec3 out) {
		if (out == null) out = new Vec3();

		double oma = 1 - alpha;
		double oma2 = oma * oma;
		double oma3 = oma2 * oma;
		double alpha2 = alpha * alpha;
		double alpha3 = alpha2 * alpha;

		out.x = ax * oma3 + 3 * c1x * alpha * oma2 + 3 * c2x * alpha2 * oma + bx * alpha3;
		out.y = ay * oma3 + 3 * c1y * alpha * oma2 + 3 * c2y * alpha2 * oma + by * alpha3;
		out.z = az * oma3 + 3 * c1z * alpha * oma2 + 3 * c2z * alpha2 * oma + bz * alpha3;

		return out;
	}

	public Node node() {
		return nodes.get(nodes.size() - 1);
	}

	public FLine add(Node n) {
		nodes.add(n);
		mod++;
		return this;
	}

	public FLine add(FLine n) {

		for (Node nn : n.nodes) {
			this.copyTo(nn);
		}

		return this;
	}


	public void modify() {
		mod++;
	}

	public FLine moveTo(double x, double y) {
		return add(new MoveTo(x, y, 0));
	}

	public FLine moveTo(Vec2 x) {
		return add(new MoveTo(x.x, x.y, 0));
	}

	public FLine moveTo(Vec3 x) {
		return add(new MoveTo(x.x, x.y, x.z));
	}

	public FLine lineTo(double x, double y) {
		if (nodes.size() == 0) return moveTo(x, y);
		return add(new LineTo(x, y, 0));
	}

	public FLine lineTo(Vec2 x) {
		if (nodes.size() == 0) return moveTo(x);
		return add(new LineTo(x.x, x.y, 0));
	}

	public FLine lineTo(Vec3 x) {
		if (nodes.size() == 0) return moveTo(x);
		return add(new LineTo(x.x, x.y, x.z));
	}

	public FLine moveTo(double x, double y, double z) {
		return add(new MoveTo(x, y, z));
	}

	public FLine lineTo(double x, double y, double z) {
		if (nodes.size() == 0) return moveTo(x, y, z);
		return add(new LineTo(x, y, z));
	}

	public FLine cubicTo(double c1x, double c1y, double c2x, double c2y, double x, double y) {
		if (nodes.size() == 0) return moveTo(x, y);
		return add(new CubicTo(c1x, c1y, 0, c2x, c2y, 0, x, y, 0));
	}

	public FLine cubicTo(Vec2 c1, Vec2 c2, Vec2 x) {
		if (nodes.size() == 0) return moveTo(x);
		return add(new CubicTo(c1.x, c1.y, 0, c2.x, c2.y, 0, x.x, x.y, 0));
	}

	public FLine cubicTo(double c1x, double c1y, double c1z, double c2x, double c2y, double c2z, double x, double y, double z) {
		if (nodes.size() == 0) return moveTo(x, y, z);
		return add(new CubicTo(c1x, c1y, c1z, c2x, c2y, c2z, x, y, z));
	}

	public FLine cubicTo(Vec3 c1, Vec3 c2, Vec3 x) {
		if (nodes.size() == 0) return moveTo(x);
		return add(new CubicTo(c1.x, c1.y, c1.z, c2.x, c2.y, c2.z, x.x, x.y, x.z));
	}

	/**
	 * A Cubic segment that's offset from the cubic segment that would give a straight line between the current position and 'destination'
	 *
	 * @param r1          -- 'radius' multiplier of first control point. 1 = 1/3 of the distance from the current position to the destination
	 * @param theta1      -- 'angle' of the first control point. 0 = lies on the line between current position and the destination
	 * @param r2          -- 'radius' multiplier of second control point. 1 = 1/3 of the distance from the current position to the destination
	 * @param theta2      -- 'angle' of the second control point. 0 = lies on the line between current position and the destination
	 * @param destination
	 * @return
	 */
	public FLine polarCubicTo(float r1, float theta1, float r2, float theta2, Vec2 destination) {
		Vec2 a = last().to.toVec2();
		Vec2 d = Vec2.sub(destination, a, new Vec2());

		Vec2 c1 = new Vec2(d).scale(r1 / 3);
		Vec2 c2 = new Vec2(d).scale(-r2 / 3);

		c1 = new Quat().setFromAxisAngle(new Vec3(0, 0, 1), theta1)
			       .rotate(c1)
			       .toVec2();
		c2 = new Quat().setFromAxisAngle(new Vec3(0, 0, 1), theta2)
			       .rotate(c2)
			       .toVec2();

		Vec2.add(c1, a, c1);
		Vec2.add(c2, destination, c2);

		cubicTo(c1, c2, destination);
		return this;
	}

	/**
	 * A Cubic segment that's offset from the cubic segment that would give a straight line between the current position and 'destination'
	 *
	 * @param r1            -- 'radius' multiplier of first control point. 1 = 1/3 of the distance from the current position to the destination
	 * @param theta1        -- 'angle' of the first control point. 0 = lies on the line between current position and the destination
	 * @param r2            -- 'radius' multiplier of second control point. 1 = 1/3 of the distance from the current position to the destination
	 * @param theta2        -- 'angle' of the second control point. 0 = lies on the line between current position and the destination
	 * @param destinationx, destinationy
	 * @return
	 */
	public FLine polarCubicTo(float r1, float theta1, float r2, float theta2, float destinationx, float destinationy) {
		Vec2 destination = new Vec2(destinationx, destinationy);
		return polarCubicTo(r1, theta1, r2, theta2, destination);
	}

	public FLine rect(Rect r) {
		return rect((float) r.x, (float) r.y, (float) r.w, (float) r.h);
	}

	public FLine rect(double x, double y, double w, double h) {
		this.moveTo(x, y);
		this.lineTo(x + w, y);
		this.lineTo(x + w, y + h);
		this.lineTo(x, y + h);
		return this.lineTo(x, y);
	}

	public FLine roundedRect(double x, double y, double w, double h, double r) {
		FLinesAndJavaShapes.drawRoundedRectInto(this, x, y, w, h, r);
		return this;
	}

	public FLine circle(double x, double y, double r) {
		double k = 0.5522847498f * r;
		this.moveTo(x, y - r);
		this.cubicTo(x + k, y - r, x + r, y - k, x + r, y);
		this.cubicTo(x + r, y + k, x + k, y + r, x, y + r);
		this.cubicTo(x - k, y + r, x - r, y + k, x - r, y);
		return this.cubicTo(x - r, y - k, x - k, y - r, x, y - r);
	}

	/**
	 * This takes a list of "things" and successively transforms them into a list of (list of...) Vec3 or Vec2. Each of the transformation rules are tried in turn (and in order) and anything that
	 * returns non-null terminates the transformation for that "turn". Collections are understood. All exceptions are suppressed inside the .apply method of these transformations.
	 * <p>
	 * If, after all this, you have a List of Vec2 or Vec3 then you get a single line, otherwise a List of List of Vec2 or Vec3 gets you a group of lines
	 */
	public FLine dataLines(Collection<Object> input, Function<Object, Object>... transformation) {
		List<Object> m = transform(input, transformation);
		doDataLines(m);

		return this;
	}

	private void doDataLines(Collection<Object> m) {
		boolean first = true;
		for (Object o : m) {
			System.out.println(" TRANSFORM returned :" + o);
			if (o instanceof Vec2) {
				if (first) moveTo((Vec2) o);
				else lineTo((Vec2) o);
				first = false;
			} else if (o instanceof Vec3) {
				if (first) moveTo((Vec3) o);
				else lineTo((Vec3) o);
				first = false;
			} else if (o instanceof Collection) {
				doDataLines((Collection) o);
				first = true;
			} else {
				throw new IllegalArgumentException(" cannot interpret :" + o);
			}
		}
	}

	protected List<Object> transform(Collection<Object> input, Function<Object, Object>... transformation) {
		List<Object> a = new ArrayList<Object>(input);
		List<Object> o = new ArrayList<Object>();
		boolean workDone = false;
		do {
			workDone = false;
			for (int i = 0; i < a.size(); i++) {
				Object q = a.get(i);
				if (q instanceof Vec2 || q instanceof Vec3) {
					o.add(q);
				} else if (q instanceof Collection) {
					// note, we'll keep doing this over and over
					List<Object> qq = transform((Collection) q, transformation);
					workDone |= !qq.equals(q);
					o.add(qq);
				} else {
					boolean done = false;
					System.out.println(" looking at :" + transformation.length + " transformations ");
					for (Function<Object, Object> f : transformation) {
						System.out.println(" trying transformation :" + f);
						try {
							Object m = f.apply(q);
							if (m != null) {
								System.out.println(" transformation returns :" + m + " for " + q);
								o.add(m);
								done = true;
								workDone |= !m.equals(q);
								break;
							}
						} catch (Throwable e) {
							System.out.println(" exception thrown in transformation, continuing on");
							e.printStackTrace();
						}
					}
					if (!done) {
						throw new IllegalArgumentException(" can't transform " + q);
					}
				}
			}

			a = o;
			o = new ArrayList<>();

		} while (workDone);
		return a;
	}

	/**
	 * take 'many things' and turn them into a line based on the formatting string.
	 * <p>
	 * 'm' - moveTo, needs a Vec2 or a Vec3 'l' - lineTo, needs a Vec2 or a Vec3 'c' - cubicTo, needs 3 Vec2 or Vec3; 's' - smoothTo, needs 1 Vec2 or Vec3 '*' will loop the previous instruction;
	 * '+' will loop the whole set of instructions 'd' will drop a Vec3; 'C' — is a cubic segment that consumes the next two instructions as well (e.g. you need to write 12C to do the same as 'c')
	 * until you run out of Vec3 inputs; 't' — dispatches based on the tag of a TaggedVec3
	 */
	public FLine data(String format, Object... input) {
		List<Vec3> f = flattenInput(input);

		int q = 0;

		char[] cs = format.toCharArray();
		boolean looping = false;
		for (int i = 0; i < cs.length; i++) {
			char c = cs[i];

			if (c == '*') {
				c = cs[i - 1];
				i--;
				looping = true;
			}
			if (c == '.') {
				c = cs[0];
				i = 0;
				looping = true;
			}
			if (c == 't') c = f.get(q) instanceof TaggedVec3 ? ((TaggedVec3) f.get(q)).tag : 'l';

			try {
				switch (c) {

					case 'm':
						moveTo(f.get(q++));
						break;
					case 'l':
						lineTo(f.get(q++));
						break;
					case 'd':
						q++;
						break;
					case 's':
						smoothTo(f.get(q++));
						break;
					case 'c':
						cubicTo(f.get(q++), f.get(q++), f.get(q++));
						break;
					case 'C':
						cubicTo(f.get(q - 2), f.get(q - 1), f.get(q));
						q++;
						break;
					case '1':
						q++;
						break;
					case '2':
						q++;
						break;

					default:
						throw new IllegalArgumentException(" unknown format specification ");
				}
			} catch (IndexOutOfBoundsException e) {
				if (looping) return this;
				throw e;
			}
		}
		return this;
	}

	/**
	 * take 'many things' and turn them into a line based on the formatting string.
	 * <p>
	 * 'm' - moveTo, needs a Vec2 or a Vec3 'l' - lineTo, needs a Vec2 or a Vec3 'c' - cubicTo, needs 3 Vec2 or Vec3; 's' - smoothTo, needs 1 Vec2 or Vec3 '*' will loop the previous instruction
	 * until you run out of Vec3 inputs
	 */
	public FLine data(String format, Iterator<Vec3> f) {

		// FIXME, duplicative and out of date

		int q = 0;

		char[] cs = format.toCharArray();
		boolean looping = false;
		for (int i = 0; i < cs.length; i++) {
			char c = cs[i];

			if (c == '*') {
				c = cs[i - 1];
				c--;
				looping = true;
			}

			try {
				switch (c) {
					case 'm':
						moveTo(f.next());
						break;
					case 'l':
						lineTo(f.next());
						break;
					case 's':
						smoothTo(f.next());
						break;
					case 'd':
						q++;
						break;
					case 'c':
						cubicTo(f.next(), f.next(), f.next());
						break;
					default:
						throw new IllegalArgumentException(" unknown format specification ");
				}
			} catch (ArrayIndexOutOfBoundsException e) {
				if (looping) return this;
				throw e;
			}
		}
		return this;
	}

	/**
	 * copies a node from (potentially some other) line to here. Also copies attributes
	 */
	public FLine copyTo(Node node) {
		if (node instanceof MoveTo) {
			this.moveTo(node.to);
			this.last().attributes.putAll(node.attributes.duplicate());
			return this;
		} else if (node instanceof LineTo) {
			this.lineTo(node.to);
			this.last().attributes.putAll(node.attributes.duplicate());
			return this;
		} else if (node instanceof CubicTo) {
			this.cubicTo(((CubicTo) node).c1, ((CubicTo) node).c2, ((CubicTo) node).to);
			this.last().attributes.putAll(node.attributes.duplicate());
			return this;
		}
		throw new NotImplementedException();
	}

	/**
	 * copies a node from (potentially some other) line to here. Also copies attributes. Cubic segments are reversed.
	 */
	public FLine copyToFlipped(Node node, Vec3 dest) {
		if (node instanceof MoveTo) {
			this.moveTo(dest);
			this.last().attributes.putAll(node.attributes.duplicate());
			return this;
		} else if (node instanceof LineTo) {
			this.lineTo(dest);
			this.last().attributes.putAll(node.attributes.duplicate());
			return this;
		} else if (node instanceof CubicTo) {
			this.cubicTo(((CubicTo) node).c2, ((CubicTo) node).c1, dest);
			this.last().attributes.putAll(node.attributes.duplicate());
			return this;
		}
		throw new NotImplementedException();
	}

	/**
	 * "smoothly" builds a cubic segment to this point. This will rewrite the previous node to have the correct tangent.
	 *
	 * @param v
	 */
	public FLine smoothTo(Vec3 v) {
		if (this.nodes.size() == 0) return moveTo(v);
		if (this.nodes.size() == 1) return lineTo(v);
		if (last() instanceof MoveTo) return lineTo(v);

		Node b = last();
		Node a = nodes.get(nodes.size() - 2);

		if (b instanceof CubicTo) {
			// we have three points

			Vec3 p1 = a.to;
			Vec3 p2 = b.to;
			Vec3 p3 = v;

			Vec3 tan = new Vec3(p3).sub(p1);
			Vec3 tan2 = new Vec3(p2).sub(p1);

			tan.normalise();

			double d1 = p2.distanceFrom(p1);
			double d2 = p2.distanceFrom(p3);

			Vec3 c12 = new Vec3(p2).add(tan, -d1 / 3);
			Vec3 c21 = new Vec3(p2).add(tan, d2 / 3);
			Vec3 c22 = new Vec3(p3).add(tan2, -1 / 3);

			((CubicTo) b).c2.set(c12);

			cubicTo(c21, c22, p3);
		} else // b instanceof LineTo
		{
			// we have an incoming tangent

			Vec3 p1 = a.to;
			Vec3 p2 = b.to;
			Vec3 p3 = v;

			Vec3 tan2 = new Vec3(p3).sub(p2);
			Vec3 tan1 = new Vec3(p2).sub(p1);

			tan2.normalise();
			tan1.normalise();

			double d1 = p2.distanceFrom(p1);
			double d2 = p2.distanceFrom(p3);

			Vec3 c21 = new Vec3(p2).add(tan1, d1 / 3);
			Vec3 c22 = new Vec3(p3).add(tan2, -d2 / 3);

			cubicTo(c21, c22, p3);
		}

		return this;

	}

	private List<Vec3> flattenInput(Object i) {
		List<Vec3> o = new ArrayList<>();


		if (i instanceof Vec2) o.add(((Vec2) i).toVec3());
		else if (i instanceof Vec3) o.add((Vec3) i);
		else if (i instanceof Vec4) o.add(((Vec4) i).toVec3());
		else if (i instanceof Supplier) {
			Object ni = ((Supplier) i).get();
			if (i != ni) o.addAll(flattenInput(ni));
		} else if (i instanceof Object[]) {
			for (Object ii : (Object[]) i)
				o.addAll(flattenInput(ii));
		} else if (i instanceof CubicTo) {
			o.add(((CubicTo) i).c1);
			o.add(((CubicTo) i).c2);
			o.add(((CubicTo) i).to);
		} else if (i instanceof LineTo) {
			o.add(((LineTo) i).to);
		} else if (i instanceof MoveTo) {
			o.add(((MoveTo) i).to);
		} else if (i instanceof Quad) {
			o.addAll(flattenInput(((Quad) i).first));
			o.addAll(flattenInput(((Quad) i).second));
			o.addAll(flattenInput(((Quad) i).third));
			o.addAll(flattenInput(((Quad) i).fourth));
		} else if (i instanceof Triple) {
			o.addAll(flattenInput(((Triple) i).first));
			o.addAll(flattenInput(((Triple) i).second));
			o.addAll(flattenInput(((Triple) i).third));
		} else if (i instanceof Pair) {
			o.addAll(flattenInput(((Pair) i).first));
			o.addAll(flattenInput(((Pair) i).second));
		} else if (i instanceof MappedFloatArray) {
			Iterator<Vec3> ii = ((MappedFloatArray) i).getVec3s();
			ii.forEachRemaining(x -> o.add(x));
		} else if (i instanceof Collection) {

			Collection co = (Collection) i;
			if (co.size() == 2 || co.size() == 3) {
				//peek at it, it might be a vec3 or vec2 spelled as a collection
				Iterator m = co.iterator();
				List<Double> out = new ArrayList<>();
				boolean fail = false;
				while (m.hasNext()) {
					Object mn = m.next();
					if (!(mn instanceof Number)) {
						fail = true;
						break;
					} else out.add(((Number) mn).doubleValue());
				}
				if (!fail) {
					if (out.size() == 2) {
						return flattenInput(new Vec3(out.get(0), out.get(1), 0));
					} else {
						return flattenInput(new Vec3(out.get(0), out.get(1), out.get(2)));
					}
				}
			}

			for (Object ii : (Collection) i)
				o.addAll(flattenInput(ii));

		} else if (i instanceof FLinesAndJavaShapes.CubicSegment3) {
			o.add(((FLinesAndJavaShapes.CubicSegment3) i).a);
			o.add(((FLinesAndJavaShapes.CubicSegment3) i).b);
			o.add(((FLinesAndJavaShapes.CubicSegment3) i).c);
			o.add(((FLinesAndJavaShapes.CubicSegment3) i).d);
		} else if (i instanceof FLinesAndJavaShapes.CubicSegment2) {
			o.add(((FLinesAndJavaShapes.CubicSegment2) i).a.toVec3());
			o.add(((FLinesAndJavaShapes.CubicSegment2) i).b.toVec3());
			o.add(((FLinesAndJavaShapes.CubicSegment2) i).c.toVec3());
			o.add(((FLinesAndJavaShapes.CubicSegment2) i).d.toVec3());
		} else throw new IllegalArgumentException(" cannot understand object " + i);

		return o;

	}

	public FLine transform(Function<Vec3, Vec3> by) {
		for (Node n : nodes) {
			n.transform(by);
		}
		modify();
		return this;
	}

	public FLine clear() {
		nodes.clear();
		modify();
		return this;
	}

	public Node last() {
		return nodes.size() == 0 ? null : nodes.get(nodes.size() - 1);
	}

	public void clearCache() {
		cache.clear();
		cache_thickening.clear();
	}

	public void clearCache(MeshBuilder m) {
		cache.remove(m);
		cache_thickening.remove(m);
	}

	public FLine duplicate() {
		FLine fLine = new FLine();
		for (Node n : this.nodes) {
			fLine.nodes.add(n.duplicate());
		}
		fLine.attributes.putAll(attributes);
		fLine.modify();
		return fLine;
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

		Log.log("drawing.trace", "should skip ? " + mod);

		return m.skipTo(c.start, c.end, mod, () -> {

			flattenAuxProperties();
			m.open();
			try {
				MeshBuilder.Bookmark start = null;
				// todo: AUX!

				Log.log("drawing.trace", "ACTUALLY DRAWING");

				Node a = null;
				for (int i = 0; i < nodes.size(); i++) {
					Node b = nodes.get(i);

					if (b instanceof MoveTo) {
						if (start != null) m.nextLine(start.at() + 1);
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
					m.nextLine(start.at() + 1);
				}
			} finally {
				m.close();
			}
		});
	}

	public boolean renderToLine(MeshBuilder m, int fixedSizeForCubic) {
		Log.log("drawing.trace", "renderToLine");
		return renderToLine(m, this::renderMoveTo, this::renderLineTo, renderCubicTo(fixedSizeForCubic));
	}

	public boolean renderLineToMeshByStroking(MeshBuilder m, int fixedSizeForCubic, BasicStroke stroke) {
		BookmarkCache c = cache_thickening.computeIfAbsent(m, (k) -> new BookmarkCache(m));

		return m.skipTo(c.start, c.end, mod, () -> {
			Shape s = /*stroke.createStrokedShape*/(flineToJavaShape(this));
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
						}

					}
				}

				MeshBuilder.Bookmark end = m.bookmark();

				if (start != null && start.at() != end.at()) {
					ts.endContour();
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

	public FLine addAuxProperties(Map<Integer, String> propertiesToAuxChannels) {
		if (auxProperties == null) auxProperties = new LinkedHashMap<>();
		auxProperties.putAll(propertiesToAuxChannels);
		return this;
	}

	public FLine addAuxProperties(int i, String prop) {
		if (auxProperties == null) auxProperties = new LinkedHashMap<>();
		auxProperties.put(i, prop);
		return this;
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

	@HiddenInAutocomplete
	public boolean renderToMesh(MeshBuilder m, int fixedSizeForCubic) {
		return renderToMesh(m, this::renderMoveTo, this::renderLineTo, renderCubicTo(fixedSizeForCubic));
	}

	@HiddenInAutocomplete
	public Node renderMoveTo(MeshAcceptor m, Node from, MoveTo to) {
		if (to.flatAuxData != null) for (int i = 0; i < to.flatAuxData.length; i++) {
			int channel = to.flatAux[i];
			float[] value = to.flatAuxData[i];
			if (value != null && channel > 0) m.aux(channel, value);
		}
		Log.log("drawing.trace", "moveTo " + to.to);
		m.nextVertex(to.to.x, to.to.y, to.to.z);
		return to;
	}

	@HiddenInAutocomplete
	public Node renderLineTo(MeshAcceptor m, Node from, LineTo to) {
		if (to.flatAuxData != null) for (int i = 0; i < to.flatAuxData.length; i++) {
			int channel = to.flatAux[i];
			float[] value = to.flatAuxData[i];
			if (value != null && channel > 0) m.aux(channel, value);
		}

		Log.log("drawing.trace", "lineTo " + to.to);

		m.nextVertex(to.to.x, to.to.y, to.to.z);
		return to;
	}

	/*
	 * Everybody is taught in the textbooks that the way to draw a cubic spline segment is to recursively subdivide it until the sub-segments are flat enough that you can just draw them with straight lines. This is a great, efficient and beautiful idea. However, it suffers from a serious problem in the case where the geometry you are drawing is animated: the number of line segments that you emit is constantly changing. This completely destroys our caching strategy here. For after the first animated cubic spline segment that enters the meshbuilder all other segments will need to be completely remade and reuploaded to the GPU. It's better in most cases to burn through extra vertices to have a shot at a fixed geometry layout in most cases. If you want a recursive flattening renderCubicTo, add one here by all means, that's why you can pass in a different Function3 here.
	 */
	@HiddenInAutocomplete
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
					if (a == null && b == null) continue;
					float[] r = interpolate(alpha, a, b, a == null ? b.length : a.length);
					if (r != null && channel > 0) meshBuilder.aux(channel, r);
				}

				meshBuilder.nextVertex(o.x, o.y, o.z);
			}
			return to;
		};

	}

	public FLine byTransforming(Function<Vec3, Vec3> spaceTransform) {

		FLine f = new FLine();
		f.attributes = attributes.duplicate();
		for (Node n : nodes) {
			if (n instanceof MoveTo) f.add(new MoveTo(spaceTransform.apply(n.to)));
			else if (n instanceof LineTo) f.add(new LineTo(spaceTransform.apply(n.to)));
			else if (n instanceof CubicTo) f.add(new CubicTo(spaceTransform.apply(((CubicTo) n).c1), spaceTransform.apply(((CubicTo) n).c2), spaceTransform.apply(n.to)));
			f.nodes.get(f.nodes.size() - 1).attributes.putAll(n.attributes.duplicate());
		}
		return f;
	}

	/**
	 * returns a new FLine by translating, rotating and scaling this line such that it's endpoints are 'start' and 'end'
	 */
	public FLine byFixingEndpointsTo(Vec3 start, Vec3 end) {

		Vec3 cStart = nodes.get(0).to;
		Vec3 cEnd = nodes.get(nodes.size() - 1).to;

		Vec3 center = new Vec3().lerp(cStart, cEnd, 0.5);
		Vec3 center2 = new Vec3().lerp(start, end, 0.5);

		Vec3 t = new Vec3(center2).sub(center);

		Vec3 cD = new Vec3(cEnd).sub(cStart)
					.normalise();
		Vec3 d = new Vec3(end).sub(start);

		Quat q = cD.isNaN() || d.isNaN() ? new Quat() : new Quat().setFromTwoVec3(d, cD);

		double s = end.distanceFrom(start) / cEnd.distanceFrom(cStart);

		if (Double.isNaN(s)) s = 1;

		double fs = s;

		return byTransforming(x -> q.rotate(new Vec3(x).sub(center))
					    .scale(fs)
					    .add(center)
					    .add(t));

	}


	public long getModCount() {
		return mod;
	}

	@Override
	public FLine get() {
		return this;
	}

	public List<Node> getNodes() {
		return nodes;
	}

	public FLinesAndJavaShapes.Cursor cursor() {
		return new FLinesAndJavaShapes.Cursor(this, 0.1f);
	}

	@Override
	@HiddenInAutocomplete
	public boolean asMap_isProperty(String p) {
		if (Dict.Canonical.findCannon(p) != null) return true;

		if (knownNonProperties == null) knownNonProperties = computeKnownNonProperties();

		if (knownNonProperties.contains(p)) return false;

		return true;
	}

	@HiddenInAutocomplete
	protected Set<String> computeKnownNonProperties() {
		Set<String> r = new LinkedHashSet<>();
		Method[] m = this.getClass()
				 .getMethods();
		for (Method mm : m)
			r.add(mm.getName());
		Field[] f = this.getClass()
				.getFields();
		for (Field ff : f)
			r.add(ff.getName());
		return r;
	}

	@Override
	@HiddenInAutocomplete
	public Object asMap_get(String m) {

		if (m.equals("n")) return last();

		Dict.Prop cannon = new Dict.Prop(m).findCannon();

		Object ret = attributes.getOrConstruct(cannon);


		if (ret instanceof Box.FunctionOfBox) {
			return ((Supplier) (() -> ((Box.FunctionOfBox) ret).apply(this)));
		}

		return ret;
	}

	@Override
	@HiddenInAutocomplete
	public Object asMap_set(String name, Object value) {

		// workaround bug in Nashorn
		if (value instanceof ConsString) value = ((ConsString) value).toString();


//		Log.log("underscore.debug", " underscore box set :" + name + " to " + value.getClass() + " <" + Function.class.getName() + ">");
		Dict.Prop cannon = new Dict.Prop(name).toCannon();

//		Log.log("underscore.debug", " cannonical type information " + cannon.getTypeInformation());

		Object converted = convert(value, cannon.getTypeInformation());

		attributes.put(cannon, converted);

//		Log.log("underscore.debug", () -> {
//			Log.log("underscore.debug", " PROPERTIES NOW :");
//			for (Map.Entry<Dict.Prop, Object> q : attributes.getMap()
//									.entrySet()) {
//				try {
//					Log.log("underscore.debug", "     " + q.getKey() + " = " + q.getValue());
//				} catch (NullPointerException e) {
//					//JDK bug JDK-8035426 --- sometimes Nashorn lambdas throw NPE's when they are .toString'd
//				}
//			}
//			return null;
//		});

		modify();

		return this;
	}

	@HiddenInAutocomplete
	public Object convert(Object value, List<Class> fit) {
		if (fit == null) return value;
		if (fit.get(0)
		       .isInstance(value)) return value;

		// promote non-arrays to arrays
		if (List.class.isAssignableFrom(fit.get(0))) {
			if (!(value instanceof List)) {
				return Collections.singletonList(convert(value, fit.subList(1, fit.size())));
			} else {
				return value;
			}
		} else if (Map.class.isAssignableFrom(fit.get(0)) && String.class.isAssignableFrom(fit.get(1))) {
			// promote non-Map<String, V> to Map<String, V>
			if (!(value instanceof Map)) {
				return Collections.singletonMap("" + value + ":" + System.identityHashCode(value), convert(value, fit.subList(2, fit.size())));
			} else {
				return value;
			}

		} else if (Collection.class.isAssignableFrom(fit.get(0))) {
			if (!(value instanceof Collection)) {
				return Collections.singletonList(convert(value, fit.subList(1, fit.size())));
			} else {
				return value;
			}

		}

		if (value instanceof ScriptFunctionImpl) {
			StaticClass adapterClassFor = JavaAdapterFactory.getAdapterClassFor(new Class[]{fit.get(0)}, (ScriptObject) value, MethodHandles.lookup());
			try {
				return adapterClassFor.getRepresentedClass()
						      .newInstance();
			} catch (InstantiationException e) {
				Log.log("underscore.error", " problem instantiating adaptor class to take us from " + value + " ->" + fit.get(0), e);
			} catch (IllegalAccessException e) {
				Log.log("underscore.error", " problem instantiating adaptor class to take us from " + value + " ->" + fit.get(0), e);
			}
		}

		return value;
	}

	@Override
	@HiddenInAutocomplete
	public Object asMap_call(Object a, Object b) {
		System.err.println(" call called :" + a + " " + b + " " + (b instanceof Map ? ((Map) b).keySet() : b.getClass()
														    .getSuperclass() + " " + Arrays.asList(b.getClass()
																			    .getInterfaces())));
		boolean success = false;
		try {
			Map<?, ?> m = (Map<?, ?>) ScriptUtils.convert(b, Map.class);
			for (Map.Entry<?, ?> e : m.entrySet()) {
				asMap_set("" + e.getKey(), e.getValue());
			}
			success = true;
		} catch (UnsupportedOperationException e) {

		}
		if (!success) {
			throw new IllegalArgumentException(" can't understand parameter :" + b);
		}
		return this;
	}

	@Override
	@HiddenInAutocomplete
	public Object asMap_new(Object b) {
		boolean success = false;
		try {
			Map<?, ?> m = (Map<?, ?>) ScriptUtils.convert(b, Map.class);

			FLine o = this.duplicate();

			for (Map.Entry<?, ?> e : m.entrySet()) {
				o.asMap_set("" + e.getKey(), e.getValue());
			}
			success = true;
			return o;
		} catch (UnsupportedOperationException e) {
			throw new IllegalArgumentException(" can't understand parameter :" + b);
		}
	}

	@Override
	@HiddenInAutocomplete
	public Object asMap_new(Object b, Object c) {
		throw new NoSuchMethodError(" two argument constructor to fline not implemented");
	}

	@Override
	public Object asMap_getElement(int element) {
		return nodes.get(element);
	}

	@Override
	public Object asMap_setElement(int element, Object v) {
		throw new NotImplementedException();
	}


	public class Node implements Linker.AsMap {
		public final Vec3 to;
		public final Dict attributes = new Dict();
		transient protected Set<String> knownNonProperties;
		transient float[][] flatAuxData;
		transient int[] flatAux;

		protected Node(Vec3 n) {
			this.to = new Vec3(n);
		}

		protected Node(double x, double y, double z) {
			this.to = new Vec3(x, y, z);
		}

		public void transform(Function<Vec3, Vec3> by) {
			this.to.set(by.apply(to));
			modify();
		}

		public Node duplicate() {
			throw new NotImplementedException();
		}

		@Override
		@HiddenInAutocomplete
		public Object asMap_get(String m) {

			Dict.Prop cannon = new Dict.Prop(m).findCannon();

			Object ret = attributes.getOrConstruct(cannon);


			if (ret instanceof Box.FunctionOfBox) {
				return ((Supplier) (() -> ((Box.FunctionOfBox) ret).apply(this)));
			}

			return ret;
		}

		@Override
		@HiddenInAutocomplete
		public Object asMap_set(String name, Object value) {

			// workaround bug in Nashorn
			if (value instanceof ConsString) value = ((ConsString) value).toString();


//			Log.log("underscore.debug", " underscore box set :" + name + " to " + value.getClass() + " <" + Function.class.getName() + ">");
			Dict.Prop cannon = new Dict.Prop(name).toCannon();

//			Log.log("underscore.debug", " cannonical type information " + cannon.getTypeInformation());

			Object converted = convert(value, cannon.getTypeInformation());

			attributes.put(cannon, converted);

//			Log.log("underscore.debug", () -> {
//				Log.log("underscore.debug", " PROPERTIES NOW :");
//				for (Map.Entry<Dict.Prop, Object> q : attributes.getMap()
//										.entrySet()) {
//					try {
//						Log.log("underscore.debug", "     " + q.getKey() + " = " + q.getValue());
//					} catch (NullPointerException e) {
//						//JDK bug JDK-8035426 --- sometimes Nashorn lambdas throw NPE's when they are .toString'd
//					}
//				}
//				return null;
//			});

			modify();

//			Log.log("node", attributes+" on "+System.identityHashCode(this)+" / "+System.identityHashCode(FLine.this));

			return this;
		}

		@Override
		public Object asMap_new(Object b, Object c) {
			throw new NoSuchMethodError(" two argument constructor to node not implemented");
		}

		@HiddenInAutocomplete
		public Object convert(Object value, List<Class> fit) {
			if (fit == null) return value;
			if (fit.get(0)
			       .isInstance(value)) return value;

			// promote non-arrays to arrays
			if (List.class.isAssignableFrom(fit.get(0))) {
				if (!(value instanceof List)) {
					return Collections.singletonList(convert(value, fit.subList(1, fit.size())));
				} else {
					return value;
				}
			} else if (Map.class.isAssignableFrom(fit.get(0)) && String.class.isAssignableFrom(fit.get(1))) {
				// promote non-Map<String, V> to Map<String, V>
				if (!(value instanceof Map)) {
					return Collections.singletonMap("" + value + ":" + System.identityHashCode(value), convert(value, fit.subList(2, fit.size())));
				} else {
					return value;
				}

			} else if (Collection.class.isAssignableFrom(fit.get(0))) {
				if (!(value instanceof Collection)) {
					return Collections.singletonList(convert(value, fit.subList(1, fit.size())));
				} else {
					return value;
				}

			}

			if (value instanceof ScriptFunctionImpl) {
				StaticClass adapterClassFor = JavaAdapterFactory.getAdapterClassFor(new Class[]{fit.get(0)}, (ScriptObject) value, MethodHandles.lookup());
				try {
					return adapterClassFor.getRepresentedClass()
							      .newInstance();
				} catch (InstantiationException e) {
					Log.log("underscore.error", " problem instantiating adaptor class to take us from " + value + " ->" + fit.get(0), e);
				} catch (IllegalAccessException e) {
					Log.log("underscore.error", " problem instantiating adaptor class to take us from " + value + " ->" + fit.get(0), e);
				}
			}

			return value;
		}

		@Override
		public Object asMap_call(Object a, Object b) {
			System.err.println(" call called :" + a + " " + b + " " + (b instanceof Map ? ((Map) b).keySet() : b.getClass()
															    .getSuperclass() + " " + Arrays.asList(b.getClass()
																				    .getInterfaces())));
			boolean success = false;
			try {
				Map<?, ?> m = (Map<?, ?>) ScriptUtils.convert(b, Map.class);
				for (Map.Entry<?, ?> e : m.entrySet()) {
					asMap_set("" + e.getKey(), e.getValue());
				}
				success = true;
			} catch (UnsupportedOperationException e) {

			}
			if (!success) {
				throw new IllegalArgumentException(" can't understand parameter :" + b);
			}
			return this;
		}

		@Override
		public Object asMap_new(Object b) {
			boolean success = false;
			try {
				Map<?, ?> m = (Map<?, ?>) ScriptUtils.convert(b, Map.class);

				Node o = this.duplicate();

				for (Map.Entry<?, ?> e : m.entrySet()) {
					o.asMap_set("" + e.getKey(), e.getValue());
				}
				success = true;
				return o;
			} catch (UnsupportedOperationException e) {
				throw new IllegalArgumentException(" can't understand parameter :" + b);
			}
		}

		@Override
		public Object asMap_getElement(int element) {
			throw new NotImplementedException();
		}

		@Override
		public Object asMap_setElement(int element, Object v) {
			throw new NotImplementedException();
		}

		@Override
		@HiddenInAutocomplete
		public boolean asMap_isProperty(String p) {
			if (Dict.Canonical.findCannon(p) != null) return true;

			if (knownNonProperties == null) knownNonProperties = computeKnownNonProperties();

			if (knownNonProperties.contains(p)) return false;

			return true;
		}

		protected Set<String> computeKnownNonProperties() {
			Set<String> r = new LinkedHashSet<>();
			Method[] m = this.getClass()
					 .getMethods();
			for (Method mm : m)
				r.add(mm.getName());
			Field[] f = this.getClass()
					.getFields();
			for (Field ff : f)
				r.add(ff.getName());
			return r;
		}

		public Node toMoveTo() {
			MoveTo m = new MoveTo(new Vec3(this.to));
			m.attributes.putAll(attributes.duplicate());
			return m;
		}

		public void modify() {
			FLine.this.modify();
		}

	}

	public class BookmarkCache {
		MeshBuilder.Bookmark start;
		MeshBuilder.Bookmark end;

		public BookmarkCache(MeshBuilder on) {
			start = on.bookmark();
			end = on.bookmark();
		}
	}

	public class MoveTo extends Node {

		public MoveTo(Vec3 to) {
			super(to);
		}

		public MoveTo(double x, double y, double z) {
			super(new Vec3(x, y, z));
		}

		public MoveTo(Vec2 to) {
			super(new Vec3(to.x, to.y, 0));
		}

		@Override
		public Node duplicate() {
			MoveTo l = new MoveTo(to);
			l.attributes.putAll(attributes);
			return l;
		}

	}

	public class LineTo extends Node {
		public LineTo(Vec3 to) {
			super(to);
		}

		public LineTo(Vec2 to) {
			super(to.x, to.y, 0);
		}

		public LineTo(double x, double y, double z) {
			super(x, y, z);
		}

		@Override
		public Node duplicate() {
			LineTo l = new LineTo(to);
			l.attributes.putAll(attributes);
			return l;
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

		public CubicTo(double c1x, double c1y, double c1z, double c2x, double c2y, double c2z, double x, double y, double z) {
			super(x, y, z);
			this.c1 = new Vec3(c1x, c1y, c1z);
			this.c2 = new Vec3(c2x, c2y, c2z);
		}

		@Override
		public void transform(Function<Vec3, Vec3> by) {
			super.transform(by);
			this.c1.set(by.apply(c1));
			this.c2.set(by.apply(c2));
		}

		@Override
		public Node duplicate() {
			CubicTo l = new CubicTo(c1, c2, to);
			l.attributes.putAll(attributes);
			return l;
		}

	}

}
