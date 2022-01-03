package fieldpython

import field.app.ThreadSync2
import field.linalg.Vec4
import field.utility.Dict
import field.utility.Pair
import field.utility.Quad
import field.utility.Triple
import fieldbox.boxes.Box
import fieldbox.boxes.Boxes
import fieldbox.boxes.Drawing
import fieldbox.boxes.plugins.BoxDefaultCode
import fieldbox.execution.Completion
import fieldbox.execution.Execution
import fieldbox.execution.JavaSupport
import fieldbox.io.IO
import fielded.RemoteEditor
import fielded.plugins.Out
import jep.*
import java.io.IOException
import java.io.Writer
import java.util.*
import java.util.function.Consumer
import java.util.stream.Collectors


class PI : Execution(null) {

    val pi = mutableMapOf<Box, Execution.ExecutionSupport>()

    val stdlib = BoxDefaultCode.findSource(this.javaClass, "stdlib")

    val errorRegex = Regex(", ([\\d]+), ([\\d]+), ")

    override fun support(box: Box, prop: Dict.Prop<String>): Execution.ExecutionSupport? {
        if (box == this) return null
        if (prop != Execution.code) return null;

        val ef = this.properties.get(Execution.executionFilter)
        return if (ef == null || ef.apply(box)) pi.computeIfAbsent(box) {
            wrap(
                box,
                prop,
                initPythonInterface(box, prop)
            )
        } else null
    }

    companion object {

        val jc = object : JepConfig() {
            init {
                setSharedModules(setOf("numpy", "spacy", "spacy.lang"))
                setClassLoader(PI::class.java.classLoader)
            }
        }

    }

    lateinit var globalShared: SharedInterpreter


    private fun initPythonInterface(
        box: Box,
        prop: Dict.Prop<String>
    ): SharedInterpreter {

        if (this::globalShared.isInitialized)
        {
            makeLocals(globalShared, box)
            return globalShared
        }

//        initSharedInterpreter()
        var s = SharedInterpreter();
        s.exec(stdlib)
        s.set("_", box)
        s.exec(
            """
    __localmap__ = {}
     
    import sys
    
    from java.lang import System
    class StdOutToJava(object):
        def __init__(self):
            self.oldout = sys.stdout
    
        def write(self, msg):
            System.out.println(msg)
            if ("__writer__" in globals()):
                __writer__.write(str(msg))
    
        def flush(self):
            if ("__writer__" in globals()):
                __writer__.flush()
    
    def setup():
        sys.stdout = StdOutToJava()
        sys.stderr = StdOutToJava()
    
    setup()
            """.trimIndent()
        )
        globalShared = s
        makeLocals(s, box)
        return s

    }

    private fun makeLocals(s: SharedInterpreter, box: Box) {
        s.set("_", box)
        val root = box.find(Boxes.root, upwards()).findFirst()!!
        s.set("__", root)
        s.exec("__localmap__[_] = {'_':BoxWrapper(_), '__':BoxWrapper(__)}")
    }


    private fun wrap(
        box: Box,
        prop: Dict.Prop<String>,
        pi: SharedInterpreter
    ): Execution.ExecutionSupport {

        val output = box.find(Out.__out, box.both())
            .findFirst()
            .orElseThrow { IllegalStateException("Can't find html output support") }

        var written = false
        var currentLineNumber: Triple<Box, Int, Boolean>? = null

        val writer = object : Writer() {
            @Throws(IOException::class)
            override fun write(cbuf: CharArray, off: Int, len: Int) {
                if (len > 0) {
                    val s = String(cbuf, off, len)

                    if (s.trim { it <= ' ' }.length == 0) return
                    written = true
                    if (currentLineNumber == null || currentLineNumber!!.first == null || currentLineNumber!!.second == -1) {
                        val o: Set<Consumer<Quad<Box, Int, String, Boolean>?>> = box.find(
                            directedOutput, box.upwards()
                        )
                            .collect(Collectors.toSet())
                        o.forEach(Consumer { x: Consumer<Quad<Box, Int, String, Boolean>?> ->
                            x.accept(
                                Quad(
                                    box,
                                    -1,
                                    s,
                                    true
                                )
                            )
                        })
                    } else {
                        val o: Set<Consumer<Quad<Box, Int, String, Boolean>?>> = box.find(
                            directedOutput, box.upwards()
                        )
                            .collect(Collectors.toSet())
                        if (o.size > 0) {
                            o.forEach(Consumer { x: Consumer<Quad<Box, Int, String, Boolean>?> ->
                                x.accept(
                                    Quad(
                                        currentLineNumber!!.first, currentLineNumber!!.second, s,
                                        currentLineNumber!!.third
                                    )
                                )
                            })
                        } else {
                            o.forEach(Consumer { x: Consumer<Quad<Box, Int, String, Boolean>?> ->
                                x.accept(
                                    Quad(
                                        box,
                                        -1,
                                        s,
                                        true
                                    )
                                )
                            })
                        }
                    }
                }
            }

            @Throws(IOException::class)
            override fun flush() {
            }

            @Throws(IOException::class)
            override fun close() {
            }
        }

        return object : Execution.ExecutionSupport {

            var lineOffset = 0

            internal var uniq: Long = 0

            override fun executeTextFragment(
                textFragment: String,
                suffix: String,
                success: Consumer<String>,
                lineErrors: Consumer<Pair<Int, String>>
            ): Any? {
                var textFragment = textFragment
                RemoteEditor.removeBoxFeedback(Optional.of(box), "__redmark__")

                try {
                    Execution.context.get().push(box)

                    for (i in 0 until lineOffset)
                        textFragment = "\n" + textFragment

                    written = false
                    currentLineNumber = null
                    output.setWriter(
                        writer
                    ) {
                        currentLineNumber = it
                    }

                    val res = doEval(textFragment, lineErrors, success)

                    println(" result is $res")

                    RemoteEditor.boxFeedback(Optional.of(box), Vec4(0.3, 0.7, 0.3, 0.5))

                    return null
                } finally {
                    Execution.context.get().pop()
                }
            }


            private fun doEval(
                textFragment: String,
                lineErrors: Consumer<Pair<Int, String>>,
                success: Consumer<String>
            ): Pair<Any?, Boolean> {

                return ThreadSync2.inMainThread {
                    var errored = false

//                    println(" -- PI doeval in thread " + Thread.currentThread() + " " + pi.second)
                    pi.set("__writer__", writer)
//                    pi.second.put("_", box)
                    try {
                        //                        pi.exec(textFragment)
                        pi.set("__fragment__", textFragment)
                        pi.set("_", box)
                        pi.exec("__fieldeval__(__fragment__, __localmap__[_])")
//                        pi.first.exec(textFragment)
                    } catch (e: jep.JepException) {
                        e.printStackTrace()
                        val r = errorRegex.find(e.message!!)
                        if (r != null) // e.g syntax errors
                        {
                            val line = r.groupValues[1].toInt()
                            val char = r.groupValues[2].toInt()
                            lineErrors.accept(Pair(line, e.message!!.replace("<", " ").replace(">", " ")))
                        }

                        lineErrors.accept(
                            Pair(
                                e.stackTrace[0].lineNumber,
                                e.message!!.replace("<", " ").replace(">", " ")
                            )
                        )

                        return@inMainThread Pair(null, false)
                    }

                    return@inMainThread Pair(null, true)
                }
            }

            private fun target(box: Box): String? {
                return "box_" + box.properties.get(Box.name) + "|" + box.properties.getOrConstruct(IO.id);
            }


            override fun getBinding(name: String): Any? {
                // this needs more sauce for sure given __localmap__
                return pi.getValue(name)
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
                endOthers: Boolean
            ): String? {
                //				if (endOthers) end(lineErrors, success);
                RemoteEditor.removeBoxFeedback(Optional.of(box), "__redmark__")

                try {
                    Execution.context.get().push(box)

                    val name = "main._animatorPython_"

                    var code = box.properties.get(Execution.code)!!


                    RemoteEditor.boxFeedback(Optional.of(box), Vec4(0.3, 0.7, 0.3, 0.5))


                    return null
                } finally {
                    Execution.context.get().pop()
                }

            }

            override fun end(lineErrors: Consumer<Pair<Int, String>>, success: Consumer<String>) {
                RemoteEditor.removeBoxFeedback(Optional.of(box), "__redmark__")


                RemoteEditor.boxFeedback(Optional.of(box), Vec4(0.3, 0.7, 0.3, 0.5))
                Drawing.dirty(box)
            }

            override fun setConsoleOutput(stdout: Consumer<String>, stderr: Consumer<String>) {


            }

            override fun completion(
                allText: String,
                line: Int,
                ch: Int,
                results: Consumer<List<Completion>>,
                explicit: Boolean
            ) {
                val lines = allText.split("\n".toRegex())
                var c = 0
                for (i in 0 until line) {
                    c += lines[i].length + 1
                }

                c += ch

//                val completions = ki.completeAndWait(allText, c)
//                when (completions) {
//                    is KotlinInterface.CompletionResult.Success -> {
//                        val matches: List<String> = completions.matches
//
//
//                        val comp = completions.metadata.map {
//                            if (it.tail == "keyword")
//                                Completion(
//                                    completions.bounds.start,
//                                    completions.bounds.end,
//                                    it.text,
//                                    "<span class=type>${it.displayText}</span>"
//                                )
//                            else
//                                Completion(
//                                    completions.bounds.start,
//                                    completions.bounds.end,
//                                    it.text,
//                                    "<span class=type>${it.displayText} -> ${it.tail} <small>${it.icon}</small></span>"
//                                )
//                        }
//                        results.accept(comp)
//                    }
//                    else -> {
//                        println(" no completions ? ")
//                    }
//                }
            }

            override fun imports(allText: String, line: Int, ch: Int, results: Consumer<List<Completion>>) {

                val lines = allText.split("\n".toRegex()).toTypedArray()
                var c = 0
                for (i in 0 until line) {
                    c += lines[i].length + 1
                }

                c += ch

                var found = allText.substring(0, c).indexOfLast {
                    it.isWhitespace()
                }
                val prefix = allText.substring(found + 1, c)

                val possible = JavaSupport.javaSupport.getPossibleJavaClassesFor(prefix)

                val r = mutableListOf<Completion>()
                for (p in possible) {
                    val tail = Math.max(p.first.lastIndexOf("."), p.first.lastIndexOf("$"))
                    val ex = Completion(
                        c - prefix.length, c, p.first.substring(
                            tail +
                                    1
                        ), p.second
                    )
                    var typeName = p.first.replace("\\$".toRegex(), ".")
                    if (typeName.endsWith(".")) typeName = typeName.substring(0, typeName.length - 1)
//                    ex.header = ("var " + p.first.substring(tail + 1) + " = Java.type('" + typeName
//                            + "')")
                    ex.header = "import ${typeName}"
                    r.add(ex)
                }

                results.accept(r)

            }

            override fun getCodeMirrorLanguageName(): String {
                return "text/x-python"
            }

            override fun getDefaultFileExtension(): String {
                return ".py"
            }

            override fun setLineOffsetForFragment(lineOffset: Int, origin: Dict.Prop<String>) {
                this.lineOffset = lineOffset
            }
        }
    }


}

private operator fun <A, B> Pair<A, B>.component1(): A = this.first
private operator fun <A, B> Pair<A, B>.component2(): B = this.second

private fun shorten(c: Class<*>): String? {
    val nn = c.name
    val p = nn.split("[\\.\\$]".toRegex()).toTypedArray()
    val shor = p[p.size - 1]
    return if (shor.equals("ScriptObjectMirror", ignoreCase = true)) "Object" else shor
}

private fun shorten(c: String): String? {
    val nn = c
    val p = nn.split("[\\.\\$]".toRegex()).toTypedArray()
    val shor = p[p.size - 1]
    return if (shor.equals("ScriptObjectMirror", ignoreCase = true)) "Object" else shor
}