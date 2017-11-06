package fieldbox.boxes;

import field.graphics.FLine;
import field.graphics.StandardFLineDrawing;
import field.graphics.Window;
import field.linalg.Vec2;
import field.linalg.Vec4;
import field.utility.Dict;
import field.utility.Log;
import field.utility.Rect;
import field.utility.Triple;
import fieldbox.boxes.plugins.Planes;
import fieldbox.boxes.plugins.Scrolling;
import fieldbox.boxes.plugins.SimpleSnapHelper;
import fieldbox.ui.Cursors;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static fieldbox.boxes.plugins.Planes.plane;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_TAB;

/**
 * Plugin: Adds the ability to drag boxes around with the mouse and change their sizes with their edges. Provides textual feedback when this happens.
 * <p>
 * Also addes the ability to translate the canvas around with the middle mouse button (button 2).
 */
public class FrameManipulation extends Box {

	static public final Dict.Prop<Boolean> lockWidth = new Dict.Prop<>("lockWidth").type()
		.toCanon()
		.doc("set to true to disable changes to the width of this box via the mouse");
	static public final Dict.Prop<Boolean> lockHeight = new Dict.Prop<>("lockHeight").type()
		.toCanon()
		.doc("set to true to disable changes to the height of this box via the mouse");
	static public final Dict.Prop<Boolean> lockX = new Dict.Prop<>("lockX").type()
		.toCanon()
		.doc("set to true to disable changes to the x-position of this box via the mouse");
	static public final Dict.Prop<Boolean> lockY = new Dict.Prop<>("lockY").type()
		.toCanon()
		.doc("set to true to disable changes to the y-position of this box via the mouse");

	static public final Dict.Prop<FunctionOfBoxValued<List<Box>>> selection = new Dict.Prop<>("selection").toCanon()
		.type()
		.doc("the list of boxes that are selected");

	private final Box root;

	SimpleSnapHelper helper;
	List<FLine> extraAnnotations = Collections.emptyList();

	public FrameManipulation(Box root) {
		this.root = root;
		this.helper = new SimpleSnapHelper(root, 10);
		this.properties.put(plane, "__always__");
		this.properties.put(selection, x -> breadthFirst(both()).filter(q -> (q.properties.isTrue(Mouse.isSelected, false) && !q.properties.isTrue(Mouse.isSticky, false)))
			.collect(Collectors.toList()));
		this.properties.putToMap(Mouse.onMouseDown, "__frameManipulation__", this::onMouseDown);
		this.properties.putToMap(Mouse.onMouseMove, "__frameManipulation__", e -> {

			Optional<Drawing> drawing = this.find(Drawing.drawing, both())
				.findFirst();
			Vec2 point = new Vec2(e.after.mx, e.after.my);

			Optional<Box> hit = breadthFirst(both()).filter(b -> frame(b) != null)
				.filter(b -> !b.properties.isTrue(Box.hidden, false))
				.filter(b -> Planes.on(root, b) >= 0.5f)
				.filter(b -> frame(b).intersects(point))
				.sorted((a, b) -> Float.compare(order(frame(a)), order(frame(b))))
				.findFirst();

			if (hit.isPresent()) {
				Box hitBox = hit.get();
				List<DragTarget> targets = targetsFor(frame(hitBox), point, drawing.orElseThrow(() -> new IllegalArgumentException(
						" cant mouse around something without drawing support (to provide coordinate system)"))
						.getScale(), hitBox.properties.isTrue(lockWidth, false),
					hitBox.properties.isTrue(lockHeight, false));

				find(Boxes.window, both()).findFirst()
					.ifPresent(x -> {
						if (targets.contains(DragTarget.bottom)) Cursors.arrowDown(x);
						else if (targets.contains(DragTarget.left)) Cursors.arrowLeft(x);
						else if (targets.contains(DragTarget.right)) Cursors.arrowRight(x);
						else if (targets.contains(DragTarget.top)) Cursors.arrowUp(x);
						else Cursors.clear(x);
					});
			} else {
				find(Boxes.window, both()).findFirst()
					.ifPresent(x -> {
						Cursors.clear(x);
					});
			}
			return null;
		});

		this.properties.putToMap(FLineDrawing.bulkLines, "__extraAnnotationsForSnap__", () -> {
			return extraAnnotations;
		});
	}

	static public void continueTranslationFeedback(Box b, boolean endNow) {

		Drawing q = b.find(Drawing.drawing, b.both())
			.findFirst()
			.get();

		Rect view = q.getCurrentViewBounds(b);

		b.properties.putToMap(FLineDrawing.frameDrawing, "__panning__", FLineDrawing.expires(box -> {

			FLine f = new FLine();

			for (int x = 100 * (int) (view.x / 100); x < view.x + view.w + 100; x += 100) {
				f.moveTo(x, view.y - 100);
				f.lineTo(x, view.y + view.h + 100);
				if (x % 500 == 0) {
					f.moveTo(x, view.y - 100);
					f.lineTo(x, view.y + view.h + 100);
				}
			}
			for (int x = 100 * (int) (view.y / 100); x < view.y + view.h + 100; x += 100) {
				f.moveTo(view.x - 100, x);
				f.lineTo(view.x + view.w + 100, x);
				if (x % 500 == 0) {
					f.moveTo(view.x - 100, x);
					f.lineTo(view.x + view.w + 100, x);
				}
			}

			f.attributes.put(StandardFLineDrawing.color, new Vec4(0, 0, 0, 0.25f));
			f.attributes.put(FLineDrawing.layer, "__main__.fast");

			return f;
		}, 100));

		b.properties.putToMap(FLineDrawing.frameDrawing, "__origin__", FLineDrawing.expires(box -> {

			FLine f = new FLine();

			f.moveTo(0, view.y - 100);
			f.lineTo(0, view.y + view.h + 100);
			f.moveTo(view.x - 100, 0);
			f.lineTo(view.x + view.w + 100, 0);


			f.attributes.put(StandardFLineDrawing.thicken, new BasicStroke(10));
			f.attributes.put(StandardFLineDrawing.color, new Vec4(0, 0, 0, 0.25f));
			f.attributes.put(FLineDrawing.layer, "__main__.fast");

			return f;
		}, 100));

	}

	public Mouse.Dragger onMouseDown(Window.Event<Window.MouseState> e, int button) {

		// if this event has already been consumed by somebody else, then let's do nothing
		if (e.properties.isTrue(Window.consumed, false)) {
			return null;
		}

		if (button == 0 /*|| button == 1*/) return button0(e);
		if (button == 2) return button2(e);

		return null;
	}

	public Mouse.Dragger button2(Window.Event<Window.MouseState> e) {
		Optional<Drawing> drawing = this.find(Drawing.drawing, both())
			.findFirst();
		Vec2 point = new Vec2(e.after.mx, e.after.my);

		Optional<Box> hit = breadthFirst(both()).filter(b -> frame(b) != null)
			.filter(b -> !b.properties.isTrue(Box.hidden, false))
			.filter(b -> Planes.on(root, b) >= 0)
			.filter(b -> frame(b).intersects(point))
			.sorted((a, b) -> Float.compare(order(frame(a)), order(frame(b))))
			.findFirst();

		startTranslationFeedback();

		if (!hit.isPresent()) {

			Drawing d = drawing.get();
			Vec2 originalT = d.getTranslation();
			Vec2 originalScale = d.getScale();

			return (drag, termination) -> {
				Vec2 deltaNow = drawing.map(x -> x.windowSystemToDrawingSystemDelta(new Vec2(drag.after.x - e.after.x, drag.after.y - e.after.y)))
					.orElseThrow(() -> new IllegalArgumentException(" cant mouse around something without drawing support (to provide coordinate system)"));

				if (drawing.isPresent()) {

					if (drag.after.keyboardState.isShiftDown()) {
						Window.MouseState q = e.after.withScroll((drag.after.x - drag.before.x) / 6, (drag.after.y - drag.before.y) / 6);

						q.mx = drag.after.mx;
						q.my = drag.after.my;
						q.mdx = drag.after.mdx;
						q.mdy = drag.after.mdy;

						Window.Event<Window.MouseState> e2 = new Window.Event<>(drag.before, q);
						e2.after.keyboardState = drag.after.keyboardState;
						Scrolling.scrollForMouseEvent(this, e2, drawing.get());
					} else {

						Vec2 t = new Vec2(originalT).add(deltaNow.x, deltaNow.y);
						d.setTranslation(FrameManipulation.this, t);
						continueTranslationFeedback(FrameManipulation.this, termination);
					}
				}
				return !termination;
			};
		}
		return null;
	}

	private void startTranslationFeedback() {
		continueTranslationFeedback(this, false);
	}

	public Mouse.Dragger button0(Window.Event<Window.MouseState> e) {
		Optional<Drawing> drawing = this.find(Drawing.drawing, both())
			.findFirst();

		Vec2 point = new Vec2(e.after.mx, e.after.my);

		Optional<Box> hit = breadthFirst(both()).filter(b -> frame(b) != null)
			.filter(b -> !b.properties.isTrue(Box.hidden, false))
			//.filter(b -> !b.properties.isTrue(Mouse.isSticky, false))
			.filter(b -> Planes.on(root, b) >= 0.5)
			.filter(b -> frame(b).intersects(point))
			.sorted((a, b) -> Float.compare(order(frame(a)), order(frame(b))))
			.findFirst();


		Log.log("selection", () -> "hit box is " + hit.orElse(null));


		return hit.map(hitBox -> {

			Log.log("selection", () -> "hit box is really hidden ? " + hitBox.properties.get(Box.hidden));

			Drawing.dirty(hitBox);

			boolean shift = e.after.keyboardState.isShiftDown();

			boolean command = e.after.keyboardState.isSuperDown();

			command = command | hitBox.properties.isTrue(Mouse.isSticky, false);

			final boolean finalCommand = command;
			Log.log("selection", () -> "shift / command " + shift + " / " + finalCommand);

			boolean selected = hitBox.properties.isTrue(Mouse.isSelected, false);

			Log.log("selection", () -> "hit box is currently selected :" + selected);

			Rect originalFrame = frame(hitBox);

			Set<Box> workingSet = new LinkedHashSet<Box>();

			if (!command) {
				if (shift) {
					workingSet.addAll(breadthFirst(both()).filter(x -> x.properties.isTrue(Mouse.isSelected, false))
//									      .filter(b -> !b.properties.isTrue(Mouse.isSticky, false))
						.filter(x -> x.properties.has(Box.frame))
						.filter(x -> x.properties.has(Box.name))
						.collect(Collectors.toSet()));
					workingSet.add(hitBox);
				} else {
					workingSet.add(hitBox);
				}

				Log.log("selection", () -> "working set is :" + workingSet);


				System.err.println("RUN: setSelectionTo:" + workingSet);
				setSelectionTo(this, workingSet);
			} else {
				workingSet.add(hitBox);
				hitBox.breadthFirst(x -> {
					LinkedHashSet<Box> c = new LinkedHashSet<>(x.children());
					return c.stream().filter(q -> q != root).filter(q -> Planes.on(root, q) >= 1).collect(Collectors.toSet());
				}).filter(x -> Planes.on(root, x) >= 0.5).forEach(x -> workingSet.add(x));
			}

			if (hitBox.properties.isTrue(Mouse.isSticky, false)) {
				workingSet.clear();
				;
				workingSet.add(hitBox);
			}


			workingSet.forEach(x -> x.properties.put(Mouse.isManipulated, true));

			if (e.after.buttonsDown.contains(0)) {
				// what kind of drag might this be?

				List<DragTarget> targets = targetsFor(frame(hitBox), point, drawing.orElseThrow(() -> new IllegalArgumentException(
						" cant mouse around something without drawing support (to provide coordinate system)"))
						.getScale(), hitBox.properties.isTrue(lockWidth, false),
					hitBox.properties.isTrue(lockHeight, false));


				feedback(hitBox, originalFrame, originalFrame, -1);

				hitBox.properties.put(Mouse.isManipulated, true);


				Set<Box> dependands = workingSet.stream()
					.flatMap(x -> singleChildrenFor(x).stream())
					.collect(Collectors.toSet());

				workingSet.addAll(dependands);


				return (Mouse.Dragger) (drag, termination) -> {

					if (drag.properties.isTrue(Window.consumed, false)) return true;

					Log.log("selection", () -> "hit box is really hidden ? " + hitBox.properties.get(Box.hidden));

					Vec2 delta = new Vec2(drag.after.dx, drag.after.dy);
					Vec2 drawingDelta = drawing.map(x -> x.windowSystemToDrawingSystemDelta(delta))
						.orElseThrow(() -> new IllegalArgumentException(
							" cant mouse around something without drawing support (to provide coordinate system)"));

//					System.out.println(" delta :"+delta+" -> "+drawingDelta);

					List<Triple<Box, Rect, Rect>> c1 = workingSet.stream().map(x -> {
						Rect r0 = frame(x);
						return transformPart1(x, r0, targets, drawingDelta);
					}).collect(Collectors.toList());

					if (drag.after.keyboardState.keysDown.contains(GLFW_KEY_TAB))
						extraAnnotations = helper.help(c1);
					else
						extraAnnotations = Collections.emptyList();

					transformPart2(c1);

					c1.forEach(x -> {
						x.first.properties.put(frame, x.third);
						feedback(hitBox, originalFrame, x.third, termination ? 60 : -1);
						Drawing.dirty(hitBox);
					});

//					workingSet.stream()
//
//						.forEach(x -> {
//							Rect r0 = frame(x);
//							Rect r = transform(x, r0, targets, drawingDelta);
//							x.properties.put(frame, r);
//							feedback(hitBox, originalFrame, r, termination ? 60 : -1);
//							Drawing.dirty(hitBox);
//						});

					if (!e.after.keyboardState.isSuperDown())
						if (termination && frame(hitBox).equals(originalFrame) && selected) {
							hitBox.properties.put(Mouse.isSelected, false);
							Drawing.dirty(hitBox);
						}


					if (termination) {

						extraAnnotations = Collections.emptyList();

						if (!e.after.keyboardState.isSuperDown())
							if (drag.after.x == e.after.x && drag.after.y == e.after.y) {
								if (!shift /*&& !hitBox.properties.isTrue(Mouse.isSticky, false)*/)
									breadthFirst(both())/*.filter(x -> !x.properties.isTrue(Mouse.isSticky, false))*/
										.forEach(x -> Callbacks.transition(x, Mouse.isSelected,
											false, false,
											Callbacks.onSelect,
											Callbacks.onDeselect));

								Callbacks.transition(hitBox, Mouse.isSelected, true, false, Callbacks.onSelect, Callbacks.onDeselect);

							}


						breadthFirst(both()).forEach(x -> x.properties.remove(Mouse.isManipulated));

						find(Boxes.window, both()).findFirst()
							.ifPresent(x -> Cursors.clear(x));
					}

					return true;
				};
			}
			return null;
		})
			.orElseGet(() -> {

				if (!e.after.buttonsDown.contains(0)) return null;
				if (e.after.keyboardState.isAltDown()) return null;

				boolean shift = e.after.keyboardState.keysDown.contains(GLFW_KEY_LEFT_SHIFT) || e.after.keyboardState.keysDown.contains(GLFW_KEY_RIGHT_SHIFT);
				if (!shift)
					breadthFirst(both()).forEach(x -> Callbacks.transition(x, Mouse.isSelected, false, false, Callbacks.onSelect, Callbacks.onDeselect));

				Map<Box, Boolean> frozenAt = new LinkedHashMap<Box, Boolean>();
				breadthFirst(both()).forEach(x -> frozenAt.put(x, x.properties.isTrue(Mouse.isSelected, false)));

				Drawing.dirty(this);

				Vec2 downAt = new Vec2(point);

				return (drag, termination) -> {
					Vec2 delta = new Vec2(drag.after.dx, drag.after.dy);

					Vec2 drawingDelta = drawing.map(x -> x.windowSystemToDrawingSystemDelta(delta))
						.orElseThrow(() -> new IllegalArgumentException(
							" cant mouse around something without drawing support (to provide coordinate system)"));
					downAt.x += drawingDelta.x;
					downAt.y += drawingDelta.y;

					if (termination)
						this.properties.removeFromMap(FLineDrawing.frameDrawing, "__marquee__");
					else {
						breadthFirst(both()).forEach(x -> {
							Boolean b = frozenAt.get(x);

							Rect f = frame(x);
							if (f == null) return;

							if (f.intersects(new Rect(Math.min(downAt.x, point.x), Math.min(downAt.y, point.y), Math.max(downAt.x, point.x) - Math.min(downAt.x, point.x),
								Math.max(downAt.y, point.y) - Math.min(downAt.y, point.y))) && (!x.properties.isTrue(Box.hidden, false)) && Planes.on(root, x) >= 0.5) {
								Callbacks.transition(x, Mouse.isSelected, true, false, Callbacks.onSelect, Callbacks.onDeselect);
							} else {
								if (b == null || !b)
									Callbacks.transition(x, Mouse.isSelected, false, false, Callbacks.onSelect, Callbacks.onDeselect);
							}
						});

						this.properties.putToMap(FLineDrawing.frameDrawing, "__marquee__", (box) -> {

							FLine m = new FLine();
							m.moveTo(point.x, point.y);
							m.lineTo(point.x, downAt.y);
							m.lineTo(downAt.x, downAt.y);
							m.lineTo(downAt.x, point.y);
							m.lineTo(point.x, point.y);

							m.attributes.put(StandardFLineDrawing.thicken, new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
							m.attributes.put(StandardFLineDrawing.strokeColor, new Vec4(1, 1, 1, 0.2f));
							m.attributes.put(StandardFLineDrawing.fillColor, new Vec4(1, 1, 1, 0.3f));
							m.attributes.put(StandardFLineDrawing.stroked, true);
							m.attributes.put(StandardFLineDrawing.filled, true);
							m.attributes.put(StandardFLineDrawing.pointed, false);

							return m;
						});
					}

					Drawing.dirty(this);
					return true;
				};
			});
	}

	static public Runnable setSelectionTo(Box root, Set<Box> workingSet) {
		Set<Box> previouslySelected = new LinkedHashSet<>();

		root.breadthFirst(root.both())
			.filter(x -> x.properties.has(Box.frame))
			.filter(x -> x.properties.has(Box.name)).forEach(x -> {

			if (x.properties.isTrue(Mouse.isSelected, false)) {
				previouslySelected.add(x);
			}

			if (x.properties.isTrue(Mouse.isSelected, false) && !workingSet.contains(x)) {
				Callbacks.transition(x, Mouse.isSelected, false, false, Callbacks.onSelect, Callbacks.onDeselect);
				Drawing.dirty(x);
			} else if (!x.properties.isTrue(Mouse.isSelected, false) && workingSet.contains(x)) {
				Callbacks.transition(x, Mouse.isSelected, true, false, Callbacks.onSelect, Callbacks.onDeselect);
				Drawing.dirty(x);
			}

		});

		return () -> setSelectionTo(root, previouslySelected);
	}

	private Set<Box> singleChildrenFor(Box z) {

		// for now, let's just move this box
		return Collections.singleton(z);
	}

	protected Rect frame(Box hitBox) {
		return hitBox.properties.get(Box.frame);
	}

	private Triple<Box, Rect, Rect> transformPart1(Box b, Rect r, List<DragTarget> targets, Vec2 drawingDelta) {
		Rect source = r.duplicate();
		for (DragTarget d : targets) {

			Rect was = r;

			switch (d) {
				case bottom:
					r = new Rect(r.x, r.y, r.w, r.h + drawingDelta.y);
					break;
				case top:
					r = new Rect(r.x, r.y + drawingDelta.y, r.w, r.h - drawingDelta.y);
					break;
				case left:
					r = new Rect(r.x + drawingDelta.x, r.y, r.w - drawingDelta.x, r.h);
					break;
				case right:
					r = new Rect(r.x, r.y, r.w + drawingDelta.x, r.h);
					break;
				case translate:
					r = new Rect(r.x + drawingDelta.x, r.y + drawingDelta.y, r.w, r.h);
					break;
			}

			if (r.w < 20) r = new Rect(r.x, r.y, 20, r.h);
			if (r.h < 20) r = new Rect(r.x, r.y, r.w, 20);

			if (b.properties.isTrue(lockWidth, false)) r = new Rect(r.x, r.y, was.w, r.h);
			if (b.properties.isTrue(lockHeight, false)) r = new Rect(r.x, r.y, r.w, was.h);
			if (b.properties.isTrue(lockX, false)) r = new Rect(was.x, r.y, r.w, r.h);
			if (b.properties.isTrue(lockY, false)) r = new Rect(r.x, was.y, r.w, r.h);
		}

//		r = Callbacks.frameChange(b, r);
//
//		return r;
		return new Triple(b, source, r);
	}

	private List<Triple<Box, Rect, Rect>> transformPart2(List<Triple<Box, Rect, Rect>> actions) {
		return actions.stream()
			.map(x -> new Triple<Box, Rect, Rect>(x.first, x.second, Callbacks.frameChange(x.first, x.third))).collect(Collectors.toList());
	}


	private List<DragTarget> targetsFor(Rect rect, Vec2 point, Vec2 scale, boolean lockWidth, boolean lockHeight) {
		List<DragTarget> r = new ArrayList<>();

		float inset = 10;
		if (Math.abs(point.x - rect.x) < inset && !lockWidth) r.add(DragTarget.left);
		if (Math.abs(point.x - rect.x - rect.w) < inset && !lockWidth) r.add(DragTarget.right);
		if (Math.abs(point.y - rect.y) < inset && !lockHeight) r.add(DragTarget.top);
		if (Math.abs(point.y - rect.y - rect.h) < inset && !lockHeight) r.add(DragTarget.bottom);
		if (r.size() == 0) r.add(DragTarget.translate);

		return r;
	}

	protected float order(Rect r) {
		return Math.abs(r.w) + Math.abs(r.h);
	}

	protected void feedback(Box b, Rect r0, Rect r, int exp) {
		if (!r0.equals(r) && (b.properties.getFromMap(FLineDrawing.frameDrawing, "__feedback__") != null || exp == -1 || true))
			b.properties.putToMap(FLineDrawing.frameDrawing, "__feedback__", FLineDrawing.expires(box -> {
				FLine f = new FLine();
				f.attributes.put(StandardFLineDrawing.hasText, true);

				float d = b.properties.getFloat(depth, 0f);

				f.moveTo(r.x + r.w / 2, r.y + r.h + 17, d);
				List<String> text = new ArrayList<String>();
				List<Vec4> color = new ArrayList<Vec4>();

				text.add(String.format("%.0f", r.x));
				color.add(r.x == r0.x ? new Vec4(1, 1, 1, 0.5f) : new Vec4(1, 1, 1, 1));
				text.add(",");
				color.add(new Vec4(1, 1, 1, 0.5f));
				text.add(String.format("%.0f", r.y));
				color.add(r.y == r0.y ? new Vec4(1, 1, 1, 0.5f) : new Vec4(1, 1, 1, 1));
				text.add(" ");
				color.add(new Vec4(1, 1, 1, 0.5f));
				text.add(String.format("%.0f", r.w));
				color.add(r.w == r0.w ? new Vec4(1, 1, 1, 0.5f) : new Vec4(1, 1, 1, 1));
				text.add("x");
				color.add(new Vec4(1, 1, 1, 0.5f));
				text.add(String.format("%.0f", r.h));
				color.add(r.h == r0.h ? new Vec4(1, 1, 1, 0.5f) : new Vec4(1, 1, 1, 1));

				f.nodes.get(f.nodes.size() - 1).attributes.put(StandardFLineDrawing.textSpans, text);
				f.nodes.get(f.nodes.size() - 1).attributes.put(StandardFLineDrawing.textColorSpans, color);
				return f;
			}, exp));

		Drawing.dirty(b);

	}

	public enum DragTarget {
		translate, left, top, right, bottom
	}
}
