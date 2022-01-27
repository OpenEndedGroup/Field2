package auw

import auw.functional.FFunction
import field.utility.Dict
import field.utility.OverloadedMath
import fieldlinker.AsMap_slots
import fieldlinker.Linker
import java.lang.ClassCastException
import java.lang.reflect.Modifier
import java.nio.FloatBuffer
import java.util.LinkedHashSet


@Suppress("NAME_SHADOWING")
class CBuffer(@JvmField val a: FBuffer, val a2: FBuffer) : AsMap_slots,
    OverloadedMath {

    val info = Dict()


    override fun setSlot(x: Int, v: Double): Object? {
        return a.setSlot(x, v)
    }

    override fun setSlot(x: Int, v: Int): Object? {
        return a.setSlot(x, v)
    }

    @JvmField
    val length = a.length

    override fun getSlot(x: Int): Double = a.getSlot(x)

    override fun __radd__(b: Any?): Any {
        return binary(
            b,
            { a, b, c -> BufferTools.add(a, b, c) },
            { a, b, c -> BufferTools.add(a, b, c) })
    }

    override fun __rsub__(b: Any?): Any {
        return binary(
            b,
            { a, b, c -> BufferTools.sub(b, a, c) },
            { a, b, c -> BufferTools.sub(b, a, c) })
    }

    override fun __rxor__(b: Any?): Any {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun __add__(b: Any?): Any {
        return binary(
            b,
            { a, b, c -> BufferTools.add(a, b, c) },
            { a, b, c -> BufferTools.add(a, b, c) },
            { b, thiz -> b.__radd__(thiz) })
    }

    override fun __div__(b: Any?): Any {
        return binary(
            b,
            { a, b, c -> BufferTools.div(a, b, c) },
            { a, b, c -> BufferTools.div(a, b, c) },
            { b, thiz -> b.__rdiv__(thiz) })
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
            { b, thiz -> b.__rsub__(thiz) })
    }

    override fun __mul__(b: Any?): Any {
        return binaryMul(
            b,
            { a, b, c -> BufferTools.mul(a, b, c) },
            { a, b, c -> BufferTools.mul(a, b, c) },
            { b, thiz -> b.__rmul__(thiz) })
    }

    override fun __rdiv__(b: Any?): Any {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun binary(
        b: Any?,
        perform: (FBuffer, FBuffer, FBuffer) -> Unit,
        performScalar: (FBuffer, Float, FBuffer) -> Unit,
        opposite: ((OverloadedMath, CBuffer) -> Any)? = null
    ): Any {

        if (b is FBuffer) {
            val outA = BoxTools.stack2.get().allocate()
            val outB = BoxTools.stack2.get().allocate()
            perform(this.a, b, outA)
            return CBuffer(outA, outB)
        }

        // this isn't actually a complex multiplication
        if (b is CBuffer) {
            val outA = BoxTools.stack2.get().allocate()
            val outB = BoxTools.stack2.get().allocate()
            perform(this.a, b.a, outA)
            perform(this.a2, b.a2, outB)
            return CBuffer(outA, outB)
        }
        if (b is Number) {
            val outA = BoxTools.stack2.get().allocate()
            val outB = BoxTools.stack2.get().allocate()
            performScalar(this.a, b.toFloat(), outA)
            return CBuffer(outA, outB)
        }

        if (b == null)
            throw ClassCastException(" can't add a buffer to nothing ")

        if (opposite != null)
            if (b is OverloadedMath)
                return opposite(b, this)

        throw ClassCastException(" can't add a buffer to ${b.javaClass}")

    }


    private fun binaryMul(
        b: Any?,
        perform: (FBuffer, FBuffer, FBuffer) -> Unit,
        performScalar: (FBuffer, Float, FBuffer) -> Unit,
        opposite: ((OverloadedMath, CBuffer) -> Any)? = null
    ): Any {

        if (b is FBuffer) {
            val outA = BoxTools.stack2.get().allocate()
            val outB = BoxTools.stack2.get().allocate()
            perform(this.a, b, outA)
            perform(this.a, b, outB)
            return CBuffer(outA, outB)
        }

        // this isn't actually a complex multiplication
        if (b is CBuffer) {
            val outA = BoxTools.stack2.get().allocate()
            val outB = BoxTools.stack2.get().allocate()

            performComplexMul(this.a, this.a2, b.a, b.a2, outA, outB)

            return CBuffer(outA, outB)
        }
        if (b is Number) {
            val outA = BoxTools.stack2.get().allocate()
            val outB = BoxTools.stack2.get().allocate()
            performScalar(this.a, b.toFloat(), outA)
            performScalar(this.a, b.toFloat(), outB)
            return CBuffer(outA, outB)
        }

        if (b == null)
            throw ClassCastException(" can't add a buffer to nothing ")

        if (opposite != null)
            if (b is OverloadedMath)
                return opposite(b, this)

        throw ClassCastException(" can't add a buffer to ${b.javaClass}")

    }

    private fun performComplexMul(r0: FBuffer, i0: FBuffer, r1: FBuffer, i1: FBuffer, r: FBuffer, i: FBuffer) {

        for (x in 0 until r0.length) {
            r.a[x] = r0.a[x] * r1.a[x] - i0.a[x] * i1.a[x]
            i.a[x] = r0.a[x] * i1.a[x] + i0.a[x] * r1.a[x]
        }


    }

    val properties = mutableMapOf<String, Any?>()
    var previous: CBuffer? = null


}