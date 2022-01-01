package trace.physics

import field.app.RunLoop
import field.graphics.FLine
import field.graphics.StandardFLineDrawing.texCoord
import field.linalg.Vec3
import field.utility.Dict
import field.utility.IdempotencyMap
import field.utility.Rect
import org.jbox2d.callbacks.ContactImpulse
import org.jbox2d.callbacks.ContactListener
import org.jbox2d.collision.Manifold
import org.jbox2d.collision.WorldManifold
import org.jbox2d.collision.shapes.PolygonShape
import org.jbox2d.common.Transform
import org.jbox2d.common.Vec2
import org.jbox2d.dynamics.*
import org.jbox2d.dynamics.contacts.Contact
import org.jbox2d.dynamics.joints.DistanceJointDef
import org.jbox2d.dynamics.joints.Joint
import org.jbox2d.dynamics.joints.WeldJointDef
import java.util.*
import java.util.function.BiConsumer
import java.util.function.Consumer

class PhysicsSystem {

    val physics: Dict.Prop<FLineToPhysics> = Dict.Prop<Any>("physics")
            .toCanon<Any>().set(Dict.domain, "fline")

    val onContactStart: Dict.Prop<IdempotencyMap<BiConsumer<FLine, List<field.linalg.Vec2>?>>> = Dict.Prop<Any>("onContactStart")
            .toCanon<Any>().autoConstructs { IdempotencyMap<BiConsumer<FLine, List<field.linalg.Vec2>?>>(BiConsumer::class.java) }.set(Dict.domain, "fline")

    val onContactEnd: Dict.Prop<IdempotencyMap<Consumer<FLine>>> = Dict.Prop<Any>("onContactEnd")
            .toCanon<Any>().autoConstructs { IdempotencyMap<Consumer<FLine>>(Consumer::class.java) }.set(Dict.domain, "fline")


    val world = World(Vec2(0f, 0f))

    val allPhysicsLines = mutableSetOf<FLineToPhysics>();

    var TICK = 0L;

    class Contact(val tick: Long, val A: FLine, val B: FLine) {
        var terminated = -1L

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Contact) return false

            if (A != other.A) return false
            if (B != other.B) return false

            return true
        }

        override fun hashCode(): Int {
            var result = A.hashCode()
            result = 31 * result + B.hashCode()
            return result
        }

        var points: List<field.linalg.Vec2>? = null
    }

    val contacts = mutableSetOf<Contact>();

    init {
        world.setContactListener(object : ContactListener {
            override fun beginContact(contact: org.jbox2d.dynamics.contacts.Contact) {
                val A = fixturesToFLines[contact.fixtureA]
                val B = fixturesToFLines[contact.fixtureB]
                if (A != null && B != null) {
                    val c = Contact(tick = TICK, A = A, B = B)
                    val wm = WorldManifold()
                    contact.getWorldManifold(wm)
                    c.points = wm.points.map { convert2(it) }

                    contacts.remove(c)
                    contacts.add(c)
                }
            }

            override fun endContact(contact: org.jbox2d.dynamics.contacts.Contact) {
                val A = fixturesToFLines[contact.fixtureA]
                val B = fixturesToFLines[contact.fixtureB]
                if (A != null && B != null) {
                    val c = contacts.filter { it.equals(Contact(-1, A, B)) }.getOrNull(0)
                    if (c != null) {
                        c.terminated = TICK
                    }
                }
            }

            override fun preSolve(p0: org.jbox2d.dynamics.contacts.Contact?, p1: Manifold?) {
            }

            override fun postSolve(p0: org.jbox2d.dynamics.contacts.Contact?, p1: ContactImpulse?) {
            }

        })
    }

    fun update(delta: Float) {
        var timeStep = delta / 1000f
        if (timeStep > 0.2) {
            System.err.println(" warning: large timestep :" + timeStep)
            timeStep = 0.2f
        } else if (timeStep < 1e-8) {
            timeStep = 1 / 30f;
        }

        val velocityIterations = 6
        val positionIterations = 3

        world.step(timeStep, velocityIterations, positionIterations)

        allPhysicsLines.forEach {
            it.physicsToFLine()
        }

        contacts.retainAll {
            if (it.tick == TICK) {
                fire<List<field.linalg.Vec2>>(it.A, onContactStart, it.points)
                fire<List<field.linalg.Vec2>>(it.B, onContactStart, it.points)
            }
            if (it.terminated != -1L) {
                fire(it.A, onContactEnd)
                fire(it.B, onContactEnd)
                false
            }
            true
        }
        TICK++
    }

    private fun fire(b: FLine, prop: Dict.Prop<IdempotencyMap<Consumer<FLine>>>) {
        b.attributes.getOrConstruct(prop).values.forEach {
            it.accept(b)
        }
    }

    private fun <T> fire(b: FLine, prop: Dict.Prop<IdempotencyMap<BiConsumer<FLine, T?>>>, arg: T?) {
        b.attributes.getOrConstruct(prop).values.forEach {
            it.accept(b, arg)
        }
    }

    var lastTime = -1L
    fun update() {
        if (lastTime == -1L) lastTime = System.currentTimeMillis()
        update((System.currentTimeMillis() - lastTime).toFloat())
        lastTime = System.currentTimeMillis()
    }

    fun addPhysics(f: FLine): FLineToPhysics {
        val pp = f.attributes.computeIfAbsent(physics, { FLineToPhysics(f, f.center(), 1.0) })
        allPhysicsLines.add(pp)
        return pp
    }

    fun removePhysics(f: FLine) {
        val pp = f.attributes.getOr(physics, { null })
        if (pp != null) {
            allPhysicsLines.remove(pp)
            pp.remove()
        }
    }

    fun setGravity(g: field.linalg.Vec2) {
        world.gravity = convert(g)
    }


    fun distanceJoint(a: FLine, b: FLine): Joint {
        val pa = addPhysics(a)
        val pb = addPhysics(b)

        val d = DistanceJointDef()
        d.initialize(pa.body, pb.body, convert(field.linalg.Vec2(0.0, 0.0)), convert(field.linalg.Vec2(0.0, 0.0)))
        d.dampingRatio = 0.2f
        d.frequencyHz = 1f
        val w = world.createJoint(d)
        return w
    }

    fun distanceJoint(a: FLine, b: FLine, dampingRatio: Double, frequencyHz: Double): Joint {
        val pa = addPhysics(a)
        val pb = addPhysics(b)

        val d = DistanceJointDef()
        d.initialize(pa.body, pb.body, convert(field.linalg.Vec2(0.0, 0.0)), convert(field.linalg.Vec2(0.0, 0.0)))
        d.dampingRatio = dampingRatio.toFloat()
        d.frequencyHz = frequencyHz.toFloat()
        val w = world.createJoint(d)
        return w
    }

    fun weldJoint(a: FLine, b: FLine, dampingRatio: Double, frequencyHz: Double): Joint {
        val pa = addPhysics(a)
        val pb = addPhysics(b)

        val d = WeldJointDef()
        d.initialize(pa.body, pb.body, convert(field.linalg.Vec2(0.0, 0.0)));
        d.dampingRatio = dampingRatio.toFloat()
        d.frequencyHz = frequencyHz.toFloat()
        val w = world.createJoint(d)
        return w
    }


    private val fixturesToFLines = LinkedHashMap<Fixture, FLine>();

    inner class FLineToPhysics(val fline: FLine, val center: field.linalg.Vec2? = null, density: Double = 1.0) {

        var ff: ArrayList<Fixture>
        var body: Body

        init {
            ff = ArrayList<Fixture>()

            val shape = mutableListOf<field.linalg.Vec2>();

            fline.nodes.forEach {
                if (!shape.contains(it.to.toVec2())) // O(N^2) !!
                    shape.add(it.to.toVec2())
            }

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

            val exploded = fline.pieces()
            exploded.forEach {
                val shape2 = mutableListOf<field.linalg.Vec2>();

                it.nodes.forEach {
                    if (!shape2.contains(it.to.toVec2())) // O(N^2) !!
                        shape2.add(it.to.toVec2())
                }

                val v = ConcaveSeparator.validate(shape2)
                if (v == 2 || v == 3)
                    Collections.reverse(shape)

                val groups = ConcaveSeparator.separate(shape2)

                val fd = FixtureDef()
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
                    val fixture = body.createFixture(fd)
                    fixturesToFLines[fixture] = fline
                    ff.add(fixture)
                }
            }

        }

        var removed = false
        fun remove() {
            if (removed) return

            world.destroyBody(body)
            removed = true
        }

        fun setFixed() {
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

        fun setRestitution(r: Double) {
            ff.forEach { it.restitution = r.toFloat() }
        }

        fun setFriction(r: Double) {
            ff.forEach { it.friction = r.toFloat() }
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

        fun decayVelocity(by: Double, limit: Double) {
            val v = convert2(body.linearVelocity)
            if (v.length() > 0.001)
                body.applyForceToCenter(convert(field.linalg.Vec2(-v.x, -v.y).mul(by)))
            else
                body.linearVelocity = convert(field.linalg.Vec2(0.0, 0.0))
        }

        fun decayAngularVelocity(forceLength: Float, limit: Double) {
            val v = body.angularVelocity
            if (Math.abs(v) > 0.001)
                body.applyTorque(-v * forceLength)
            else
                body.angularVelocity = 0f
        }

        fun getRotation() = body.transform.q.angle

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

        var textureRect: Rect? = null


        fun physicsToFLine() {
            if (removed) return

            var f: Fixture? = body.fixtureList
            val t = body.transform
            val vo = org.jbox2d.common.Vec2()

            fline.clear()

            var r: Rect? = null

            while (f != null) {
                val sh = f.shape
                if (sh is PolygonShape) {
                    val v = sh.vertices
                    val vcount = sh.vertexCount
                    for (ii in 0..vcount - 1) {
                        r = Rect.union(r, Rect(v[ii].x, v[ii].y, 0f, 0f))
                    }
                }
                f = f.next
            }

            f = body.fixtureList

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

                        if (textureRect != null) {
                            val tx = textureRect!!.x + textureRect!!.w * (v[ii].x - r!!.x) / r!!.w;
                            val ty = textureRect!!.y + textureRect!!.h * (v[ii].y - r!!.y) / r!!.h;
                            fline.node().attributes.put(texCoord, field.linalg.Vec2(tx.toDouble(), ty.toDouble()))
                        }

                    }
                    if (v.size > 0) {
                        Transform.mulToOut(t, v[0], vo)
                        fline.lineTo(convert2(vo))
                        if (textureRect != null) {
                            val tx = textureRect!!.x + textureRect!!.w * (v[0].x - r!!.x) / r!!.w;
                            val ty = textureRect!!.y + textureRect!!.h * (v[0].y - r!!.y) / r!!.h;
                            fline.node().attributes.put(texCoord, field.linalg.Vec2(tx.toDouble(), ty.toDouble()))
                        }

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