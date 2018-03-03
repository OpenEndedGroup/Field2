package trace.graphics

import field.graphics.Camera
import field.utility.remAssign

class SimpleCamera(val camera: Camera) {

    @JvmField
    val position = camera.state.position

    @JvmField
    val target = camera.state.target

    @JvmField
    val up = camera.state.up

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

    fun update() {

        val s = camera.state
        s.position = position
        s.target = target
        s.up = up
        camera.state = s
    }
}