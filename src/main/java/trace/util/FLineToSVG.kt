package trace.util

import field.graphics.FLine
import field.graphics.FLinesAndJavaShapes
import field.graphics.StandardFLineDrawing
import field.linalg.Vec2
import field.linalg.Vec3
import field.linalg.Vec4
import field.utility.plus
import field.utility.times
import org.jfree.graphics2d.svg.SVGGraphics2D
import trace.graphics.Stage
import java.awt.BasicStroke
import java.awt.Color
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardOpenOption

class FLineToSVG(val fn: String) {
    var g2: SVGGraphics2D

    init {

        g2 = SVGGraphics2D(1000, 1000)

    }

    fun addLine(f: FLine, sg: Stage.ShaderGroup) {

        val V = sg.__camera.view().transpose()
        val P = sg.__camera.projectionMatrix().transpose()
        val scale = sg.scale.duplicate()
        val bounds = sg.bounds.duplicate()

        val f2 = f.bySubdividing().byTransforming {

            val xy = (Vec2(it.x, it.y) + Vec2(0.5, 0.5)) * Vec2(1 / bounds.x, 1 / bounds.y)
            val xyz = Vec3(scale.x * (-1 + xy.x * 2), scale.y * (-1 + xy.y * 2), it.z)

            val H = P.transform(V.transform(Vec4(xyz.x, xyz.y, xyz.z, 1.0)))


            val v = Vec3(H.x / H.w, H.y / H.w, H.z / H.w)


            Vec3(v.x+1, v.y+1, v.z+1)*0.5*1000


//            Vec3(v.x+500, v.y+500, v.z+500)

        }

        val s = FLinesAndJavaShapes.flineToJavaShape_notThickened(f2)

        val strokeColor = f.attributes.getOr(StandardFLineDrawing.color, { Vec4(0.5, 0.5, 0.5, 1.0) }).get()
        val fillColor = f.attributes.getOr(StandardFLineDrawing.color, { Vec4(0.5, 0.5, 0.5, 1.0) }).get()

        if (f.attributes.getOr(StandardFLineDrawing.stroked) {true}) {

            g2.color = Color(strokeColor.x.toFloat(), strokeColor.y.toFloat(), strokeColor.z.toFloat(), strokeColor.w.toFloat())
            val ft = f.attributes.get(StandardFLineDrawing.fastThicken)

            if (ft != null) {
                g2.stroke = BasicStroke(ft)
            } else {
                val ft2 = f.attributes.get(StandardFLineDrawing.thicken)
                if (ft2 != null) {
                    g2.stroke = ft2
                } else {
                    g2.stroke = BasicStroke(1f)
                }
            }

            g2.draw(s)
        }

        if (f.attributes.getOr(StandardFLineDrawing.filled) {false}) {

            g2.color = Color(fillColor.x.toFloat(), fillColor.y.toFloat(), fillColor.z.toFloat(), fillColor.w.toFloat())
            g2.fill(s)
        }

    }

    fun save() {
        val contents = g2.getSVGDocument()

        Files.newBufferedWriter(File(fn).toPath(), StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING).use {
            it.write(contents)
        }

    }

}