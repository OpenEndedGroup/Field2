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
        val mm = Pattern.compile("style=\".*?fill:#(..)(..)(..);.*?\".*?d=\"([cqmzVHvhMLlCQZ\\- ,\t0-9\\.]+)\"")
        val mm2 = Pattern.compile("class=\".*?\".*?d=\"([cqmzVHvhMLlCQZ\\- ,\t0-9\\.]+)\"")
        val m2 = Pattern.compile("style=\".*?fill:none;.*?\" .*?d=\"([cqmzVHvhMLlCQZ\\- ,\t0-9\\.]+)\"")
        val m3 = Pattern.compile("d=\"([cqmzVHvhMLlCQZ\\- ,\t0-9\\.]+)\" .*?style=\".*?fill: #(..)(..)(..);.*?\"")
        val m3b = Pattern.compile("d=\"([cqmzVHvhMLlCQZ\\- ,\t0-9\\.]+)\"")

        val p1 = Pattern.compile("style=\".*?fill:#(..)(..)(..);.*?\".*?points=\"([cqmzVHvhMLlCQZ\\- ,\t0-9\\.]+)\"")
        (Files.readAllLines(File(filename).toPath())).joinToString(" ").split("/>").forEach {
            println("line: ${it}")

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
                return@forEach
            }
            val mmmB = mm2.matcher(it)
            if (mmmB.find()) {
//                println("found")
                val path = mmmB.group(1)
                val qq = toFLine(path, colorStringToColor2("00","00","00"), { 0.0 })
                bounds = Rect.union(bounds, qq.bounds())
                r += qq
                return@forEach
            }
            val mmm2 = m2.matcher(it)
            if (mmm2.find()) {
//                println("found")

                val path = mmm2.group(1)
                val qq = toFLine(path, colorStringToColor2("00", "00", "00"), { 0.0 })
                bounds = Rect.union(bounds, qq.bounds())
                r += qq
                return@forEach
            }
            val mmm3 = m3.matcher(it)
            if (mmm3.find()) {
//                println("found")

                val path = mmm3.group(1)
                val colorr = mmm3.group(2)
                val colorg = mmm3.group(3)
                val colorb = mmm3.group(4)

                val qq = toFLine(path, colorStringToColor2(colorr, colorg, colorb), { 0.0 })
                bounds = Rect.union(bounds, qq.bounds())
                r += qq
                return@forEach
            }
            val mmm3b = m3b.matcher(it)
            if (mmm3b.find()) {
//                println("found")

                val path = mmm3b.group(1)

                val qq = toFLine(path, colorStringToColor2("00", "00", "00"), { 0.0 })
                bounds = Rect.union(bounds, qq.bounds())
                r += qq
                return@forEach
            }

            val pp1 = p1.matcher(it)
            if (pp1.find()) {
//                println("found")

                val colorr = pp1.group(1)
                val colorg = pp1.group(2)
                val colorb = pp1.group(3)

                val path = pp1.group(4)
                val qq = toFLine(path, colorStringToColor2(colorr, colorg, colorb), { 0.0 })
                bounds = Rect.union(bounds, qq.bounds())
                r += qq
                return@forEach
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
                .replace("v", " v ")
                .replace("h", " h ")
                .replace("V", " V ")
                .replace("H", " H ")
                .replace(Regex(" +"), " ")
                .replace(Regex("\t+"), " ")
                .replace(Regex(" +"), " ")
                .replace(Regex("[ ,]+"), " ")
                .trim()
        val pieces = p2.split(" ", ",")

//        println("looking at ::" + p2 + "::")

        var i = 0
        var was: Vec3? = null
        var lastCommand = "M"

        println("parsing ${Arrays.asList(pieces)}")

        while (i < pieces.size) {
            var command = pieces[i]
            if (isNumber(command)) {
                command = lastCommand
                i--
            }
            when (command) {
                "M" -> {
                    f.moveTo(pieces[++i].toDouble(), pieces[++i].toDouble())
                    was = f.at()
                    command = "L"
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
                "V" -> {
                    f.lineTo(f.at().x, pieces[++i].toDouble())
                }
                "v" -> {
                    f.lineTo(f.at().x, f.at().y + pieces[++i].toDouble())
                }
                "H" -> {
                    f.lineTo(pieces[++i].toDouble(), f.at().y)
                }
                "h" -> {
                    f.lineTo(f.at().x + pieces[++i].toDouble(), f.at().y)
                }
                "Z" -> {
                    f.lineTo(was!!)
                }
                "z" -> {
                    f.lineTo(was!!)
                }
                else -> {
                    throw IllegalArgumentException(">>" + pieces[i] + "<< " + f.nodes + " element $i of ${Arrays.asList(pieces)}")
                }
            }
            i++
            lastCommand = command
        }


        f.attributes += StandardFLineDrawing.stroked to false
        f.attributes += StandardFLineDrawing.filled to true
        f.attributes += StandardFLineDrawing.color to color

        val zz = z(f)

        for (o in f.nodes)
            o.setZ(zz.toFloat())
        return f
    }

    private fun isNumber(q: String): Boolean {
        try {

            q.toDouble()
            return true
        } catch (e: NumberFormatException) {
            return false
        }
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
