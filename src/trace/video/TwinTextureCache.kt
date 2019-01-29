package trace.video

import java.nio.ByteBuffer

import field.graphics.FastJPEG
import field.graphics.Texture

class TwinTextureCache(unit: Int, private val source: ImageCache) {
    private val a: ByteBuffer
    val textureA: Texture
    private val b: ByteBuffer
    val textureB: Texture

    internal var q = FastJPEG()

    internal var playing = false
    var isDirty = false
        internal set

    internal var time: Double = 0.toDouble()
    internal var lastTime = -100.0
    internal var alpha = 0.5

    internal var cA = -1
    internal var cB = -1

    var autoDirectionSet = false

    internal var LEFT = 0
    internal var RIGHT = 0

    init {

        a = ByteBuffer.allocateDirect(source.width * source.height * 3)
        b = ByteBuffer.allocateDirect(source.width * source.height * 3)

        textureA = Texture(Texture.TextureSpecification.byte3(unit, source.width, source.height, a, false, false))
        textureB = Texture(Texture.TextureSpecification.byte3(unit + 1, source.width, source.height, b, false, false))

        textureA.setIsDoubleBuffered(false)
        textureB.setIsDoubleBuffered(false)
    }

    fun getAlpha(): Float {
        return Math.max(0.0, Math.min(1.0, alpha)).toFloat()
    }

    fun setPlaying(p: Boolean) {
        if (p && !playing) {
            source.prerollAndWait(time.toInt())
        }
        this.playing = p
    }

    fun setTime(t: Double) {

        if (t == lastTime)
            return
        if (t.toInt() == lastTime.toInt()) {
            time = t
            return
        }
        if (autoDirectionSet) {
            if (t < lastTime)
                source.setPlaybackDirection(t.toInt(), false)
            else if (t > lastTime)
                source.setPlaybackDirection(t.toInt(), true)
        }
        time = t
        isDirty = true
    }

    fun update() {
        if (isDirty) {
            lastTime = time
            var left = time.toInt()
            var right = left + 1

            println(":::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::: time " + time + " " + left + " " + right + " " + cA + " " + cB + "   " + ImageCache.synchronous)

            if (left == cA) {
                if (right == cB) {
                    isDirty = false
                } else {
                    b.rewind()
                    if (source.copy(right, b)) {
                        b.rewind()
                        cB = right
                        if (ImageCache.synchronous)
                            textureB.forceUploadNow(b)
                        else
                            textureB.upload(b, true)

//                        q.compress("/var/tmp/RIGHT" + pad(RIGHT++) + ".jpg", b, source.width, source.height)


                        isDirty = false
                    } else {
                        println(" failed to load right ")
                    }
                }
            } else if (right == cA) {
                if (left == cB) {
                    isDirty = false
                } else {
                    b.rewind()
                    if (source.copy(left, b)) {
                        b.rewind()
                        cB = left
                        if (ImageCache.synchronous)
                            textureB.forceUploadNow(b)
                        else
                            textureB.upload(b, true)

//                        q.compress("/var/tmp/RIGHT" + pad(RIGHT++) + ".jpg", b, source.width, source.height)

                        isDirty = false
                    } else {
                        println(" failed to load left ")
                    }
                }
            } else if (left == cB) {
                a.rewind()
                if (source.copy(right, a)) {
                    a.rewind()
                    cA = right
                    if (ImageCache.synchronous)
                        textureA.forceUploadNow(a)
                    else
                        textureA.upload(a, true)

//                    q.compress("/var/tmp/LEFT" + pad(LEFT++) + ".jpg", a, source.width, source.height)

                    isDirty = false
                } else {
                    println(" failed to load right ")
                }
            } else if (right == cB) {
                a.rewind()
                if (source.copy(left, a)) {
                    a.rewind()
                    cA = left
                    if (ImageCache.synchronous)
                        textureA.forceUploadNow(a)
                    else
                        textureA.upload(a, true)
                    isDirty = false

//                    q.compress("/var/tmp/LEFT" + pad(LEFT++) + ".jpg", a, source.width, source.height)

                } else {
                    println(" failed to load left ")
                }
            } else {

                // let's do this in order

                println(" double update ")
                if (right > left) {
                    println(" flipping order")
                    val q = right
                    right = left
                    left = q
                }

                var w = false
                b.rewind()
                if (source.copy(right, b)) {
                    b.rewind()
                    cB = right
                    println(" scheduling upload of $cB to right")
                    if (ImageCache.synchronous)
                        textureB.forceUploadNow(b)
                    else
                        textureB.upload(b, true)

//                    q.compress("/var/tmp/RIGHT" + pad(RIGHT++) + ".jpg", b, source.width, source.height)

                } else {
                    w = true
                    println(" right missed it's deadline, will call again")
                }

                a.rewind()
                if (source.copy(left, a)) {
                    a.rewind()
                    cA = left
                    println(" scheduling upload of $cA to left")
                    if (ImageCache.synchronous)
                        textureA.forceUploadNow(a)
                    else
                        textureA.upload(a, true)

//                    q.compress("/var/tmp/LEFT" + pad(LEFT++) + ".jpg", a, source.width, source.height)

                } else {
                    w = true
                    println(" left missed it's deadline, will call again")
                }

                isDirty = w

            }
        }

        if (cA > cB) {
            alpha = 1 - (time - cB)
        } else {
            alpha = time - cA
        }

    }

    companion object {

        fun pad(i: Int): String {
            var r = "" + i
            while (r.length < 5) r = "0" + r
            return r
        }
    }

}
