package fieldbox.boxes;

import field.graphics.FLine;
import field.graphics.MeshBuilder;
import field.linalg.Vec2;
import field.linalg.Vec4;
import field.utility.Dict;
import field.utility.Log;

import java.awt.*;
import java.awt.List;
import java.util.*;

/**
 * Class that encapsulates the standard interpretation of FLine -> MeshBuilder (in particular the standard interpretation of the properties)
 *
 * We've pulled this out of FLineDrawing as we inch towards having this part of the "main" graphics system
 */
public class StandardFLineDrawing {


	static public final Dict.Prop<Boolean> stroked = new Dict.Prop<>("stroked").type().toCannon()
		    .doc("should the line be stroked? defaults to true");
	static public final Dict.Prop<BasicStroke> thicken = new Dict.Prop<>("thicken").type().toCannon()
		    .doc("should the line be thickened and tesselated with a java.awt.BasicStroke?");
	static public final Dict.Prop<Boolean> filled = new Dict.Prop<>("filled").type().toCannon()
		    .doc("should the line be filled and tessellated? defaults to false");
	static public final Dict.Prop<Boolean> pointed = new Dict.Prop<>("pointed").type().toCannon()
		    .doc("should the points on the line be drawn? defaults to false");

	static public final Dict.Prop<Vec4> color = new Dict.Prop<>("color").type().toCannon()
		    .doc("the color for the line. Defaults to black = Vec4(0,0,0,0)");
	static public final Dict.Prop<Float> opacity = new Dict.Prop<>("opacity").type().toCannon().doc("the opacity to the line. Defaults to 1.0");
	static public final Dict.Prop<Vec4> strokeColor = new Dict.Prop<>("strokeColor").type().toCannon()
		    .doc("the color for the stroke of a line. Defaults to the value of 'color'");
	static public final Dict.Prop<Vec4> fillColor = new Dict.Prop<>("fillColor").type().toCannon()
		    .doc("the color for the fill of a line. Defaults to the value of 'color'");
	static public final Dict.Prop<Vec4> pointColor = new Dict.Prop<>("pointColor").type().toCannon()
		    .doc("the color for the points on a line. Defaults to the value of 'color'");

	static public final Dict.Prop<Boolean> hasText = new Dict.Prop<>("hasText").type().toCannon().doc("does this line contain text?");
	static public final Dict.Prop<String> text = new Dict.Prop<>("text").type().toCannon()
		    .doc("NODE ATTRIBUTE. set this to be the text label centered on this node");
	static public final Dict.Prop<Number> textScale = new Dict.Prop<>("textScale").type().toCannon()
		    .doc("the scale of the text on this node. Defaults to 0.2f, the size of the labels on the boxes in Field");
	static public final Dict.Prop<String> font = new Dict.Prop<>("font").type().toCannon().doc("the (distance bitmap) font for Field text");
	static public final Dict.Prop<java.util.List<String>> textSpans = new Dict.Prop<>("textSpans").type().toCannon()
		    .doc("a list of text spans for doing multi-color, multi-font runs of text");
	static public final Dict.Prop<java.util.List<String>> fontSpans = new Dict.Prop<>("fontSpans").type().toCannon()
		    .doc("a list of font spans for doing multi-color, multi-font runs of text");
	static public final Dict.Prop<java.util.List<Vec4>> textColorSpans = new Dict.Prop<>("textColorSpans").type().toCannon()
		    .doc("a list of color spans for doing multi-color, multi-font runs of text");


	static protected void dispatchLine(FLine fline, MeshBuilder mesh, MeshBuilder line, MeshBuilder points, Optional<TextDrawing> ot, String layerName) {

		Log.log("drawing.trace", "dispatching line :" + fline);

		Vec4 sc = new Vec4(fline.attributes.getOr(strokeColor, () -> fline.attributes.getOr(color, () -> new Vec4(0, 0, 0, 1))));
		Vec4 fc = new Vec4(fline.attributes.getOr(fillColor, () -> fline.attributes.getOr(color, () -> new Vec4(0, 0, 0, 1))));
		Vec4 pc = new Vec4(fline.attributes.getOr(pointColor, () -> fline.attributes.getOr(color, () -> new Vec4(0, 0, 0, 1))));

		float op = fline.attributes.getOr(opacity, () -> 1f);
		sc.w *= op;
		fc.w *= op;
		pc.w *= op;

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
			fline.nodes.stream().filter(node -> node.attributes.has(text)).forEach(node -> {
				String textToDraw = node.attributes.get(text);
				float textScale = node.attributes.getFloat(StandardFLineDrawing.textScale, 1f) * 0.2f;
				ot.map(t -> t.getFontSupport(fline.attributes.getOr(font, () -> "source-sans-pro-regular.fnt"), layerName))
					    .ifPresent(fs -> {
						    Vec2 v = fs.font.dimensions(textToDraw, textScale);
						    fs.mesh.aux(1, fc);
						    fs.font.draw(textToDraw, new Vec2(node.to.x - v.x / 2, node.to.y), textScale, fline);
					    });
			});

			fline.nodes.stream().filter(node -> node.attributes.has(textSpans)).forEach(node -> {
				java.util.List<String> textToDraw = node.attributes.get(textSpans);
				java.util.List<String> fontToDraw = node.attributes.get(fontSpans);
				java.util.List<Vec4> colorsToDraw = node.attributes.get(textColorSpans);
				float textScale = node.attributes.getFloat(StandardFLineDrawing.textScale, 1f) * 0.2f;
				String prev = "source-sans-pro-regular.fnt";
				Vec4 prevColor = fc;

				Vec2 dim = new Vec2();

				for (int i = 0; i < textToDraw.size(); i++) {
					String m = textToDraw.get(i);
					String f = fontToDraw == null ? prev : (i >= fontToDraw.size() ? prev : fontToDraw.get(i));
					ot.map(t -> t.getFontSupport(fline.attributes.getOr(font, () -> f), layerName)).ifPresent(fs -> {
						Vec2 v = fs.font.dimensions(m, textScale);
						dim.x += v.x;
					});
					prev = f;
				}

				Vec2 o = new Vec2();
				for (int i = 0; i < textToDraw.size(); i++) {
					String m = textToDraw.get(i);
					String f = fontToDraw == null ? prev : (i >= fontToDraw.size() ? prev : fontToDraw.get(i));
					Vec4 fcHere = colorsToDraw == null ? prevColor : (i >= colorsToDraw.size() ? prevColor : colorsToDraw.get(i));
					ot.map(t -> t.getFontSupport(fline.attributes.getOr(font, () -> f))).ifPresent(fs -> {
						fs.mesh.aux(1, new Vec4(fcHere).scale(op));
						fs.font.draw(m, new Vec2(node.to.x - dim.x / 2 + o.x, node.to.y), textScale, fline);
						o.x += fs.font.dimensions(m, textScale).x;
					});
					prev = f;
					prevColor = fcHere;
				}
			});
		}

	}
}
