package trace.graphics


import field.linalg.Quat
import field.linalg.Vec3
import field.utility.remAssign
import field.utility.times
import java.io.*


class LoadPly {

    private var reader: BufferedReader? = null
    var points: ArrayList<Point> = ArrayList()
    var filename: String? = null

    data class Point(val at: Vec3, val color: Vec3?, val normal: Vec3?) {
    }

    @Throws(IOException::class)
    protected constructor() {
    }

    @Throws(IOException::class)
    constructor(filename: String) {
        this.filename = filename
        points = ArrayList()

        add(filename)
    }

    var first = true

    fun show(layer: Stage.ShaderGroup, name: String, size: Float) {
        val v = layer.pointBuilder(name)

        v.open()
        v.aux(2, size)
        points.forEach {
            v.aux(1, it.color)
            if (it.normal != null)
                v.aux(4, it.normal)
            if (it.color != null)
                v.aux(1, it.color)
            v.v(it.at)
        }
        v.close()
    }

    fun planarRotate(center: Vec3, up: Vec3, amount: Float) {
        val q = Quat().setAngleAxis(amount.toDouble(), up.normalize())

        points.forEach {
            val d = (it.at.x - center.x) * up.x + (it.at.y - center.y) * up.y + (it.at.z - center.z) * up.z;
            if (d > 0) {
                val v = q.transform(Vec3(it.at.x - center.x, it.at.y - center.y, it.at.z - center.z))
                v.x += center.x
                v.y += center.y
                v.z += center.z
                it.at %= v
            }
        }
    }


    @Throws(FileNotFoundException::class, IOException::class)
    protected fun add(filename: String) {
        reader = BufferedReader(FileReader(File(filename)), 1024 * 1024 * 10)
        while (true) {
            val m = reader!!.readLine()
            println("header :$m")
            if (m.startsWith("end_"))
                break
        }

        var num = 0
        while (reader!!.ready()) {
            val line = reader!!.readLine()
            val elements = line.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

            if (elements.size == 6) {
                val p = Point(set(elements, 0), set(elements, 3) * (1 / 255.0), null)

                points.add(p)
            } else if (elements.size == 9) {
                val p = Point(set(elements, 0), set(elements, 6) * (1 / 255.0), set(elements, 3))

                points.add(p)
            } else if (elements.size == 10) {
                val p = Point(set(elements, 0), set(elements, 6) * (1 / 255.0), set(elements, 3))

                points.add(p)
            } else if (elements.size == 7) {
                val p = Point(set(elements, 0), set(elements, 4) * (1 / 255.0), null)

                points.add(p)
            }

            num += 1
            if (num % 10000 == 0)
                println(" loaded :$num")

        }
    }

    private operator fun set(elements: Array<String>, i: Int): Vec3 {
        return Vec3(elements[i].toDouble(), (elements[i + 1]).toDouble(), (elements[i + 2]).toDouble())
    }

    fun allPositions(): List<Vec3> {
        val v = ArrayList<Vec3>(points.size)
        for (p in points)
            v.add(p.at)
        return v
    }

}
