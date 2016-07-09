package fieldbox.boxes;


import field.graphics.*;
import field.linalg.Mat4;
import field.linalg.Vec2;
import field.linalg.Vec3;
import field.linalg.Vec4;
import field.utility.*;
import fieldbox.boxes.plugins.FileBrowser;
import fieldbox.boxes.plugins.Planes;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static field.graphics.StandardFLineDrawing.*;

/**
 * Fundamental drawing support for Boxes
 * <p>
 * This is the ingest side of FLine drawing system --- the Field vector drawing framework. FLines are receptacles for geometry, both lines, tessellated shapes and text, and when added to certain
 * properties of Boxes they will appear inside the FieldBoxWindow. By setting properties on the FLines, you can control their appearance and behavior.
 * <p>
 * Two properties are observed by this plugin. "frameDrawing" and "lines".
 * <p>
 * "lines" --- signature: this is just a Map or List containing either FLines or Supplier<FLine>. These are lines that are drawn with this box. It's handily auto-constructed to be a map where you can
 * do things like:
 * <p>
 * _.lines.banana = myGreatFLine
 * <p>
 * This is more helpful than writing _.lines.add(myGreatFLine) if you end up repeatedly executing this code.
 * <p>
 * <p>
 * "frameDrawing" --- has a more complex signature: Map<String, Function<Box, FLine>>. That's a string key'd Map of functions that take Boxes and return an FLine. All Keys that start with "_" are
 * reserved by Field. This signature is often easier to write caching strategies for. For example, in the case where you want to draw the frame of a box, if the frame hasn't changed, and the box's
 * selection status hasn't changed, there's no need to recompute the FLine, just return the old one. The graphics system can optimize FLines very aggressively. In the case where absolutely all of the
 * same FLines are added to a MeshBuilder during an animation cycle then no data ends up being uploaded to OpenGL whatsoever and the number of state changes is greatly reduced. For a helper class for
 * this kind of caching see the inner class Cached below.
 * <p>
 * If a box has a "frame" property (i.e. typically if it isn't a plugin) and it has a blank "frameDrawing" then this plugin will give it a default look. The standard Field box look --- that's a name
 * in the middle, a light grey gradient box, and a construction site selection trim.
 */
public class FLineDrawing extends Box implements Drawing.Drawer {

	static public final Dict.Prop<Map<String, Function<Box, FLine>>> frameDrawing = new Dict.Prop<>("frameDrawing").type()
		.toCannon()
		.doc("Functions that compute lines to be drawn along with this box");

	static public final Dict.Prop<IdempotencyMap<Supplier<FLine>>> lines = new Dict.Prop<>("lines").type()
		.toCannon()
		.doc("Geometry to be drawn along with this box")
		.autoConstructs(() -> new IdempotencyMap<>(Supplier.class));


	static public final Dict.Prop<IdempotencyMap<Supplier<Collection<Supplier<FLine>>>>> bulkLines = new Dict.Prop<>("bulkLines").type()
		.toCannon()
		.doc("Geometry to be drawn along with this box")
		.autoConstructs(() -> new IdempotencyMap<>(Supplier.class));

	static public final Dict.Prop<String> layer = new Dict.Prop<>("layer").type()
		.toCannon()
		.doc("which layer to draw to? Defaults to `__main__`, the other alternative right now is `__glass__` to draw on the blur layer above Field");
	private final Box root;

	public FLineDrawing(Box root) {
		this.root = root;
		this.properties.putToList(Drawing.drawers, this);
	}

	static public Function<Box, FLine> boxOrigin(Function<Box, FLine> wrap, Vec2 origin) {
		return new Cached<Box, Object, FLine>((box, previously) -> {
			Rect frame = box.properties.get(Box.frame);
			Vec2 o = new Vec2(frame.x + frame.w * origin.x, frame.y + frame.h * origin.y);
			return wrap.apply(box)
				.byTransforming((pos) -> new Vec3(pos.x + o.x, pos.y + o.y, pos.z));
		}, (box) -> new Pair<>(box.properties.get(frame), box.properties.isTrue(Mouse.isSelected, false)));
	}

	static public Supplier<FLine> boxOrigin(Supplier<FLine> wrap, Vec2 origin, Box inside) {
		return new Cached<Box, Object, FLine>((box, previously) -> {
			Rect frame = box.properties.get(Box.frame);
			Vec2 o = new Vec2(frame.x + frame.w * origin.x, frame.y + frame.h * origin.y);
			return wrap.get()
				.byTransforming((pos) -> new Vec3(pos.x + o.x, pos.y + o.y, pos.z));
		}, (box) -> new Pair<>(box.properties.get(frame), box.properties.isTrue(Mouse.isSelected, false))).toSupplier(() -> inside);
	}

	static public Function<Box, FLine> boxScale(Function<Box, FLine> wrap) {
		return new Cached<Box, Object, FLine>((box, previously) -> {
			Rect frame = box.properties.get(Box.frame);
			if (frame == null) return null;

			return wrap.apply(box)
				.byTransforming((pos) -> new Vec3(frame.x + pos.x * frame.w, frame.y + pos.y * frame.h, pos.z));
		}, (box) -> new Pair<>(box.properties.get(frame), box.properties.isTrue(Mouse.isSelected, false)));
	}

	static public Function<Box, FLine> windowOrigin(Function<Box, FLine> wrap) {
		return new Cached<Box, Object, FLine>((box, previously) -> {

			Rect v = box.find(Drawing.drawing, box.upwards())
				.findFirst()
				.get()
				.getCurrentViewBounds(box);

			return wrap.apply(box)
				.byTransforming((pos) -> new Vec3(v.x + v.w / 2 + pos.x, v.y + v.h / 2 + pos.y, pos.z));
		}, (box) -> box.find(Drawing.drawing, box.upwards())
			.findFirst()
			.get()
			.getCurrentViewBounds(box));
	}


	static public Supplier<FLine> camera(Supplier<FLine> wrap, Camera camera, Vec2 scale, Vec2 center) {
		Mat4 p = camera.projectionMatrix(0);
		Mat4 v = camera.view();

		return new Cached<Camera, Object, FLine>((c, was) -> {
			Mat4 o = Mat4.mul(p, v, new Mat4());
			o.transpose();
			return wrap.get()
				.byTransforming(x -> {
					Vec3 q = Mat4.transform(o, x, new Vec3());
					q.x *= scale.x;
					q.y *= scale.y;
					q.x += center.x;
					q.y += center.y;
					q.z = 0;
					return q;
				});
		}, (c) -> new Pair<Camera, Camera.State>(camera, camera.getState())).toSupplier(() -> camera);
	}

	static public Function<Box, FLine> expires(Function<Box, FLine> wrap, int updates) {
		int[] counter = {updates};
		return box -> {
			if (updates < 0) return wrap.apply(box);
			FLine f = wrap.apply(box);
			String l = f.attributes.getOr(layer, () -> "__main__");
			f.attributes.multiply(opacity, 1, counter[0] / (float) updates);
			f.modify();
			Drawing.dirty(box, l);
			return counter[0]-- == 0 ? null : f;
		};
	}

	static public Function<Box, FLine> expires(Function<Box, FLine> wrap, int updates, Runnable done) {
		int[] counter = {updates};
		return box -> {
			if (updates < 0) return wrap.apply(box);
			FLine f = wrap.apply(box);
			String l = f.attributes.getOr(layer, () -> "__main__");
			f.attributes.multiply(opacity, 1, counter[0] / (float) updates);
			f.modify();
			if (counter[0] >= 0) Drawing.dirty(box, l);
			if (--counter[0] == 0) done.run();
			return counter[0] == 0 ? null : f;
		};
	}

	static public Function<Box, FLine> expires(Function<Box, FLine> wrap, int updates, Function<Integer, Double> opacityCurve, Runnable done) {
		int[] counter = {updates};
		float[] lf = {-1};
		return box -> {
			if (updates < 0) return wrap.apply(box);
			FLine f = wrap.apply(box);
			String l = f.attributes.getOr(layer, () -> "__main__");
			float ff = opacityCurve.apply(counter[0])
				.floatValue();

			f.attributes.multiply(opacity, 1, ff);
			f.modify();
			if (counter[0] >= 0 || ff != lf[0]) Drawing.dirty(box, l);
			lf[0] = ff;
			if (--counter[0] == 0) done.run();
			return counter[0] == 0 ? null : f;
		};
	}

	static public Function<Box, FLine> expires(Function<Box, FLine> wrap, int updates, float power) {
		int[] counter = {updates};
		return box -> {
			if (updates < 0) return wrap.apply(box);
			FLine f = wrap.apply(box);
			String l = f.attributes.getOr(layer, () -> "__main__");
			f.attributes.multiply(opacity, 1, (float) Math.pow(counter[0] / (float) updates, power));
			f.modify();
			if (counter[0] >= 0) Drawing.dirty(box, l);
			return --counter[0] == 0 ? null : f;
		};
	}

	// we need to be able to assign blame and propagate exceptions into callbacks
	@Override
	public void draw(DrawingInterface context) {

		Util.Errors error = new Util.Errors();
		Optional<TextDrawing> text = context.getTextDrawing(this);

		this.breadthFirst(this.both())
			.forEach(Util.wrap(x -> {
				if (Planes.on(root, x)<=0) {
					return;
				}

//			    System.out.println(" lines for :"+x);

				Log.log("drawing.trace", () -> "lines for " + x);

				if (x.properties.isTrue(Box.hidden, false)) return;

				String defaultLayer = x.properties.getOr(layer, () -> "__main__");

				Map<String, Function<Box, FLine>> drawing = x.properties.computeIfAbsent(frameDrawing, this::defaultdrawsLines);


				List<FLine> all = new ArrayList<>();
				Iterator<Function<Box, FLine>> it = drawing.values()
					.iterator();
				while (it.hasNext()) {
					Function<Box, FLine> f = it.next();
					FLine fl = f.apply(x);
					if (fl == null) it.remove();
					else all.add(fl);
				}

				Log.log("drawing.trace", () -> " --> " + drawing);

				drawing.values()
					.stream()
					.map(c -> c.apply(x))
					.filter(fline -> fline != null)
					.collect(Collectors.toList())
					.forEach(fline -> dispatchLine(fline, context, text, defaultLayer));
				Map<String, Supplier<FLine>> ll = x.properties.computeIfAbsent(lines, (k) -> new IdempotencyMap<>(Supplier.class));

				all = new ArrayList<>();
				Iterator<Supplier<FLine>> it2 = ll.values()
					.iterator();
				while (it2.hasNext()) {
					Supplier<FLine> f = it2.next();
					FLine fl = f.get();
					if (fl == null) it2.remove();
					else all.add(fl);
				}

				Log.log("drawing.trace", () -> " --> " + ll);

				ll.values()
					.stream()
					.map(c -> c.get())
					.filter(fline -> fline != null)
					.forEach(fline -> dispatchLine(fline, context, text, defaultLayer));


				Map<String, Supplier<Collection<Supplier<FLine>>>> bl = x.properties.get(bulkLines);

				if (bl != null) {
					all = new ArrayList<>();
					Iterator<Supplier<Collection<Supplier<FLine>>>> it3 = bl.values()
						.iterator();
					while (it3.hasNext()) {
						Supplier<Collection<Supplier<FLine>>> f = it3.next();
						Collection<Supplier<FLine>> fl = f.get();
						if (fl == null) it3.remove();
						else {
							final List<FLine> finalAll = all;
							fl.stream()
								.map(z -> z.get())
								.filter(z -> z != null)
								.forEach(z -> finalAll.add(z));
						}
					}

					final List<FLine> finalAll = all;
					Log.log("drawing.trace", () -> " --> " + finalAll);

					all.forEach(fline -> dispatchLine(fline, context, text, defaultLayer));
				}
				Log.log("drawing.trace", () -> "lines for " + x + " finished");

			}, error));

		if (error.hasErrors()) {
			error.getErrors()
				.stream()
				.forEach(x -> {
					System.err.println("exception caused by :" + x.second);
					x.first.printStackTrace();
				});
		}
	}

	protected void dispatchLine(FLine fline, DrawingInterface context, Optional<TextDrawing> text) {
		dispatchLine(fline, context, text, "__main__");
	}

	protected void dispatchLine(FLine fline, DrawingInterface context, Optional<TextDrawing> text, String defaultLayer) {
		String layerName = fline.attributes.getOr(layer, () -> defaultLayer);

		MeshBuilder line = context.getLine(layerName);
		MeshBuilder mesh = context.getMesh(layerName);
		MeshBuilder points = context.getPoints(layerName);

		StandardFLineDrawing.dispatchLine(fline, mesh, line, points, text, layerName);
	}

	protected Map<String, Function<Box, FLine>> defaultdrawsLines(Dict.Prop<Map<String, Function<Box, FLine>>> k) {
		Map<String, Function<Box, FLine>> r = new IdempotencyMap<>(Function.class);


		r.put("__outlineFill__", new Cached<Box, Object, FLine>((box, previously) -> {
			Rect rect = box.properties.get(frame);
			if (rect == null) return new FLine();

			boolean selected = box.properties.isTrue(Mouse.isSelected, false);

			float ar = (float) (selected ? Colors.boxBackground1Selected.x : Colors.boxBackground1.x);
			float ag = (float) (selected ? Colors.boxBackground1Selected.y : Colors.boxBackground1.y);
			float ab = (float) (selected ? Colors.boxBackground1Selected.z : Colors.boxBackground1.z);
			float br = (float) (selected ? Colors.boxBackground2Selected.x : Colors.boxBackground2.x);
			float bg = (float) (selected ? Colors.boxBackground2Selected.y : Colors.boxBackground2.y);
			float bb = (float) (selected ? Colors.boxBackground2Selected.z : Colors.boxBackground2.z);
			float s = (float) (selected ? Colors.boxBackground1Selected.w : Colors.boxBackground1.w);

			FLine f = new FLine();

			if (box.getClass() != Box.class) {
				f.rect(rect);
			} else FLinesAndJavaShapes.drawRoundedRectInto(f, rect.x, rect.y, rect.w, rect.h, 19);

			for (int i = 0; i < f.nodes.size(); i++) {
				float alpha = ((i + 2) % f.nodes.size()) / (f.nodes.size() - 1f);
				alpha = alpha * (1 - alpha) * 4;
				f.nodes.get(i).attributes.put(fillColor, new Vec4(ar * (1 - alpha) + alpha * br, ag * (1 - alpha) + alpha * bg, ab * (1 - alpha) + alpha * bb, s));
			}

			f.attributes.put(filled, true);
			f.attributes.put(stroked, false);

			Map<Integer, String> customFill = new LinkedHashMap<Integer, String>();
			customFill.put(1, "fillColor");
			f.setAuxProperties(customFill);

			return f;
		}, (box) -> new Pair(box.properties.get(frame), box.properties.get(Mouse.isSelected))));

		r.put("__outline__", new Cached<Box, Object, FLine>((box, previously) -> {

			Rect rect = box.properties.get(frame);
			if (rect == null) return new FLine();

			boolean selected = box.properties.isTrue(Mouse.isSelected, false);

			FLine f = new FLine();

			if (selected) rect = rect.inset(3.5f);
			else rect = rect.inset(-0.5f);

//			rect = rect.inset(5);

			//f.rect(rect);
			FLinesAndJavaShapes.drawRoundedRectInto(f, rect.x, rect.y, rect.w, rect.h, 19 + (selected ? -8 : 1.0f));

			f.attributes.put(strokeColor, selected ? Colors.boxStrokeSelected : Colors.boxStroke);

			f.attributes.put(thicken, new BasicStroke(selected ? 16 : 0.5f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));

			f.attributes.put(stroked, true);

			return f;
		}, (box) -> new Pair(box.properties.get(frame), box.properties.get(Mouse.isSelected))));

		r.put("__name__", new Cached<Box, Object, FLine>((box, previously) -> {
			Rect rect = box.properties.get(frame);
			if (rect == null) return new FLine();

			boolean selected = box.properties.isTrue(Mouse.isSelected, false);

			FLine f = new FLine();
			f.moveTo(rect.x + rect.w / 2, rect.y + rect.h / 2 + 36 / 5.0f);

			f.attributes.put(hasText, true);
			f.attributes.put(color, new Vec4(0, 0, 0, 0.75f));
			String name = box.properties.getOr(Box.name, () -> "");

			if (box.properties.isTrue(FileBrowser.isLinked, false)) name = "{ " + name + " }";

			f.nodes.get(f.nodes.size() - 1).attributes.put(text, name);

//			f.attributes.put(layer, "glass");

			return f;
		}, (box) -> new Triple(box.properties.get(frame), box.properties.get(Box.name), box.properties.get(Mouse.isSelected))));

		TextDrawing text = first(TextDrawing.textDrawing, both()).get();
		TextDrawing.FontSupport fs = text.getFontSupport("source-sans-pro-regular-92.fnt");

		r.put("__nameGlass__", new Cached<Box, Object, FLine>((box, previously) -> {
			Rect rect = box.properties.get(frame);
			if (rect == null) return null;

			boolean selected = box.properties.isTrue(Mouse.isSelected, false);

			FLine f = new FLine();

			String name = box.properties.getOr(Box.name, () -> "");
			if (box.properties.isTrue(FileBrowser.isLinked, false)) name = "{ " + name + " }";
			Vec2 d = fs.font.dimensions(name, 0.15f);

			if (rect.w > d.x + 5) return new FLine();

			f.rect((int) (rect.x + rect.w / 2 - d.x / 2 - 10), (int) (rect.y + rect.h / 2 - 36 / 5.0 - 5), (int) d.x + 20, 30);

			f.attributes.put(filled, true);
			f.attributes.put(stroked, false);
			f.attributes.put(fillColor,
				selected ? new Vec4(Colors.boxTextBackground2.x, Colors.boxTextBackground2.y, Colors.boxTextBackground2.z, 0.5f) : new Vec4(Colors.boxTextBackground1.x,
					Colors.boxTextBackground1.y,
					Colors.boxTextBackground1.z,
					0.5f));
			f.attributes.put(strokeColor, new Vec4(0, 0, 0, 1f));

//			f.attributes.put(layer, "glass");

			return f;
		}, (box) -> new Triple(box.properties.get(frame), box.properties.get(Box.name), box.properties.get(Mouse.isSelected))));


		return r;
	}


}
