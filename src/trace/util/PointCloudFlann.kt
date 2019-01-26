package trace.util

import marc.math.Flann

class PointCloudFlann(val pc: trace.graphics.PointCloud) {
    private var f: Flann

    init {
        f = Flann()
        f.build3d((0 until pc.points.size).toList(), { pc.points[it].first })
    }

    fun closestN(to : Int, n : Int): MutableList<Int>? {
        return f.find3(pc.points[to].first, n)
    }

}