package trace.input

import field.graphics.Texture
import org.openkinect.freenect.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer


class KinectDriver {


    companion object {
        @JvmField
        var texture: Texture? = null
        @JvmField
        var floatFrame: FloatBuffer? = null

        @JvmField
        var dev: Device

        @JvmField
        var ctx: Context = Freenect.createContext()

        var currentFrame: ByteBuffer? = null
        var shortFrame: ShortBuffer? = null

        var width: Int = -1
        var height: Int = -1


        var lock = Object()

        init {
            ctx.setLogHandler { dev, level, info ->
                println("** $dev | $level `$info`")
            }
            ctx.setLogLevel(LogLevel.SPEW)

            if (ctx.numDevices() > 0) {
                dev = ctx.openDevice(0)

                dev.setDepthFormat(DepthFormat.D11BIT)
            } else {
                throw IllegalArgumentException(" couldn't find a Kinect")
            }

            dev.startDepth(object : DepthHandler {
                internal var frameCount = 0

                override fun onFrameReceived(mode: FrameMode?, frame: ByteBuffer?, timestamp: Int) {
                    if (frame == null) {
                        System.err.println(" -- received null frame, continuing on")
                        return
                    }
//                    println(" -- frame received, mode${mode?.depthFormat} / ${mode?.width} x ${mode?.height}")
                    if (currentFrame == null) {
                        currentFrame = ByteBuffer.allocate(frame.capacity()).order(ByteOrder.nativeOrder())
                        shortFrame = currentFrame!!.asShortBuffer()

                        val floatFrame_b = ByteBuffer.allocateDirect(shortFrame!!.capacity() * 4).order(ByteOrder.nativeOrder())
                        floatFrame = floatFrame_b.asFloatBuffer()

                        texture = Texture(Texture.TextureSpecification.float1(3, mode!!.width.toInt(), mode!!.height.toInt(), floatFrame_b))
                    }
                    synchronized(lock)
                    {
                        frame.rewind()
                        currentFrame!!.rewind().put(frame)
                        currentFrame!!.rewind()

//                        println(shortFrame!!.get(100))

                        copyToFloatBuffer(shortFrame!!, floatFrame!!)
                        texture!!.upload()

                        if (mode != null) {
                            width = mode.width.toInt()
                            height = mode.height.toInt()
                        }
                    }
                }
            })
        }

        @JvmStatic
        var difference = 0f

        private fun copyToFloatBuffer(shortFrame: ShortBuffer, floatFrame: FloatBuffer) {
            floatFrame.clear()
            shortFrame.clear()
            var d = 0f
            for (n in 0 until shortFrame.capacity()) {
                var ff = shortFrame.get() / 1024f
                if (ff > 1) ff = 0f;

                d += Math.abs(floatFrame.get(n) - ff)
                floatFrame.put(n, ff)
            }
            floatFrame.clear()
            shortFrame.clear()
            difference = d
        }

        @JvmStatic
        fun depthAt(x: Double, y: Double): Float {
            val xx = Math.max(0, Math.min(width - 1, (width * x / 100).toInt()))
            val yy = Math.max(0, Math.min(height - 1, (height * y / 100).toInt()))

            return floatFrame?.get(yy * width + xx) ?: 0f

        }
    }


}