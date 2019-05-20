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

    var slop = 0.01f

    constructor(init : (Buttons) -> Unit)
    {
        init(this)
    }

    override fun run() {

        val currentDown = axesMap.entries.filter { abs(it.value.toDouble()) > slop }.map { it.key }.toSet()

        if (currentDown.size>0)
            println("current : "+currentDown)

        val downEvents = LinkedHashSet<String>(currentDown)
        downEvents.removeAll(lastDown)
        downEvents.map { down[it] }.filter { it != null }.map { it!!.values }.forEach {
            it.removeIf { v -> !v.get() }
        }

        if (downEvents.size>0)
            println("down :"+downEvents)

        val upEvents = LinkedHashSet<String>(lastDown)
        upEvents.removeAll(currentDown)
        upEvents.map { up[it] }.filter { it != null }.map { it!!.values }.forEach {
            it.removeIf { v -> !v.get() }
        }

        if (upEvents.size>0)
            println("up :"+upEvents)

        lastDown.clear()
        lastDown.addAll(currentDown);

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
