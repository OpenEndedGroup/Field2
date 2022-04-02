package auw.signal

import auw.*


class Filter_svf {

    val cutoff = FInterpolator()
    val q = FInterpolator()

    var ic2eq = 0.0
    var ic1eq = 0.0

    fun apply_low(input: _FBuffer, cutoff: Float, res: Float): _FBuffer {
        return FBufferSource(this) {

            print(ic2eq)

            var inp = input.get()

            var _cutoff = this.cutoff.next(cutoff.toDouble())
            var _q = this.q.next(res.toDouble())
            val output = BoxTools.stack.get().allocate()

            var g = Math.tan(_cutoff.apply(0.0) / IO.sampleRate)
            var k = 2 - 2 * _q.apply(0.0)
            var a1 = 1 / (1 + g * (g + k))
            var a2 = g * a1;
            var a3 = g * a2;

            if (this.cutoff.isStatic() && this.q.isStatic()) {

                for (i in 0 until inp.length) {
                    var v0 = inp.a.get(i)

                    var v3 = v0 - ic2eq;
                    var v1 = a1 * ic1eq + a2 * v3;
                    var v2 = ic2eq + a2 * ic1eq + a3 * v3;
                    ic1eq = 2 * v1 - ic1eq;
                    ic2eq = 2 * v2 - ic2eq;


                    var low = v2;
                    /*
                    var band = v1;
                    var high = v0 - k * v1 - v2;
                    var notch = low + high
                    var peak = low - high
                    var all = low + high - k*band
*/
                    output.a.put(i, low.toFloat())
                }
            }
            else
                if (this.cutoff.isStatic()) {

                    for (i in 0 until inp.length) {
                        var v0 = inp.a.get(i)

                        k = 2 - 2 * _q.apply(0.0)
                        a1 = 1 / (1 + g * (g + k))
                        a2 = g * a1;
                        a3 = g * a2;


                        var v3 = v0 - ic2eq;
                        var v1 = a1 * ic1eq + a2 * v3;
                        var v2 = ic2eq + a2 * ic1eq + a3 * v3;
                        ic1eq = 2 * v1 - ic1eq;
                        ic2eq = 2 * v2 - ic2eq;


                        var low = v2;
                        /*
                        var band = v1;
                        var high = v0 - k * v1 - v2;
                        var notch = low + high
                        var peak = low - high
                        var all = low + high - k*band
    */
                        output.a.put(i, low.toFloat())
                    }
                }
                else {

                    for (i in 0 until inp.length) {
                        var v0 = inp.a.get(i)

                        g = Math.tan(_cutoff.apply(0.0) / IO.sampleRate)
                        k = 2 - 2 * _q.apply(0.0)
                        a1 = 1 / (1 + g * (g + k))
                        a2 = g * a1;
                        a3 = g * a2;


                        var v3 = v0 - ic2eq;
                        var v1 = a1 * ic1eq + a2 * v3;
                        var v2 = ic2eq + a2 * ic1eq + a3 * v3;
                        ic1eq = 2 * v1 - ic1eq;
                        ic2eq = 2 * v2 - ic2eq;


                        var low = v2;
                        /*
                        var band = v1;
                        var high = v0 - k * v1 - v2;
                        var notch = low + high
                        var peak = low - high
                        var all = low + high - k*band
    */
                        output.a.put(i, low.toFloat())
                    }
                }


            if (ic1eq.isNaN()) ic1eq = 0.0
            if (ic2eq.isNaN()) ic2eq = 0.0


            output
        }
    }

    fun apply_band(input: _FBuffer, cutoff: Float, res: Float): _FBuffer {
        return FBufferSource(this) {

            var inp = input.get()

            var _cutoff = this.cutoff.next(cutoff.toDouble())
            var _q = this.q.next(res.toDouble())
            val output = BoxTools.stack.get().allocate()

            var g = Math.tan(_cutoff.apply(0.0) / IO.sampleRate)
            var k = 2 - 2 * _q.apply(0.0)
            var a1 = 1 / (1 + g * (g + k))
            var a2 = g * a1;
            var a3 = g * a2;

            if (this.cutoff.isStatic() && this.q.isStatic()) {

                for (i in 0 until inp.length) {
                    var v0 = inp.a.get(i)

                    var v3 = v0 - ic2eq;
                    var v1 = a1 * ic1eq + a2 * v3;
                    var v2 = ic2eq + a2 * ic1eq + a3 * v3;
                    ic1eq = 2 * v1 - ic1eq;
                    ic2eq = 2 * v2 - ic2eq;


//                    var low = v2;
                    var band = v1;
//                    var high = v0 - k * v1 - v2;
//                    var notch = low + high
//                    var peak = low - high
//                    var all = low + high - k*band
                    output.a.put(i, band.toFloat())
                }
            }
            else
                if (this.cutoff.isStatic()) {

                    for (i in 0 until inp.length) {
                        var v0 = inp.a.get(i)

                        k = 2 - 2 * _q.apply(0.0)
                        a1 = 1 / (1 + g * (g + k))
                        a2 = g * a1;
                        a3 = g * a2;


                        var v3 = v0 - ic2eq;
                        var v1 = a1 * ic1eq + a2 * v3;
                        var v2 = ic2eq + a2 * ic1eq + a3 * v3;
                        ic1eq = 2 * v1 - ic1eq;
                        ic2eq = 2 * v2 - ic2eq;


//                        var low = v2;
                        var band = v1;
//                        var high = v0 - k * v1 - v2;
//                        var notch = low + high
//                        var peak = low - high
//                        var all = low + high - k*band
                        output.a.put(i, band.toFloat())
                    }
                }
                else {

                    for (i in 0 until inp.length) {
                        var v0 = inp.a.get(i)

                        g = Math.tan(_cutoff.apply(0.0) / IO.sampleRate)
                        k = 2 - 2 * _q.apply(0.0)
                        a1 = 1 / (1 + g * (g + k))
                        a2 = g * a1;
                        a3 = g * a2;


                        var v3 = v0 - ic2eq;
                        var v1 = a1 * ic1eq + a2 * v3;
                        var v2 = ic2eq + a2 * ic1eq + a3 * v3;
                        ic1eq = 2 * v1 - ic1eq;
                        ic2eq = 2 * v2 - ic2eq;


//                        var low = v2;

                        var band = v1;
//                        var high = v0 - k * v1 - v2;
//                        var notch = low + high
//                        var peak = low - high
//                        var all = low + high - k*band

                        output.a.put(i, band.toFloat())
                    }
                }


            if (ic1eq.isNaN()) ic1eq = 0.0
            if (ic2eq.isNaN()) ic2eq = 0.0


            output
        }
    }

    fun apply_high(input: _FBuffer, cutoff: Float, res: Float): _FBuffer {
        return FBufferSource(this) {

            var inp = input.get()

            var _cutoff = this.cutoff.next(cutoff.toDouble())
            var _q = this.q.next(res.toDouble())
            val output = BoxTools.stack.get().allocate()

            var g = Math.tan(_cutoff.apply(0.0) / IO.sampleRate)
            var k = 2 - 2 * _q.apply(0.0)
            var a1 = 1 / (1 + g * (g + k))
            var a2 = g * a1;
            var a3 = g * a2;

            if (this.cutoff.isStatic() && this.q.isStatic()) {

                for (i in 0 until inp.length) {
                    var v0 = inp.a.get(i)

                    var v3 = v0 - ic2eq;
                    var v1 = a1 * ic1eq + a2 * v3;
                    var v2 = ic2eq + a2 * ic1eq + a3 * v3;
                    ic1eq = 2 * v1 - ic1eq;
                    ic2eq = 2 * v2 - ic2eq;


//                    var low = v2;
//                    var band = v1;
                    var high = v0 - k * v1 - v2;
//                    var notch = low + high
//                    var peak = low - high
//                    var all = low + high - k*band
                    output.a.put(i, high.toFloat())
                }
            }
            else
                if (this.cutoff.isStatic()) {

                    for (i in 0 until inp.length) {
                        var v0 = inp.a.get(i)

                        k = 2 - 2 * _q.apply(0.0)
                        a1 = 1 / (1 + g * (g + k))
                        a2 = g * a1;
                        a3 = g * a2;


                        var v3 = v0 - ic2eq;
                        var v1 = a1 * ic1eq + a2 * v3;
                        var v2 = ic2eq + a2 * ic1eq + a3 * v3;
                        ic1eq = 2 * v1 - ic1eq;
                        ic2eq = 2 * v2 - ic2eq;


//                        var low = v2;
//                        var band = v1;
                        var high = v0 - k * v1 - v2;
//                        var notch = low + high
//                        var peak = low - high
//                        var all = low + high - k*band
                        output.a.put(i, high.toFloat())
                    }
                }
                else {

                    for (i in 0 until inp.length) {
                        var v0 = inp.a.get(i)

                        g = Math.tan(_cutoff.apply(0.0) / IO.sampleRate)
                        k = 2 - 2 * _q.apply(0.0)
                        a1 = 1 / (1 + g * (g + k))
                        a2 = g * a1;
                        a3 = g * a2;


                        var v3 = v0 - ic2eq;
                        var v1 = a1 * ic1eq + a2 * v3;
                        var v2 = ic2eq + a2 * ic1eq + a3 * v3;
                        ic1eq = 2 * v1 - ic1eq;
                        ic2eq = 2 * v2 - ic2eq;


//                        var low = v2;

//                        var band = v1;
                        var high = v0 - k * v1 - v2;
//                        var notch = low + high
//                        var peak = low - high
//                        var all = low + high - k*band

                        output.a.put(i, high.toFloat())
                    }
                }


            if (ic1eq.isNaN()) ic1eq = 0.0
            if (ic2eq.isNaN()) ic2eq = 0.0


            output
        }
    }

    fun apply_notch(input: _FBuffer, cutoff: Float, res: Float): _FBuffer {
        return FBufferSource(this) {

            var inp = input.get()

            var _cutoff = this.cutoff.next(cutoff.toDouble())
            var _q = this.q.next(res.toDouble())
            val output = BoxTools.stack.get().allocate()

            var g = Math.tan(_cutoff.apply(0.0) / IO.sampleRate)
            var k = 2 - 2 * _q.apply(0.0)
            var a1 = 1 / (1 + g * (g + k))
            var a2 = g * a1;
            var a3 = g * a2;

            if (this.cutoff.isStatic() && this.q.isStatic()) {

                for (i in 0 until inp.length) {
                    var v0 = inp.a.get(i)

                    var v3 = v0 - ic2eq;
                    var v1 = a1 * ic1eq + a2 * v3;
                    var v2 = ic2eq + a2 * ic1eq + a3 * v3;
                    ic1eq = 2 * v1 - ic1eq;
                    ic2eq = 2 * v2 - ic2eq;


                    var low = v2;
//                    var band = v1;
                    var high = v0 - k * v1 - v2;
                    var notch = low + high
//                    var peak = low - high
//                    var all = low + high - k*band
                    output.a.put(i, notch.toFloat())
                }
            }
            else
                if (this.cutoff.isStatic()) {

                    for (i in 0 until inp.length) {
                        var v0 = inp.a.get(i)

                        k = 2 - 2 * _q.apply(0.0)
                        a1 = 1 / (1 + g * (g + k))
                        a2 = g * a1;
                        a3 = g * a2;


                        var v3 = v0 - ic2eq;
                        var v1 = a1 * ic1eq + a2 * v3;
                        var v2 = ic2eq + a2 * ic1eq + a3 * v3;
                        ic1eq = 2 * v1 - ic1eq;
                        ic2eq = 2 * v2 - ic2eq;


                        var low = v2;
//                        var band = v1;
                        var high = v0 - k * v1 - v2;
                        var notch = low + high
//                        var peak = low - high
//                        var all = low + high - k*band
                        output.a.put(i, notch.toFloat())
                    }
                }
                else {

                    for (i in 0 until inp.length) {
                        var v0 = inp.a.get(i)

                        g = Math.tan(_cutoff.apply(0.0) / IO.sampleRate)
                        k = 2 - 2 * _q.apply(0.0)
                        a1 = 1 / (1 + g * (g + k))
                        a2 = g * a1;
                        a3 = g * a2;


                        var v3 = v0 - ic2eq;
                        var v1 = a1 * ic1eq + a2 * v3;
                        var v2 = ic2eq + a2 * ic1eq + a3 * v3;
                        ic1eq = 2 * v1 - ic1eq;
                        ic2eq = 2 * v2 - ic2eq;


                        var low = v2;

//                        var band = v1;
                        var high = v0 - k * v1 - v2;
                        var notch = low + high
//                        var peak = low - high
//                        var all = low + high - k*band

                        output.a.put(i, notch.toFloat())
                    }
                }


            if (ic1eq.isNaN()) ic1eq = 0.0
            if (ic2eq.isNaN()) ic2eq = 0.0


            output
        }
    }

    fun apply_peak(input: _FBuffer, cutoff: Float, res: Float): _FBuffer {
        return FBufferSource(this) {

            var inp = input.get()

            var _cutoff = this.cutoff.next(cutoff.toDouble())
            var _q = this.q.next(res.toDouble())
            val output = BoxTools.stack.get().allocate()

            var g = Math.tan(_cutoff.apply(0.0) / IO.sampleRate)
            var k = 2 - 2 * _q.apply(0.0)
            var a1 = 1 / (1 + g * (g + k))
            var a2 = g * a1;
            var a3 = g * a2;

            if (this.cutoff.isStatic() && this.q.isStatic()) {

                for (i in 0 until inp.length) {
                    var v0 = inp.a.get(i)

                    var v3 = v0 - ic2eq;
                    var v1 = a1 * ic1eq + a2 * v3;
                    var v2 = ic2eq + a2 * ic1eq + a3 * v3;
                    ic1eq = 2 * v1 - ic1eq;
                    ic2eq = 2 * v2 - ic2eq;


                    var low = v2;
//                    var band = v1;
                    var high = v0 - k * v1 - v2;
//                    var notch = low + high
                    var peak = low - high
//                    var all = low + high - k*band
                    output.a.put(i, peak.toFloat())
                }
            }
            else
                if (this.cutoff.isStatic()) {

                    for (i in 0 until inp.length) {
                        var v0 = inp.a.get(i)

                        k = 2 - 2 * _q.apply(0.0)
                        a1 = 1 / (1 + g * (g + k))
                        a2 = g * a1;
                        a3 = g * a2;


                        var v3 = v0 - ic2eq;
                        var v1 = a1 * ic1eq + a2 * v3;
                        var v2 = ic2eq + a2 * ic1eq + a3 * v3;
                        ic1eq = 2 * v1 - ic1eq;
                        ic2eq = 2 * v2 - ic2eq;


                        var low = v2;
//                        var band = v1;
                        var high = v0 - k * v1 - v2;
//                        var notch = low + high
                        var peak = low - high
//                        var all = low + high - k*band
                        output.a.put(i, peak.toFloat())
                    }
                }
                else {

                    for (i in 0 until inp.length) {
                        var v0 = inp.a.get(i)

                        g = Math.tan(_cutoff.apply(0.0) / IO.sampleRate)
                        k = 2 - 2 * _q.apply(0.0)
                        a1 = 1 / (1 + g * (g + k))
                        a2 = g * a1;
                        a3 = g * a2;


                        var v3 = v0 - ic2eq;
                        var v1 = a1 * ic1eq + a2 * v3;
                        var v2 = ic2eq + a2 * ic1eq + a3 * v3;
                        ic1eq = 2 * v1 - ic1eq;
                        ic2eq = 2 * v2 - ic2eq;


                        var low = v2;

//                        var band = v1;
                        var high = v0 - k * v1 - v2;
//                        var notch = low + high
                        var peak = low - high
//                        var all = low + high - k*band

                        output.a.put(i, peak.toFloat())
                    }
                }


            if (ic1eq.isNaN()) ic1eq = 0.0
            if (ic2eq.isNaN()) ic2eq = 0.0


            output
        }
    }

    fun apply_all(input: _FBuffer, cutoff: Float, res: Float): _FBuffer {
        return FBufferSource(this) {

            var inp = input.get()

            var _cutoff = this.cutoff.next(cutoff.toDouble())
            var _q = this.q.next(res.toDouble())
            val output = BoxTools.stack.get().allocate()

            var g = Math.tan(_cutoff.apply(0.0) / IO.sampleRate)
            var k = 2 - 2 * _q.apply(0.0)
            var a1 = 1 / (1 + g * (g + k))
            var a2 = g * a1;
            var a3 = g * a2;

            if (this.cutoff.isStatic() && this.q.isStatic()) {

                for (i in 0 until inp.length) {
                    var v0 = inp.a.get(i)

                    var v3 = v0 - ic2eq;
                    var v1 = a1 * ic1eq + a2 * v3;
                    var v2 = ic2eq + a2 * ic1eq + a3 * v3;
                    ic1eq = 2 * v1 - ic1eq;
                    ic2eq = 2 * v2 - ic2eq;


                    var low = v2;
                    var band = v1;
                    var high = v0 - k * v1 - v2;
//                    var notch = low + high
//                    var peak = low - high
                    var all = low + high - k*band
                    output.a.put(i, all.toFloat())
                }
            }
            else
                if (this.cutoff.isStatic()) {

                    for (i in 0 until inp.length) {
                        var v0 = inp.a.get(i)

                        k = 2 - 2 * _q.apply(0.0)
                        a1 = 1 / (1 + g * (g + k))
                        a2 = g * a1;
                        a3 = g * a2;


                        var v3 = v0 - ic2eq;
                        var v1 = a1 * ic1eq + a2 * v3;
                        var v2 = ic2eq + a2 * ic1eq + a3 * v3;
                        ic1eq = 2 * v1 - ic1eq;
                        ic2eq = 2 * v2 - ic2eq;


                        var low = v2;
                        var band = v1;
                        var high = v0 - k * v1 - v2;
//                        var notch = low + high
//                        var peak = low - high
                        var all = low + high - k*band
                        output.a.put(i, all.toFloat())
                    }
                }
                else {

                    for (i in 0 until inp.length) {
                        var v0 = inp.a.get(i)

                        g = Math.tan(_cutoff.apply(0.0) / IO.sampleRate)
                        k = 2 - 2 * _q.apply(0.0)
                        a1 = 1 / (1 + g * (g + k))
                        a2 = g * a1;
                        a3 = g * a2;


                        var v3 = v0 - ic2eq;
                        var v1 = a1 * ic1eq + a2 * v3;
                        var v2 = ic2eq + a2 * ic1eq + a3 * v3;
                        ic1eq = 2 * v1 - ic1eq;
                        ic2eq = 2 * v2 - ic2eq;


                        var low = v2;

                        var band = v1;
                        var high = v0 - k * v1 - v2;
//                        var notch = low + high
//                        var peak = low - high
                        var all = low + high - k*band

                        output.a.put(i, all.toFloat())
                    }
                }


            if (ic1eq.isNaN()) ic1eq = 0.0
            if (ic2eq.isNaN()) ic2eq = 0.0


            output
        }
    }



}