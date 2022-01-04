package field.graphics.util.onsheetui;

import field.app.RunLoop;
import field.graphics.FLine;
import field.graphics.StandardFLineDrawing;
import field.graphics.Window;
import field.linalg.Vec2;
import field.linalg.Vec4;
import field.utility.*;
import fieldagent.Main;
import fieldbox.boxes.*;
import fieldbox.boxes.plugins.BoxDefaultCode;
import fieldbox.boxes.plugins.Exec;
import fieldbox.boxes.plugins.FrameChangedHash;
import fieldbox.io.IO;
import fieldlinker.AsMap;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static fielded.ServerSupport.readFile;

/**
 * Extra help, enough to run the javascript from in browser to the same effect
 */
public class SimpleCanvas extends Box implements IO.Loaded {

	static public final Dict.Prop<String> __preamble = new Dict.Prop<>("__preamble").type()
		.toCanon().set(Dict.domain, "attributes").set(BoxDefaultCode._configured, true);

	protected final Runnable repaint;
	protected final Map<String, Set> translation = new LinkedHashMap<>();
	private final Consumer<Supplier<FLine>> dest;
	private final Function<Vec2, Vec2> drawingToCanvas;

	public class Shape extends AsMapDelegator {
		FLine sourceGeometry;

		public Shape(FLine g) {
			this.sourceGeometry = g;//g.byTransforming(a -> canvasToDrawing.apply(a.toVec2()).toVec3());
			this.sourceGeometry.attributes.put(FLineInteraction.interactionOutset, 15f);
		}

		public void animate(Map<String, Object> p, int over) {

			long timeIn = System.currentTimeMillis();

			Dict original = sourceGeometry.attributes.duplicate();

			RunLoop.main.getLoop().attach(pass -> {
				boolean c = (System.currentTimeMillis() - timeIn) < over;

				float f = (System.currentTimeMillis() - timeIn) / over;
				if (f > 1) f = 1;
				if (c) f = 1;
				if (f < 0) f = 0;

				float finalF = f;
				p.entrySet().stream().forEach(x -> {
					handle(x.getKey(), original, x.getValue(), finalF, sourceGeometry);
				});

				change(sourceGeometry);

				return c;
			});
		}

		public void drag(BiConsumer<Float, Float> move, Runnable down, Runnable up) {

			sourceGeometry.attributes.putToMap(Mouse.onMouseDown, "__shape__", (state, button) -> {

				if (state.after.keyboardState.isSuperDown()) return null;

				Vec2 opos = drawingToCanvas.apply(new Vec2(state.after.mx, state.after.my));

				down.run();

				state.properties.put(Window.consumed, true);

				return (next, end) -> {
					Vec2 position = new Vec2(next.after.mx, next.after.my);
					position = drawingToCanvas.apply(position);

					move.accept((float) (position.x - opos.x), (float) (position.y - opos.y));
					next.properties.put(Window.consumed, true);

					if (end) {
						up.run();
					}
					return !end;
				};
			});

		}

		public void hover(Runnable over, Runnable out) {

		}

		public void click(Runnable click) {
			sourceGeometry.attributes.putToMap(Mouse.onMouseDown, "__shape__click", (state, button) -> {

				if (state.after.keyboardState.isSuperDown()) return null;

				state.properties.put(Window.consumed, true);

				click.run();

				return null;
			});
		}

		public Shape attr(Map<String, Object> p) {
			boolean[] change = {
				false
			};
			p.entrySet().stream().forEach(x -> {
				change[0] |= handle(x.getKey(), null, x.getValue(), 1, sourceGeometry);
			});

			if (change[0]) change(sourceGeometry);
			return this;
		}

		public Object attr(String p) {
			return null;
		}

		protected boolean handle(String key, Dict original, Object value, float fract, FLine sourceGeometry) {
			Set s = translation.get(key);
			if (s == null) {
				System.err.println(" warning: unhandled property <" + key + ">");
				return false;
			}

			return s.set(key, original, value, fract, sourceGeometry);
		}

		Dict d = new Dict();

		@Override
		protected AsMap delegateTo() {
			return d;
		}
	}


	public interface Set {
		boolean set(String key, Dict original, Object value, float fraction, FLine target);
	}


	private void change(FLine sourceGeometry) {
		sourceGeometry.modify();
		repaint.run();
	}


	public SimpleCanvas() {

		this.repaint = () ->
			Drawing.dirty(this);

		this.dest = line -> {
			String n2 = "__" + name + "__" + System.identityHashCode(line);

			line = FLineDrawing.boxOrigin(line, new Vec2(0, 0), this);

			this.properties.putToMap(FLineDrawing.lines, n2, line);
			this.properties.putToMap(FLineInteraction.interactiveLines, n2, line);

		};

		this.drawingToCanvas = x -> new Vec2(x).sub(new Vec2(properties.get(Box.frame).x, properties.get(Box.frame).y));

		this.properties.putToMap(FLineDrawing.frameDrawing, "__canvas__", FrameChangedHash.getCached(this, (a, b) -> {

			if (noFrame) return new FLine();

			FLine m = new FLine().rect(this.properties.get(Box.frame));
			m.attributes.put(StandardFLineDrawing.filled, true);
			m.attributes.put(StandardFLineDrawing.fillColor, new Vec4(0, 0, 0, 0.1));
			m.attributes.put(StandardFLineDrawing.stroked, true);
			m.attributes.put(StandardFLineDrawing.strokeColor, new Vec4(1, 1, 1, 0.3));
			float dd = this.properties.getFloat(depth, 0f);
			if (dd == 0)
				m.attributes.put(StandardFLineDrawing.hint_noDepth, true);
			else
				m.nodes.forEach(x -> x.setZ(dd));
			return m;
		}, () -> 0L));


		buildProperties();
	}

	public void clear() {
		IdempotencyMap<Supplier<FLine>> ll = this.properties.get(FLineDrawing.lines);
		if (ll != null) ll.clear();

		ll = this.properties.get(FLineInteraction.interactiveLines);
		if (ll != null) ll.clear();
	}

	public void loaded() {
		String drags = readFile(Main.app + "/lib/web/drags.js");

		String m = this.properties.get(__preamble) + "\n" + drags;

		Optional<Triple<Object, java.util.List<String>, List<Pair<Integer, String>>>> ret
			= this.find(Exec.exec, this.upwards()).findFirst().map(x -> x.apply(this, m));

		System.out.println(" ran preamble and got :"+ret);

		this.properties.put(FrameManipulation.lockHeight, true);
		this.properties.put(FrameManipulation.lockWidth, true);
	}

	public boolean noFrame = false;


	static public SimpleCanvas makeNewCanvas(String name, Box parent, Rect canvas) {
		// todo, need to make canvas live
		SimpleCanvas[] c = {null};

		c[0] = new SimpleCanvas(() ->
			Drawing.dirty(c[0]), line -> {
			String n2 = "__" + name + "__" + System.identityHashCode(line);

			line = FLineDrawing.boxOrigin(line, new Vec2(0, 0), c[0]);

			c[0].properties.putToMap(FLineDrawing.lines, n2, line);
			c[0].properties.putToMap(FLineInteraction.interactiveLines, n2, line);

		}, drawing -> new Vec2(drawing).sub(new Vec2(c[0].properties.get(Box.frame).x, c[0].properties.get(Box.frame).y)));

		SimpleCanvas cc = c[0];

		parent.connect(cc);

		cc.properties.put(Box.name, name);
		cc.properties.put(Box.frame, canvas);

		cc.properties.putToMap(FLineDrawing.frameDrawing, "__canvas__", FrameChangedHash.getCached(parent, (a, b) -> {

			if (cc.noFrame) return new FLine();

			FLine m = new FLine().rect(cc.properties.get(Box.frame));
			m.attributes.put(StandardFLineDrawing.filled, true);
			m.attributes.put(StandardFLineDrawing.fillColor, new Vec4(0, 0, 0, 0.1));
			m.attributes.put(StandardFLineDrawing.stroked, true);
			m.attributes.put(StandardFLineDrawing.strokeColor, new Vec4(1, 1, 1, 0.3));
			float dd = cc.properties.getFloat(depth, 0f);
			if (dd == 0)
				m.attributes.put(StandardFLineDrawing.hint_noDepth, true);
			else
				m.nodes.forEach(x -> x.setZ(dd));
			return m;
		}, () -> 0L));

		String drags = readFile(Main.app + "/lib/web/drags.js");

		String m = cc.properties.get(__preamble) + "\n" + drags;

		Optional<Triple<Object, java.util.List<String>, List<Pair<Integer, String>>>> ret = cc.find(Exec.exec, cc.upwards()).findFirst().map(x -> x.apply(cc, m));

		System.out.println(" run preamble and drags and got :" + ret);

		return cc;
	}

	protected SimpleCanvas(Runnable repaint, Consumer<Supplier<FLine>> dest,
			       Function<Vec2, Vec2> drawingToCanvas) {

		this.repaint = repaint;
		this.dest = dest;
		this.drawingToCanvas = drawingToCanvas;

		buildProperties();
	}


	protected void buildProperties() {
		translation.put("stroke", getSetColor(StandardFLineDrawing.strokeColor, StandardFLineDrawing.stroked));
		translation.put("fill", getSetColor(StandardFLineDrawing.fillColor, StandardFLineDrawing.filled));
		translation.put("stroke-opacity", getSetNumber(StandardFLineDrawing.strokeOpacity, StandardFLineDrawing.stroked));
		translation.put("fill-opacity", getSetNumber(StandardFLineDrawing.fillOpacity, StandardFLineDrawing.filled));
//		translation.put("stroke-width", getSetNumber(StandardFLineDrawing.thicken, StandardFLineDrawing.filled, width -> new BasicStroke(width), stroke -> stroke.getLineWidth()));
	}

	private Set getSetColor(Dict.Prop<Supplier<Vec4>> p, Dict.Prop<Boolean> flag) {
		return (String key, Dict original, Object value, float fraction, FLine target) -> {
			Vec4 c = color(value);

			if (c == null) return false;

			if (flag != null) {
				target.attributes.put(flag, true);
			}

			if (original == null || fraction == 1)
				target.attributes.put(p, c);
			else if (fraction > 0) {
				Supplier<Vec4> o = original.get(p);
				if (o == null)
					target.attributes.put(p, c);
				else {

					Vec4 g = o.get();
					if (g == null)
						target.attributes.put(p, c);
					else
						target.attributes.put(p, new Vec4().lerp(g, c, fraction));
				}
			}

			return c != null;
		};
	}

	private Set getSetNumber(Dict.Prop<Float> p, Dict.Prop<Boolean> flag) {
		return getSetNumber(p, flag, x -> x, y -> y);
	}

	private <T> Set getSetNumber(Dict.Prop<T> p, Dict.Prop<Boolean> flag, Function<Float, T> write, Function<T, Float> read) {
		return (String key, Dict original, Object value, float fraction, FLine target) -> {

			Float c = number(value);
			if (c == null) return false;

			if (flag != null) {
				target.attributes.put(flag, true);
			}

			if (original == null || fraction == 1)
				target.attributes.put(p, write.apply(c));
			else if (fraction > 0) {
				Number o = read.apply(original.get(p));
				if (o == null)
					target.attributes.put(p, write.apply(c));
				else {
					target.attributes.put(p, write.apply(o.floatValue() * (1 - fraction) + fraction * c.floatValue()));
				}
			}

			return c != null;
		};
	}


	private Vec4 color(Object value) {
		String m = "" + value;
		if (m.startsWith("#")) {
			if (m.length() == 4) {
				float r = Integer.parseInt(m.charAt(1) + "" + m.charAt(1), 16) / 255f;
				float g = Integer.parseInt(m.charAt(2) + "" + m.charAt(2), 16) / 255f;
				float b = Integer.parseInt(m.charAt(3) + "" + m.charAt(3), 16) / 255f;
				return new Vec4(r, g, b, 1);
			} else if (m.length() == 7) {
				float r = Integer.parseInt(m.charAt(1) + "" + m.charAt(2), 16) / 255f;
				float g = Integer.parseInt(m.charAt(3) + "" + m.charAt(4), 16) / 255f;
				float b = Integer.parseInt(m.charAt(5) + "" + m.charAt(6), 16) / 255f;
				return new Vec4(r, g, b, 1);
			}
		}
		System.err.println(" don't know what to do with color " + m);
		return null;
	}

	private Float number(Object value) {
		if (value instanceof Number) return ((Number) value).floatValue();
		if (value instanceof String) return Float.parseFloat((String) value);
		System.err.println(" don't know what to do with number " + value);
		return null;
	}


	public Shape circle(float x, float y, float r) {
		FLine g = new FLine().circle(x, y, r);
		Shape s = new CircleShape(g, x, y, r);
		dest.accept(s.sourceGeometry);
		return s;
	}

	public Shape path(String def) {
		FLine f = new FLine();
		parsePathList(def, f);
		Shape s = new PathShape(f);
		dest.accept(s.sourceGeometry);
		return s;
	}

	protected void parsePathList(String list, FLine path) {
		final Matcher matchPathCmd = Pattern.compile("([MmLlHhVvAaQqTtCcSsZz])|([-+]?((\\d*\\.\\d+)|(\\d+))([eE][-+]?\\d+)?)").matcher(list);

		//Tokenize
		LinkedList<String> tokens = new LinkedList<String>();
		while (matchPathCmd.find()) {
			tokens.addLast(matchPathCmd.group());
		}

		char curCmd = 'Z';
		while (tokens.size() != 0) {
			String curToken = tokens.removeFirst();
			char initChar = curToken.charAt(0);
			if ((initChar >= 'A' && initChar <= 'Z') || (initChar >= 'a' && initChar <= 'z')) {
				curCmd = initChar;
			} else {
				tokens.addFirst(curToken);
			}

			switch (curCmd) {
				case 'M':
					path.moveTo(nextFloat(tokens), nextFloat(tokens));
					curCmd = 'L';
					break;
				case 'm':
					path.moveToRel(nextFloat(tokens), nextFloat(tokens));
					curCmd = 'l';
					break;
				case 'L':
					path.lineTo(nextFloat(tokens), nextFloat(tokens));
					break;
				case 'l':
					path.lineToRel(nextFloat(tokens), nextFloat(tokens));
					break;
				case 'h':
					path.lineToRel(nextFloat(tokens), 0);
					break;
				case 'v':
					path.lineToRel(0, nextFloat(tokens));
					break;
				case 'C':
					path.cubicTo(nextFloat(tokens), nextFloat(tokens),
						nextFloat(tokens), nextFloat(tokens),
						nextFloat(tokens), nextFloat(tokens));
					break;
				case 'c':
					path.cubicToRel(nextFloat(tokens), nextFloat(tokens),
						nextFloat(tokens), nextFloat(tokens),
						nextFloat(tokens), nextFloat(tokens));
					break;
				case 'S':
					// not quite the same
					nextFloat(tokens);
					nextFloat(tokens);
					path.smoothTo(
						nextFloat(tokens), nextFloat(tokens));
					break;
				default:
					throw new RuntimeException("Invalid path element " + curCmd);
			}
		}
	}

	static protected float nextFloat(LinkedList<String> l) {
		String s = l.removeFirst();
		return Float.parseFloat(s);
	}


	public class CircleShape extends Shape {
		float cx;
		float cy;
		float cr;

		public CircleShape(FLine g, float x, float y, float r) {
			super(g);
			cx = x;
			cy = y;
			cr = r;
		}

		@Override
		public Object attr(String p) {
			if (p.equals("cx"))
				return cx;
			if (p.equals("cy"))
				return cy;
			if (p.equals("y"))
				return cr;
			return null;
		}

		protected boolean handle(String key, Dict original, Object value, float fract, FLine sourceGeometry) {
			System.out.println(key + " <- " + value);

			if (key.equals("cx")) {
				this.cx = number(value);
				Vec2 out = /*canvasToDrawing.apply(*/new Vec2(this.cx, this.cy);
				FLine c = new FLine().circle(out.x, out.y, cr);

				this.sourceGeometry.clear();
				this.sourceGeometry.append(c);
				this.sourceGeometry.modify();
				return true;
			}
			if (key.equals("cy")) {
				this.cy = number(value);
				Vec2 out = /*canvasToDrawing.apply(*/new Vec2(this.cx, this.cy);

				FLine c = new FLine().circle(out.x, out.y, cr);

				this.sourceGeometry.clear();
				this.sourceGeometry.append(c);
				this.sourceGeometry.modify();
				return true;
			}
			return super.handle(key, original, value, fract, sourceGeometry);
		}
	}

	public class PathShape extends Shape {

		public PathShape(FLine g) {
			super(g);
		}

		protected boolean handle(String key, Dict original, Object value, float fract, FLine sourceGeometry) {
			if (key.equals("path")) {
				sourceGeometry.clear();
				parsePathList("" + value, sourceGeometry);
				sourceGeometry.modify();
				return true;
			}
			return super.handle(key, original, value, fract, sourceGeometry);
		}
	}
}
