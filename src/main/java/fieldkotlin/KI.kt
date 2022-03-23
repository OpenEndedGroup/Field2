package fieldkotlin

import field.linalg.Vec4
import field.utility.*
import fieldbox.boxes.*
import fieldbox.boxes.plugins.BoxDefaultCode
import fieldbox.boxes.plugins.IsExecuting
import fieldbox.execution.Completion
import fieldbox.execution.Execution
import fieldbox.execution.JavaSupport
import fieldbox.io.IO
import fieldcef.plugins.up
import fielded.RemoteEditor
import fielded.boxbrowser.BoxBrowser.HasMarkdownInformation
import fielded.plugins.Out
import fieldkotlin.Magics.Companion.magics
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.resolve.calls.callUtil.getType
import java.io.IOException
import java.io.Writer
import java.util.*
import java.util.function.BiConsumer
import java.util.function.Consumer
import java.util.function.Supplier
import java.util.regex.Pattern
import java.util.stream.Collectors
import kotlin.script.experimental.api.ResultValue
import kotlin.script.experimental.api.SourceCode

// need a globally edited preamble that gets executed in all boxes
// that might end up taking a long time to update?

class KI : Execution(null) {


    val ki = mutableMapOf<Box, Execution.ExecutionSupport>()

    var global: () -> String = { "" }

    val stdlib = BoxDefaultCode.findSource(this.javaClass, "stdlib")

    init {
        this.properties += FLineDrawing.boxBackground to vec(0.6, 0.5, 0.4, 0.75)
        this.properties += FLineDrawing.boxOutline to vec(1, 0.9, 0.8, 0.6)
    }

    override fun support(box: Box, prop: Dict.Prop<String>): Execution.ExecutionSupport? {
        if (box == this) return null
        if (prop != Execution.code) return null;

        val ef = this.properties.get(Execution.executionFilter)
        return if (ef == null || ef.apply(box)) ki.computeIfAbsent(box) {
            val w = wrap(
                box,
                prop,
                initKotlinInterface(box, prop)
            )

            val g = global()
            if (g.length > 0 && box.properties[Box.name] != "GLOBAL") {
                w.executeTextFragment(g, box)
            }

            w
        } else null
    }

    var currentLineNumber: Triple<Box, Int, Boolean>? = null
    var written = false

    fun makeWriter(box: Box) = object : Writer() {
        @Throws(IOException::class)
        override fun write(cbuf: CharArray, off: Int, len: Int) {
            if (len > 0) {
                val s = String(cbuf, off, len)

//							if (s.endsWith("\n"))
//								s = s.substring(0, s.length() - 1) + "<br>";
                if (s.trim { it <= ' ' }.length == 0) return
                written = true
                if (currentLineNumber == null || currentLineNumber!!.first == null || currentLineNumber!!.second == -1) {
//								success.accept(s);
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
//									success.accept(finalS);
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

    companion object {
        var uniq = 0
    }


    private fun initKotlinInterface(box: Box, prop: Dict.Prop<String>): KotlinInterface {

        val root: Box = (box up Boxes.root)!!


        box.properties += FLineDrawing.boxBackground to vec(0.6, 0.5, 0.4, 0.75)
        box.properties += FLineDrawing.boxOutline to vec(1, 0.9, 0.8, 0.6)

        return KotlinInterface(
            propertyClasses = mapOf("__bx__" to Box::class, "__root__" to Box::class, "__writer__" to Writer::class),
            properties = mapOf("__bx__" to box, "__root__" to root, "__writer__" to makeWriter(box)),
            classLoader = this.javaClass.classLoader,
            filePrefix = "bx$" + box[IO.id],
        )
    }


    private fun wrap(box: Box, prop: Dict.Prop<String>, ki: KotlinInterface): Execution.ExecutionSupport {

        val isGlobal = box.properties[Box.name] == "GLOBAL"

        val output = box.find(Out.__out, box.both())
            .findFirst()
            .orElseThrow { IllegalStateException("Can't find html output support") }

        var magic = Magics()
        box[magics] = magic

        println(" -- booting up KI ---")
        ki.execAndWait(stdlib, { ce -> print(ce) }, { ee -> print(ee) }, { ex -> print(ex) })
        println(" -- booting up KI, phase 1 complete ---")
        magic.checkMagic(stdlib, box) {
            println(" running $it")
            var res = ki.execAndWait(it, { ce -> print(ce) }, { ee -> print(ee) }, { ex -> print(ex) })
            println(" got $res")
            when (res) {
                is ResultValue.Value -> {
                    when (res.value) {
                        is String -> return@checkMagic res.value
                        else -> return@checkMagic null
                    }
                }
                else -> return@checkMagic null
            }
        }
        println(" -- booting up KI, finished ---")

        return object : Execution.ExecutionSupport {

            var lineOffset = 0

            fun internalTextFragmentExec(
                textFragment: String,
                suffix: String,
                success: Consumer<String>,
                lineErrors: Consumer<Pair<Int, String>>, isAll: Boolean
            ): Any? {
                var textFragment =
                    if (isAll) textFragment else Callbacks.thread(box, textFragment, Callbacks.onExecuteFragment)

                if (isAll)
                    Callbacks.call(box, Callbacks.onExecute)


                RemoteEditor.removeBoxFeedback(Optional.of(box), "__redmark__")

                try {
                    context.get().push(box)

                    for (i in 0 until lineOffset)
                        textFragment = "\n" + textFragment

                    written = false
                    currentLineNumber = null
                    output.setWriter(
                        ki.properties["__writer__"] as Writer
                    ) {
                        currentLineNumber = it
                    }


                    magic.checkMagic(textFragment, box) {
                        val r = doEval(it, lineErrors, Consumer { })
                        if (r.second) throw IllegalArgumentException("Error in magic code '$it'")
                        else r.first
                    }

                    val res = doEval(textFragment, lineErrors, success)
                    ki.contextUpdater.update()
                    box.properties += Dict.Prop<VariablesIntrospection>("kotlin_introspection") to VariablesIntrospection(ki.context)

                    println(" result is $res")

                    if (res is Pair) {
                        val k = ki.context.vars

                        val found = k.values.firstOrNull {
                            it.value == res.first
                        }

                        if (found != null) {
                            println(" -- identified by name as $found")
                        }
                    }

                    RemoteEditor.boxFeedback(Optional.of(box), Vec4(0.3, 0.7, 0.3, 0.5))

                    if (isGlobal) {
                        global = {
                            box.properties[code]
                        }

                        this@KI.ki.forEach { k, v ->
                            if (k != box) {
                                v.executeTextFragment(textFragment, suffix, success, lineErrors)
                            }
                        }
                    }


                    return null
                } finally {
                    Execution.context.get().pop()
                }
            }


            override fun executeTextFragment(
                textFragment: String,
                suffix: String,
                success: Consumer<String>,
                lineErrors: Consumer<Pair<Int, String>>,
            ) = internalTextFragmentExec(textFragment, suffix, success, lineErrors, false)

            private fun parseHooks(textFragment: String): String {
                val rules = mutableListOf<(PsiElement, String) -> String>()

                val counts = mutableMapOf<String, Int>()
                rules.add { element, text ->

                    when (element) {
                        is KtCallExpression -> {
                            val name = element.firstChild.firstChild.getText()
                            counts[name] = 1 + counts.computeIfAbsent(name) { 0 }
                            if (name == "hereIsAFunction") {
                                return@add "__${name}__(\"\"\"${
                                    computeScopeLabel(
                                        element,
                                        text,
                                        counts[name]!!
                                    )
                                }\"\"\")" + element.getText().substring(name.length)
                            }
                        }


                    }

                    text
                }
                val o = ki.parseAndRewrite(textFragment, rules)
                print("after transformation `$o`")
                return o
            }

            private fun computeScopeLabel(element: PsiElement, text: String, counts: Int = 0): String {
                var n = 0

                //TODO: count things like functions as always making a new context?
                //... how to do explict context management ? ...
                // we need a //%push('$arg') or something

                for (i in element.parents) print("$i, ")
                return "$n+$text+$counts"
            }

            private fun doEval(
                textFragment: String,
                lineErrors: Consumer<Pair<Int, String>>,
                success: Consumer<String>
            ): Pair<Any?, Boolean> {
                var errored = false

                val tf2 = parseHooks(textFragment)

//                val tf2 = textFragment
                val res = ki.execAndWait(tf2, { compileError ->
                    println(" compile error ${compileError}")

                    compileError.sortedBy { -(it.location as SourceCode.Location).start.line }
                        .sortedBy { -it.severity.ordinal }.filter { it.severity.name != "WARNING" }.forEach {
                            println(it)
                            errored = true
                            val message = it.message
                            val line = if (it.location != null) (it.location as SourceCode.Location).start.line else -1
                            lineErrors.accept(Pair(line, message))
                        }

                }, { evalError ->
                    println(" eval error ${evalError}")

                    evalError.sortedBy {
                        1.0
                    }.forEach {
                        val message = it.message
                        val line = if (it.location != null) (it.location as SourceCode.Location).start.line else -1
                        lineErrors.accept(Pair(line, message))
                    }

                    errored = true
                }, { e ->
                    println(" eval exception ${e}")
                    errored = true

                    val extraMessage = ""

                    // let's see if we can't scrape a line number out of the exception stacktrace
                    val s: Array<StackTraceElement> = e.getStackTrace()
                    var found = false
                    if (s != null) {
                        for (i in s.indices) {
                            if (s[i].fileName != null && s[i].fileName
                                    .startsWith("bx$")
                            ) {
                                var m: String = e.message ?: ""
                                if (m == null) m = "" + shorten(e.javaClass)
                                lineErrors.accept(
                                    Pair(s[i].lineNumber, extraMessage + " " + m)
                                )
                                found = true
                            }
                        }
                    }
                    if (!found) {
                        lineErrors.accept(Pair(-1, extraMessage + " " + e.message + "<br>"))
                    }
                    e.printStackTrace()

                })

                if (!written && !errored) {
                    when (res) {
                        null -> {
                            success.accept(" &#10003; ")
                            return Pair(null, false)
                        }
                        is ResultValue.Value -> {
                            if (res.value == null)
                                success.accept(" &#10003; ")
                            else
                                success.accept("${res.value} <i><smaller>${shorten(res.type)}</smaller></i><br>")
                            return Pair(res.value, false)
                        }
                        is ResultValue.Unit -> {
                            success.accept(" &#10003; ")
                            return Pair(null, false)
                        }
                        is ResultValue.Error -> {
                            // does this ever happen?
                            success.accept("${res}<br>")
                            return Pair(res, true)
                        }
                    }

                }
                return Pair(res, errored)
            }


            private fun target(box: Box): String? {
                return "box_" + box.properties.get(Box.name) + "|" + box.properties.getOrConstruct(IO.id);
            }


            override fun getBinding(name: String): Any? {
                return ki.context.vars[name]?.value ?: null
            }

            override fun executeAll(
                allText: String,
                lineErrors: Consumer<Pair<Int, String>>,
                success: Consumer<String>
            ) {
                lineOffset = 0
                internalTextFragmentExec(allText, "", success, lineErrors, isAll = true)
            }


            val prefix = "" + uniq++

            var lastCodeBegan = ""


            override fun begin(
                lineErrors: Consumer<Pair<Int, String>>,
                success: Consumer<String>,
                initiator: Map<String, Any>,
                endOngoing: Boolean
            ): String? {

                print(" -- initiator for begin is $initiator")
                //				if (endOthers) end(lineErrors, success);
                RemoteEditor.removeBoxFeedback(Optional.of(box), "__redmark__")
                // is "run" defined here or anywhere above?

                box.properties.put(IsExecuting.executionSequenceCount, box.properties.computeIfAbsent(
                    IsExecuting.executionSequenceCount
                ) { k: Dict.Prop<Int>? -> 0 } + 1)

                try {
                    context.get().push(box)

                    val name = "main._animatorKotlin_"

                    var code = box.properties.get(Execution.code)!!

                    // new rule: if code hasn't changed since the last run then look for a main property
                    var _r: Any? = null
                    if (code == lastCodeBegan) {
                        _r = getBinding("_r")
                        println(" -- skipping code execution altogether and reusuing ${_r}")
                    }

                    if (_r == null) {
                        written = false
                        currentLineNumber = null
                        output.setWriter(
                            ki.properties["__writer__"] as Writer
                        ) {
                            currentLineNumber = it
                        }

                        magic.checkMagic(code, box) {
                            val r = doEval(it, lineErrors, Consumer { })
                            if (r.second) throw IllegalArgumentException("Error in magic code '$it'")
                            else r.first
                        }

                        ki.context.vars.remove("_r")
                        val res = doEval(code, lineErrors, success)

                        ki.contextUpdater.update()
                        _r = getBinding("_r")
                    }

                    println(" result is ${_r}")
                    if (_r != null) {
                        println("    type is ${_r::class.java}")
                        println("    type is ${_r::class.java.superclass}")
                        println("    type is ${_r::class.java.interfaces.toList()}")
                    }

                    if (_r is kotlin.jvm.functions.Function0<*>) {
                        val was = _r
                        _r = frame {
                            was()
                        }
                    }

                    if (_r is Sequence<*>) {

                        val s = _r as Sequence<*>

                        _r = phases {
                            var i: Iterator<*>? = null
                            begin {
                                i = s.iterator()
                                i!!.hasNext()
                            }
                            cont {
                                if (i!!.hasNext()) {
                                    i!!.next()
                                }
                            }
                            end {
                                if (i!!.hasNext()) {
                                    i!!.next()
                                }
                            }
                        }
                    }

                    if (_r is ElasticAnimation.Block) {
                        // todo: glorious syntax for blocks, including calling them at the right
                        // 'alpha'
                        // and calling them with time = 1 at the end.
                        val root = ElasticAnimation().run(_r)
                        _r = phases {
                            begin {
                                root.runAtAlpha(0.0)
                            }
                            cont {
                                root.runAtAlpha(0.5) // TODO!
                            }
                            end {
                                root.runAtAlpha(1.0)
                            }
                        }
                    }



                    if (_r is PhaseList) {
                        _r.reset()
                        if (endOngoing) end(lineErrors, success)

                        val name: String = "main._animator" + prefix + "_" + uniq
                        box.properties.putToMap<String, Supplier<Boolean>>(Boxes.insideRunLoop, name, _r)
                        box.first(IsExecuting.isExecuting)
                            .ifPresent { x: BiConsumer<Box?, String?> ->
                                x.accept(
                                    box,
                                    name
                                )
                            }

                        uniq++
                        box.properties.put(IsExecuting.executionSequenceCount, box.properties.computeIfAbsent(
                            IsExecuting.executionSequenceCount
                        ) { k: Dict.Prop<Int>? -> 1 } - 1)

                        lastCodeBegan = code

                        return name
                    }

                    return null
                } finally {
                    Execution.context.get().pop()
                }

            }

            override fun end(lineErrors: Consumer<Pair<Int, String>>, success: Consumer<String>) {
                RemoteEditor.removeBoxFeedback(Optional.of(box), "__redmark__")

                val m = box.properties.get(Boxes.insideRunLoop)
                    ?: return


                val p = Pattern.compile("main._animator" + prefix + "_.*")

                for (s in ArrayList(m.keys)) {
                    if (p.matcher(s).matches()) {
                        val b = m[s]!!
                        if (b is Consumer<*>) (b as Consumer<Boolean>).accept(false) else {
                            m.remove(s)
                        }
                    }
                }

                Drawing.dirty(box)

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

                val completions = ki.completeAndWait(allText, c)
                when (completions) {
                    is KotlinInterface.CompletionResult.Success -> {
                        val matches: List<String> = completions.matches
//                        print(completions.bounds)
//                        print(completions.metadata)
//                        print(completions.text)
//                        print(completions.cursor)


                        val comp = completions.metadata.mapNotNull {


                            if (it.tail == "keyword")
//                                Completion(
//                                    completions.bounds.start,
//                                    completions.bounds.end,
//                                    it.text,
//                                    "<span class=type>${clean(it.displayText)}</span>"
//                                )
                                null
                            else {
                                var stripped = clean(it.displayText)
                                if (stripped.startsWith(it.text)) {
                                    if (it.text.endsWith("("))
                                        stripped = stripped.substring(it.text.length - 1)
                                    else
                                        stripped = stripped.substring(it.text.length)
                                }

                                Completion(
                                    completions.bounds.start,
                                    completions.bounds.end,
                                    clean(it.text),
                                    "<span class=type>${stripped} -> ${clean(it.tail)} <small>${clean(it.icon)}</small></span>"
                                )
                            }
                        }
                        results.accept(comp)
                    }
                    else -> {
                        println(" no completions ? ")
                    }
                }
            }

            private fun clean(displayText: String): String =
                displayText.replace("<", "&lt;").replace(">", "&gt;")

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
                return "text/x-kotlin"
            }

            override fun getDefaultFileExtension(): String {
                return ".kt"
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