package trace.input

import field.linalg.Vec2
import field.linalg.Vec3
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class LiveOpenPose(val fn: String, val doFaces: Boolean) {

    class Person {
        val points = mutableListOf<Vec3>()
    }

    class Face {
        val points = mutableListOf<Vec3>()
    }

    var people = mutableListOf<Person>()
    var faces = mutableListOf<Face>()

    var previousPeople = mutableListOf<List<Person>>()
    var previousFaces = mutableListOf<List<Face>>()


    init {

        var p = ProcessBuilder()
        if (doFaces) p.command(fn, "-face")
        else p.command(fn)
        p.directory(File(fn).parentFile.parentFile)
        p.redirectErrorStream(true)

        println(" starting process ...")

        val pp = p.start()

        val stream = pp.inputStream

        val reader = BufferedReader(InputStreamReader(stream))

        Thread {
            while (true) {
                while (!line(reader).toLowerCase().startsWith("keypoints:"));

                val l1 = line(reader)
                print(l1.toLowerCase().startsWith("person pose keypoints"))
                val people = mutableListOf<Person>()

                var l2 = line(reader)
                while (l2.startsWith("Person")) {
                    val p = readPerson(reader)
                    people.add(p)
                    l2 = line(reader)
                }

                while (!line(reader).toLowerCase().startsWith("face keypoints"));
                l2 = line(reader)
                if (l2.trim().length > 0) {
                    push(faces, previousFaces)
                    faces = readFaces(l2, reader)
                }


                push(this.people, this.previousPeople)
                this.people = people
            }

        }.start()
    }


    fun <T> push(people: MutableList<T>, previousPeople: MutableList<List<T>>) {

        previousPeople.add(people)
        if (previousPeople.size > 10)
            previousPeople.removeAt(0)

    }

    private fun readFaces(ff: String, reader: BufferedReader): MutableList<Face> {

        var f = Face()

        var first = ff
        while (first.trim().length > 0) {

            f.points.add(first.split(" ").toVec3())
            first = line(reader)
        }


        return mutableListOf<Face>(f)

    }

    private fun line(reader: BufferedReader): String {
        val r = reader.readLine()
        println("read $r")
        return r
    }

    private fun readPerson(reader: BufferedReader): Person {
        val p = Person()

        var n = 0
        while (n < 25) {
            var r = line(reader)

            val pieces = r.split(" ")
            p.points.add(Vec3(pieces[0].toDouble(), pieces[1].toDouble(), pieces[2].toDouble()))

            n++
        }

        return p
    }
}

private fun <String> List<String>.toVec3(): Vec3 {
    return Vec3(this[0].toString().toDouble(), this[1].toString().toDouble(), this[2].toString().toDouble())
}
