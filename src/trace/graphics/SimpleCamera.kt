package trace.graphics

import field.graphics.Camera
import field.linalg.Vec3
import field.utility.Dict
import field.utility.remAssign
import fieldbox.boxes.Box
import fieldbox.io.IO

class SimpleCamera(val camera: Camera) {

    val position = camera.state.position

    val target = camera.state.target

    val up = camera.state.up

    fun setPosition(v : Vec3)
    {
        camera.advanceState {
            val s = it.copy()
            s.position %= v
            s
        }
        updateState()
    }

    fun setTarget(v : Vec3)
    {
        camera.advanceState {
            val s = it.copy()
            s.target %= v
            s
        }
        updateState()
    }
    fun setUp(v : Vec3)
    {
        camera.advanceState {
            val s = it.copy()
            s.up %= v
            s
        }
        updateState()
    }

    fun getState() : Camera.State
    {
        return camera.state.copy()
    }

    fun setState(s : Camera.State)
    {
        camera.advanceState {
            s
        }
        update()
    }


    fun orbitLeft(degrees: Double) {
        camera.advanceState {
            it.orbitLeft((Math.PI * degrees / 180).toFloat())
        }
        updateState()
    }

    private fun updateState() {
        position %= camera.state.position
        target %= camera.state.target
        up %= camera.state.up
    }

    fun orbitRight(degrees: Double) {
        camera.advanceState {
            it.orbitLeft(-(Math.PI * degrees / 180).toFloat())
        }
        updateState()
    }

    fun orbitUp(degrees: Double) {
        camera.advanceState {
            it.orbitUp((Math.PI * degrees / 180).toFloat())
        }
        updateState()
    }

    fun orbitDown(degrees: Double) {
        camera.advanceState {
            it.orbitUp(-(Math.PI * degrees / 180).toFloat())
        }
        updateState()
    }

    fun dollyIn(by: Double) {
        camera.advanceState {
            it.dollyIn(by.toFloat())
        }
        updateState()
    }

    fun dollyOut(by: Double) {
        camera.advanceState {
            it.dollyIn((-by).toFloat())
        }
        updateState()
    }

    fun roll(degrees: Double) {
        camera.advanceState {
            it.roll((Math.PI * degrees / 180).toFloat())
        }
        updateState()
    }

    fun lookLeft(degrees: Double) {
        camera.advanceState {
            it.lookLeft((Math.PI * degrees / 180).toFloat())
        }
        updateState()
    }

    fun lookRight(degrees: Double) {
        camera.advanceState {
            it.lookLeft(-(Math.PI * degrees / 180).toFloat())
        }
        updateState()
    }

    fun translateLeft(d: Double) {
        camera.advanceState {
            it.translateLeft(d.toFloat())
        }
        updateState()
    }

    fun translateRight(d: Double) {
        camera.advanceState {
            it.translateLeft(-d.toFloat())
        }
        updateState()
    }

    fun translateIn(d: Double) {
        camera.advanceState {
            it.translateIn(d.toFloat())
        }
        updateState()
    }

    fun translateOut(d: Double) {
        camera.advanceState {
            it.translateIn(-d.toFloat())
        }
        updateState()
    }

    fun interpolate(amount : Float, s : Camera.State)
    {
        camera.advanceState {
            it.copy().interpolate(amount, s)
        }
        updateState()
    }

    fun interpolate( from : Camera.State, amount : Float, to : Camera.State)
    {
        camera.advanceState {
            from.copy().interpolate(amount, to)
        }
        updateState()
    }


    fun remember(b : Box, name : String) : String
    {
        if (b.properties.has(Dict.Prop<Any>(name))) throw IllegalArgumentException(" you already have a camera state called $name, do you mean to say 'overwrite'? instead?")

        var p = Dict.Prop<Camera.State>(name)
        p = p.toCanon<Camera.State>()
        p.attributes.put(IO.persistent, true)
        b.properties.put(p, getState())

        return "saved ${getState()} to $name"
    }

    fun overwrite(b : Box, name : String) : String
    {
        var p = Dict.Prop<Camera.State>(name)
        p = p.toCanon<Camera.State>()
        p.attributes.put(IO.persistent, true)
        b.properties.put(p, getState())

        return "saved ${getState()} to $name"
    }

    override fun toString(): String {
        return getState().toString()
    }



    fun update() {

        /*
        val s = camera.state
        s.position = position
        s.target = target
        s.up = up
        camera.state = s
        */
    }
}