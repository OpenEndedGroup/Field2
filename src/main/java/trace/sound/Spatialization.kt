package trace.sound

import com.google.common.io.Files
import fielded.webserver.NanoHTTPD
import fielded.webserver.Server
import java.io.File
import java.net.InetAddress
import java.nio.charset.Charset

class Spatialization
{
    val BOOT = "/boot"

    var s: Server

    var id = 0;

    var hostname: String

    init {
        this.s = Server(8092, 8093)

        s.addDocumentRoot(fieldagent.Main.app + "/modules/fieldbox/resources/")
        s.addDocumentRoot(fieldagent.Main.app + "/modules/fielded/resources/")
        s.addDocumentRoot(fieldagent.Main.app + "/modules/fieldcore/resources/")
        s.addDocumentRoot(fieldagent.Main.app + "/lib/web/")
        s.addDocumentRoot(fieldagent.Main.app + "/win/lib/web/")

        s.addDocumentRoot(fieldagent.Main.app + "/win/lib/web/")

        val addr = InetAddress.getLocalHost().address
        val addrs = "${addr[0].toInt() and 255}.${addr[1].toInt() and 255}.${addr[2].toInt()and 255}.${addr[3].toInt()and 255}"
        hostname = "http://"+addrs+":8090/boot"

        s.addURIHandler { uri, method, headers, params, files ->
            if (uri.startsWith(BOOT)) {

                println(" booting up... ")

                var text = Files.toString(File(Main.app + "lib/web/init_remote.html"), Charset.defaultCharset())

                System.out.println(" canonical host name is :"+ InetAddress.getLocalHost().getCanonicalHostName())


                text = text.replace("///IP///", addrs) //!!
                text = text.replace("///ID///", ""+(id++))
                text = text.replace("///WSPORT///", "8091")

                return@addURIHandler NanoHTTPD.Response(NanoHTTPD.Response.Status.OK, "text/html", text)
            }
            null
        }
    }
}