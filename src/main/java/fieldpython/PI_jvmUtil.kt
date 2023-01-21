package fieldpython

import jep.DirectNDArray
import java.nio.ByteBuffer
import java.nio.ByteOrder

object PI_jvmUtil {

    val tunnels = mutableMapOf<String, DirectNDArray<*>>()

    @JvmStatic
    @JvmOverloads
    fun tunnel(n: String, dim: IntArray, dtype: String = "f"): DirectNDArray<*> {
        var q = tunnels.computeIfAbsent(n) {
            directArray(dim, dtype)
        }

        if (q.data.capacity() != product(dim)) {
            tunnels.remove(n)
            q = tunnels.computeIfAbsent(n) {
                directArray(dim, dtype)
            }
        }

        return q
    }

    fun product(d: IntArray) = d.fold(1) { a, b -> a * b }

    fun directArray(dim: IntArray, dtype: String = "f") = when (dtype) {
        "f", "float32", "f32" -> DirectNDArray(
            ByteBuffer.allocateDirect(size(dim, dtype)).order(ByteOrder.nativeOrder()).asFloatBuffer(),
            *dim
        )

        "d", "f64", "float64" -> DirectNDArray(
            ByteBuffer.allocateDirect(size(dim, dtype)).order(ByteOrder.nativeOrder()).asDoubleBuffer(), *dim
        )

        "uint8", "int8", "i8" -> DirectNDArray(
            ByteBuffer.allocateDirect(size(dim, dtype)).order(ByteOrder.nativeOrder()), *dim
        )

        "i", "i32", "int", "int32" -> DirectNDArray(
            ByteBuffer.allocateDirect(size(dim, dtype)).order(ByteOrder.nativeOrder()).asIntBuffer(),
            *dim
        )

        "l", "long", "i64", "int64" -> DirectNDArray(
            ByteBuffer.allocateDirect(size(dim, dtype)).order(ByteOrder.nativeOrder()).asLongBuffer(),
            *dim
        )


        else -> throw IllegalArgumentException(" unknown dtype '$dtype'")
    }

    fun size(dim: IntArray, dtype: String) = when (dtype) {
        "f", "float32", "f32", "i", "i32", "int", "int32" -> product(dim) * 4
        "l", "long", "i64", "int64", "d", "f64", "float64" -> product(dim) * 8
        "uint8", "int8", "i8" -> product(dim) * 1
        else -> throw IllegalArgumentException(" unknown dtype '$dtype'")
    }
}