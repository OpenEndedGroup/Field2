package fieldkotlin

import field.app.RunLoop
import field.graphics.FLine
import field.graphics.StandardFLineDrawing
import field.linalg.Vec2
import field.linalg.Vec3
import field.linalg.Vec4
import field.utility.Dict
import field.utility.IdempotencyMap
import field.utility.plus
import fieldbox.boxes.Box
import fieldbox.boxes.Boxes
import fieldbox.boxes.FLineDrawing
import fieldbox.boxes.plugins.Planes
import fieldbox.execution.Execution
import fieldcef.plugins.up
import fieldlinker.AsMap
import java.io.File
import java.nio.file.Files
import java.util.function.Supplier
import kotlin.collections.List
import kotlin.collections.forEach
import kotlin.collections.mutableMapOf
import kotlin.collections.set
import kotlin.reflect.KProperty

var FLine.color: Supplier<Vec4> by FLineDelegate(StandardFLineDrawing.color)
var FLine.fillColor: Supplier<Vec4> by FLineDelegate(StandardFLineDrawing.fillColor)
var FLine.strokeColor: Supplier<Vec4> by FLineDelegate(StandardFLineDrawing.strokeColor)
var FLine.pointColor: Supplier<Vec4> by FLineDelegate(StandardFLineDrawing.pointColor)


var FLine.thicken: Number by FLineDelegateFloat(StandardFLineDrawing.fastThicken) // !!

var FLine.filled by FLineDelegate(StandardFLineDrawing.filled)
var FLine.stroked by FLineDelegate(StandardFLineDrawing.stroked)
var FLine.pointed by FLineDelegate(StandardFLineDrawing.pointed)

var Box.name by BoxDelegate(Box.name)
val Box.frame by BoxDelegate(Box.frame)
val Box.lines by BoxDelegate(FLineDrawing.lines)

val Box.lines3 by BoxDelegate(fieldbox.boxes.plugins.Viewport.lines3)

fun boxNamed(n: String, r: Box) = r.children.first { it.name == n }!!
fun boxNamed_plane(n: String, r: Box, plane: Box): Box {
    val p = plane.properties[Planes.plane]
    return r.children.first { it.name == n && it.properties[Planes.plane] == p }!!
}

val Box.begin
    get() = (this.asMap_get("begin") as Supplier<*>).get()
val Box.beginAgain
    get() = (this.asMap_get("beginAgain") as Supplier<*>).get()



val Box.end
    get() = (this.asMap_get("end") as Supplier<*>).get()


fun dur(n : Int) = (0 until n).map { it/(n-1.0) }

class AsMapDelegate<T>(val p: Dict.Prop<T>) {
    operator fun getValue(f: Box, property: KProperty<*>): T = f.asMap_get(p.name) as T

    operator fun setValue(f: Box, property: KProperty<*>, v: T) = f.asMap_set(p.name, v)
}


class Delegation<T> {
    operator fun getValue(f: AsMap, property: KProperty<*>): T = f.asMap_get(property.name) as T

    operator fun setValue(f: AsMap, property: KProperty<*>, v: T) = f.asMap_set(property.name, v)
}


class BoxDelegate<T>(val p: Dict.Prop<T>) {
    operator fun getValue(f: Box, property: KProperty<*>): T = f.asMap_get(p.name) as T

    operator fun setValue(f: Box, property: KProperty<*>, v: T) = f.asMap_set(p.name, v)
}


class FLineDelegate<T>(val p: Dict.Prop<T>) {
    operator fun getValue(f: FLine, property: KProperty<*>): T = f.attributes.get(p) as T

    operator fun setValue(f: FLine, property: KProperty<*>, v: T) = f.attributes.put(p, v)
}

class FLineDelegateFloat<T : Number>(val p: Dict.Prop<Float>) {
    operator fun getValue(f: FLine, property: KProperty<*>): T = f.attributes.get(p) as T

    operator fun setValue(f: FLine, property: KProperty<*>, v: Number) = f.attributes.put(p, v.toFloat())
}


operator fun <T> IdempotencyMap<T>.plusAssign(m: Pair<String, T>) {
    this.asMap_set(m.first, m.second)
}

operator fun <T> AsMap.plusAssign(m: Pair<String, T>) {
    this.asMap_set(m.first, m.second)
}

operator fun <T> IdempotencyMap<T>.plusAssign(m: T) {
    this.add(m)
}

operator fun <T> IdempotencyMap<T>.minusAssign(m: T) {
    this.removeValue(m)
}


inline operator fun <T> IntRange.invoke(value: (Int) -> T) = this.map(value)

inline operator fun <T> Int.invoke(value: (Int) -> T) = (0 until this).invoke(value)

inline operator fun <T> Double.invoke(value: (Double) -> T) = (0 until this.toInt()).map {
    value(it / this)
}

fun <T> Sequence<T>.loop(): Sequence<T> {
    var o = this
    return sequence {
        while (true) {
            var a = o.iterator()
            while (a.hasNext())
                yield(a.next())
        }
    }
}

operator fun <A, B> Sequence<A>.rem(b: Sequence<B>) = this.zip(b)

@JvmName("mixVec2Vec2")
infix fun kotlin.Pair<Vec2, Vec2>.mix(d: Number) = Vec2(this.first).lerp(this.second, d.toDouble())

@JvmName("towardsVec2Vec2")
infix fun kotlin.Pair<Vec2, Vec2>.towards(d: Number) =
    Vec2(this.first).lerp(this.second, d.toDouble() / this.second.distance(this.first))

@JvmName("mixVec3Vec3")
infix fun kotlin.Pair<Vec3, Vec3>.mix(d: Number) = Vec3(this.first).lerp(this.second, d.toDouble())

@JvmName("towardsVec3Vec3")
infix fun kotlin.Pair<Vec3, Vec3>.towards(d: Number) =
    Vec3(this.first).lerp(this.second, d.toDouble() / this.second.distance(this.first))

@JvmName("mixVec4Vec4")
infix fun kotlin.Pair<Vec4, Vec4>.mix(d: Number) = Vec4(this.first).lerp(this.second, d.toDouble())

@JvmName("towardsVec4Vec4")
infix fun kotlin.Pair<Vec4, Vec4>.towards(d: Number) =
    Vec4(this.first).lerp(this.second, d.toDouble() / this.second.distance(this.first))

suspend fun SequenceScope<Any>.yield() {
    yield(true)
}


private var _launch_uniq = 0

private var _alpha = 0.0
var alpha: Double
    get() = _alpha
    set(v) {
        _alpha = v
    }


fun launchSeq(frames: Int, name: String? = null, forceEnd: Boolean = false, s: suspend SequenceScope<Any>.() -> Unit) {
    var rn = name ?: "anon_${_launch_uniq++}"
    var ss = sequence(s).iterator()
    var frame = 0
    RunLoop.main.loop.attach(rn) { i ->
        alpha = ++frame / frames.toDouble()
        _ended = frame == frames
        if (frame > frames && forceEnd) return@attach false
        if (ss.hasNext()) {
            when (val z = ss.next()) {
                null -> true
                is Boolean -> z
                else -> throw IllegalArgumentException(" didn't understand launched return value $z")
            }
        }
        false
    }
}


fun launch(frames: Int, name: String? = null, s: (Double) -> Unit) {
    var rn = name ?: "anon_${_launch_uniq++}"
    var frame = 0
    RunLoop.main.loop.attach(rn) { i ->
        alpha = ++frame / frames.toDouble()
        s(alpha)
        alpha < 1
    }
}


val Int.alphas: Sequence<Double>
    get() = sequence {
        for (n in 0 until this@alphas) {
            yield(n / (this@alphas - 1.0))
        }
    }

fun List<Vec3>.max(): Vec3 {
    val q = Vec3(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY)
    this.forEach {
        q.max(it)
    }
    return q
}

fun List<Vec3>.min(): Vec3 {
    val q = Vec3(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY)
    this.forEach {
        q.min(it)
    }
    return q
}

fun List<Vec3>.bounds() = min() to max()

fun List<Vec3>.center(): Vec3 {
    val q = Vec3()
    this.forEach {
        q.add(it)
    }
    q.scale(1.0 / this.size)
    return q
}

var _ended = false
val Box.ended: Boolean
    get() = _ended

val __include_cache__ = mutableMapOf<kotlin.Pair<Box, String>, String>()

fun _include(name: String) = object : Magics.Magic {
    override fun run(inside: Box, executionService: (String) -> Any?): Any? {

        var code = if (!name.contains("/")) {

            val o = (inside up Boxes.root)!!.breadthFirstAll(inside.downwards()).filter {
                it.properties.get(Box.name) == name
            }.findFirst()

            if (!o.isPresent) throw IllegalArgumentException(" couldn't find include '$name'")

            var s = o.get()!!
            s.properties.get(Execution.code)
        } else {
            Files.readString(File(name).toPath())
        }
        val was = __include_cache__[inside to name]
        if (was == code) return null

        executionService.invoke(code)

        __include_cache__[inside to name] = code
        return null // signal success
    }
}
