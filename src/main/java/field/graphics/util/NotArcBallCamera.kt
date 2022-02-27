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
import fieldbox.boxes.Box

/**
 * Created by marc on 12/30/16.
 */
class NotArcBallCamera(private val target: Camera, installInto: Box) {
    inner class Down {
        var from: Vec3? = null
        var fromState: Camera.State? = null
        var fromView: Mat4? = null
    }

    fun install(b: Box) {
        b.properties.putToMap(Mouse.onMouseDown, "__arcball", OnMouseDown { e: Window.Event<MouseState>, button: Int ->
            val rr = b.properties.get(Box.frame)
            val x = (e.after.x - rr.x) / rr.w
            val y = (e.after.y - rr.y) / rr.h
            if (b.properties.isTrue(Mouse.isSelected, false) && x > 0 && x < 1 && y > 0 && y < 1) {
                e.properties.put(Window.consumed, true)
            } else return@putToMap null
            val down = down(x, y)
            Dragger { e2: Window.Event<MouseState>, end: Boolean ->
                e2.properties.put(Window.consumed, true)
                val rr2 = b.properties.get(Box.frame)
                val x2 = (e2.after.x - rr2.x) / rr.w
                val y2 = (e2.after.y - rr2.y) / rr.h
                val state = drag(down, x2, y2)
                target.state = state
                Drawing.dirty(b)
                !end
            }
        })
    }

    fun down(ndc_x: Double, ndc_y: Double): Down {
        val d: Down = Down()
        d.from = projectToSphere(1.0, ndc_x, ndc_y, Vec3())
        d.fromState = target.state
        d.fromView = target.view()
        return d
    }

    fun drag(d: Down, ndc_x: Double, ndc_y: Double): Camera.State {
        val r1 = Quat().fromAxisAngleRad(Vec3(0, 1, 0), ndc_x - d.from!!.x)
    }

    companion object {
        fun projectToSphere(r: Double, x: Double, y: Double, v: Vec3?): Vec3 {
            var v = v
            if (v == null) v = Vec3()
            val d = Math.sqrt(x * x + y * y)
            val z: Double
            z = if (d < r * 0.70710678118654752440) {
                /* Inside sphere */
                Math.sqrt(r * r - d * d)
            } else {
                /* On hyperbola */
                val t = r / 1.41421356237309504880
                t * t / d
            }
            return v.set(x, y, z)
        }
    }

    init {
        install(installInto)
    }
}