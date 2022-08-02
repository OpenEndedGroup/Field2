package trace.mocap

import field.linalg.Vec3
import java.io.File
import java.nio.file.Files

class LoadTRC(val f: String) {
    var numbers: List<List<Double>>
    var names: List<String>

    var interpolators = mapOf<String, CInterp3>()

    init {
        val lines = Files.readAllLines(File(f).toPath())

        names = lines[3].trim().split("\t").subList(2).windowed(1, step = 3).map { it[0] }
        numbers = lines.subList(6).map {
            it.split("\t").subList(2).map {
                if (it.trim().length == 0)
                    Double.NaN
                else
                    it.toDouble()
            }
        }
        interpolators = names.mapIndexed { i, s ->
            s to CInterp3(pointsNotNan(i).map { it.first.toDouble() to it.second })
        }.toMap()
    }

    fun getPoint(name : String, x : Double) : Vec3 = interpolators[name]!!.interpolate(x)


    fun pointSource(): (String, Double) -> Vec3 {
        return { k, t ->
            interpolators[k]!!.interpolate(t)
        }
    }

    fun range(): Pair<Vec3, Vec3> {
        val max = Vec3(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY)
        val min = Vec3(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY)
        names.indices.forEach {
            pointsNotNan(it).forEach {
                max.max(it.second)
                min.min(it.second)
            }
        }
        return max to min
    }

    fun pointsNotNan(n: Int): List<Pair<Int, Vec3>> {

        return numbers.mapIndexed { i, d ->
            i to Vec3(d[n * 3 + 0], d[n * 3 + 1], d[n * 3 + 2])
        }.filter { !it.second.isNaN }
    }

    fun points(n: Int): List<Pair<Int, Vec3>> {
        return numbers.mapIndexed { i, d ->
            i to Vec3(d[n * 3 + 0], d[n * 3 + 1], d[n * 3 + 2])
        }
    }
}

private fun <E> List<E>.subList(fromIndex: Int) = this.subList(fromIndex, this.size)
