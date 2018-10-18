package trace.graphics.remote

import com.google.common.io.Files
import fieldagent.Main
import fieldbox.boxes.Box
import fieldbox.boxes.Callbacks
import fielded.webserver.NanoHTTPD
import fielded.webserver.Server
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.net.InetAddress
import java.nio.charset.Charset
import java.util.*
import java.util.function.Consumer

class RemoteServer {
    var initFile = "init_remote.html" // can swap this to go 'headless'
    val BOOT = "/boot"
    val RESOURCE = "/resource-"
    val MESSAGE = "message"
    val LOG = "log"
    val ERROR = "error"

    var s: Server

    var id = 0;

    var hostname: String

    var responseMap = mutableMapOf<String, (JSONObject) -> Boolean>();

    init {
        this.s = Server(8090, 8091)

        s.addDocumentRoot(fieldagent.Main.app + "/modules/fieldbox/resources/")
        s.addDocumentRoot(fieldagent.Main.app + "/modules/fielded/resources/")
        s.addDocumentRoot(fieldagent.Main.app + "/modules/fieldcore/resources/")
        s.addDocumentRoot(fieldagent.Main.app + "/lib/web/")
        s.addDocumentRoot(fieldagent.Main.app + "/win/lib/web/")

        val addr = InetAddress.getLocalHost().address
        val addrs = "${addr[0].toInt() and 255}.${addr[1].toInt() and 255}.${addr[2].toInt() and 255}.${addr[3].toInt() and 255}"
        hostname = "http://" + addrs + ":8090/boot"

        s.addURIHandler { uri, method, headers, params, files ->
            if (uri.startsWith(BOOT)) {

                println(" booting up... ")

                var text = Files.toString(File(Main.app + "lib/web/$initFile"), Charset.defaultCharset())

                System.out.println(" canonical host name is :" + InetAddress.getLocalHost().getCanonicalHostName())


                text = text.replace("///IP///", addrs) //!!
                text = text.replace("///ID///", "" + (id++))
                text = text.replace("///WSPORT///", "8091")

                return@addURIHandler NanoHTTPD.Response(NanoHTTPD.Response.Status.OK, "text/html", text)
            } else if (uri.startsWith(RESOURCE)) {
                var found = resourceMap.get(uri)

                if (found == null) {
                    System.out.println("\n\n couldn't find " + uri + " in " + resourceMap + "\n\n");
                    return@addURIHandler NanoHTTPD.Response(NanoHTTPD.Response.Status.NOT_FOUND, null, "not found")
                }

                return@addURIHandler s.server.serveFile(uri, headers, File(found!!), "audio/wav")
            }
            null
        }

        s.addHandlerLast { server, from, address, payload ->

            if (address.equals(MESSAGE)) {
                println(" recieved message from $address to $payload")

                val a = (payload as JSONObject).get("respondTo")
                val hand = responseMap.remove(a)
                if (hand!=null)
                {
                    if (hand.invoke(payload as JSONObject))
                        responseMap.put(a as String, hand)
                }
            }
        }

        s.addHandlerLast { server, from, address, payload ->
            if (address.equals(LOG)) {
                println(" log message from $address to $payload")
            }
        }

        s.addHandlerLast { server, from, address, payload ->
            if (address.equals(ERROR)) {
                println(" error message from $address to $payload")
            }
        }

    }

    fun nowHeadless() {
        initFile = "init_headless.html"
    }

    fun nowHeaded() {
        initFile = "init_remote.html"
    }


    var res: Int = 0

    var resourceMap = mutableMapOf<String, String>()

    fun declareResource(fn: String): String {

        // check for backwards
        resourceMap.entries.forEach {
            if (it.value.equals(fn)) return it.key
        }

        var suffix = ""

        if (!File(fn).exists()) throw IllegalArgumentException(" can't find the file " + fn + " are you sure it exists?")

        if (File(fn).name.contains("."))
            suffix = "." + File(fn).name.split(".").last()

        res++

        var name = "${RESOURCE}${res}${suffix}"

        resourceMap[name] = fn

        return name

    }

    fun execute(s: String) {
        execute(s, null)
    }

    fun execute(s: String, res : ((JSONObject) -> Boolean)? ) {
        this.s.webSocketServer.connections().forEach {

            var c = UUID.randomUUID().toString()

            var m = JSONObject()
            m.put("execute", s)
            m.put("respondTo", c)


            responseMap.put(c, {
                println(" got response $it")
                if (res!=null)
                    return@put res.invoke(it)

                false
            })

            it.send(s)
        }
    }


}