package fieldbox.boxes.plugins;

import field.app.RunLoop;
import field.graphics.FLine;
import field.graphics.Window;
import field.linalg.Vec2;
import field.linalg.Vec4;
import field.utility.Dict;
import field.utility.Pair;
import field.utility.Rect;
import field.utility.Triple;
import fieldbox.boxes.Box;
import fieldbox.boxes.Drawing;
import fieldbox.boxes.MarkingMenus;
import fieldbox.boxes.Mouse;
import fieldbox.execution.Execution;
import fielded.RemoteEditor;
import fieldlinker.Linker;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static field.graphics.StandardFLineDrawing.*;
import static fieldbox.boxes.FLineDrawing.*;

/**
 * Created by marc on 4/16/14.
 */
public class Chorder extends Box {

	static public final Dict.Prop<FunctionOfBox<Supplier<Boolean>>> begin = new Dict.Prop<>(
		"begin").type()
			.toCanon()
			.doc("call <code>.begin()</code> to add a box to the animation cycle, ending any animation that this box is currently performing. Returns a function that tells you whether this box is still running.");

	static public final Dict.Prop<FunctionOfBox> end = new Dict.Prop<>("end").type()
										 .toCanon()
										 .doc("call <code>.end()</code> to remove a box from the animation cycle.");


	static public final Dict.Prop<FunctionOfBox<Runnable>> beginAgain = new Dict.Prop<>(
		"beginAgain").type()
			     .toCanon()
			     .doc("call <code>.beginAgain()</code> to add a box to the animation cycle. Returns a function that, when you call it, will forcibly _.end() just that execution (while, potentially, other executions continue to run).");

	static public final Dict.Prop<Boolean> nox = new Dict.Prop<>("_nox").type()
									    .toCanon()
									    .doc("'NO eXecution. Set to prevent option-clicking from running this box");


	public Chorder(Box root_unused) {

		properties.put(Planes.plane, "__always__");

		properties.putToMap(Mouse.onMouseDown, "__chorder__", (e, button) -> {
			if (button != 0) return null;
			if (!e.after.keyboardState.isAltDown()) return null;
			if (e.after.keyboardState.isShiftDown()) return null;
			if (e.after.keyboardState.isSuperDown()) return null;
			if (e.after.keyboardState.isControlDown()) return null;

			Optional<Drawing> drawing = this.find(Drawing.drawing, both())
							.findFirst();
			Vec2 point = new Vec2(e.after.mx, e.after.my);

			Stream<Pair<Box, Rect>> fr = breadthFirst(both()).map(b -> new Pair<>(b, frame(b)))
									 .filter(b -> !b.first.properties.isTrue(nox, false))
									 .filter(b -> b.second != null);

			List<Pair<Box, Rect>> frames = fr.collect(Collectors.toList());

			Optional<Pair<Box, Rect>> hit = frames.stream()
							      .filter(b -> !b.first.properties.isTrue(Box.hidden, false))
							      .filter(b -> b.second.intersects(point))
							      .filter(b -> !b.first.properties.isTrue(nox, false))
							      .sorted((a, b) -> Float.compare(order(a.second), order(b.second)))
							      .findFirst();

			if (hit.isPresent()) {
				return executeNowAt(e, point, hit.get().first);
			}

			// we have an execution chord

			return chordAt(frames, point, e);
		});

		this.properties.put(begin,  box -> {
			box.first(Execution.execution)
			   .ifPresent(x -> x.support(box, Execution.code)
					    .begin(box, getInitiator(box)));
			return () -> box.properties.computeIfAbsent(IsExecuting.executionCount, (k) -> 0) > 0;
		});
		this.properties.put(beginAgain, box -> {
			Optional<String> name = box.first(Execution.execution).map(x -> x.support(box, Execution.code)
											 .begin(box, getInitiator(box), false));
			if (name.isPresent()) {
				Function<Box, Consumer<Pair<Integer, String>>> ef = box.first(RemoteEditor.outputErrorFactory)
										       .orElse((x) -> (is -> System.err.println("error (without remote editor attached) :" + is)));
				Function<Box, Consumer<String>> of = box.first(RemoteEditor.outputFactory)
									.orElse(x -> (is -> System.out.println("output (without remote editor attached) :" + is)));

				return () -> {
					box.first(Execution.execution).ifPresent(x -> x.support(box, Execution.code).end(name.get(), ef.apply(box), of.apply(box)));
				};
			} else {
				return null;
			}
		});
		this.properties.put(end, box -> {
			((Box)box).first(Execution.execution)
			   .ifPresent(x -> x.support(((Box)box), Execution.code)
					    .end(((Box)box)));
			return null;
		});
	}

	private Mouse.Dragger executeNowAt(Window.Event<Window.MouseState> e, Vec2 point, Box box) {
		e.properties.put(Window.consumed, true);

		if (properties.get(frameDrawing) != null) // we only add decoration to things that are drawn
			properties.putToMap(frameDrawing, "__feedback__chorderbox", expires(b -> {

				FLine f = new FLine();
				Rect fr = frame(box);
				int i = 0;
				f.rect(fr.x + i, fr.y + i, fr.w - i * 2, fr.h - i * 2);

				f.depthTo(box);
				f.attributes.put(strokeColor, new Vec4(0.5f, 0.75f, 0.5f, -0.5f));
				f.attributes.put(thicken, new BasicStroke(10.5f));
				f.attributes.put(layer, "__main__.fast");

				return f;

			}, 50));

		int count0 = box.properties.computeIfAbsent(IsExecuting.executionCount, (k) -> 0);

		if (count0 < 1) {

			fieldlinker.AsMap i = Initiators.get(box, Initiators.mouseX(box, point.x), Initiators.mouseY(box, point.y));

			box.first(Execution.execution)
			   .ifPresent(x -> x.support(box, Execution.code)
					    .begin(box, Collections.singletonMap("_t", i)));
			// with remote back ends it's possible we'll have to defer this to the next update cycle to give them a chance to acknowledge that were actually executing
			int count1 = box.properties.computeIfAbsent(IsExecuting.executionCount,
				(k) -> 0);

			if (count1 > count0) {
				MarkingMenus.MenuSpecification menuSpec
					= new MarkingMenus.MenuSpecification();
				menuSpec.items.put(MarkingMenus.Position.NH,
					new MarkingMenus.MenuItem("Continue", () -> {
					}));
				menuSpec.nothing = () -> {
					box.first(Execution.execution)
					   .ifPresent(x -> x.support(box, Execution.code)
							    .end(box));
					Drawing.dirty(this, 3);
				};
				return MarkingMenus.runMenu(box, point, menuSpec);
			}
		} else {
			MarkingMenus.MenuSpecification menuSpec
				= new MarkingMenus.MenuSpecification();
			menuSpec.items.put(MarkingMenus.Position.SH, new MarkingMenus.MenuItem("Stop", () -> {
				box.first(Execution.execution)
				   .ifPresent(x -> x.support(box, Execution.code)
						    .end(box));
				Drawing.dirty(this, 3);
			}));
			return MarkingMenus.runMenu(box, point, menuSpec);
		}

		return null;
	}

	private Mouse.Dragger chordAt(List<Pair<Box, Rect>> frames, Vec2 start, Window.Event<Window.MouseState> initiation) {

		boolean[] once = {false};


		this.properties.putToMap(StatusBar.statuses, "__chordfeedback__",
			() -> "Drag to start multiple boxes");

		return (e, end) -> {

			Vec2 point = new Vec2(e.after.mx, e.after.my);
			chordOver(frames, start, point, end);

			if (end && !once[0]) {

				List<Triple<Vec2, Float, Box>> intersections = intersectionsFor(
					frames, start, point);

				intersections = intersections.stream().filter(x -> x != null).filter(x -> x.third != null).sorted((a, b) -> Double.compare(a.second, b.second)).collect(Collectors.toList());

				once[0] = true;
				for (int i = 0; i < intersections.size(); i++) {
					if (intersections.get(i) == null) continue;

					Box b = intersections.get(i).third;

					Vec2 p = intersections.get(i).first;

					fieldlinker.AsMap in = Initiators.get(b, () -> p.x, () -> p.y);

					b.first(Execution.execution)
					 .ifPresent(x -> x.support(b, Execution.code)
							  .begin(b, Collections.singletonMap("_t", in)));
				}

				long count = intersections.stream()
							  .filter(x -> x != null)
							  .count();

				this.properties.putToMap(StatusBar.statuses, "__chordfeedback__",
					() -> "Drag started " + (count == 0 ? "" : (count == 1 ? "this " : "these ") + count + " box" + (count == 1 ? "" : "es")));

				RunLoop.main.delay(
					() -> this.properties.removeFromMap(StatusBar.statuses,
						"__chordfeedback__"),
					1000);

			}

			return !end;
		};

	}

	protected float order(Rect r) {
		return Math.abs(r.w) + Math.abs(r.h);
	}

	private void chordOver(List<Pair<Box, Rect>> frames, Vec2 start, Vec2 end, boolean termination) {


		properties.putToMap(frameDrawing, "__feedback__chorder", expires(box -> {
			FLine f = new FLine();
			f.moveTo(start.x, start.y, 0);
			f.lineTo(end.x, end.y, 0);
			f.attributes.put(color, new Vec4(0.5f, 0.95f, 0.6f, 0.15f));
			f.attributes.put(thicken, new BasicStroke(3.5f));
			f.attributes.put(layer, "__main__.fast");

			return f;
		}, termination ? 50 : -1));
		properties.putToMap(frameDrawing, "__feedback__chorderC", expires(box -> {
			FLine f = new FLine();
			f.moveTo(start.x, start.y, 0);
			f.lineTo(end.x, end.y, 0);
			f.attributes.put(color, new Vec4(0.5f, 0.95f, 0.6f, 0.5f));
			f.attributes.put(thicken, new BasicStroke(1.5f));
			f.attributes.put(layer, "__main__.fast");

			return f;
		}, termination ? 50 : -1));

		List<Triple<Vec2, Float, Box>> i = intersectionsFor(frames, start, end);

		long count = i.stream()
			      .filter(x -> x != null)
			      .count();
		if (count > 0) this.properties.putToMap(StatusBar.statuses, "__chordfeedback__",
			() -> "Release to start " + (count == 0 ? "" : (count == 1 ? "this " : "these ") + count + " box" + (count == 1 ? "" : "es")));
		else this.properties.putToMap(StatusBar.statuses, "__chordfeedback__",
			() -> "Drag to start multiple boxes");

		properties.putToMap(frameDrawing, "__feedback__chorderbox", expires(box -> {

			FLine f = new FLine();

			i.stream()
			 .filter(x -> x != null)
			 .forEach((x) -> {

				 Rect fr = frame(x.third);

				 f.rect(fr.x, fr.y, fr.w, fr.h).depthTo(x.third);

			 });

			f.attributes.put(strokeColor, new Vec4(0.5f, 0.75f, 0.5f, -0.5f));
			f.attributes.put(thicken, new BasicStroke(10.5f));
			f.attributes.put(layer, "__main__.fast");

			return f;

		}, termination ? 50 : -1));
		properties.putToMap(frameDrawing, "__feedback__chorderbox2", expires(box -> {

			FLine f = new FLine();

			i.stream()
			 .filter(x -> x != null)
			 .forEach((x) -> {

				 Rect fr = frame(x.third);

				 float w = 8;
				 f.rect(x.first.x - w, x.first.y - w, w * 2, w * 2).depthTo(x.third);

			 });

			f.attributes.put(color, new Vec4(0.5f, 0.75f, 0.5f, -0.75f));
			f.attributes.put(filled, true);
			f.attributes.put(stroked, false);
			f.attributes.put(layer, "__main__.fast");
			return f;

		}, termination ? 50 : -1));

		properties.putToMap(frameDrawing, "__feedback__chorderbox3", expires(box -> {

			FLine f = new FLine();

			int[] counter = {1};

			Vec2 delta = new Vec2(end.y - start.y, start.x - end.x);
			delta.normalize();
			i.stream()
			 .filter(x -> x != null)
			 .forEach((x) -> {

				 f.moveTo(x.first.x + delta.x * 12, x.first.y + delta.y * 12, x.third.properties.getFloat(depth, 0f));
				 f.nodes.get(f.nodes.size() - 1).attributes.put(text,
					 " " + (counter[0]++));
			 });

			f.attributes.put(color, new Vec4(0.1f, 0.25f, 0.1f, 0.75f));
			f.attributes.put(hasText, true);
			f.attributes.put(layer, "__main__.fast");
			return f;

		}, termination ? 50 : -1));


		Drawing.dirty(this);

	}

	protected Rect frame(Box hitBox) {
		return hitBox.properties.get(frame);
	}


	public List<Triple<Vec2, Float, Box>> intersectionsFor(List<Pair<Box, Rect>> frames, Vec2 start, Vec2 end) {
		List<Triple<Vec2, Float, Box>> ret = new ArrayList<>();

		for (Pair<Box, Rect> br : frames) {
			Rect r = br.second;

			Vec2 v1 = getLineIntersection(new Vec2(r.x, r.y), new Vec2(r.x + r.w, r.y),
				start, end);
			Vec2 v2 = getLineIntersection(new Vec2(r.x, r.y + r.h),
				new Vec2(r.x + r.w, r.y + r.h), start, end);
			Vec2 v3 = getLineIntersection(new Vec2(r.x, r.y + r.h), new Vec2(r.x, r.y),
				start, end);
			Vec2 v4 = getLineIntersection(new Vec2(r.x + r.w, r.y),
				new Vec2(r.x + r.w, r.y + r.h), start, end);
			List<Vec2> al = Arrays.asList(v1, v2, v3, v4);
			Collections.sort(al, (a, b) -> {
				if (a == null) return b == null ? 0 : 1;
				if (b == null) return -1;
				return Double.compare(a.distance(start), b.distance(start));
			});

			if (al.get(0) == null) {
				ret.add(null);
				continue;
			}

			ret.add(new Triple<>(al.get(0), (float) al.get(0)
								  .distance(start), br.first));
		}

		return ret;
	}

	public Vec2 getLineIntersection(Vec2 p0, Vec2 p1, Vec2 p2, Vec2 p3) {
		double s02_x, s02_y, s10_x, s10_y, s32_x, s32_y, s_numer, t_numer, denom, t;
		s10_x = p1.x - p0.x;
		s10_y = p1.y - p0.y;
		s32_x = p3.x - p2.x;
		s32_y = p3.y - p2.y;

		denom = s10_x * s32_y - s32_x * s10_y;
		if (denom == 0) return null;

		boolean denomPositive = denom > 0;

		s02_x = p0.x - p2.x;
		s02_y = p0.y - p2.y;
		s_numer = s10_x * s02_y - s10_y * s02_x;
		if ((s_numer < 0) == denomPositive) return null;

		t_numer = s32_x * s02_y - s32_y * s02_x;
		if ((t_numer < 0) == denomPositive) return null;

		if (((s_numer > denom) == denomPositive) || ((t_numer > denom) == denomPositive))
			return null;

		t = t_numer / denom;
		return new Vec2(p0.x + (t * s10_x), p0.y + (t * s10_y));
	}

	public Map<String, Object> getInitiator(Box forBox) {
		return Collections.singletonMap("_t", Initiators.constant(forBox, 0.5));
	}
}
