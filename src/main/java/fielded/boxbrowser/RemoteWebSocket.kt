package fielded.boxbrowser

import fielded.webserver.Server

import java.io.IOException

class RemoteWebSocket @Throws(IOException::class)
constructor(val httpPort: Int, val websocketPort: Int) {

    val s: Server

    val messages = mutableMapOf<String, MutableList<String>>()

    init {
        this.s = Server(httpPort, websocketPort)

        s.addHandlerLast { server, socket, address, payload ->
            println("message from $socket is $payload")
            payload
        }

    }

    fun send(v: String) {
        s.broadcast(v)
    }

}
