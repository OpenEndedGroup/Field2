package auw.standard

import auw._FBuffer
import auw.signal.Filter_svf
import field.utility.Documentation


@Documentation("`FilterLow`(input, frequency, resonance)")
class FilterLow
{
    var a = Filter_svf()

    @JvmOverloads
    fun apply(input: _FBuffer, freq: Float = 440f, resonance: Float = 0.5f) : _FBuffer
    {
        return a.apply_low(input, freq, resonance)
    }
}