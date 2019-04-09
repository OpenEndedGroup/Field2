package fieldbox.boxes;

import field.graphics.FLine;
import field.linalg.Vec4;
import field.utility.*;
import fieldbox.boxes.plugins.Chorder;
import fieldbox.boxes.plugins.Delete;
import fieldbox.boxes.plugins.Initiators;
import fieldbox.boxes.plugins.Planes;
import fieldbox.boxes.plugins.LocalTime;
import fieldbox.execution.Execution;
import fieldlinker.Linker;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static field.graphics.StandardFLineDrawing.*;
import static fieldbox.boxes.FLineDrawing.frameDrawing;

/**
 * A TimeSlider is a (thin) box that executes (that is calls .begin() and .end() on) other boxes that it passes over when dragged horizontally
 * around.
 * <p>
 * This has been built for "subclassability". Change the drawing by changing this::defaultdrawsLines, change the population of elements that can be
 * executed by changing this::population, change the behavior by changing this::off, this::on, this::skipForward, this::skipBackward.
 */
public class TimeSlider extends Box {

	static public final Dict.Prop<TimeSlider> time = new Dict.Prop<>("time").toCanon().doc("the default red-line time slider. Set `_.time.frame.x` = something to move it around.");
	static public final Dict.Prop<Double> velocity = new Dict.Prop<>("velocity").toCanon().doc("the rate at which this time slider is moving (that is, delta-frame.x per update).");
	static public final Dict.Prop<Boolean> isRunning = new Dict.Prop<>("isRunning").toCanon().doc("set this to true if you are smoothly changing time in a way that it makes sense to interpolate" +
		" and dead-reckon off of. Automatically set to false when the frame is dragged around.");


	public static Dict.Prop<BiFunctionOfBoxAnd<Double, Double>> localTime = new Dict.Prop<>("localTime")
		.toCanon().type().doc("setting this property modifies");

	Rect was = null;

	int width = 20;
	private Map<Box, Double> currentMapping = new LinkedHashMap<>();
	private float currentMappingAtTime = 0;

	public float topLimit = Float.NEGATIVE_INFINITY;
	public float bottomLimit = Float.POSITIVE_INFINITY;

	public boolean disable = false;

	public TimeSlider() {
		this.properties.putToMap(Boxes.insideRunLoop, "main.__force_onscreen__", this::forceOnscreen);
		this.properties.putToMap(Boxes.insideRunLoop, "main.__swipe__", this::swiper);

		this.properties.putToMap(Callbacks.onFrameChanged, "__dontInterpolate__", (box, next) -> {
			this.properties.put(isRunning, false);
			return was;
		});

		properties.put(frame, new Rect(0, 0, width, 5000));

		this.properties.put(isRunning, false);
		this.properties.put(Delete.undeletable, true);
		this.properties.put(FrameManipulation.lockWidth, true);
		this.properties.put(FrameManipulation.lockHeight, true);
		this.properties.put(Boxes.dontSave, true);
		this.properties.put(Box.name, "TimeSlider");

		this.properties.computeIfAbsent(frameDrawing, this::defaultdrawsLines);
		this.properties.put(velocity, 0d);
	}

	protected boolean swiper() {
		if (was == null) {
			this.properties.put(velocity, 0d);
			was = this.properties.get(frame).duplicate();
		} else {
			Rect now = this.properties.get(frame);
			if (now.x == was.x) {
				this.properties.put(velocity, 0d);
			} else {
				this.properties.put(velocity, (double) (now.x - was.x));
				perform(was, now);
			}
			was = now.duplicate();
		}

		return true;
	}

	protected boolean forceOnscreen() {
		Rect f = properties.get(frame);

//		if (was!=null && was.equals(f)) return true;

		Rect w = f.inset(0);

		Drawing d = first(Drawing.drawing).orElse(null);

		float safety = 500;

		Rect viewBounds = d.getCurrentViewBounds(TimeSlider.this);

		if (d != null) {
			w.y = viewBounds.y - width - safety;
			w.h = viewBounds.h + width * 2 + safety * 2;
			w.w = width;
		}

		// this check stops us from having to blow the MeshBuilder cache on every vertical scroll...
		if (!w.equals(f) && (f.y > viewBounds.y || f.y + f.h < viewBounds.y + viewBounds.h)) {
			properties.put(frame, w);
			Drawing.dirty(TimeSlider.this);
		} else {
		}

		return true;
	}

	/*
	protected void perform(Rect was, Rect now) {

		Stream<Box> off = population().filter(x -> x.properties.get(frame).intersectsX(was.x))
			.filter(x -> !x.properties.get(frame)
				.intersectsX(now.x));
		Stream<Box> on = population().filter(x -> !x.properties.get(frame).intersectsX(was.x))
			.filter(x -> x.properties.get(frame)
				.intersectsX(now.x));
		Stream<Box> skipForward = population().filter(x -> x.properties.get(frame)
			.inside(was.x, now.x));
		Stream<Box> skipBackward = population().filter(x -> x.properties.get(frame).inside(now.x, was.x));

		off(off);
		on(on);
		skipForward(skipForward);
		skipBackward(skipBackward);

	}
*/
	protected void perform(Rect was, Rect now) {

		Map<Box, Double> previousMapping = LocalTime.Companion.growTimeFor(null, population().filter(x -> x.properties.get(frame).y>topLimit && x.properties.get(frame).y<bottomLimit).filter(x -> x.properties.get(frame).intersectsX(was.x)).collect(Collectors.toSet()), was.x);
		previousMapping.entrySet().removeIf(m -> !m.getKey().properties.get(frame).intersectsX(m.getValue()));
		Map<Box, Double> currentMapping = LocalTime.Companion.growTimeFor(null, population().filter(x -> x.properties.get(frame).y>topLimit && x.properties.get(frame).y<bottomLimit).filter(x -> x.properties.get(frame).intersectsX(now.x)).collect(Collectors.toSet()), now.x);
		currentMapping.entrySet().removeIf(m -> !m.getKey().properties.get(frame).intersectsX(m.getValue()));

		Set<Box> off = new LinkedHashSet<>(previousMapping.keySet());
		off.removeAll(currentMapping.keySet());
		Set<Box> on = new LinkedHashSet<>(currentMapping.keySet());
		on.removeAll(previousMapping.keySet());


//		Stream<Box> off = population().filter(x -> x.properties.get(frame).intersectsX(was.x))
//			.filter(x -> !x.properties.get(frame)
//				.intersectsX(now.x));
//		Stream<Box> on = population().filter(x -> !x.properties.get(frame).intersectsX(was.x))
//			.filter(x -> x.properties.get(frame)
//				.intersectsX(now.x));

		Set<Box> skipForward = new LinkedHashSet<>(currentMapping.keySet());
		skipForward.retainAll(previousMapping.keySet());
		skipForward.removeIf(x -> !x.properties.get(frame).inside(previousMapping.get(x).floatValue(), currentMapping.get(x).floatValue()));

//		Stream<Box> skipForward = population().filter(x -> x.properties.get(frame)
//			.inside(was.x, now.x));


//		Stream<Box> skipBackward = population().filter(x -> x.properties.get(frame).inside(now.x, was.x));
		this.currentMapping = currentMapping;
		this.currentMappingAtTime = now.x;

		off(off);
		on(on, currentMapping);
		skipForward(skipForward, currentMapping);
//		skipBackward(skipBackward);


	}

	protected void off(Set<Box> off) {
		if (disable) return;
		List<Box> son = new ArrayList<>(off);
		son.sort(Comparator.comparingDouble(a -> a.properties.get(frame).y));
		son.sort(Comparator.comparingDouble(a -> a.properties.get(frame).x+a.properties.get(frame).w));

		son.forEach(b -> {
			Log.log("debug.execution", () -> " -- END :" + b);
			if (b != null)
				b.first(Execution.execution).ifPresent(x -> x.support(b, Execution.code).end(b));
		});
	}

	protected void on(Set<Box> on, Map<Box, Double> mapping) {
		if (disable) return;

		List<Box> son = new ArrayList<>(on);
		son.sort(Comparator.comparingDouble(a -> a.properties.get(frame).y));
		son.sort(Comparator.comparingDouble(a -> a.properties.get(frame).x));

		son.forEach(b -> {
			Log.log("debug.execution", () -> " -- BEGIN :" + b);
			if (b != null)
				b.first(Execution.execution).ifPresent(x -> x.support(b, Execution.code).begin(b, initiator(b, mapping.get(b))));
		});
	}

	/**
	 * returns the list of boxes that this Time Slider currently intersects (and thus interacts) with
	 *
	 * @return
	 */
	public List<Box> intersectsWith() {
		Rect now = properties.get(frame);
		List<Box> nx = population().filter(x -> !x.properties.isTrue(Chorder.nox, false))
			.filter(x -> x.properties.get(frame)
				.intersectsX(now.x))
			.collect(Collectors.toList());

		return nx;
	}

	/**
	 * returns the list of boxes that this Time Slider could intersect with at time 't'
	 *
	 * @return
	 */
	public List<Box> intersectsWith(double t) {
		List<Box> nx = population().filter(x -> !x.properties.isTrue(Chorder.nox, false))
			.filter(x -> x.properties.get(frame)
				.intersectsX(t))
			.collect(Collectors.toList());

		return nx;
	}


	/**
	 * builds the initiator object for this "begin" call. This can be used to get at the object that caused this "animation" to begin.
	 *
	 * @param b
	 */
	public Map<String, Object> initiator(Box b, double t) {
		fieldlinker.AsMap init = Initiators.get(b, () -> {
			Double mapped = currentMapping.get(b);
			if (mapped==null)
			{
				//this happens when a box is overrun
				// t is the time when it was initiated not the current time
				return currentMappingAtTime;
			}
			return mapped.floatValue();
		}, () -> this.properties.get(Box.frame).y);
		init.asMap_set("slider", this);
		return Collections.singletonMap("_t", init);
	}

	/**
	 * begins a box, given this slider. This is a lot like `_.begin()` except this slider is passed in as the reason that this box is running. In particular `_t()` works as expected and is tied to the position of this slider.
	 */
	public void beginBox(Box b) {
		b.first(Execution.execution).ifPresent(x -> x.support(b, Execution.code).begin(b, initiator(b, this.properties.get(frame).x)));
	}

	/**
	 * by default things that we skip over backwards we _do_ run (and then immediately stop).
	 */
	protected void skipForward(Set<Box> skipForward, Map<Box, Double> times) {
		if (disable) return;

		List<Box> son = new ArrayList<>(skipForward);
		son.sort(Comparator.comparingDouble(a -> a.properties.get(frame).y));
		son.sort(Comparator.comparingDouble(a -> a.properties.get(frame).x));


		son.forEach(b -> {
			Log.log("debug.execution", () -> " -- FORWARD :" + b);
			b.first(Execution.execution).ifPresent(x -> x.support(b, Execution.code).begin(b, initiator(b, times.get(b))));
			b.first(Execution.execution).ifPresent(x -> x.support(b, Execution.code).end(b));
		});
	}

	/**
	 * by default things that we skip over backwards we _do not_ run
	 */
	protected void skipBackward(Stream<Box> skipBackward) {
		skipBackward.forEach(b -> {
			Log.log("debug.execution", () -> " -- backward :" + b);
//			b.first(Execution.execution).ifPresent(x -> x.support(b, Execution.code).begin(b, initiator(b)));
//			b.first(Execution.execution).ifPresent(x -> x.support(b, Execution.code).end(b));
		});

	}

	/**
	 * returns a Stream of potential boxes that we can execute. Subclass to constrain further, but by default it's all of the children of this
	 * timeslider's first parent that have Manipulation.frames that aren't this box --- i.e. all the siblings of this box
	 */
	protected Stream<Box> population() {
		return parents().iterator()
			.next()
			.breadthFirst(this.downwards())
			.filter(x -> !x.properties.isTrue(Chorder.nox,
				x.properties.has(Drawing.windowSpace) ||
					x.properties.has(Drawing.windowScale)))
			.filter(x -> Planes.on(parents().iterator().next(), x) >= 0.5)
			.filter(x -> x.properties.has(frame))
			.filter(x -> x != this);
	}

	protected Map<String, Function<Box, FLine>> defaultdrawsLines(Dict.Prop<Map<String, Function<Box, FLine>>> k) {
		Map<String, Function<Box, FLine>> r = new LinkedHashMap<>();

		r.put("__outline__", new Cached<Box, Object, FLine>((box, previously) -> {
			Rect rect = box.properties.get(frame);
			if (rect == null) return null;

			boolean selected = box.properties.isTrue(Mouse.isSelected, false);
			boolean manipulated = box.properties.isTrue(Mouse.isManipulated, false);

			FLine f = new FLine();
			rect.inset(-0.5f);

			f.moveTo(rect.x, rect.y);
			f.lineTo(rect.x, rect.y + rect.h);

			f.attributes.put(strokeColor,
				selected ? new Vec4(1, 0.3, 0.2, -1.0f) : new Vec4(0.85f, manipulated ? 0.5f : 0, 0, 0.5f));

			f.attributes.put(thicken,
				new BasicStroke(selected ? 2.5f : 2.5f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));

			f.attributes.put(stroked, true);

			return f;
		}, (box) -> new Triple(box.properties.get(frame), box.properties.get(Mouse.isSelected),
			box.properties.get(Mouse.isManipulated))));

		r.put("__localtime__", new Cached<Box, Object, FLine>((box, previously) -> {

			if (currentMapping == null) return new FLine();
			List<FLine> ff = LocalTime.Companion.drawTimesFor(currentMapping, currentMappingAtTime);

			FLine container = new FLine();
			IdempotencyMap<Supplier<FLine>> sl = container.attributes.getOrConstruct(subLines);
			sl.addAll(ff);

			return container;

		}, (box) -> new Triple(box.properties.get(frame), box.properties.get(Mouse.isSelected), box.properties.get(Mouse.isManipulated))));

		r.put("__outlineFill__", new Cached<Box, Object, FLine>((box, previously) -> {
			Rect rect = box.properties.get(frame);
			if (rect == null) return null;

			boolean selected = box.properties.isTrue(Mouse.isSelected, false);

			float a = selected ? 0.9f / 3 : 0.8f / 3;
			float b = selected ? 0.75f / 3 : 0.75f / 3;
			float s = selected ? -0.25f : 0.1f;

			a = 1;
			b = 0.88f;

			FLine f = new FLine();
			f.moveTo(rect.x, rect.y);
			f.nodes.get(f.nodes.size() - 1).attributes.put(color, new Vec4(a, 0.1, 0, s));
			f.lineTo(rect.x + rect.w, rect.y);
			f.nodes.get(f.nodes.size() - 1).attributes.put(color, new Vec4(b, 0.1, 0.1, s));
			f.lineTo(rect.x + rect.w, rect.y + rect.h);
			f.nodes.get(f.nodes.size() - 1).attributes.put(color, new Vec4(a, 0.1, 0, s));
			f.lineTo(rect.x, rect.y + rect.h);
			f.nodes.get(f.nodes.size() - 1).attributes.put(color, new Vec4(a, 0.1, 0, s));
			f.lineTo(rect.x, rect.y);
			f.nodes.get(f.nodes.size() - 1).attributes.put(color, new Vec4(a, 0.1, 0.1, s));

			f.attributes.put(filled, true);


			return f;
		}, (box) -> new Triple(box.properties.get(frame), box.properties.get(Mouse.isSelected),
			box.properties.get(Mouse.isManipulated))));


		return r;
	}


	/* localtime will complicate this */
	public double getTime(Box box) {
		return this.properties.get(Box.frame).x;
	}
}
