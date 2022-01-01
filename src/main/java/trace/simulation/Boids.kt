package trace.simulation


import field.linalg.Transform2D
import field.linalg.Vec2
import field.utility.*

class Boids {
    inner class Boid(val at: Vec2, val heading: Vec2, val speed: Double = 1.0, val misc: Dict = Dict()) {
        fun rotation(): Transform2D = Transform2D.rotate(Math.atan2(heading.y, heading.x) * 180 / Math.PI)
    }

    var all = mutableListOf<Boid>()
    var important = mutableSetOf<Boid>()

    fun update(scale: Double) {
        val next = mutableListOf<Boid>()

        ArrayList(all).forEach {

            val n = propagate(it, all, scale)
            if (n != null) {
                next.add(n)
            }
        }

        all = next
        important.clear()

    }

    fun addBoid(at: Vec2, heading: Vec2) {
        all.add(Boid(at, heading.safeNormalize()))
    }

    fun addBoid(at: Vec2, heading: Vec2, speed: Double) {
        all.add(Boid(at, heading.safeNormalize(), speed))
    }

    fun headBoid(index: Int, at: Vec2, heading: Vec2) {
        all[index].at %= at
        all[index].heading %= heading.safeNormalize()
        important.add(all[index])
    }

    var tooClose = 5.0
    var tooFar = 20.0
    var W = 1.0

    private fun propagate(a: Boid, all: MutableList<Boid>, speed: Double): Boid {

        val all2 = mutableListOf<Boid>()
        all2.addAll(all)

        all2.sortBy { it.at.distanceSquared(a.at) }
        all.sortBy { it.at.distanceSquared(a.at) }

        all2.addAll(0, important)
        val n = Math.min(7 + important.size, all2.size - 1)

        val center = Vec2(a.at)
        val heading = Vec2(a.heading) * W // LPF

        for (x in 1 until n) {
            center %= center + all2[x].at
            heading %= heading + all2[x].heading
        }

        center %= center * (1.0 / (1.0 + n))
        heading %= heading * (1.0 / (W + n))

        if (heading.length() == 0.0) heading %= Vec2(0.0, 0.0).noise(1.0F).safeNormalize()
        else heading.normalize()

        // other behaviors

        if (all.size > 1) {
            tweakHeading(heading, a, all[1], center, tooFar)
            for(i in important.size-1 downTo 0)
            {
                tweakHeading(heading, a, all2[i], all2[i].at, tooFar*5)
            }
        }
        return Boid(a.at + heading * a.speed * speed, heading, a.speed, a.misc)
    }

    private fun tweakHeading(heading: Vec2, a: Boid, all: Boid, center : Vec2, tooFar : Double) {
        val Z = all.at.distance(a.at)
        if (Z > 0) {
            if (Z < tooClose) {
                heading %= (heading + (a.at - all.at).safeNormalize() * ((tooClose - Z) / tooClose)).safeNormalize()
            }

            val Z2 = center.distance(a.at)
            if (Z2 > tooFar) {
                heading %= (heading + (center - a.at).safeNormalize() * ((Z2 - tooFar) / tooFar)).safeNormalize()
            }
        }

    }


    fun center(): Vec2 {
        val v = Vec2()
        all.forEach { v %= v + it.at }

        return v * (1.0 / all.size)
    }

}

private fun Vec2.safeNormalize(): Vec2 {

    while (this.length() == 0.0)
        this.noise(1.0f).safeNormalize()
    return this.normalize()

}
