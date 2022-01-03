package trace.graphics

import field.graphics.vr.OpenVRDrawTarget
import field.utility.IdempotencyMap
import java.util.function.Supplier

class SimpleButtons(o : OpenVRDrawTarget) : Runnable
{

    val triggerDown = IdempotencyMap<Runnable>(Runnable::class.java)
    val triggerUp = IdempotencyMap<Runnable>(Runnable::class.java)

    init {
        o.buttons.down["button33_right"]!!["__simplebuttons__"] =  Supplier<Boolean> {
            triggerDown.values.forEach { it.run() }
            true
        }
        o.buttons.up["button33_right"]!!["__simplebuttons__"] = Supplier<Boolean>{
            triggerUp.values.forEach { it.run() }
            true
        }
        o.buttons.down["button33_left"]!!["__simplebuttons__"] =Supplier<Boolean> {
            triggerDown.values.forEach { it.run() }
            true
        }
        o.buttons.up["button33_left"]!!["__simplebuttons__"] =Supplier<Boolean> {
            triggerUp.values.forEach { it.run() }
            true
        }
    }

    override fun run() {

    }



}