package field.linalg

import field.graphics.FLine
import field.utility.Documentation
import field.utility.OverloadedMath
import fieldnashorn.annotations.HiddenInAutocomplete
import javafx.geometry.Point2D
import javafx.geometry.Point3D
import javafx.scene.transform.Affine
import javafx.scene.transform.Rotate
import javafx.scene.transform.Transform
import java.util.function.Function

/**
 * represents an arbitrary 2d transformation.
 */
class Transform2D : OverloadedMath {
    override fun __sub__(b: Any?): Any {

        when (b) {
            is Transform2D -> {
                val q = Affine(a)
                val z = Affine(b.a)
                z.invert()
                q.prepend(z)
                return Transform2D(q)
            }
            is Quat -> {
                val q = Affine(a)
                val t = Affine.rotate(-b.angle(), 0.0, 0.0)
                q.prepend(t)
                return Transform2D(q)
            }
            is Vec3 -> {
                val q = Affine(a)
                val t = Affine.translate(-b.x, -b.y)
                q.prepend(t)
                return Transform2D(q)
            }
            is Vec2 -> {
                val q = Affine(a)
                val t = Affine.translate(-b.x, -b.y)
                q.prepend(t)
                return Transform2D(q)
            }
            is FLine -> {
                // inverse?
                val q = Affine(a)
                q.invert()
                return b.byTransforming(Function { x -> Transform2D(q).__mul__(x) as Vec3 })
            }
            is Number -> {
                val q = Affine(a)
                val t = Affine.scale(-b.toDouble(), -b.toDouble())
                q.prepend(t)
                return Transform2D(q)
            }
            is OverloadedMath -> {
                return b.__rsub__(this)
            }
            else -> {
                throw IllegalArgumentException("can't add a transform and $b")
            }
        }
    }

    override fun __rsub__(b: Any?): Any {
        when (b) {
            is Transform2D -> {
                val q = Affine(a)
                val z = Affine(b.a)
                z.invert()
                q.append(z)
                return Transform2D(q)
            }
            is Quat -> {
                val q = Affine(a)
                val t = Affine.rotate(-b.angle(), 0.0, 0.0)
                q.append(t)
                return Transform2D(q)
            }
            is Vec3 -> {
                val q = Affine(a)
                val t = Affine.translate(-b.x, -b.y)
                q.append(t)
                return Transform2D(q)
            }
            is Vec2 -> {
                val q = Affine(a)
                val t = Affine.translate(-b.x, -b.y)
                q.append(t)
                return Transform2D(q)
            }
            is FLine -> {
                // inverse?
                val q = Affine(a)
                q.invert()
                return b.byTransforming(Function { x -> Transform2D(q).__mul__(x) as Vec3 })
            }
            is Number -> {
                val q = Affine(a)
                val t = Affine.scale(-b.toDouble(), -b.toDouble())
                q.prepend(t)
                return Transform2D(q)
            }
            else -> {
                throw IllegalArgumentException("can't add a transform and $b")
            }
        }
    }

    override fun __add__(b: Any?): Any {
        when (b) {
            is Transform2D -> {
                val q = Affine(a)
                q.prepend(b.a)
                return Transform2D(q)
            }
            is Quat -> {
                val q = Affine(a)
                val t = Affine.rotate(b.angle(), 0.0, 0.0)
                q.prepend(t)
                return Transform2D(q)
            }
            is Vec3 -> {
                val q = Affine(a)
                val t = Affine.translate(b.x, b.y)
                q.prepend(t)
                return Transform2D(q)
            }
            is Vec2 -> {
                val q = Affine(a)
                val t = Affine.translate(b.x, b.y)
                q.prepend(t)
                return Transform2D(q)
            }
            is FLine -> {
                // inverse?
                return b.byTransforming(Function { x -> __mul__(x) as Vec3 })
            }
            is Number -> {
                val q = Affine(a)
                val t = Affine.scale(b.toDouble(), b.toDouble())
                q.prepend(t)
                return Transform2D(q)
            }
            is OverloadedMath -> {
                return b.__rmul__(this)
            }
            else -> {
                throw IllegalArgumentException("can't add a transform and $b")
            }
        }

    }

    override fun __radd__(b: Any?): Any {
        when (b) {
            is Transform2D -> {
                val q = Affine(a)
                q.prepend(b.a)
                return Transform2D(q)
            }
            is Quat -> {
                val q = Affine(a)
                val t = Affine.rotate(b.angle(), 0.0, 0.0)
                q.append(t)
                return Transform2D(q)
            }
            is Vec3 -> {
                val q = Affine(a)
                val t = Affine.translate(b.x, b.y)
                q.append(t)
                return Transform2D(q)
            }
            is Vec2 -> {
                val q = Affine(a)
                val t = Affine.translate(b.x, b.y)
                q.append(t)
                return Transform2D(q)
            }
            is FLine -> {
                // inverse?
                return b.byTransforming(Function { x -> __mul__(x) as Vec3 })
            }
            is Number -> {
                val q = Affine(a)
                val t = Affine.scale(b.toDouble(), b.toDouble())
                q.prepend(t)
                return Transform2D(q)
            }
            else -> {
                throw IllegalArgumentException("can't add a transform and $b")
            }
        }
    }

    override fun __mul__(b: Any?): Any {

        when (b) {
            is Transform2D -> {
                val q = Affine(a)
                q.prepend(b.a)
                return Transform2D(q)
            }
            is Quat -> {
                val q = Affine(a)
                val t = Affine.rotate(b.angle(), 0.0, 0.0)
                q.prepend(t)
                return Transform2D(q)
            }
            is Vec3 -> {
                return toVec3(a.transform(b.x, b.y, b.z))
            }
            is Vec2 -> {
                return toVec2(a.transform(b.x, b.y))
            }
            is FLine -> {
                // inverse?
                return b.byTransforming(Function { x -> __mul__(x) as Vec3 })
            }
            is Number -> {
                val q = Affine(a)
                val t = Affine.scale(b.toDouble(), b.toDouble())
                q.prepend(t)
                return Transform2D(q)
            }
            is OverloadedMath -> {
                return b.__rmul__(this)
            }
            else -> {
                throw IllegalArgumentException("can't multiply a transform and $b")
            }
        }

    }

    override fun __rmul__(b: Any?): Any {
        when (b) {
            is Transform2D -> {
                val q = Affine(a)
                q.prepend(b.a)
                return Transform2D(q)
            }
            is Quat -> {
                val q = Affine(a)
                val t = Affine.rotate(b.angle(), 0.0, 0.0)
                q.append(t)
                return Transform2D(q)
            }
            is Vec3 -> {
                return toVec3(a.transform(b.x, b.y, b.z))
            }
            is Vec2 -> {
                return toVec2(a.transform(b.x, b.y))
            }
            is FLine -> {
                // inverse?
                return b.byTransforming(Function { x -> __mul__(x) as Vec3 })
            }
            is Number -> {
                val q = Affine(a)
                val t = Affine.scale(b.toDouble(), b.toDouble())
                q.append(t)
                return Transform2D(q)
            }
            else -> {
                throw IllegalArgumentException("can't multiply a transform and $b")
            }
        }
    }


    fun toVec3(a: Point3D): Vec3 = Vec3(a.x, a.y, a.z)
    fun toVec2(a: Point2D): Vec2 = Vec2(a.x, a.y)


    constructor(q: Transform) {
        this.a = q.clone()
    }

    @Documentation("returns a new transformation that's centered around `pivot`. For example, while `rotation(10)` rotates 10 degrees clockwise around the origin, `rotation(10).pivot(vec(10,10))` rotates 10 degrees around the point `10,10`.")
    fun pivot(pivot: Vec2): Transform2D = (translate(-pivot.x, -pivot.y).__mul__(this) as Transform2D).__mul__(translate(pivot.x, pivot.y)) as Transform2D
    @Documentation("returns a new transformation that's centered around `pivot`. For example, while `rotation(10)` rotates 10 degrees clockwise around the origin, `rotation(10).pivot(10,10)` rotates 10 degrees around the point `10,10`.")
    fun pivot(pivot_x: Number, pivot_y: Number): Transform2D = (translate(-pivot_x.toDouble(), -pivot_y.toDouble()).__mul__(this) as Transform2D).__mul__(translate(pivot_x.toDouble(), pivot_y.toDouble())) as Transform2D

    companion object {
        @JvmStatic
        fun rotate(angle: Double) = Transform2D(Transform.rotate(angle, 0.0, 0.0))

        @JvmStatic
        fun rotate(angle: Number, pivot: Vec2) = Transform2D(Transform.rotate(angle.toDouble(), pivot.x, pivot.y))

        @JvmStatic
        fun rotate(angle: Number, pivotx: Number, pivoty: Number) = Transform2D(Transform.rotate(angle.toDouble(), pivotx.toDouble(), pivoty.toDouble()))

        @JvmStatic
        fun translate(translate: Vec2) = Transform2D(Transform.translate(translate.x, translate.y))

        @JvmStatic
        fun translate(x: Number, y: Number) = Transform2D(Transform.translate(x.toDouble(), y.toDouble()))

        @JvmStatic
        fun translate(translate: Vec3) = Transform2D(Transform.translate(translate.x, translate.y))

        @JvmStatic
        fun scale(by: Number) = Transform2D(Transform.scale(by.toDouble(), by.toDouble()))

        @JvmStatic
        fun scale(x: Number, y: Number) = Transform2D(Transform.scale(x.toDouble(), y.toDouble()))

        @JvmStatic
        fun scale(by: Number, pivot: Vec2) = Transform2D(Transform.scale(by.toDouble(), by.toDouble(), pivot.x, pivot.y))

        @JvmStatic
        fun scale(x: Number, y: Number, pivot: Vec2) = Transform2D(Transform.scale(x.toDouble(), y.toDouble(), pivot.x, pivot.y))

        @JvmStatic
        fun scale(by: Vec2, pivot: Vec2) = Transform2D(Transform.scale(by.x, by.y, pivot.x, pivot.y))
    }


    //Mainly a wrapper around JavaFX's Affine class

    @HiddenInAutocomplete
    var a: Transform = Transform.translate(0.0, 0.0)


    override fun toString(): String = a.toString()

}