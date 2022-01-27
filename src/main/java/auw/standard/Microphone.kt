package auw.standard

import auw.BoxTools
import auw.FBuffer
import auw.IO

class Microphone {
    fun apply(): FBuffer {
        val v = BoxTools.stack.get().allocate()
        v.a.put(IO.thisInputSample)
        v.a.clear()
        IO.thisInputSample.clear()

        return v
    }
}