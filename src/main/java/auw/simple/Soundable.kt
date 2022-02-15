package auw.simple

import auw._FBuffer

interface Soundable : _FBuffer
{
    fun sounding() : Boolean

    override fun source(): Any {
        return this
    }
}
