package auw

import auw.simple.Oscs

class Submixer : _FBuffer {
    override fun source(): Any {
        return this
    }

    val always = mutableListOf<_FBuffer>()

    val oscs = Oscs()
    init {
        always.add(oscs)
    }

    override fun get(): FBuffer {

        val accumulator = BoxTools.stack.get().allocate()
        BufferTools.zero(accumulator)

        always.forEach {
            BufferTools.add(accumulator, it.get(), accumulator)
        }

        return accumulator

    }
}