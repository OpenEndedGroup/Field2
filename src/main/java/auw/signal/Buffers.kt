package auw.signal

object Buffers {

    val loaded = mutableMapOf<String, Buffer>()

    fun buffer(f: String): Buffer {
        return loaded.computeIfAbsent(f, { Buffer(f) })
    }
}