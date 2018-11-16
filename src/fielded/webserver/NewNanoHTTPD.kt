package fielded.webserver

import org.json.JSONObject
import org.nanohttpd.protocols.http.IHTTPSession
import org.nanohttpd.protocols.http.request.Method
import org.nanohttpd.protocols.http.response.Response
import org.nanohttpd.protocols.http.response.Status.*
import org.nanohttpd.protocols.http.response.Status
import org.nanohttpd.protocols.websockets.CloseCode
import org.nanohttpd.protocols.websockets.NanoWSD
import org.nanohttpd.protocols.websockets.WebSocket
import org.nanohttpd.protocols.websockets.WebSocketFrame
import java.io.*
import java.util.*
import java.io.IOException
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.nio.file.CopyOption
import java.nio.file.Files
import java.nio.file.StandardCopyOption


// experimenting with new version of nanohttpd (which will serve websockets on the same port)
class NewNanoHTTPD(val port: Int) {

    val openWebsockets = mutableListOf<WebSocket>()
    var handlers = mutableListOf<(String, Method, Map<String, String>, Map<String, List<String>>, Map<String, String>) -> Response?>()
    var messageHandlers = mutableListOf<(WebSocket, String, Any) -> Boolean>()
    var htmlFilters = mutableListOf<(String) -> String>()


    fun addDocumentRoot(s: String) {
        handlers.add { uri, _, _, _, _ ->
            val f = File(s + "/" + uri)
            if (f.exists()) {
                serveFile(f)
            } else
                null
        }
    }

    var dynamicRoots = mutableMapOf<String, ()-> String>()

    fun addDynamicRoot(name: String, s: () -> String) {
        dynamicRoots.put(name, s)
    }


    fun serveFile(f: File): Response {

        filterFile(f)?.let {
            return@serveFile it
        }

        val inputStream = BufferedInputStream(FileInputStream(f))
        return Response.newFixedLengthResponse(OK, mimeTypeFor(f), inputStream, f.length())
    }

    fun filterFile(f : File) : Response? {
        if (f.name.endsWith(".html"))
        {
            val fm = File.createTempFile("field_filtered_", ".html")
            val txt = filterFile(f, Files.readString(f.toPath()))

            if (txt!=null)
                return Response.newFixedLengthResponse(OK, mimeTypeFor(f), txt)

        }
        return null
    }

    private fun filterFile(f: File, contents: String): String? {

        var contents = contents
        htmlFilters.forEach {
            contents = it(contents)
        }
        return contents
    }


    fun serveFile(header: Map<String, String>, file: File, mime: String): Response {
        var res: Response
        try {
            // Calculate etag
            val etag = Integer.toHexString((file.absolutePath + file.lastModified() + "" + file.length()).hashCode())

            // Support (simple) skipping:
            var startFrom: Long = 0
            var endAt: Long = -1
            var range: String? = header["range"]
            if (range != null) {
                if (range.startsWith("bytes=")) {
                    range = range.substring("bytes=".length)
                    val minus = range.indexOf('-')
                    try {
                        if (minus > 0) {
                            startFrom = java.lang.Long.parseLong(range.substring(0, minus))
                            endAt = java.lang.Long.parseLong(range.substring(minus + 1))
                        }
                    } catch (ignored: NumberFormatException) {
                    }

                }
            }

            // get if-range header. If present, it must match etag or else we
            // should ignore the range request
            val ifRange = header["if-range"]
            val headerIfRangeMissingOrMatching = ifRange == null || etag == ifRange

            val ifNoneMatch = header["if-none-match"]
            val headerIfNoneMatchPresentAndMatching = ifNoneMatch != null && ("*" == ifNoneMatch || ifNoneMatch == etag)

            // Change return code and add Content-Range header when skipping is
            // requested
            val fileLen = file.length()

            if (headerIfRangeMissingOrMatching && range != null && startFrom >= 0 && startFrom < fileLen) {
                // range request that matches current etag
                // and the startFrom of the range is satisfiable
                if (headerIfNoneMatchPresentAndMatching) {
                    // range request that matches current etag
                    // and the startFrom of the range is satisfiable
                    // would return range from file
                    // respond with not-modified
                    res = Response.newFixedLengthResponse(Status.NOT_MODIFIED, mime, "")
                    res.addHeader("ETag", etag)
                } else {
                    if (endAt < 0) {
                        endAt = fileLen - 1
                    }
                    var newLen = endAt - startFrom + 1
                    if (newLen < 0) {
                        newLen = 0
                    }

                    val fis = FileInputStream(file)
                    fis.skip(startFrom)

                    res = Response.newFixedLengthResponse(PARTIAL_CONTENT, mime, fis, newLen)
                    res.addHeader("Accept-Ranges", "bytes")
                    res.addHeader("Content-Length", "" + newLen)
                    res.addHeader("Content-Range", "bytes $startFrom-$endAt/$fileLen")
                    res.addHeader("ETag", etag)
                }
            } else {

                if (headerIfRangeMissingOrMatching && range != null && startFrom >= fileLen) {
                    // return the size of the file
                    // 4xx responses are not trumped by if-none-match
                    res = Response.newFixedLengthResponse(RANGE_NOT_SATISFIABLE, NanoHTTPD.MIME_PLAINTEXT, "")
                    res.addHeader("Content-Range", "bytes */$fileLen")
                    res.addHeader("ETag", etag)
                } else if (range == null && headerIfNoneMatchPresentAndMatching) {
                    // full-file-fetch request
                    // would return entire file
                    // respond with not-modified
                    res = Response.newFixedLengthResponse(Status.NOT_MODIFIED, mime, "")
                    res.addHeader("ETag", etag)
                } else if (!headerIfRangeMissingOrMatching && headerIfNoneMatchPresentAndMatching) {
                    // range request that doesn't match current etag
                    // would return entire (different) file
                    // respond with not-modified

                    res = Response.newFixedLengthResponse(Status.NOT_MODIFIED, mime, "")
                    res.addHeader("ETag", etag)
                } else {
                    // supply the file
                    res = newFixedFileResponse(file, mime)
                    res.addHeader("Content-Length", "" + fileLen)
                    res.addHeader("ETag", etag)
                }
            }
        } catch (ioe: IOException) {
            res = Response.newFixedLengthResponse(FORBIDDEN, NanoHTTPD.MIME_PLAINTEXT, "FORBIDDEN: ");
        }

        return res
    }

    @Throws(FileNotFoundException::class)
    private fun newFixedFileResponse(file: File, mime: String): Response {
        val res: Response
        res = Response.newFixedLengthResponse(Status.OK, mime, FileInputStream(file), file.length())
        res.addHeader("Accept-Ranges", "bytes")
        return res
    }


    val knownMimeExtensions = mutableMapOf<String, String>("css" to "text/css",
            "js" to "application/javascript",
            "mov" to "video/quicktime",
            "mp4" to "video/mp4",
            "wav" to "audio/wav",
            "aif" to "audio/aiff",
            "aiff" to "audio/aiff",
            "gif" to "image/gif", "jpg" to "image/jpeg", "png" to "image/png", "html" to "text/html")

    fun mimeTypeFor(f: File): String {

        if (f.name.indexOf('.') == -1) return "text/html"

        val suffix = f.name.substring(f.name.lastIndexOf('.') + 1)

        return knownMimeExtensions.getOrDefault(suffix, "text/html")
    }

    val server = object : NanoWSD(port) {
        override fun openWebSocket(p0: IHTTPSession?): WebSocket {

            return object : WebSocket(p0) {
                override fun onOpen() {
                    print("onOpen")
                    openWebsockets.add(this)
                }

                override fun onClose(p0: CloseCode?, p1: String?, p2: Boolean) {
                    print("onClose $p0 $p1 $p2")
                    openWebsockets.remove(this)
                }

                override fun onPong(p0: WebSocketFrame?) {
                    print("pong $p0")
                }

                override fun onMessage(p0: WebSocketFrame?) {
                    print("message $p0")

                    try {
                        val o = JSONObject(p0!!.textPayload)
                        val address = o.getString("address")
                        var payload = o.get("payload")
                        val originalPayload = payload

                        var from = o.get("from")
                        if (payload is JSONObject)
                            payload.put("__originalid__", from)

                        for (v in messageHandlers) {
                            try {
                                if (v(this, address, payload))
                                    return
                            } catch (e: Throwable) {
                                println(" -- exception thrown in message handler code, this is never a good thing --")
                                println(" -- original payload is $payload")
                                e.printStackTrace()
                            }
                        }

                    } catch (e: Throwable) {
                        println(" mallformed message ? $p0")
                        e.printStackTrace()
                    }

                }

                override fun onException(p0: IOException?) {
                    print("exception $p0")
                    p0!!.printStackTrace()
                }

            }
        }

        fun send(message: String) {
            openWebsockets.forEach {
                it.send(message)
            }
        }

        val QUERY_STRING_PARAMETER = "NannoHTTPD.QUERY_STRING_PARAMETER"

        override fun serve(session: IHTTPSession?): Response {

            val files = HashMap<String, String>()
            val method = session!!.getMethod()
            if (Method.PUT == method || Method.POST == method) {
                try {
                    session.parseBody(files)
                } catch (ioe: IOException) {
                    return Response.newFixedLengthResponse(INTERNAL_ERROR, MIME_PLAINTEXT, "SERVER INTERNAL ERROR: IOException: " + ioe.message)
                } catch (re: ResponseException) {
                    return Response.newFixedLengthResponse(re.status, MIME_PLAINTEXT, re.message)
                }

            }

            val parms = session.parameters
            parms.put(QUERY_STRING_PARAMETER, Collections.singletonList(session.getQueryParameterString()))

            val parameters = session.getParameters()

            return if (session.getUri().toString() == "/favicon.ico") Response.newFixedLengthResponse(NOT_FOUND, null, "")
            else serve(session.getUri(), method, session.getHeaders(), parameters, files)

        }


        private fun serve(uri: String, method: Method, headers: Map<String, String>, parms: Map<String, List<String>>, files: Map<String, String>): Response {

            for (h in handlers) {
                val r = h(uri, method, headers, parms, files)
                if (r != null) return r
            }

            return Response.newFixedLengthResponse(NOT_FOUND, null, "Couldn't understand request")
        }

    }

    init {

        server.start()

        handlers.add { uri, _, headers, _, _ ->
            dynamicRoots.entries.stream().map { kv ->
                val f = File(kv.value() + "/" + uri)
                if (f.exists()) {
                    serveFile(headers, f, mimeTypeFor(f))
                } else
                    null
            }.filter { it !=null }.findFirst().orElse(null)
        }

        Thread() {
            while (true) {
                Thread.sleep(1500)
                println("ping")
                openWebsockets.forEach {
                    it.ping(byteArrayOf(0))
                }
            }
        }.start()
    }


}