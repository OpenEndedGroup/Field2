package fieldcef.plugins

import field.app.RunLoop
import field.utility.Dict
import field.utility.Log
import field.utility.Pair
import field.utility.Rect
import field.utility.Util.ExceptionlessAutoClosable
import fieldagent.Main
import fieldbox.boxes.Box
import fieldbox.boxes.Box.FunctionOfBoxValued
import fieldbox.boxes.Boxes
import fieldbox.boxes.Drawing
import fieldbox.boxes.Mouse
import fieldbox.boxes.plugins.Templates
import fieldbox.execution.Completion
import fieldbox.execution.Execution
import fieldbox.io.IO.Loaded
import fieldcef.browser.Browser
import fielded.ServerSupport
import fielded.plugins.Out
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.util.*
import java.util.function.Consumer
import java.util.stream.Stream

/**
 * An OutputBox is a box that just lets you print html to it. This is a plugin that lets you make them.
 */
class OutputBox(private val root: Box) : Box(), Loaded {
    var out: Out? = null
    var playlist = Arrays.asList("preamble.js", "jquery-2.1.0.min.js", "jquery.autosize.input.js", "modal.js")
    var styleSheet = "field-codemirror.css"
    var styles: String? = null
    private fun toHTML(param: Any): String {
        return if (out == null) "" + param else out!!.convert(param)
    }

    var tick = 0
    override fun loaded() {
        out = find(Out.__out, both()).findAny().orElseGet { null }
    }

    protected fun make(w: Int, h: Int, b: Box): Browser {
        Log.log("OutputBox.debug") { "initializing browser" }
        val browser = find(Templates.templateChild, both()).findFirst()
            .map { x: BiFunctionOfBoxAnd<String, Box> -> x.apply(b, "html output") }
            .orElseGet { Browser() } as Browser
        val f = b.properties.get(frame)
        val inset = 10f
        browser.properties.put(
            frame,
            Rect((f.x + f.w - inset).toDouble(), (f.y + f.h - inset).toDouble(), w.toDouble(), h.toDouble())
        )
        browser.properties.put(Boxes.dontSave, true)
        b.connect(browser)
        browser.loaded()
        properties.put(Boxes.dontSave, true)
        browser.properties.put(name, "outputbox")
        styles = findAndLoad(styleSheet, false)
        val t = longArrayOf(0)
        boot(browser)
        browser.pauseForBoot()
        val ed: TextEditorExecution = TextEditorExecution(browser)
        ed.connect(browser)
        return browser
    }

    var postamble = "</body></html>"
    fun boot(browser: Browser) {
        val s = this.find(ServerSupport.server, both())
            .findFirst()
            .orElseThrow { IllegalArgumentException(" Server not found ") }
        val bootstrap =
            "<html class='outputbox' style='background:rgba(0,0,0,0.2);padding:8px;'><head><style>$styles</style></head><body class='outputbox' style='background:rgba(0,0,0,0.02);'>$postamble"
        val res = UUID.randomUUID()
            .toString()
        s.setFixedResource("/$res", bootstrap)
        browser.properties.put(Browser.url, "http://localhost:" + s.port + "/" + res)
        tick = 0
        RunLoop.main.loop
            .attach { x: Int ->
                tick++
                if (browser.browser!!.url
                    == "http://localhost:" + s.port + "/" + res
                ) {
                    inject2(browser)
                    //					    try {
//						    Callbacks.call(browser, Callbacks.main, null);
//					    }
//					    catch(Throwable e)
//					    {
//						    e.printStackTrace();
//					    };
                    return@attach false
                }
                Log.log("glassBrowser.boot") { "WAITING url:" + browser.browser!!.url }
                Drawing.dirty(this)
                tick < 100
            }
        Drawing.dirty(this)
    }

    var ignoreHide = 0
    fun inject2(browser: Browser) {
        Log.log("glassbrowser.debug") { "inject 2 is happening" }
        for (s in playlist) {
            Log.log("glassbrowser.debug") { "executing :$s" }
            browser.executeJavaScript(findAndLoad(s, true))
        }
        //		 hide();
        browser.finishBooting()
    }

    private fun findAndLoad(f: String, append: Boolean): String? {
        val roots =
            arrayOf(Main.app + "/modules/fieldcore/resources/", Main.app + "/lib/web/", Main.app + "/win/lib/web/")
        for (s in roots) {
            if (File("$s/$f").exists()) return readFile("$s/$f", append)
        }
        Log.log("glassbrowser.error") { this.javaClass.toString() + " Couldnt' find file in playlist :" + f }
        return null
    }

    private fun selection(): Stream<Box> {
        return breadthFirst(both()).filter { x: Box -> x.properties.isTrue(Mouse.isSelected, false) }
    }

    inner class TextEditorExecution(delegate: Browser) : Execution(null) {
        private val delegate: Browser
        private val queue: List<Runnable>? = null

        override fun support(box: Box, prop: Dict.Prop<String>): ExecutionSupport? {
            return if (box is Browser) null else wrap(box)
        }

        private fun wrap(box: Box): ExecutionSupport {
            return object : ExecutionSupport {
                protected var previousPush: ExceptionlessAutoClosable? = null
                override fun getBinding(name: String): Any? {
                    return null
                }

                override fun executeTextFragment(
                    textFragment: String,
                    suffix: String,
                    success: Consumer<String>,
                    lineErrors: Consumer<Pair<Int, String>>
                ): Any? {
                    delegate.executeJavaScript(textFragment)
                    return null
                }

                override fun executeAll(
                    allText: String,
                    lineErrors: Consumer<Pair<Int, String>>,
                    success: Consumer<String>
                ) {
                    executeTextFragment(allText, "", success, lineErrors)
                }

                override fun begin(
                    lineErrors: Consumer<Pair<Int, String>>,
                    success: Consumer<String>,
                    initiator: Map<String, Any>,
                    killCurrent: Boolean
                ): String? {
                    //TODO
/*
					System.out.println(" WRAPPED (begin)");
					String name = s.begin(lineErrors, success);
					if (name == null) return null;
					Supplier<Boolean> was = box.properties.removeFromMap(Boxes.insideRunLoop, name);
					String newName = name.replace("main.", "editor.");
					box.properties.putToMap(Boxes.insideRunLoop, newName, was);
					box.first(IsExecuting.isExecuting).ifPresent(x -> x.accept(box, newName));

					return name;
					*/
                    return null
                }

                override fun end(lineErrors: Consumer<Pair<Int, String>>, success: Consumer<String>) {
                    //TODO
                }

                override fun setConsoleOutput(stdout: Consumer<String>, stderr: Consumer<String>) {}
                override fun completion(
                    allText: String,
                    line: Int,
                    ch: Int,
                    results: Consumer<List<Completion>>,
                    explicitlyRequested: Boolean
                ) {
//					tern.completion(x -> delegateTo.sendJavaScript(x), "remoteFieldProcess", allText, line, ch);
//					results.accept(Collections.emptyList());
                }

                override fun imports(allText: String, line: Int, ch: Int, results: Consumer<List<Completion>>) {}
                override fun getCodeMirrorLanguageName(): String {
                    return "javascript"
                }

                override fun getDefaultFileExtension(): String {
                    return ".js"
                }
            }
        }

        init {
            this.properties.put(Boxes.dontSave, true)
            this.delegate = delegate
        }
    }

    companion object {
        val output = Dict.Prop<FunctionOfBoxValued<TemplateMap<Browser>>>("output")//.type<Any>()
            .doc<FunctionOfBoxValued<TemplateMap<Browser>>>("`_.output.blah.print('something')` will print to (and, if necessary, create) an html output box")
            .toCanon<FunctionOfBoxValued<TemplateMap<Browser>>>()

        private fun readFile(s: String, append: Boolean): String {
            try {
                BufferedReader(FileReader(File(s))).use { r ->
                    var line = ""
                    while (r.ready()) {
                        line += """
                        ${r.readLine()}
                        
                        """.trimIndent()
                    }
                    if (append) line += "\n//# sourceURL=$s"
                    return line
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return ""
        }
    }

    init {
        properties.put(output, FunctionOfBoxValued { box: Box? ->
            TemplateMap(box, "output", Browser::class.java) { x: Box -> make(400, 300, x) }
                .makeCallable { map: TemplateMap<Browser>, param: Any ->
                    (map.asMap_get("default") as Browser).print(toHTML(param))
                    null
                }
        })
    }
}