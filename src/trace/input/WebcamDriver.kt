package trace.input

import com.github.sarxos.webcam.Webcam
import com.github.sarxos.webcam.WebcamResolution
import field.graphics.Texture
import java.nio.ByteBuffer


class WebcamDriver {
    var w: Int
    var h: Int

    var storage: ByteBuffer
    val texture: Texture
    var webcam: Webcam

    init {
        // get default webcam and open it
        webcam = Webcam.getDefault()

        println(" opening webcam ? $webcam")

        webcam.viewSize = WebcamResolution.VGA.size
        webcam.open(false)
        println(" opening webcam ? $webcam")

        storage = ByteBuffer.allocateDirect(webcam.viewSize.width * webcam.viewSize.height * 3);

        w = webcam.viewSize.width
        h = webcam.viewSize.height

        webcam.getImageBytes(storage)

        texture = Texture(Texture.TextureSpecification.byte3(0, w, h, storage, false))
    }

    var last = 0L

    fun update() {
        if (System.currentTimeMillis() - last > 1000 / 30) {
            webcam.getImageBytes(storage)
            texture.upload(storage, true)
            last = System.currentTimeMillis()
        }
    }

}