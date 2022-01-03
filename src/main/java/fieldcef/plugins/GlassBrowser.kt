package fieldcef.plugins

import fieldbox.io.IO.Loaded
import fieldcef.browser.Browser
import field.app.RunLoop
import field.graphics.Scene.Perform
import field.graphics.Window
import field.utility.Dict
import field.utility.Log
import field.utility.Rect
import fieldagent.Main
import fieldbox.boxes.*
import fieldbox.boxes.Keyboard.OnKeyDown
import org.lwjgl.glfw.GLFW
import fielded.ServerSupport
import java.lang.IllegalArgumentException
import java.lang.Runnable
import java.lang.InterruptedException
import org.json.JSONObject
import fielded.RemoteEditor.ExtendedCommand
import fieldbox.ui.FieldBoxWindow
import java.io.File
import fieldcef.plugins.GlassBrowser
import fielded.Commands
import java.io.BufferedReader
import java.io.FileReader
import java.io.IOException
import java.util.*
import java.util.function.Consumer
import java.util.stream.Stream

/**
 * This is a browser, created by default, that covers the whole window in the glass layer. We can then either center it in the window, or crop into it.
 */
class GlassBrowser(root: Box) : Box(), Loaded {
    private val root: Box
    var playlist = Arrays.asList("preamble.js", "jquery-2.1.0.min.js", "jquery.autosize.input.js", "modal.js")
    var styleSheet = "field-codemirror.css"

    // we'll need to make sure that this is centered on larger screens
    var maxw = 650
    var maxh = 800
    var browser: Browser? = null
    var styles: String? = null
    var tick = 0
    var commandHelper = Commands()
    override fun loaded() {

        Log.log("glassbrowser.debug") { "initializing browser" }
        browser = Browser()
        browser!!.properties.put(frame, Rect(0.0, 0.0, maxw.toDouble(), maxh.toDouble()))
        browser!!.properties.put(FLineDrawing.layer, "glass2")
        //		browser.properties.put(Drawing.windowSpace, new Vec2(0,0));
        browser!!.properties.put(Boxes.dontSave, true)
        browser!!.properties.put(hidden, true)
        browser!!.properties.put(Mouse.isSticky, true)
        browser!!.properties.put(FrameManipulation.lockHeight, true)
        browser!!.properties.put(FrameManipulation.lockY, true)
        browser!!.connect(root)
        browser!!.loaded()
        properties.put(Boxes.dontSave, true)
        styles = findAndLoad(styleSheet, false)
        browser!!.properties.put(name, "GLASS")

//		boot();
        // we've been having an incredibly hard time tracking down a problem on OS X where sometimes the CefSystem will fail to initialize the browser.
        val t = longArrayOf(0)
        RunLoop.main.loop
            .attach { x: Int ->
                if (t[0] == 0L) t[0] = System.currentTimeMillis()
                if (System.currentTimeMillis() - t[0] > 1000) {
                    System.out.println(" -- going to boot the glass browser")
                    boot()
                    return@attach false
                }
                true
            }


        // I've been looking forward to this for a while
        properties.putToMap(
            Keyboard.onKeyDown,
            "__glassbrowser__",
            OnKeyDown { e: Window.Event<Window.KeyboardState>, k: Int ->
                if (!e.properties.isTrue(Window.consumed, false)) {
                    if (e.after.keysDown.contains(GLFW.GLFW_KEY_SPACE) && e.after.isControlDown && !e.before.keysDown.contains(
                            GLFW.GLFW_KEY_SPACE
                        )
                    ) {
//					if (!visible)
                        run {
                            center()
                            runCommands()
                        }
                    }
                }
                null
            })
    }

    fun boot() {
        val s = this.find(ServerSupport.server, both())
            .findFirst()
            .orElseThrow { IllegalArgumentException(" Server not found ") }
        val bootstrap =
            "<html style='background:rgba(0,0,0,0.02);'><head><style>$styles</style></head><body class='CodeMirror' style='background:rgba(0,0,0,0.02);'></body></html>"
        val res = UUID.randomUUID()
            .toString()
        s.setFixedResource("/$res", bootstrap)
        browser!!.properties.put(Browser.url, "http://localhost:" + s.port + "/" + res)
        tick = 0
        RunLoop.main.loop
            .attach { x: Int ->
                tick++
                if (browser!!.browser!!.url
                        .startsWith("http://localhost:" + s.port + "/" + res)
                ) {
                    RunLoop.main.delayTicks({ inject2() }, 4)
                    //					inject2();
                    return@attach false
                }
                Log.log("glassBrowser.boot") { "WAITING url:" + browser!!.browser!!.url }
                Drawing.dirty(this)
                if (tick % 200 == 0) {
                    Log.log("glassBrowser.boot") { "trying to dislodge" }
                    browser!!.properties.put(Browser.url, "http://localhost:" + s.port + "/" + res + "?t=" + tick)
                }
                true
            }
        Drawing.dirty(this)
    }

    var ignoreHide = 0
    fun inject2() {
        Log.log("glassbrowser.debug") { "inject 2 is happening" }
        for (s in playlist) {
            Log.log("glassbrowser.debug") { "executing :$s" }
            browser!!.executeJavaScript(findAndLoad(s, true))
            try {
                Thread.sleep(200)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
        hide()
        browser!!.addHandler(
            { x: String -> x == "focus" },
            Browser.Handler { address: String?, payload: JSONObject?, ret: Consumer<String?>? ->
                if (ignoreHide > 0) ignoreHide-- else hide()
                ret?.accept("OK")
            })
        browser!!.addHandler(
            { x: String -> x == "request.commands" },
            Browser.Handler { address: String?, paylod: JSONObject?, ret: Consumer<String?>? ->
                commandHelper.requestCommands(
                    Optional.of(
                        selection().findFirst()
                            .orElse(root)
                    ), null, null, ret, -1, -1
                )
            })
        browser!!.addHandler(
            { x: String -> x == "call.command" },
            Browser.Handler { address: String?, payload: JSONObject?, ret: Consumer<String?>? ->
                val command = payload!!.getString("command")
                val r = commandHelper.callTable[command]
                if (r != null) {
                    if (r is ExtendedCommand) r.begin(commandHelper.supportsPrompt { x: String ->
                        Log.log("glassbrowser.debug") { "continue commands $x" }
                        browser!!.executeJavaScript("continueCommands(JSON.parse('$x'))")
                        ignoreHide = 4
                        show()
                    }, null)
                    r.run()
                }
                ret?.accept("OK")
            })
        browser!!.addHandler(
            { x: String -> x == "call.alternative" },
            Browser.Handler { address: String?, payload: JSONObject?, ret: Consumer<String?>? ->
                val command = payload!!.getString("command")
                val text = payload!!.getString("text")
                val r: Runnable? = commandHelper.callTable_alternative
                if (r != null) {
                    if (r is ExtendedCommand) r.begin(commandHelper.supportsPrompt { x: String ->
                        Log.log("glassbrowser.debug") { "continue commands $x" }
                        browser!!.executeJavaScript("continueCommands(JSON.parse('$x'))")
                        ignoreHide = 4
                        show()
                    }, text)
                    r.run()
                }
                ret?.accept("OK")
            })
        browser!!.finishBooting()
    }

    var visible = false
    fun show() {
        val d = first(Drawing.drawing, both()).orElse(null)
        val safety = 500f
        val viewBounds = d!!.getCurrentViewBounds(this)
        val vb = Rect(
            (viewBounds.x + viewBounds.w / 2 - maxw / 2).toDouble(),
            (viewBounds.y + viewBounds.h / 2 - maxh / 2).toDouble(),
            maxw.toDouble(),
            maxh.toDouble()
        )
        //		Rect vb = new Rect(viewBounds.x+viewBounds.w/2-maxw/2, viewBounds.y+viewBounds.h/2-maxh/2, maxw, maxh);

//		viewBounds.x+=100;
//		viewBounds.y+=100;
//		viewBounds.w-=200;
//		viewBounds.h-=200;
        browser!!.properties.put(frame, vb)
        center()
        visible = true
        browser!!.properties.put(hidden, false)
        browser!!.focus = true
        Drawing.dirty(browser)
    }

    fun hide() {
        Log.log("selection") { "hidding now" }
        visible = false
        tick = 0
        RunLoop.main.loop
            .attach { x: Int ->
                if (tick == 5) {
                    browser!!.focus = false
                    browser!!.properties.put(hidden, true)
                    browser!!.properties.put(Mouse.isSelected, false)
                    Log.log("selection") { "hidding now, again" }
                    Drawing.dirty(this)
                }
                tick++
                tick != 5
            }
        browser!!.focus = false
        browser!!.properties.put(Mouse.isSelected, false)
        browser!!.properties.put(hidden, true)
        Drawing.dirty(browser)
    }

    fun runCommands() {
        browser!!.executeJavaScript_queued("goCommands()")
        show()
    }

    fun center() {
        val window = this.find(Boxes.window, both())
            .findFirst()
            .get()
        if (!browser!!.properties.isTrue(hidden, false)) Drawing.dirty(this)
    }

    fun joinCommands(rename: String): Boolean {
        val ret: MutableList<String> = ArrayList()
        commandHelper.requestCommands(
            Optional.of(
                selection().findFirst()
                    .orElse(this)
            ), null, null, { x: String -> ret.add(x) }, -1, -1
        )
        var found: Runnable? = null
        val e: Set<Map.Entry<String?, String>> = commandHelper.callTableName.entries
        for ((key, value) in e) {
            if (value.lowercase(Locale.getDefault()) == rename.lowercase(Locale.getDefault())) {
                found = commandHelper.callTable[key]
                break
            }
        }
        if (found == null) {
            return false
        }
        if (found !is ExtendedCommand) {
            found.run()
            return false
        }
        show()
        found.begin(commandHelper.supportsPrompt { x: String ->
            Log.log("glassbrowser.debug") { "continue commands $x" }
            browser!!.executeJavaScript("continueCommands(JSON.parse('$x'))")
            //			ignoreHide = 4;
            show()
        }, null)
        found.run()
        return true
    }

    private fun findAndLoad(f: String, append: Boolean): String? {
        val roots = arrayOf(
            Main.app + "/modules/fieldcore/resources/",
            Main.app + "/modules/fieldcef_macosx/resources/",
            Main.app + "/lib/web/",
            Main.app + "/win/lib/web/"
        )
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
        @JvmField
        val glassBrowser = Dict.Prop<GlassBrowser>("glassBrowser").toCanon<GlassBrowser>()
            //.type<Any>()
            .doc<GlassBrowser>("The Browser that is stuck in front of the window, in window coordinates")

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
        properties.put(glassBrowser, this)
        this.root = root
    }
}