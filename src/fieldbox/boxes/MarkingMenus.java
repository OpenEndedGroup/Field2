package fieldbox.boxes;

import field.graphics.FLine;
import field.graphics.FLinesAndJavaShapes;
import field.graphics.StandardFLineDrawing;
import field.graphics.Window;
import field.linalg.Vec2;
import field.linalg.Vec4;
import field.utility.Dict;
import field.utility.IdempotencyMap;
import field.utility.Rect;
import field.utility.SimpleVoronoi;
import fieldbox.boxes.plugins.Planes;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.awt.geom.Area;
import java.util.*;
import java.util.List;
import java.util.function.Function;

import static field.graphics.StandardFLineDrawing.*;
import static field.utility.Log.log;
import static fieldbox.boxes.FLineDrawing.frameDrawing;
import static fieldbox.boxes.FLineDrawing.layer;

/**
 * Plugin: MarkingMenus adds support for building and showing radial menus for boxes and for the canvas itself to Field.
 * <p>
 * Menus are built up from coalescing MenuSpecifications from the "menuSpecs" property (signature: Function<Window.Event<Window.MouseState>, MenuSpecification>). That puts labels and callbacks at
 * compass points (enum Position), and designs a Voronoi tesselation of the space between them. The idea is to allow quick gestures from the mouse or touchpad to access menuSpecs items via muscle
 * memory.
 */
public class MarkingMenus extends Box {

	static public Dict.Prop<MarkingMenus> markingMenus = new Dict.Prop<>("markingMenus");

	// sophisticated interface
	static public Dict.Prop<Function<Window.Event<Window.MouseState>, MenuSpecification>> menuSpecs = new Dict.Prop<>("menuSpecs"); // TODO: comment

	// easy interface
	static public Dict.Prop<IdempotencyMap<FunctionOfBox>> menu = new Dict.Prop<>("menu").toCannon()
		.autoConstructs(() -> new IdempotencyMap<>(FunctionOfBox.class)).doc("Adds a menu item to a box at a particular direction. For example `_.menu.show_something_n = function(_) { ... }` will place a menu item 'show something' at the 'N(orth)' position. Note this position can get 'bumped' by other items.");


	// easy interface
	static public Dict.Prop<IdempotencyMap<FunctionOfBox>> spaceMenu = new Dict.Prop<>("spaceMenu").toCannon()
		.autoConstructs(() -> new IdempotencyMap<>(FunctionOfBox.class)).doc("Adds a menu item to a box at a particular direction. For example `_.spaceMenu.show_something_n = function(_) { ... }` will place a menu item 'show something' at the 'N(orth)' position. Note this position can get 'bumped' by other items.");


	static public Dict.Prop<IdempotencyMap<FunctionOfBox>> nothingCallback = new Dict.Prop<>("nothingCallback").toCannon()
			.autoConstructs(() -> new IdempotencyMap<>(FunctionOfBox.class)).doc("Callback that's called should nothing be selected in a right-click menu. Allows you to bind a right-click to a box.");



	public MarkingMenus(Box root) {

		this.properties.put(markingMenus, this);
		this.properties.put(FLineDrawing.layer, "glass2");
		this.properties.put(Planes.plane, "__always__");

		this.properties.putToMap(Mouse.onMouseDown, "__markingmenus__", (event, button) -> {
			if (button != 1) return null;

			if (event.properties.isTrue(Window.consumed, false)) return null;

			Box startAt = Intersects.startAt(event.after, root);


			if (startAt!=null && !startAt.properties.isTrue(Mouse.isSelected, false))
			{
				FrameManipulation.setSelectionTo(root, Collections.singleton(startAt));
			}

			MenuSpecification m = startAt.find(menuSpecs, upwardsOrDownwards())
				.filter(x -> x != null)
				.map(x -> x.apply(event))
				.filter(x -> x != null)
				.reduce(new MenuSpecification(), (a, b) -> {

					a = a.copy();
					for (Map.Entry<Position, MenuItem> e : b.items.entrySet()) {
						Position k = e.getKey();
						int tries = 0;
						while (a.items.containsKey(k) && tries < Position.values().length) {
							k = Position.values()[(k.ordinal() + 1) % Position.values().length];
							tries++;
						}
						if (tries < Position.values().length) a.items.put(k, e.getValue());
					}

					return a;
				});

			startAt.find(menu, upwardsOrDownwards())
				.flatMap(x -> x.entrySet()
					.stream())
				.forEach(x -> {
					String key = x.getKey();
					String label = key;
					FunctionOfBox v = x.getValue();

					Position p = null;

					if (key.contains("_")) {
						String[] q = key.split("_");
						label = Arrays.asList(q).subList(0, q.length - 1).stream().reduce((a, b) -> a + " " + b).get();

						try {
							p = Position.valueOf(q[q.length - 1].toUpperCase());
							if (p == null) p = Position.N;
						} catch (IllegalArgumentException e) {
						}
					} else {
						p = Position.N;
					}
					int tries = 0;
					while (m.items.containsKey(p) && tries < Position.values().length) {
						p = Position.values()[(p.ordinal() + 1) % Position.values().length];
						tries++;
					}
					if (tries < Position.values().length)
						m.items.put(p, new MenuItem(label, () -> v.apply(startAt)));
				});


			log("debug.markingmenus", () -> "merged spec and got :" + m.items.keySet());

			if (m.items.size() > 0) {
				event.properties.put(Window.consumed, true);

				return runMenu(startAt==null ? root : startAt, new Vec2(event.after.mx, event.after.my), m);
			} else {
				log("debug.markingmenus", () -> " no menuSpecs for event ");
				return null;
			}
		});

		this.properties.putToMap(Keyboard.onKeyDown, "__markingmenus__", (event, key) -> {

			if (event.properties.isTrue(Window.consumed, false)) return null;

			if (!event.after.keysDown.contains(GLFW.GLFW_KEY_SPACE)) return null;
			if (event.before.keysDown.contains(GLFW.GLFW_KEY_SPACE)) return null;
			if (event.after.keysDown.size()!=1) return null;
			if (event.before.keysDown.size()!=0) return null;

			Box startAt = Intersects.startAt(event.after.mouseState, root);


			if (startAt!=null && !startAt.properties.isTrue(Mouse.isSelected, false))
			{
				FrameManipulation.setSelectionTo(root, Collections.singleton(startAt));
			}

			MenuSpecification m = new MenuSpecification();

			startAt.find(spaceMenu, upwardsOrDownwards())
				.flatMap(x -> x.entrySet()
					.stream())
				.forEach(x -> {
					String K = x.getKey();
					String label = K;
					FunctionOfBox v = x.getValue();

					Position p = null;

					if (K.contains("_")) {
						String[] q = K.split("_");
						label = Arrays.asList(q).subList(0, q.length - 1).stream().reduce((a, b) -> a + " " + b).get();

						try {
							p = Position.valueOf(q[q.length - 1].toUpperCase());
							if (p == null) p = Position.N;
						} catch (IllegalArgumentException e) {
						}
					} else {
						p = Position.N;
					}
					int tries = 0;
					while (m.items.containsKey(p) && tries < Position.values().length) {
						p = Position.values()[(p.ordinal() + 1) % Position.values().length];
						tries++;
					}
					if (tries < Position.values().length)
						m.items.put(p, new MenuItem(label, () -> v.apply(startAt)));
				});


			log("debug.markingmenus", () -> "merged spec and got :" + m.items.keySet());

			if (m.items.size() > 0) {
				event.properties.put(Window.consumed, true);

				return runMenuHold(startAt==null ? root : startAt, new Vec2(event.after.mouseState.mx, event.after.mouseState.my), m);
			} else {
				log("debug.markingmenus", () -> " no menuSpecs for event ");
				return null;
			}
		});

	}


	static public Mouse.Dragger runMenu(Box origin, Vec2 center, MenuSpecification m) {

		MarkingMenus menus = origin.first(markingMenus, origin.both())
			.orElseThrow(() -> new IllegalArgumentException(" can't show marking menus if we can't find MarkingMenus"));
		List<Position> hover = menus.show(center, m);
		Mouse.Dragger[] replaceWith = {null};
		return (event, term) -> {
			if (replaceWith[0] != null) {
				return replaceWith[0].update(event, term);
			}

			if (term) {
				menus.hide();
				try {
					if (hover.size() > 0) {
						m.items.get(hover.get(hover.size() - 1)).callback.run();
					} else {
						if (m.nothing != null) m.nothing.run();
					}
				} finally {
					if (m.onExit != null) {
						m.onExit.run();
					}
				}
			} else {
				if (hover.size() > 0) {
					Position h = hover.get(0);
					MenuItem sp = m.items.get(h);
					MenuSpecification sub = sp.submenu;
					if (sub != null) {
						menus.hide();
						log("marking", () -> "going submenu");
						replaceWith[0] = runMenu(origin, new Vec2(event.after.mx, event.after.my), sub);
					}
				}
			}


			return true;
		};
	}

	static public Keyboard.Hold runMenuHold(Box origin, Vec2 center, MenuSpecification m) {

		MarkingMenus menus = origin.first(markingMenus, origin.both())
			.orElseThrow(() -> new IllegalArgumentException(" can't show marking menus if we can't find MarkingMenus"));
		List<Position> hover = menus.show(center, m);
		Keyboard.Hold[] replaceWith = {null};
		return (event, term) -> {

			if (replaceWith[0] != null) {
				return replaceWith[0].update(event, term);
			}

			if (term) {
				menus.hide();
				try {
					if (hover.size() > 0) {
						m.items.get(hover.get(hover.size() - 1)).callback.run();
					} else {
						if (m.nothing != null) m.nothing.run();
					}
				} finally {
					if (m.onExit != null) {
						m.onExit.run();
					}
				}
			} else {
				if (hover.size() > 0) {
					Position h = hover.get(0);
					MenuItem sp = m.items.get(h);
					MenuSpecification sub = sp.submenu;
					if (sub != null) {
						menus.hide();
						log("marking", () -> "going submenu");
						replaceWith[0] = runMenuHold(origin, new Vec2(event.after.mouseState.mx, event.after.mouseState.my), sub);
					}
				}
			}


			return true;
		};
	}

	public List<Position> show(Vec2 center, MenuSpecification m) {
		center.x = (int) center.x;
		center.y = (int) center.y;
		List<Position> over = new ArrayList<Position>();

		TextDrawing t = this.find(TextDrawing.textDrawing, both())
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException(" got to be able to draw text to draw a menuSpecs"));
		TextDrawing.FontSupport defaultFont = t.getDefaultFont();

		float scale = 75;

		FLine textLine = new FLine();
		textLine.attributes.put(hasText, true);
		double maxHeight = 0;


			SimpleVoronoi v = new SimpleVoronoi();
		Map<Position, SimpleVoronoi.Pnt> sites = new HashMap<>();
		for (Map.Entry<Position, MenuItem> e : m.items.entrySet()) {
			sites.put(e.getKey(), v.add(new Vec2(e.getKey().pos.x * scale, e.getKey().pos.y * scale)));
		}
		sites.put(null, v.add(new Vec2(0,0)));




		float outset = 10;

		List<Area> areas = new ArrayList<>();
		FLine centerLine = new FLine();
		float cr = 5;
		centerLine.rect(center.x - cr, center.y - cr, cr * 2, cr * 2);
		this.properties.putToMap(frameDrawing, "center", box -> centerLine);
		centerLine.attributes.put(filled, true);
		centerLine.attributes.put(fillColor, new Vec4(1, 1, 1, 0.9f));
		centerLine.attributes.put(strokeColor, new Vec4(0, 0, 0, 0.1f));

		centerLine.attributes.put(filled, true);

		centerLine.attributes.put(layer, "glass2");

		areas.add(new Area(FLinesAndJavaShapes.flineToJavaShape(centerLine)));

		Map<FLine, FLine> toLabels = new LinkedHashMap<>();
		Map<FLine, FLine> toBoxes = new LinkedHashMap<>();

		for (Map.Entry<Position, MenuItem> e : m.items.entrySet()) {
			textLine.moveTo(center.x + e.getKey().pos.x * scale, center.y + e.getKey().pos.y * scale);

			log("debug.markingmenus", () -> e.getKey() + " " + e.getKey().pos);

			String lab = e.getValue().label;

			if (e.getValue().submenu != null && !lab.endsWith("...")) lab = lab + "...";

			textLine.node().attributes.put(text, lab);
			textLine.attributes.put(layer, "glass2");
			textLine.attributes.put(color, new Vec4(0, 0, 0, 0.75f));

			textLine.attributes.put(color, new Vec4(1, 1, 1, 0.9f));
			textLine.node().attributes.put(textScale, 1.2f);

			maxHeight = Math.max(maxHeight, defaultFont.font.dimensions(e.getValue().label, 0.2f).y);


			{
				double w = defaultFont.font.dimensions(e.getValue().label, 0.2f).x;
				FLine label = new FLine();
				label.rect(center.x + e.getKey().pos.x * scale - w / 2 - outset, center.y + e.getKey().pos.y * scale - maxHeight - outset, w + outset * 2, maxHeight + outset * 2);
				//this.properties.putToMap(FrameDrawer.frameDrawing, "label" + e.getKey(), box -> label);

				areas.add(new Area(FLinesAndJavaShapes.flineToJavaShape(label)));
			}

			FLine fo = v.makeFLine(v.getContourForSite(center, sites.get(e.getKey())));
			FLine f = bound(center, fo);

			this.properties.putToMap(frameDrawing, "contour" + e.getKey(), box -> f);
			this.properties.putToMap(FLineInteraction.interactiveDrawing, "contour" + e.getKey(), box -> f);

			f.attributes.put(strokeColor, new Vec4(0.45f, 0.45f, 0.45f, 0.1f));
			f.attributes.put(fillColor, new Vec4(0.40f, 0.40f, 0.40f, 0.55f));
			f.attributes.put(filled, true);
			f.attributes.put(stroked, false);
			f.attributes.put(layer, "glass2");
			f.attributes.put(thicken, new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));


			toLabels.put(f, textLine);

			f.attributes.putToMap(Mouse.onMouseEnter, "__markingmenu__", (event) -> {
//				FLine fm = v.makeFLine(v.getContourForSite(sites.get(e.getKey())));
//
////				fm = FLinesAndJavaShapes.insetShape(fm, 16);
//
//				f.nodes.clear();
//				f.nodes.addAll(fm.nodes);

//				f.attributes.put(fillColor, new Vec4(1.14f, 0.195f, 0.145f, 0.85f));
//				f.attributes.put(thicken, new BasicStroke(16, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
//				f.attributes.put(strokeColor, new Vec4(0, 0.5f, 0, -0.15f));

				f.attributes.put(filled, true);
				f.attributes.put(stroked, false);

				f.attributes.put(fillColor, new Vec4(Colors.executionColor, 0.15f));
				f.attributes.put(strokeColor, new Vec4(Colors.executionColor, 0.15f));

				toBoxes.entrySet().forEach(x -> {
					if (x.getKey() != f) {
						x.getValue().attributes.put(StandardFLineDrawing.opacity, 0.0f);
						x.getValue().modify();
//						x.getKey().attributes.put(StandardFLineDrawing.opacity, 0.5f);
//						x.getKey().modify();
					} else {
						x.getValue().attributes.put(fillColor, new Vec4(Colors.executionColor, 0.45f));
						x.getValue().attributes.put(strokeColor, new Vec4(1.0f, 1.0f, 1.0f, 0.5f));
						x.getValue().attributes.put(stroked, true);
						x.getValue().modify();
					}
				});

				toLabels.entrySet().forEach(x -> {
					if (x.getKey() != f) {
//						x.getValue().attributes.put(StandardFLineDrawing.opacity, 0.5f);
//						x.getValue().modify();
					} else {

					}
				});

				f.modify();
				over.remove(e.getKey());
				over.add(e.getKey());
				Drawing.dirty(this);
				return null;
			});
			f.attributes.putToMap(Mouse.onMouseExit, "__markingmenu__", (event) -> {
//				FLine fm = v.makeFLine(v.getContourForSite(sites.get(e.getKey())));
//				f.nodes.clear();
//				f.nodes.addAll(fm.nodes);

				f.attributes.put(strokeColor, new Vec4(0.05f, 0.05f, 0.05f, 0.05f));
				f.attributes.put(fillColor, new Vec4(0.40f, 0.40f, 0.40f, 0.55f));
				f.attributes.put(filled, true);
				f.attributes.put(stroked, false);

				toBoxes.entrySet().forEach(x -> {
					if (x.getKey() != f) {
						x.getValue().attributes.put(StandardFLineDrawing.opacity, 0.5f);
						x.getValue().modify();
					} else {
						x.getValue().attributes.put(fillColor, new Vec4(0.16f, 0.16f, 0.16f, 0.7f));
						x.getValue().modify();
						x.getValue().attributes.put(strokeColor, new Vec4(1.0f, 1.0f, 1.0f, 0.3f));
						x.getValue().attributes.put(stroked, false);
					}
				});

				toLabels.entrySet().forEach(x -> {
					if (x.getKey() != f) {
						x.getValue().attributes.remove(StandardFLineDrawing.opacity);
						x.getValue().modify();
					}
				});

				over.remove(e.getKey());
				f.modify();
				Drawing.dirty(this);
			});

			{
				double w = defaultFont.font.dimensions(e.getValue().label, 0.2f).x;
				FLine label = new FLine();
				label.roundedRect(center.x + e.getKey().pos.x * scale - w / 2 - outset, center.y + e.getKey().pos.y * scale - maxHeight - outset, w + outset * 2, maxHeight + outset * 2, 7);
				toBoxes.put(f, label);

				this.properties.putToMap(frameDrawing, "label" + e.getKey(), box -> label);

				areas.add(new Area(FLinesAndJavaShapes.flineToJavaShape(label)));

				label.attributes.put(filled, true);

				label.attributes.put(fillColor, new Vec4(0.16f, 0.16f, 0.16f, 0.7f));
//			label.attributes.put(fillColor, new Vec4(0.16f, 0.16f, 0.16f, 0.88f));
//			label.attributes.put(strokeColor, new Vec4(1, 1, 1, 0.1f));
				label.attributes.put(layer, "glass2");
				label.attributes.put(stroked, false);

//                FLine labelo = label.duplicate();
//
//                labelo.attributes.put(thicken, new BasicStroke(2));
//                labelo.attributes.put(filled, false);
//                labelo.attributes.put(stroked, true);
//
//                this.properties.putToMap(frameDrawing, "label2" + e.getKey(), box -> labelo);

				if (e.getValue().submenu != null) {
					FLine label2 = new FLine();
					label2.rect(center.x + e.getKey().pos.x * scale - w / 2 - outset, center.y + e.getKey().pos.y * scale - maxHeight - outset, w + outset * 2, maxHeight + outset * 2);
//				this.properties.putToMap(frameDrawing, "labelShade" + e.getKey(), box -> label2);

					areas.add(new Area(FLinesAndJavaShapes.flineToJavaShape(label2)));

					label2.attributes.put(filled, true);
//				label2.attributes.put(color, new Vec4(0.8f, 0.8f, 0.8f, -0.3f));

					label2.attributes.put(filled, false);
					label2.attributes.put(stroked, false);

//				label2.attributes.put(thicken, new BasicStroke(16));
					label2.attributes.put(stroked, false);
					label2.attributes.put(layer, "glass2");

				}
			}
		}

		for (Map.Entry<Position, MenuItem> e : m.items.entrySet()) {
			FLine connective = new FLine();
			connective.moveTo(center.x, center.y);
			connective.lineTo(center.x + e.getKey().pos.x * scale, center.y + e.getKey().pos.y * scale);
			Shape s = new BasicStroke(2.5f).createStrokedShape(FLinesAndJavaShapes.flineToJavaShape(connective));
			Area as = new Area(s);
			for (Area aa : areas) {
				as.subtract(aa);
			}
			FLine connective2 = FLinesAndJavaShapes.javaShapeToFLine(as);
			this.properties.putToMap(frameDrawing, "connective" + e.getKey(), box -> connective2);

			connective2.attributes.put(stroked, false);
			connective2.attributes.put(filled, true);
			connective2.attributes.put(fillColor, new Vec4(0.07f, 0.07f, 0.07f, 0.95f));
			connective2.attributes.put(layer, "glass2");

		}
		this.properties.putToMap(frameDrawing, "menuSpecs", box -> textLine);
		Drawing.dirty(this);

		return over;
	}

	private FLine bound(Vec2 center, FLine fo) {

		Drawing d = find(Drawing.drawing, both()).findFirst().get();
		Rect b = d.getCurrentViewBounds(this).inset(10);

		Shape s1 = FLinesAndJavaShapes.flineToJavaShape(fo);
		Shape s2 = FLinesAndJavaShapes.flineToJavaShape(new FLine().circle(center.x, center.y, Math.min(b.w, b.h) / 2));

		Area a1 = new Area(s1);
		Area a2 = new Area(s2);

		a1.intersect(a2);

		return FLinesAndJavaShapes.javaShapeToFLine(a1);
	}

	public void hide() {
		this.properties.remove(frameDrawing);
		this.properties.remove(FLineInteraction.interactiveDrawing);
		Drawing.dirty(this);
	}


	public enum Position {
		N(0, -1, 1), N2(0, -1, 2), NE2(1, -1, 2), NE(1, -1, 1), NEH(1, -1, 0.5f), EH(1, 0, 0.5f), E(1, 0, 1), E2(1, 0, 2), SE2(1, 1, 2), SE(1, 1, 1), SEH(1, 1, 0.5f),
		SH(0, 1, 0.5f), S(0, 1, 1), S2(0, 1, 2), SW2(-1, 1, 2), SW(-1, 1, 1), SWH(-1, 1, 0.5f), WH(-1, 0, 0.5f), W(-1, 0, 1), W2(-1, 0, 2), NW2(-1, -1, 2), NW(-1, -1, 1),
		NWH(-1, -1, 0.5f), NH(0, -1, 0.5f);

		final public Vec2 pos;

		Position(float x, float y, float length) {
			this.pos = new Vec2(x, y).normalize()
				.mul(length);
			this.pos.x *= 1.5f;
		}
	}

	static public class MenuSpecification {
		public Map<Position, MenuItem> items = new LinkedHashMap<>();

		public Runnable nothing = null;
		public Runnable onExit = null;

		public MenuSpecification copy() {
			MenuSpecification m = new MenuSpecification();
			m.items.putAll(items);
			m.nothing = nothing;
			m.onExit = onExit;
			return m;
		}
	}

	static public class MenuItem {
		String label;
		Runnable callback;

		MenuSpecification submenu = null;

		public MenuItem(String label, Runnable c) {
			this.label = label;
			this.callback = c;
		}

		public MenuItem setSubmenu(MenuSpecification submenu) {
			this.submenu = submenu;
			return this;
		}
	}


}
