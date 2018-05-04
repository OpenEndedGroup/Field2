package trace.graphics

import field.linalg.Vec3
import java.io.*
import java.lang.Float


import field.graphics.MeshBuilder
import field.utility.times

import java.io.BufferedReader
import java.io.File
import java.io.FileNotFoundException
import java.io.FileReader
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel.MapMode
import java.util.ArrayList


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
            }
            else if (elements.size == 10) {
                val p = Point(set(elements, 0), set(elements, 6) * (1 / 255.0), set(elements, 3))

                points.add(p)
            }else if (elements.size == 7) {
                val p = Point(set(elements, 0), set(elements, 4) * 1 / 255.0, null)

                points.add(p)
            }

            num += 1
            if (num % 10000 == 0)
                println(" loaded :$num")

        }
    }

    private operator fun set(elements: Array<String>, i: Int): Vec3 {
        return Vec3(Float.parseFloat(elements[i]).toDouble(), Float.parseFloat(elements[i + 1]).toDouble(), Float.parseFloat(elements[i + 2]).toDouble())
    }

    fun allPositions(): List<Vec3> {
        val v = ArrayList<Vec3>(points.size)
        for (p in points)
            v.add(p.at)
        return v
    }

}
