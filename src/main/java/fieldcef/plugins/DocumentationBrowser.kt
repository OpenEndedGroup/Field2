package fieldcef.plugins

import com.github.rjeschke.txtmark.BlockEmitter
import field.app.RunLoop
import field.utility.MarkdownToHTML
import field.utility.Pair
import fieldbox.boxes.Box
import fieldbox.boxes.Boxes
import fieldbox.boxes.Drawing
import fieldbox.boxes.Mouse
import fieldbox.execution.JavaSupport
import fieldbox.io.IO
import fielded.Commands
import fielded.RemoteEditor
import fielded.ServerSupport
import fielded.TextUtils
import fielded.boxbrowser.TransientCommands
import fielded.plugins.Launch
import fielded.webserver.NanoHTTPD
import java.io.File
import java.nio.charset.Charset
import java.nio.file.Files
import java.util.function.Supplier
import java.util.stream.Stream

/**
 * Created by marc on 2/2/17.
 */
class DocumentationBrowser(val root: Box) : Box(), IO.Loaded {

    val DOCUMENTATION = "/doc/"
    val LAUNCH= "/launch/"


    var cn = 0;
    var preamble = Files.readString(File(fieldagent.Main.app + "lib/web/init_docbrowser.html_fragment").toPath(), Charset.defaultCharset())

    val postamble = "</div></body>"

    override fun loaded() {
        println(" documentation browser booting up ");

        this.properties.put(Boxes.dontSave, true)

        this.properties.put<Supplier<Map<Pair<String, String>, Runnable>>>(Commands.commands, Supplier<Map<Pair<String, String>, Runnable>> {
            val v = HashMap<Pair<String, String>, Runnable>()

//            val u = "http://localhost:8080/doc/field/graphics/FLine"
            val u = "http://localhost:4000/index.html"

            v.put(Pair("New Documentation Browser", "Makes a new browser for reading documentation"), Runnable { makeNewDocumentationBrowesr(root, u) })
            v
        })

        val s = root.find(ServerSupport.server, upwards()).findFirst().get()

        s.addURIHandler({ uri, method, headers, params, files ->
            if (uri.startsWith(DOCUMENTATION)) {

                preamble = Files.readString(File(fieldagent.Main.app + "lib/web/init_docbrowser.html_fragment").toPath(), Charset.defaultCharset())

                var uu = uri.substring(DOCUMENTATION.length)
                val pieces = uu.split("/".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()

                println(" documentation request with pieces ${printArray(pieces)}")

                var response: NanoHTTPD.Response? = null;

                // try to locate a resource for uu
                JavaSupport.srcZipsDeltWith.map {
                    println(" checking "+it+" for dir "+File(it).isDirectory)
                    if (File(it).isDirectory) {
                        response = serveIfPossible(File(it + "/" + uu + ".html"), { it })

                        // need some preamble

                        if (response == null)
                            response = serveIfPossible(File(it + "/" + uu + ".md"), {

                                val o = MarkdownToHTML.convertFully(it, BlockEmitter { stringBuilder, mutableList, s ->

                                    println(s)

                                    val all = mutableList.reduce( {a,b -> a+"\n"+b})

                                    stringBuilder.append("<textarea readonly class='ta_" + cn + "'>");
                                    mutableList.forEach { stringBuilder.append(it+"\n") }
                                    stringBuilder.append("</textarea> <script language='javascript'>CodeMirror.fromTextArea($('.ta_" + cn + "')[0], {lineNumbers:true, lineWrapping:true, viewportMargin:Infinity, mode:'javascript', readOnly:true, gutters:[\"CodeMirror-foldgutter\", \"CodeMirror-lint-markers\", \"CodeMirror-linenumbers\"]})</script>")

                                    stringBuilder.append(button(">", 0, {
                                        pasteAndRunAtCursor(all)
                                    }))

                                    cn++;

                                });

                                preamble+"\n"+o+"\n"+postamble;
                            })

                        if (response==null)
                        {
                            response = NanoHTTPD.Response(NanoHTTPD.Response.Status.OK, null,preamble+"\n(no document found)\n"+postamble )
                        }

                        response
                    }
                    else null
                }.filter { it!=null }.first()

            }
            else
                null
        })

        s.addURIHandler({ uri, method, headers, params, files ->
            if (uri.startsWith(LAUNCH)) {

                var uu = uri.substring(LAUNCH.length)

                val pieces = uu.split("/");
                val name = pieces[0];
                val url = uu.substring(name.length+1);

                preamble = Files.readString(File(fieldagent.Main.app + "lib/web/didLaunch.html").toPath(), Charset.defaultCharset())

                val r = NanoHTTPD.Response(NanoHTTPD.Response.Status.OK, null,preamble+"Field is downloading and launching file "+name+" from "+url);

                Thread{
                    Launch(root).downloadDecompressAndOpen(name, "http://"+url);
                }.start()

                r
            }
            else
                null
        })
    }

    fun pasteAndRunAtCursor(all: String)
    {
//        val selected = selection().collect(Collectors.toList())

        val re = root.find(RemoteEditor.editor, upwards()).findFirst().get()

        re.sendJavaScriptNow("var c = cm.getCursor()\n" +
                "cm.setCursor({line:10000})\n" +
                "cm.replaceRange(\"\\n"+TextUtils.quoteNoOuter(all)+"\\n\", cm.getCursor())\n" +
                "\n" +
                "cm.execCommand(\"goCharLeft\")\n" +
                "cm.setSelection(c, cm.getCursor())\n" +
                "\n" +
                "Run_Selection()\n" +
                "\n" +
                "cm.setCursor(c);cm.refresh();selectTab('code');\n")
    }


    private fun serveIfPossible(pf: File, transform: (String) -> String?): NanoHTTPD.Response? {
        println("checking $pf ${pf.exists()}")
        if (pf.exists()) {

            val content = transform(Files.readString(pf.toPath(), Charset.defaultCharset()))

            if (content != null)
                return NanoHTTPD.Response(NanoHTTPD.Response.Status.OK, null, content.byteInputStream(Charset.defaultCharset()))
        }
        return null;
    }

    init {


    }

    private fun pin(target: Box) {
        val ed = target.find(RemoteEditor.editor, upwards()).findFirst().get()
        ed.pin()
        target.breadthFirst(upwards()).filter { x -> x is TextEditor }.forEach { x -> (x as TextEditor).pin() }
    }

    private fun unpin(target: Box) {
        val ed = target.find(RemoteEditor.editor, upwards()).findFirst().get()
        ed.unpin()
        target.breadthFirst(upwards()).filter { x -> x is TextEditor }.forEach { x -> (x as TextEditor).unpin() }
    }

    fun makeNewDocumentationBrowesr(target: Box, url: String): Box {
        val te = TextEditor(root)
        te.connect(root)
        te.loaded(url)

        RunLoop.main.delay({

            te.browser_.properties.put(Box.hidden, false)
            te.pin()
            Drawing.dirty(te)

        }, 100)

        return te.browser_
    }


    private fun selection(): Stream<Box> {
        return breadthFirst(both()).filter { x -> x.properties.isTrue(Mouse.isSelected, false) }
    }

    private fun printArray(first: Array<*>): String {
        var s = ""
        for (n in first)
            s += " " + n
        return s.trim()
    }


    fun button(txt : String, n : Int, go : () -> Unit) : String
    {
        return "<div class='Field-codeFloatButton' "+TransientCommands.transientCommands.onclickForCommand(go)+" style='right: ${15 + n * 25}px;'>$txt</div>"
    }


}
