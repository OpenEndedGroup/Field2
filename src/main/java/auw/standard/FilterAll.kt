package auw.standard

import auw._FBuffer
import auw.signal.Filter_svf

class FilterAll
{
    var a = Filter_svf()

    fun apply(input: _FBuffer, freq: Float, resonance: Float) : _FBuffer
    {
        return a.apply_all(input, freq, resonance)
    }
}