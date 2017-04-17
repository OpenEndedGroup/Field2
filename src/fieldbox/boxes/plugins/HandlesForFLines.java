package fieldbox.boxes.plugins;

import field.graphics.FLine;
import field.graphics.StandardFLineDrawing;
import field.linalg.Vec2;
import field.linalg.Vec4;
import field.utility.Dict;
import field.utility.IdempotencyMap;
import fieldbox.boxes.Box;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static fieldbox.boxes.plugins.Handles.draggables;
import static fieldbox.boxes.plugins.Handles.hasDraggables;

/**
 * Useful classes for turning FLines into editable things using Handles
 */
public class HandlesForFLines extends Box {

	static public void offset(FLine.Node node, int element, Vec2 by) {
		switch (element) {
			case 0: {
				node.to.x += by.x;
				node.to.y += by.y;
				node.modify();
				break;
			}
			case 1: {
				((FLine.CubicTo) node).c1.x += by.x;
				((FLine.CubicTo) node).c1.y += by.y;
				node.modify();
				break;
			}
			case 2: {
				((FLine.CubicTo) node).c2.x += by.x;
				((FLine.CubicTo) node).c2.y += by.y;
				node.modify();
				break;
			}
			default:
				throw new IllegalArgumentException("-- offset :" + node + " " + element + " " + by + " -- ");
		}
	}

	static public Vec2 v(double x, double y) {
		return new Vec2(x, y);
	}

	public String describeAll(String util, Box b, Dict.Prop<IdempotencyMap<FLine>> prop, FLine o) {

		IdempotencyMap<FLine> v = b.properties.get(prop);
		String found = null;
		for (Map.Entry<String, FLine> e : v.entrySet()) {
			if (e.getValue() == o) {
				found = e.getKey();
			}
		}
		if (found == null)
			throw new NullPointerException(" couldn't find line " + o + " in " + prop + " of " + b + " = " + v);
		return describeAll(util, "_." + prop.getName() + "." + found, o);
	}

	public String describeAll(String util, Box b, Dict.Prop<IdempotencyMap<FLine>> prop) {

		IdempotencyMap<FLine> v = b.properties.get(prop);
		String found = null;
		String ret = "";
		for (Map.Entry<String, FLine> e : v.entrySet()) {
			ret += describeAll(util, "_." + prop.getName() + "." + found, e.getValue());
		}
		return ret;
	}

	public String describeAll(String util, String flineName, FLine o) {
		List<Handles.Draggable> l1 = o.nodes.stream().filter(x -> x.attributes.has(draggables)).flatMap(x -> x.attributes.get(draggables).values().stream()).collect(Collectors.toList());
		;
		List<Handles.Draggable> n = new ArrayList<>();
		for (Handles.Draggable d : l1) {
			if (d.appearance == null) continue;
			d.appearance.get().stream().filter(x -> x.attributes.isTrue(hasDraggables, false))
				.flatMap(x -> x.nodes.stream())
				.filter(x -> x.attributes.has(draggables))
				.flatMap(x -> x.attributes.get(draggables)
					.values()
					.stream()).forEach(x -> n.add(x));

		}
		l1.addAll(n);

		String code = "_l=" + flineName + "\n";
		for (Handles.Draggable d : l1) {
			code += d.describe(util, "_l") + "\n";
		}

		return code;
	}


	static public class DraggableNode extends Handles.Draggable {

		private final int index;
		private final FLine nextCubic;
		private final FLine prevCubic;
		private final DraggableCubicHandle nextCubicDrag;
		private final DraggableCubicHandle prevCubicDrag;
		private final Consumer<Handles.Draggable> description;

		public DraggableNode(FLine source, FLine.Node on, Consumer<Handles.Draggable> description) {
			this.description = description;

			on.attributes.putToMap(draggables, "__center", this);

			index = source.nodes.indexOf(on);

			if (index < source.nodes.size() - 1 && source.nodes.get(index + 1) instanceof FLine.CubicTo) {
				FLine.CubicTo c = (FLine.CubicTo) source.nodes.get(index + 1);
				nextCubic = new FLine();
				nextCubic.moveTo(c.c1.x, c.c1.y);
				nextCubic.lineTo(on.to.x, on.to.y);
				nextCubic.attributes.put(StandardFLineDrawing.color, new Vec4(0, 0, 0, 0.5));
				nextCubicDrag = new DraggableCubicHandle(source, nextCubic.nodes.get(0), c, 0, description);
				nextCubic.attributes.put(hasDraggables, true);
				nextCubic.nodes.get(0).attributes.putToMap(draggables, "__cubic", nextCubicDrag);
			} else {
				nextCubic = null;
				nextCubicDrag = null;
			}

			if (on instanceof FLine.CubicTo) {
				FLine.CubicTo c = (FLine.CubicTo) source.nodes.get(index);
				prevCubic = new FLine();
				prevCubic.moveTo(c.c2.x, c.c2.y);
				prevCubic.lineTo(on.to.x, on.to.y);
				prevCubic.attributes.put(StandardFLineDrawing.color, new Vec4(0, 0, 0, 0.5));
				prevCubicDrag = new DraggableCubicHandle(source, prevCubic.nodes.get(0), c, 1, description);
				prevCubic.attributes.put(hasDraggables, true);
				prevCubic.nodes.get(0).attributes.putToMap(draggables, "__cubic", prevCubicDrag);
			} else {
				prevCubic = null;
				prevCubicDrag = null;
			}

			this.get = () -> on.to.toVec2();

			this.setAndConstrain = (next, previous, initial) -> {
				on.to.x = next.x;
				on.to.y = next.y;
				source.modify();

				if (nextCubic != null) {
					nextCubic.nodes.get(1).to.x = next.x;
					nextCubic.nodes.get(1).to.y = next.y;
					nextCubic.modify();
				}

				if (prevCubic != null) {
					prevCubic.nodes.get(1).to.x = next.x;
					prevCubic.nodes.get(1).to.y = next.y;
					prevCubic.modify();
				}

				return next;
			};
			this.select = x -> x;
			this.appearance = () -> {

				FLine f = new FLine();
				f.moveTo(on.to.x, on.to.y);
				f.attributes.put(StandardFLineDrawing.pointSize, isSelected() ? 10f : 5f);
				f.attributes.put(StandardFLineDrawing.pointed, true);
				f.attributes.put(StandardFLineDrawing.color, new Vec4(0, 0, 0, isSelected() ? 0.95 : 0.5f));

				List<FLine> ret = new ArrayList<>();
				ret.add(f);
				if (isSelected() || (prevCubic != null && prevCubicDrag.isSelected()) || (nextCubic != null && nextCubicDrag.isSelected())) {
					if (nextCubic != null) ret.add(nextCubic);
					if (prevCubic != null) ret.add(prevCubic);
				}

				System.out.println(" returning appearance :"+ret);

				return ret;

			};
			this.finisher = (v) -> {
				return v;
			};

			source.attributes.put(hasDraggables, true);

			init();

		}

		@Override
		public void commit() {
			if (sourcePosition.distance(cachePosition) > 0) {
				description.accept(this);
			}
			super.commit();
		}

		@Override
		public String describe(String utilities, String theline) {
			Vec2 v = new Vec2(cachePosition).sub(sourcePosition);
			return utilities + ".offset(" + theline + ".nodes[" + index + "], " + 0 + ", " + utilities + ".v(" + v.x+", "+v.y + "))";
		}
	}

	static public class DraggableCubicHandle extends Handles.Draggable {

		private final int index;
		private int c;
		private Consumer<Handles.Draggable> description;

		public DraggableCubicHandle(FLine source, FLine.Node vis, FLine.CubicTo on, int c, Consumer<Handles.Draggable> description) {
			this.c = c;
			this.description = description;

			index = source.nodes.indexOf(on);

			this.get = () -> vis.to.toVec2();
			this.setAndConstrain = (next, previous, inital) -> {

				vis.to.x = next.x;
				vis.to.y = next.y;

				if (c == 0) {
					on.c1.x = next.x;
					on.c1.y = next.y;
				} else {
					on.c2.x = next.x;
					on.c2.y = next.y;
				}
				source.modify();
				on.modify();
				vis.modify();

				return next;
			};
			this.select = x -> x;
			this.appearance = () -> {

				FLine f = new FLine();
				f.moveTo(vis.to.x, vis.to.y);
				f.attributes.put(StandardFLineDrawing.pointSize, isSelected() ? 10f : 5f);
				f.attributes.put(StandardFLineDrawing.pointed, true);
				f.attributes.put(StandardFLineDrawing.color, new Vec4(0.25f, 0, 0, isSelected() ? 0.95 : 0.5f));


				return Collections.singletonList(f);

			};
			this.finisher = (v) -> {
				return v;
			};

			source.attributes.put(hasDraggables, true);

			init();

		}

		@Override
		public void commit() {
			if (sourcePosition.distance(cachePosition) > 0) {
				description.accept(this);
			}
			super.commit();
		}

		@Override
		public String describe(String utilities, String theline) {
			Vec2 v = new Vec2(cachePosition).sub(sourcePosition);
			return utilities + ".offset(" + theline + ".nodes[" + index + "], " + (c + 1) + ", " + utilities + ".v(" + v.x+", "+v.y + "))";
		}
	}

}
