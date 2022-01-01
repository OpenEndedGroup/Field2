package trace.input

import com.github.sarxos.webcam.Webcam
import com.github.sarxos.webcam.WebcamResolution
import field.app.RunLoop
import field.graphics.Texture
import java.nio.ByteBuffer
import java.util.function.Consumer
import kotlin.system.measureNanoTime


class WebcamDriver2(val unit: Int, val cam : Int = -1) {

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
        var webcam: Webcam? = null

    }

    init {
        if (storage == null) {

            var wc = Webcam.getWebcams()

            // get default webcam and open it
            if (cam==-1)
                webcam = Webcam.getDefault()
            else
                webcam = wc[cam]

            println(" opening webcam ? $webcam")

//            webcam!!.viewSize = WebcamResolution.VGA.size
            webcam!!.open(false)
            println(" opening webcam ? $webcam")

            storage = ByteBuffer.allocateDirect(webcam!!.viewSize.width * webcam!!.viewSize.height * 3);

            w = webcam!!.viewSize.width
            h = webcam!!.viewSize.height

            webcam!!.getImageBytes(storage)

            texture = Texture(Texture.TextureSpecification.byte3(unit, w, h, storage, false))

            RunLoop.main.mainLoop.attach(0) {
                update()
            }
        }
    }


    var lastAt = 0L

    fun update() {
        if (System.currentTimeMillis() - lastAt > 1000 / 20.0 && webcam!!.isImageNew) {
            val st = RunLoop.workerPool.submit {
                val ns = measureNanoTime {
                    webcam!!.getImageBytes(storage)
                }
                print(" imagebytes took ${ns / 1000000.0} ms")
                storage
            }

            RunLoop.main.`when`(st) {
                System.out.println(" triggering update for real")
                texture!!.upload(storage, false)
                lastAt = System.currentTimeMillis()
            }

        }
    }

}