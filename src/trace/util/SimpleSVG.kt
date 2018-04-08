package trace.util

import field.graphics.FLine
import field.graphics.MeshBuilder
import field.graphics.StandardFLineDrawing
import field.linalg.Vec3
import field.linalg.Vec4
import field.utility.Dict
import field.utility.Rect
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.util.*
import java.util.regex.Pattern

class SimpleSVG(val filename: String) {
    var bounds: Rect? = null;

    private var all: List<FLine>

    init {

        val r = mutableListOf<FLine>()

//        val mm = Pattern.compile("fill=\"rgb\\(([0-9]+),([0-9]+),([0-9]+)\\)\" .*? d=\"([MLCQZ\\- 0-9\\.]+)\"")
        val mm = Pattern.compile("style=\"fill:#(..)(..)(..);\" .*?d=\"([cqmzMLCQZ\\- ,\t0-9\\.]+)\"")
        (Files.readAllLines(File(filename).toPath())).joinToString(" ").split("/>").forEach {

//            println(it)
            //<path fill="rgb(60,108,146)" stroke="rgb(60,108,146)" stroke-width="1" opacity="1" d="M 153.5 0 L 153.5 1 L
            //<path fill="#9093a4" opacity="1.00" d=" M 189.78 174.70 C 191.07 174.31 192.38 173.97 193.69 173.65 C 194.20 173.77 195.23 174.00 195.74 174.12 C 198.42 174.70 201.22 174.78 203.95 175.10 C 203.78 176.67 201.33 175.58 200.73 176.98 C 202.31 176.99 205.46 177.01 207.03 177.02 L 207.99 177.02 C 207.99 177.53 208.00 178.56 208.00 179.07 C 207.18 179.13 205.56 179.25 204.74 179.30 C 203.57 179.17 202.39 179.05 201.21 178.95 L 200.29 178.83 C 199.77 178.56 198.74 178.00 198.22 177.73 C 195.62 176.18 192.62 175.61 189.78 174.70 Z" />
            val mmm = mm.matcher(it)
            if (mmm.find()) {
//                println("found")
                val colorr = mmm.group(1)
                val colorg = mmm.group(2)
                val colorb = mmm.group(3)

                val path = mmm.group(4)
                val qq = toFLine(path, colorStringToColor2(colorr, colorg, colorb), { 0.0 })
                bounds = Rect.union(bounds, qq.bounds())
                r += qq
            }
        }


        all = r.map {
            it.byTransforming {
                Vec3(100 * (it.x - bounds!!.x) / bounds!!.w, 100 * (it.y - bounds!!.y) / bounds!!.h, 0.0)
            }
        }

    }

    fun lines(): List<FLine> {
        return all
    }

    private fun applyToBuilder(color: Vec4, path: String, m: MeshBuilder, z: (FLine) -> Double) {
        var f = toFLine(path, color, z)

        StandardFLineDrawing.dispatchLine(f, m, null, null, Optional.empty(), null)
    }

    private fun toFLine(path: String, color: Vec4, z: (FLine) -> Double): FLine {
        var f = FLine()
        val p2 = path.replace("-", " -")
                .replace("M", " M ")
                .replace("L", " L ")
                .replace("l", " l ")
                .replace("C", " C ")
                .replace("c", " c ")
                .replace("Q", " Q ")
                .replace("q", " q ")
                .replace("Z", " Z ")
                .replace("z", " z ")
                .replace(Regex(" +"), " ")
                .replace(Regex("\t+"), " ")
                .replace(Regex(" +"), " ")
                .trim()
        val pieces = p2.split(" ", ",")

//        println("looking at ::" + p2 + "::")

        var i = 0
        while (i < pieces.size) {
            when (pieces[i]) {
                "M" -> {
                    f.moveTo(pieces[++i].toDouble(), pieces[++i].toDouble())
                }
                "L" -> {
                    f.lineTo(pieces[++i].toDouble(), pieces[++i].toDouble())
                }
                "l" -> {
                    f.lineToRel(pieces[++i].toFloat(), pieces[++i].toFloat())
                }
                "C" -> {
                    f.cubicTo(pieces[++i].toDouble(), pieces[++i].toDouble(), pieces[++i].toDouble(), pieces[++i].toDouble(), pieces[++i].toDouble(), pieces[++i].toDouble())
                }
                "c" -> {
                    f.cubicToRel(pieces[++i].toDouble(), pieces[++i].toDouble(), pieces[++i].toDouble(), pieces[++i].toDouble(), pieces[++i].toDouble(), pieces[++i].toDouble())
                }
                "Q" -> {
                    f.quadTo(pieces[++i].toDouble(), pieces[++i].toDouble(), pieces[++i].toDouble(), pieces[++i].toDouble())
                }
                "q" -> {
                    f.quadToRel(pieces[++i].toDouble(), pieces[++i].toDouble(), pieces[++i].toDouble(), pieces[++i].toDouble())
                }
                "Z" -> {
                }
                "z" -> {
                }
                else -> {
                    throw IllegalArgumentException(">>" + pieces[i] + "<< " + f.nodes)
                }
            }
            i++
        }


        f.attributes += StandardFLineDrawing.stroked to false
        f.attributes += StandardFLineDrawing.filled to true
        f.attributes += StandardFLineDrawing.color to color

        val zz = z(f)

        for (o in f.nodes)
            o.setZ(zz.toFloat())
        return f
    }

    private fun colorStringToColor(colorr: String, colorg: String, colorb: String): Vec4 {

        val r = Integer.parseInt(colorr)
        val g = Integer.parseInt(colorg)
        val b = Integer.parseInt(colorb)

        return Vec4(r / 255.0, g / 255.0, b / 255.0, 1.0)

    }

    private fun colorStringToColor2(colorr: String, colorg: String, colorb: String): Vec4 {

        val r = Integer.parseInt(colorr, 16)
        val g = Integer.parseInt(colorg, 16)
        val b = Integer.parseInt(colorb, 16)

        return Vec4(r / 255.0, g / 255.0, b / 255.0, 1.0)

    }

}

private operator fun <T> Dict.plusAssign(a: Pair<Dict.Prop<T>, T>) {
    this.put(a.first, a.second)
}
