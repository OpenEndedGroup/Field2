package trace.util

import field.graphics.FLine
import field.graphics.StandardFLineDrawing
import field.linalg.Vec2
import field.linalg.Vec3
import field.linalg.Vec4
import field.utility.Vec4
import field.utility.remAssign
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter

import java.util.function.Function

/**
 * Created by marc on 6/25/17.
 */

class Rasterizer(val filename: String, val transform: (Vec3) -> Vec2) {
    private val filename_out: String
    private var out: FileOutputStream

    init {
        this.filename_out = filename + ".exr"
        this.out = FileOutputStream(File(filename))
    }

    fun append(f: FLine) {

        val f2 = f.byTransforming { x -> transform(x).toVec3() }
        out.write('B'.toInt())
        for (n in f2.nodes) {
            when (n) {
                is FLine.CubicTo -> {
                    out.write('c'.toInt()); F(n.c1.x); F(n.c1.y); F(n.c2.x); F(n.c2.y); F(n.to.x); F(n.to.y)
                }
                is FLine.LineTo -> {
                    out.write('l'.toInt()); F(n.to.x); F(n.to.y)
                }
                is FLine.MoveTo -> {
                    out.write('m'.toInt()); F(n.to.x); F(n.to.y)
                }
            }
        }


        run {
            val c = f2.attributes.getOr(StandardFLineDrawing.color, { Vec4(1, 1, 1, 1) })?.get()
            if (c != null) {
                out.write('s'.toInt())
                F(c.x)
                F(c.y)
                F(c.z)
                F(c.w)
                out.write('f'.toInt())
                F(c.x)
                F(c.y)
                F(c.z)
                F(c.w)
            }
        }

        run {
            val c = f2.attributes.get(StandardFLineDrawing.strokeColor)?.get()
            if (c != null) {
                out.write('s'.toInt())
                F(c.x)
                F(c.y)
                F(c.z)
                F(c.w)
            }
        }
        run {
            val c = f2.attributes.get(StandardFLineDrawing.fillColor)?.get()
            if (c != null) {
                out.write('f'.toInt())
                F(c.x)
                F(c.y)
                F(c.z)
                F(c.w)
            }
        }

        if (f2.attributes.isTrue(StandardFLineDrawing.stroked, true) && f2.attributes.isTrue(StandardFLineDrawing.filled, false))
            out.write('q'.toInt())
        else if (f2.attributes.isTrue(StandardFLineDrawing.stroked, true))
            out.write('S'.toInt())
        else if (f2.attributes.isTrue(StandardFLineDrawing.filled, false))
            out.write('F'.toInt())


    }

    fun appendQuad(a: Vec2, b: Vec2, c: Vec2, d: Vec2) {
        out.write('B'.toInt())
        out.write('m'.toInt())
        F(transform(a.toVec3()))
        out.write('l'.toInt())
        F(transform(b.toVec3()))
        out.write('l'.toInt())
        F(transform(c.toVec3()))
        out.write('l'.toInt())
        F(transform(d.toVec3()))
        out.write('z'.toInt())
    }

    fun finish() {
        out.close()
    }

    val currentStrokeColor = Vec4(-1, -1, -1, -1)
    val currentFillColor = Vec4(-1, -1, -1, -1)
    var currentStrokeThickness = -1.0

    fun strokeInfo(c: Vec4, t: Number) {
        if (c.distance(currentStrokeColor) > 1e-10) {
            out.write('s'.toInt())
            F(c.x)
            F(c.y)
            F(c.z)
            F(c.w)
            currentStrokeColor %= c
        }
        if (Math.abs(currentStrokeThickness - t.toDouble()) > 1e-10) {
            out.write('w'.toInt())
            F(t.toDouble())
            currentStrokeThickness = t.toDouble()
        }
    }

    fun fillInfo(c: Vec4) {
        if (c.distance(currentFillColor) > 1e-10) {
            out.write('f'.toInt())
            F(c.x)
            F(c.y)
            F(c.z)
            F(c.w)
            currentFillColor %= c
        }
    }

    fun fill() {
        out.write('F'.toInt())
    }

    fun stroke() {
        out.write('S'.toInt())
    }

    private fun F(x: Vec2) {
        F(x.x)
        F(x.y)
    }

    private fun F(x: Double) {

        val bits = java.lang.Float.floatToIntBits(x.toFloat())

        val b0 = (bits.shr(24)).and(0xff)
        val b1 = (bits.shr(16)).and(0xff)
        val b2 = (bits.shr(8)).and(0xff)
        val b3 = (bits.shr(0)).and(0xff)
//		println("writing "+b0+" "+b1+" "+b2+" "+b3+"  from "+x.toFloat())
        out.write(b3)
        out.write(b2)
        out.write(b1)
        out.write(b0)
    }

    companion object {
        val command = "/usr/local/bin/python3 /Users/marc/temp/rasterizer/rasterize.py %s %s"
    }

}
