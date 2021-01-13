package trace.input

import com.github.sarxos.webcam.Webcam
import field.app.RunLoop
import field.graphics.Texture
import org.bytedeco.javacv.FrameGrabber
import org.bytedeco.javacv.OpenCVFrameGrabber
import java.nio.ByteBuffer
import kotlin.system.measureNanoTime

class WebcamDriver3(val unit: Int) {

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

            val grabber = OpenCVFrameGrabber(0)
//            grabber.imageWidth = captureWidth
//            grabber.imageHeight = captureHeight
            grabber.imageMode = FrameGrabber.ImageMode.COLOR
            grabber.start()

            webcam = grabber

            w = grabber.imageWidth
            h = grabber.imageHeight

            storage = ByteBuffer.allocateDirect(grabber.imageWidth * grabber.imageHeight * 3);
            texture = Texture(Texture.TextureSpecification.byte3(unit, w, h, storage, false))

            RunLoop.main.mainLoop.attach(0) {
                update()
            }

        }
    }

    var last: ByteBuffer? = null

    var lastAt = 0L

    fun update() {
        if (System.currentTimeMillis() - lastAt > 1000 / 20.0) {
            val f = webcam!!.grab()
//            println(f)
//            println(webcam!!.frameNumber)
            last = f!!.image[0] as ByteBuffer?
            texture!!.upload(last, false)
            lastAt = System.currentTimeMillis()

        }
    }
}