package fieldbox.boxes.plugins;

import field.graphics.Window;
import field.linalg.Vec2;
import field.utility.Log;
import fieldbox.boxes.Box;
import fieldbox.boxes.Drawing;
import fieldbox.boxes.FrameManipulation;
import fieldbox.boxes.Mouse;
import fieldbox.ui.FieldBoxWindow;

/**
 * Adds mouse wheel (and, thus two-finger drag on OS X) pan support to the canvas
 */
public class Scrolling extends Box {

    public Scrolling(Box root_unused) {
        this.properties.put(Planes.plane, "__always__");
        this.properties.putToMap(Mouse.onMouseScroll, "__scrolling__", e -> {

            if (e.properties.isTrue(FieldBoxWindow.consumed, false)) return;

            Log.log("scrolling", () -> "not consumed");
            if (e.after.dwheel != 0 || e.after.dwheely != 0) {
                this.find(Drawing.drawing, this.both())
                        .findFirst()
                        .ifPresent(x ->
                                scrollForMouseEvent(Scrolling.this, e, x));
            }
        });
    }

    static public void scrollForMouseEvent(Box from, Window.Event<Window.MouseState> e, Drawing x) {

        if (e.after.keyboardState.isShiftDown()) {
            Vec2 t = new Vec2(x.getScale());

            Vec2 dm = new Vec2(e.after.mx, e.after.my);

            Vec2 d = new Vec2(e.after.x, e.after.y);


            double sc = x.getScale().x;

            t.x = t.y = t.y * Math.pow(2, e.after.dwheely / 50f);

            t.x = Math.max(0.1f, Math.min(10, t.x));
            t.y = Math.max(0.1f, Math.min(10, t.y));

            x.setScale(from, t);

            double r = (t.x / sc);

            Vec2 trans;
            {
                Vec2 dm2 = windowSystemToDrawingSystem(d, t, x.getTranslation());
                x.setTranslation(from, trans = x.getTranslation().add(new Vec2(dm).sub(dm2).mul(-t.x)));
            }
        } else {
            Vec2 t = x.getTranslation();

            double sc = x.getScale().x;

            t.x += e.after.dwheel * 8;
            t.y += e.after.dwheely * 8;

            x.setTranslation(from, t);
        }

        FrameManipulation.continueTranslationFeedback(from, false);

    }

    /**
     * to convert between event / mouse / pixel coordinates and OpenGL / Box / Drawing coordinates.
     */
    static public Vec2 windowSystemToDrawingSystem(Vec2 window, Vec2 scale, Vec2 translation) {
        double y = window.y;
        double x = window.x;

        x = x / scale.x;
        y = y / scale.y;
        x -= translation.x / scale.x;
        y -= translation.y / scale.y;

        return new Vec2(x, y);
    }

    /**
     * to convert between OpenGL / Box / Drawing coordinates and event / mouse / pixel coordinates.
     */
    static public Vec2 drawingSystemToWindowSystem(Vec2 window, Vec2 scale, Vec2 translation) {
        double y = window.y;
        double x = window.x;

        x = x * scale.x;
        y = y * scale.y;
        x += translation.x;
        y += translation.y;

        return new Vec2(x, y);
    }


}
