package trace.graphics.remote

import com.google.common.io.Files
import fieldagent.Main
import fieldbox.boxes.Box
import fieldbox.boxes.Callbacks
import fielded.webserver.NanoHTTPD
import fielded.webserver.Server
import java.io.File
import java.io.FileInputStream
import java.net.InetAddress
import java.nio.charset.Charset

class RemoteServer
{
    val BOOT = "/boot"

    var s: Server

    var id = 0;

    init {
        this.s = Server(8090, 8091)

        s.addDocumentRoot(fieldagent.Main.app + "/modules/fieldbox/resources/")
        s.addDocumentRoot(fieldagent.Main.app + "/modules/fielded/resources/")
        s.addDocumentRoot(fieldagent.Main.app + "/modules/fieldcore/resources/")
        s.addDocumentRoot(fieldagent.Main.app + "/lib/web/")
        s.addDocumentRoot(fieldagent.Main.app + "/win/lib/web/")

        s.addURIHandler { uri, method, headers, params, files ->
            if (uri.startsWith(BOOT)) {

                println(" booting up... ")

                var text = Files.toString(File(Main.app + "lib/web/init_remote.html"), Charset.defaultCharset())

                System.out.println(" canonical host name is :"+InetAddress.getLocalHost().getCanonicalHostName())

                text = text.replace("///IP///", InetAddress.getLocalHost().getCanonicalHostName()) //!!
                text = text.replace("///ID///", ""+(id++))
                text = text.replace("///WSPORT///", "8091")

                return@addURIHandler NanoHTTPD.Response(NanoHTTPD.Response.Status.OK, "text/html", text)
            }
            null
        }
    }

}