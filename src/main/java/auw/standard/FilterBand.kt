package auw.standard

import auw._FBuffer
import auw.signal.Filter_svf

class FilterBand
{
    var a = Filter_svf()

    fun apply(input: _FBuffer, freq: Float, resonance: Float) : _FBuffer
    {
        return a.apply_band(input, freq, resonance)
    }
}