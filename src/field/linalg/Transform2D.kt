package field.linalg

import field.graphics.FLine
import field.utility.Documentation
import field.utility.OverloadedMath
import fieldnashorn.annotations.HiddenInAutocomplete
import java.awt.geom.AffineTransform
import java.awt.geom.Point2D

import java.util.function.Function

/**
 * represents an arbitrary 2d transformation.
 */
class Transform2D : OverloadedMath {
    override fun __sub__(b: Any?): Any {

        when (b) {
            is Transform2D -> {
                val q = AffineTransform(a)
                val z = AffineTransform(b.a)
                z.invert()
                q.preConcatenate(z)
                return Transform2D(q)
            }
            is Quat -> {
                val q = AffineTransform(a)
                val t = AffineTransform.getRotateInstance(-b.angle() * DEGTORAD, 0.0, 0.0)
                q.preConcatenate(t)
                return Transform2D(q)
            }
            is Vec3 -> {
                val q = AffineTransform(a)
                val t = AffineTransform.getTranslateInstance(-b.x, -b.y)
                q.preConcatenate(t)
                return Transform2D(q)
            }
            is Vec2 -> {
                val q = AffineTransform(a)
                val t = AffineTransform.getTranslateInstance(-b.x, -b.y)
                q.preConcatenate(t)
                return Transform2D(q)
            }
            is FLine -> {
                // inverse?
                val q = AffineTransform(a)
                q.invert()
                return b.byTransforming(Function { x -> Transform2D(q).__mul__(x) as Vec3 })
            }
            is Number -> {
                val q = AffineTransform(a)
                val t = AffineTransform.getScaleInstance(-b.toDouble(), -b.toDouble())
                q.preConcatenate(t)
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
                val q = AffineTransform(a)
                val z = AffineTransform(b.a)
                z.invert()
                q.concatenate(z)
                return Transform2D(q)
            }
            is Quat -> {
                val q = AffineTransform(a)
                val t = AffineTransform.getRotateInstance(-b.angle() * DEGTORAD, 0.0, 0.0)
                q.concatenate(t)
                return Transform2D(q)
            }
            is Vec3 -> {
                val q = AffineTransform(a)
                val t = AffineTransform.getTranslateInstance(-b.x, -b.y)
                q.concatenate(t)
                return Transform2D(q)
            }
            is Vec2 -> {
                val q = AffineTransform(a)
                val t = AffineTransform.getTranslateInstance(-b.x, -b.y)
                q.concatenate(t)
                return Transform2D(q)
            }
            is FLine -> {
                // inverse?
                val q = AffineTransform(a)
                q.invert()
                return b.byTransforming(Function { x -> Transform2D(q).__mul__(x) as Vec3 })
            }
            is Number -> {
                val q = AffineTransform(a)
                val t = AffineTransform.getScaleInstance(-b.toDouble(), -b.toDouble())
                q.preConcatenate(t)
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
                val q = AffineTransform(a)
                q.preConcatenate(b.a)
                return Transform2D(q)
            }
            is Quat -> {
                val q = AffineTransform(a)
                val t = AffineTransform.getRotateInstance(b.angle() * DEGTORAD, 0.0, 0.0)
                q.preConcatenate(t)
                return Transform2D(q)
            }
            is Vec3 -> {
                val q = AffineTransform(a)
                val t = AffineTransform.getTranslateInstance(b.x, b.y)
                q.preConcatenate(t)
                return Transform2D(q)
            }
            is Vec2 -> {
                val q = AffineTransform(a)
                val t = AffineTransform.getTranslateInstance(b.x, b.y)
                q.preConcatenate(t)
                return Transform2D(q)
            }
            is FLine -> {
                // inverse?
                return b.byTransforming(Function { x -> __mul__(x) as Vec3 })
            }
            is Number -> {
                val q = AffineTransform(a)
                val t = AffineTransform.getScaleInstance(b.toDouble(), b.toDouble())
                q.preConcatenate(t)
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
                val q = AffineTransform(a)
                q.preConcatenate(b.a)
                return Transform2D(q)
            }
            is Quat -> {
                val q = AffineTransform(a)
                val t = AffineTransform.getRotateInstance(b.angle() * DEGTORAD, 0.0, 0.0)
                q.concatenate(t)
                return Transform2D(q)
            }
            is Vec3 -> {
                val q = AffineTransform(a)
                val t = AffineTransform.getTranslateInstance(b.x, b.y)
                q.concatenate(t)
                return Transform2D(q)
            }
            is Vec2 -> {
                val q = AffineTransform(a)
                val t = AffineTransform.getTranslateInstance(b.x, b.y)
                q.concatenate(t)
                return Transform2D(q)
            }
            is FLine -> {
                // inverse?
                return b.byTransforming(Function { x -> __mul__(x) as Vec3 })
            }
            is Number -> {
                val q = AffineTransform(a)
                val t = AffineTransform.getScaleInstance(b.toDouble(), b.toDouble())
                q.preConcatenate(t)
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
                val q = AffineTransform(a)
                q.preConcatenate(b.a)
                return Transform2D(q)
            }
            is Quat -> {
                val q = AffineTransform(a)
                val t = AffineTransform.getRotateInstance(b.angle() * DEGTORAD, 0.0, 0.0)
                q.preConcatenate(t)
                return Transform2D(q)
            }
            is Vec3 -> {
                return toVec3(a.transform(toPoint(b)))
            }
            is Vec2 -> {
                return toVec2(a.transform(toPoint(b)))
            }
            is FLine -> {
                // inverse?
                return b.byTransforming(Function { x -> __mul__(x) as Vec3 })
            }
            is Number -> {
                val q = AffineTransform(a)
                val t = AffineTransform.getScaleInstance(b.toDouble(), b.toDouble())
                q.preConcatenate(t)
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

    override fun __div__(b: Any?): Any {
        val aa = AffineTransform(a)
        aa.invert()
        return Transform2D(aa).__mul__(b)
    }

    override fun __rdiv__(b: Any?): Any {
        val aa = AffineTransform(a)
        aa.invert()
        return Transform2D(aa).__rmul__(b)
    }

    private fun toVec3(pout: Point2D.Double): Vec3 {
        return Vec3(pout.x, pout.y, 0)
    }

    private fun toVec2(pout: Point2D.Double): Vec2 {
        return Vec2(pout.x, pout.y)
    }

    private fun toPoint(b: Vec3): Point2D {
        return Point2D.Double(b.x, b.y)
    }

    private fun toPoint(b: Vec2): Point2D {
        return Point2D.Double(b.x, b.y)
    }

    override fun __xor__(b: Any?): Any {
        if (b is OverloadedMath)
            b.__rxor__(this)
        throw IllegalArgumentException("can't xor a transform and $b")
    }

    override fun __rxor__(b: Any?): Any {
        throw IllegalArgumentException("can't xor $b and a transform")
    }

    override fun __rmul__(b: Any?): Any {
        when (b) {
            is Transform2D -> {
                val q = AffineTransform(a)
                q.preConcatenate(b.a)
                return Transform2D(q)
            }
            is Quat -> {
                val q = AffineTransform(a)
                val t = AffineTransform.getRotateInstance(b.angle() * DEGTORAD, 0.0, 0.0)
                q.concatenate(t)
                return Transform2D(q)
            }
            is Vec3 -> {
                return toVec3(a.transform(toPoint(b)))
            }
            is Vec2 -> {
                return toVec2(a.transform(toPoint(b)))
            }
            is FLine -> {
                // inverse?
                return b.byTransforming(Function { x -> __mul__(x) as Vec3 })
            }
            is Number -> {
                val q = AffineTransform(a)
                val t = AffineTransform.getScaleInstance(b.toDouble(), b.toDouble())
                q.concatenate(t)
                return Transform2D(q)
            }
            else -> {
                throw IllegalArgumentException("can't multiply a transform and $b")
            }
        }
    }


    constructor(q: AffineTransform) {
        this.a = AffineTransform(q)
    }

    @Documentation("returns a new transformation that's centered around `pivot`. For example, while `rotation(10)` rotates 10 degrees clockwise around the origin, `rotation(10).pivot(vec(10,10))` rotates 10 degrees around the point `10,10`.")
    fun pivot(pivot: Vec2): Transform2D = (translate(-pivot.x, -pivot.y).__mul__(this) as Transform2D).__mul__(translate(pivot.x, pivot.y)) as Transform2D

    @Documentation("returns a new transformation that's centered around `pivot`. For example, while `rotation(10)` rotates 10 degrees clockwise around the origin, `rotation(10).pivot(10,10)` rotates 10 degrees around the point `10,10`.")
    fun pivot(pivot_x: Number, pivot_y: Number): Transform2D = (translate(-pivot_x.toDouble(), -pivot_y.toDouble()).__mul__(this) as Transform2D).__mul__(translate(pivot_x.toDouble(), pivot_y.toDouble())) as Transform2D

    companion object {
        @JvmStatic
        fun rotate(angle: Double) = Transform2D(AffineTransform.getRotateInstance(angle * DEGTORAD, 0.0, 0.0)).describe("rotate by $angle (clockwise)")

        @JvmStatic
        fun rotate(angle: Number, pivot: Vec2) = Transform2D(AffineTransform.getRotateInstance(angle.toDouble() * DEGTORAD, pivot.x, pivot.y)).describe("rotate by $angle (clockwise) around $pivot")

        @JvmStatic
        fun rotate(angle: Number, pivotx: Number, pivoty: Number) = Transform2D(AffineTransform.getRotateInstance(angle.toDouble() * DEGTORAD, pivotx.toDouble(), pivoty.toDouble())).describe("rotate by $angle (clockwise) around $pivotx, $pivoty")

        @JvmStatic
        fun translate(translate: Vec2) = Transform2D(AffineTransform.getTranslateInstance(translate.x, translate.y)).describe("translate by $translate")

        @JvmStatic
        fun translate(x: Number, y: Number) = Transform2D(AffineTransform.getTranslateInstance(x.toDouble(), y.toDouble())).describe("translate by $x, $y")

        @JvmStatic
        fun translate(translate: Vec3) = Transform2D(AffineTransform.getTranslateInstance(translate.x, translate.y)).describe("translate by $translate")

        @JvmStatic
        fun scale(by: Number) = Transform2D(AffineTransform.getScaleInstance(by.toDouble(), by.toDouble())).describe("scale by $by")

        @JvmStatic
        fun scale(x: Number, y: Number) = Transform2D(AffineTransform.getScaleInstance(x.toDouble(), y.toDouble())).describe("scale by $x x $y")

        @JvmStatic
        fun scale(by: Number, pivot: Vec2) = Transform2D(AffineTransform.getScaleInstance(by.toDouble(), by.toDouble())).pivot(pivot.x, pivot.y).describe("scale by $by around $pivot")

        @JvmStatic
        fun scale(x: Number, y: Number, pivot: Vec2) = Transform2D(AffineTransform.getScaleInstance(x.toDouble(), y.toDouble())).pivot(pivot.x, pivot.y).describe("scale by $x x $y, around $pivot")

        @JvmStatic
        fun scale(by: Vec2, pivot: Vec2) = Transform2D(AffineTransform.getScaleInstance(by.x, by.y)).pivot(pivot.x, pivot.y).describe("scale by $by around $pivot")

        const val DEGTORAD = Math.PI / 180
    }

    //Mainly a wrapper around JavaFX's AffineTransform class

    @HiddenInAutocomplete
    var a: AffineTransform = AffineTransform.getTranslateInstance(0.0, 0.0)

    override fun toString(): String = description ?: a.toString()

    var description: String? = null

    fun describe(s: String): Transform2D {
        description = s
        return this
    }

}

private fun AffineTransform.transform(toPoint: Point2D): Point2D.Double {
    var o = Point2D.Double()
    this.transform(toPoint, o)
    return o
}
