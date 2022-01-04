
package trace.graphics.remote

import field.utility.Pair
import fieldagent.Main
import fieldbox.boxes.Box
import fieldbox.execution.Completion
import fieldbox.execution.Errors
import fielded.RemoteEditor
import fielded.webserver.NewNanoHTTPD
import org.json.JSONArray
import org.json.JSONObject
import org.nanohttpd.protocols.http.response.Response.newFixedLengthResponse
import org.nanohttpd.protocols.http.response.Status
import java.io.File
import java.net.InetAddress
import java.nio.charset.Charset
import java.nio.file.Files
import java.util.*
import java.util.function.Consumer
import java.util.function.Function

class RemoteServer {
    var initFile = "init_remote.html" // can swap this to go 'headless'
    val BOOT = "/boot"
    val RESOURCE = "/resource-"
    val MESSAGE = "message"
    val LOG = "log"
    val ERROR = "error"

    var s: NewNanoHTTPD

    var id = 0;

    var hostname: String

    var responseMap = mutableMapOf<String, (JSONObject) -> Boolean>();
    var errorRoot: Box? = null

    val rpc = RPC()

    init {
        this.s = NewNanoHTTPD(8090)


        s.addDocumentRoot(Main.app + "/modules/fieldbox/resources/")
        s.addDocumentRoot(Main.app + "/modules/fielded/resources/")
        s.addDocumentRoot(Main.app + "/modules/fieldcore/resources/")
        s.addDocumentRoot(Main.app + "/lib/web/")
        s.addDocumentRoot(Main.app + "/win/lib/web/")

        val addr = InetAddress.getLocalHost().address
        val addrs = "${addr[0].toInt() and 255}.${addr[1].toInt() and 255}.${addr[2].toInt() and 255}.${addr[3].toInt() and 255}"
        hostname = "http://" + addrs + ":8090/boot"

        s.handlers.add { uri, method, headers, params, files ->
            if (uri.startsWith(BOOT)) {

                println(" booting up... ")

                var text = Files.readString(File(Main.app + "lib/web/$initFile").toPath(), Charset.defaultCharset())

                System.out.println(" canonical host name is :" + InetAddress.getLocalHost().getCanonicalHostName())


                text = text.replace("///IP///", addrs) //!!
                text = text.replace("///ID///", "" + (id++))
                text = text.replace("///WSPORT///", "8090")

                return@add newFixedLengthResponse(Status.OK, "text/html", text)
            } else if (uri.startsWith(RESOURCE)) {
                var found = resourceMap.get(uri)

                if (found == null) {
                    System.out.println("\n\n couldn't find " + uri + " in " + resourceMap + "\n\n");
                    return@add newFixedLengthResponse(Status.NOT_FOUND, "text/html", "not found")
                }

                return@add s.serveFile(File(found!!))
            }
            null
        }
        s.messageHandlers.add { server, address, payload ->
            if (payload is JSONObject) {
                val p = payload as JSONObject
                if (p.has("id")) {
                    val id = p.getString("id")

                    if (id != null)
                        rpc.map(server, id)
                }
                false
            }
            else
            {
                println("Payload was supposed to be JSON was $payload instead?")
                false
            }
        }

        s.messageHandlers.add { server, address, payload ->
            if (address.equals("files.map")) {
                val p = payload as JSONObject

                var f = p.getString("mapFrom");
                var t = p.getString("mapTo");

                if (!f.endsWith("/")) f = f + "/"
                if (!t.endsWith("/")) t = t + "/"

                s.handlers.add { uri, _, headers, _, _ ->
                    if (uri.startsWith(f, ignoreCase = true)) {
                        var q = uri.substring(f.length)
                        val ff = File(File(t), q)

                        if (ff.exists()) {
                            return@add s.serveFile(headers, ff, s.mimeTypeFor(ff))
                        }
                    }
                    null
                }
                true
            }
            false
        }

        s.messageHandlers.add { server, address, payload ->
            if (address.equals("kv.receive")) {
                val p = payload as JSONObject
                rpc.handle(p)
                true
            }
            false
        }

        s.messageHandlers.add { server, address, payload ->

            if (address.equals("log")) {
                println("LOG::(from browser):: $payload")
                return@add true
            } else if (address.equals("error")) {
                println("ERROR::(from browser):: $payload")
                return@add true
            } else if (address.equals("(handler error)")) {
                // need general panic out
                println("general handler error $payload")

                val p = payload as JSONObject
                val line = p.getInt("line");
                var fn = p.getString("filename").replace("box_", "")

                val (name, uid) = fn.split("|")

                errorRoot?.let {
                    Errors.reportError(it, uid, name, null, line, null, """Error thrown in remote event handler: ${p.getString("message")}""")
                }

                return@add true
            } else {
                println("\n::HANDLE:: $address / $payload")

                val o = handlers.remove(address)
                if (o != null) {
                    if (o.invoke(payload as JSONObject)) {
                        println("::: WILL CALL AGAIN :::")
                        handlers.put(address, o)
                    } else {

                        // tmp hack -- we are missing some error messages
                        handlers.put(address, o)
                    }
                } else {
                    println("::: UNHANDLED ::: ${handlers.keys}")

                    (payload as? JSONObject)?.let {
                        if (it.has("kind"))
                        {
                            var k = it.getString("kind")
                            if (k.toLowerCase() == "error")
                            {
                                val oef: Optional<Function<Box, Consumer<Pair<Int, String>>>>? = errorRoot?.first(RemoteEditor.outputErrorFactory)
                                if (oef!=null && oef.isPresent)
                                {
                                    if (it.has("line")) {
                                        oef.get().apply(errorRoot!!).accept(Pair(it.getInt("line"), it.getString("message") + "(on line ${it.getInt("line")})"))
                                    }
                                    else
                                        oef.get().apply(errorRoot!!).accept(Pair(-1, it.getString("message")))
                                }
                            }
                        }
                    }

                    return@add false
                }
                return@add true
            }

            false
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

    val handlers = object : LinkedHashMap<String, (JSONObject) -> Boolean>() {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, (JSONObject) -> Boolean>?): Boolean {
            return size > 100
        }
    }

    @JvmOverloads
    fun execute(s: String,
                filename: String = "unknown", launchable: Boolean = false,
                requiresSandbox: Boolean = true,
                timeStart: Double? = null, timeEnd: Double? = null, handleTo: ((JSONObject) -> Boolean)? = null
    ): Boolean {

        val u = UUID.randomUUID().toString()
        if (handleTo != null)
            handlers.put(u, handleTo)

        val q = JSONObject()
        q.put("code", s)
        q.put("returnTo", u)
        q.put("codeName", filename)
        q.put("launchable", launchable)

        if (!requiresSandbox)
            q.put("noSandbox", true)

        if (timeStart != null)
            q.put("timeStart", timeStart)
        if (timeEnd != null)
            q.put("timeEnd", timeEnd)

        this.s.openWebsockets.forEach {
            it.send(q.toString())
        }

        return this.s.openWebsockets.size > 0
    }

    fun complete(file: String, index: Int, allowExecution: Boolean, success: (List<Completion>) -> Unit) {
        val u = UUID.randomUUID().toString()
        handlers.put(u, {
            println(" return from completion")

            var s = it.getString("message").trim()
            if (s.startsWith("<json>")) s = s.substring("<json>".length);

            var j = JSONArray(s)
            println(j)

            success(j.map {
                println(" recieved " + it)
                println(" of class " + if (it == null) null else it.javaClass)
                val o = it as JSONObject
                val extra = o.getString("title").length - o.getString("add").length

                val extraDoc = o.getString("extraDoc") ?: ""
                println("extraDoc $extraDoc")

                println(extra)
                Completion(index + 1 - extra, index + 1, o.getString("title"), o.getString("from") + " " + (extraDoc));

            }.toList())

            false
        })

        var quoted = JSONObject.quote(file)

        val q = JSONObject()
        q.put("__var0", file)
        q.put("code", "window.completeMe($index, _field.__var0, $allowExecution)")
        q.put("codeName", "((internal))")
        q.put("returnTo", u)

        this.s.openWebsockets.forEach {
            it.send(q.toString())
        }
    }


}