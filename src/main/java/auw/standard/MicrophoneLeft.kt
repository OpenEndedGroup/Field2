package auw.standard

import auw.BoxTools
import auw.FBuffer
import auw.IO

class MicrophoneLeft {
    fun apply(): FBuffer {
        val v = BoxTools.stack.get().allocate()
        v.a.put(IO.thisInputSampleLeft)
        v.a.clear()
        IO.thisInputSampleLeft.clear()

        return v
    }
}