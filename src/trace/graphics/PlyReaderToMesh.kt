package trace.graphics

import field.graphics.MeshBuilder
import field.linalg.Vec3
import field.linalg.Vec4
import field.utility.times
import java.io.File


class PlyReaderToMesh {
    fun load(fn: String, target: MeshBuilder) {
        val m = PlyReader.readMesh(File(fn))
        target.open()

        val startAt = target.vertexCursor

        m.first.forEach {


            var color: Vec4? = null
            var normal: Vec3? = null

            it.attributes.forEach {

                if (it.key.contains("red")) {
                    if (color == null) color = Vec4(0.0, 0.0, 0.0, 255.0)
                    color!!.x = it.value.get(0).toDouble()
                }
                if (it.key.contains("green")) {
                    if (color == null) color = Vec4(0.0, 0.0, 0.0, 255.0)
                    color!!.y = it.value.get(0).toDouble()
                }
                if (it.key.contains("blue")) {
                    if (color == null) color = Vec4(0.0, 0.0, 0.0, 255.0)
                    color!!.z = it.value.get(0).toDouble()
                }
                if (it.key.contains("alpha")) {
                    if (color == null) color = Vec4(0.0, 0.0, 0.0, 255.0)
                    color!!.w = it.value.get(0).toDouble()
                }

            }

            if (color != null) {
                target.aux(1, color!! * 1/255.0)
            }
            if (normal != null) {
                target.aux(2, normal)
            }

            target.v(it.at)

        }

        val nowAt = target.vertexCursor

        m.second.forEach {
            target.e((nowAt-startAt)-it[0]-1, (nowAt-startAt)-it[1]-1, (nowAt-startAt)-it[2]-1)
        }

        target.close()
    }
}