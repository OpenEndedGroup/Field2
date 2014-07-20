package fieldbox.boxes;

import field.graphics.FLine;
import field.graphics.FLinesAndJavaShapes;
import field.graphics.Window;
import field.linalg.Vec2;
import field.linalg.Vec4;
import field.utility.Dict;
import field.utility.Log;
import field.utility.SimpleVoronoi;

import java.awt.*;
import java.awt.geom.Area;
import java.util.*;
import java.util.List;
import java.util.function.Function;
import static fieldbox.boxes.StandardFLineDrawing.*;
import static fieldbox.boxes.FLineDrawing.*;

/**
 * Plugin: MarkingMenus adds support for building and showing radial menus for boxes and for the canvas itself to Field.
 * <p>
 * Menus are built up from coalescing MenuSpecifications from the "menu" property (signature: Function<Window.Event<Window.MouseState>,
 * MenuSpecification>). That puts labels and callbacks at compass points (enum Position), and designs a Voronoi tesselation of the space between them.
 * The idea is to allow quick gestures from the mouse or touchpad to access menu items via muscle memory.
 */
public class MarkingMenus extends Box {

	static public Dict.Prop<MarkingMenus> markingMenus = new Dict.Prop<>("markingMenus");
	static public Dict.Prop<Function<Window.Event<Window.MouseState>, MenuSpecification>> menu = new Dict.Prop<>("menu");

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

	static public enum Position {
		N(0, -1, 1), N2(0, -1, 2), NE2(1, -1, 2), NE(1, -1, 1), NEH(1, -1, 0.5f), EH(1, 0, 0.5f), E(1, 0, 1), E2(1, 0, 2), SE2(1, 1, 2), SE(1, 1, 1), SEH(1, 1, 0.5f),
		SH(0, 1, 0.5f), S(0, 1, 1), S2(0, 1, 2), SW2(-1, 1, 2), SW(-1, 1, 1), SWH(-1, 1, 0.5f), WH(-1, 0, 0.5f), W(-1, 0, 1), W2(-1, 0, 2), NW2(-1, -1, 2), NW(-1, -1, 1),
		NWH(-1, -1, 0.5f), NH(0, -1, 0.5f);

		final public Vec2 pos;

		Position(float x, float y, float length) {
			this.pos = new Vec2(x, y).normalise().scale(length);
			this.pos.x *= 1.5f;
		}
	}

	static public class MenuItem {
		String label;
		Runnable callback;

		public MenuItem(String label, Runnable c) {
			this.label = label;
			this.callback = c;
		}
	}

	public MarkingMenus(Box root_unused) {

		this.properties.put(markingMenus, this);
		this.properties.putToList(Mouse.onMouseDown, (event, button) -> {
			if (button != 1) return null;

			MenuSpecification m = find(menu, both()).filter(x -> x != null).map(x -> x.apply(event)).filter(x -> x != null)
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

			Log.log("debug.markingmenus", "merged spec and got :" + m.items.keySet());

			if (m.items.size() > 0) {
				return runMenu(this, convertCoordinateSystem(new Vec2(event.after.x, event.after.y)), m);
			} else {
				Log.log("debug.markingmenus"," no menu for event ");
				return null;
			}
		});

	}

	public Vec2 convertCoordinateSystem(Vec2 event) {
		Optional<Drawing> drawing = this.find(Drawing.drawing, both()).findFirst();
		return drawing.map(x -> x.windowSystemToDrawingSystem(event))
			    .orElseThrow(() -> new IllegalArgumentException(" cant mouse around something without drawing support (to provide coordinate system)"));
	}


	public List<Position> show(Vec2 center, MenuSpecification m) {
		center.x = (int) center.x;
		center.y = (int) center.y;
		List<Position> over = new ArrayList<Position>();

		TextDrawing t = this.find(TextDrawing.textDrawing, both()).findFirst()
			    .orElseThrow(() -> new IllegalArgumentException(" got to be able to draw text to draw a menu"));
		TextDrawing.FontSupport defaultFont = t.getDefaultFont();

		float scale = 100;

		FLine textLine = new FLine();
		textLine.attributes.put(hasText, true);
		float maxHeight = 0;

		for (Map.Entry<Position, MenuItem> e : m.items.entrySet()) {
			textLine.moveTo(center.x + e.getKey().pos.x * scale, center.y + e.getKey().pos.y * scale);

			Log.log("debug.markingmenus", () -> e.getKey() + " " + e.getKey().pos);

			textLine.node().attributes.put(text, e.getValue().label);
			textLine.attributes.put(layer, "glass");
			textLine.attributes.put(color, new Vec4(0, 0, 0, 0.75f));
			maxHeight = Math.max(maxHeight, defaultFont.font.dimensions(e.getValue().label, 0.2f).y);
		}

		float outset = 8;

		List<Area> areas = new ArrayList<>();

		for (Map.Entry<Position, MenuItem> e : m.items.entrySet()) {
			float w = defaultFont.font.dimensions(e.getValue().label, 0.2f).x;

			FLine label = new FLine();
			label.rect(center.x + e.getKey().pos.x * scale - w / 2 - outset, center.y + e
				    .getKey().pos.y * scale - maxHeight - outset, w + outset * 2, maxHeight + outset * 2);
			//this.properties.putToMap(FrameDrawer.frameDrawing, "label" + e.getKey(), box -> label);

			areas.add(new Area(FLinesAndJavaShapes.flineToJavaShape(label)));

		}

		FLine centerLine = new FLine();
		float cr = 5;
		centerLine.rect(center.x - cr, center.y - cr, cr * 2, cr * 2);
		this.properties.putToMap(frameDrawing, "center", box -> centerLine);
		centerLine.attributes.put(filled, true);
		centerLine.attributes.put(fillColor, new Vec4(1, 1, 1, 0.4f));
		centerLine.attributes.put(strokeColor, new Vec4(0, 0, 0, 0.1f));
		centerLine.attributes.put(layer, "glass");

		areas.add(new Area(FLinesAndJavaShapes.flineToJavaShape(centerLine)));

		for (Map.Entry<Position, MenuItem> e : m.items.entrySet()) {
			FLine connective = new FLine();
			connective.moveTo(center.x, center.y);
			connective.lineTo(center.x + e.getKey().pos.x * scale, center.y + e.getKey().pos.y * scale);
			Shape s = new BasicStroke(4).createStrokedShape(FLinesAndJavaShapes.flineToJavaShape(connective));
			Area as = new Area(s);
			for (Area aa : areas) {
				as.subtract(aa);
			}
			FLine connective2 = FLinesAndJavaShapes.javaShapeToFLine(as);
			this.properties.putToMap(frameDrawing, "connective" + e.getKey(), box -> connective2);

			connective2.attributes.put(stroked, false);
			connective2.attributes.put(filled, true);
			connective2.attributes.put(fillColor, new Vec4(0, 0, 0, 0.7f));
			connective2.attributes.put(layer, "glass");

		}

		SimpleVoronoi v = new SimpleVoronoi();
		Map<Position, SimpleVoronoi.Pnt> sites = new HashMap<>();
		for (Map.Entry<Position, MenuItem> e : m.items.entrySet()) {
			sites.put(e.getKey(), v.add(new Vec2(center.x + e.getKey().pos.x * scale, center.y + e.getKey().pos.y * scale)));
		}
		sites.put(null, v.add(center));

		for (Map.Entry<Position, MenuItem> e : m.items.entrySet()) {
			FLine f = v.makeFLine(v.getContourForSite(sites.get(e.getKey())));
			this.properties.putToMap(frameDrawing, "contour" + e.getKey(), box -> f);
			this.properties.putToMap(FLineInteraction.interactiveLines, "contour" + e.getKey(), box -> f);
			f.attributes.put(strokeColor, new Vec4(0.15f, 0.15f, 0.15f, 0.5f));
			f.attributes.put(filled, true);
			f.attributes.put(fillColor, new Vec4(0, 0.0f, 0, 0.15f));
			f.attributes.put(layer, "glass");
			f.attributes.put(thicken, new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			f.attributes.putToList(Mouse.onMouseEnter, (event) -> {
				FLine fm = v.makeFLine(v.getContourForSite(sites.get(e.getKey())));

//				fm = FLinesAndJavaShapes.insetShape(fm, 16);

				f.nodes.clear();
				f.nodes.addAll(fm.nodes);

				f.attributes.put(fillColor, new Vec4(0, 0.5f, 0, 0.25f));
				f.attributes.put(thicken, new BasicStroke(16, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
				f.attributes.put(strokeColor, new Vec4(0, 0.5f, 0, -0.15f));
				f.modify();
				over.remove(e.getKey());
				over.add(e.getKey());
				Drawing.dirty(this);
				return null;
			});
			f.attributes.putToList(Mouse.onMouseExit, (event) -> {
				FLine fm = v.makeFLine(v.getContourForSite(sites.get(e.getKey())));
				f.nodes.clear();
				f.nodes.addAll(fm.nodes);

				f.attributes.put(fillColor, new Vec4(0, 0.0f, 0, 0.05f));
				f.attributes.put(thicken, new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
				f.attributes.put(strokeColor, new Vec4(0, 0, 0, 0.5f));
				over.remove(e.getKey());
				f.modify();
				Drawing.dirty(this);
			});
		}

		FLine f = v.makeFLine(v.getContourForSite(sites.get(null)));
		this.properties.putToMap(frameDrawing, "contour" + null, box -> f);
		f.attributes.put(strokeColor, new Vec4(0, 0, 0, 0.1f));
		f.attributes.put(filled, false);
		f.attributes.put(layer, "glass");

		for (Map.Entry<Position, MenuItem> e : m.items.entrySet()) {
			float w = defaultFont.font.dimensions(e.getValue().label, 0.2f).x;

			FLine label = new FLine();
			label.rect(center.x + e.getKey().pos.x * scale - w / 2 - outset, center.y + e
				    .getKey().pos.y * scale - maxHeight - outset, w + outset * 2, maxHeight + outset * 2);
			this.properties.putToMap(frameDrawing, "label" + e.getKey(), box -> label);

			areas.add(new Area(FLinesAndJavaShapes.flineToJavaShape(label)));

			label.attributes.put(filled, true);
			label.attributes.put(fillColor, new Vec4(0.8f, 0.8f, 0.8f, 0.9f));
			label.attributes.put(strokeColor, new Vec4(0, 0, 0, 0.9f));
			label.attributes.put(layer, "glass");
		}

		this.properties.putToMap(frameDrawing, "menu", box -> textLine);
		Drawing.dirty(this);

		return over;
	}


	static public Mouse.Dragger runMenu(Box origin, Vec2 center, MenuSpecification m) {
		MarkingMenus menus = origin.first(markingMenus, origin.both())
			    .orElseThrow(() -> new IllegalArgumentException(" can't show marking menus if we can't find MarkingMenus"));
		List<Position> hover = menus.show(center, m);
		return (event, term) -> {
			if (term) {
				menus.hide();
				try {
					if (hover.size() > 0) {
						m.items.get(hover.get(hover.size() - 1)).callback.run();
					} else {
						if (m.nothing != null) m.nothing.run();
					}
				} finally {
					if (m.onExit != null)
					{
						m.onExit.run();
					}
				}
			}
			return true;
		};
	}


	public void hide() {
		this.properties.remove(frameDrawing);
		this.properties.remove(FLineInteraction.interactiveLines);
		Drawing.dirty(this);
	}


}
