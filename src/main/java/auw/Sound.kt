package auw

import field.linalg.Vec3
import org.lwjgl.BufferUtils
import org.lwjgl.openal.*
import org.lwjgl.openal.ALC10.*
import org.lwjgl.util.WaveData
import java.io.BufferedInputStream
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer
import java.util.*
import java.util.function.Supplier


class Sound {

    private var context: Long = 0

    internal var sources: MutableList<Source> = ArrayList()
    internal var free: MutableList<Source> = ArrayList()

    inner class Buffer {
        var buffer = BufferUtils.createIntBuffer(1)
    }

    inner class Source {
        var source = BufferUtils.createIntBuffer(1)
        internal var sourcePos = BufferUtils.createFloatBuffer(3).put(
            floatArrayOf(0.0f, 0.0f, 0.0f)
        )
        internal var sourceVel = BufferUtils.createFloatBuffer(3).put(
            floatArrayOf(0.0f, 0.0f, 0.0f)
        )
        internal var offset = BufferUtils.createFloatBuffer(1).put(
            floatArrayOf(0.0f)
        )
    }

    fun makeSource(): Source? {
        val s = Source()
        AL10.alGenSources(s.source)
        if (AL10.alGetError() != AL10.AL_NO_ERROR)
            return null

        AL10.alSourcef(s.source.get(0), AL10.AL_PITCH, 1.0f)
        AL10.alSourcef(s.source.get(0), AL10.AL_GAIN, 1.0f)
        s.sourcePos.rewind()
        s.sourceVel.rewind()
        AL10.alSource3f(s.source.get(0), AL10.AL_POSITION, 0f, 0f, 0f)
        AL10.alSource3f(s.source.get(0), AL10.AL_VELOCITY, 0f, 0f, 0f)
        s.sourcePos.rewind()
        s.sourceVel.rewind()

        sources.add(s)
        free.add(s)
        return s
    }


    var inputDevice: Long = 0

    fun init(num: Int): Sound {

        System.out.println(" -------------------------------------------------------- ")
        System.out.println(" -------------------------------------------------------- ")

        val device = alcOpenDevice(null as ByteBuffer?)

        val deviceCaps = ALC.createCapabilities(device)

        context = alcCreateContext(device, null as IntBuffer?)
        alcMakeContextCurrent(context)
        AL.createCapabilities(deviceCaps)

        println("devices :" + ALC10.alcGetString(0, ALC10.ALC_EXTENSIONS))
        println("devices :" + ALC10.alcGetString(0, ALC10.ALC_DEVICE_SPECIFIER))
        println("devices :" + AL10.alGetString(AL10.AL_VENDOR))
        println("devices :" + AL10.alGetString(AL10.AL_RENDERER))
        println("devices :" + AL10.alGetString(AL10.AL_VERSION))
        println("devices :" + ALC10.alcGetInteger(0, ALC10.ALC_MAJOR_VERSION))
        println("devices :" + ALC10.alcGetInteger(0, ALC10.ALC_MINOR_VERSION))
        System.out.println(" -------------------------------------------------------- ")

        for (i in 0 until num)
            makeSource()


        println(
            " init openal :" + AL10.alGetError() + " "
                    + sources.size + " sources"
        )

//        inputDevice = ALC11.alcCaptureOpenDevice(null as? ByteBuffer, 48000, AL10.AL_FORMAT_MONO16, 48000 / 4)
        inputDevice = ALC11.alcCaptureOpenDevice(null as? ByteBuffer, 48000, AL10.AL_FORMAT_STEREO16, 48000 / 4)
        if (inputDevice != 0L)
            ALC11.alcCaptureStart(inputDevice)


        System.out.println(" -------------------------------------------------------- ")
        System.out.println(" -------------------------------------------------------- ")

        return this
    }

    fun canCaptureSound(): Int {
        val v = IntArray(1)
        ALC11.alcGetIntegerv(inputDevice, ALC11.ALC_CAPTURE_SAMPLES, v);
        return v[0]
    }

    fun allocate(): Source? {
        return if (free.size == 0) null else free.removeAt(0)
    }

    @JvmOverloads
    fun play(
        f: Buffer, s: Source, pitch: Float, gain: Float,
        position: Vec3, velocity: Vec3, positionInSames: Int = 0
    ): Supplier<Double> {
        AL10.alSourcei(s.source.get(0), AL10.AL_BUFFER, f.buffer.get(0))

        //        AL10.alSourcef(s.source.get(0), AL10.AL_PITCH, 1.0f);
        //        AL10.alSourcef(s.source.get(0), AL10.AL_GAIN, 1.0f);
        //
        s.sourcePos.rewind()
        s.sourcePos.put(position.x.toFloat())
        s.sourcePos.put(position.y.toFloat())
        s.sourcePos.put(position.z.toFloat())
        s.sourcePos.rewind()

        s.sourceVel.rewind()
        s.sourceVel.put(velocity.x.toFloat())
        s.sourceVel.put(velocity.y.toFloat())
        s.sourceVel.put(velocity.z.toFloat())
        s.sourceVel.rewind()

        s.offset.rewind()
        s.offset.put(positionInSames.toFloat())
        s.offset.rewind()

        AL10.alSource3f(
            s.source.get(0),
            AL10.AL_POSITION,
            position.x.toFloat(),
            position.y.toFloat(),
            position.z.toFloat()
        )
        AL10.alSource3f(
            s.source.get(0),
            AL10.AL_VELOCITY,
            velocity.x.toFloat(),
            velocity.y.toFloat(),
            velocity.z.toFloat()
        )
        AL10.alSourcef(s.source.get(0), AL11.AL_SAMPLE_OFFSET, s.offset.get())
        AL10.alSourcePlay(s.source.get(0))

        return Supplier<Double> {
            val v = AL10.alGetSourcef(s.source.get(0), AL11.AL_SEC_OFFSET)
            v.toDouble()
        }
    }


    class DeadReckoner(private val t: Supplier<Double>) : Supplier<Double> {

        internal var at: Long = 0
        internal var out: Double = 0.toDouble()
        internal var prev = 0.0
        internal var was: Double = 0.toDouble()
        internal var now: Double = 0.toDouble()
        internal var change: Long = 0

        internal var firstChangeAt: Long = 0
        internal var firstChangeTo = 0.0

        var isLive = false
            internal set

        override fun get(): Double {
            val m = t.get()

            if (m != prev) {
                println("change took " + ((System.nanoTime() - change) / 1000000000.0 - (m - prev)) + "   " + m + " <- " + prev)
                println("   run" + (System.nanoTime() - firstChangeAt) / 1000000000.0 / (m - firstChangeTo))

                change = System.nanoTime()
                was = prev
                now = m
                prev = m
                isLive = true
            }

            if (m == 0.0) {
                at = System.nanoTime()
                out = 0.0
                return out
            }

            if (m != 0.0 && firstChangeAt == 0L) {
                //                live = true;
                firstChangeAt = System.nanoTime()
                firstChangeTo = m
            }

            return m
        }

        fun blockUntilLive(): Runnable {
            return Runnable {
                while (!isLive) {
                    try {
                        Thread.sleep(0, 100)
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }

                }
            }
        }

        //        public void blockUntilLive(SwapControl c) {
        //            c.addNextSwap("" + this, blockUntilLive());
        //        }
    }

    fun stop(s: Source) {
        AL10.alSourceStop(s.source.get(0))
    }

    fun pause(s: Source) {
        AL10.alSourcePause(s.source.get(0))
    }

    fun play(s: Source) {
        AL10.alSourcePlay(s.source.get(0))
    }


    fun deallocate(s: Source) {
        AL10.alSourceStop(s.source.get(0))
        AL10.alSourcei(s.source.get(0), AL10.AL_BUFFER, 0)
        free.add(s)
    }

    @Throws(FileNotFoundException::class)
    fun makeBufferFromFile(filename: String): Buffer {
        val data = WaveData.create(
            BufferedInputStream(
                FileInputStream(filename)
            )
        )

        println(
            " loaded :" + data.format + " " + data.samplerate
                    + " " + data.data
        )
        val r = Buffer()
        AL10.alGenBuffers(r.buffer)
        AL10.alBufferData(
            r.buffer.get(0), data.format, data.data,
            data.samplerate
        )
        //		AL10.alBufferData(r.buffer.get(0), 0x1205, data.data,
        //				data.samplerate);
        data.dispose()

        return r
    }

    @Throws(FileNotFoundException::class)
    fun makeBufferFromData(data: WaveData): Buffer {

        println(
            " loaded :" + data.format + " " + data.samplerate
                    + " " + data.data
        )
        val r = Buffer()
        AL10.alGenBuffers(r.buffer)
        AL10.alBufferData(
            r.buffer.get(0), data.format, data.data,
            data.samplerate
        )
        //		AL10.alBufferData(r.buffer.get(0), 0x1205, data.data,
        //				data.samplerate);
        data.dispose()

        return r
    }

    @Throws(FileNotFoundException::class)
    fun makeBufferFromFloats(data: FloatArray, sampleRate: Int): Buffer {

        val s = ByteBuffer.allocateDirect(data.size * 2)
            .order(ByteOrder.nativeOrder()).asShortBuffer()
        for (f in data)
            s.put(
                Math.min(
                    java.lang.Short.MAX_VALUE.toFloat(),
                    Math.max(java.lang.Short.MIN_VALUE.toFloat(), java.lang.Short.MAX_VALUE * f)
                ).toInt().toShort()
            )
        s.rewind()
        val r = Buffer()
        AL10.alGenBuffers(r.buffer)
        AL10.alBufferData(r.buffer.get(0), AL10.AL_FORMAT_MONO16, s, sampleRate)
        return r
    }

    @Throws(FileNotFoundException::class)
    fun makeBufferForLength(length: Int, channels: Int, sampleRate: Int): Buffer {

        val r = Buffer()
        AL10.alGenBuffers(r.buffer)
        if (channels == 1)
            AL10.alBufferData(r.buffer.get(0), AL10.AL_FORMAT_MONO16, IntArray(length), sampleRate)
        else if (channels == 2)
            AL10.alBufferData(r.buffer.get(0), AL10.AL_FORMAT_STEREO16, IntArray(length * 2), sampleRate)
        else
            throw IllegalArgumentException("" + channels)

        return r
    }

    @Throws(FileNotFoundException::class)
    fun makeBufferFromFloats(
        left: FloatArray, right: FloatArray,
        sampleRate: Int
    ): Buffer {

        val s = ByteBuffer.allocateDirect(left.size * 4)
            .order(ByteOrder.nativeOrder()).asShortBuffer()

        for (f in left.indices) {
            s.put(
                Math.min(
                    java.lang.Short.MAX_VALUE.toFloat(),
                    Math.max(java.lang.Short.MIN_VALUE.toFloat(), java.lang.Short.MAX_VALUE * left[f])
                ).toInt().toShort()
            )
            s.put(
                Math.min(
                    java.lang.Short.MAX_VALUE.toFloat(),
                    Math.max(java.lang.Short.MIN_VALUE.toFloat(), java.lang.Short.MAX_VALUE * right[f])
                ).toInt().toShort()
            )
        }
        s.rewind()
        val r = Buffer()
        AL10.alGenBuffers(r.buffer)
        AL10.alBufferData(
            r.buffer.get(0), AL10.AL_FORMAT_STEREO16, s,
            sampleRate
        )
        return r
    }

    companion object {

        @Throws(FileNotFoundException::class)
        fun floatsFromFile(filename: String): FloatBuffer? {
            val data = WaveData.create(
                BufferedInputStream(
                    FileInputStream(filename)
                )
            )

            println(
                " format :" + data.format + " " + data.samplerate
                        + " " + data.data
            )
            try {
                when (data.format) {
                    AL10.AL_FORMAT_MONO16 -> {
                        val f = FloatBuffer.allocate(data.data.capacity() / 2)
                        println(" mono 16")
                        val s = data.data.asShortBuffer()
                        for (i in 0 until s.capacity()) {
                            f.put(s.get(i) / java.lang.Short.MAX_VALUE.toFloat())
                        }
                        return f
                    }
                    AL10.AL_FORMAT_STEREO16 -> {
                        val f = FloatBuffer.allocate(data.data.capacity() / 4)
                        println(" stereo 16, taking left channel")
                        val s = data.data.asShortBuffer()
                        for (i in 0 until s.capacity() / 2) {
                            f.put(s.get(i * 2) / java.lang.Short.MAX_VALUE.toFloat())
                        }
                        return f
                    }
                    else -> {
                        throw java.lang.IllegalArgumentException(" can't read audio from file $filename")
                    }
                }
            } finally {
                data.dispose()
            }
            return null
        }
    }

}
