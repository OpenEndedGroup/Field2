package trace.mocap

import field.linalg.Vec2
import field.linalg.Vec3
import org.json.JSONObject
import org.json.JSONTokener
import java.io.File
import java.io.FileInputStream

class OpenPose(val fn: String, val imageWidth: Int, val imageHeight: Int) {

    var files: List<out File>?

    class Frame {
        val people = mutableListOf<Person>()
        val faces = mutableListOf<Face>()

        override fun toString(): String {
            return "Frame with ${people.size} people and ${faces.size} faces"
        }
    }

    var frames = mutableListOf<Frame>()

    inner class Face {
        val points = mutableListOf<Vec3>()
        override fun toString(): String {
            return "Face with ${points.size} points"
        }
    }

    inner class Person {
        val allJoints = mutableListOf<Vec2>()
        val points = mutableListOf<Vec3>()

        val face = mutableListOf<Vec3>()
        val leftHand = mutableListOf<Vec3>()
        val rightHand = mutableListOf<Vec3>()

        fun hasFace() = face.size > 0
        fun hasLeftHand() = leftHand.size > 0
        fun hasRightHand() = rightHand.size > 0

        var done = false

        var frame = 0
        var person = 0

        override fun toString(): String {
            return "Person with ${points.size} points / ${points.filter { it.z>0 }.size} confident points"
        }

    }

    init {
        files = File(fn).listFiles { d, x -> x.endsWith(".json") }.sorted()

        files!!.forEach {
            val (p, face) = read(it)
            var f = Frame()

            p.forEach {
                it.frame = frames.size
                it.person = f.people.size
                f.people.add(it)
            }

            f.faces.addAll(face)
            frames.add(f)
        }
    }

    fun getPoint(frame: Int, person: Int, point: Int): Vec3 {
        try {

            var a = if (frame < 0) 0 else if (frame > frames.size - 1) frames.size - 1 else frame
            var ff = frames[a]
            var p = if (person < 0) 0 else if (person > ff.people.size - 1) ff.people.size - 1 else person
            var pp = ff.people[p]

            var o = if (point < 0) 0 else if (point > pp.points.size - 1) pp.points.size - 1 else point

            return pp.points[o]
        } catch (a: ArrayIndexOutOfBoundsException) {

        }
        return Vec3(0.0, 0.0, 0.0)
    }


    private fun read(f: File): Pair<List<Person>, List<Face>> {
        if (files == null) return mutableListOf<Person>() to mutableListOf<Face>()

        val r = JSONTokener(FileInputStream(f)).nextValue() as JSONObject
        val p = r.getJSONArray("people")

        val ret = mutableListOf<Person>()
        val retFace = mutableListOf<Face>()

        for (pp in 0 until p.length()) {
            val personObject = p.getJSONObject(pp)
            val person = Person()

            val q = personObject.getJSONArray("pose_keypoints_2d")
            for (qq in 0 until q.length() / 3) {
                val x = q.getDouble(qq * 3 + 0)
                val y = q.getDouble(qq * 3 + 1)
                val c = q.getDouble(qq * 3 + 2)

                if (c > 0) {
                    person.allJoints.add(Vec2(100 * x / imageWidth, 100 * y / imageHeight))
                }
                person.points.add(Vec3(100 * x / imageWidth, 100 * y / imageHeight, c))
            }
            val f = personObject.getJSONArray("face_keypoints_2d")
            if (f != null) {
                var ff = Face()

                for (qq in 0 until f.length() / 3) {
                    val x = f.getDouble(qq * 3 + 0)
                    val y = f.getDouble(qq * 3 + 1)
                    val c = f.getDouble(qq * 3 + 2)

                    person.face.add(Vec3(100 * x / imageWidth, 100 * y / imageHeight, c))
                    ff.points.add(Vec3(100 * x / imageWidth, 100 * y / imageHeight, c))
                }
                retFace.add(ff)
            }

            val lh = personObject.getJSONArray("hand_left_keypoints_2d")
            if (lh != null) {
                for (qq in 0 until lh.length() / 3) {
                    val x = lh.getDouble(qq * 3 + 0)
                    val y = lh.getDouble(qq * 3 + 1)
                    val c = lh.getDouble(qq * 3 + 2)

                    person.leftHand.add(Vec3(100 * x / imageWidth, 100 * y / imageHeight, c))
                }
            }

            val rh = personObject.getJSONArray("hand_right_keypoints_2d")
            if (rh != null) {
                for (qq in 0 until rh.length() / 3) {
                    val x = rh.getDouble(qq * 3 + 0)
                    val y = rh.getDouble(qq * 3 + 1)
                    val c = rh.getDouble(qq * 3 + 2)

                    person.rightHand.add(Vec3(100 * x / imageWidth, 100 * y / imageHeight, c))
                }
            }

            ret.add(person)
        }
        return ret to retFace
    }

}