package fielded.live

import field.linalg.Vec2
import field.linalg.Vec3
import field.linalg.Vec4
import field.utility.OverloadedMath
import field.utility.PerThread
import field.utility.Vec2
import field.utility.Vec3
import field.utility.Dict
import fieldbox.boxes.Box
import fieldcef.plugins.up
import fieldnashorn.babel.SourceTransformer
import jdk.nashorn.api.tree.*
import java.lang.reflect.Method
import java.util.*
import java.util.function.Function

/*    AST based rewriting
 */
class Asta {

    val disabled = field.utility.Options.dict().isTrue(Dict.Prop<Number>("noOverloads"), false) == true;

    class Options {
        var overloads = false
        var numbers = false
        var functionRewrite = false
        fun on(): Boolean = overloads || numbers || functionRewrite
    }

    var options by PerThread { Options() }

    var currentMapping = mutableMapOf<Pair<Int, Int>, Pair<Int, Int>>()
    var lastReturn = mutableMapOf<Pair<Int, Int>, Double>()
    var currentContents: String? = null
    var baseContents: String? = null

    fun __LNC__(start: Int, end: Int, def: Double): Double {
        val c = currentContents
        if (c != null) {
            val m = currentMapping.get(start to end)
            if (m != null) {
                val newText = c.substring(m.first until m.second)
                val dnl = newText.toDoubleOrNull()
                if (dnl != null) {
                    lastReturn.put(start to end, dnl)
                    return dnl
                } else {
                    if (debug) println(" text updated to non-number " + newText + " we could try evaluating it?")
                    return lastReturn.getOrDefault(start to end, def)
                }
            } else {
                if (currentMapping.size == 0) // no changes
                {
                    return def
                } else {
                    // indicate failure with closest matching area ?
                    if (debug) println(" couldn't find mapping for number, structural change? ")
                    return lastReturn.getOrDefault(start to end, def)
                }
            }
        }
        return def
    }

    companion object {

        @JvmStatic
        var debug = false

        @JvmStatic
        fun __MINUS__(va: Any?, vb: Any?): Any? {

            var a = va
            var b = vb

            if (a is OverloadedMath.ReducedWhenOverloaded) a = a.get()
            if (b is OverloadedMath.ReducedWhenOverloaded) b = b.get()

            if (a is Number && b is Number) return a.toDouble() - b.toDouble()


            if (a == null) throw NullPointerException("can't subtract `$a` and `$b`")
            if (b == null) throw NullPointerException("can't subtract `$a` and `$b`")

            return when (a) {
                is Int -> return when (b) {
                    is Int -> a - b
                    is Float -> a - b
                    is Double -> a - b
                    is Long -> a - b
                    is Vec2 -> Vec2(a, a).sub(b)
                    is Vec3 -> Vec3(a, a, a).sub(b)
                    is Vec4 -> Vec4(a.toDouble(), a.toDouble(), a.toDouble(), 1.0).sub(b)
                    is OverloadedMath -> b.__rsub__(a)
                    else -> throw NullPointerException("can't subtract `$a` and `$b`")
                }
                is Float -> return when (b) {
                    is Int -> a - b
                    is Float -> a - b
                    is Double -> a - b
                    is Long -> a - b
                    is Vec2 -> Vec2(a, a).sub(b)
                    is Vec3 -> Vec3(a, a, a).sub(b)
                    is Vec4 -> Vec4(a.toDouble(), a.toDouble(), a.toDouble(), 1.0).sub(b)
                    is OverloadedMath -> b.__rsub__(a)
                    else -> throw NullPointerException("can't subtract `$a` and `$b`")
                }
                is Double -> return when (b) {
                    is Int -> a - b
                    is Float -> a - b
                    is Double -> a - b
                    is Long -> a - b
                    is Vec2 -> Vec2(a, a).sub(b)
                    is Vec3 -> Vec3(a, a, a).sub(b)
                    is Vec4 -> Vec4(a.toDouble(), a.toDouble(), a.toDouble(), 1.0).sub(b)
                    is OverloadedMath -> b.__rsub__(a)
                    else -> throw NullPointerException("can't subtract `$a` and `$b`")
                }
                is Long -> return when (b) {
                    is Int -> a - b
                    is Float -> a - b
                    is Double -> a - b
                    is Long -> a - b
                    is Vec2 -> Vec2(a, a).sub(b)
                    is Vec3 -> Vec3(a, a, a).sub(b)
                    is Vec4 -> Vec4(a.toDouble(), a.toDouble(), a.toDouble(), 1.0).sub(b)
                    is OverloadedMath -> b.__rsub__(a)
                    else -> throw NullPointerException("can't subtract `$a` and `$b`")
                }
                is Vec2 -> return when (b) {
                    is Int -> Vec2(a).sub(Vec2(b, b))
                    is Float -> Vec2(a).sub(Vec2(b, b))
                    is Double -> Vec2(a).sub(Vec2(b, b))
                    is Long -> Vec2(a).sub(Vec2(b, b))
                    is Vec2 -> Vec2(a).sub(b)
                    is Vec3 -> a.toVec3().sub(b)
                    is Vec4 -> Vec4(a.x, a.y, 0.0, 1.0).sub(b)
                    is OverloadedMath -> b.__rsub__(a)
                    else -> throw NullPointerException("can't subtract `$a` and `$b`")
                }
                is Vec3 -> return when (b) {
                    is Int -> Vec3(a).sub(Vec3(b, b, b))
                    is Float -> Vec3(a).sub(Vec3(b, b, b))
                    is Double -> Vec3(a).sub(Vec3(b, b, b))
                    is Long -> Vec3(a).sub(Vec3(b, b, b))
                    is Vec2 -> Vec3(a).sub(Vec3(b.x, b.y, 0))
                    is Vec3 -> Vec3(a).sub(b)
                    is Vec4 -> Vec4(a.x, a.y, a.z, 1.0).sub(b)
                    is OverloadedMath -> b.__rsub__(a)
                    else -> throw NullPointerException("can't subtract `$a` and `$b`")
                }
                is Vec4 -> return when (b) {
                    is Int -> Vec4(a).sub(Vec4(b.toDouble(), 0.0, 0.0, 0.0))
                    is Float -> Vec4(a).sub(Vec4(b.toDouble(), 0.0, 0.0, 0.0))
                    is Double -> Vec4(a).sub(Vec4(b.toDouble(), 0.0, 0.0, 0.0))
                    is Long -> Vec4(a).sub(Vec4(b.toDouble(), 0.0, 0.0, 0.0))
                    is Vec2 -> Vec4(a).sub(Vec4(b.x, b.y, 0.0, 0.0))
                    is Vec3 -> Vec4(a).sub(Vec4(b.x, b.y, b.z, 0.0))
                    is Vec4 -> Vec4(a).sub(b)
                    is OverloadedMath -> b.__rsub__(a)
                    else -> throw NullPointerException("can't subtract `$a` and `$b`")
                }
                is OverloadedMath -> a.__sub__(b)
                else -> throw NullPointerException("can't subtract `$a` and `$b`")
            }
        }

        @JvmStatic
        fun __PLUS__(va: Any?, vb: Any?): Any? {

            var a = va
            var b = vb

            if (a is OverloadedMath.ReducedWhenOverloaded) a = a.get()
            if (b is OverloadedMath.ReducedWhenOverloaded) b = b.get()

            if (a is Number && b is Number) return a.toDouble() + b.toDouble()
            if (a is String || b is String) return a.toString() + b.toString()

            if (a == null) throw NullPointerException("can't add `$a` and `$b`")
            if (b == null) throw NullPointerException("can't add `$a` and `$b`")

            return when (a) {
                is Int -> return when (b) {
                    is Int -> a + b
                    is Float -> a + b
                    is Double -> a + b
                    is Long -> a + b
                    is Vec2 -> Vec2(a, a).add(b)
                    is Vec3 -> Vec3(a, a, a).add(b)
                    is Vec4 -> Vec4(a.toDouble(), a.toDouble(), a.toDouble(), 1.0).add(b)
                    is OverloadedMath -> b.__radd__(a)
                    else -> throw NullPointerException("can't add `$a` and `$b`")
                }
                is Float -> return when (b) {
                    is Int -> a + b
                    is Float -> a + b
                    is Double -> a + b
                    is Long -> a + b
                    is Vec2 -> Vec2(a, a).add(b)
                    is Vec3 -> Vec3(a, a, a).add(b)
                    is Vec4 -> Vec4(a.toDouble(), a.toDouble(), a.toDouble(), 1.0).add(b)
                    is OverloadedMath -> b.__radd__(a)
                    else -> throw NullPointerException("can't add `$a` and `$b`")
                }
                is Double -> return when (b) {
                    is Int -> a + b
                    is Float -> a + b
                    is Double -> a + b
                    is Long -> a + b
                    is Vec2 -> Vec2(a, a).add(b)
                    is Vec3 -> Vec3(a, a, a).add(b)
                    is Vec4 -> Vec4(a.toDouble(), a.toDouble(), a.toDouble(), 1.0).add(b)
                    is OverloadedMath -> b.__radd__(a)
                    else -> throw NullPointerException("can't add `$a` and `$b`")
                }
                is Long -> return when (b) {
                    is Int -> a + b
                    is Float -> a + b
                    is Double -> a + b
                    is Long -> a + b
                    is Vec2 -> Vec2(a, a).add(b)
                    is Vec3 -> Vec3(a, a, a).add(b)
                    is Vec4 -> Vec4(a.toDouble(), a.toDouble(), a.toDouble(), 1.0).add(b)
                    is OverloadedMath -> b.__radd__(a)
                    else -> throw NullPointerException("can't add `$a` and `$b`")
                }
                is Vec2 -> return when (b) {
                    is Int -> Vec2(a).add(Vec2(b, b))
                    is Float -> Vec2(a).add(Vec2(b, b))
                    is Double -> Vec2(a).add(Vec2(b, b))
                    is Long -> Vec2(a).add(Vec2(b, b))
                    is Vec2 -> Vec2(a).add(b)
                    is Vec3 -> a.toVec3().add(b)
                    is Vec4 -> Vec4(a.x, a.y, 0.0, 1.0).add(b)
                    is OverloadedMath -> b.__radd__(a)
                    else -> throw NullPointerException("can't add `$a` and `$b`")
                }
                is Vec3 -> return when (b) {
                    is Int -> Vec3(a).add(Vec3(b, b, b))
                    is Float -> Vec3(a).add(Vec3(b, b, b))
                    is Double -> Vec3(a).add(Vec3(b, b, b))
                    is Long -> Vec3(a).add(Vec3(b, b, b))
                    is Vec2 -> Vec3(a).add(Vec3(b.x, b.y, 0))
                    is Vec3 -> Vec3(a).add(b)
                    is Vec4 -> Vec4(a.x, a.y, a.z, 1.0).add(b)
                    is OverloadedMath -> b.__radd__(a)
                    else -> throw NullPointerException("can't add `$a` and `$b`")
                }
                is Vec4 -> return when (b) {
                    is Int -> Vec4(a).add(Vec4(b.toDouble(), 0.0, 0.0, 0.0))
                    is Float -> Vec4(a).add(Vec4(b.toDouble(), 0.0, 0.0, 0.0))
                    is Double -> Vec4(a).add(Vec4(b.toDouble(), 0.0, 0.0, 0.0))
                    is Long -> Vec4(a).add(Vec4(b.toDouble(), 0.0, 0.0, 0.0))
                    is Vec2 -> Vec4(a).add(Vec4(b.x, b.y, 0.0, 0.0))
                    is Vec3 -> Vec4(a).add(Vec4(b.x, b.y, b.z, 0.0))
                    is Vec4 -> Vec4(a).add(b)
                    is OverloadedMath -> b.__radd__(a)
                    else -> throw NullPointerException("can't add `$a` and `$b`")
                }
                is OverloadedMath -> a.__add__(b)
                else -> throw NullPointerException("can't add `$a` and `$b`")
            }
        }

        @JvmStatic
        fun __MULTIPLY__(va: Any?, vb: Any?): Any? {

            var a = va
            var b = vb

            if (a is OverloadedMath.ReducedWhenOverloaded) a = a.get()
            if (b is OverloadedMath.ReducedWhenOverloaded) b = b.get()

            if (a is Number && b is Number) return a.toDouble() * b.toDouble()

            if (a == null) throw NullPointerException("can't multiply `$a` and `$b`")
            if (b == null) throw NullPointerException("can't multiply `$a` and `$b`")

            return when (a) {
                is Int -> return when (b) {
                    is Int -> a * b
                    is Float -> a * b
                    is Double -> a * b
                    is Long -> a * b
                    is Vec2 -> Vec2(a, a).mul(b)
                    is Vec3 -> Vec3(a, a, a).mul(b)
                    is Vec4 -> Vec4(a.toDouble(), a.toDouble(), a.toDouble(), 1.0).mul(b)
                    is OverloadedMath -> b.__rmul__(a)
                    else -> throw NullPointerException("can't multiply `$a` and `$b`")
                }
                is Float -> return when (b) {
                    is Int -> a * b
                    is Float -> a * b
                    is Double -> a * b
                    is Long -> a * b
                    is Vec2 -> Vec2(a, a).mul(b)
                    is Vec3 -> Vec3(a, a, a).mul(b)
                    is Vec4 -> Vec4(a.toDouble(), a.toDouble(), a.toDouble(), 1.0).mul(b)
                    is OverloadedMath -> b.__rmul__(a)
                    else -> throw NullPointerException("can't multiply `$a` and `$b`")
                }
                is Double -> return when (b) {
                    is Int -> a * b
                    is Float -> a * b
                    is Double -> a * b
                    is Long -> a * b
                    is Vec2 -> Vec2(a, a).mul(b)
                    is Vec3 -> Vec3(a, a, a).mul(b)
                    is Vec4 -> Vec4(a.toDouble(), a.toDouble(), a.toDouble(), 1.0).mul(b)
                    is OverloadedMath -> b.__rmul__(a)
                    else -> throw NullPointerException("can't multiply `$a` and `$b`")
                }
                is Long -> return when (b) {
                    is Int -> a * b
                    is Float -> a * b
                    is Double -> a * b
                    is Long -> a * b
                    is Vec2 -> Vec2(a, a).mul(b)
                    is Vec3 -> Vec3(a, a, a).mul(b)
                    is Vec4 -> Vec4(a.toDouble(), a.toDouble(), a.toDouble(), 1.0).mul(b)
                    is OverloadedMath -> b.__rmul__(a)
                    else -> throw NullPointerException("can't multiply `$a` and `$b`")
                }
                is Vec2 -> return when (b) {
                    is Int -> Vec2(a).mul(Vec2(b, b))
                    is Float -> Vec2(a).mul(Vec2(b, b))
                    is Double -> Vec2(a).mul(Vec2(b, b))
                    is Long -> Vec2(a).mul(Vec2(b, b))
                    is Vec2 -> Vec2(a).mul(b)
                    is Vec3 -> a.toVec3().mul(b)
                    is Vec4 -> Vec4(a.x, a.y, 0.0, 1.0).mul(b)
                    is OverloadedMath -> b.__rmul__(a)
                    else -> throw NullPointerException("can't multiply `$a` and `$b`")
                }
                is Vec3 -> return when (b) {
                    is Int -> Vec3(a).mul(Vec3(b, b, b))
                    is Float -> Vec3(a).mul(Vec3(b, b, b))
                    is Double -> Vec3(a).mul(Vec3(b, b, b))
                    is Long -> Vec3(a).mul(Vec3(b, b, b))
                    is Vec2 -> Vec3(a).mul(Vec3(b.x, b.y, 0))
                    is Vec3 -> Vec3(a).mul(b)
                    is Vec4 -> Vec4(a.x, a.y, a.z, 1.0).mul(b)
                    is OverloadedMath -> b.__rmul__(a)
                    else -> throw NullPointerException("can't multiply `$a` and `$b`")
                }
                is Vec4 -> return when (b) {
                    is Int -> Vec4(a).mul(Vec4(b.toDouble(), 0.0, 0.0, 0.0))
                    is Float -> Vec4(a).mul(Vec4(b.toDouble(), 0.0, 0.0, 0.0))
                    is Double -> Vec4(a).mul(Vec4(b.toDouble(), 0.0, 0.0, 0.0))
                    is Long -> Vec4(a).mul(Vec4(b.toDouble(), 0.0, 0.0, 0.0))
                    is Vec2 -> Vec4(a).mul(Vec4(b.x, b.y, 0.0, 0.0))
                    is Vec3 -> Vec4(a).mul(Vec4(b.x, b.y, b.z, 0.0))
                    is Vec4 -> Vec4(a).mul(b)
                    is OverloadedMath -> b.__rmul__(a)
                    else -> throw NullPointerException("can't multiply `$a` and `$b`")
                }
                is OverloadedMath -> a.__mul__(b)
                else -> throw NullPointerException("can't multiply `$a` and `$b`")
            }
        }


        @JvmStatic
        fun __DIVIDE__(va: Any?, vb: Any?): Any? {

            var a = va
            var b = vb

            if (a is OverloadedMath.ReducedWhenOverloaded) a = a.get()
            if (b is OverloadedMath.ReducedWhenOverloaded) b = b.get()

            if (a is Number && b is Number) return a.toDouble() / b.toDouble()

            if (a == null) throw NullPointerException("can't divide `$a` and `$b`")
            if (b == null) throw NullPointerException("can't divide `$a` and `$b`")

            return when (a) {
                is Int -> return when (b) {
                    is Int -> a / b
                    is Float -> a / b
                    is Double -> a / b
                    is Long -> a / b
                    is Vec2 -> Vec2(a, a).div(b)
                    is Vec3 -> Vec3(a, a, a).div(b)
                    is Vec4 -> Vec4(a.toDouble(), a.toDouble(), a.toDouble(), 1.0).div(b)
                    is OverloadedMath -> b.__rdiv__(a)
                    else -> throw NullPointerException("can't divide `$a` and `$b`")
                }
                is Float -> return when (b) {
                    is Int -> a / b
                    is Float -> a / b
                    is Double -> a / b
                    is Long -> a / b
                    is Vec2 -> Vec2(a, a).div(b)
                    is Vec3 -> Vec3(a, a, a).div(b)
                    is Vec4 -> Vec4(a.toDouble(), a.toDouble(), a.toDouble(), 1.0).div(b)
                    is OverloadedMath -> b.__rdiv__(a)
                    else -> throw NullPointerException("can't divide `$a` and `$b`")
                }
                is Double -> return when (b) {
                    is Int -> a / b
                    is Float -> a / b
                    is Double -> a / b
                    is Long -> a / b
                    is Vec2 -> Vec2(a, a).div(b)
                    is Vec3 -> Vec3(a, a, a).div(b)
                    is Vec4 -> Vec4(a.toDouble(), a.toDouble(), a.toDouble(), 1.0).div(b)
                    is OverloadedMath -> b.__rdiv__(a)
                    else -> throw NullPointerException("can't divide `$a` and `$b`")
                }
                is Long -> return when (b) {
                    is Int -> a / b
                    is Float -> a / b
                    is Double -> a / b
                    is Long -> a / b
                    is Vec2 -> Vec2(a, a).div(b)
                    is Vec3 -> Vec3(a, a, a).div(b)
                    is Vec4 -> Vec4(a.toDouble(), a.toDouble(), a.toDouble(), 1.0).div(b)
                    is OverloadedMath -> b.__rdiv__(a)
                    else -> throw NullPointerException("can't divide `$a` and `$b`")
                }
                is Vec2 -> return when (b) {
                    is Int -> Vec2(a).div(Vec2(b, b))
                    is Float -> Vec2(a).div(Vec2(b, b))
                    is Double -> Vec2(a).div(Vec2(b, b))
                    is Long -> Vec2(a).div(Vec2(b, b))
                    is Vec2 -> Vec2(a).div(b)
                    is Vec3 -> a.toVec3().div(b)
                    is Vec4 -> Vec4(a.x, a.y, 0.0, 1.0).div(b)
                    is OverloadedMath -> b.__rdiv__(a)
                    else -> throw NullPointerException("can't divide `$a` and `$b`")
                }
                is Vec3 -> return when (b) {
                    is Int -> Vec3(a).div(Vec3(b, b, b))
                    is Float -> Vec3(a).div(Vec3(b, b, b))
                    is Double -> Vec3(a).div(Vec3(b, b, b))
                    is Long -> Vec3(a).div(Vec3(b, b, b))
                    is Vec2 -> Vec3(a).div(Vec3(b.x, b.y, 0))
                    is Vec3 -> Vec3(a).div(b)
                    is Vec4 -> Vec4(a.x, a.y, a.z, 1.0).div(b)
                    is OverloadedMath -> b.__rdiv__(a)
                    else -> throw NullPointerException("can't divide `$a` and `$b`")
                }
                is Vec4 -> return when (b) {
                    is Int -> Vec4(a).div(Vec4(b.toDouble(), 0.0, 0.0, 0.0))
                    is Float -> Vec4(a).div(Vec4(b.toDouble(), 0.0, 0.0, 0.0))
                    is Double -> Vec4(a).div(Vec4(b.toDouble(), 0.0, 0.0, 0.0))
                    is Long -> Vec4(a).div(Vec4(b.toDouble(), 0.0, 0.0, 0.0))
                    is Vec2 -> Vec4(a).div(Vec4(b.x, b.y, 0.0, 0.0))
                    is Vec3 -> Vec4(a).div(Vec4(b.x, b.y, b.z, 0.0))
                    is Vec4 -> Vec4(a).div(b)
                    is OverloadedMath -> b.__rdiv__(a)
                    else -> throw NullPointerException("can't divide `$a` and `$b`")
                }
                is OverloadedMath -> a.__div__(b)
                else -> throw NullPointerException("can't divide `$a` and `$b`")
            }
        }


        @JvmStatic
        fun __XOR__(va: Any?, vb: Any?): Any? {

            var a = va
            var b = vb

            if (a is OverloadedMath.ReducedWhenOverloaded) a = a.get()
            if (b is OverloadedMath.ReducedWhenOverloaded) b = b.get()

            if (a is Number && b is Number) return a.toDouble() * b.toDouble()

            if (a == null) throw NullPointerException("can't xor `$a` and `$b`")
            if (b == null) throw NullPointerException("can't xor `$a` and `$b`")

            return when (a) {
                is Int -> return when (b) {
                    is Int -> Math.pow(a.toDouble(), b.toDouble())
                    is Float -> Math.pow(a.toDouble(), b.toDouble())
                    is Double -> Math.pow(a.toDouble(), b.toDouble())
                    is Long -> Math.pow(a.toDouble(), b.toDouble())
                    is OverloadedMath -> b.__rxor__(a)
                    else -> throw NullPointerException("can't xor `$a` and `$b`")
                }
                is Float -> return when (b) {
                    is Int -> Math.pow(a.toDouble(), b.toDouble())
                    is Float -> Math.pow(a.toDouble(), b.toDouble())
                    is Double -> Math.pow(a.toDouble(), b.toDouble())
                    is Long -> Math.pow(a.toDouble(), b.toDouble())
                    is OverloadedMath -> b.__rxor__(a)
                    else -> throw NullPointerException("can't xor `$a` and `$b`")
                }
                is Double -> return when (b) {
                    is Int -> Math.pow(a.toDouble(), b.toDouble())
                    is Float -> Math.pow(a.toDouble(), b.toDouble())
                    is Double -> Math.pow(a.toDouble(), b.toDouble())
                    is Long -> Math.pow(a.toDouble(), b.toDouble())
                    is OverloadedMath -> b.__rxor__(a)
                    else -> throw NullPointerException("can't xor `$a` and `$b`")
                }
                is Long -> return when (b) {
                    is Int -> Math.pow(a.toDouble(), b.toDouble())
                    is Float -> Math.pow(a.toDouble(), b.toDouble())
                    is Double -> Math.pow(a.toDouble(), b.toDouble())
                    is Long -> Math.pow(a.toDouble(), b.toDouble())
                    is OverloadedMath -> b.__rxor__(a)
                    else -> throw NullPointerException("can't xor `$a` and `$b`")
                }
                is Vec2 -> return when (b) {
                    is Vec2 -> a.toVec3().cross(b.toVec3())
                    is Vec3 -> a.toVec3().cross(Vec3(b))
                    is OverloadedMath -> b.__rxor__(a)
                    else -> throw NullPointerException("can't xor `$a` and `$b`")
                }
                is Vec3 -> return when (b) {
                    is Vec2 -> a.cross(b.toVec3())
                    is Vec3 -> Vec3(a).cross(b)
                    is OverloadedMath -> b.__rxor__(a)
                    else -> throw NullPointerException("can't xor `$a` and `$b`")
                }
                is Vec4 -> return when (b) {
                    is OverloadedMath -> b.__rmul__(a)
                    else -> throw NullPointerException("can't xor `$a` and `$b`")
                }
                is OverloadedMath -> a.__xor__(b)
                else -> throw NullPointerException("can't xor `$a` and `$b`")
            }
        }
    }

    val accessorMap: Map<Tree.Kind, List<Method>> = Tree.Kind.values().filter { it.asInterface() != null }.map {
        val access = it.asInterface().declaredMethods.filter {
            (it.parameterCount == 0 && Tree::class.java.isAssignableFrom(it.returnType)) || (it.parameterCount == 0 && List::class.java.isAssignableFrom(it.returnType))
        }.toMutableList()

        if (it == Tree.Kind.FOR_IN_LOOP) {
            // also do ForOfLoop
            access.addAll(ForOfLoopTree::class.java.declaredMethods.filter {
                (it.parameterCount == 0 && Tree::class.java.isAssignableFrom(it.returnType)) || (it.parameterCount == 0 && List::class.java.isAssignableFrom(it.returnType))
            }.toList())
        }

        it to access
    }.toMap()


    val trapKinds = setOf(Tree.Kind.PLUS, Tree.Kind.MINUS, Tree.Kind.DIVIDE, Tree.Kind.MULTIPLY, Tree.Kind.XOR)

    fun parse(v: String) {
        val p = Parser.create("--language=es6")
        val r = p.parse("<<internal>>", v, {
            print("diagnostic: $it")
        })

        r.sourceElements.forEach {
            //            print("\n\n sourceElement: $it\n\n")
            recurOver(it, 0, v)
        }

    }

    fun transformer(box: Box): SourceTransformer {

        val r = object : SourceTransformer {
            override fun transform(c: String, fragment: Boolean): field.utility.Pair<String, Function<Int, Int>> {

                if (disabled || c.contains("%%NO_OVERLOADS%%")) return field.utility.Pair(c, Function { x: Int -> x })

                options.numbers = box.properties.isTrue(OverloadedMath.withLiveNumbers, false)
                options.overloads = box.properties.isTrue(OverloadedMath.withOverloading, true)
                options.functionRewrite = box.properties.isTrue(OverloadedMath.withFunctionRewriting, false)

                val v = parseAndReconstruct(c, {
                    if (it.kind.ordinal < Diagnostic.Kind.WARNING.ordinal) {
                        throw SourceTransformer.TranslationFailedException(it.message + " on line " + it.lineNumber + " at col " + it.columnNumber)
                    }
                    print("Diagnostic: $it")
                }, box)

                if (!fragment && options.on()) {
                    currentContents = c
                    baseContents = c
                    currentMapping = mutableMapOf()
                }

                return field.utility.Pair(v, Function { x: Int -> x })
            }

            override fun incrementalUpdate(now: String?) {

                if (debug) println(" incrementalUpdate! ")
                if (!box.properties.isTrue(OverloadedMath.withLiveNumbers, false)) return;

                if (baseContents != null && now != null) {
                    currentMapping = MappingGenerator().gum(baseContents!!, now)
                    currentContents = now
                }

            }
        }

        return r
    }

    var hash = 0L
    @JvmOverloads
    fun parseAndReconstruct(v: String, d: (Diagnostic) -> Unit = { print("diagnostic: $it") }, box: Box): String {

        if (!options.on()) return v

        val (trimmed, suffix) = trimSrcURL(v)


        val p = Parser.create("--language=es6")
        val r = p.parse("<<internal>>", trimmed + "\n", d)

        var actions = mutableListOf<() -> Unit>()

        var replaced = trimmed

        var forbiddenRanges = mutableListOf<Pair<Long, Long>>();

        r.sourceElements.forEach {
            findForbiddenRanges(it, forbiddenRanges);
        }


        hash = 0L

        r.sourceElements.forEach {
            print("\n\n sourceElement: $it\n\n")

            var (s, e) = startAndEndPositionFor(it)

            actions.add { replaced = replaced.replaceRange(s, e, reconstructOver(it, 0, v, forbiddenRanges, parent = null, box = box, hash = 0L)) }
        }

        actions.reversed().forEach { it() }

        return replaced + "\n" + suffix
    }

    private fun findForbiddenRanges(it: Tree, forbiddenRanges: MutableList<Pair<Long, Long>>) {

        if (it is ForLoopTree) {
            val a = it.startPosition
            val b = it.statement.startPosition
            forbiddenRanges.add(a to b)
        }

        childrenFor(it).forEach { findForbiddenRanges(it, forbiddenRanges) }

    }

    private fun recurOver(tree: Tree, i: Int, v: String) {
        if (debug) println("${indent(i)}" + tree.kind)
        val children = childrenFor(tree)

        var (start, end) = startAndEndPositionFor(tree)

        var text = v.substring(start, end)

//        if (debug) println(" node covers ${start} -> ${end} = $text")

        var n = children.size - 1
        for (c in children.asReversed()) {

//            if (debug) println(" replace range ${(c.startPosition - start).toInt()..(c.endPosition - start - 1).toInt() - 1}  : '" + text + "'")

            val (s, e) = startAndEndPositionFor(c)

            text = text.replaceRange((c.startPosition - start).toInt()..(c.endPosition - start - 1).toInt(), "<<$n>>")
            n--
        }

        if (tree is BinaryTree) {
            if (debug) println(" ** binary operator :" + tree)
        }

        if (debug) println("${indent(i)}'" + text + "'")

        children.forEach { recurOver(it!!, i + 2, v) }
    }

    private fun reconstructOver(tree: Tree, i: Int, v: String, forbiddenRanges: List<Pair<Long, Long>>, parent: Tree? = null, box: Box, hash: Long): String {

        if (debug) println("${indent(i)}" + tree.kind + " ##" + hash)
        val children = childrenFor(tree)

        var (start, end) = startAndEndPositionFor(tree, true)

        var text = v.substring(start, end)


//        if (debug) println("${indent(i)} node covers ${start} -> ${end} = $text")

        if (options.overloads && tree is BinaryTree && tree.kind in trapKinds && !forbidden(tree.startPosition, tree.endPosition, forbiddenRanges)) {
//            if (debug) println(" ** binary operator :" + tree)

//            if (!(text.contains("'") || text.contains("\"") || text.contains("`")))
            if (true) {

                // is this inside the initializer of a for loop? if so, replacing anything here will wreck everyone's offsets

                val left = children.get(0)
                val right = children.get(1)

                val (sr, er) = startAndEndPositionFor(right)
                var rightR = reconstructOver(right, i + 5, v, forbiddenRanges, parent, box, hash = 31 * hash + hashFor(right, "right".hashCode().toLong()))//text.replaceRange((sr - start).toInt()..(er - start - 1).toInt(), )


                val (sc, ec) = startAndEndPositionFor(left)
                var leftR = reconstructOver(left, i + 5, v, forbiddenRanges, parent, box, hash = 31 * hash + hashFor(left, "left".hashCode().toLong()))//rightR.replaceRange((sc - start).toInt()..(ec - start - 1).toInt())

                if (debug) println(" -> leftR : $leftR")

                val middle = v.substring(ec, sr)

                // search for missing brackets --- Some parens are part of no element apparently

                var closeBracket = count(middle, ')');
                val openBracket = count(middle, '(');

//                println(" middle text is `$middle` $closeBracket, $openBracket")
//                println("left `$leftR`")
//                println("right `$rightR`")

                if (rightR.trim().endsWith("//"))
                    rightR = rightR.trim().substring(0, rightR.trim().length - 2)

                if (leftR.startsWith("(") && count(leftR, '(') == count(leftR, ')') + 1 && closeBracket > 0) {
                    leftR = leftR.substring(1)
                    closeBracket--
                }
                if (leftR.endsWith(")") && count(leftR, ')') == count(leftR, '(') + 1 && closeBracket == 0 && openBracket == 0) {
                    leftR = leftR.substring(0, leftR.length - 1)
                    closeBracket++
                }

                return duplicate(openBracket, "(") + "__" + tree.kind + "__(" + leftR + "," + rightR + ")" + duplicate(closeBracket, ")")
            }
        }

        if (options.numbers && tree is LiteralTree && tree.kind.equals(Tree.Kind.NUMBER_LITERAL)) {
            return "__NUMBER_LITERAL__(" + start + ", " + end + ", " + text + ")"
        }

        if (options.functionRewrite && tree is IdentifierTree && tree.kind.equals(Tree.Kind.IDENTIFIER) && parent != null && parent is FunctionCallTree) {
            // lookup
            if (debug) System.out.println(" functional name '" + tree.name + "' " + tree)
            val newName = rewriteFunctionName(tree.name, start, end, box, hash)
            if (newName != null) {
                return newName
            }
        }

        var n = children.size - 1
        for (c in children.asReversed()) {

            val (s, e) = startAndEndPositionFor(c)

            if (debug) println("${indent(i + 2)} replace range ${(s - start).toInt()..(e - start - 1).toInt()}  : '" + text.substring((s - start).toInt()..(e - start - 1).toInt()) + "'")

//            text = text.replaceRange((c.startPosition - start).toInt()..(c.endPosition - start - 1).toInt(), reconstructOver(c, i + 5, v))
            text = text.replaceRange((s - start).toInt()..(e - start - 1).toInt(), reconstructOver(c, i + 5, v, forbiddenRanges, parent = tree, box = box, hash = hash * 31 + hashFor(c, n.toLong())))
            n--
        }

        if (debug) println("${indent(i)} becomes = '" + text + "'")

//        children.forEach { recurOver(it!!, i + 2, v) }
        return text
    }

    private fun hashFor(c: Tree, salt: Long = 0): Long {
        System.out.println("HASH? " + c.kind.hashCode())
        return c.kind.hashCode().toLong() * 31 + salt
    }

    private fun rewriteFunctionName(name: String, start: Int, end: Int, box: Box, hash: Long): String? {
        return (box up OverloadedMath.functionRewriteTrap)?.apply(box, name, hash)
    }

    private fun forbidden(startPosition: Long, endPosition: Long, f: List<Pair<Long, Long>>): Boolean {
        f.forEach {
            if (startPosition < it.second && endPosition > it.first) return true
        }
        return false
    }

    private fun count(middle: String, s: Char): Int {
        var x = 0;
        for (a in middle) {
            if (a == s) x++
        }
        return x;
    }

    private fun duplicate(num: Int, s: String): String {
        var r = ""
        for (i in 0 until num)
            r += s
        return r
    }

    private fun childrenFor(tree: Tree): List<Tree> {
//        if (debug) println(" children for :" + tree + " " + tree.javaClass + " " + tree.kind)
        return accessorMap[tree.kind]!!.flatMap {
            //            if (debug) println("-> $it ($tree)")
            try {
                try {
                    val rr = it.invoke(tree)
                    when (rr) {
                        null -> {
                            if (debug) println("warning: $it returned null on ${tree.kind} / $tree")
                            Collections.EMPTY_LIST
                        }
                        is Tree -> Collections.singleton(rr)
                        is List<*> -> rr
                        else -> {
                            if (debug) println("warning: $it returned $rr (unknown) on ${tree.kind} / $tree")
                            Collections.EMPTY_LIST
                        }
                    }
                } catch (ee: ClassCastException) {
                    // this happens when we call forloop stuff on a forofloop
//                    ee.printStackTrace()
                    Collections.EMPTY_LIST
                }
            } catch (e: java.lang.IllegalArgumentException) {
//                e.printStackTrace()
                Collections.EMPTY_LIST
            }
        }.filter { it != null }.map { it as Tree }.sortedBy { it!!.startPosition }.toList()
    }

    private fun startAndEndPositionFor(c: Tree, correct: Boolean = true): Pair<Int, Int> {
        var start = c.startPosition.toInt()
        var end = c.endPosition.toInt()

        println("start/end in $c = $start/$end")
        // Nashorn appears to just lie about the start and end position of some parts of the tree.

        if (c.kind == Tree.Kind.STRING_LITERAL) {
            return start - 1 to end + 1
        }

        if (/*start == end &&*/ correct) {
            val cc = childrenFor(c)
            if (cc.size == 0) return start to end

            val ccSE = cc.map { startAndEndPositionFor(it) }.toList()
            val min = ccSE.minBy { it.first }!!.first
            val max = ccSE.maxBy { it.second }!!.second
            val r = Math.min(min, start) to Math.max(max, end)

//            println("start/end out $c = $r")
            return r;
        }
        return start to end
    }

    private fun indent(i: Int): String {
        var q = ""
        while (q.length < i) q = q + " "
        return q
    }

    fun options(): Options {
        return options
    }

    // Field source urls look like:
    //  //# sourceURL=bx[Untitled]/_d5b9fda0_5d2b_40a3_96c8_7926a9fb56cb

    // sourceURL=bx[viewport]/viewport/_f46e4d28_e0e1_453b_b698_ae4352804e51
    var srcURL = Regex("(\\/\\/# sourceURL=bx\\[.*\\]\\/_........_...._...._...._............)")

    fun trimSrcURL(src: String): Pair<String, String> {
        val res = srcURL.find(src)
        if (res != null) {
            return srcURL.replace(src, "") to res.groups[0]!!.value
        }
        return src to ""
    }

}