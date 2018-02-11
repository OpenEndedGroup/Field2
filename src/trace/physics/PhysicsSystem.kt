package trace.physics

import field.graphics.FLine
import field.linalg.Vec3
import field.utility.Dict
import org.jbox2d.collision.shapes.PolygonShape
import org.jbox2d.common.Transform
import org.jbox2d.common.Vec2
import org.jbox2d.dynamics.*
import java.util.*

class FLinesAndPhysics {

    val physics: Dict.Prop<FLineToPhysics> = Dict.Prop<Any>("physics")
            .toCanon<Any>().set(Dict.domain, "fline")

    val world = World(Vec2(0f, 0f))

    val allPhysicsLines = mutableSetOf<FLineToPhysics>();

    fun update(delta: Float) {
        var timeStep = delta / 1000f
        if (timeStep > 0.2) {
            System.err.println(" warning: large timestep :" + timeStep)
            timeStep = 0.2f
        }

        val velocityIterations = 6
        val positionIterations = 3

        world.step(timeStep, velocityIterations, positionIterations)

        allPhysicsLines.forEach {
            it.physicsToFLine()
        }
    }

    fun addPhysics(f: FLine) {
        allPhysicsLines.add(f.attributes.computeIfAbsent(physics, { FLineToPhysics(f, f.center(), 1.0) }))
    }

    fun setGravity(g: field.linalg.Vec2) {
        world.gravity = convert(g)
    }

    inner class FLineToPhysics(val fline: FLine, val center: field.linalg.Vec2? = null, density: Double = 1.0) {

        var ff: ArrayList<Fixture>
        var body: Body

        init {

            val shape = mutableListOf<field.linalg.Vec2>();
            fline.nodes.forEach {
                if (!shape.contains(it.to.toVec2())) // O(N^2) !!
                    shape.add(it.to.toVec2())
            }

            val v = ConcaveSeparator.validate(shape)
            if (v == 2 || v == 3)
                Collections.reverse(shape)

            val middle = field.linalg.Vec2()
            for (s in shape)
                middle.add(s)
            middle.mul((1f / shape.size).toDouble())

            val bd = BodyDef()
            bd.position = convert(center ?: fline.center())
            bd.angle = 0f;
            bd.type = BodyType.DYNAMIC
            body = world.createBody(bd)
            body.isBullet = true

            val groups = ConcaveSeparator.separate(shape)

            val fd = FixtureDef()
            ff = ArrayList<Fixture>()
            for (g in groups) {
                val sd = PolygonShape()

                val vv = arrayOfNulls<org.jbox2d.common.Vec2>(g.size)
                for (i in g.indices) {
                    vv[i] = convert(g[i])
                    vv[i]!!.x -= middle.x.toFloat()
                    vv[i]!!.y -= middle.y.toFloat()
                }

                sd.set(vv, vv.size)
                fd.shape = sd
                fd.restitution = 1f
                fd.density = density.toFloat() / 100000
                ff.add(body.createFixture(fd))
            }
        }

        fun setFixed()
        {
            body.type = BodyType.STATIC
        }

        fun setCenter(v: field.linalg.Vec2, angle: Float) {
            if (v.distance(convert2(body.position)) > 1e-9 || Math.abs(angle - body.angle) > 1e-9) {
                body.setAwake(true)
                body.setTransform(convert(v), angle)
            }
        }

        fun setCenter(v: field.linalg.Vec2) {
            if (v.distance(convert2(body.position)) > 1e-9) {
                body.setAwake(true)
                body.setTransform(convert(v), body.angle)
            }
        }

        fun applyForce(force: field.linalg.Vec2) {
            body.applyForceToCenter(convert(force))
        }

        fun limitVelocity(max: Float, forceLength: Float) {
            val v = convert2(body.linearVelocity)
            if (v.length() < max) return
            val excess = (v.length() - max) * forceLength
            body.applyForceToCenter(convert(field.linalg.Vec2(-v.x, -v.y).normalize().mul(excess)))
        }

        fun limitAngularVelocity(max: Float, forceLength: Float) {
            val v = body.angularVelocity
            if (Math.abs(v) < max) return
            val excess = ((Math.abs(v) - max) * forceLength).toDouble()
            body.applyTorque((-Math.signum(v) * excess).toFloat())
        }

        fun applyTorque(rotation: Float) {
            body.applyTorque(rotation)
        }

        fun setLinearVelocity(v: field.linalg.Vec2) {
            body.linearVelocity = convert(v)
        }

        fun setAngularVelocity(d: Double) {
            body.angularVelocity = d.toFloat()
        }

        fun getLinearVelocity(v: field.linalg.Vec2): field.linalg.Vec2 {
            return convert2(body.linearVelocity)
        }

        fun setDensity(d: Double) {
            var f = body.fixtureList
            while (f != null) {
                f.density = d.toFloat()
                f = f.next
            }
            body.resetMassData()
        }

        fun physicsToFLine() {
            var f: Fixture? = body.fixtureList
            val t = body.transform
            val vo = org.jbox2d.common.Vec2()

            fline.clear()

            while (f != null) {
                val sh = f.shape
                if (sh is PolygonShape) {
                    val v = sh.vertices
                    val vcount = sh.vertexCount
                    for (ii in 0..vcount - 1) {
                        Transform.mulToOut(t, v[ii], vo)
                        if (ii == 0)
                            fline.moveTo(convert2(vo))
                        else
                            fline.lineTo(convert2(vo))

                    }
                    if (v.size > 0) {
                        Transform.mulToOut(t, v[0], vo)
                        fline.lineTo(convert2(vo))
                    }
                }
                f = f.next
            }
        }

    }

    private fun convert(center: field.linalg.Vec2): org.jbox2d.common.Vec2 =
            org.jbox2d.common.Vec2(center.x.toFloat(), center.y.toFloat())

    private fun convert2(center: org.jbox2d.common.Vec2): field.linalg.Vec2 =
            field.linalg.Vec2(center.x.toDouble(), center.y.toDouble())

}