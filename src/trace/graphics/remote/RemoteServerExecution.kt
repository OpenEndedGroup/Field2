package trace.graphics.remote

import field.graphics.Shader
import field.graphics.util.onsheetui.Label
import field.linalg.Vec4
import field.utility.*
import fieldbox.boxes.Box
import fieldbox.boxes.Boxes
import fieldbox.boxes.Drawing
import fieldbox.boxes.TimeSlider
import fieldbox.boxes.plugins.Chorder
import fieldbox.boxes.plugins.GraphicsSupport
import fieldbox.boxes.plugins.IsExecuting
import fieldbox.execution.Completion
import fieldbox.execution.Execution
import fieldbox.io.IO
import fieldcef.plugins.up
import fielded.Commands
import fielded.RemoteEditor
import fielded.live.Asta
import org.json.JSONObject
import org.nanohttpd.protocols.websockets.WebSocket
import trace.graphics.Stage

import java.net.InetAddress
import java.util.*
import java.util.function.BiConsumer
import java.util.function.Consumer
import java.util.function.Supplier

class RemoteServerExecution : Execution(null) {
    val server: RemoteServer

    private var labelMaker: Optional<IdempotencyMap<String>>? = null

    var time: TimeSlider? = null
    var timeWas = -1.0;
    var wasRunning = false;

    init {

        server = Stage.rs
        server.errorRoot = this

        this.properties.putToMap(Boxes.insideRunLoop, "main.copyTime", Supplier<Boolean> {
            if (time == null)
                find(TimeSlider.time, both()).findFirst().ifPresent { time = it }

            time?.let {
                val z = it.properties.get(Box.frame).x.toDouble();
                val r = it.properties.get(TimeSlider.isRunning)
                if (timeWas != z || r != wasRunning) {
                    timeWas = z
                    wasRunning = r
                    sendTime(z, r)
                }
            }

            true
        })

        var last = System.currentTimeMillis()
        this.properties.putToMap(Boxes.insideRunLoop, "main.copyTopology", Supplier<Boolean> {

            if ((System.currentTimeMillis() - last) > 1000) {
                last = System.currentTimeMillis()
                broadcastTopology()
            }

            true
        })



        this.properties.putToMap(Boxes.insideRunLoop, "main.checkConnections", Supplier<Boolean> {

            val l = server.s.openWebsockets.size

            var descrption = {
                if (l == 0)
                    "No connected web-browsers, connect to " + hostname()?.replace("/boot", "/ar.html")
                else {
                    var description = "$l connection" + (if (l == 1) "" else "s")

                    if (l > 0) {
                        description += " from :"
                        server.s.openWebsockets.map {
                            description += "${it.handshakeRequest.remoteHostName} = ${it.handshakeRequest.remoteIpAddress} "
                        }
                    }

                    description
                }
            }

            if (labelMaker == null || !labelMaker!!.isPresent) {
                labelMaker = first(Label.label, both()).map {
                    it.apply(this)
                }

            }

            labelMaker!!.ifPresent {

                val was = it.get("s")

                if (was == null || !was.equals(descrption())) {
                    it.put("s", descrption())
                    Drawing.dirty(this)
                }

            }

            true
        })

        server.s.messageHandlers.add { server, address, payload ->
            if (address.equals("util.begin")) {
                val p = payload as JSONObject

                findBoxByName(p.getString("name")).ifPresent {
                    (it up Chorder.begin)!!.apply(it)
                }
                true
            } else if (address.equals("util.end")) {
                val p = payload as JSONObject

                findBoxByName(p.getString("name")).ifPresent {
                    (it up Chorder.end)!!.apply(it)
                }

                true
            } else
                false
        }

        server.s.messageHandlers.add { server, address, payload ->
            if (address.equals("request.shaderSupport")) {
                val p = payload as JSONObject

                var v = p.getString("defaultVertex");
                var f = p.getString("defaultFragment");
                var id = p.getString("boxID");
                var callback = p.getString("reloadCallback");

                initializeShaderSupport(id, v, f, callback);
                true
            } else
                false
        }
        server.s.messageHandlers.add { server, address, payload ->
            if (address.equals("status.shaderSupport")) {
                val p = payload as JSONObject

                var ty = p.getString("type")
                var id = p.getString("boxID");
                if (ty == "success") {
                    val e = GraphicsSupport.errorHandler(findBoxByID(id).get(), "browser shader");
                    e.noError()
                } else if (ty == "error") {
                    val e = GraphicsSupport.errorHandler(findBoxByID(id).get(), "browser shader");
                    var m = p.getString("message")
                    if (p.getString("v").length > 0) m += "\n on vertex shader '" + p.getString("v") + "'"
                    if (p.getString("f").length > 0) m += "\n on fragment shader '" + p.getString("f") + "'"
                    e.errorOnLine(1, m)
                }

                true
            } else
                false
        }


        properties.put(Commands.commands, Supplier<Map<Pair<String, String>, Runnable>> {
            val m = LinkedHashMap<Pair<String, String>, Runnable>()
            val ed = this.find(RemoteEditor.editor, both()).findFirst().get()

            val box = ed.currentlyEditing


            val s = knownShaderBoxes.get(box)

            if (s != null)
                m[Pair("Reload shader (browser)", "Reloads all shaders associated with this box via `_.newShader()`")] = Runnable {
                    reloadBoxShader(s, box.properties.get(GraphicsSupport.vertex), box.properties.get(GraphicsSupport.fragment))
                }


            m
        })
    }

    val knownShaderBoxes = mutableMapOf<Box, String>();

    fun initializeShaderSupport(id: String, v: String, f: String, callback: String) {

        this.findBoxByID(id).ifPresent {
            val v2 = it.properties.computeIfAbsent(GraphicsSupport.vertex, { v });
            val f2 = it.properties.computeIfAbsent(GraphicsSupport.fragment, { f });

            if (v2 != v || f2 != f) {
                reloadBoxShader(callback, v2, f2);
            }
            knownShaderBoxes.put(it, callback)


        }
    }

    private fun reloadBoxShader(callback: String, v2: String?, f2: String?) {
        server.execute("__reloadShader(`" + callback + "`, `" + v2 + "`, `" + f2 + "`)", requiresSandbox = false)

    }

    protected fun findBoxByID(uid: String): Optional<Box> {
        return breadthFirst(both()).filter { x -> Util.safeEq(x.properties.getOrConstruct(IO.id), uid) }
                .findFirst()
    }

    protected fun findBoxByName(name: String): Optional<Box> {
        return breadthFirst(both()).filter { x -> Util.safeEq(x.properties.get(Box.name), name) }
                .findFirst()
    }

    var lastHostName: String? = null
    var hostNameCheckedAt = System.currentTimeMillis()

    private fun hostname(): String? {

        if (lastHostName == null || System.currentTimeMillis() - hostNameCheckedAt > 5000) {
            val addr = InetAddress.getLocalHost().address
            val addrs = "${addr[0].toInt() and 255}.${addr[1].toInt() and 255}.${addr[2].toInt() and 255}.${addr[3].toInt() and 255}"
            val hostname = "http://" + addrs + ":8090/boot"
            hostNameCheckedAt = System.currentTimeMillis()
            lastHostName = hostname
            return hostname
        } else
            return lastHostName
    }

    fun addDynamicRoot(name: String, router: () -> String) {
        server.s.addDynamicRoot(name, router)
    }

    private fun sendTime(t: Double, running: Boolean) {

        server.execute("__setTime($t, $running)", requiresSandbox = false)

    }

    val asta = Asta()


    override fun support(box: Box, prop: Dict.Prop<String>): Execution.ExecutionSupport? {
        if (box == this) return null
        if (prop != Execution.code) return null;

        val ef = this.properties.get(Execution.executionFilter)
        return if (ef == null || ef.apply(box)) wrap(box, prop) else null
    }


    private fun wrap(box: Box, prop: Dict.Prop<String>): Execution.ExecutionSupport {

        return object : Execution.ExecutionSupport {

            var lineOffset = 0

            internal var uniq: Long = 0

            override fun executeTextFragment(textFragment: String, suffix: String, success: Consumer<String>, lineErrors: Consumer<Pair<Int, String>>): Any? {
                var textFragment = textFragment
                RemoteEditor.removeBoxFeedback(Optional.of(box), "__redmark__")

                try {
                    Execution.context.get().push(box)

                    try {
                        textFragment = asta.transformer(box).transform(textFragment, false).first
                    } catch (e: Exception) {
                        System.out.println(" -- ASTA didn't succeed, usually a syntax error of some kind")
                        e.printStackTrace()
                    }

                    for (i in 0 until lineOffset)
                        textFragment = "\n" + textFragment

                    val name = "main._animatorJS_"

                    server.execute(textFragment, target(box)!!, false) { o ->

                        val k = o.getString("kind")
                        if (k.equals("success", ignoreCase = true)) {

                            if (o.getString("message").equals("ok", ignoreCase = true))
                                success.accept(" &#10003; ")
                            else
                                success.accept(o.getString("message"))
                        } else if (k.equals("run-start", ignoreCase = true)) {
                            val s1 = box.properties.getOrConstruct(Boxes.insideRunLoop).size
                            box.properties.putToMap(Boxes.insideRunLoop, name + o.getString("altName"), Supplier<Boolean> { true })
                            val s2 = box.properties.getOrConstruct(Boxes.insideRunLoop).size

//                            if (s2>s1)
                                box.first<BiConsumer<Box, String>>(IsExecuting.isExecuting)
                                        .ifPresent { x -> x.accept(box, name + o.getString("altName")) }
                            Drawing.dirty(box)

                        } else if (k.equals("run-stop", ignoreCase = true)) {
                            box.properties.removeFromMap(Boxes.insideRunLoop, name + o.getString("altName"))
                            Drawing.dirty(box)

                            return@execute false
                        } else {
                            RemoteEditor.boxFeedback(Optional.of(box), Vec4(1.0, 0.0, 0.0, 0.5), "__redmark__", -1, -1)
                            lineErrors.accept(Pair(o.getInt("line"), o.getString("message")))
                        }

//                        false
                        true
                    }.ifFalse {
                        lineErrors.accept(Pair(-1, "DID NOT EXECUTE ANYWHERE - please connect a webbrowser"))
                    }
                    RemoteEditor.boxFeedback(Optional.of(box), Vec4(0.3, 0.7, 0.3, 0.5))


                    return null
                } finally {
                    Execution.context.get().pop()
                }
            }

            private fun target(box: Box): String? {
                return "box_" + box.properties.get(Box.name) + "|" + box.properties.getOrConstruct(IO.id);
            }


            override fun getBinding(name: String): Any? {
                return null
            }

            override fun executeAll(allText: String, lineErrors: Consumer<Pair<Int, String>>, success: Consumer<String>) {
                executeTextFragment(allText, "", success, lineErrors)
            }

            override fun begin(lineErrors: Consumer<Pair<Int, String>>, success: Consumer<String>, initiator: Map<String, Any>, endOthers: Boolean): String? {
                //				if (endOthers) end(lineErrors, success);
                RemoteEditor.removeBoxFeedback(Optional.of(box), "__redmark__")

                try {
                    Execution.context.get().push(box)

                    val name = "main._animatorJS_"

                    var code = box.properties.get(Execution.code)!!

                    try {
                        code = asta.transformer(box).transform(code, true).first
                    } catch (e: Exception) {
                        System.out.println(" -- ASTA didn't succeed, usually a syntax error of some kind")
                        e.printStackTrace()
                    }
                    server.execute(code, target(box)!!, true, true,
                            timeStart = box.properties.get(Box.frame).x.toDouble(),
                            timeEnd = box.properties.get(Box.frame).xw.toDouble()) { o ->

                        val k = o.getString("kind")
                        if (k.equals("success", ignoreCase = true)) {
                            if (o.getString("message").equals("ok", ignoreCase = true))
                                success.accept(" &#10003; ")
                            else
                                success.accept(o.getString("message"))
                        } else if (k.equals("error", ignoreCase = true)) {
                            RemoteEditor.boxFeedback(Optional.of(box), Vec4(1.0, 0.0, 0.0, 0.5), "__redmark__", -1, -1)
                            lineErrors.accept(Pair(o.getInt("line"), o.getString("message")))
                        } else if (k.equals("run-start", ignoreCase = true)) {
                            val s1 = box.properties.getOrConstruct(Boxes.insideRunLoop).size
                            box.properties.putToMap(Boxes.insideRunLoop, name + o.getString("altName"), Supplier<Boolean> { true })
                            val s2 = box.properties.getOrConstruct(Boxes.insideRunLoop).size

//                            if (s2==1)
                                box.first<BiConsumer<Box, String>>(IsExecuting.isExecuting)
                                        .ifPresent { x -> x.accept(box, name + o.getString("altName")) }
                            Drawing.dirty(box)

                        } else if (k.equals("run-stop", ignoreCase = true)) {
                            box.properties.removeFromMap(Boxes.insideRunLoop, name + o.getString("altName"))
                            Drawing.dirty(box)

                            return@execute false
                        }

                        true
                    }
                    RemoteEditor.boxFeedback(Optional.of(box), Vec4(0.3, 0.7, 0.3, 0.5))


                    return null
                } finally {
                    Execution.context.get().pop()
                }

            }

            override fun end(lineErrors: Consumer<Pair<Int, String>>, success: Consumer<String>) {
                RemoteEditor.removeBoxFeedback(Optional.of(box), "__redmark__")

                server.execute("if (__tasks[\"" + target(box) + "\"]) __tasks[\"" + target(box) + "\"].stop()", target(box)!!, true) { o ->
                    println(" got feedback from ending something ")
                    println(o)
                    false
                }

                RemoteEditor.boxFeedback(Optional.of(box), Vec4(0.3, 0.7, 0.3, 0.5))
                Drawing.dirty(box)
            }

            override fun setConsoleOutput(stdout: Consumer<String>, stderr: Consumer<String>) {


            }

            override fun completion(allText: String, line: Int, ch: Int, results: Consumer<List<Completion>>, explicit: Boolean) {

                server.complete(allText, indexFor(allText, line, ch), explicit) { completions ->
                    results.accept(completions)
                    null
                }

            }

            override fun imports(allText: String, line: Int, ch: Int, results: Consumer<List<Completion>>) {}

            override fun getCodeMirrorLanguageName(): String {
                return "javascript"
            }

            override fun getDefaultFileExtension(): String {
                return ".js"
            }

            override fun setLineOffsetForFragment(lineOffset: Int, origin: Dict.Prop<String>) {
                this.lineOffset = lineOffset
            }
        }
    }

    private fun indexFor(allText: String, line: Int, ch: Int): Int {
        var line = line
        var ch = ch

        val oline = line
        val och = ch

        for (i in 0 until allText.length) {
            if (allText[i] == '\n') {
                line--
            } else if (line == 0) {
                ch--
            }
            if (line == 0 && ch == 0) return i
        }
        println(" warning, couldn't find :$oline $och in >$allText")
        return 0

    }

    fun broadcastTopology() {
        val pairs = mutableSetOf<kotlin.Pair<String, String>>()

        this.breadthFirstAll(downwards()).forEach {
            var a = it.properties.getOrConstruct(IO.id)
            if (it == this)
                a = "root"

            it.children().forEach {
                var b = it.properties.getOrConstruct(IO.id)
                if (it == this)
                    b = "root"
                pairs.add(a to b)


            }

        }

        val command = "__clearEdges();" + pairs.joinToString(separator = "") {
            if (it.first.length > 0 && it.second.length > 0) "__addEdge('${it.first}', '${it.second}'); " else ""
        }

//        println(" -- updating topology :" + command)
        server.execute(command, requiresSandbox = false)

    }
}

private fun Boolean.ifFalse(a: () -> Unit): Boolean {
    if (!this) a()
    return this
}

private fun Boolean.ifTrue(a: () -> Unit): Boolean {
    if (this) a()
    return this
}

