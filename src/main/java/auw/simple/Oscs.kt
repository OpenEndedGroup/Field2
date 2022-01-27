package auw.simple

import auw.*
import java.util.*
import kotlin.collections.HashMap

class Oscs() : _FBuffer {

    init {
        default_oscs = this
    }

    companion object {
        var default_oscs: Oscs? = null
    }

    val internalBuffer = BoxTools.stack.get().allocate()

    override fun get(): FBuffer {

        BufferTools.zero(internalBuffer)

        val r = Collections.newSetFromMap(HashMap<Soundable, Boolean>())
        synchronized(known) {
            known.forEach {
                BufferTools.add(internalBuffer, it.get(), internalBuffer)
                if (it.sounding())
                    r.add(it)
            }
        }

        retained = r

        return internalBuffer
    }

    val known = Collections.newSetFromMap(WeakHashMap<Soundable, Boolean>())
    var retained = Collections.newSetFromMap(HashMap<Soundable, Boolean>())

    override fun source(): Any {
        return this
    }

}