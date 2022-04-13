package fieldpython

import field.graphics.Texture
import jep.NDArray
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*

class TextureFromArray {

    companion object {
        val previous = mutableMapOf<String, Texture>()

        @JvmStatic
        fun make(name: String, unit: Int, n: NDArray<*>): Texture {

            val d = n.data
            val dim = n.dimensions

            if (d is ByteArray && dim.size == 3 && dim[2] == 3) {
                return make_byte3(name, unit, n)
            }

            throw IllegalArgumentException(" can't fathom what to do with a NDArray of dimensions ${Arrays.toString(n.dimensions)} and of type ${d::class.java}")
        }

        private fun make_byte3(name: String, unit: Int, n: NDArray<*>): Texture {
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
            println("$name -> $unit -> ${r.specification.pixels} -> ${r.specification.pixels.get(0)}, ${r.specification.pixels.get(1)}, ${r.specification.pixels.get(2)}")
            r.upload(r.specification.pixels, true)
            return r
        }
    }
}