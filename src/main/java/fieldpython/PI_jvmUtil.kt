package fieldpython

import field.app.ThreadSync2
import field.linalg.Vec4
import field.utility.Dict
import field.utility.Pair
import field.utility.Quad
import field.utility.Triple
import fieldbox.boxes.Box
import fieldbox.boxes.Boxes
import fieldbox.boxes.Drawing
import fieldbox.boxes.plugins.BoxDefaultCode
import fieldbox.boxes.plugins.IsExecuting
import fieldbox.execution.Completion
import fieldbox.execution.Execution
import fieldbox.execution.JavaSupport
import fieldbox.io.IO
import fielded.RemoteEditor
import fielded.plugins.Out
import fieldkotlin.KI
import fieldkotlin.frame
import jep.DirectNDArray
import jep.JepConfig
import jep.JepException
import jep.SharedInterpreter
import jep.python.PyCallable
import java.io.IOException
import java.io.Writer
import java.lang.IllegalArgumentException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.*
import java.util.function.BiConsumer
import java.util.function.Consumer
import java.util.function.Supplier
import java.util.regex.Pattern
import java.util.stream.Collectors

object PI_jvmUtil {

    fun product(d: IntArray) = d.fold(1) { a, b -> a * b }

    fun directArray(dim: IntArray, dtype: String = "f") = when (dtype) {
        "f" -> DirectNDArray(
            ByteBuffer.allocateDirect(product(dim) * 4).order(ByteOrder.nativeOrder()).asFloatBuffer(),
            *dim
        )

        "f32" -> DirectNDArray(
            ByteBuffer.allocateDirect(product(dim) * 4).order(ByteOrder.nativeOrder()).asFloatBuffer(), *dim
        )

        "d" -> DirectNDArray(
            ByteBuffer.allocateDirect(product(dim) * 8).order(ByteOrder.nativeOrder()).asDoubleBuffer(), *dim
        )

        "f64" -> DirectNDArray(
            ByteBuffer.allocateDirect(product(dim) * 8).order(ByteOrder.nativeOrder()).asDoubleBuffer(), *dim
        )

        "i" -> DirectNDArray(
            ByteBuffer.allocateDirect(product(dim) * 4).order(ByteOrder.nativeOrder()).asIntBuffer(),
            *dim
        )

        "i32" -> DirectNDArray(
            ByteBuffer.allocateDirect(product(dim) * 4).order(ByteOrder.nativeOrder()).asIntBuffer(),
            *dim
        )

        "l" -> DirectNDArray(
            ByteBuffer.allocateDirect(product(dim) * 4).order(ByteOrder.nativeOrder()).asLongBuffer(),
            *dim
        )

        "i64" -> DirectNDArray(
            ByteBuffer.allocateDirect(product(dim) * 4).order(ByteOrder.nativeOrder()).asLongBuffer(), *dim
        )

        else -> throw IllegalArgumentException(" unknown dtype '$dtype'")
    }
}