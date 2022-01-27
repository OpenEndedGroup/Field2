package auw

class FBufferSource(val s: Any, val f: () -> FBuffer) : _FBuffer {
    override fun get(): FBuffer = f()

    override fun source(): Any = s

    @JvmField
    val length = IO.vectorSize

}