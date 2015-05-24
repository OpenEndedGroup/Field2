package fieldbox.boxes;

import field.graphics.FLine;
import field.linalg.Vec4;
import field.utility.*;
import fieldbox.boxes.plugins.Initiators;
import fieldbox.execution.Execution;
import fieldlinker.Linker;

import java.awt.*;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
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

	static public final Dict.Prop<TimeSlider> time = new Dict.Prop<>("time").toCannon().doc("the defualt red-line time slider. Set _.time.frame.x = something to move it around.");

	Rect was = null;

	int width = 20;

	public TimeSlider() {
		this.properties.putToMap(Boxes.insideRunLoop, "main.__swipe__", this::swiper);
		this.properties.putToMap(Boxes.insideRunLoop, "main.__force_onscreen__", this::forceOnscreen);

		properties.put(frame, new Rect(0, 0, width, 5000));


		this.properties.put(FrameManipulation.lockWidth, true);
		this.properties.put(FrameManipulation.lockHeight, true);
		this.properties.put(Boxes.dontSave, true);
		this.properties.put(Box.name, "TimeSlider");

		this.properties.computeIfAbsent(frameDrawing, this::defaultdrawsLines);
	}

	protected boolean swiper() {

		if (was == null) {
			was = this.properties.get(frame).duplicate();
		} else {
			Rect now = this.properties.get(frame);
			if (now.x == was.w) {

			} else {
				perform(was, now);
			}
			was = now.duplicate();
		}

		return true;
	}

	protected boolean forceOnscreen() {
		Rect f = properties.get(frame);

		Rect w = f.inset(0);

		Drawing d = first(Drawing.drawing).orElse(null);

		float safety = 500;

		Rect viewBounds = d.getCurrentViewBounds(TimeSlider.this);

		if (d != null) {
			w.y = viewBounds.y - width-safety;
			w.h = viewBounds.h + width * 2+safety*2;
			w.w = width;
		}

		// this check stops us from having to blow the MeshBuilder cache on every vertical scroll...
		if (!w.equals(f) && (f.y>viewBounds.y || f.y+f.h<viewBounds.y+viewBounds.h))
		{
			properties.put(frame, w);
			Drawing.dirty(TimeSlider.this);
		} else {
		}

		return true;
	}

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

	protected void off(Stream<Box> off) {
		off.forEach(b -> {
			Log.log("debug.execution", " -- END :" + b);
			if (b!=null)
			b.first(Execution.execution).ifPresent(x -> x.support(b, Execution.code).end(b));
		});
	}

	protected void on(Stream<Box> on) {
		on.forEach(b -> {
			Log.log("debug.execution", " -- BEGIN :"+b);
			if (b!=null)
			b.first(Execution.execution).ifPresent(x -> x.support(b, Execution.code).begin(b, initiator(b)));
		});
	}

	public List<Box> intersectsWith()
	{
		Rect now = properties.get(frame);
		return population()
			    .filter(x -> x.properties.get(frame)
						      .intersectsX(now.x)).collect(Collectors.toList());
	}

	/**
	 * builds the initiator object for this "begin" call. This can be used to get at the object that caused this "animation" to begin.
	 * @param b
	 */
	protected Map<String, Object> initiator(Box b) {
		Linker.AsMap init = Initiators.get(b, () -> this.properties.get(Box.frame).x, () -> this.properties.get(Box.frame).y);
		init.asMap_set("slider", this);
		return Collections.singletonMap("_t", init);
	}

	/**
	 * by default things that we skip over backwards we _do_ run (and then immediately stop).
	 */
	protected void skipForward(Stream<Box> skipForward) {
		skipForward.forEach(b -> {
			Log.log("debug.execution", " -- FORWARD :"+b);
			b.first(Execution.execution).ifPresent(x -> x.support(b, Execution.code).begin(b, initiator(b)));
			b.first(Execution.execution).ifPresent(x -> x.support(b, Execution.code).end(b));
		});
	}

	/**
	 * by default things that we skip over backwards we _do not_ run
	 */
	protected void skipBackward(Stream<Box> skipBackward) {
		skipBackward.forEach(b -> {
			Log.log("debug.execution", " -- backward :"+b);
			b.first(Execution.execution).ifPresent(x -> x.support(b, Execution.code).begin(b, initiator(b)));
			b.first(Execution.execution).ifPresent(x -> x.support(b, Execution.code).end(b));
		});

	}

	/**
	 * returns a Stream of potential boxes that we can execute. Subclass to constrain further, but by default it's all of the children of this
	 * timeslider's first parent that have Manipulation.frames that aren't this box --- i.e. all the siblings of this box
	 */
	protected Stream<Box> population() {
		return parents().iterator().next().breadthFirst(this.downwards()).filter(x -> x.properties.has(frame)).filter(x -> x != this);
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

			f.attributes.put(strokeColor, selected ? new Vec4(1, 0, 0, -1.0f) : new Vec4(0.5f, manipulated ? 0.5f: 0, 0, 0.5f));

			f.attributes.put(thicken, new BasicStroke(selected ? 2.5f : 2.5f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));

			f.attributes.put(stroked, true);

			return f;
		}, (box) -> new Triple(box.properties.get(frame), box.properties.get(Mouse.isSelected), box.properties.get(Mouse.isManipulated))));

		r.put("__outlineFill__", new Cached<Box, Object, FLine>((box, previously) -> {
			Rect rect = box.properties.get(frame);
			if (rect == null) return null;

			boolean selected = box.properties.isTrue(Mouse.isSelected, false);

			float a = selected ? 0.9f / 3 : 0.8f / 3;
			float b = selected ? 0.75f / 3 : 0.75f / 3;
			float s = selected ? -0.25f : 0.06f;

			a = 1;
			b = 0.88f;

			FLine f = new FLine();
			f.moveTo(rect.x, rect.y);
			f.nodes.get(f.nodes.size() - 1).attributes.put(color, new Vec4(a, 0, 0, s));
			f.lineTo(rect.x + rect.w, rect.y);
			f.nodes.get(f.nodes.size() - 1).attributes.put(color, new Vec4(b, 0, 0, s));
			f.lineTo(rect.x + rect.w, rect.y + rect.h);
			f.nodes.get(f.nodes.size() - 1).attributes.put(color, new Vec4(a, 0, 0, s));
			f.lineTo(rect.x, rect.y + rect.h);
			f.nodes.get(f.nodes.size() - 1).attributes.put(color, new Vec4(a, 0, 0, s));
			f.lineTo(rect.x, rect.y);
			f.nodes.get(f.nodes.size() - 1).attributes.put(color, new Vec4(a, 0, 0, s));

			f.attributes.put(filled, true);


			return f;
		}, (box) -> new Triple(box.properties.get(frame), box.properties.get(Mouse.isSelected), box.properties.get(Mouse.isManipulated))));


		return r;
	}


}
