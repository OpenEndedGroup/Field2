package fieldbox.boxes.plugins;

import static org.lwjgl.glfw.GLFW.*;
import field.graphics.Window;
import field.linalg.Vec2;
import field.utility.Cached;
import field.utility.Dict;
import field.utility.Rect;
import field.utility.Triple;
import fieldbox.boxes.*;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Keyboard navigation to move selection around but also refer to boxes programmatically
 */
public class Directionality extends Box {

	static public final Dict.Prop<Directionality> directionality = new Dict.Prop<>(
		    "_directionality");

	static public final Dict.Prop<FunctionOfBox<Box>> moveLeft = new Dict.Prop<>("moveLeft");
	static public final Dict.Prop<FunctionOfBox<Box>> moveRight = new Dict.Prop<>("moveRight");
	static public final Dict.Prop<FunctionOfBox<Box>> moveUp = new Dict.Prop<>("moveUp");
	static public final Dict.Prop<FunctionOfBox<Box>> moveDown = new Dict.Prop<>("moveDown");

	private final Cached<Object, Long, ONN2<Triple<Box, Vec2, ONN2.Dir>>> structure;

	static public class ONN2<T> {

		protected List<T> n;
		protected Function<T, Vec2> unwrap;
		protected List<Node> nodes;

		public enum Dir {
			left(1, 0), right(-1, 0), up(0, 1), down(0, -1);

			public final int x;
			public final int y;

			Dir(int x, int y) {
				this.x = x;
				this.y = y;
			}

		}


		static public class Node {
			public Vec2 v;

			public Node[] connections = new Node[4];
			public int index;
			public int indexX;
			public int indexY;

			@Override
			public String toString() {
				return "" + v + "@(" + indexX + ", " + indexY + ")";
			}

		}

		public List<Node> compute(List<T> x, Function<T, Vec2> unwrap) {
			this.n = x;
			this.unwrap = unwrap;

			nodes = new ArrayList<Node>();
			n.stream()
			 .map(unwrap::apply)
			 .forEach(v -> {
				 Node n = new Node();
				 n.v = v;
				 n.index = nodes.size();
				 nodes.add(n);
			 });

			ArrayList<Node> left = new ArrayList<Node>(nodes);
			ArrayList<Node> down = new ArrayList<Node>(nodes);

			Collections.sort(left, new Comparator<Node>() {

				@Override
				public int compare(Node o1, Node o2) {
					return Double.compare(o1.v.x, o2.v.x);
				}
			});

			Collections.sort(down, new Comparator<Node>() {

				@Override
				public int compare(Node o1, Node o2) {
					return Double.compare(o1.v.y, o2.v.y);
				}
			});

			for (int i = 0; i < left.size(); i++) {
				left.get(i).indexX = i;
				down.get(i).indexY = i;
			}

			for (Node n : nodes) {
				connect(left, down, n);
			}

			return nodes;
		}

		private void connect(ArrayList<Node> left, ArrayList<Node> down, Node n) {
			if (n.connections[Dir.right.ordinal()] == null) {
				double best = Double.POSITIVE_INFINITY;
				int bestIs = -1;

				for (int i = n.indexX + 1; i < left.size(); i++) {
					if (Math.abs(left.get(i).v.y - n.v.y) <= left.get(
						    i).v.x - n.v.x) {

						double d = left.get(i).v.distance(n.v);
						if (d < best && check(n.connections, left.get(i))) {
							best = d;
							bestIs = i;
						}
					}
					if (Math.abs(left.get(i).v.x - n.v.x) > best) break;
				}
				if (bestIs != -1)
					n.connections[Dir.right.ordinal()] = left.get(bestIs);
			}

			if (n.connections[Dir.left.ordinal()] == null) {
				double best = Double.POSITIVE_INFINITY;
				int bestIs = -1;

				for (int i = n.indexX - 1; i >= 0; i--) {

					if (Math.abs(left.get(i).v.y - n.v.y) <= n.v.x - left.get(
						    i).v.x) {

						double d = left.get(i).v.distance(n.v);
						if (d < best && check(n.connections, left.get(i))) {
							best = d;
							bestIs = i;
						}
					}
					if (Math.abs(n.v.x - left.get(i).v.x) > best) break;
				}
				if (bestIs != -1)
					n.connections[Dir.left.ordinal()] = left.get(bestIs);
			}

			if (n.connections[Dir.down.ordinal()] == null) {
				double best = Double.POSITIVE_INFINITY;
				int bestIs = -1;

				for (int i = n.indexY + 1; i < down.size(); i++) {
					if (Math.abs(down.get(i).v.x - n.v.x) <= down.get(
						    i).v.y - n.v.y) {

						double d = down.get(i).v.distance(n.v);
						if (d < best && check(n.connections, down.get(i))) {
							best = d;
							bestIs = i;
						}
					}
					if (Math.abs(down.get(i).v.y - n.v.y) > best) break;
				}
				if (bestIs != -1)
					n.connections[Dir.down.ordinal()] = down.get(bestIs);
			}

			if (n.connections[Dir.up.ordinal()] == null) {
				double best = Float.POSITIVE_INFINITY;
				int bestIs = -1;

				for (int i = n.indexY - 1; i >= 0; i--) {

					if (Math.abs(down.get(i).v.x - n.v.x) <= n.v.y - down.get(
						    i).v.y) {

						double d = down.get(i).v.distance(n.v);
						if (d < best && check(n.connections, down.get(i))) {
							best = d;
							bestIs = i;
						}
					}
					if (Math.abs(n.v.y - down.get(i).v.y) > best) break;
				}
				if (bestIs != -1)
					n.connections[Dir.up.ordinal()] = down.get(bestIs);
			}
		}

		private boolean check(Node[] connections, Node node) {
			for (Node n : connections)
				if (n == node) return false;
			return true;
		}
	}

	Map<Integer, FunctionOfBox<Box>> keys = new LinkedHashMap<>();


	public Directionality(Box root) {

		structure = FrameChangedHash.getCached(root,
						       (BiFunction<Object, ONN2<Triple<Box, Vec2, ONN2.Dir>>, ONN2<Triple<Box, Vec2, ONN2.Dir>>>) (nothing, previous) -> computeStrctureNow());
		this.properties.put(directionality, this);
		this.properties.put(moveUp, this::up);
		this.properties.put(moveDown, this::down);
		this.properties.put(moveLeft, this::left);
		this.properties.put(moveRight, this::right);

		keys.put(GLFW_KEY_LEFT, this::left);
		keys.put(GLFW_KEY_RIGHT, this::right);
		keys.put(GLFW_KEY_UP, this::up);
		keys.put(GLFW_KEY_DOWN, this::down);

		this.properties.putToMap(Keyboard.onKeyDown, "__directionality__", (e, k) -> {
			if (e.properties.isTrue(Window.consumed, false)) return null;

			for (Map.Entry<Integer, FunctionOfBox<Box>> ff : keys.entrySet()) {
				Integer kk = ff.getKey();

				if (k == kk && !e.before.keysDown.contains(
					    k) && !e.after.isAltDown() && !e.after.isControlDown() && !e.after.isShiftDown() && !e.after.isSuperDown() && selection().count() == 1) {
					e.properties.put(Window.consumed, true);
					Box s = selection().findFirst()
							   .get();
					Box s2 = ff.getValue()
						   .apply(s);
					if (s2 != null) {
						Callbacks.transition(s, Mouse.isSelected, false, false, Callbacks.onSelect, Callbacks.onDeselect);
						Callbacks.transition(s2, Mouse.isSelected, true, false, Callbacks.onSelect, Callbacks.onDeselect);
						Drawing.dirty(s);
					}

				}
			}
			return null;
		});
	}


	private Stream<Box> selection() {
		return breadthFirst(both()).filter(
			    x -> x.properties.isTrue(Mouse.isSelected, false));
	}

	protected ONN2<Triple<Box, Vec2, ONN2.Dir>> computeStrctureNow() {
		ONN2<Triple<Box, Vec2, ONN2.Dir>> structure = new ONN2<>();
		List<Triple<Box, Vec2, ONN2.Dir>> p = this.breadthFirst(this.downwards())
							  .filter(x -> x.properties.has(Box.frame))
							  .filter(x -> x.properties.has(Box.name))
							  .filter(x -> !(x instanceof TimeSlider))
							  .filter(x -> x.properties.has(
								      FLineDrawing.frameDrawing))
							  .flatMap(x -> {

									   ArrayList<Triple<Box, Vec2, ONN2.Dir>>
										       points
										       = new ArrayList<>();

									   Rect f
										       = x.properties.get(
										       Box.frame);

									   points.add(new Triple<>(
										       x,
										       f.convert(0.5f,
												 0.5f),
										       null));
									   points.add(new Triple<>(
										       x,
										       f.convert(0,
												 0.5f),
										       ONN2.Dir.left));
									   points.add(new Triple<>(
										       x,
										       f.convert(1,
												 0.5f),
										       ONN2.Dir.right));
									   points.add(new Triple<>(
										       x,
										       f.convert(0.5f,
												 0),
										       ONN2.Dir.up));
									   points.add(new Triple<>(
										       x,
										       f.convert(0.5f,
												 1),
										       ONN2.Dir.down));

									   return points.stream();
								   })
							  .collect(Collectors.toList());

		structure.compute(p, x -> x.second);
		return structure;
	}

	public Box left(Box a) {
		ONN2.Node n = locate(a, null);
		ONN2.Node n2 = step(n, ONN2.Dir.left);
		if (n2 == null) return null;
		return structure.apply(structure).n.get(n2.index).first;
	}

	public Box right(Box a) {
		ONN2.Node n = locate(a, null);
		ONN2.Node n2 = step(n, ONN2.Dir.right);
		if (n2 == null) return null;
		return structure.apply(structure).n.get(n2.index).first;
	}

	public Box up(Box a) {
		ONN2.Node n = locate(a, null);
		ONN2.Node n2 = step(n, ONN2.Dir.up);
		if (n2 == null) return null;
		return structure.apply(structure).n.get(n2.index).first;
	}

	public Box down(Box a) {
		ONN2.Node n = locate(a, null);
		ONN2.Node n2 = step(n, ONN2.Dir.down);
		if (n2 == null) return null;
		return structure.apply(structure).n.get(n2.index).first;
	}

	private ONN2.Node locate(Box a, ONN2.Dir f) {
		int found = -1;
		ONN2<Triple<Box, Vec2, ONN2.Dir>> o = structure.apply(structure);

		for (int i = 0; i < o.nodes.size(); i++) {

			if (o.n.get(i).first == a && o.n.get(i).third == f) {
				found = i;
				break;
			}
		}
		if (found == -1) return null;
		return o.nodes.get(found);
	}

	protected ONN2.Node step(ONN2.Node a, ONN2.Dir d) {

		Box o = structure.apply(structure).n.get(a.index).first;

		ONN2.Node next = a.connections[d.ordinal()];
		while (next != null && structure.apply(structure).n.get(next.index).first == o)
			next = next.connections[d.ordinal()];
		if (next == null) return null;

		return next;
	}


	protected Box rawMove(Box a, ONN2.Dir dir, HashSet<ONN2.Node> ignore) {

		ONN2<Triple<Box, Vec2, ONN2.Dir>> o = structure.apply(structure);

		int found = -1;
		for (int i = 0; i < o.nodes.size(); i++) {

			if (o.n.get(i).first == a && !ignore.contains(o.nodes.get(i))) {
				found = i;
				ignore.add(o.nodes.get(i));
				break;
			}
		}
		if (found == -1) return null;
		ONN2.Node to = o.nodes.get(found).connections[dir.ordinal()];
		if (to == null) return null;
		return o.n.get(to.index).first;
	}

}
