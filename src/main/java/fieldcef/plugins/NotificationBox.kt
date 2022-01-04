package fieldcef.plugins


import field.app.RunLoop
import field.graphics.StandardFLineDrawing
import field.linalg.Vec2
import field.utility.Dict
import field.utility.Log
import field.utility.Rect
import fieldagent.Main
import fieldbox.boxes.*
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
import java.util.function.Supplier
import java.util.stream.Stream

/**
 * An OutputBox is a box that just lets you print html to it. This is a plugin that lets you make them.
 */
class NotificationBox(private val root: Box) : Box(), Loaded {
    private var theBox: Browser? = null
    var out: Out? = null
    var playlist = Arrays.asList("preamble.js", "jquery-2.1.0.min.js", "jquery.autosize.input.js", "modal.js")
    var styleSheet = "field-codemirror.css"
    var styles: String? = null
    var tick = 0
    override fun loaded() {
        theBox = make(400, 100)
    }

    protected fun make(w: Int, h: Int): Browser {
        Log.log("OutputBox.debug") { "initializing browser" }
        val browser = Browser()
        properties.put(FLineDrawing.layer, "glass")
        browser.properties.put(FLineDrawing.layer, "glass")
        val bounds = root.first(Drawing.drawing).map { x: Drawing ->
            x.getCurrentViewBounds(
                root
            )
        }.orElseGet { Rect(0.0, 0.0, 500.0, 500.0) }
        val inset = 10f
        browser.properties.put(
            frame,
            Rect(
                (bounds.x + inset).toDouble(),
                (bounds.y + bounds.h - h - inset).toDouble(),
                w.toDouble(),
                h.toDouble()
            )
        )
        browser.properties.put(Boxes.dontSave, true)
        browser.properties.put(Drawing.windowSpace, Vec2(0.0, 1.0))
        root.connect(browser)
        browser.loaded()
        properties.put(Boxes.dontSave, true)
        browser.properties.put(name, "outputbox")
        styles = findAndLoad(styleSheet, false)
        val t = longArrayOf(0)
        boot(browser)
        browser.pauseForBoot()

//		browser.printHTML("<div class='notification'>huh?</div>");
        return browser
    }

    fun print(note: String?) {
        theBox!!.properties.put(StandardFLineDrawing.opacity, 1f)
        theBox!!.printHTML(note!!)
    }

    fun clear() {
        theBox!!.clear()
    }

    var postamble = "</body></html>"
    fun boot(browser: Browser) {
        val s = this.find(ServerSupport.server, both())
            .findFirst()
            .orElseThrow { IllegalArgumentException(" Server not found ") }
        val bootstrap =
            "<html class='outputbox' style='background:rgba(0,0,0,0.2);padding:8px;'><head><style>$styles</style></head><body class='outputbox' style='border-radius: 5px; background:rgba(0,0,0,0.02);'>$postamble"
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
        Log.log("glassbrowser.error") { "Couldnt' find file in playlist :$f" }
        return null
    }

    private fun selection(): Stream<Box> {
        return breadthFirst(both()).filter { x: Box -> x.properties.isTrue(Mouse.isSelected, false) }
    }

    companion object {
        val note = Dict.Prop<Any>("note")//.type<Any>()
            .toCanon<Consumer<String>>().doc<Consumer<String>>("...")
        val clearNote = Dict.Prop<Runnable>("clearNote")//.type<Any>()
            .toCanon<Runnable>().doc<Runnable>("...")
        @JvmStatic
		fun notification(from: Box, html: String) {
            var html = html
            if (!html.endsWith("<br>")) html += "<br>"
            html = "<div class='notification'>$html</div>"
            from.first(note, from.both()).orElse(
                Consumer { x: String? -> System.err.println(x) }).accept(html)
        }

        @JvmStatic
		fun clearNotifications(from: Box) {
            from.first(clearNote, from.both()).orElse(Runnable {}).run()
        }

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
        properties.put(note, Consumer { note: String? -> this.print(note) })
        properties.put(clearNote, Runnable { this.clear() })
        properties.putToMap(Boxes.insideRunLoop, "main.__updateOpacity__", Supplier {
            var next = 1f
            theBox!!.properties.put(
                StandardFLineDrawing.opacity,
                0.999f * theBox!!.properties.getOr(StandardFLineDrawing.opacity) { 1f }
                    .also { next = it })
            if (next < 0.9) theBox!!.properties.put(
                StandardFLineDrawing.opacity,
                0.995f * theBox!!.properties.getOr(StandardFLineDrawing.opacity) { 1f }
                    .also { next = it })
            if (next < 0.5) theBox!!.properties.put(
                StandardFLineDrawing.opacity,
                0.99f * theBox!!.properties.getOr(StandardFLineDrawing.opacity) { 1f }
                    .also { next = it })
            if (next < 0.11f) {
                theBox!!.properties.put(hidden, true)
            } else {
                theBox!!.properties.put(hidden, false)
                Drawing.dirty(this, 1)
            }
            true
        })
    }
}