package auw.functional

import auw.FBuffer
import auw._FBuffer


class Functions
{
    interface F1
    {
        fun apply(f : Float) : _FBuffer
    }
    interface F2
    {
        fun apply(f : Float, f2 : Float) : _FBuffer
    }
    interface F3
    {
        fun apply(f : Float, f2 : Float, f3 : Float) : _FBuffer
    }
}