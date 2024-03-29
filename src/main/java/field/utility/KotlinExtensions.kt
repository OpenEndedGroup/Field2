package field.utility

import field.graphics.Bracketable
import field.graphics.FLine
import field.linalg.Quat
import field.linalg.Vec2
import field.linalg.Vec3
import field.linalg.Vec4
import fieldbox.boxes.Box
import fieldlinker.AsMap
import kotlin.Pair
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Grab-bag of Kotlin Extension functions for doing things in the Field codebase
 */

operator fun <T> AsMap.plusAssign(pair: Pair<Dict.Prop<T>, T>) {
    this.asMap_set(pair.first.name, pair.second)
}


operator fun <T> AsMap.plusAssign(pair: Dict.Prop<Boolean>) {
    this.asMap_set(pair.name, true)
}

operator fun <T> AsMap.set(k: Dict.Prop<T>, v: T) {
    this.asMap_set(k.name, v)
}


operator fun <T> Box.set(k: Dict.Prop<T>, v: T) {
    this.properties[k] = v
}

operator fun <T> Box.get(k: Dict.Prop<T>) = this.properties[k]


inline fun <R, T : Bracketable> T.use(b: (T) -> R): R {
    this.open()
    var r: R
    try {
        r = b(this)
    } finally {
        this.close()
    }
    return r
}

val Number.d: Double
    get() = this.toDouble()

val String.d: Double
    get() = this.toDouble()

val String.i: Int
    get() = this.toInt()

val Number.i: Int
    get() = this.toInt()

val Int.d: Double
    get() = this.toDouble()

val Double.i: Int
    get() = this.toInt()

val Byte.d: Double
    get() = this.toDouble()

val Byte.i: Int
    get() = this.toInt()


/**
 * Threadlocal delegate, missing from Kotlin Stdlib?
 */

class PerThread<T>(val initial: () -> T) : ReadWriteProperty<Any, T> {

    val t: ThreadLocal<T> = ThreadLocal.withInitial { initial() }

    override fun getValue(thisRef: Any, property: KProperty<*>): T {
        return t.get()
    }

    override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
        t.set(value)
    }

}


/**
 * Linalg work below
 */


fun vec(x: Number, y: Number) = Vec2(x.d, y.d)
fun vec(x: Number, y: Number, z: Number) = Vec3(x.d, y.d, z.d)
fun vec(x: Number, y: Number, z: Number, w: Number) = Vec4(x.d, y.d, z.d, w.d)

operator fun Vec2.plus(other: Vec2): Vec2 = Vec2(this.x + other.x, this.y + other.y)

operator fun Vec3.plus(other: Vec3): Vec3 = Vec3(this.x + other.x, this.y + other.y, this.z + other.z)

operator fun Vec4.plus(other: Vec4): Vec4 = Vec4(this.x + other.x, this.y + other.y, this.z + other.z, this.w + other.w)

operator fun Vec2.minus(other: Vec2): Vec2 = Vec2(this.x - other.x, this.y - other.y)

operator fun Vec3.minus(other: Vec3): Vec3 = Vec3(this.x - other.x, this.y - other.y, this.z - other.z)

operator fun Vec4.minus(other: Vec4): Vec4 =
    Vec4(this.x - other.x, this.y - other.y, this.z - other.z, this.w - other.w)

operator fun Vec2.times(other: Number): Vec2 = Vec2(this.x * other.toDouble(), this.y * other.toDouble())

operator fun Vec3.times(other: Number): Vec3 =
    Vec3(this.x * other.toDouble(), this.y * other.toDouble(), this.z * other.toDouble())

operator fun Vec2.times(other: Vec2): Vec2 = Vec2(this.x * other.x, this.y * other.y)

operator fun Vec3.times(other: Double): Vec3 = Vec3(this.x * other, this.y * other, this.z * other)

operator fun Vec3.times(other: Vec3): Vec3 = Vec3(this.x * other.x, this.y * other.y, this.z * other.z)

operator fun Vec4.times(other: Double): Vec4 = Vec4(this.x * other, this.y * other, this.z * other, this.w * other)

operator fun Vec4.times(other: Number): Vec4 =
    Vec4(this.x * other.toDouble(), this.y * other.toDouble(), this.z * other.toDouble(), this.w * other.toDouble())

infix fun Vec2.dot(v: Vec2) = this.dot(v)
infix fun Vec3.dot(v: Vec3) = this.dot(v)
infix fun Vec4.dot(v: Vec4) = this.dot(v)

fun Vec2.invoke(x: Double? = null, y: Double? = null): Vec2 {
    val v = Vec2(this)
    if (x != null)
        v.x = x
    if (y != null)
        v.y = y
    return v
}

fun Vec3.invoke(x: Double? = null, y: Double? = null, z: Double? = null): Vec3 {
    val v = Vec3(this)
    if (x != null)
        v.x = x
    if (y != null)
        v.y = y
    if (z != null)
        v.z = z
    return v
}

fun Vec4.invoke(x: Double? = null, y: Double? = null, z: Double? = null, w: Double? = null): Vec4 {
    val v = Vec4(this)
    if (x != null)
        v.x = x
    if (y != null)
        v.y = y
    if (z != null)
        v.z = z
    if (w != null)
        v.w = w
    return v
}

operator fun Vec2.remAssign(to: Vec2) {
    this.set(to)
};
operator fun Vec3.remAssign(to: Vec3) {
    this.set(to)
};
operator fun Vec4.remAssign(to: Vec4) {
    this.set(to)
};

operator fun Vec2.compareTo(n: Number): Int = this.length().compareTo(n.toDouble())
operator fun Vec3.compareTo(n: Number): Int = this.length().compareTo(n.toDouble())
operator fun Vec4.compareTo(n: Number): Int = this.length().compareTo(n.toDouble())


operator fun Vec2.timesAssign(other: Double) {
    this.mul(other)
}

operator fun Vec3.timesAssign(other: Double) {
    this.mul(other)
}

operator fun Vec4.timesAssign(other: Double) {
    this.mul(other)
}

fun Vec2(x: Number = 0, y: Number = 0) = Vec2().set(x.toDouble(), y.toDouble())
fun Vec3(x: Number = 0, y: Number = 0, z: Number = 0) = Vec3().set(x.toDouble(), y.toDouble(), z.toDouble())
fun Vec4(x: Number = 0, y: Number = 0, z: Number = 0, w: Number = 0) =
    Vec4().set(x.toDouble(), y.toDouble(), z.toDouble(), w.toDouble())


fun Vec3(x: List<Number>, offset : Int = 0) =
    Vec3().set(x[offset].d, x[offset+1].d, x[offset+2].d)
fun Vec4(x: List<Number>, offset : Int = 0) =
    Vec4().set(x[offset].d, x[offset+1].d, x[offset+2].d, x[offset+3].d)
fun Vec2(x: List<Number>, offset : Int = 0) =
    Vec2().set(x[offset].d, x[offset+1].d)


var Vec4.xyz: Vec3
    get() = Vec3(this.x, this.y, this.z)
    set(v: Vec3) {
        this.x = v.x
        this.y = v.y
        this.z = v.z
    }

var Vec4.xy: Vec2
    get() = Vec2(this.x, this.y)
    set(v: Vec2) {
        this.x = v.x
        this.y = v.y
    }

var Vec3.xy: Vec2
    get() = Vec2(this.x, this.y)
    set(v: Vec2) {
        this.x = v.x
        this.y = v.y
    }

var Vec3.yx: Vec2
    get() = Vec2(this.y, this.x)
    set(v: Vec2) {
        this.x = v.y
        this.y = v.x
    }


operator fun FLine.plus(f: FLine) = this.__add__(f) as FLine
operator fun FLine.plus(f: Vec2) = this.__add__(f) as FLine
operator fun FLine.plus(f: Vec3) = this.__add__(f) as FLine
operator fun FLine.plus(f: Quat) = this.__add__(f) as FLine

operator fun FLine.minus(f: FLine) = this.__sub__(f) as FLine
operator fun FLine.minus(f: Vec2) = this.__sub__(f) as FLine
operator fun FLine.minus(f: Vec3) = this.__sub__(f) as FLine
operator fun FLine.minus(f: Quat) = this.__sub__(f) as FLine

operator fun FLine.times(f: FLine) = this.__mul__(f) as FLine
operator fun FLine.times(f: Vec2) = this.__mul__(f) as FLine
operator fun FLine.times(f: Vec3) = this.__mul__(f) as FLine
operator fun FLine.times(f: Quat) = this.__mul__(f) as FLine
operator fun FLine.times(f: Number) = this.__mul__(f.toDouble()) as FLine

infix fun FLine.xor(f2: FLine) = this.__xor__(f2) as FLine

