package fielded.webserver

import org.nanohttpd.protocols.http.response.Response

class T_NewNanoHTTPD(val port: Int) {
    var server: NewNanoHTTPD

    init {

        server = NewNanoHTTPD(port)

        server.handlers.add({ address, method, headers, parameters, files ->
            Response.newFixedLengthResponse("Server appears to be serving html just dandy")
        });

        server.messageHandlers.add({ webSocket, s, any ->

            println(" got a message just fine $webSocket $s $any")

            false
        })


    }
}