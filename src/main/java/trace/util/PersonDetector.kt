package trace.util

import field.utility.Rect
import field.utility.Vec3
import java.io.File
import java.nio.file.Files

class PersonDetector(val fn: String, val w: Int, val h: Int) {

    inner class RectWithName(var name: String, x: Double, y: Double, w: Double, h: Double) : Rect(x, y, w, h) {
        var track: Track? = null
    }

    inner class Frame(val index: Int) {
        val r = mutableListOf<RectWithName>()

    }

    inner class Track {
        val x0 = TCB3()
        val y0 = TCB3()
        val x1 = TCB3()
        val y1 = TCB3()

        val frames = mutableListOf<Frame>()

        var lostFor = 0;
        var seen = false;

        var name = "";

        fun startTime(): Double {
            return x0.n[0].x / duration()
        }

        fun endTime(): Double {
            return x0.n.last().x / duration()
        }

        fun rectAtTime(t: Double): Rect {
            val ax0 = x0.evaluate(t * duration(), Vec3()).x
            val ax1 = x1.evaluate(t * duration(), Vec3()).x
            val ay0 = y0.evaluate(t * duration(), Vec3()).x
            val ay1 = y1.evaluate(t * duration(), Vec3()).x

            return Rect(100 * Math.min(ax0, ax1),
                    100 * Math.min(ay0, ay1),
                    100 * (Math.max(ax0, ax1) - Math.min(ax0, ax1)),
                    100 * (Math.max(ay0, ay1) - Math.min(ay0, ay1)))
        }
    }

    val frames = mutableListOf<Frame>()
    val tracks = mutableListOf<Track>()
    val allTracks = mutableListOf<Track>()

    init {

        val all = Files.readAllLines(File(fn).toPath())
        var currentFrame = Frame(-1)

        all.forEach {
            //Enter Image Path: /Users/marc/Desktop/zachAndXander/test1/src/o_000903.jpg: Predicted in 8.763337 seconds.

            if (it.startsWith("Enter Image Path:")) {
                currentFrame = Frame(frames.size)
                frames.add(currentFrame);
            }
            if (it.startsWith("-- person:")) {
                val p = it.trim().split(" ")
                val rr = RectWithName("", p[p.size - 4].toInt() / w.toDouble(), p[p.size - 3].toInt() / h.toDouble(), p[p.size - 2].toInt() / w.toDouble(), p[p.size - 1].toInt() / h.toDouble())
                rr.w = rr.w - rr.x
                rr.h = rr.h - rr.y
                if (rr.w < rr.h)
                    currentFrame.r.add(rr)
            }
        }

        frames.forEachIndexed { index, frame ->

            tracks.forEach { it.seen = false }
            tracks.forEach { claimBest(it, frame.r, frame.index) }

            frame.r.filter { it.track == null }.forEach {
                val t = Track()
                t.name = "track${allTracks.size}"
                claim(t, it, frame.index)
                t.seen = true
                tracks.add(t)
                allTracks.add(t)
            }

            tracks.forEach { if (!it.seen) it.lostFor++ }

            tracks.removeIf { it.lostFor > 6 }
        }

        allTracks.forEachIndexed { index, t ->
            t.x0.tcbAll()
            t.y0.tcbAll()
            t.x1.tcbAll()
            t.y1.tcbAll()
        }


    }


    private fun claimBest(track: Track, rects: MutableList<RectWithName>, frameNum: Int) {

        val x0 = track.x0.n.last().v.x
        val x1 = track.x1.n.last().v.x
        val y0 = track.y0.n.last().v.x
        val y1 = track.y1.n.last().v.x

        val r = Rect(Math.min(x0, x1), Math.min(y0, y1), Math.max(x0, x1), Math.max(y0, y1))
        r.w = r.w - r.x;
        r.h = r.h - r.y;
        val best = rects.filter { it.track == null }.maxByOrNull { it: RectWithName -> overlap(r, it) }

        if (best != null && overlap(r, best) / Math.max(r.area(), best.area()) > 0.5) {
            claim(track, best, frameNum)
        }
    }

    private fun overlap(r1: Rect, r2: Rect): Double {

        val left = Math.max(r1.x, r2.x)
        val top = Math.max(r1.y, r2.y)
        val right = Math.min(r1.x + r1.w, r2.x + r2.w)
        val bottom = Math.min(r1.y + r1.h, r2.y + r2.h)

        return Math.max(0f, (right - left)) * Math.max(0f, (bottom - top)).toDouble()
    }


    private fun claim(track: Track, best: RectWithName, frameNum: Int) {
        track.x0.newNode(frameNum.toDouble(), Vec3(Math.min(best.x, best.x + best.w), 0.0, 0.0))
        track.y0.newNode(frameNum.toDouble(), Vec3(Math.min(best.y, best.y + best.h), 0.0, 0.0))
        track.x1.newNode(frameNum.toDouble(), Vec3(Math.max(best.x, best.x + best.w), 0.0, 0.0))
        track.y1.newNode(frameNum.toDouble(), Vec3(Math.max(best.y, best.y + best.h), 0.0, 0.0))
        best.track = track
        track.seen = true
        track.lostFor = 0
        best.name = track.name
    }

    fun rectsAt(t: Double): List<Rect> {
        var frame = (t * frames.size).toInt()
        if (frame < 0) frame = 0
        if (frame > frames.size - 1) frame = frames.size - 1
        return frames[frame].r.map { Rect(it.x * 100, it.y * 100, it.w * 100, it.h * 100) }
    }

    fun tracksAt(t: Double): List<Track?> {
        var frame = (t * frames.size).toInt()
        if (frame < 0) frame = 0
        if (frame > frames.size - 1) frame = frames.size - 1
        return frames[frame].r.map { it.track }.filter { it != null }
    }

    fun duration(): Int {
        return frames.size
    }

}
