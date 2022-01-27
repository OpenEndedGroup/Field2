package auw.signal

/******************************************************************************
 *
 * libresample4j
 * Copyright (c) 2009 Laszlo Systems, Inc. All Rights Reserved.
 *
 * libresample4j is a Java port of Dominic Mazzoni's libresample 0.1.3,
 * which is in turn based on Julius Smith's Resample 1.7 library.
 * http://www-ccrma.stanford.edu/~jos/resample/
 *
 * License: LGPL -- see the file LICENSE.txt for more information
 *
 */

/**
 * This file provides Kaiser-windowed low-pass filter support,
 * including a function to create the filter coefficients, and
 * two functions to apply the filter at a particular point.
 *
 * <pre>
 * reference: "Digital Filters, 2nd edition"
 * R.W. Hamming, pp. 178-179
 *
 * Izero() computes the 0th order modified bessel function of the first kind.
 * (Needed to compute Kaiser window).
 *
 * LpFilter() computes the coeffs of a Kaiser-windowed low pass filter with
 * the following characteristics:
 *
 * c[]  = array in which to store computed coeffs
 * frq  = roll-off frequency of filter
 * N    = Half the window length in number of coeffs
 * Beta = parameter of Kaiser window
 * Num  = number of coeffs before 1/frq
 *
 * Beta trades the rejection of the lowpass filter against the transition
 * width from passband to stopband.  Larger Beta means a slower
 * transition and greater stopband rejection.  See Rabiner and Gold
 * (Theory and Application of DSP) under Kaiser windows for more about
 * Beta.  The following table from Rabiner and Gold gives some feel
 * for the effect of Beta:
 *
 * All ripples in dB, width of transition band = D*N where N = window length
 *
 * BETA    D       PB RIP   SB RIP
 * 2.120   1.50  +-0.27      -30
 * 3.384   2.23    0.0864    -40
 * 4.538   2.93    0.0274    -50
 * 5.658   3.62    0.00868   -60
 * 6.764   4.32    0.00275   -70
 * 7.865   5.0     0.000868  -80
 * 8.960   5.7     0.000275  -90
 * 10.056  6.4     0.000087  -100
</pre> *
 */
object FilterKit {

    // Max error acceptable in Izero
    private val IzeroEPSILON = 1E-21

    private fun Izero(x: Double): Double {
        var sum: Double
        var u: Double
        val halfx: Double
        var temp: Double
        var n: Int

        n = 1
        u = n.toDouble()
        sum = u
        halfx = x / 2.0
        do {
            temp = halfx / n.toDouble()
            n += 1
            temp *= temp
            u *= temp
            sum += u
        } while (u >= IzeroEPSILON * sum)
        return sum
    }

    fun lrsLpFilter(c: DoubleArray, N: Int, frq: Double, Beta: Double, Num: Int) {
        val IBeta: Double
        var temp: Double
        var temp1: Double
        val inm1: Double
        var i: Int

        // Calculate ideal lowpass filter impulse response coefficients:
        c[0] = 2.0 * frq
        i = 1
        while (i < N) {
            temp = Math.PI * i.toDouble() / Num.toDouble()
            c[i] = Math.sin(2.0 * temp * frq) / temp // Analog sinc function,
            i++
            // cutoff = frq
        }

        /*
         * Calculate and Apply Kaiser window to ideal lowpass filter. Note: last
         * window value is IBeta which is NOT zero. You're supposed to really
         * truncate the window here, not ramp it to zero. This helps reduce the
         * first sidelobe.
         */
        IBeta = 1.0 / Izero(Beta)
        inm1 = 1.0 / (N - 1).toDouble()
        i = 1
        while (i < N) {
            temp = i.toDouble() * inm1
            temp1 = 1.0 - temp * temp
            temp1 = if (temp1 < 0) 0.0 else temp1
            c[i] *= Izero(Beta * Math.sqrt(temp1)) * IBeta
            i++
        }
    }

    /**
     *
     * @param Imp impulse response
     * @param ImpD impulse response deltas
     * @param Nwing length of one wing of filter
     * @param Interp Interpolate coefs using deltas?
     * @param Xp_array Current sample array
     * @param Xp_index Current sample index
     * @param Ph Phase
     * @param Inc increment (1 for right wing or -1 for left)
     * @return
     */
    fun lrsFilterUp(
        Imp: FloatArray, ImpD: FloatArray, Nwing: Int, Interp: Boolean, Xp_array: FloatArray, Xp_index: Int, Ph: Double,
        Inc: Int
    ): Float {
        var Xp_index = Xp_index
        var Ph = Ph
        var a = 0.0
        var v: Float
        var t: Float

        Ph *= Resampler.Npc.toDouble() // Npc is number of values per 1/delta in impulse
        // response

        v = 0.0f // The output value

        val Hp_array = Imp
        var Hp_index = Ph.toInt()

        val End_array = Imp
        var End_index = Nwing

        val Hdp_array = ImpD
        var Hdp_index = Ph.toInt()

        if (Interp) {
            // Hdp = &ImpD[(int)Ph];
            a = Ph - Math.floor(Ph) /* fractional part of Phase */
        }

        if (Inc == 1)
        // If doing right wing...
        { // ...drop extra coeff, so when Ph is
            End_index-- // 0.5, we don't do too many mult's
            if (Ph == 0.0)
            // If the phase is zero...
            { // ...then we've already skipped the
                Hp_index += Resampler.Npc // first sample, so we must also
                Hdp_index += Resampler.Npc // skip ahead in Imp[] and ImpD[]
            }
        }

        if (Interp)
            while (Hp_index < End_index) {
                t = Hp_array[Hp_index] /* Get filter coeff */
                t += (Hdp_array[Hdp_index] * a).toFloat() /* t is now interp'd filter coeff */
                Hdp_index += Resampler.Npc /* Filter coeff differences step */
                t *= Xp_array[Xp_index] /* Mult coeff by input sample */
                v += t /* The filter output */
                Hp_index += Resampler.Npc /* Filter coeff step */
                Xp_index += Inc /* Input signal step. NO CHECK ON BOUNDS */
            }
        else
            while (Hp_index < End_index) {
                t = Hp_array[Hp_index] /* Get filter coeff */
                t *= Xp_array[Xp_index] /* Mult coeff by input sample */
                v += t /* The filter output */
                Hp_index += Resampler.Npc /* Filter coeff step */
                Xp_index += Inc /* Input signal step. NO CHECK ON BOUNDS */
            }

        return v
    }

    /**
     *
     * @param Imp impulse response
     * @param ImpD impulse response deltas
     * @param Nwing length of one wing of filter
     * @param Interp Interpolate coefs using deltas?
     * @param Xp_array Current sample array
     * @param Xp_index Current sample index
     * @param Ph Phase
     * @param Inc increment (1 for right wing or -1 for left)
     * @param dhb filter sampling period
     * @return
     */
    fun lrsFilterUD(
        Imp: FloatArray, ImpD: FloatArray, Nwing: Int, Interp: Boolean, Xp_array: FloatArray, Xp_index: Int, Ph: Double,
        Inc: Int, dhb: Double
    ): Float {
        var Xp_index = Xp_index
        var a: Float
        var v: Float
        var t: Float
        var Ho: Double = Ph * dhb

        v = 0.0f // The output value

        val End_array = Imp
        var End_index = Nwing

        if (Inc == 1)
        // If doing right wing...
        { // ...drop extra coeff, so when Ph is
            End_index-- // 0.5, we don't do too many mult's
            if (Ph == 0.0)
            // If the phase is zero...
                Ho += dhb // ...then we've already skipped the
        } // first sample, so we must also
        // skip ahead in Imp[] and ImpD[]

        val Hp_array = Imp
        var Hp_index: Int

        if (Interp) {
            val Hdp_array = ImpD
            var Hdp_index: Int

            while ((Ho.toInt()) < End_index) {
                Hp_index = Ho.toInt()
                t = Hp_array[Hp_index] // Get IR sample
                Hdp_index = Ho.toInt() // get interp bits from diff table
                a = (Ho - Math.floor(Ho)).toFloat() // a is logically between 0
                // and 1
                t += Hdp_array[Hdp_index] * a // t is now interp'd filter coeff
                t *= Xp_array[Xp_index] // Mult coeff by input sample
                v += t // The filter output
                Ho += dhb // IR step
                Xp_index += Inc // Input signal step. NO CHECK ON BOUNDS
            }
        } else {
            while (( Ho.toInt()) < End_index) {
                Hp_index = Ho.toInt()
                t = Hp_array[Hp_index] // Get IR sample
                t *= Xp_array[Xp_index] // Mult coeff by input sample
                v += t // The filter output
                Ho += dhb // IR step
                Xp_index += Inc // Input signal step. NO CHECK ON BOUNDS
            }
        }

        return v
    }

}