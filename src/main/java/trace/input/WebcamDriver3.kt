package trace.input

import field.app.RunLoop
import field.graphics.Texture
import org.bytedeco.javacv.FrameGrabber
import org.bytedeco.javacv.OpenCVFrameGrabber
import java.nio.ByteBuffer
import kotlin.system.measureNanoTime

class WebcamDriver3(val unit: Int, val wc : Int = 0) {

    companion object {
        @JvmStatic
        var w: Int = 0

        @JvmStatic
        var h: Int = 0

        @JvmStatic
        var storage: ByteBuffer? = null

        @JvmStatic
        var texture: Texture? = null

        @JvmStatic
        var webcam: OpenCVFrameGrabber? = null

    }

    init {
        if (storage == null) {
            val captureWidth = 640
            val captureHeight = 480

            val grabber = OpenCVFrameGrabber(wc)
//            grabber.imageWidth = captureWidth
//            grabber.imageHeight = captureHeight
            grabber.imageMode = FrameGrabber.ImageMode.COLOR
            grabber.start()

            webcam = grabber

            w = grabber.imageWidth
            h = grabber.imageHeight

            println(" image dimensions are $w $h")

//            storage = ByteBuffer.allocateDirect(grabber.imageWidth * grabber.imageHeight * 4);
            storage = ByteBuffer.allocateDirect(1920 * 1080 * 4);
            texture = Texture(Texture.TextureSpecification.byte3(unit, w, h, storage, false))

            RunLoop.main.mainLoop.attach(0) {
                update()
            }

        }
    }

    var last: ByteBuffer? = null

    var lastAt = 0L

    fun update() {
        if (System.currentTimeMillis() - lastAt > 1000 / 40.0) {

            val st = RunLoop.workerPool.submit {
                val ns = measureNanoTime {
                    try {
                        val f = webcam!!.grab()
                        last = f!!.image[0] as ByteBuffer?
                        print(last)
                    }
                    catch(f : FrameGrabber.Exception)
                    {
                        print(f)
                        f.printStackTrace()
                        println(" reopening .... ?")
                        val grabber = OpenCVFrameGrabber(wc)
                        OpenCVFrameGrabber(wc)
                        grabber.imageMode = FrameGrabber.ImageMode.COLOR
                        grabber.start()

                        webcam = grabber

                        w = grabber.imageWidth
                        h = grabber.imageHeight
                    }
                }
                WebcamDriver3.storage
            }

            RunLoop.main.whenNamed("__webcam__", st, {
                texture!!.upload(last, false)
                lastAt = System.currentTimeMillis()
            })
        }
    }
}