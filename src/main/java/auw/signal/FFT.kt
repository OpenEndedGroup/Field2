package auw.signal

import auw.BufferTools.zero
import auw.set
import java.nio.FloatBuffer

class Fft(val N : Int) {

    /*
	 * Computes the discrete Fourier transform (DFT) of the given complex vector, storing the result back into the vector.
	 * The vector can have any length. This is a wrapper function.
	 */
    fun transform(real: FloatBuffer, imag: FloatBuffer) {
        val n = real.capacity()
        if (n!=N)
            throw IllegalArgumentException("Mismatched lengths")

        if (n != imag.capacity())
            throw IllegalArgumentException("Mismatched lengths")

        if (n == 0)
            return
        else if (n and n - 1 == 0)
        // Is power of 2
            transformRadix2(real, imag)
        else
            throw IllegalArgumentException("non-pot length")
    }


    /*
	 * Computes the inverse discrete Fourier transform (IDFT) of the given complex vector, storing the result back into the vector.
	 * The vector can have any length. This is a wrapper function. This transform does not perform scaling, so the inverse is not a true inverse.
	 */
    fun inverseTransform(real: FloatBuffer, imag: FloatBuffer) {
        transform(imag, real)
    }

    val cosTable = FloatArray(N / 2)
    val sinTable = FloatArray(N / 2)

    init {
        // Trigonometric tables
        for (i in 0 until N / 2) {
            cosTable[i] = Math.cos(2.0 * Math.PI * i.toDouble() / N).toFloat()
            sinTable[i] = Math.sin(2.0 * Math.PI * i.toDouble() / N).toFloat()
        }
    }


    /*
	 * Computes the discrete Fourier transform (DFT) of the given complex vector, storing the result back into the vector.
	 * The vector's length must be a power of 2. Uses the Cooley-Tukey decimation-in-time radix-2 algorithm.
	 */
    fun transformRadix2(real: FloatBuffer, imag: FloatBuffer) {
        // Length variables
        val n = real.capacity()
        if (n != imag.capacity())
            throw IllegalArgumentException("Mismatched lengths")
        val levels = 31 - Integer.numberOfLeadingZeros(n)  // Equal to floor(log2(n))
        if (1 shl levels != n)
            throw IllegalArgumentException("Length is not a power of 2")


        // Bit-reversed addressing permutation
        for (i in 0 until n) {
            val j = Integer.reverse(i).ushr(32 - levels)
            if (j > i) {
                var temp = real[i]
                real[i] = real[j]
                real[j] = temp
                temp = imag[i]
                imag[i] = imag[j]
                imag[j] = temp
            }
        }

        // Cooley-Tukey decimation-in-time radix-2 FFT
        var size = 2
        while (size <= n) {
            val halfsize = size / 2
            val tablestep = n / size
            var i = 0
            while (i < n) {
                var j = i
                var k = 0
                while (j < i + halfsize) {
                    val l = j + halfsize
                    val tpre = real[l] * cosTable[k] + imag[l] * sinTable[k]
                    val tpim = -real[l] * sinTable[k] + imag[l] * cosTable[k]
                    real[l] = real[j] - tpre
                    imag[l] = imag[j] - tpim
                    real[j] += tpre
                    imag[j] += tpim
                    j++
                    k += tablestep
                }
                i += size
            }
            if (size == n)
            // Prevent overflow in 'size *= 2'
                break
            size *= 2
        }
    }

    /*
	 * Computes the circular convolution of the given real vectors. Each vector's length must be the same.
	 *
	 * // destroys input vector
	 */
    fun convolve(x: FloatBuffer, y: FloatBuffer, out: FloatBuffer, tmp_x : FloatBuffer, tmp_y : FloatBuffer, tmp_z : FloatBuffer) {
        val n = x.capacity()
        if (n != y.capacity() || n != out.capacity())
            throw IllegalArgumentException("Mismatched lengths")

        zero(tmp_x)
        zero(tmp_y)
        zero(tmp_z)

        _convolve(x, tmp_x, y, tmp_y, out, tmp_z)
    }


    /*
	 * Computes the circular convolution of the given complex vectors. Each vector's length must be the same.
	 */
    fun _convolve(
        xreal: FloatBuffer, ximag: FloatBuffer,
        yreal: FloatBuffer, yimag: FloatBuffer, outreal: FloatBuffer, outimag: FloatBuffer
    ) {
        var xreal = xreal
        var ximag = ximag
        var yreal = yreal
        var yimag = yimag

        val n = xreal.capacity()
        if (n != ximag.capacity() || n != yreal.capacity() || n != yimag.capacity()
            || n != outreal.capacity() || n != outimag.capacity()
        )
            throw IllegalArgumentException("Mismatched lengths")

        transform(xreal, ximag)
        transform(yreal, yimag)

        for (i in 0 until n) {
            val temp = xreal[i] * yreal[i] - ximag[i] * yimag[i]
            ximag[i] = ximag[i] * yreal[i] + xreal[i] * yimag[i]
            xreal[i] = temp
        }
        inverseTransform(xreal, ximag)

        for (i in 0 until n) {  // Scaling (because this FFT implementation omits it)
            outreal[i] = xreal[i] / n
            outimag[i] = ximag[i] / n
        }
    }

}