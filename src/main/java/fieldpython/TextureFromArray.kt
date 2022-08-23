package fieldpython

import field.graphics.Texture
import jep.DirectNDArray
import jep.NDArray
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*

class TextureFromArray {

    companion object {
        val previous = mutableMapOf<String, Texture>()

        @JvmStatic
        fun make(name: String, unit: Int, n: NDArray<*>): Pair<Texture, () -> Unit> {

            val d = n.data
            val dim = n.dimensions

            if (d is ByteArray && dim.size == 3 && dim[2] == 3) {
                return make_byte3(name, unit, n)
            }

            if (d is ByteArray && dim.size == 2) {
                return make_byte1(name, unit, n)
            }

            if (d is FloatArray && dim.size == 3) {
                return make_float3(name, unit, n)
            }

            throw IllegalArgumentException(" can't fathom what to do with a NDArray of dimensions ${Arrays.toString(n.dimensions)} and of type ${d::class.java}")
        }

        @JvmStatic
        fun makeDirect(name: String, unit: Int, n: DirectNDArray<*>): Pair<Texture, () -> Unit> {

            val d = n.data
            val dim = n.dimensions

            if (d is ByteBuffer && dim.size == 3 && dim[2] == 3) {
                return make_byte3_buffer(name, unit, n)
            }

            if (d is ByteBuffer && dim.size == 2) {
                return make_byte1_buffer(name, unit, n)
            }

            throw IllegalArgumentException(" can't fathom what to do with a NDArray of dimensions ${Arrays.toString(n.dimensions)} and of type ${d::class.java}")
        }

        private fun make_byte3(name: String, unit: Int, n: NDArray<*>): Pair<Texture, () -> Unit> {
            val d = n.data
            val dim = n.dimensions

            val r = previous.computeIfAbsent(name) {
                val source = ByteBuffer.allocateDirect(dim[0] * dim[1] * 3).order(ByteOrder.nativeOrder())
                source.put(d as ByteArray)
                source.rewind()
                Texture(Texture.TextureSpecification.byte3_rev(unit, dim[1], dim[0], source, false))
            }

            r.specification.pixels.rewind()
            r.specification.pixels.put(d as ByteArray)
            r.specification.pixels.rewind()
            println(
                "$name -> $unit -> ${r.specification.pixels} -> ${r.specification.pixels.get(0)}, ${
                    r.specification.pixels.get(
                        1
                    )
                }, ${r.specification.pixels.get(2)}"
            )
            r.upload(r.specification.pixels, true)
            return r to {
                r.specification.pixels.rewind()
                r.specification.pixels.put(n.data as ByteArray)
                r.specification.pixels.rewind()
                r.upload(r.specification.pixels, true)
            }
        }

        private fun make_float3(name: String, unit: Int, n: NDArray<*>): Pair<Texture, () -> Unit> {
            val d = n.data
            val dim = n.dimensions

            val r = previous.computeIfAbsent(name) {
                val source = ByteBuffer.allocateDirect(dim[0] * dim[1] * 3 * 4).order(ByteOrder.nativeOrder())
                val sourceF = source.asFloatBuffer()
                source.rewind()
                sourceF.put(d as FloatArray)
                source.rewind()
                Texture(Texture.TextureSpecification.float3(unit, dim[1], dim[0], source))
            }

            r.specification.pixels.rewind()
            r.specification.pixels.asFloatBuffer().put(d as FloatArray)
            r.specification.pixels.rewind()
            println(
                "$name -> $unit -> ${r.specification.pixels} -> ${r.specification.pixels.get(0)}, ${
                    r.specification.pixels.get(
                        1
                    )
                }, ${r.specification.pixels.get(2)}"
            )
            r.upload(r.specification.pixels, true)
            return r to {
                r.specification.pixels.rewind()
                r.specification.pixels.asFloatBuffer().put(n.data as FloatArray)
                r.specification.pixels.rewind()
                r.upload(r.specification.pixels, true)
            }
        }

        private fun make_byte1(name: String, unit: Int, n: NDArray<*>): Pair<Texture, () -> Unit> {
            val d = n.data
            val dim = n.dimensions

            val r = previous.computeIfAbsent(name) {
                val source = ByteBuffer.allocateDirect(dim[0] * dim[1] ).order(ByteOrder.nativeOrder())
                source.put(d as ByteArray)
                source.rewind()
                Texture(Texture.TextureSpecification.byte1(unit, dim[1], dim[0], source, false))
            }

            r.specification.pixels.rewind()
            r.specification.pixels.put(d as ByteArray)
            r.specification.pixels.rewind()
            println(
                "$name -> $unit -> ${r.specification.pixels} -> ${r.specification.pixels.get(0)}, ${
                    r.specification.pixels.get(
                        1
                    )
                }, ${r.specification.pixels.get(2)}"
            )
            r.upload(r.specification.pixels, true)
            return r to {
                r.specification.pixels.rewind()
                r.specification.pixels.put(n.data as ByteArray)
                r.specification.pixels.rewind()
                r.upload(r.specification.pixels, true)
            }
        }

        private fun make_byte3_buffer(name: String, unit: Int, n: DirectNDArray<*>): Pair<Texture, () -> Unit> {
            val dim = n.dimensions

            val r = previous.computeIfAbsent(name) {
                val source = n.data as ByteBuffer
                Texture(Texture.TextureSpecification.byte3_rev(unit, dim[1], dim[0], source, false))
            }

            if (r.specification.pixels != n.data) throw java.lang.IllegalArgumentException(" DirectNDArray has changed storage location ")
            r.specification.pixels.rewind()
            println(
                "$name -> $unit -> ${r.specification.pixels} -> ${r.specification.pixels.get(0)}, ${
                    r.specification.pixels.get(
                        1
                    )
                }, ${r.specification.pixels.get(2)}"
            )
            r.upload(r.specification.pixels, true)
            return r to {
                r.upload(r.specification.pixels, true)
            }
        }

        private fun make_byte1_buffer(name: String, unit: Int, n: DirectNDArray<*>): Pair<Texture, () -> Unit> {
            val dim = n.dimensions

            val r = previous.computeIfAbsent(name) {
                val source = n.data as ByteBuffer
                Texture(Texture.TextureSpecification.byte1(unit, dim[1], dim[0], source, false))
            }

            if (r.specification.pixels != n.data) throw java.lang.IllegalArgumentException(" DirectNDArray has changed storage location ")
            r.specification.pixels.rewind()
            println(
                "$name -> $unit -> ${r.specification.pixels} -> ${r.specification.pixels.get(0)}, ${
                    r.specification.pixels.get(
                        1
                    )
                }, ${r.specification.pixels.get(2)}"
            )
            r.upload(r.specification.pixels, true)
            return r to {
                r.upload(r.specification.pixels, true)
            }
        }
    }
}