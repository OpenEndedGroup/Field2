package fieldbox.boxes;


import field.graphics.FLine;
import field.graphics.MeshBuilder;
import static fieldbox.boxes.StandardFLineDrawing.*;
import field.linalg.Vec2;
import field.linalg.Vec3;
import field.linalg.Vec4;
import field.utility.*;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Fundamental drawing support for Boxes
 * <p>
 * This is the ingest side of FLine drawing system --- the Field vector drawing framework. FLines are receptacles for geometry, both lines,
 * tessellated shapes and text, and when added to certain properties of Boxes they will appear inside the FieldBoxWindow. By setting properties on the
 * FLines, you can control their appearance and behavior.
 * <p>
 * Two properties are observed by this plugin. "frameDrawing" and "lines".
 * <p>
 * "lines" --- signature: this is just a Map or List containing either FLines or Supplier<FLine>. These are lines that are drawen with this box.
 * <p>
 * "frameDrawing" --- has a more complex signature: Map<String, Function<Box, FLine>>. That's a string key'd Map of functions that take Boxes and
 * return an FLine. All Keys that start with "_" are reserved by Field. This signature is often easier to write caching strategies for. For example,
 * in the case where you want to draw the frame of a box, if the frame hasn't changed, and the box's selection status hasn't changed, there's no need
 * to recompute the FLine, just return the old one. The graphics system can optimize FLines very aggressively. In the case where absolutely all of the
 * same FLines are added to a MeshBuilder during an animation cycle then no data ends up being uploaded to OpenGL whatsoever and the number of state
 * changes is greatly reduced. For a helper class for this kind of caching see the inner class Cached below.
 * <p>
 * If a box has a "frame" property (i.e. typically if it isn't a plugin) and it has a blank "frameDrawing" then this plugin will give it a default
 * look. The standard Field box look --- that's a name in the middle, a light grey gradient box, and a construction site selection trim.
 */
public class FLineDrawing extends Box implements Drawing.Drawer {

	static public final Dict.Prop<Map<String, Function<Box, FLine>>> frameDrawing = new Dict.Prop<>("frameDrawing").type().toCannon()
		    .doc("Functions that compute lines to be drawn along with this box");
	static public final Dict.Prop<LinkedHashMapAndArrayList<Supplier<FLine>>> lines = new Dict.Prop<>("lines").type().toCannon()
		    .doc("Geometry to be drawn along with this box");

	static public final Dict.Prop<Boolean> dirty = new Dict.Prop<>("dirty").type().toCannon()
		    .doc("set _.dirty=1 to cause a repaint of the window on the next animation cycle");


	static public final Dict.Prop<String> layer = new Dict.Prop<>("layer").type().toCannon()
		    .doc("which layer to draw to? Defaults to __main__, the other alternative right now is 'glass' to draw on the blur layer above Field");

	public FLineDrawing() {
		this.properties.putToList(Drawing.drawers, this);

		// allow things like _.dirty=1 to cause redraw next frame
		this.first(Watches.watches).map(w -> w.addWatch(dirty, (x) -> {
			if (Util.truthy(x.third)) {
				x.second.properties.remove(x.first);
				Drawing.dirty(x.second);
			}
		}));
	}


	@Override
	public void draw(Drawing context) {

		this.breadthFirst(this.both()).forEach(x -> {
			Rect r = x.properties.get(frame);

			Map<String, Function<Box, FLine>> drawing = x.properties.computeIfAbsent(frameDrawing, this::defaultdrawsLines);

			List<FLine> all = new ArrayList<>();
			Iterator<Function<Box, FLine>> it = drawing.values().iterator();
			while (it.hasNext()) {
				Function<Box, FLine> f = it.next();
				FLine fl = f.apply(x);
				if (fl == null) it.remove();
				else all.add(fl);
			}

			drawing.values().stream().map(c -> c.apply(x)).filter(fline -> fline != null).collect(Collectors.toList()).forEach(fline -> dispatchLine(fline, context));


			Map<String, Supplier<FLine>> ll = x.properties.computeIfAbsent(lines, (k) -> new LinkedHashMapAndArrayList<>());

			all = new ArrayList<>();
			Iterator<Supplier<FLine>> it2 = ll.values().iterator();
			while (it.hasNext()) {
				Supplier<FLine> f = it2.next();
				FLine fl = f.get();
				if (fl == null) it.remove();
				else all.add(fl);
			}

			ll.values().stream().map(c -> c.get()).filter(fline -> fline != null).forEach(fline -> dispatchLine(fline, context));

		});
	}

	protected void dispatchLine(FLine fline, Drawing context) {

		String layerName = fline.attributes.getOr(layer, () -> "__main__");

		MeshBuilder line = context.getLine(layerName);
		MeshBuilder mesh = context.getMesh(layerName);
		MeshBuilder points = context.getPoints(layerName);
		Optional<TextDrawing> text = first(TextDrawing.textDrawing, both());


		StandardFLineDrawing.dispatchLine(fline, mesh, line, points, text, layerName);

	}


	protected Map<String, Function<Box, FLine>> defaultdrawsLines(Dict.Prop<Map<String, Function<Box, FLine>>> k) {
		Map<String, Function<Box, FLine>> r = new LinkedHashMap<>();

		r.put("__outline__", new Cached<Box, Object, FLine>((box, previously) -> {
			Rect rect = box.properties.get(frame);
			if (rect == null) return null;

			boolean selected = box.properties.isTrue(Mouse.isSelected, false);

			FLine f = new FLine();
			if (selected) rect = rect.inset(8f);
			else rect = rect.inset(-0.5f);

			f.moveTo(rect.x, rect.y);
			f.lineTo(rect.x + rect.w, rect.y);
			f.lineTo(rect.x + rect.w, rect.y + rect.h);
			f.lineTo(rect.x, rect.y + rect.h);
			f.lineTo(rect.x, rect.y);

			f.attributes.put(strokeColor, selected ? new Vec4(0, 0, 0, -1.0f) : new Vec4(0, 0, 0, 0.5f));

			f.attributes.put(thicken, new BasicStroke(selected ? 16 : 1.5f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));

			f.attributes.put(stroked, true);

			return f;
		}, (box) -> new Pair(box.properties.get(frame), box.properties.get(Mouse.isSelected))));

		r.put("__outlineFill__", new Cached<Box, Object, FLine>((box, previously) -> {
			Rect rect = box.properties.get(frame);
			if (rect == null) return null;

			boolean selected = box.properties.isTrue(Mouse.isSelected, false);

			float a = selected ? 0.9f / 3 : 0.8f / 3;
			float b = selected ? 0.75f / 3 : 0.75f / 3;
			float s = selected ? 0.5f : 0.6f;

			a = 1;
			b = 0.88f;

			FLine f = new FLine();
			f.moveTo(rect.x, rect.y);
			f.nodes.get(f.nodes.size() - 1).attributes.put(fillColor, new Vec4(a, a, a, s));
			f.lineTo(rect.x + rect.w, rect.y);
			f.nodes.get(f.nodes.size() - 1).attributes.put(fillColor, new Vec4(b, b, b, s));
			f.lineTo(rect.x + rect.w, rect.y + rect.h);
			f.nodes.get(f.nodes.size() - 1).attributes.put(fillColor, new Vec4(a, a, a, s));
			f.lineTo(rect.x, rect.y + rect.h);
			f.nodes.get(f.nodes.size() - 1).attributes.put(fillColor, new Vec4(a, a, a, s));
			f.lineTo(rect.x, rect.y);
			f.nodes.get(f.nodes.size() - 1).attributes.put(fillColor, new Vec4(a, a, a, s));

			f.attributes.put(filled, true);

			Map<Integer, String> customFill = new LinkedHashMap<Integer, String>();
			customFill.put(1, "fillColor");
			f.setAuxProperties(customFill);


			return f;
		}, (box) -> new Pair(box.properties.get(frame), box.properties.get(Mouse.isSelected))));

		r.put("__name__", new Cached<Box, Object, FLine>((box, previously) -> {
			Rect rect = box.properties.get(frame);
			if (rect == null) return null;

			boolean selected = box.properties.isTrue(Mouse.isSelected, false);

			FLine f = new FLine();
			f.moveTo(rect.x + rect.w / 2, rect.y + rect.h / 2 + 36 / 5.0f);

			f.attributes.put(hasText, true);
			f.attributes.put(color, new Vec4(0, 0, 0, 0.75f));
			f.nodes.get(f.nodes.size() - 1).attributes.put(text, box.properties.get(Box.name));

			return f;
		}, (box) -> new Pair(box.properties.get(frame), box.properties.get(Box.name))));

		// todo

		return r;
	}


	static public Function<Box, FLine> boxOrigin(Function<Box, FLine> wrap, Vec2 origin) {
		return new Cached<Box, Object, FLine>((box, previously) -> {
			Rect frame = box.properties.get(Box.frame);
			Vec2 o = new Vec2(frame.x + frame.w * origin.x, frame.y + frame.h * origin.y);
			return wrap.apply(box).byTransforming((pos) -> new Vec3(pos.x + o.x, pos.y + o.y, pos.z));
		}, (box) -> box.properties.get(frame));
	}

	static public Function<Box, FLine> boxScale(Function<Box, FLine> wrap, Vec2 origin) {
		return new Cached<Box, Object, FLine>((box, previously) -> {
			Rect frame = box.properties.get(Box.frame);
			return wrap.apply(box).byTransforming((pos) -> new Vec3(frame.x + pos.x * frame.w, frame.y + pos.y * frame.h, pos.z));
		}, (box) -> box.properties.get(frame));
	}

	static public Function<Box, FLine> expires(Function<Box, FLine> wrap, int updates) {
		int[] counter = {updates};
		return box -> {
			if (updates < 0) return wrap.apply(box);
			FLine f = wrap.apply(box);
			f.attributes.multiply(opacity, 1, counter[0] / (float) updates);
			f.modify();
			Drawing.dirty(box);
			return counter[0]-- < 0 ? null : f;
		};
	}

	static public Function<Box, FLine> expires(Function<Box, FLine> wrap, int updates, float power) {
		int[] counter = {updates};
		return box -> {
			if (updates < 0) return wrap.apply(box);
			FLine f = wrap.apply(box);
			f.attributes.multiply(opacity, 1, (float) Math.pow(counter[0] / (float) updates, power));
			f.modify();
			Drawing.dirty(box);
			return counter[0]-- < 0 ? null : f;
		};
	}


}
