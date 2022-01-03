package trace.mocap

import field.linalg.Vec2
import field.linalg.Vec3
import trace.util.TCB3

class OpenPoseTracks(val op: OpenPose) {


    class Track(val jointNumber: Int) {
        val position = TCB3()
    }

    private var notDone: MutableSet<OpenPose.Person>
    val tracks = mutableListOf<Track>()


    init {
        notDone = op.frames.flatMap {
            it.people
        }.map {
            it.done = false
            it
        }.toMutableSet()

        while (notDone.isNotEmpty()) {
            val i = notDone.iterator()
            val p = i.next()
            notDone.remove(p)

            tracks.addAll(track(p))

        }

    }

    fun track(p: OpenPose.Person): List<Track> {

        val tracks = p.points.mapIndexed { i, at ->

            var t = Track(i)
            if (at.z > 0) {
                addToTrack(t, at, p.frame)
            }

            t
        }

        for (f in p.frame + 1 until op.frames.size) {
            link(tracks, op.frames[f])
        }

        tracks.forEach {
            if (it.position.n.size > 0) {
                it.position.tcbAll()
            }
        }

        return tracks
    }

    private fun link(current: List<Track>, nextFrame: OpenPose.Frame) {

        val P = nextFrame.people.filter { !it.done }.toList()

        if (P.size == 0) return
        if (P.size == 1) linkTo(current, nextFrame.people[0])

        linkTo(current, P.sortedBy {
            score(current, it)
        }.first())
    }

    private fun linkTo(current: List<Track>, person: OpenPose.Person) {
        person.done = true
        notDone.remove(person)

        current.mapIndexed { index, t ->

            if (person.points[index].z > 0) {
                addToTrack(t, person.points[index], person.frame)
            }

        }
    }

    private fun score(at: List<Track>, next: OpenPose.Person): Double {

        val average = at.mapIndexed { i, track ->
            if (track.position.n.size > 0) {
                val v1 = track.position.n[track.position.n.size - 1].v
                val v2 = next.points.get(i)
                if (v2.z > 0) {
                    1 to v1.distance(v2)
                } else {
                    0 to 0.0
                }
            } else
                0 to 0.0
        }.reduce { a, b ->
            a.first + b.first to a.second + b.second
        }

        if (average.first == 0) return 1000.0
        return average.second / average.first
    }

    private fun addToTrack(t: Track, at: Vec3, frame: Int) {
        t.position.Node(frame.toDouble(), Vec3(at))
    }

}