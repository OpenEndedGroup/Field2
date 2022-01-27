package auw

import field.linalg.Vec3
import org.lwjgl.openal.AL10
import org.lwjgl.openal.AL11
import org.lwjgl.openal.ALC11
import org.lwjgl.openal.ALC11.alcCaptureSamples
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer

open class IO(val s: Sound) {

    companion object {
        @JvmStatic
        val vectorSize = BoxTools.size

        val queueSize = 4
        val channels = 1
        val format = AL10.AL_FORMAT_MONO16

        @JvmStatic
        val sampleRate = 48000

        val thisInputSample =
            ByteBuffer.allocateDirect(IO.vectorSize * 4 ).order(ByteOrder.nativeOrder()).asFloatBuffer()

        val thisInputSampleLeft =
            ByteBuffer.allocateDirect(IO.vectorSize * 4 ).order(ByteOrder.nativeOrder()).asFloatBuffer()

    }

    val buffers = (0 until queueSize).map { s.makeBufferForLength(vectorSize, channels, sampleRate) }
    val containers = (0 until queueSize).map {
        buffers[it].buffer.get() to ByteBuffer.allocateDirect(channels * vectorSize * 2).order(ByteOrder.nativeOrder())
            .asShortBuffer()
    }.toMap()

    val _binputAudio = ByteBuffer.allocateDirect(IO.vectorSize * 2 *2).order(ByteOrder.nativeOrder())
    val inputAudio = _binputAudio.asShortBuffer()

    val source: Sound.Source by lazy {
        s.allocate()!!
    }

    var clock: Long = 0

    val timer_process = RollingTimer("process_time", 500, 5f)
    val timer_buffers = RollingTimer("buffers_free", 50, 5f)


    fun launch() {
        for (b in buffers) {
            AL10.alSourceQueueBuffers(source.source.get(0), b.buffer.get(0))
        }

        AL10.alSourcePlay(source.source.get(0))

        while (true) {
            val n = AL10.alGetSourcei(source.source.get(0), AL10.AL_BUFFERS_PROCESSED)
//            val q = AL10.alGetSourcei(source.source.get(0), AL10.AL_BUFFERS_QUEUED)


            timer_buffers.add(n)
            if (n == 0)
                yield()

            (0 until n).forEach {

                if (s.canCaptureSound() > 0) {
                    alcCaptureSamples(s.inputDevice, inputAudio, IO.vectorSize);
                    for (n in 0 until IO.vectorSize) {
                        thisInputSample[n] = inputAudio[2*n] / Short.MAX_VALUE.toFloat()
                        thisInputSampleLeft[n] = inputAudio[2*n+1] / Short.MAX_VALUE.toFloat()

//                            println("${thisInputSample[n]} + ${thisInputSampleLeft[n]}")
                    }
                }

                clock++
                val id = AL10.alSourceUnqueueBuffers(source.source.get(0))

                val result = containers.get(id)!!
                zero(result)
                clock++

                try {
                    timer_process.run { process(result) }
                } catch (e: Throwable) {
                    System.err.println(" exception in audio thread::process")
                    e.printStackTrace()
                }

                AL10.alBufferData(id, format, result, sampleRate)
                AL10.alSourceQueueBuffers(source.source.get(0), id)
            }
            val isPlaying = AL10.alGetSourcei(source.source.get(0), AL10.AL_SOURCE_STATE) == AL10.AL_PLAYING

            if (!isPlaying) {
                println(" source is not playing, restarting")
                AL10.alGetError().let {
                    println(" e1=$it ")
                }
                AL10.alSourcePlay(source.source.get(0))
                AL10.alGetError().let {
                    println(" e2=$it ")
                }
            }

//            System.out.print("." + (if (isPlaying) n else "-"))
        }
    }


    fun setSourcePosition(at: Vec3) {
        AL10.alSource3f(
            source.source[0],
            AL10.AL_POSITION,
            at.x.toFloat(),
            at.y.toFloat(),
            at.z.toFloat()
        )
    }


    open fun yield() {
        Thread.sleep(5)
    }


    fun zero(output: ShortBuffer) {
        for (x in 0 until output.limit()) {
            output.put(x, 0)
        }
    }

    open fun process(result: ShortBuffer) {

    }


}