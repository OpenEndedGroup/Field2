package fieldcef.plugins;

import field.graphics.FLine;
import field.graphics.Window;
import field.graphics.util.onsheetui.SimpleCanvas;
import field.linalg.Vec2;
import field.linalg.Vec4;
import field.utility.Dict;
import field.utility.Rect;
import fieldbox.DefaultMenus;
import fieldbox.boxes.Box;
import fieldbox.boxes.BoxChildHelper;
import fieldbox.boxes.Drawing;
import fieldbox.boxes.Mouse;
import fieldbox.boxes.plugins.*;
import fieldbox.execution.Execution;
import fieldbox.io.IO;

import java.awt.*;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.stream.Collectors;

import static field.graphics.StandardFLineDrawing.*;
import static fieldbox.boxes.FLineDrawing.frameDrawing;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_P;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_T;

// A Pad is a special form of Tap
public class Pads extends Box implements Mouse.OnMouseDown, IO.Loaded {

    static String code_padFactory = BoxDefaultCode.findSource(Pads.class, "padFactory");

    static public final Dict.Prop<FunctionOfBoxValued<BoxChildHelper>> padOutward = new Dict.Prop<FunctionOfBoxValued<BoxChildHelper>>("padOutward").doc("a collection of boxes that are the outward connections to this box")
            .toCanon()
            .type();
    static public final Dict.Prop<FunctionOfBoxValued<BoxChildHelper>> padInward = new Dict.Prop<FunctionOfBoxValued<BoxChildHelper>>("padInward").doc("a collection of boxes that are the inward connections to this box")
            .toCanon()
            .type();


    static public final Dict.Prop<Boolean> isPad = new Dict.Prop<>("_isPad").type()
            .toCanon();
    private final Box root;

    public Pads(Box root)
    {
        this.root = root;
    }

    @Override
    public void loaded() {
        this.properties.put(Planes.plane, "__always__");
        this.properties.putToMap(Mouse.onMouseDown, "__pads__", this);

        root.properties.put(padOutward, (box) -> {
            return new BoxChildHelper(box.children()
                                              .stream()
                                              .filter(x -> x.properties.has(TopologyBox.head))
                                              .filter(x -> x.properties.get(TopologyBox.head)
                                                      .get(root) == box)
                                              .map(x -> x.properties.get(TopologyBox.tail)
                                                      .get(root))
                                              .collect(Collectors.toCollection(() -> new LinkedHashSet<>())));
        });
        root.properties.put(padInward, (box) -> {
            return new BoxChildHelper(box.children()
                                              .stream()
                                              .filter(x -> x.properties.has(TopologyBox.tail))
                                              .filter(x -> x.properties.get(TopologyBox.tail)
                                                      .get(root) == box)
                                              .map(x -> x.properties.get(TopologyBox.head)
                                                      .get(root))
                                              .collect(Collectors.toCollection(() -> new LinkedHashSet<>())));
        });

    }

    public static Box padFactory(Box parent, String kind, String name) {

        Box c = parent.first(DefaultMenus.ensureChildOfClass)
                .get()
                .apply(parent, name, SimpleCanvas.class);

        if (c.properties.isTrue(DefaultMenus.wasNew, false))
            c.properties.put(Execution.code, code_padFactory);

        c.properties.put(DragToCopy._ownedByParent, true);

        return c;
    }

    @Override
    public Mouse.Dragger onMouseDown(Window.Event<Window.MouseState> e, int button) {
        if (button == 0) return button0(e);
        return null;
    }

    protected Mouse.Dragger button0(Window.Event<Window.MouseState> e) {
        if (!e.after.keyboardState.keysDown.contains(GLFW_KEY_P)) return null;

        Optional<Drawing> drawing = this.find(Drawing.drawing, both())
                .findFirst();
        Vec2 point = new Vec2(e.after.mx, e.after.my);

        Optional<Box> hit = breadthFirst(both()).filter(b -> frame(b) != null)
                .filter(x -> !x.properties.isTrue(Box.hidden, false))
                .filter(b -> frame(b).intersects(point))
                .filter(b -> b.properties.isTrue(Pads.isPad, false))
                .sorted((a, b) -> Float.compare(order(frame(a)), order(frame(b))))
                .findFirst();

        if (hit.isPresent()) {
            e.properties.put(Window.consumed, true);

            Box origin = hit.get();

            return (e1, termination) -> {

                Vec2 point1 = new Vec2(e1.after.mx, e1.after.my);

                Optional<Box> hit1 = breadthFirst(both()).filter(x -> !x.properties.isTrue(Box.hidden, false))
                        .filter(b -> frame(b) != null)
                        .filter(b -> frame(b).intersects(point1))
                        .filter(b -> b.properties.isTrue(Pads.isPad, false))
                        .sorted((a, b) -> Float.compare(order(frame(a)), order(frame(b))))
                        .findFirst();

                if (hit1.isPresent()) {
                    showCompleteDrag(origin, hit1.get());
                    if (termination) {
                        completeDrag(origin, hit1.get());
                    }
                } else {
                    showIncompleteDrag(origin, point1);
                    if (termination) {
                        Pads.this.properties.removeFromMap(frameDrawing, "__ongoingDrag__");
                    }
                }


                return !termination;
            };
        }
        return null;
    }

    protected void showIncompleteDrag(Box start, Vec2 to) {
        this.properties.putToMap(frameDrawing, "__ongoingDrag__", (box) -> {

            Rect f1 = frame(start);

            FLine m = TopologyBox.thickenArc(TopologyBox.arc(f1, new Rect(to.x - 10, to.y - 10, 20, 20)), f1, new Rect(to.x - 10, to.y - 10, 20, 20));

            boolean selected = false;

            float o = -0.5f;

            m.attributes.put(fillColor, selected ? new Vec4(1, 1, 1, 1.0f * o) : new Vec4(1, 1, 1, 0.5f * o));
            m.attributes.put(strokeColor, selected ? new Vec4(1, 1, 1, 0.25f * o) : new Vec4(1, 1, 1, 0.1f * o));
            m.attributes.put(thicken, new BasicStroke(selected ? 3 : 0.5f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));

            m.attributes.put(filled, true);
            m.attributes.put(stroked, true);

            m.rect(to.x - 10, to.y - 10, 20, 20);

            return m;
        });
        Drawing.dirty(this);
    }

    protected float order(Rect r) {
        return Math.abs(r.w) + Math.abs(r.h);
    }

    protected void completeDrag(Box start, Box box) {
        this.properties.removeFromMap(frameDrawing, "__ongoingDrag__");

        PadsBox b = new PadsBox(start, box);
        start.connect(b);
        box.connect(b);

        Drawing.dirty(this);
    }

    protected void showCompleteDrag(Box start, Box end) {
        this.properties.putToMap(frameDrawing, "__ongoingDrag__", (box) -> {

            Rect f1 = frame(start);
            Rect f2 = frame(end);


            FLine m = TopologyBox.thickenArc(TopologyBox.arc(f1, f2), f1, f2);

            boolean selected = true;

            float o = -0.5f;

            m.attributes.put(fillColor, selected ? new Vec4(1, 1, 1, 1.0f * o) : new Vec4(1, 1, 1, 0.75f * o));
            m.attributes.put(strokeColor, selected ? new Vec4(1, 1, 1, 0.5f * o) : new Vec4(1, 1, 1, 0.25f * o));
            m.attributes.put(thicken, new BasicStroke(selected ? 3 : 0.5f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));

            m.attributes.put(filled, true);
            m.attributes.put(stroked, true);

            return m;
        });
        Drawing.dirty(this);
    }

    protected Rect frame(Box hitBox) {
        return hitBox.properties.get(frame);
    }

}
