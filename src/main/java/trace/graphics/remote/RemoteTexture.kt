package trace.graphics.remote

import fielded.webserver.NewNanoHTTPD
import org.java_websocket.server.WebSocketServer
import org.nanohttpd.protocols.websockets.WebSocket
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files

class RemoteTexture(val websocket: NewNanoHTTPD, var channel_name: String) {

    fun sendFile(fn: String) {

        if (channel_name.length > 30) {
            channel_name = channel_name.substring(0, 15) + channel_name.hashCode() + channel_name.substring(channel_name.length-2)
        }

        val bytes = Files.readAllBytes(File(fn).toPath())
        val buffer = ByteBuffer.allocate(bytes.size + 2 * 64).order(ByteOrder.nativeOrder());
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
        websocket.openWebsockets.forEach {
            it.send(buffer.array())
        }
        buffer.order(ByteOrder.LITTLE_ENDIAN)
    }
}