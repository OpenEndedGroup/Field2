package fieldbox.boxes.plugins;


import field.graphics.Window;
import field.linalg.Vec2;
import field.utility.Rect;
import fieldbox.boxes.*;

import java.util.Optional;

import static fieldbox.boxes.Intersects.*;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_S;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_T;

/**
 * utilities for helping with moving the time slider around with the mouse
 */
public class TimeHelper extends Box implements Mouse.OnMouseDown {

    private final Box root;

    public TimeHelper(Box root) {
        this.root = root;
        this.properties.put(Planes.plane, "__always__");
        this.properties.putToMap(Mouse.onMouseDown, "__timeHelper__", this);

    }

    @Override
    public Mouse.Dragger onMouseDown(Window.Event<Window.MouseState> e, int button) {
        if (button == 0) return button0(e);
        return null;
    }

    protected Mouse.Dragger button0(Window.Event<Window.MouseState> e) {
        if (!e.after.keyboardState.keysDown.contains(GLFW_KEY_S)) return null;
        e.properties.put(Window.consumed, true);

        Optional<Drawing> drawing = this.find(Drawing.drawing, both())
                .findFirst();

        Drawing.dirty(this);

        Vec2 point = new Vec2(e.after.mx, e.after.my);

        Box startAt = startAt(e.after, root);
        if (startAt == root) {
            // start anywhere

            if (e.after.keyboardState.isShiftDown()) {
                set((float) point.x);
                return null;
            } else {
                final float start = (float) point.x;
                final float startAtT = get();
                return (e1, termination) -> {
                    Vec2 point1 = new Vec2(e1.after.mx, e1.after.my);
                    double d = point1.x-start;
                    set((float) (startAtT+d/5));
                    Drawing.dirty(this);
                    // TODO: actual feedback
                    return !termination;
                };
            }
        } else {
            if (Math.abs(point.x - startAt.properties.get(frame).x) < Math.abs(
                    point.x - startAt.properties.get(frame).x - startAt.properties.get(frame).w)) {
                set(startAt.properties.get(frame).x);
                return null;
            } else {
                set(startAt.properties.get(frame).x + startAt.properties.get(frame).w);
                return null;
            }
        }

    }

    private void set(float to) {

        root.breadthFirst(root.both()).filter(x -> x instanceof TimeSlider).findFirst().ifPresent(x -> {
            Rect r = x.properties.get(frame);
            r.x = to;
            Rect eventually = Callbacks.frameChange(x, r);
            x.properties.put(frame, eventually);
        });
    }

    private float get() {

        return root.breadthFirst(root.both())
                .filter(x -> x instanceof TimeSlider)
                .findFirst()
                .map(x -> x.properties.get(frame).x)
                .orElse(0f);

    }
}
