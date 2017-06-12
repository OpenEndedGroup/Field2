package field.graphics.vr

import field.utility.IdempotencyMap
import java.lang.Math.abs
import java.util.ArrayList
import java.util.function.Supplier

class Buttons : Runnable {

    // a button is just an axis
    val axesMap: MutableMap<String, Number> = mutableMapOf();

    // callbacks
    val down: MutableMap<String, IdempotencyMap<Supplier<Boolean>>> = mutableMapOf()
    val up: MutableMap<String, IdempotencyMap<Supplier<Boolean>>> = mutableMapOf()

    private val lastDown: MutableSet<String> = mutableSetOf()

    var slop = 0.01f;

    constructor()
    {

    }

    constructor(init : (Buttons) -> Unit)
    {
        init(this)
    }

    override fun run() {

        val currentDown = axesMap.entries.filter { abs(it.value.toDouble()) > slop }.map { it.key }.toSet()

        val downEvents = LinkedHashSet<String>(currentDown)
        downEvents.removeAll(lastDown)
        downEvents.map { down[it] }.filter { it != null }.flatMap { it!!.values }.forEach { it.get() }

        val upEvents = LinkedHashSet<String>(lastDown)
        upEvents.removeAll(currentDown)
        upEvents.map { up[it] }.filter { it != null }.flatMap { it!!.values }.forEach { it.get() }

    }

    fun setAxis(name: String, value: Number) {
        axesMap[name] = value
    }

    fun addAxis(vararg  name: String)
    {
        for (n in name) {
            axesMap[n] = 0f;
            down[n] = IdempotencyMap(Supplier::class.java)
            up[n] = IdempotencyMap(Supplier::class.java)
        }
    }

}
