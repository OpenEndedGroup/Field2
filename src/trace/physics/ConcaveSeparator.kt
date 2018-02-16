package trace.physics

import field.linalg.Vec2

import java.util.*

object ConcaveSeparator {

    fun separate(vertices: List<Vec2>): List<List<Vec2>> {
        return calcShapes(vertices)
    }

    private fun removeZeroLengthEdges(normalizedVertices: Array<Vec2>): Array<Vec2> {
        if (normalizedVertices.size > 1) {
            val first = normalizedVertices[0]
            if (first == normalizedVertices[normalizedVertices.size - 1]) {
                val nonZeroVertices = LinkedList(Arrays.asList(*normalizedVertices))
                nonZeroVertices.removeLast()
                return nonZeroVertices.toTypedArray()
            }
        }
        return normalizedVertices
    }

    /**
     * Checks whether the vertices in `verticesVec` can be properly distributed into the new fixtures (more specifically, it makes sure there are no overlapping segments and the vertices are in clockwise order).
     * It is recommended that you use this method for debugging only, because it may cost more CPU usage.
     *
     *

     * @param vertices The vertices to be validated.
     * *
     * @return An integer which can have the following values:
     * *
     * *  * 0 if the vertices can be properly processed.
     * *  * 1 If there are overlapping lines.
     * *  * 2 if the points are **not** in clockwise order.
     * *  * 3 if there are overlapping lines **and** the points are **not** in clockwise order.
     * *
     */
    fun validate(vertices: List<Vec2>): Int {
        val listSize = vertices.size
        var ret = 0

        var fl2 = false
        for (i in 0..listSize - 1) {
            val i2 = if (i < listSize - 1) i + 1 else 0
            val i3 = if (i > 0) i - 1 else listSize - 1

            var fl = false
            for (j in 0..listSize - 1) {
                if (j != i && j != i2) {
                    if (!fl) {
                        val d = det(vertices[i].x, vertices[i].y,
                                vertices[i2].x, vertices[i2].y,
                                vertices[j].x, vertices[j].y)
                        if (d > 0) {
                            fl = true
                        }
                    }

                    if (j != i3) {
                        val j2 = if (j < listSize - 1) j + 1 else 0
                        val hit = hitSegment(vertices[i].x, vertices[i].y,
                                vertices[i2].x, vertices[i2].y,
                                vertices[j].x, vertices[j].y,
                                vertices[j2].x, vertices[j2].y)
                        if (hit != null) {
                            ret = 1
                        }
                    }
                }
            }

            if (!fl) {
                fl2 = true
            }
        }

        if (fl2) {
            if (ret == 1) {
                ret = 3
            } else {
                ret = 2
            }

        }
        return ret
    }

    private fun calcShapes(verticesVec: List<Vec2>): List<List<Vec2>> {
        val separations = ArrayList<List<Vec2>>()

        val queue = LinkedList<List<Vec2>>()
        queue.add(verticesVec)

        var isConvex: Boolean
        while (!queue.isEmpty()) {
            val list = queue.peek()
            isConvex = true

            val listSize = list.size
            for (i1 in 0..listSize - 1) {
                val i2 = if (i1 < listSize - 1) i1 + 1 else i1 + 1 - listSize
                val i3 = if (i1 < listSize - 2) i1 + 2 else i1 + 2 - listSize

                val p1 = list[i1]
                val p2 = list[i2]
                val p3 = list[i3]

                val result = det(p1.x, p1.y, p2.x, p2.y, p3.x, p3.y)
                if (result < 0) {
                    isConvex = false
                    var minLen = java.lang.Double.MAX_VALUE

                    var j1: Int
                    var j2: Int
                    var h = 0
                    var k = 0

                    var v1: Vec2? = null
                    var v2: Vec2? = null
                    var hitV: Vec2? = null

                    j1 = 0
                    while (j1 < listSize) {
                        if (j1 != i1 && j1 != i2) {
                            j2 = if (j1 < listSize - 1) j1 + 1 else 0

                            v1 = list[j1]
                            v2 = list[j2]

                            val v = hitRay(p1.x, p1.y, p2.x, p2.y, v1.x, v1.y, v2.x, v2.y)

                            if (v != null) {
                                val dx = p2.x - v.x
                                val dy = p2.y - v.y
                                val t = dx * dx + dy * dy

                                if (t < minLen) {
                                    h = j1
                                    k = j2
                                    hitV = v
                                    minLen = t
                                }
                            }
                        }
                        j1++
                    }

                    if (minLen == java.lang.Double.MAX_VALUE) {
                        err()
                    }

                    val vec1 = ArrayList<Vec2>()
                    val vec2 = ArrayList<Vec2>()

                    j1 = h
                    j2 = k

                    if (!pointsMatch(hitV!!.x, hitV.y, v2!!.x, v2.y)) {
                        vec1.add(hitV)
                    }

                    if (!pointsMatch(hitV.x, hitV.y, v1!!.x, v1.y)) {
                        vec2.add(hitV)
                    }

                    h = -1
                    k = i1

                    while (true) {
                        if (k != j2) {
                            vec1.add(list[k])
                        } else {
                            if (h < 0 || h >= listSize) {
                                err()
                            }
                            if (!isOnSegment(v2.x, v2.y, list[h].x, list[h].y, p1.x, p1.y)) {
                                vec1.add(list[k])
                            }
                            break
                        }

                        h = k
                        if (k - 1 < 0) {
                            k = listSize - 1
                        } else {
                            k--
                        }
                    }

                    Collections.reverse(vec1)

                    h = -1
                    k = i2

                    while (true) {
                        if (k != j1) {
                            vec2.add(list[k])
                        } else {
                            if (h < 0 || h >= listSize) {
                                err()
                            }
                            if (k == j1 && !isOnSegment(v1.x, v1.y, list[h].x, list[h].y, p2.x, p2.y)) {
                                vec2.add(list[k])
                            }
                            break
                        }

                        h = k
                        if (k + 1 > listSize - 1) {
                            k = 0
                        } else {
                            k++
                        }
                    }

                    queue.add(vec1)
                    queue.add(vec2)
                    queue.poll()
                    break
                }
            }

            if (isConvex) {
                separations.add(queue.poll())
            }
        }

        return separations
    }

    private fun hitRay(x1: Double, y1: Double, x2: Double, y2: Double, x3: Double, y3: Double, x4: Double, y4: Double): Vec2? {
        val t1 = x3 - x1
        val t2 = y3 - y1
        val t3 = x2 - x1
        val t4 = y2 - y1
        val t5 = x4 - x3
        val t6 = y4 - y3
        val t7 = t4 * t5 - t3 * t6

        val a = (t5 * t2 - t6 * t1) / t7
        val px = x1 + a * t3
        val py = y1 + a * t4

        val b1 = isOnSegment(x2, y2, x1, y1, px, py)
        val b2 = isOnSegment(px, py, x3, y3, x4, y4)

        if (b1 && b2) {
            return Vec2(px, py)
        }
        return null
    }

    private fun hitSegment(x1: Double, y1: Double, x2: Double, y2: Double, x3: Double, y3: Double, x4: Double, y4: Double): Vec2? {
        val t1 = x3 - x1
        val t2 = y3 - y1
        val t3 = x2 - x1
        val t4 = y2 - y1
        val t5 = x4 - x3
        val t6 = y4 - y3
        val t7 = t4 * t5 - t3 * t6

        val a = (t5 * t2 - t6 * t1) / t7
        val px = x1 + a * t3
        val py = y1 + a * t4

        val b1 = isOnSegment(px, py, x1, y1, x2, y2)
        val b2 = isOnSegment(px, py, x3, y3, x4, y4)

        if (b1 && b2) {
            return Vec2(px, py)
        }
        return null
    }

    private fun isOnSegment(px: Double, py: Double, x1: Double, y1: Double, x2: Double, y2: Double): Boolean {
        val b1 = x1 + 0.1 >= px && px >= x2 - 0.1 || x1 - 0.1 <= px && px <= x2 + 0.1
        val b2 = y1 + 0.1 >= py && py >= y2 - 0.1 || y1 - 0.1 <= py && py <= y2 + 0.1
        return b1 && b2 && isOnLine(px, py, x1, y1, x2, y2)
    }

    private fun pointsMatch(x1: Double, y1: Double, x2: Double, y2: Double): Boolean {
        val dx = if (x2 >= x1) x2 - x1 else x1 - x2
        val dy = if (y2 >= y1) y2 - y1 else y1 - y2
        return dx < 0.1 && dy < 0.1
    }

    private fun isOnLine(px: Double, py: Double, x1: Double, y1: Double, x2: Double, y2: Double): Boolean {
        if (x2 - x1 > 0.1 || x1 - x2 > 0.1) {
            val a = (y2 - y1) / (x2 - x1)
            val possibleY = a * (px - x1) + y1
            val diff = if (possibleY > py) possibleY - py else py - possibleY
            return diff < 0.1
        }
        return px - x1 < 0.1 || x1 - px < 0.1
    }

    private fun det(x1: Double, y1: Double, x2: Double, y2: Double, x3: Double, y3: Double): Double {
        return x1 * y2 + x2 * y3 + x3 * y1 - y1 * x2 - y2 * x3 - y3 * x1
    }

    private fun err() {
        throw IllegalArgumentException("A problem has occurred, Use the validate() method to see where the problem is.")
    }

}