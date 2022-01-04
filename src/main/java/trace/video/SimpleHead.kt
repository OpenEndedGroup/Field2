package trace.video

import java.io.File

class SimpleHead {
    class Frame {
        @JvmField
        var vx: Float = 0f

        @JvmField
        var vy: Float = 0f

        @JvmField
        var brightness: Float = 0f

        @JvmField
        var red: Float = 0f

        @JvmField
        var green: Float = 0f

        @JvmField
        var blue: Float = 0f

        @JvmField
        var meanv_x: Float = 0f

        @JvmField
        var meanv_y: Float = 0f

        @JvmField
        var meanb_x: Float = 0f

        @JvmField
        var meanb_y: Float = 0f

        override fun toString(): String {
            return "Frame(vx=$vx, vy=$vy, brightness=$brightness, red=$red, green=$green, blue=$blue, meanv_x=$meanv_x, meanv_y=$meanv_y, meanb_x=$meanb_x, meanb_y=$meanb_y)"
        }

    }

    fun readLine(l: String): Frame {
        val pieces = l.split(" ")
        val f = Frame()
        f.vx = pieces[0].toFloat()
        f.vy = pieces[1].toFloat()
        f.brightness = pieces[2].toFloat()
        f.red = pieces[3].toFloat()
        f.green = pieces[4].toFloat()
        f.blue = pieces[5].toFloat()
        f.meanv_x = pieces[6].toFloat()
        f.meanv_y = pieces[7].toFloat()
        f.meanb_x = pieces[8].toFloat()
        f.meanb_y = pieces[9].toFloat()
        return f
    }

    companion object {

        @JvmStatic
        var head: SimpleHead? = null

        @JvmStatic
        var now: Frame? = null

        var started: Boolean = false

        @JvmStatic
        fun start(path: String) {
            if (started) return

            if (!File(path).exists()) throw IllegalArgumentException(" can't find the path '${path}', typo?")
            if (!File(path + "/simpleHead").exists()) throw IllegalArgumentException(" can't find the binary inside'${path}', is this the correct directory?")

            val b = ProcessBuilder()
            b.command(path + "/simpleHead").redirectErrorStream(true)
            val p = b.start()
            val reader = p.inputStream.bufferedReader()

            head = SimpleHead()

            Thread {
                started = true

                while (true) {
                    try {
                        val rr = reader.readLine()
                        print("r " + rr)
                        if (rr != null)
                            now = head!!.readLine(rr)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Thread.sleep(5)
                    }
                }
            }.start()

        }

        @JvmStatic
        fun reader(path: String): (Double) -> Frame {

            val h = SimpleHead()
            val lines = File(path).readLines().map { h.readLine(it) }

            return { time ->

                val left = Math.max(0.0, Math.min(lines.size - 1.0, time * lines.size)).toInt()
                val right = Math.max(0.0, Math.min(lines.size - 1.0, time * lines.size + 1)).toInt()
                val alpha = time * lines.size - left

                val fl = lines[left]
                val fr = lines[right]

                blend(fl, fr, alpha)

            }

        }

        private fun blend(a: Frame, b: Frame, alpha: Double): Frame {
            val r = Frame()

            r.vx = (a.vx * (1 - alpha) + alpha * b.vx).toFloat()
            r.vy = (a.vy * (1 - alpha) + alpha * b.vy).toFloat()
            r.brightness = (a.brightness * (1 - alpha) + alpha * b.brightness).toFloat()
            r.red = (a.red * (1 - alpha) + alpha * b.red).toFloat()
            r.green = (a.green * (1 - alpha) + alpha * b.green).toFloat()
            r.blue = (a.blue * (1 - alpha) + alpha * b.blue).toFloat()
            r.meanv_x = (a.meanv_x * (1 - alpha) + alpha * b.meanv_x).toFloat()
            r.meanv_y = (a.meanv_y * (1 - alpha) + alpha * b.meanv_y).toFloat()
            r.meanb_x = (a.meanb_x * (1 - alpha) + alpha * b.meanb_x).toFloat()
            r.meanb_y = (a.meanb_y * (1 - alpha) + alpha * b.meanb_y).toFloat()

            return r
        }

    }

}