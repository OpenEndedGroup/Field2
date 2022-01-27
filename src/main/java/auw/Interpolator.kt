package auw


class Interpolator : java.util.function.Function<Float, Any?> {

    @JvmField
    var was: Any? = null
    @JvmField
    var now: Any? = null

    var interpolator: (Float) -> Any? = { null }

    fun next(n: Any?) {
        if (was?.javaClass == now?.javaClass && now?.javaClass == n?.javaClass) {
            was = now
            now = n
            return
        }

        was = now
        now = n

        (now as? FBuffer)?.apply {
            was.let {
                if (it as? FBuffer == null) {
                    previous = silence()
                    was = previous
                } else {
                    previous = it
                }
            }
        }

        if (was == null && now == null)
            interpolator = { null }
        else if (was == null)
            interpolator = { now }
        else if (was is Number && now is Number) {
            interpolator = {
                val N = (now as Number).toFloat()
                N + ((was as Number).toFloat() - N) * it
            }
        }
        else
            interpolator = { now }
    }


    override fun apply(t: Float): Any? {
        return interpolator(t)
    }

}


