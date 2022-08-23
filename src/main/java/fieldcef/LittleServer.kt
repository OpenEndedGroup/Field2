package fieldcef

import field.utility.Log
import field.utility.Ports
import fieldagent.Main
import fielded.webserver.Server
import org.java_websocket.WebSocket

/**
 * this is a standard replacement for window.cefQuery which has simply stopped working reliably; we still depend on browser.executeJavaScript
 */
class LittleServer() {

    var s: Server

    var initialized = false

    init {
        val a = Ports.nextAvailable(8180)
        val b = Ports.nextAvailable(a + 1)
        this.s = Server(a, b)

        s.addDocumentRoot(Main.app + "/lib/web/")

        s.addHandlerLast(
            { x: String -> x == "initialize" }
        ) { server: Server?, socket: WebSocket?, address: String?, payload: Any ->
            println( " little server has checked in ")
            initialized = true
            payload
        }
    }

    fun addFixedResource(suffix : String, mapsToHTML : String) {
        s.setFixedResource("/$suffix", mapsToHTML)
    }


}