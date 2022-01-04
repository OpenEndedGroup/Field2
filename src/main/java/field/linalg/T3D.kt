package field.linalg

import field.graphics.FLine
import field.utility.Documentation
import field.utility.OverloadedMath
import fieldnashorn.annotations.HiddenInAutocomplete
import java.util.function.Function
import javax.vecmath.AxisAngle4d
import javax.vecmath.Point3d
import javax.vecmath.Quat4d
import javax.vecmath.Vector3d

/**
 * represents an arbitrary 2d transformations.
 */
class T3D : OverloadedMath {
    override fun __sub__(b: Any?): Any {

        when (b) {
            is T3D -> {
                val q = Transform3D(a)
                val z = Transform3D(b.a)
                z.invert()
                z.mul(q)
                return T3D(z)
            }
            is Quat -> {
                val q = Transform3D(a)
                val t = Transform3D()
                t.setRotation(Quat4d(b.x, b.y, b.z, b.w))
                t.invert()
                t.mul(q)
                return T3D(t)
            }
            is Vec3 -> {
                val q = Transform3D(a)
                val t = Transform3D()
                t.setTranslation(Vector3d(b.x, b.y, b.z))
                t.invert()
                t.mul(q)
                return T3D(t)
            }
            is Vec2 -> {
                val q = Transform3D(a)
                val t = Transform3D()
                t.setTranslation(Vector3d(b.x, b.y, 0.0))
                t.invert()
                t.mul(q)
                return T3D(t)
            }
            is FLine -> {
                // inverse?
                val q = Transform3D(a)
                q.invert()
                return b.byTransforming(Function { x -> T3D(q).__mul__(x) as Vec3 })
            }
            is Number -> {
                val q = Transform3D(a)
                val t = Transform3D()
                t.setScale(Vector3d(b.toDouble(), b.toDouble(), b.toDouble()))
                t.invert()
                t.mul(q)
                return T3D(t)
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

        throw IllegalArgumentException("can't add a transform and $b")
    }

    override fun __add__(b: Any?): Any {
        when (b) {
            is T3D -> {
                val q = Transform3D(a)
                val z = Transform3D(b.a)
                z.mul(q)
                return T3D(z)
            }
            is Quat -> {
                val q = Transform3D(a)
                val t = Transform3D()
                t.setRotation(Quat4d(b.x, b.y, b.z, b.w))
                t.mul(q)
                return T3D(t)
            }
            is Vec3 -> {
                val q = Transform3D(a)
                val t = Transform3D()
                t.setTranslation(Vector3d(b.x, b.y, b.z))
                t.mul(q)
                return T3D(t)
            }
            is Vec2 -> {
                val q = Transform3D(a)
                val t = Transform3D()
                t.setTranslation(Vector3d(b.x, b.y, 0.0))
                t.mul(q)
                return T3D(t)
            }
            is FLine -> {
                // inverse?
                val q = Transform3D(a)
                return b.byTransforming(Function { x -> T3D(q).__mul__(x) as Vec3 })
            }
            is Number -> {
                val q = Transform3D(a)
                val t = Transform3D()
                t.setScale(Vector3d(b.toDouble(), b.toDouble(), b.toDouble()))
                t.mul(q)
                return T3D(t)
            }
            is OverloadedMath -> {
                return b.__radd__(this)
            }
            else -> {
                throw IllegalArgumentException("can't add a transform and $b")
            }
        }

    }

    override fun __radd__(b: Any?): Any {
        when (b) {
            is T3D -> {
                val q = Transform3D(a)
                val z = Transform3D(b.a)
                z.mul(q)
                return T3D(z)
            }
            is Quat -> {
                val q = Transform3D(a)
                val t = Transform3D()
                t.setRotation(Quat4d(b.x, b.y, b.z, b.w))
                t.mul(q)
                return T3D(t)
            }
            is Vec3 -> {
                val q = Transform3D(a)
                val t = Transform3D()
                t.setTranslation(Vector3d(b.x, b.y, b.z))
                t.mul(q)
                return T3D(t)
            }
            is Vec2 -> {
                val q = Transform3D(a)
                val t = Transform3D()
                t.setTranslation(Vector3d(b.x, b.y, 0.0))
                t.mul(q)
                return T3D(t)
            }
            is FLine -> {
                // inverse?
                val q = Transform3D(a)
                return b.byTransforming(Function { x -> T3D(q).__mul__(x) as Vec3 })
            }
            is Number -> {
                val q = Transform3D(a)
                val t = Transform3D()
                t.setScale(Vector3d(b.toDouble(), b.toDouble(), b.toDouble()))
                t.mul(q)
                return T3D(t)
            }
            is OverloadedMath -> {
                return b.__radd__(this)
            }
            else -> {
                throw IllegalArgumentException("can't add a transform and $b")
            }

        }
    }

    override fun __mul__(b: Any?): Any {

        when (b) {
            is T3D -> {
                val q = Transform3D(a)
                val z = Transform3D(b.a)
                z.mul(q)
                return T3D(z)
            }
            is Quat -> {
                val q = Transform3D(a)
                val t = Transform3D()
                t.setRotation(Quat4d(b.x, b.y, b.z, b.w))
                t.mul(q)
                return T3D(t)
            }
            is Vec3 -> {
                return toVec3(toPoint(b).apply { a.transform(this) })
            }
            is Vec2 -> {
                return toVec3(toPoint(b).apply { a.transform(this) })
            }
            is FLine -> {
                // inverse?
                val q = Transform3D(a)
                return b.byTransforming(Function { x -> T3D(q).__mul__(x) as Vec3 })
            }
            is Number -> {
                val q = Transform3D(a)
                val t = Transform3D()
                t.setScale(Vector3d(b.toDouble(), b.toDouble(), b.toDouble()))
                t.mul(q)
                return T3D(t)
            }
            is OverloadedMath -> {
                return b.__radd__(this)
            }
            else -> {
                throw IllegalArgumentException("can't add a transform and $b")
            }
        }
    }

    private fun toVec3(v: Vector3d): Vec3 {
        return Vec3(v.x, v.y, v.z)
    }


    private fun toVec3(v: Point3d): Vec3 {
        return Vec3(v.x, v.y, v.z)
    }


    private fun toPoint(v: Vec3): Point3d {
        return Point3d(v.x, v.y, v.z)
    }

    private fun toPoint(v: Vec2): Point3d {
        return Point3d(v.x, v.y, 0.0)
    }

    override fun __div__(b: Any?): Any {
        val aa = Transform3D(a)
        aa.invert()
        return T3D(aa).__mul__(b)
    }

    override fun __rdiv__(b: Any?): Any {
        val aa = Transform3D(a)
        aa.invert()
        return T3D(aa).__rmul__(b)
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
            is T3D -> {
                val q = Transform3D(a)
                val z = Transform3D(b.a)
                z.mul(q)
                return T3D(z)
            }
            is Quat -> {
                val q = Transform3D(a)
                val t = Transform3D()
                t.setRotation(Quat4d(b.x, b.y, b.z, b.w))
                t.mul(q)
                return T3D(t)
            }
            is Vec3 -> {
                return toVec3(toPoint(b).apply { a.transform(this) })
            }
            is Vec2 -> {
                return toVec3(toPoint(b).apply { a.transform(this) })
            }
            is FLine -> {
                // inverse?
                val q = Transform3D(a)
                return b.byTransforming(Function { x -> T3D(q).__mul__(x) as Vec3 })
            }
            is Number -> {
                val q = Transform3D(a)
                val t = Transform3D()
                t.setScale(Vector3d(b.toDouble(), b.toDouble(), b.toDouble()))
                t.mul(q)
                return T3D(t)
            }
            is OverloadedMath -> {
                return b.__radd__(this)
            }
            else -> {
                throw IllegalArgumentException("can't add a transform and $b")
            }
        }
    }


    constructor(q: Transform3D) {
        this.a = Transform3D(q)
    }

    @Documentation("returns a new transformation that's centered around `pivot`. For example, while `rotation(10)` rotates 10 degrees clockwise around the origin, `rotation(10).pivot(vec(10,10))` rotates 10 degrees around the point `10,10`.")
    fun pivot(pivot: Vec2): T3D = (translate(-pivot.x, -pivot.y).__mul__(this) as T3D).__mul__(translate(pivot.x, pivot.y)) as T3D

    @Documentation("returns a new transformation that's centered around `pivot`. For example, while `rotation(10)` rotates 10 degrees clockwise around the origin, `rotation(10).pivot(vec(10,10))` rotates 10 degrees around the point `10,10`.")
    fun pivot(pivot: Vec3): T3D = (translate(-pivot.x, -pivot.y, -pivot.z).__mul__(this) as T3D).__mul__(translate(pivot.x, pivot.y, pivot.z)) as T3D

    @Documentation("returns a new transformation that's centered around `pivot`. For example, while `rotation(10)` rotates 10 degrees clockwise around the origin, `rotation(10).pivot(10,10)` rotates 10 degrees around the point `10,10`.")
    fun pivot(pivot_x: Number, pivot_y: Number): T3D = (translate(-pivot_x.toDouble(), -pivot_y.toDouble()).__mul__(this) as T3D).__mul__(translate(pivot_x.toDouble(), pivot_y.toDouble())) as T3D

    @Documentation("returns a new transformation that's centered around `pivot`. For example, while `rotation(10)` rotates 10 degrees clockwise around the origin, `rotation(10).pivot(10,10)` rotates 10 degrees around the point `10,10`.")
    fun pivot(pivot_x: Number, pivot_y: Number, pivot_z: Number): T3D = (translate(-pivot_x.toDouble(), -pivot_y.toDouble(), -pivot_z.toDouble()).__mul__(this) as T3D).__mul__(translate(pivot_x.toDouble(), pivot_y.toDouble(), pivot_z.toDouble())) as T3D

    companion object {
        @JvmStatic
        fun rotate(angle: Double) = T3D(Transform3D().apply { this.setRotation(AxisAngle4d(0.0, 0.0, 1.0, angle * DEGTORAD)) }).describe("rotate by $angle (clockwise) around z")

        @JvmStatic
        fun rotate(angle: Double, axis: Vec3) = T3D(Transform3D().apply { this.setRotation(AxisAngle4d(axis.x, axis.y, axis.z, angle * DEGTORAD)) }).describe("rotate by $angle (clockwise) around $axis")

        @JvmStatic
        fun translate(translate: Vec2) = T3D(Transform3D().apply { this.setTranslation(javax.vecmath.Vector3d(translate.x, translate.y, 0.0)) }).describe("translate by $translate")

        @JvmStatic
        fun translate(x: Number, y: Number) = translate(Vec3(x.toDouble(), y.toDouble(), 0.0))

        @JvmStatic
        fun translate(x: Number, y: Number, z: Number) = translate(Vec3(x.toDouble(), y.toDouble(), z.toDouble()))

        @JvmStatic
        fun translate(translate: Vec3) = T3D(Transform3D().apply { this.setTranslation(javax.vecmath.Vector3d(translate.x, translate.y, translate.z)) }).describe("translate by $translate")

        @JvmStatic
        fun scale(by: Number) = T3D(Transform3D().apply { this.setScale(javax.vecmath.Vector3d(by.toDouble(), by.toDouble(), by.toDouble())) }).describe("scale by $by")

        @JvmStatic
        fun scale(x: Number, y: Number) = T3D(Transform3D().apply { this.setScale(javax.vecmath.Vector3d(x.toDouble(), y.toDouble(), 0.0)) }).describe("scale by $x x $y x 0")

        @JvmStatic
        fun scale(x: Number, y: Number, z: Number) = T3D(Transform3D().apply { this.setScale(javax.vecmath.Vector3d(x.toDouble(), y.toDouble(), z.toDouble())) }).describe("scale by $x x $y x $z")

        @JvmStatic
        fun scale(x: Vec3) = T3D(Transform3D().apply { this.setScale(javax.vecmath.Vector3d(x.x, x.y, x.z)) }).describe("scale by $x")

        const val DEGTORAD = Math.PI / 180
    }

    //Mainly a wrapper around Java3D's Transform3D class

    @HiddenInAutocomplete
    var a: Transform3D = Transform3D()

    override fun toString(): String = description ?: a.toString()

    var description: String? = null

    fun describe(s: String): T3D {
        description = s
        return this
    }

}
