package field.graphics;

import field.linalg.Vec2;
import field.linalg.Vec3;
import field.linalg.Vec4;
import field.utility.Dict;
import field.utility.IdempotencyMap;
import fieldbox.boxes.TextDrawing;

import java.awt.*;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Class that encapsulates the standard interpretation of FLine -> MeshBuilder (in particular the standard interpretation of the properties)
 * <p>
 * We've pulled this out of FLineDrawing as we inch towards having this part of the "main" graphics system
 */
public class StandardFLineDrawing {


    static public final Dict.Prop<Boolean> stroked = new Dict.Prop<>("stroked").type()
            .toCanon()
            .doc("should the line be stroked? defaults to true").set(Dict.domain, "fline");
    static public final Dict.Prop<BasicStroke> thicken = new Dict.Prop<>("thicken")
            .type()
            .toCanon()
            .doc("should the line be thickened and tesselated with a particular thickness (or java.awt.BasicStroke?)")
            .set(Dict.domain, "fline")
            .set(Dict.customCaster, v -> {
                if (v instanceof Number)
                    return new BasicStroke(((Number) v).floatValue(), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
                return v;
            });
    static public final Dict.Prop<Float> fastThicken = new Dict.Prop<>("fastThicken").type()
            .toCanon()
            .doc("`line.fastThicken=4` thickens this line before drawing it to be 4 units wide. This is the fastest, least pretty, and least likely to be correct line thickening algorithm.").set
                    (Dict
                             .domain,
                     "fline");

    static public final Dict.Prop<Boolean> hint_noDepth = new Dict.Prop<>("hint_noDepth").type().toCanon().doc("set on a line to hint to the renderer that z=0 for all nodes in this line. At " +
                                                                                                                       "as spresent this merely allows lines to be `thicken` faster.");
    static public final Dict.Prop<Boolean> filled = new Dict.Prop<>("filled").type()
            .toCanon()
            .doc("should the line be filled and tessellated? defaults to false").set(Dict.domain, "fline");
    static public final Dict.Prop<Boolean> pointed = new Dict.Prop<>("pointed").type()
            .toCanon()
            .doc("should the points on the line be drawn? defaults to false").set(Dict.domain, "fline");

    static public final Dict.Prop<Supplier<Vec4>> color = new Dict.Prop<>("color").type()
            .toCanon()
            .doc("the color for the line. Defaults to black = Vec4(0,0,0,0)").set(Dict.domain, "fline fnode");
    static public final Dict.Prop<Float> opacity = new Dict.Prop<>("opacity").type()
            .toCanon()
            .doc("the opacity to the line. Defaults to 1.0").set(Dict.domain, "fline");
    static public final Dict.Prop<Float> fillOpacity = new Dict.Prop<>("fillOpacity").type()
            .toCanon()
            .doc("the opacity of any fill the line has. Defaults to 1.0, multiplies along with `opacity` and any `fillColor`. ").set(Dict.domain, "fline");
    static public final Dict.Prop<Float> strokeOpacity = new Dict.Prop<>("strokeOpacity").type()
            .toCanon()
            .doc("the opacity to any stroke the line has. Defaults to 1.0, multiplies along with `opacity` and any `strokeColor`. ").set(Dict.domain, "fline");

    static public final Dict.Prop<Supplier<Vec4>> strokeColor = new Dict.Prop<>("strokeColor").type()
            .toCanon()
            .doc("the color for the stroke of a line. Defaults to the value of 'color'").set(Dict.domain, "fline fnode");
    static public final Dict.Prop<Supplier<Vec4>> fillColor = new Dict.Prop<>("fillColor").type()
            .toCanon()
            .doc("the color for the fill of a line. Defaults to the value of 'color'").set(Dict.domain, "fline fnode");

    static public final Dict.Prop<Supplier<Vec4>> pointColor = new Dict.Prop<>("pointColor").type()
            .toCanon()
            .doc("the color for the points on a line. Defaults to the value of 'color'").set(Dict.domain, "fline fnode");

    static public final Dict.Prop<Supplier<Vec2>> texCoord = new Dict.Prop<>("texCoord").type()
            .toCanon()
            .doc("adds texture coordinates to a node along the line").set(Dict.domain, "fnode");

    static public final Dict.Prop<Boolean> hasText = new Dict.Prop<>("hasText").type()
            .toCanon()
            .doc("does this line contain text?").set(Dict.domain, "fline");
    static public final Dict.Prop<String> text = new Dict.Prop<>("text").type()
            .toCanon()
            .doc("set this to be the text label centered on this node").set(Dict.domain, "fnode");
    static public final Dict.Prop<Number> textScale = new Dict.Prop<>("textScale").type()
            .toCanon()
            .doc("the scale of the text on this node. Defaults to 1, the size of the labels on the boxes in Field").set(Dict.domain, "fnode");
    static public final Dict.Prop<Number> textAlign = new Dict.Prop<>("textAlign").type()
            .toCanon()
            .doc("0.5 centers the text, 0 is left justified, 1 is right").set(Dict.domain, "fnode");
    static public final Dict.Prop<String> font = new Dict.Prop<>("font").type()
            .toCanon()
            .doc("the (distance bitmap) font for Field text").set(Dict.domain, "fline");
    static public final Dict.Prop<java.util.List<String>> textSpans = new Dict.Prop<>("textSpans").type()
            .toCanon()
            .doc("a list of text spans for doing multi-color, multi-font runs of text").set(Dict.domain, "fline");
    static public final Dict.Prop<java.util.List<String>> fontSpans = new Dict.Prop<>("fontSpans").type()
            .toCanon()
            .doc("a list of font spans for doing multi-color, multi-font runs of text").set(Dict.domain, "fline");
    static public final Dict.Prop<java.util.List<Vec4>> textColorSpans = new Dict.Prop<>("textColorSpans").type()
            .toCanon()
            .doc("a list of color spans for doing multi-color, multi-font runs of text").set(Dict.domain, "fline");

    static public final Dict.Prop<Vec3> textScaleCenter = new Dict.Prop<>("textScaleCenter").type().toCanon().doc("sets the center of a text label (where it scales around in response to the " +
                                                                                                                          "canvas being scaled");

    static public final Dict.Prop<Number> pointSize = new Dict.Prop<>("pointSize").type()
            .toCanon()
            .doc("sets the size of the point (if this line is drawn `.pointed=true`). This can be applied per-node or per-line.").set(Dict.domain, "fline fnode");

    static public final Dict.Prop<IdempotencyMap<Supplier<FLine>>> subLines = new Dict.Prop<>("subLines").type()
            .toCanon().autoConstructs(() -> new IdempotencyMap<Supplier<FLine>>(Supplier.class))
            .doc("other, additional lines that are drawn along side this one. This can be applied per-node or per-line. Useful for decorations, annotations, selection marks etc.").set(Dict
                                                                                                                                                                                                .domain, "fline fnode");

    static public final Dict.Prop<IdempotencyMap<Consumer<FLine>>> hooks = new Dict.Prop<>("hooks").type()
            .toCanon().autoConstructs(() -> new IdempotencyMap<Consumer<FLine>>(Supplier.class))
            .doc("Hooks are functions executed on FLines just prior to drawing").set(Dict.domain, "fline fnode");

    static public final Dict.Prop<Boolean> noContours = new Dict.Prop<>("noContours").type()
            .toCanon()
            .doc("setting `.noContours=true` turns off any smartness in the tessellator about how to fill FLines, speeding up tessellation considerably").set(Dict.domain, "fline");

    static public final Dict.Prop<Boolean> notation = new Dict.Prop<>("notation").type()
            .toCanon()
            .doc("setting `.notation=true` advertises this line as being notation, allowing it to be intersected by Intersections").set(Dict.domain, "fline");


    static public final Dict.Prop<Number> curveScale = new Dict.Prop<>("curveScale").type()
            .toCanon()
            .doc("increases the resolution of cubic spline rendering by a factor.").set(Dict.domain, "fline");


    static public void dispatchLine(FLine fline, MeshBuilder mesh, MeshBuilder line, MeshBuilder points, Optional<TextDrawing> ot, String layerName) {
        dispatchLine(fline, mesh, line, points, ot, layerName, 1f);
    }

    static public void dispatchLine(FLine fline, MeshBuilder mesh, MeshBuilder line, MeshBuilder points, Optional<TextDrawing> ot, String layerName, float opacityMultiply) {

        IdempotencyMap<Consumer<FLine>> hooksHere = fline.attributes.getOr(hooks, () -> null);
        if (hooksHere != null) {
            hooksHere.values().forEach(x -> x.accept(fline));
        }

        Vec4 sc = new Vec4(fline.attributes.getOr(strokeColor, () -> fline.attributes.getOr(color, () -> new Vec4(0, 0, 0, 1))).get());
        Vec4 fc = new Vec4(fline.attributes.getOr(fillColor, () -> fline.attributes.getOr(color, () -> new Vec4(0, 0, 0, 1))).get());
        Vec4 pc = new Vec4(fline.attributes.getOr(pointColor, () -> fline.attributes.getOr(color, () -> new Vec4(0, 0, 0, 1))).get());

        float op = fline.attributes.getOr(opacity, () -> 1f) * opacityMultiply;
        float sop = fline.attributes.getOr(strokeOpacity, () -> 1f) * opacityMultiply;
        float fop = fline.attributes.getOr(fillOpacity, () -> 1f) * opacityMultiply;

        sc.w *= op * sop;
        fc.w *= op * fop;
        pc.w *= op;

        if (line != null) line.aux(1, sc);
        if (mesh != null) mesh.aux(1, fc);
        if (points != null) points.aux(1, pc);

        BasicStroke s = fline.attributes.getOr(thicken, () -> null);
        if (s != null && mesh != null) {
            mesh.aux(1, sc);
            fline.renderLineToMeshByStroking(mesh, (int) Math.max(2, 20 * fline.attributes.getOr(curveScale, () -> 1f).floatValue()), s);
            mesh.aux(1, sc);

        } else if (fline.attributes.has(fastThicken)) {
            float t = fline.attributes.getFloat(fastThicken, 1f);
            mesh.aux(1, sc);
            new EvenFasterThicken(t).renderToMeshByThickening(fline, mesh);
            mesh.aux(1, sc);
        } else if (fline.attributes.isTrue(stroked, true) && line != null) {
            if (opacityMultiply == 1)
                fline.addAuxProperties(1, color.getName());
            else
                fline.addAuxPropertiesFunctions(1, n -> {
                    Supplier<Vec4> qq = n.attributes.get(color);
                    if (qq == null) return null;
                    Vec4 v = qq.get();
                    return new Vec4(v.x, v.y, v.z, v.w * opacityMultiply);
                });
            fline.renderToLine(line, (int) Math.max(2, 10 * fline.attributes.getOr(curveScale, () -> 1f).floatValue()));
        }
        if (fline.attributes.isTrue(filled, false) && mesh != null) {
            if (opacityMultiply == 1)
                fline.addAuxProperties(1, color.getName());
            else
                fline.addAuxPropertiesFunctions(1, n -> {
                    Supplier<Vec4> qq = n.attributes.get(color);
                    if (qq == null) return null;
                    Vec4 v = qq.get();
                    return new Vec4(v.x, v.y, v.z, v.w * opacityMultiply);
                });
            mesh.aux(1, fc);
            fline.renderToMesh(mesh, (int) Math.max(2, 10 * fline.attributes.getOr(curveScale, () -> 1f).floatValue()));
            mesh.aux(1, fc);
        }
        if (fline.attributes.isTrue(pointed, false) && points != null) {
            float ps = fline.attributes.getFloat(pointSize, 0f);
            points.aux(2, ps);
            fline.addAuxProperties(2, pointSize.getName());
            fline.renderToPoints(points, (int) Math.max(2, 10 * fline.attributes.getOr(curveScale, () -> 1f).floatValue()));
            points.aux(2, ps);
        }


        IdempotencyMap<Supplier<FLine>> sl = fline.attributes.getOr(subLines, () -> null);

        if (sl != null) {
            sl.values().stream().filter(n -> n != null).map(x -> x.get()).filter(x -> x != null).forEach(x -> {
                dispatchLine(x, mesh, line, points, ot, layerName);
            });
        }

        fline.nodes.stream().
                map(n -> n.attributes.get(subLines)).
                filter(n -> n != null).
                flatMap(n -> n.values().
                        stream()).
                map(x -> x.get()).
                filter(x -> x != null).
                forEach(x ->
                        {
                            dispatchLine(x, mesh, line, points, ot, layerName, opacityMultiply);
                        });

        if (fline.attributes.isTrue(hasText, false) && ot.isPresent()) {
            fline.nodes.stream()
                    .filter(node -> node.attributes.has(text))
                    .forEach(node -> {
                        String textToDraw = "" + node.attributes.get(text);
                        float textScale = node.attributes.getFloat(StandardFLineDrawing.textScale, 1f) * 0.12f;
                        float align = node.attributes.getFloat(StandardFLineDrawing.textAlign, 0.5f);
                        ot.map(t -> t.getFontSupport(fline.attributes.getOr(font, () -> "source-sans-pro-regular-92.fnt"), layerName))
                                .ifPresent(fs -> {
                                    Vec2 v = fs.font.dimensions(textToDraw, textScale);
                                    fs.mesh.aux(1, fc);
                                    fs.font.draw(textToDraw, new Vec3(node.to.x - align * v.x, node.to.y, node.to.z), textScale, fline, node.attributes.getOr(textScaleCenter, ()
                                            -> node.to));
                                });
                    });

            fline.nodes.stream()
                    .filter(node -> node.attributes.has(textSpans))
                    .forEach(node -> {
                        java.util.List<String> textToDraw = node.attributes.get(textSpans);
                        java.util.List<String> fontToDraw = node.attributes.get(fontSpans);
                        java.util.List<Vec4> colorsToDraw = node.attributes.get(textColorSpans);
                        float textScale = node.attributes.getFloat(StandardFLineDrawing.textScale, 1f) * 0.12f;
                        String prev = "source-sans-pro-regular-92.fnt";
                        Vec4 prevColor = fc;

                        Vec2 dim = new Vec2();

                        for (int i = 0; i < textToDraw.size(); i++) {
                            String m = textToDraw.get(i);
                            String f = fontToDraw == null ? prev : (i >= fontToDraw.size() ? prev : fontToDraw.get(i));
                            ot.map(t -> t.getFontSupport(fline.attributes.getOr(font, () -> f), layerName))
                                    .ifPresent(fs -> {
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
                            ot.map(t -> t.getFontSupport(fline.attributes.getOr(font, () -> f)))
                                    .ifPresent(fs -> {
                                        fs.mesh.aux(1, new Vec4(fcHere).mul(op));
                                        fs.font.draw(m, new Vec3(node.to.x - dim.x / 2 + o.x, node.to.y, node.to.z), textScale, fline, node.to);
                                        o.x += fs.font.dimensions(m, textScale).x;
                                    });
                            prev = f;
                            prevColor = fcHere;
                        }
                    });
        }

    }

}
