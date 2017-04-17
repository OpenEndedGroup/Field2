package fieldbox.boxes.plugins;

import field.graphics.FLine;
import field.graphics.Window;
import field.linalg.Vec2;
import field.utility.Dict;
import field.utility.IdempotencyMap;
import field.utility.Log;
import fieldbox.boxes.Box;
import fieldbox.boxes.Drawing;
import fieldbox.boxes.FLineDrawing;
import fieldbox.boxes.Mouse;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Extends FLineInteraction to include, essentially, a node editor. Unlike Field1 this is going to be much more pluggable
 * <p>
 * Rather than add this as a plug-in globally, merely add this above the boxes that you want to add this functionality to.
 */
public class Handles extends Box implements Mouse.OnMouseDown, Mouse.OnMouseMove {

	static public final Dict.Prop<Boolean> editable = new Dict.Prop<Boolean>("editable").type()
		.toCannon()
		.doc("Set this to true on an FLine to have it be editable by the handles system");
	static public final Dict.Prop<Handles> handles = new Dict.Prop<Handles>("handles").type()
		.toCannon()
		.doc("Provides customizeable node editing for FLines");

	static public final Dict.Prop<IdempotencyMap<Draggable>> draggables = new Dict.Prop<>("draggables").type()
		.toCannon()
		.doc("Collection of Draggable instances that offer interactive elements on the canvas. Can be inserted on FLine nodes")
		.autoConstructs(() -> new IdempotencyMap<Draggable>(Draggable.class));
	static public final Dict.Prop<Boolean> hasDraggables = new Dict.Prop<>("hasDraggables").type()
		.toCannon()
		.doc("set to mark that an FLine contains Draggables inside it, and that it should be searched for draggable Nodes");


	public interface SetAndConstrain {
		Vec2 apply(Vec2 next, Vec2 previous, Vec2 initial);
	}

	static public class Draggable {
		public Vec2 cachePosition = null;
		public Vec2 initialPosition = null;
		public Vec2 sourcePosition = null;

		public String name;

		public boolean selected = false;

		public Supplier<Vec2> get;
		public SetAndConstrain setAndConstrain;
		public Function<Boolean, Boolean> select;
		public Supplier<Collection<FLine>> appearance;
		public Function<Vec2, Vec2> finisher;
		public Runnable commit;

		public Draggable(Supplier<Vec2> get, SetAndConstrain setAndConstrain, Function<Boolean, Boolean> select, Supplier<Collection<FLine>> appearance, Function<Vec2, Vec2> finisher, Runnable commit) {
			this.get = get;
			this.setAndConstrain = setAndConstrain;
			this.select = select;
			this.appearance = appearance;
			this.finisher = finisher;
			this.commit = commit;
			init();
		}

		public Draggable(FLine.Node on, SetAndConstrain setAndConstrain, Function<Boolean, Boolean> select, Supplier<Collection<FLine>> appearance, Function<Vec2, Vec2> finisher, Runnable commit) {
			this.get = () -> on.to.toVec2();
			this.setAndConstrain = (next, previous, initial) -> {
				Vec2 v = setAndConstrain.apply(next, previous, initial);
				on.to.set(v, 0);
				return v;
			};
			this.select = select;
			this.appearance = appearance;
			this.finisher = finisher;
			this.commit = commit;
			init();
		}

		protected void init() {
			initialPosition = get.get();
			cachePosition = initialPosition.duplicate();
			sourcePosition = cachePosition.duplicate();
		}

		protected Draggable() {
		}

		public Vec2 getPosition() {
			if (cachePosition == null)
				return cachePosition = setAndConstrain.apply(get.get(), get.get(), initialPosition);
			return cachePosition;
		}

		public Vec2 getInitialPosition() {
			return initialPosition;
		}

		public void set(Vec2 v) {
			cachePosition = setAndConstrain.apply(v, getPosition(), initialPosition);
		}

		public void select(boolean to) {
			selected = select == null ? true : select.apply(to);
		}

		public void finish() {
			if (finisher != null)
				initialPosition = setAndConstrain.apply(finisher.apply(getPosition()), getPosition(), initialPosition);
			initialPosition = getPosition();
		}

		public boolean isSelected() {
			return selected;
		}

		public void commit() {
			if (sourcePosition.distance(cachePosition) > 0) {
				if (commit != null) commit.run();
				init();
			}
		}

		public String describe(String utilities, String theline) {
			return "";
		}
	}

	public Handles(Box root) {
		properties.put(Planes.plane, "__always__");
		properties.put(handles, this);
		properties.putToMap(Mouse.onMouseDown, "__handles__", this);
		properties.putToMap(Mouse.onMouseMove, "__handles__", this);
		properties.putToMap(FLineDrawing.bulkLines, "__handles__", this::appearence);
	}

	/**
	 * this needs to get behind a 'cached'
	 *
	 * @return
	 */
	public List<Draggable> all() {
		Set<Draggable> l1 = breadthFirst(downwards())
			.filter(x -> x != this)
			.filter(x -> x.properties.has(FLineDrawing.lines))
			.flatMap(x -> x.properties.get(FLineDrawing.lines)
				.values()
				.stream())
			.map(x -> x.get())
			.filter(x -> x != null)
			.filter(x -> x.attributes.isTrue(hasDraggables, false))
			.flatMap(x -> x.nodes.stream())
			.filter(x -> x.attributes.has(draggables))
			.flatMap(x -> x.attributes.get(draggables)
				.values()
				.stream())
			.collect(Collectors.toSet());
		Set<Draggable> next = new LinkedHashSet<>();
		Set<Draggable> fringe = new LinkedHashSet<>(l1);
		int sz;
		do {
			sz = l1.size();
			for (Draggable f : fringe) {
				Set<Draggable> finalNext = next;
				f.appearance.get().stream()
					.filter(x -> x.attributes.isTrue(hasDraggables, false)).map(x -> {

						System.out.println(x+" -> "+x.attributes.get(draggables));
						return x;
				})
					.flatMap(x -> x.nodes.stream())
					.filter(x -> x.attributes.has(draggables))
					.flatMap(x -> x.attributes.get(draggables).values().stream())
					.forEach(x -> finalNext.add(x));
			}

			l1.addAll(next);
			fringe = next;
			next = new LinkedHashSet<>();
		} while (l1.size() != sz);

		return new ArrayList<>(l1);
	}


	/**
	 * this needs to get behind a 'cached'
	 *
	 * @return
	 */
	public List<Supplier<FLine>> appearence() {
		List<Supplier<FLine>> a = all().stream()
			.flatMap(x -> (x.appearance == null ? new ArrayList<FLine>() : x.appearance.get()).stream())
			.collect(Collectors.toList());
		if (a.size() > 0)
			Log.log("handles", () -> "appearance is :" + a);
		return a;
	}

	@Override
	public Mouse.Dragger onMouseDown(Window.Event<Window.MouseState> e, int button) {
		List<Draggable> d = all();
		float r = 15;

		Optional<Drawing> drawing = this.find(Drawing.drawing, both())
			.findFirst();
		Vec2 pos = new Vec2(e.after.mx, e.after.my);


		Log.log("handles", () -> "onMouseDown :" + pos);
		Draggable selected = d.stream()
			.filter(x -> x.getPosition()
				.distance(pos) < r)
			.sorted((a, b) -> -Double.compare(a.getPosition()
				.distance(pos), b.getPosition()
				.distance(pos)))
			.findFirst()
			.orElse(null);

		Log.log("handles", () -> "selected :" + selected);

		if (selected == null) {

			boolean[] outgoingEdge = {false};

			// we do this in two stages so we can call commit() before we change the selection
			d.stream()
				.forEach(x -> {
					if (x != selected && x.selected) {
//						x.select(false);
						outgoingEdge[0] = true;
					}
				});
			Drawing.dirty(this);

			if (outgoingEdge[0]) {
				// we commit on selected -> nothing selected edges
				commit();
			}
			d.stream()
				.forEach(x -> {
					if (x != selected && x.selected) {
						x.select(false);
//						outgoingEdge[0] = true;
					}
				});

			return null;
		}

		if (e.after.keyboardState.isShiftDown()) {
			if (selected.selected) {
				selected.select(false);
			} else {
				selected.select(true);
			}
		} else {
			d.stream()
				.forEach(x -> {
					if (x != selected && x.selected) x.select(false);
				});
			selected.select(true);
		}

		Set<Draggable> sel = d.stream()
			.filter(x -> x.selected)
			.collect(Collectors.toSet());

		Log.log("handles", () -> "selected set :" + selected);

		Drawing.dirty(this);
		e.properties.put(Window.consumed, true);

		return draggerForSelection(e, sel);

	}

	private void commit() {
		all().forEach(x -> x.commit());
	}

	private Mouse.Dragger draggerForSelection(Window.Event<Window.MouseState> e, Set<Draggable> sel) {
		Optional<Drawing> drawing = this.find(Drawing.drawing, both())
			.findFirst();
		return (drag, term) -> {

			try {
				Vec2 deltaNow = drawing.map(x -> x.windowSystemToDrawingSystemDelta(new Vec2(drag.after.x - e.after.x, drag.after.y - e.after.y)))
					.orElseThrow(() -> new IllegalArgumentException(" cant mouse around something without drawing support (to provide coordinate system)"));

				drag.properties.put(Window.consumed, true);

				sel.forEach(d -> {
					d.set(Vec2.add(d.getInitialPosition(), deltaNow, null));
				});

				if (term) {
					sel.forEach(Draggable::finish);
				}
				Drawing.dirty(this);
				return !term;
			} catch (Throwable t) {
				t.printStackTrace();
				return false;
			}
		};
	}

	@Override
	public Mouse.Dragger onMouseMove(Window.Event<Window.MouseState> e) {
		return null;
	}

}
