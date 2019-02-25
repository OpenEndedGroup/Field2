package trace.mocap

import com.google.gson.JsonObject
import com.google.gson.stream.JsonReader
import field.linalg.Vec2
import field.linalg.Vec3
import org.json.JSONObject
import org.json.JSONTokener
import java.io.*
import java.util.zip.ZipInputStream

class OpenPose(val fn: String, val imageWidth: Int, val imageHeight: Int) {

    var files: Array<out File>?

    inner class Person {
        val allJoints = mutableListOf<Vec2>()
        val joints = mutableListOf<Vec3>()

        val face = mutableListOf<Vec3>()
        val leftHand = mutableListOf<Vec3>()
        val rightHand = mutableListOf<Vec3>()

        fun hasFace() = face.size > 0
        fun hasLeftHand() = leftHand.size > 0
        fun hasRightHand() = rightHand.size > 0

    }

    init {
        files = File(fn).listFiles { d, x -> x.endsWith(".json") }
    }

    private fun read(f: File): List<Person> {
        if (files == null) return mutableListOf()

        val r = JSONTokener(FileInputStream(f)).nextValue() as JSONObject
        val p = r.getJSONArray("people")

        val ret = mutableListOf<Person>()

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
                person.joints.add(Vec3(100 * x / imageWidth, 100 * y / imageHeight, c))
            }
            val f = personObject.getJSONArray("face_keypoints_2d")
            if (f != null) {
                for (qq in 0 until f.length() / 3) {
                    val x = f.getDouble(qq * 3 + 0)
                    val y = f.getDouble(qq * 3 + 1)
                    val c = f.getDouble(qq * 3 + 2)

                    person.face.add(Vec3(100 * x / imageWidth, 100 * y / imageHeight, c))
                }
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
        return ret
    }

}