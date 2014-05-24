package fieldbox.boxes;

import field.graphics.FLine;
import field.linalg.Vec4;
import field.utility.Cached;
import field.utility.Dict;
import field.utility.Pair;
import field.utility.Rect;
import fielded.Execution;

import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * A TimeSlider is a (thin) box that executes (that is calls .begin() and .end() on) other boxes that it passes over when dragged horizontally
 * around.
 * <p>
 * This has been built for "subclassability". Change the drawing by changing this::defaultdrawsLines, change the population of elements that can be
 * executed by changing this::population, change the behavior by changing this::off, this::on, this::skipForward, this::skipBackward.
 */
public class TimeSlider extends Box {

	Rect was = null;

	int width = 20;

	public TimeSlider() {
		this.properties.putToMap(Boxes.insideRunLoop, "__swipe__", this::swiper);
		this.properties.putToMap(Boxes.insideRunLoop, "__force_onscreen__", this::forceOnscreen);

		properties.put(frame, new Rect(0, 0, width, 5000));


		this.properties.put(FrameManipulation.lockWidth, true);
		this.properties.put(FrameManipulation.lockHeight, true);
		this.properties.put(Boxes.dontSave, true);
		this.properties.put(Box.name, "TimeSlider");

		this.properties.computeIfAbsent(FLineDrawing.frameDrawing, this::defaultdrawsLines);
	}

	protected boolean swiper() {

		if (was == null) {
			was = this.properties.get(frame);
		} else {
			Rect now = this.properties.get(frame);
			if (now.x == was.w) {

			} else {
				perform(was, now);
			}
			was = now;
		}

		return true;
	}

	protected boolean forceOnscreen() {
		Rect f = properties.get(frame);

		Rect w = f.inset(0);

		Drawing d = first(Drawing.drawing).orElse(null);
		if (d != null) {
			Rect viewBounds = d.getCurrentViewBounds(TimeSlider.this);
//				System.out.println(" current view bounds are :" + viewBounds);

			w.y = viewBounds.y - width;
			w.h = viewBounds.h + width * 2;
			w.w = width;
		}

		if (!w.equals(f)) {
//				System.out.println(" new frame is :"+w);
			properties.put(frame, w);
			Drawing.dirty(TimeSlider.this);
		} else {
//				System.out.println(" no frame change :"+w+" "+f);
		}

		return true;
	}

	protected void perform(Rect was, Rect now) {

		Stream<Box> off = population().filter(x -> x.properties.get(frame).intersectsX(was.x))
			    .filter(x -> !x.properties.get(frame).intersectsX(now.x));
		Stream<Box> on = population().filter(x -> !x.properties.get(frame).intersectsX(was.x))
			    .filter(x -> x.properties.get(frame).intersectsX(now.x));
		Stream<Box> skipForward = population().filter(x -> x.properties.get(frame).inside(was.x, now.x));
		Stream<Box> skipBackward = population().filter(x -> x.properties.get(frame).inside(now.x, was.x));

		off(off);
		on(on);
		skipForward(skipForward);
		skipBackward(skipBackward);

	}

	protected void off(Stream<Box> off) {
		off.forEach(b -> {
//			System.out.println(" -- END :"+b);
			b.first(Execution.execution).ifPresent(x -> x.support(b, Execution.code).end(b));
		});
	}

	protected void on(Stream<Box> on) {
		on.forEach(b -> {
//			System.out.println(" -- BEGIN :"+b);
			b.first(Execution.execution).ifPresent(x -> x.support(b, Execution.code).begin(b));
		});

	}

	/**
	 * by default things that we skip over backwards we _do_ run (and then immediately stop).
	 */
	protected void skipForward(Stream<Box> skipForward) {
		skipForward.forEach(b -> {
//			System.out.println(" -- FORWARD :"+b);
			b.first(Execution.execution).ifPresent(x -> x.support(b, Execution.code).begin(b));
			b.first(Execution.execution).ifPresent(x -> x.support(b, Execution.code).end(b));
		});
	}

	/**
	 * by default things that we skip over backwards we _do not_ run
	 */
	protected void skipBackward(Stream<Box> skipBackward) {
		skipBackward.forEach(b -> {
//			System.out.println(" -- backward :"+b);
			b.first(Execution.execution).ifPresent(x -> x.support(b, Execution.code).begin(b));
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

			FLine f = new FLine();
			rect.inset(-0.5f);

			f.moveTo(rect.x, rect.y);
			f.lineTo(rect.x, rect.y + rect.h);

			f.attributes.put(FLineDrawing.strokeColor, selected ? new Vec4(1, 0, 0, -1.0f) : new Vec4(0.5f, 0, 0, 0.5f));

			f.attributes.put(FLineDrawing.thicken, new BasicStroke(selected ? 2.5f : 2.5f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));

			f.attributes.put(FLineDrawing.stroked, true);

			return f;
		}, (box) -> new Pair(box.properties.get(frame), box.properties.get(Mouse.isSelected))));

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
			f.nodes.get(f.nodes.size() - 1).attributes.put(FLineDrawing.fillColor, new Vec4(a, 0, 0, s));
			f.lineTo(rect.x + rect.w, rect.y);
			f.nodes.get(f.nodes.size() - 1).attributes.put(FLineDrawing.fillColor, new Vec4(b, 0, 0, s));
			f.lineTo(rect.x + rect.w, rect.y + rect.h);
			f.nodes.get(f.nodes.size() - 1).attributes.put(FLineDrawing.fillColor, new Vec4(a, 0, 0, s));
			f.lineTo(rect.x, rect.y + rect.h);
			f.nodes.get(f.nodes.size() - 1).attributes.put(FLineDrawing.fillColor, new Vec4(a, 0, 0, s));
			f.lineTo(rect.x, rect.y);
			f.nodes.get(f.nodes.size() - 1).attributes.put(FLineDrawing.fillColor, new Vec4(a, 0, 0, s));

			f.attributes.put(FLineDrawing.filled, true);

			Map<Integer, String> customFill = new LinkedHashMap<Integer, String>();
			customFill.put(1, "fillColor");
			f.setAuxProperties(customFill);


			return f;
		}, (box) -> new Pair(box.properties.get(frame), box.properties.get(Mouse.isSelected))));


		return r;
	}


}
