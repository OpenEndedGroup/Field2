package trace.graphics.remote

import org.java_websocket.server.WebSocketServer
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files

class RemoteTexture(val websocket: WebSocketServer, var channel_name: String) {

    fun sendFile(fn: String) {

        if (channel_name.length > 30) {
            channel_name = channel_name.substring(0, 15) + channel_name.hashCode() + channel_name.substring(channel_name.length-2)
        }

        val bytes = Files.readAllBytes(File(fn).toPath())
        val buffer = ByteBuffer.allocateDirect(bytes.size + 2 * 64).order(ByteOrder.nativeOrder());
        buffer.rewind()
        buffer.putInt(bytes.size)
        buffer.putChar('x')
        buffer.put(channel_name.length.toByte())
        buffer.putInt(0)
        for (i in 0 until channel_name.length)
            buffer.putChar(channel_name.get(i))
        buffer.position(2 * 64).put(bytes);
        buffer.rewind()
        buffer.order(ByteOrder.BIG_ENDIAN)
        websocket.connections().forEach {
            it.send(buffer)
        }
        buffer.order(ByteOrder.LITTLE_ENDIAN)
    }
}