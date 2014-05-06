package fieldbox.boxes;


import field.graphics.FLine;
import field.graphics.MeshBuilder;
import field.linalg.Vec2;
import field.linalg.Vec3;
import field.linalg.Vec4;
import field.utility.Cached;
import field.utility.Dict;
import field.utility.Pair;
import field.utility.Rect;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Function;

public class FrameDrawer extends Box implements Drawing.Drawer {

	static public final Dict.Prop<Map<String, Function<Box, FLine>>> frameDrawing = new Dict.Prop<>("frameDrawing");

	static public final Dict.Prop<Boolean> stroked = new Dict.Prop<>("stroked");
	static public final Dict.Prop<BasicStroke> thicken = new Dict.Prop<>("thicken");
	static public final Dict.Prop<Boolean> filled = new Dict.Prop<>("filled");
	static public final Dict.Prop<Boolean> pointed = new Dict.Prop<>("pointed");

	static public final Dict.Prop<Vec4> color = new Dict.Prop<>("color");
	static public final Dict.Prop<Float> opacity= new Dict.Prop<>("opacity");
	static public final Dict.Prop<Vec4> strokeColor = new Dict.Prop<>("strokeColor");
	static public final Dict.Prop<Vec4> fillColor = new Dict.Prop<>("fillColor");
	static public final Dict.Prop<Vec4> pointColor = new Dict.Prop<>("pointColor");

	static public final Dict.Prop<Boolean> hasText = new Dict.Prop<>("hasText");
	static public final Dict.Prop<String> text = new Dict.Prop<>("text");
	static public final Dict.Prop<String> font = new Dict.Prop<>("font");
	static public final Dict.Prop<List<String>> textSpans = new Dict.Prop<>("textSpans");
	static public final Dict.Prop<List<String>> fontSpans = new Dict.Prop<>("fontSpans");
	static public final Dict.Prop<List<Vec4>> textColorSpans = new Dict.Prop<>("colorSpans");

	static public final Dict.Prop<String> layer = new Dict.Prop<>("layer");

	public FrameDrawer() {
		this.properties.putToList(Drawing.drawers, this);
	}


	@Override
	public void draw(Drawing context) {

		this.breadthFirst(this.both()).forEach(x -> {
			Rect r = x.properties.get(Manipulation.frame);

			Map<String, Function<Box, FLine>> drawing = x.properties.computeIfAbsent(frameDrawing, this::defaultdrawsLines);

			List<FLine> all = new ArrayList<>();
			Iterator<Function<Box, FLine>> it = drawing.values().iterator();
			while (it.hasNext()) {
				Function<Box, FLine> f = it.next();
				FLine fl = f.apply(x);
				if (fl == null) it.remove();
				else all.add(fl);
			}

			drawing.values().stream().map(c -> c.apply(x)).filter(fline -> fline != null).forEach(fline -> {

				String layerName = fline.attributes.getOr(layer, () -> "__main__");

				MeshBuilder line = context.getLine(layerName);
				MeshBuilder mesh = context.getMesh(layerName);
				MeshBuilder points = context.getPoints(layerName);
				Optional<TextDrawing> text = first(TextDrawing.textDrawing, both());


				Vec4 sc = new Vec4(fline.attributes.getOr(strokeColor, () -> fline.attributes.getOr(color, () -> new Vec4(0, 0, 0, 1))));
				Vec4 fc = new Vec4(fline.attributes.getOr(fillColor, () -> fline.attributes.getOr(color, () -> new Vec4(0, 0, 0, 1))));
				Vec4 pc = new Vec4(fline.attributes.getOr(pointColor, () -> fline.attributes.getOr(color, () -> new Vec4(0, 0, 0, 1))));

				float op = fline.attributes.getOr(opacity, () -> 1f);
				sc.w*=op;
				fc.w*=op;
				pc.w*=op;

				line.aux(1, sc);
				mesh.aux(1, fc);
				points.aux(1, pc);

				BasicStroke s = fline.attributes.getOr(thicken, () -> null);
				if (s != null) {
					mesh.aux(1, sc);
					fline.renderLineToMeshByStroking(mesh, 20, s);
					mesh.aux(1, fc);
				} else {
					if (fline.attributes.isTrue(stroked, true)) fline.renderToLine(line, 20);
				}
				if (fline.attributes.isTrue(filled, false)) fline.renderToMesh(mesh, 20);
				if (fline.attributes.isTrue(pointed, false)) fline.renderToPoints(points, 20);
				if (fline.attributes.isTrue(hasText, false)) {
					fline.nodes.stream().filter(node -> node.attributes.has(FrameDrawer.text)).forEach(node -> {
						String textToDraw = node.attributes.get(FrameDrawer.text);
						text.map(t ->t.getFontSupport(fline.attributes.getOr(font, () -> "source-sans-pro-regular.fnt"),layerName))
							    .ifPresent(fs -> {
								    Vec2 v = fs.font.dimensions(textToDraw, 0.2f);
								    fs.mesh.aux(1, fc);
								    fs.font.draw(textToDraw, new Vec2(node.to.x - v.x / 2, node.to.y), 0.2f, fline);
							    });
					});

					fline.nodes.stream().filter(node -> node.attributes.has(FrameDrawer.textSpans)).forEach(node -> {
						List<String> textToDraw = node.attributes.get(FrameDrawer.textSpans);
						List<String> fontToDraw =node.attributes.get(FrameDrawer.fontSpans);
						List<Vec4> colorsToDraw =node.attributes.get(FrameDrawer.textColorSpans);
						String prev ="source-sans-pro-regular.fnt";
						Vec4 prevColor = fc;

						Vec2 dim = new Vec2();

						for(int i=0;i<textToDraw.size();i++)
						{
							String m = textToDraw.get(i);
							String f = fontToDraw==null ? prev : (i>=fontToDraw.size() ? prev : fontToDraw.get(i));
							text.map(t ->t.getFontSupport(fline.attributes.getOr(font, () -> f),layerName))
								    .ifPresent(fs -> {
									    Vec2 v = fs.font.dimensions(m, 0.2f);
									    dim.x += v.x;
								    });
							prev = f;
						}

						Vec2 o = new Vec2();
						for(int i=0;i<textToDraw.size();i++)
						{
							String m = textToDraw.get(i);
							String f = fontToDraw==null ? prev : (i>=fontToDraw.size() ? prev : fontToDraw.get(i));
							Vec4 fcHere = colorsToDraw==null ? prevColor : (i>=colorsToDraw.size() ? prevColor : colorsToDraw.get(i));
							text.map(t ->t.getFontSupport(fline.attributes.getOr(font, () -> f)))
								    .ifPresent(fs -> {
									    fs.mesh.aux(1, new Vec4(fcHere).scale(op));
									    fs.font.draw(m, new Vec2(node.to.x - dim.x / 2 + o.x, node.to.y), 0.2f, fline);
									    o.x += fs.font.dimensions(m, 0.2f).x;
								    });
							prev = f;
							prevColor = fcHere;
						}
					});
				}
			});

		});
	}

	protected Map<String, Function<Box, FLine>> defaultdrawsLines(Dict.Prop<Map<String, Function<Box, FLine>>> k) {
		Map<String, Function<Box, FLine>> r = new LinkedHashMap<>();

		r.put("__outline__", new Cached<Box, Object, FLine>((box, previously) -> {
			Rect rect = box.properties.get(Manipulation.frame);
			if (rect == null) return null;

			boolean selected = box.properties.isTrue(Manipulation.isSelected, false);

			FLine f = new FLine();
			if (selected) rect = rect.inset(8f);
			else rect = rect.inset(-0.5f);


			f.moveTo(rect.x, rect.y);
			f.lineTo(rect.x + rect.w, rect.y);
			f.lineTo(rect.x + rect.w, rect.y + rect.h);
			f.lineTo(rect.x, rect.y + rect.h);
			f.lineTo(rect.x, rect.y);

			f.attributes.put(strokeColor, selected ? new Vec4(0, 0, 0, 1.4f) : new Vec4(0, 0, 0, 0.2f));

			f.attributes.put(FrameDrawer.thicken, new BasicStroke(selected ? 16 : 2.5f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));

			f.attributes.put(stroked, true);

			return f;
		}, (box) -> new Pair(box.properties.get(Manipulation.frame), box.properties.get(Manipulation.isSelected))));

		r.put("__outlineFill__", new Cached<Box, Object, FLine>((box, previously) -> {
			Rect rect = box.properties.get(Manipulation.frame);
			if (rect == null) return null;

			boolean selected = box.properties.isTrue(Manipulation.isSelected, false);

			float a = selected ? 0.9f : 0.8f;
			float b = selected ? 0.75f : 0.75f;
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

			f.attributes.put(fillColor, selected ? new Vec4(1, 1, 1, 0.3f) : new Vec4(1, 1, 1, 0.6f));
			f.attributes.put(filled, true);

			Map<Integer, String> customFill = new LinkedHashMap<Integer, String>();
			customFill.put(1, "fillColor");
			f.setAuxProperties(customFill);


			return f;
		}, (box) -> new Pair(box.properties.get(Manipulation.frame), box.properties.get(Manipulation.isSelected))));

		r.put("__name__", new Cached<Box, Object, FLine>((box, previously) -> {
			Rect rect = box.properties.get(Manipulation.frame);
			if (rect == null) return null;

			boolean selected = box.properties.isTrue(Manipulation.isSelected, false);

			FLine f = new FLine();
			f.moveTo(rect.x + rect.w / 2, rect.y + rect.h / 2 + 36 / 5.0f);

			f.attributes.put(hasText, true);
			f.attributes.put(color, new Vec4(0, 0, 0, 0.75f));
			f.nodes.get(f.nodes.size() - 1).attributes.put(text, box.properties.get(Box.name));

			return f;
		}, (box) -> new Pair(box.properties.get(Manipulation.frame), box.properties.get(Box.name))));

		// todo

		return r;
	}


	static public Function<Box, FLine> boxOrigin(Function<Box, FLine> wrap, Vec2 origin) {
		return new Cached<Box, Object, FLine>((box, previously) -> {
			Rect frame = box.properties.get(Manipulation.frame);
			Vec2 o = new Vec2(frame.x + frame.w * origin.x, frame.y + frame.h * origin.y);
			return wrap.apply(box).byTransforming((pos) -> new Vec3(pos.x + o.x, pos.y + o.y, pos.z));
		}, (box) -> box.properties.get(Manipulation.frame));
	}

	static public Function<Box, FLine> boxScale(Function<Box, FLine> wrap, Vec2 origin) {
		return new Cached<Box, Object, FLine>((box, previously) -> {
			Rect frame = box.properties.get(Manipulation.frame);
			return wrap.apply(box).byTransforming((pos) -> new Vec3(frame.x + pos.x * frame.w, frame.y + pos.y * frame.h, pos.z));
		}, (box) -> box.properties.get(Manipulation.frame));
	}

	static public Function<Box, FLine> expires(Function<Box, FLine> wrap, int updates) {
		int[] counter = {updates};
		return box -> {
			if (updates<0) return wrap.apply(box);
			FLine f = wrap.apply(box);
			f.attributes.multiply(FrameDrawer.opacity, 1, counter[0]/(float)updates);
			f.modify();
			Drawing.dirty(box);
			return counter[0]-- < 0 ? null : f;
		};
	}


}
