package field.graphics;

import field.app.ThreadSync2;
import field.linalg.*;
import field.utility.*;
import fieldbox.boxes.Box;
import fieldbox.execution.Completion;
import fieldbox.execution.HandlesCompletion;
import fieldbox.execution.JavaSupport;
import fieldnashorn.annotations.HiddenInAutocomplete;
import jdk.nashorn.api.scripting.ScriptUtils;
import kotlin.jvm.functions.Function1;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static field.graphics.FLinesAndJavaShapes.flineToJavaShape;
import static field.graphics.FLinesAndJavaShapes.javaShapeToFLine;
import static field.graphics.StandardFLineDrawing.hint_noDepth;
import static field.utility.Util.safeEq;
import static fieldbox.boxes.Box.compress;
import static fieldbox.boxes.Box.depth;
import static fieldbox.boxes.Box.format;

/**
 * FLine is our main high level geometry container for lines, meshes and lists of points.
 * <p>
 * FLines consist of a list of drawing instructions, or nodes: `line.moveTo(...).lineTo(...).cubicTo(...)` .etc
 * <p>
 * This is essentially the common postscript / pdf / java2d drawing model with a few key differences and refinements.
 * <p>
 * Firstly, the drawing instructions are in 3d. 2d (the z=0 plane) is a special case; secondly, attributes can be associated with both the line itself and with individual nodes; thirdly, per-node attributes that are
 * present in some places on the line and absent in others are interpolated; finally, the structure is freely mutable, although changes to attributes are not automatically tracked (call `line.modify()` to
 * cause an explicit un-caching of this line).
 * <p>
 * The caching of the flattening of this line into MeshBuilder data (ready for OpenGL) cascades into MeshBuilder's cache structure. Thus, we have three levels of caching in total: FLine caches whether or not the geometry has changed at all, MeshBuilder caches whether or not there's any point sending anything to the OpenGL
 * underlying Buffers or whether this piece of geometry can be skipped, and finally individual ArrayBuffers can elect to skip the upload to OpenGL. This means that static geometry is extremely cheap
 * to draw.
 */
public class FLine implements Supplier<FLine>, fieldlinker.AsMap, HandlesCompletion, Serializable_safe, OverloadedMath {

	public List<Node> nodes = new ArrayList<>();
	public Dict attributes = new Dict();
	transient protected Set<String> knownNonProperties;
	long mod = 0;
	WeakHashMap<MeshBuilder, BookmarkCache> cache = new WeakHashMap<>();
	WeakHashMap<MeshBuilder, BookmarkCache> cache_thickening = new WeakHashMap<>();
	private Map<Integer, String> auxProperties;

	public FLine() {
	}

	public FLine(Map<Object, Object> attributes) {
		for (Map.Entry e : attributes.entrySet()) {
			String name = (String) Conversions.convert(e.getKey(), String.class);
			asMap_set(name, e.getValue());
		}
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

	public FLine moveToRel(float dx, float dy) {
		if (nodes.size() == 0)
			return moveTo(dx, dy);

		Vec3 t = nodes.get(nodes.size() - 1).to;
		return moveTo(t.x + dx, t.y + dy, t.z);
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

	public FLine lineToRel(float dx, float dy) {
		if (nodes.size() == 0) return moveTo(dx, dy);
		Vec3 v = nodes.get(nodes.size() - 1).to;
		return lineTo(dx + v.x, dy + v.y, v.z);
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

	public FLine quadTo(double q1x, double q1y, double x, double y) {
		if (nodes.size() == 0) return moveTo(x, y);

		Vec3 at = last().to;

		double c1x = (at.x + q1x * 2) / 3;
		double c1y = (at.y + q1y * 2) / 3;
		double c2x = (at.x * 2 + q1x) / 3;
		double c2y = (at.y * 2 + q1y) / 3;

		return cubicTo(c1x, c1y, c2x, c2y, x, y);
	}

	public FLine quadTo(double q1x, double q1y, double q1z, double x, double y, double z) {
		if (nodes.size() == 0) return moveTo(x, y);

		Vec3 at = last().to;

		double c1x = (at.x + q1x * 2) / 3;
		double c1y = (at.y + q1y * 2) / 3;
		double c1z = (at.z + q1z * 2) / 3;
		double c2x = (at.x * 2 + q1x) / 3;
		double c2y = (at.y * 2 + q1y) / 3;
		double c2z = (at.z * 2 + q1z) / 3;

		return cubicTo(c1x, c1y, c1z, c2x, c2y, c2z, x, y, z);
	}

	public FLine cubicToRel(double c1x, double c1y, double c2x, double c2y, double x, double y) {
		if (nodes.size() == 0) return moveTo(x, y);
		Vec3 v = nodes.get(nodes.size() - 1).to;
		return cubicTo(c1x + v.x, c1y + v.y, v.z, c2x + v.x, c2y + v.y, v.z, x + v.x, y + v.y, v.z);
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

		Vec2 c1 = new Vec2(d).mul(r1 / 3);
		Vec2 c2 = new Vec2(d).mul(-r2 / 3);

		c1 = new Quat().setAngleAxis(theta1, new Vec3(0, 0, 1))
			.transform(c1.toVec3())
			.toVec2();
		c2 = new Quat().setAngleAxis(theta2, new Vec3(0, 0, 1))
			.transform(c2.toVec3())
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
	 * @param theta1        -- 'angle' of the first control point. 0 = lies on the line between current position and the destination. Positive values are anti-clockwise around the current
	 *                      position.
	 * @param r2            -- 'radius' multiplier of second control point. 1 = 1/3 of the distance from the current position to the destination
	 * @param theta2        -- 'angle' of the second control point. 0 = lies on the line between current position and the destination. Positive values are anti-clockwise around the 'destination'.
	 * @param destinationx, destinationy
	 * @return
	 */
	public FLine polarCubicTo(float r1, float theta1, float r2, float theta2, float destinationx, float destinationy) {
		Vec2 destination = new Vec2(destinationx, destinationy);
		return polarCubicTo(r1, theta1, r2, theta2, destination);
	}

	public FLine rect(Rect r) {
		return rect(r.x, r.y, r.w, r.h);
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

	public FLine circle(double x, double y, double r, double z) {
		double k = 0.5522847498f * r;
		this.moveTo(x, y - r, z);
		this.cubicTo(x + k, y - r, z, x + r, y - k, z, x + r, y, z);
		this.cubicTo(x + r, y + k, z, x + k, y + r, z, x, y + r, z);
		this.cubicTo(x - k, y + r, z, x - r, y + k, z, x - r, y, z);
		return this.cubicTo(x - r, y - k, z, x - k, y - r, z, x, y - r, z);
	}

	/**
	 * calls function 'fun' with a parameter that goes from 0 -> 1 (inclusive) in 'samples' steps. Then hands the results to `data('t*', ...)`
	 */
	public FLine sampleOver(Function<Double, Object> fun, int samples) {
		List<Object> rr = new ArrayList<Object>();
		for (int i = 0; i < samples; i++) {
			double alpha = i / (samples - 1f);
			Object o = fun.apply(alpha);
			rr.add(o);
		}
		return data("t*", rr);
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
					for (Function<Object, Object> f : transformation) {
						try {
							Object m = f.apply(q);
							if (m != null) {
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
	 * until you run out of Vec3 inputs; 't' — dispatches based on the tag of a TaggedVec3; 'b' pushes the previous 'm'oveTo onto the stack to be consumed again by futher instructions (e.g 'mlllbl' draws a closed quadrilateral)
	 */
	public FLine data(String format, Object... input) {
		List<Vec3> f = flattenInput(input);

		int q = 0;

		Vec3 lastMove = null;
		char[] cs = format.toCharArray();
		boolean looping = false;
		for (int i = 0; i < cs.length; i++) {
			char c = cs[i];

			if (c == '*') {
				c = cs[i - 1];
				i--;
				looping = true;
			}
			if (c == '+') {
				c = cs[0];
				i = 0;
				looping = true;
			}
			if (c == 't')
				c = f.get(q) instanceof TaggedVec3 ? ((TaggedVec3) f.get(q)).tag : (i == 0 ? 'm' : 'l');

			try {
				switch (c) {

					case 'b':
						if (lastMove == null)
							throw new IllegalArgumentException(" 'b' without previous 'm'");
						f.add(q, lastMove);
						break;
					case 'm':
						lastMove = f.get(q++);
						moveTo(lastMove);
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
						throw new IllegalArgumentException(" unknown format specification " + c);
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
		throw new Error();
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
		throw new Error();
	}

	/**
	 * "smoothly" builds a cubic segment to this point. This will rewrite the previous node to have the correct tangent.
	 *
	 * @param v
	 */
	public FLine smoothTo(Vec2 v) {
		return smoothTo(v.toVec3());
	}

	public FLine smoothTo(double x, double y) {
		return smoothTo(new Vec3(x, y, 0));
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

			tan.normalize();

			double d1 = p2.distance(p1);
			double d2 = p2.distance(p3);

			Vec3 c12 = new Vec3(p2).fma(tan, -d1 / 3);
			Vec3 c21 = new Vec3(p2).fma(tan, d2 / 3);
			Vec3 c22 = new Vec3(p3).fma(tan2, -1 / 3);

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

			tan2.normalize();
			tan1.normalize();

			double d1 = p2.distance(p1);
			double d2 = p2.distance(p3);

			Vec3 c21 = new Vec3(p2).fma(tan1, d1 / 3);
			Vec3 c22 = new Vec3(p3).fma(tan2, -d2 / 3);

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

		// we are going to give this function (and Nashorn) a little help here

		Function<Vec3, Vec3> q = x -> {

			Vec3 xx = new Vec3(x);
			Object r = by.apply(xx);
			if (!(r instanceof Vec3)) // assume mutated it in place
				return xx;
			return (Vec3) r;
		};

		for (Node n : nodes) {
			n.transform(q);
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
		fLine.attributes.putAll(attributes.duplicate());
		fLine.modify();
		if (auxProperties != null) fLine.setAuxProperties(new LinkedHashMap<>(auxProperties));
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

		Log.log("drawing.trace", () -> "should skip ? " + mod);

		return m.skipTo(c.start, c.end, mod, () -> {

			flattenAuxProperties();
			m.open();
			try {
				MeshBuilder.Bookmark start = null;
				// todo: AUX!

				Log.log("drawing.trace", () -> "ACTUALLY DRAWING");

				Node a = null;
				for (int i = 0; i < nodes.size(); i++) {
					Node b = nodes.get(i);

					if (b instanceof MoveTo) {
						if (start != null) m.line(start.at() + 1);
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
					m.line(start.at() + 1);
				}
			} finally {
				m.close();
			}
		});
	}

	public boolean renderToLine(MeshBuilder m, int fixedSizeForCubic) {
		Log.log("drawing.trace", () -> "renderToLine");
		return renderToLine(m, this::renderMoveTo, this::renderLineTo, renderCubicTo(fixedSizeForCubic));
	}

	public boolean renderLineToMeshByStroking(MeshBuilder m, int fixedSizeForCubic, BasicStroke stroke) {
		BookmarkCache c = cache_thickening.computeIfAbsent(m, (k) -> new BookmarkCache(m));

		return m.skipTo(c.start, c.end, mod, () -> {
			Shape s = /*stroke.createStrokedShape*/(flineToJavaShape(this));
			FLine drawInstead = this.attributes.isTrue(hint_noDepth, false) ? javaShapeToFLine(s) : javaShapeToFLine(s, this, new AffineTransform());
			drawInstead.attributes.putAll(attributes);
			drawInstead.renderToMesh(m, fixedSizeForCubic);
		});

	}

	public boolean renderToMesh(MeshBuilder m, Curry.Function3<MeshAcceptor, Node, MoveTo, Node> moveTo, Curry.Function3<MeshAcceptor, Node, LineTo, Node> lineTo, Curry.Function3<MeshAcceptor, Node, CubicTo, Node> cubicTo) {

		BookmarkCache c = cache.computeIfAbsent(m, (k) -> new BookmarkCache(m));

		return m.skipTo(c.start, c.end, mod, () -> {

			MeshBuilder_tesselationSupport ts = m.getTessSupport();
			flattenAuxProperties();

			boolean noContours = attributes.isTrue(StandardFLineDrawing.noContours, false);

			m.open();
			try {
				MeshBuilder.Bookmark start = null;

				Node a = null;
				if (!noContours)
					ts.begin();

				for (int i = 0; i < nodes.size(); i++) {
					Node b = nodes.get(i);

					if (b instanceof MoveTo) {

						if (start != null) {
							if (!noContours)
								ts.endContour();
							else ts.end();
						}
						if (!noContours)
							ts.beginContour();
						else ts.begin();


						a = moveTo.apply(ts, a, (MoveTo) b);
						start = m.bookmark();
					} else {
						if (b instanceof LineTo) a = lineTo.apply(ts, a, (LineTo) b);
						else if (b instanceof CubicTo) a = cubicTo.apply(ts, a, (CubicTo) b);
						else throw new IllegalArgumentException(" unknown subclass ");
						if (start == null) {
							start = m.bookmark();
							if (!noContours)
								ts.beginContour();
							else ts.begin();
						}

					}
				}

				MeshBuilder.Bookmark end = m.bookmark();

				if (start != null && start.at() != end.at()) {
					if (!noContours)
						ts.endContour();
					else ts.end();
				}
				if (!noContours)
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

		for (Map.Entry ii : auxProperties.entrySet()) {

			// Surprise! Nashorn's map literals have string keys not integer keys

			Object k = ii.getKey();
			Integer kv = null;
			if (k instanceof Number)
				kv = ((Number) k).intValue();
			else
				kv = Integer.parseInt("" + k);

			flatAux[n] = kv;
			flatAuxNames[n++] = new Dict.Prop("" + ii.getValue());
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
		Log.log("drawing.trace", () -> "moveTo " + to.to);
		m.v(to.to.x, to.to.y, to.to.z);
		return to;
	}

	@HiddenInAutocomplete
	public Node renderLineTo(MeshAcceptor m, Node from, LineTo to) {
		if (to.flatAuxData != null) for (int i = 0; i < to.flatAuxData.length; i++) {
			int channel = to.flatAux[i];
			float[] value = to.flatAuxData[i];
			if (value != null && channel > 0) m.aux(channel, value);
		}

		Log.log("drawing.trace", () -> "lineTo " + to.to);

		m.v(to.to.x, to.to.y, to.to.z);
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

				meshBuilder.v(o.x, o.y, o.z);
			}
			return to;
		};

	}

	public FLine byTransforming(Function<Vec3, Vec3> spaceTransform) {

		FLine f = new FLine();
		f.attributes = attributes.duplicate(f);

		for (Node n : nodes) {
			if (n instanceof MoveTo) f.add(new MoveTo(spaceTransform.apply(n.to)));
			else if (n instanceof LineTo) f.add(new LineTo(spaceTransform.apply(n.to)));
			else if (n instanceof CubicTo)
				f.add(new CubicTo(spaceTransform.apply(((CubicTo) n).c1), spaceTransform.apply(((CubicTo) n).c2), spaceTransform.apply(n.to)));
			f.nodes.get(f.nodes.size() - 1).attributes = n.attributes.duplicate(f.nodes.get(f.nodes.size() - 1));
		}

		if (auxProperties != null) f.setAuxProperties(new LinkedHashMap<>(auxProperties));
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
			.normalize();
		Vec3 d = new Vec3(end).sub(start);

		Quat q = cD.isNaN() || d.isNaN() ? new Quat() : new Quat().rotateTo(d, cD);

		double s = end.distance(start) / cEnd.distance(cStart);

		if (Double.isNaN(s)) s = 1;

		double fs = s;

		return byTransforming(x -> q.transform(new Vec3(x).sub(center))
			.mul(fs)
			.add(center)
			.add(t));

	}

	/**
	 * handy method that sets the z coordinate of all the nodes in this FLine to be the `depth` of this box (or 0)
	 *
	 * @param of
	 */
	public void depthTo(Box of) {
		float d = of.properties.getFloat(depth, 0f);
		for (Node n : nodes)
			n.setZ(d);
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

		if (knownNonProperties == null)
			knownNonProperties = computeKnownNonProperties();

		if (knownNonProperties.contains(p)) return false;
		return (Dict.Canonical.findCanon(p) != null);
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

		Dict.Prop canon = new Dict.Prop(m).findCanon();

		Object ret = attributes.getOrConstruct(canon);


		if (ret instanceof Box.FunctionOfBox) {
			return ((Supplier) (() -> ((Box.FunctionOfBox) ret).apply(this)));
		}

		return ret;
	}

	@Override
	@HiddenInAutocomplete
	public Object asMap_set(String name, Object value) {

		// workaround bug in Nashorn
//		if (value instanceof ConsString) value = value.toString(); //jdk9 module security breaks this
		if (value != null && value.getClass().getName().endsWith("ConsString")) value = "" + value;

		Dict.Prop canon = new Dict.Prop(name).toCanon();

		if (canon.getAttributes().isTrue(Dict.readOnly, false))
			throw new IllegalArgumentException("can't write to property " + name);

		if (value instanceof ThreadSync2.TrappedSet) {
			Object firstValue = ((ThreadSync2.TrappedSet) value).next();

			Object r = asMap_set(name, firstValue);

			new Drivers().provokeCurrentFibre(System.identityHashCode(this) + "_" + name, new Function1<Object, Object>() {

				Object was = null;

				@Override
				public Object invoke(Object o) {
					if (o != null && !safeEq(was, o)) {
						was = o;
						modify();
						asMap_set(name, o);
					}
					return o;
				}
			}, ((ThreadSync2.TrappedSet) value));

			return r;
		}

		Function<Object, Object> c = canon.getAttributes().get(Dict.customCaster);
		if (c != null)
			value = c.apply(value);

		Object converted = convert(value, canon.getTypeInformation());

		attributes.put(canon, converted);


		modify();

		return this;
	}

	@Override
	@HiddenInAutocomplete
	public boolean asMap_delete(Object o) {
		modify();
		return attributes.asMap_delete(o);
	}

	@HiddenInAutocomplete
	public Object convert(Object value, List<Class> fit) {
		return Conversions.convert(value, fit);

//		if (fit == null) return value;
//		if (fit.get(0)
//		       .isInstance(value)) return value;
//
//		// promote non-arrays to arrays
//		if (List.class.isAssignableFrom(fit.get(0))) {
//			if (!(value instanceof List)) {
//				return Collections.singletonList(convert(value, fit.subList(1, fit.size())));
//			} else {
//				return value;
//			}
//		} else if (Map.class.isAssignableFrom(fit.get(0)) && String.class.isAssignableFrom(fit.get(1))) {
//			// promote non-Map<String, V> to Map<String, V>
//			if (!(value instanceof Map)) {
//				return Collections.singletonMap("" + value + ":" + System.identityHashCode(value), convert(value, fit.subList(2, fit.size())));
//			} else {
//				return value;
//			}
//
//		} else if (Collection.class.isAssignableFrom(fit.get(0))) {
//			if (!(value instanceof Collection)) {
//				return Collections.singletonList(convert(value, fit.subList(1, fit.size())));
//			} else {
//				return value;
//			}
//
//		}
//
////		if (value instanceof ScriptFunction) {
////			StaticClass adapterClassFor = JavaAdapterFactory.getAdapterClassFor(new Class[]{fit.get(0)}, (ScriptObject) value, MethodHandles.lookup());
////			try {
////				return adapterClassFor.getRepresentedClass()
////						      .newInstance();
////			} catch (InstantiationException e) {
////				Log.log("underscore.error", ()->" problem instantiating adaptor class to take us from " + value + " ->" + fit.get(0)+e);
////			} catch (IllegalAccessException e) {
////				Log.log("underscore.error", ()->" problem instantiating adaptor class to take us from " + value + " ->" + fit.get(0)+e);
////			}
////		}
//
//		return value;
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
		throw new Error();
	}

	@Override
	public List<Completion> getCompletionsFor(String prefix) {

		List<Completion> l1 = Dict.canonicalProperties().filter(x -> x.getAttributes().has(Dict.domain))
			.filter(x -> x.getAttributes().get(Dict.domain).contains("fline"))
			.filter(x -> x.getName().startsWith(prefix))
			.map(q -> new Completion(-1, -1, q.getName(), " = <span class='type'>" + Conversions.fold(q.getTypeInformation(), t -> compress(
				t)) + "</span> " + possibleToString(q) + " &mdash; <span class='doc'>" + format(q.getDocumentation()) + "</span>")).collect(Collectors.toList());


		l1.forEach(x -> {
			if (!x.info.contains("(unset)"))
				x.rank -= 200;
		});

		List<Completion> l1b = attributes.getMap().keySet().stream()
			.filter(x -> x.getName().startsWith(prefix))
			.map(q -> new Completion(-1, -1, q.getName(), " = <span class='type'>" + Conversions.fold(q.getTypeInformation(), t -> compress(
				t)) + "</span> " + possibleToString(q) + " &mdash; <span class='doc'>" + format(q.getDocumentation()) + "</span>")).collect(Collectors.toList());


		l1b.stream().filter(x -> {
			x.rank += 100;
			for (Completion cc : l1) {
				if (cc.replacewith.equals(x.replacewith))
					return false;
			}
			return true;
		}).forEach(x -> l1.add(x));


		List<Completion> l2 = JavaSupport.javaSupport.getOptionCompletionsFor(this, prefix);

		l1.addAll(l2.stream()
			.filter(x -> {
				for (Completion c : l1)
					if (c.replacewith.equals(x.replacewith)) return false;
				return true;
			})
			.collect(Collectors.toList()));

		return l1;

	}

	private String possibleToString(Dict.Prop q) {
//		if (!box.properties.has(q)) return "";

		Object v = this.attributes.get(q);

		if (v == null)
			return "(unset)";

		try {
			if (v.getClass().getMethod("toString").getDeclaringClass() != Object.class) {
				String r = " = " + v;
				if (r.length() < 140)
					return r;
				else return r.substring(0, 100) + "...";
			}

		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}
		return "";
	}

	@Override
	public Object __sub__(Object b) {
		if (b instanceof Vec2) {
			Vec3 finalB = ((Vec2) b).toVec3();
			return byTransforming(x -> new Vec3(x).sub(finalB));
		} else if (b instanceof Vec3) {
			Vec3 finalB = (Vec3) b;
			return byTransforming(x -> new Vec3(x).sub(finalB));
		} else if (b instanceof Quat) {
			Quat finalB = (Quat) b;
			return byTransforming(x -> finalB.invert(new Quat()).transform(x, new Vec3()));
		} else if (b instanceof FLine) {
			Shape s1 = FLinesAndJavaShapes.flineToJavaShape(this);
			Shape s2 = FLinesAndJavaShapes.flineToJavaShape((FLine) b);

			Area a1 = new Area(s1);
			Area a2 = new Area(s2);
			a1.subtract(a2);
			return FLinesAndJavaShapes.javaShapeToFLine(a1);
		} else if (b instanceof OverloadedMath) return ((OverloadedMath) b).__rsub__(this);
		throw new ClassCastException(" can't subtract '" + b + "' from this FLine");
	}

	@Override
	public Object __rsub__(Object b) {
		if (b instanceof Vec2) {
			Vec3 finalB = ((Vec2) b).toVec3();
			return byTransforming(x -> new Vec3(x).sub(finalB));
		} else if (b instanceof Vec3) {
			Vec3 finalB = (Vec3) b;
			return byTransforming(x -> new Vec3(x).sub(finalB));
		} else if (b instanceof Quat) {
			Quat finalB = (Quat) b;
			return byTransforming(x -> finalB.invert(new Quat()).transform(x, new Vec3()));
		} else if (b instanceof FLine) {
			Shape s1 = FLinesAndJavaShapes.flineToJavaShape(this);
			Shape s2 = FLinesAndJavaShapes.flineToJavaShape((FLine) b);

			Area a1 = new Area(s1);
			Area a2 = new Area(s2);
			a1.subtract(a2);
			return FLinesAndJavaShapes.javaShapeToFLine(a1);
		}
		throw new ClassCastException(" can't subtract '" + b + "' from this FLine");
	}

	@Override
	public Object __add__(Object b) {
		if (b instanceof Vec2) {
			Vec3 finalB = ((Vec2) b).toVec3();
			return byTransforming(x -> new Vec3(x).add(finalB));
		} else if (b instanceof Vec3) {
			Vec3 finalB = (Vec3) b;
			return byTransforming(x -> new Vec3(x).add(finalB));
		} else if (b instanceof Quat) {
			Quat finalB = (Quat) b;
			return byTransforming(x -> finalB.transform(x, new Vec3()));
		} else if (b instanceof FLine) {
			Shape s1 = FLinesAndJavaShapes.flineToJavaShape(this);
			Shape s2 = FLinesAndJavaShapes.flineToJavaShape((FLine) b);

			Area a1 = new Area(s1);
			Area a2 = new Area(s2);
			a1.add(a2);
			return FLinesAndJavaShapes.javaShapeToFLine(a1);
		} else if (b instanceof OverloadedMath) return ((OverloadedMath) b).__radd__(this);
		throw new ClassCastException(" can't add '" + b + "' to this FLine");
	}

	@Override
	public Object __radd__(Object b) {

		if (b instanceof Vec2) {
			Vec3 finalB = ((Vec2) b).toVec3();
			return byTransforming(x -> new Vec3(x).add(finalB));
		} else if (b instanceof Vec3) {
			Vec3 finalB = (Vec3) b;
			return byTransforming(x -> new Vec3(x).add(finalB));
		} else if (b instanceof Quat) {
			Quat finalB = (Quat) b;
			return byTransforming(x -> finalB.transform(x, new Vec3()));
		} else if (b instanceof FLine) {
			Shape s1 = FLinesAndJavaShapes.flineToJavaShape(this);
			Shape s2 = FLinesAndJavaShapes.flineToJavaShape((FLine) b);

			Area a1 = new Area(s1);
			Area a2 = new Area(s2);
			a1.add(a2);
			return FLinesAndJavaShapes.javaShapeToFLine(a1);
		}
		throw new ClassCastException(" can't add '" + b + "' to this FLine");
	}

	@Override
	public Object __mul__(Object b) {
		if (b instanceof Number) {
			return byTransforming(x -> new Vec3(x).scale(((Number) b).doubleValue()));
		} else if (b instanceof Vec2) {
			Vec3 finalB = ((Vec2) b).toVec3();
			return byTransforming(x -> new Vec3(x).mul(finalB));
		} else if (b instanceof Vec3) {
			Vec3 finalB = (Vec3) b;
			return byTransforming(x -> new Vec3(x).mul(finalB));
		} else if (b instanceof Quat) {
			Quat finalB = (Quat) b;
			return byTransforming(x -> finalB.transform(x, new Vec3()));
		} else if (b instanceof FLine) {
			Shape s1 = FLinesAndJavaShapes.flineToJavaShape(this);
			Shape s2 = FLinesAndJavaShapes.flineToJavaShape((FLine) b);

			Area a1 = new Area(s1);
			Area a2 = new Area(s2);
			a1.intersect(a2);
			return FLinesAndJavaShapes.javaShapeToFLine(a1);
		} else if (b instanceof OverloadedMath) return ((OverloadedMath) b).__rmul__(this);
		throw new ClassCastException(" can't multiply '" + b + "' to this FLine");
	}

	@Override
	public Object __rmul__(Object b) {
		if (b instanceof Number) {
			return byTransforming(x -> new Vec3(x).scale(((Number) b).doubleValue()));
		} else if (b instanceof Vec2) {
			Vec3 finalB = ((Vec2) b).toVec3();
			return byTransforming(x -> new Vec3(x).mul(finalB));
		} else if (b instanceof Vec3) {
			Vec3 finalB = (Vec3) b;
			return byTransforming(x -> new Vec3(x).mul(finalB));
		} else if (b instanceof Quat) {
			Quat finalB = (Quat) b;
			return byTransforming(x -> finalB.transform(x, new Vec3()));
		} else if (b instanceof FLine) {
			Shape s1 = FLinesAndJavaShapes.flineToJavaShape(this);
			Shape s2 = FLinesAndJavaShapes.flineToJavaShape((FLine) b);

			Area a1 = new Area(s1);
			Area a2 = new Area(s2);
			a1.intersect(a2);
			return FLinesAndJavaShapes.javaShapeToFLine(a1);
		}
		throw new ClassCastException(" can't multiply '" + b + "' to this FLine");
	}


	public class Node implements fieldlinker.AsMap {
		public final Vec3 to;
		public Dict attributes = new Dict();
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
			throw new Error();
		}

		@Override
		@HiddenInAutocomplete
		public Object asMap_get(String m) {

			Dict.Prop canon = new Dict.Prop(m).findCanon();

			Object ret = attributes.getOrConstruct(canon);


			if (ret instanceof Box.FunctionOfBox) {
				return ((Supplier) (() -> ((Box.FunctionOfBox) ret).apply(this)));
			}

			return ret;
		}

		@Override
		public boolean asMap_delete(Object o) {
			return attributes.asMap_delete(o);
		}

		@Override
		@HiddenInAutocomplete
		public Object asMap_set(String name, Object value) {

			// workaround bug in Nashorn
//			if (value instanceof ConsString) value = value.toString();
			if (value != null && value.getClass().getName().endsWith("ConsString")) value = "" + value;

			Dict.Prop canon = new Dict.Prop(name).toCanon();

			Object converted = convert(value, canon.getTypeInformation());

			attributes.put(canon, converted);

			if (value instanceof ThreadSync2.TrappedSet) {
				Object firstValue = ((ThreadSync2.TrappedSet) value).next();

				Object r = asMap_set(name, firstValue);

				Drivers.provokeCurrentFibre(System.identityHashCode(this) + "_" + name, new Function1<Object, Object>() {

					Object was = null;

					@Override
					public Object invoke(Object o) {
						if (o != null && !safeEq(was, o)) {
							was = o;
							modify();
							asMap_set(name, o);
						}
						return o;
					}
				}, ((ThreadSync2.TrappedSet) value));

				return r;
			}

			modify();


			return this;
		}

		@Override
		public Object asMap_new(Object b, Object c) {
			throw new NoSuchMethodError(" two argument constructor to node not implemented");
		}

		@HiddenInAutocomplete
		public Object convert(Object value, List<Class> fit) {
			return Conversions.convert(value, fit);

//			if (fit == null) return value;
//			if (fit.get(0)
//			       .isInstance(value)) return value;
//
//			// promote non-arrays to arrays
//			if (List.class.isAssignableFrom(fit.get(0))) {
//				if (!(value instanceof List)) {
//					return Collections.singletonList(convert(value, fit.subList(1, fit.size())));
//				} else {
//					return value;
//				}
//			} else if (Map.class.isAssignableFrom(fit.get(0)) && String.class.isAssignableFrom(fit.get(1))) {
//				// promote non-Map<String, V> to Map<String, V>
//				if (!(value instanceof Map)) {
//					return Collections.singletonMap("" + value + ":" + System.identityHashCode(value), convert(value, fit.subList(2, fit.size())));
//				} else {
//					return value;
//				}
//
//			} else if (Collection.class.isAssignableFrom(fit.get(0))) {
//				if (!(value instanceof Collection)) {
//					return Collections.singletonList(convert(value, fit.subList(1, fit.size())));
//				} else {
//					return value;
//				}
//
//			}
//
//			if (value instanceof ScriptFunction) {
//				StaticClass adapterClassFor = JavaAdapterFactory.getAdapterClassFor(new Class[]{fit.get(0)}, (ScriptObject) value, MethodHandles.lookup());
//				try {
//					return adapterClassFor.getRepresentedClass()
//							      .newInstance();
//				} catch (InstantiationException e) {
//					Log.log("underscore.error", ()->" problem instantiating adaptor class to take us from " + value + " ->" + fit.get(0)+e);
//				} catch (IllegalAccessException e) {
//					Log.log("underscore.error", ()->" problem instantiating adaptor class to take us from " + value + " ->" + fit.get(0)+e);
//				}
//			}
//
//			return value;
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
			throw new Error();
		}

		@Override
		public Object asMap_setElement(int element, Object v) {
			throw new Error();
		}

		@Override
		@HiddenInAutocomplete
		public boolean asMap_isProperty(String p) {
			if (Dict.Canonical.findCanon(p) != null) return true;

			if (knownNonProperties == null) knownNonProperties = computeKnownNonProperties();

			return !knownNonProperties.contains(p);

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

		public void setZ(float z) {
			this.to.z = z;
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
			l.attributes.putAll(attributes.duplicate());
			return l;
		}

		@Override
		public String toString() {
			return "m[" + to.x + "," + to.y + "," + to.z + "]";
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
			l.attributes.putAll(attributes.duplicate());
			return l;
		}

		@Override
		public String toString() {
			return "l[" + to.x + "," + to.y + "," + to.z + "]";
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
			l.attributes.putAll(attributes.duplicate());
			return l;
		}

		@Override
		public String toString() {
			return "c[" + c1.x + "," + c1.y + "," + c1.z + ";" + c2.x + "," + c2.y + "," + c2.z + ";" + to.x + "," + to.y + "," + to.z + "]";
		}

		public void setZ(float z) {
			this.to.z = z;
			this.c1.z = z;
			this.c2.z = z;
		}

	}

	@Override
	public String toString() {
		return "FLine (with " + nodes.size() + " node" + (nodes.size() == 1 ? "" : "s") + ")";
	}


	private void writeObject(ObjectOutputStream out) throws IOException {
		FLineSerializationHelper.writeObject(this, out);
	}

	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		nodes = new ArrayList<>();
		attributes = new Dict();
		cache = new WeakHashMap<>();
		cache_thickening = new WeakHashMap<>();

		FLineSerializationHelper.readObject(this, in);

	}
}
