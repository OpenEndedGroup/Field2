package auw.signal

import auw.*
import kotlin.math.sign

class Resample {

    var previousInput: FloatArray = FloatArray(BoxTools.size + 3)
    var previousIndex = 0.0
    var first = true

    var speed = FInterpolator()

    fun apply(input: _FBuffer, speed: Double): _FBuffer {
        return FBufferSource(this) {
            val output = BoxTools.stack.get().allocate()

            var speed = this.speed.next(Math.max(0.0, speed))

            if (first) {
                first = false

                DynamicScope.push("input").use {
                    val data = input()
                    previousIndex = flipInto(data, previousInput).toDouble()
                    previousInput[0] = previousInput[3]
                    previousInput[1] = previousInput[3]
                    previousInput[2] = previousInput[3]
                }
            }

            for (q in 0 until output.length) {

                val x0 = (previousIndex - 2)
                val xi0 = x0.toInt()
                val y0 = previousInput[xi0]
                val x1 = (previousIndex - 1)
                val xi1 = x1.toInt()
                val y1 = previousInput[xi1]
                val x2 = (previousIndex)
                val xi2 = x2.toInt()
                val y2 = previousInput[xi2]
                val x3 = (previousIndex + 1)
                val xi3 = x3.toInt()
                val y3 = previousInput[xi3]

                val alpha = (x1 - xi1).toFloat()
                println("$previousIndex")
                output.a.put(q, interpolate(y0, y1, y2, y3, alpha))

                previousIndex += speed.apply(q/output.length.toDouble())

                while (previousIndex >= previousInput.size- 1) {
                    DynamicScope.push("input").use {
                        val data = input()
                        println("-- flip --")
                        previousIndex = flipInto(data, previousInput) + (previousIndex - (previousInput.size - 1))
                        println(" > $previousIndex")
                    }
                }
            }

            output;
        }

    }

    private fun interpolate(y0: Float, y1: Float, y2: Float, y3: Float, alpha: Float): Float {


        val oma = 1 - alpha
        val oma2 = oma * oma
        val oma3 = oma2 * oma
        val alpha2 = alpha * alpha
        val alpha3 = alpha2 * alpha

        val a =  y1 * oma3 + 3*(y1 + (y2 - y0)/6) * alpha * oma2 + 3*(y2-(y3 - y2)/6) * alpha2 * oma + y2 * alpha3

        println("$y0 $y1 $y2 $y3   -- $alpha > $a")

        return a
    }

    private fun flipInto(incoming: FBuffer, storage: FloatArray): Int {
        storage[0] = storage[storage.size - 3]
        storage[1] = storage[storage.size - 2]
        storage[2] = storage[storage.size - 1]
        incoming.a.get(storage, 3, incoming.a.limit())

        return 2
    }

}

private operator fun _FBuffer.invoke(): FBuffer =
    this.get()
