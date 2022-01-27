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
package auw.signal

import java.nio.FloatBuffer

class Resampler {

    private val Imp: FloatArray
    private val ImpD: FloatArray
    private val LpScl: Float
    private val Nmult: Int
    private val Nwing: Int
    private val minFactor: Double
    private val maxFactor: Double
    private val XSize: Int
    private val X: FloatArray
    private var Xp: Int = 0 // Current "now"-sample pointer for input
    private var Xread: Int = 0 // Position to put new samples
    val filterWidth: Int
    private val Y: FloatArray
    private var Yp: Int = 0
    private var Time: Double = 0.toDouble()

    class Result(val inputSamplesConsumed: Int, val outputSamplesGenerated: Int)

    /**
     * Clone an existing resampling session. Faster than creating one from scratch.
     *
     * @param other
     */
    constructor(other: Resampler) {
        this.Imp = other.Imp.clone()
        this.ImpD = other.ImpD.clone()
        this.LpScl = other.LpScl
        this.Nmult = other.Nmult
        this.Nwing = other.Nwing
        this.minFactor = other.minFactor
        this.maxFactor = other.maxFactor
        this.XSize = other.XSize
        this.X = other.X.clone()
        this.Xp = other.Xp
        this.Xread = other.Xread
        this.filterWidth = other.filterWidth
        this.Y = other.Y.clone()
        this.Yp = other.Yp
        this.Time = other.Time
    }

    /**
     * Create a new resampling session.
     *
     * @param highQuality true for better quality, slower processing time
     * @param minFactor   lower bound on resampling factor for this session
     * @param maxFactor   upper bound on resampling factor for this session
     * @throws IllegalArgumentException if minFactor or maxFactor is not
     * positive, or if maxFactor is less than minFactor
     */
    constructor(highQuality: Boolean, minFactor: Double, maxFactor: Double) {
        if (minFactor <= 0.0 || maxFactor <= 0.0) {
            throw IllegalArgumentException("minFactor and maxFactor must be positive")
        }
        if (maxFactor < minFactor) {
            throw IllegalArgumentException("minFactor must be <= maxFactor")
        }

        this.minFactor = minFactor
        this.maxFactor = maxFactor
        this.Nmult = if (highQuality) 35 else 11
        this.LpScl = 1.0f
        this.Nwing = Npc * (this.Nmult - 1) / 2 // # of filter coeffs in right wing

        val Rolloff = 0.90
        val Beta = 6.0

        val Imp64 = DoubleArray(this.Nwing)

        FilterKit.lrsLpFilter(Imp64, this.Nwing, 0.5 * Rolloff, Beta, Npc)
        this.Imp = FloatArray(this.Nwing)
        this.ImpD = FloatArray(this.Nwing)

        for (i in 0 until this.Nwing) {
            this.Imp[i] = Imp64[i].toFloat()
        }

        // Storing deltas in ImpD makes linear interpolation
        // of the filter coefficients faster
        for (i in 0 until this.Nwing - 1) {
            this.ImpD[i] = this.Imp[i + 1] - this.Imp[i]
        }

        // Last coeff. not interpolated
        this.ImpD[this.Nwing - 1] = -this.Imp[this.Nwing - 1]

        // Calc reach of LP filter wing (plus some creeping room)
        val Xoff_min = ((this.Nmult + 1) / 2.0 * Math.max(1.0, 1.0 / minFactor) + 10).toInt()
        val Xoff_max = ((this.Nmult + 1) / 2.0 * Math.max(1.0, 1.0 / maxFactor) + 10).toInt()
        this.filterWidth = Math.max(Xoff_min, Xoff_max)

        // Make the inBuffer size at least 4096, but larger if necessary
        // in order to store the minimum reach of the LP filter and then some.
        // Then allocate the buffer an extra Xoff larger so that
        // we can zero-pad up to Xoff zeros at the end when we reach the
        // end of the input samples.
        this.XSize = Math.max(2 * this.filterWidth + 10, 4096)
        this.X = FloatArray(this.XSize + this.filterWidth)
        this.Xp = this.filterWidth
        this.Xread = this.filterWidth

        // Make the outBuffer long enough to hold the entire processed
        // output of one inBuffer
        val YSize = (this.XSize.toDouble() * maxFactor + 2.0).toInt()
        this.Y = FloatArray(YSize)
        this.Yp = 0

        this.Time = this.filterWidth.toDouble() // Current-time pointer for converter
    }

    /**
     * Process a batch of samples. There is no guarantee that the input buffer will be drained.
     *
     * @param factor    factor at which to resample this batch
     * @param buffers   sample buffer for producing input and consuming output
     * @param lastBatch true if this is known to be the last batch of samples
     * @return true iff resampling is complete (ie. no input samples consumed and no output samples produced)
     */
    fun process(factor: Double, buffers: SampleBuffers, lastBatch: Boolean): Boolean {
        if (factor < this.minFactor || factor > this.maxFactor) {
            throw IllegalArgumentException(
                "factor " + factor + " is not between minFactor=" + minFactor
                        + " and maxFactor=" + maxFactor
            )
        }

        val outBufferLen = buffers.outputBufferLength
        val inBufferLen = buffers.inputBufferLength

        val Imp = this.Imp
        val ImpD = this.ImpD
        var LpScl = this.LpScl
        val Nwing = this.Nwing
        val interpFilt = false // TRUE means interpolate filter coeffs

        var inBufferUsed = 0
        var outSampleCount = 0

        // Start by copying any samples still in the Y buffer to the output
        // buffer
        if (this.Yp != 0 && outBufferLen - outSampleCount > 0) {
            val len = Math.min(outBufferLen - outSampleCount, this.Yp)

            buffers.consumeOutput(this.Y, 0, len)
            //for (int i = 0; i < len; i++) {
            //    outBuffer[outBufferOffset + outSampleCount + i] = this.Y[i];
            //}

            outSampleCount += len
            for (i in 0 until this.Yp - len) {
                this.Y[i] = this.Y[i + len]
            }
            this.Yp -= len
        }

        // If there are still output samples left, return now - we need
        // the full output buffer available to us...
        if (this.Yp != 0) {
            return inBufferUsed == 0 && outSampleCount == 0
        }

        // Account for increased filter gain when using factors less than 1
        if (factor < 1) {
            LpScl = (LpScl * factor).toFloat()
        }

        while (true) {

            // This is the maximum number of samples we can process
            // per loop iteration

            /*
             * #ifdef DEBUG
             * printf("XSize: %d Xoff: %d Xread: %d Xp: %d lastFlag: %d\n",
             * this.XSize, this.Xoff, this.Xread, this.Xp, lastFlag); #endif
             */

            // Copy as many samples as we can from the input buffer into X
            var len = this.XSize - this.Xread

            if (len >= inBufferLen - inBufferUsed) {
                len = inBufferLen - inBufferUsed
            }

            buffers.produceInput(this.X, this.Xread, len)
            //for (int i = 0; i < len; i++) {
            //    this.X[this.Xread + i] = inBuffer[inBufferOffset + inBufferUsed + i];
            //}

            inBufferUsed += len
            this.Xread += len

            val Nx: Int
            if (lastBatch && inBufferUsed == inBufferLen) {
                // If these are the last samples, zero-pad the
                // end of the input buffer and make sure we process
                // all the way to the end
                Nx = this.Xread - this.filterWidth
                for (i in 0 until this.filterWidth) {
                    this.X[this.Xread + i] = 0f
                }
            } else {
                Nx = this.Xread - 2 * this.filterWidth
            }

            /*
             * #ifdef DEBUG fprintf(stderr, "new len=%d Nx=%d\n", len, Nx);
             * #endif
             */

            if (Nx <= 0) {
                break
            }

            // Resample stuff in input buffer
            val Nout: Int
            if (factor >= 1) { // SrcUp() is faster if we can use it */
                Nout = lrsSrcUp(this.X, this.Y, factor, /* &this.Time, */Nx, Nwing, LpScl, Imp, ImpD, interpFilt)
            } else {
                Nout = lrsSrcUD(this.X, this.Y, factor, /* &this.Time, */Nx, Nwing, LpScl, Imp, ImpD, interpFilt)
            }

            /*
             * #ifdef DEBUG
             * printf("Nout: %d\n", Nout);
             * #endif
             */

            this.Time -= Nx.toDouble() // Move converter Nx samples back in time
            this.Xp += Nx // Advance by number of samples processed

            // Calc time accumulation in Time
            val Ncreep = this.Time.toInt() - this.filterWidth
            if (Ncreep != 0) {
                this.Time -= Ncreep.toDouble() // Remove time accumulation
                this.Xp += Ncreep // and add it to read pointer
            }

            // Copy part of input signal that must be re-used
            val Nreuse = this.Xread - (this.Xp - this.filterWidth)

            for (i in 0 until Nreuse) {
                this.X[i] = this.X[i + (this.Xp - this.filterWidth)]
            }

            /*
            #ifdef DEBUG
            printf("New Xread=%d\n", Nreuse);
            #endif */

            this.Xread = Nreuse // Pos in input buff to read new data into
            this.Xp = this.filterWidth

            this.Yp = Nout

            // Copy as many samples as possible to the output buffer
            if (this.Yp != 0 && outBufferLen - outSampleCount > 0) {
                len = Math.min(outBufferLen - outSampleCount, this.Yp)

                buffers.consumeOutput(this.Y, 0, len)
                //for (int i = 0; i < len; i++) {
                //    outBuffer[outBufferOffset + outSampleCount + i] = this.Y[i];
                //}

                outSampleCount += len
                for (i in 0 until this.Yp - len) {
                    this.Y[i] = this.Y[i + len]
                }
                this.Yp -= len
            }

            // If there are still output samples left, return now,
            //   since we need the full output buffer available
            if (this.Yp != 0) {
                break
            }
        }

        return inBufferUsed == 0 && outSampleCount == 0
    }

    /**
     * Process a batch of samples. Convenience method for when the input and output are both floats.
     *
     * @param factor       factor at which to resample this batch
     * @param inputBuffer  contains input samples in the range -1.0 to 1.0
     * @param outputBuffer output samples will be deposited here
     * @param lastBatch    true if this is known to be the last batch of samples
     * @return true iff resampling is complete (ie. no input samples consumed and no output samples produced)
     */
    fun process(factor: Double, inputBuffer: FloatBuffer, lastBatch: Boolean, outputBuffer: FloatBuffer): Boolean {
        val sampleBuffers = object : SampleBuffers {
            override val inputBufferLength: Int
                get() = inputBuffer.remaining()

            override val outputBufferLength: Int
                get() = outputBuffer.remaining()

            override fun produceInput(array: FloatArray, offset: Int, length: Int) {
                inputBuffer.get(array, offset, length)
            }

            override fun consumeOutput(array: FloatArray, offset: Int, length: Int) {
                outputBuffer.put(array, offset, length)
            }
        }
        return process(factor, sampleBuffers, lastBatch)
    }

    /**
     * Process a batch of samples. Alternative interface if you prefer to work with arrays.
     *
     * @param factor         resampling rate for this batch
     * @param inBuffer       array containing input samples in the range -1.0 to 1.0
     * @param inBufferOffset offset into inBuffer at which to start processing
     * @param inBufferLen    number of valid elements in the inputBuffer
     * @param lastBatch      pass true if this is the last batch of samples
     * @param outBuffer      array to hold the resampled data
     * @return the number of samples consumed and generated
     */
    fun process(
        factor: Double,
        inBuffer: FloatArray,
        inBufferOffset: Int,
        inBufferLen: Int,
        lastBatch: Boolean,
        outBuffer: FloatArray,
        outBufferOffset: Int,
        outBufferLen: Int
    ): Result {
        val inputBuffer = FloatBuffer.wrap(inBuffer, inBufferOffset, inBufferLen)
        val outputBuffer = FloatBuffer.wrap(outBuffer, outBufferOffset, outBufferLen)

        process(factor, inputBuffer, lastBatch, outputBuffer)

        return Result(inputBuffer.position() - inBufferOffset, outputBuffer.position() - outBufferOffset)
    }


    /*
     * Sampling rate up-conversion only subroutine; Slightly faster than
     * down-conversion;
     */
    private fun lrsSrcUp(
        X: FloatArray, Y: FloatArray, factor: Double, Nx: Int, Nwing: Int, LpScl: Float, Imp: FloatArray,
        ImpD: FloatArray, Interp: Boolean
    ): Int {

        val Xp_array = X
        var Xp_index: Int

        val Yp_array = Y
        var Yp_index = 0

        var v: Float

        var CurrentTime = this.Time
        val dt: Double // Step through input signal
        val endTime: Double // When Time reaches EndTime, return to user

        dt = 1.0 / factor // Output sampling period

        endTime = CurrentTime + Nx
        while (CurrentTime < endTime) {
            val LeftPhase = CurrentTime - Math.floor(CurrentTime)
            val RightPhase = 1.0 - LeftPhase

            Xp_index = CurrentTime.toInt() // Ptr to current input sample
            // Perform left-wing inner product
            v = FilterKit.lrsFilterUp(Imp, ImpD, Nwing, Interp, Xp_array, Xp_index++, LeftPhase, -1)
            // Perform right-wing inner product
            v += FilterKit.lrsFilterUp(Imp, ImpD, Nwing, Interp, Xp_array, Xp_index, RightPhase, 1)

            v *= LpScl // Normalize for unity filter gain

            Yp_array[Yp_index++] = v // Deposit output
            CurrentTime += dt // Move to next sample by time increment
        }

        this.Time = CurrentTime
        return Yp_index // Return the number of output samples
    }

    private fun lrsSrcUD(
        X: FloatArray, Y: FloatArray, factor: Double, Nx: Int, Nwing: Int, LpScl: Float, Imp: FloatArray,
        ImpD: FloatArray, Interp: Boolean
    ): Int {

        val Xp_array = X
        var Xp_index: Int

        val Yp_array = Y
        var Yp_index = 0

        var v: Float

        var CurrentTime = this.Time
        val dh: Double // Step through filter impulse response
        val dt: Double // Step through input signal
        val endTime: Double // When Time reaches EndTime, return to user

        dt = 1.0 / factor // Output sampling period

        dh = Math.min(Npc.toDouble(), factor * Npc) // Filter sampling period

        endTime = CurrentTime + Nx
        while (CurrentTime < endTime) {
            val LeftPhase = CurrentTime - Math.floor(CurrentTime)
            val RightPhase = 1.0 - LeftPhase

            Xp_index = CurrentTime.toInt() // Ptr to current input sample
            // Perform left-wing inner product
            v = FilterKit.lrsFilterUD(Imp, ImpD, Nwing, Interp, Xp_array, Xp_index++, LeftPhase, -1, dh)
            // Perform right-wing inner product
            v += FilterKit.lrsFilterUD(Imp, ImpD, Nwing, Interp, Xp_array, Xp_index, RightPhase, 1, dh)

            v *= LpScl // Normalize for unity filter gain

            Yp_array[Yp_index++] = v // Deposit output

            CurrentTime += dt // Move to next sample by time increment
        }

        this.Time = CurrentTime
        return Yp_index // Return the number of output samples
    }

    companion object {

        // number of values per 1/delta in impulse response
        val Npc = 4096
    }

}