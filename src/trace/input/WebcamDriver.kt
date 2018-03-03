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
        webcam.viewSize = WebcamResolution.VGA.size
        webcam.open(true)

        storage = ByteBuffer.allocateDirect(webcam.viewSize.width*webcam.viewSize.height*3);

        w = webcam.viewSize.width
        h = webcam.viewSize.height

        webcam.getImageBytes(storage)

        texture = Texture(Texture.TextureSpecification.byte3(6, w, h, storage, false))
    }


    fun update() {
        webcam.getImageBytes(storage)
        texture.upload(storage, true)
    }

}