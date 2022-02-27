package field.graphics.util

import field.graphics.Camera
import field.graphics.Window
import field.linalg.Mat4
import fieldbox.boxes.Mouse.OnMouseDown
import fieldbox.boxes.Mouse
import field.graphics.Window.MouseState
import fieldbox.boxes.Mouse.Dragger
import fieldbox.boxes.Drawing
import field.graphics.util.NotArcBallCamera
import field.linalg.Quat
import field.linalg.Vec3
import field.utility.minus
import field.utility.plus
import fieldbox.boxes.Box

/**
 * Created by marc on 12/30/16.
 */
class NotArcBallCamera(private val target: Camera, installInto: Box) {
    inner class Down(val from: Vec3, val fromState: Camera.State)

    var SCALE = 2.5

    fun install(b: Box) {
        b.properties.putToMap(Mouse.onMouseDown, "__arcball", OnMouseDown { e: Window.Event<MouseState>, button: Int ->
            val rr = b.properties.get(Box.frame)
            val x = (e.after.x - rr.x) / rr.w
            val y = (e.after.y - rr.y) / rr.h
            if (b.properties.isTrue(Mouse.isSelected, false) && x > 0 && x < 1 && y > 0 && y < 1) {
                e.properties.put(Window.consumed, true)
                val down = down(x, y)

                Dragger { e2: Window.Event<MouseState>, end: Boolean ->
                    e2.properties.put(Window.consumed, true)
                    val rr2 = b.properties.get(Box.frame)
                    val x2 = (e2.after.x - rr2.x) / rr.w
                    val y2 = (e2.after.y - rr2.y) / rr.h
                    val state = drag(down, x2 - x, y2 - y)
                    target.state = state
                    Drawing.dirty(b)
                    !end
                }
            } else null
        })
    }

    fun down(ndc_x: Double, ndc_y: Double): Down {
        val d: Down = Down(Vec3(ndc_x, ndc_y, 0.0), target.state)
        return d
    }

    fun drag(d: Down, ndc_x: Double, ndc_y: Double): Camera.State {

        println("DRAG ${ndc_x - d.from!!.x} ${ndc_y - d.from!!.y}")

        val o = d.fromState.copy()

        val r1 = Quat().fromAxisAngleRad(Vec3(0.0, 1.0, 0.0), (ndc_x) * SCALE)
        val p1 = (d.fromState.position - d.fromState.target).rotate(r1) + d.fromState.target

        val r2 = Quat().fromAxisAngleRad(
            Vec3(0.0, 1.0, 0.0).cross(d.fromState.position - d.fromState.target).normalize(),
            (ndc_y) * SCALE
        )

        val p2 = (p1 - d.fromState.target).rotate(r2) + d.fromState.target

        val u1 = o.up//.rotate(r2)

        o.up = u1
        o.position = p2
        return o
    }


    init {
        install(installInto)
    }
}