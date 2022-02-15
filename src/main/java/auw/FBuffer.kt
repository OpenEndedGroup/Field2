package auw

import auw.functional.FFunction
import field.utility.Dict
import field.utility.OverloadedMath
import fieldlinker.AsMap_slots
import fieldlinker.Linker
import org.openjdk.nashorn.api.scripting.ScriptObjectMirror
import java.lang.ClassCastException
import java.lang.reflect.Modifier
import java.nio.FloatBuffer
import java.util.LinkedHashSet


@Suppress("NAME_SHADOWING")
class FBuffer(@JvmField val a: FloatBuffer, @JvmField val allocator: BoxTools.StackAllocator) : AsMap_slots,
    OverloadedMath, _FBuffer {

    override fun source(): Any {
        return this
    }

    val info = Dict()

    override fun get(): FBuffer {
        return this
    }

    override fun setSlot(x: Int, v: Double): Object? {
        a.put(x, v.toFloat())
        return null
    }

    override fun setSlot(x: Int, v: Int): Object? {
        a.put(x, v.toFloat())
        return null
    }

    @JvmField
    val length = a.limit()

    override fun getSlot(x: Int): Double = a.get(x).toDouble()

    override fun __radd__(b: Any?): Any {
        return binary(
            b,
            { a, b, c -> BufferTools.add(a, b, c) },
            { a, b, c -> BufferTools.add(a, b, c) }, name = "add"
        )
    }

    override fun __rsub__(b: Any?): Any {
        return binary(
            b,
            { a, b, c -> BufferTools.sub(b, a, c) },
            { a, b, c -> BufferTools.sub(b, a, c) }, name = "sub"
        )
    }

    override fun __rxor__(b: Any?): Any {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun __add__(b: Any?): Any {
        return binary(
            b,
            { a, b, c -> BufferTools.add(a, b, c) },
            { a, b, c -> BufferTools.add(a, b, c) },
            { b, thiz -> b.__radd__(thiz) }, "add"
        )
    }

    override fun __div__(b: Any?): Any {
        return binary(
            b,
            { a, b, c -> BufferTools.div(a, b, c) },
            { a, b, c -> BufferTools.div(a, b, c) },
            { b, thiz -> b.__rdiv__(thiz) }, "divide"
        )
    }

    override fun __rmul__(b: Any?): Any {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun __xor__(b: Any?): Any {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun __sub__(b: Any?): Any {
        return binary(
            b,
            { a, b, c -> BufferTools.sub(a, b, c) },
            { a, b, c -> BufferTools.sub(a, b, c) },
            { b, thiz -> b.__rsub__(thiz) }, "subtract"
        )
    }

    override fun __mul__(b: Any?): Any {
        return binary(
            b,
            { a, b, c -> BufferTools.mul(a, b, c) },
            { a, b, c -> BufferTools.mul(a, b, c) },
            { b, thiz -> b.__rmul__(thiz) }, "multiply"
        )
    }

    override fun __rdiv__(b: Any?): Any {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun binary(
        b: Any?,
        perform: (FBuffer, FBuffer, FBuffer) -> Unit,
        performScalar: (FBuffer, Float, FBuffer) -> Unit,
        opposite: ((OverloadedMath, FBuffer) -> Any)? = null, name: String = "add"
    ): Any {

        if (b is FBuffer) {
            val out = BoxTools.stack.get().allocate()
            perform(this, b, out)
            return out
        }
        if (b is Number) {
            val out = BoxTools.stack.get().allocate()
            performScalar(this, b.toFloat(), out)
            return out
        }

        if (b == null)
            throw ClassCastException(" can't ${name} a buffer to nothing ")

        if (ScriptObjectMirror.isUndefined(b))
            throw ClassCastException(" can't ${name} a buffer to something Undefined")

        if (opposite != null)
            if (b is OverloadedMath)
                return opposite(b, this)

        throw ClassCastException(" can't ${name} a buffer to ${b.javaClass}")

    }


    val properties = mutableMapOf<String, Any?>()
    var previous: FBuffer? = null

    fun silence(): FBuffer {
        val q = allocator.allocate()
        BufferTools.zero(q)
        return q
    }

    fun copyFrom(o: FBuffer) {

        o.a.clear()
        a.clear()
        a.put(o.a)
        o.a.clear()
        a.clear()

    }

    fun copyFromShaped(o: FBuffer) {
        o.a.clear()
        a.clear()
        val D = 64
        for (i in 0 until o.length) {
            if (i < D) {
                val alpha = i / D.toFloat()
                a[i] = o.a[i] * (3 * alpha * alpha - 2 * alpha * alpha * alpha)
            } else if (i >= o.length - D) {
                val alpha = (o.length - i) / D.toFloat()
                a[i] = o.a[i] * (3 * alpha * alpha - 2 * alpha * alpha * alpha)
            } else {
                a[i] = o.a[i]
            }
        }
        o.a.clear()
        a.clear()

    }


}