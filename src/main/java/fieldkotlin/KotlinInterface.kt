package fieldkotlin

import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.com.intellij.lang.Language
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.PsiFileFactory
import org.jetbrains.kotlin.config.JVMConfigurationKeys.IR
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.forEachDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.scripting.ide_services.compiler.KJvmReplCompilerWithIdeServices
import java.io.PrintWriter
import java.io.StringWriter
import java.lang.reflect.Field
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.kotlinFunction
import kotlin.reflect.jvm.kotlinProperty
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.with
import kotlin.script.experimental.jvm.*
import kotlin.script.experimental.jvm.impl.KJvmCompiledScript
import kotlin.script.experimental.jvm.util.determineSep
import kotlin.script.experimental.jvm.util.toSourceCodePosition
import kotlin.script.experimental.util.LinkedSnippet
import kotlin.system.measureNanoTime

// next: how to set variables (pass in a 'constructor' argument for _ ? do something with implicit this recievers?)
// and: the elephant, how to build new evaluator contexts
class KotlinInterface(
    val propertyClasses: Map<String, KClass<*>> = emptyMap(),
    val properties: Map<String, Any?> = emptyMap(),
    var classLoader: ClassLoader? = null,
    val filePrefix: String = "EVAL_"
) {

    internal inner class SourceCodeImpl(number: Int, override val text: String) : SourceCode {
        override val name: String? = "$filePrefix\$$number"
        override val locationId: String? = "LOCATION_$number"

        override fun equals(other: Any?): Boolean {
            return this == other || (other as? SourceCodeImpl)?.let { text == it.text } == true
        }

        override fun hashCode(): Int {
            return text.hashCode()
        }
    }

    var evaluator: BasicJvmReplEvaluator
    var compiler: KJvmReplCompilerWithIdeServices
    var evalConfig: ScriptEvaluationConfiguration
    var compilerConfig: ScriptCompilationConfiguration
    val contextUpdater: ContextUpdater
    val context: KotlinContext

    init {
        assert(properties.size == propertyClasses.size)
        assert(properties.keys.containsAll(propertyClasses.keys))
        if (classLoader == null) classLoader = Thread.currentThread().contextClassLoader
        val myHostConfiguration = defaultJvmScriptingHostConfiguration.with {
            jvm {
                baseClassLoader.replaceOnlyDefault(null)
                compilationCache(SimpleMemoryScriptsCache())
                IR
            }

        }
        compilerConfig = ScriptCompilationConfiguration {
            hostConfiguration.update { myHostConfiguration }
            jvm {
                compilerOptions.append("-jvm-target")
                compilerOptions.append("1.8")
                compilerOptions.append("-Xnew-inference")
                compilerOptions.append("-Xinline-classes")
//                dependenciesFromCurrentContext(wholeClasspath = true)
                dependenciesFromClassloader(
                    classLoader = classLoader!!,
                    wholeClasspath = true,
                    unpackJarCollections = true
                )
                //!
                providedProperties(*propertyClasses.map { it.key to it.value }.toTypedArray())
            }


        }


        evalConfig = ScriptEvaluationConfiguration {
            jvm {
                baseClassLoader(classLoader!!)
            }
            providedProperties(properties)

        }

        compiler = KJvmReplCompilerWithIdeServices()
        evaluator = BasicJvmReplEvaluator()

        context = KotlinContext()
        contextUpdater = ContextUpdater(context, evaluator)


    }

    inner class EvaluationEnvironment {

    }

    var id = 0

    suspend fun compile(s: String): ResultWithDiagnostics<LinkedSnippet<KJvmCompiledScript>> {
        var code = SourceCodeImpl(id++, s)
        var r: ResultWithDiagnostics<LinkedSnippet<KJvmCompiledScript>>
        var ns = measureNanoTime {
            r = compiler.compile(code, compilerConfig)
        }
        println("TIME<" + (ns / 1_000_000.0) + "ms> = compile")
        return r
    }


    suspend fun evaluate(
        e: LinkedSnippet<KJvmCompiledScript>,
        updateContext: Boolean = false
    ): ResultWithDiagnostics<LinkedSnippet<KJvmEvaluatedSnippet>> {
        var r: ResultWithDiagnostics<LinkedSnippet<KJvmEvaluatedSnippet>>
        var ns = measureNanoTime {
            r = evaluator.eval(e, evalConfig)
            if (updateContext) contextUpdater.update()
        }
        println("TIME<" + (ns / 1_000_000.0) + "ms> = evaluate")
        return r
    }

    fun String.findNthSubstring(s: String, n: Int, start: Int = 0): Int {
        if (n < 1 || start == -1) return -1

        var i = start

        for (k in 1..n) {
            i = indexOf(s, i)
            if (i == -1) return -1
            i += s.length
        }

        return i - s.length
    }


    fun Int.toSourceCodePositionWithNewAbsolute(code: SourceCode, newCode: SourceCode): SourceCode.Position? {
        val pos = toSourceCodePosition(code)
        val sep = code.text.determineSep()
        val absLineStart = if (pos.line == 1) 0
        else newCode.text.findNthSubstring(sep, pos.line - 1) + sep.length

        var nextNewLinePos = newCode.text.indexOf(sep, absLineStart)
        if (nextNewLinePos == -1) nextNewLinePos = newCode.text.length

        val abs = absLineStart + pos.col - 1
        if (abs > nextNewLinePos) return null

        return SourceCode.Position(pos.line, abs - absLineStart + 1, abs)
    }

    private interface ScriptingCacheWithCounters : CompiledJvmScriptsCache {

        val storedScripts: Int
        val retrievedScripts: Int
    }

    private class SimpleMemoryScriptsCache : ScriptingCacheWithCounters {

        internal val data = hashMapOf<Pair<SourceCode, Map<*, *>>, CompiledScript>()

        private var _storedScripts = 0
        private var _retrievedScripts = 0

        override val storedScripts: Int
            get() = _storedScripts

        override val retrievedScripts: Int
            get() = _retrievedScripts

        override fun get(
            script: SourceCode,
            scriptCompilationConfiguration: ScriptCompilationConfiguration
        ): CompiledScript? {
            println(" CACHE: get $script ")
            return data[script to scriptCompilationConfiguration.notTransientData]?.also { _retrievedScripts++ }
        }

        override fun store(
            compiledScript: CompiledScript,
            script: SourceCode,
            scriptCompilationConfiguration: ScriptCompilationConfiguration
        ) {
            println(" CACHE: put $script / $compiledScript has ${data.size}")
            data[script to scriptCompilationConfiguration.notTransientData] = compiledScript
            _storedScripts++
        }
    }


    suspend fun complete(s: String, cursor: Int): CompletionResult {
        var code = SourceCodeImpl(id++, s)


        return try {
            val cc = compiler.complete(code, cursor.toSourceCodePositionWithNewAbsolute(code, code)!!, compilerConfig)
            cc?.valueOrNull()?.toList()?.let { completionList ->
                getResult(s, cursor, completionList)
            } ?: CompletionResult.Empty(s, cursor)
        } catch (e: Exception) {
            val sw = StringWriter()
            e.printStackTrace(PrintWriter(sw))
            CompletionResult.Error(e.javaClass.simpleName, e.message ?: "", sw.toString())
        }

    }


    @JvmOverloads
    fun completeAndWait(s: String, cursor: Int = s.length): CompletionResult {
        return runBlocking { complete(s, cursor) }
    }

    fun parseAndRewrite(s: String, rules: List<(PsiElement, String) -> String>): String {

        // trying something new here

        if (s.contains("//%frames")) {
            val ll = s.split("\n")
            var imports = ""
            var rest = ""
            ll.forEach {
                if (it.trim().lowercase().startsWith("import ")) {
                    imports += it + "\n"
                } else if (it.trim().lowercase().contains(" by ")) {
                    imports += it + "\n"
                } else if (it.trim().lowercase().startsWith("//%frames")) {
                    rest += "var _r = frames {\n"
                } else {
                    rest += it + "\n"
                }
            }

            return imports + rest + "}"
        }

        return s;

        var cs = compiler.state.getCompilationState(compilerConfig)
        var env = cs.environment
        var project = env.project

//        IrGenerationExtension.registerExtension(project, object : IrGenerationExtension {
//            override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
//                print(moduleFragment)
//                print(pluginContext)
//            }
//        })


        var factory = PsiFileFactory.getInstance(project)
        var lang = Language.findLanguageByID("kotlin")!!
        val instance = factory.createFileFromText(lang, s)

        print(instance)

        var replacements = mutableListOf<Pair<Pair<Int, Int>, String>>()

        instance.forEachDescendantOfType<PsiElement> {

            //KtNamedFunction

            println(
                "$it::${it.javaClass} [${it.startOffset} -> ${it.endOffset}] / '${
                    s.substring(
                        it.startOffset,
                        it.endOffset
                    )
                }'"
            )

            for (i in it.parents) print("$i, ")
            println()

            when (it) {
                is KtCallExpression -> {
                    println(" -- call expression, calling -- ${it.name}")
                    println(" -- children are:")
                    it.children.forEach {
                        println("xx ${it}")
                    }
                }
                is KtNameReferenceExpression -> {
                }
            }

            var se = s.substring(it.startOffset, it.endOffset)
            var se2 = se
            for (n in rules) {
                se2 = n(it, se2)
            }
            if (se2 != se) {
                replacements.add((it.startOffset to it.endOffset) to se2)
            }

        }


        if (replacements.size != 0) {
            var ss = s
            replacements.sortedByDescending { it.first.first }.forEach {
                ss = ss.replaceRange(it.first.first until it.first.second, it.second)
            }
            return ss
        }
        //        var code = SourceCodeImpl(id++, s)
//        runBlocking {
//            val res = compiler.analyze(code, 0.toSourceCodePositionWithNewAbsolute(code, code)!!, compilerConfig)
//            print(res.valueOrThrow())
//        }
        return s
    }


    suspend fun exec(
        s: String,
        onCompileError: (List<ScriptDiagnostic>) -> Unit = {},
        onEvalError: (List<ScriptDiagnostic>) -> Unit = {},
        onEvalException: (Throwable) -> Unit = {}
    ): ResultValue? {

        when (val r = compile(s)) {
            is ResultWithDiagnostics.Success -> {
                return when (val r2 = evaluate(r.value)) {
                    is ResultWithDiagnostics.Success -> {
                        when (val e = r2.value.get().result) {
                            is ResultValue.Error -> {
                                println(" Evaluation Error ${e.error} / ${e.error.javaClass}")
                                onEvalException(e.error)
                                e
                            }
                            is ResultValue.NotEvaluated -> {
                                println(" not evaluated for ${r2.reports}")
                                onEvalError(r2.reports)
                                e
                            }
                            else -> {
                                e
                            }
                        }

                    }

                    is ResultWithDiagnostics.Failure -> {
                        print("evaluator error ${r2}")
                        onEvalError(r2.reports)
                        null
                    }
                }
            }
            is ResultWithDiagnostics.Failure -> {
                print("compiler error ${r}")
                onCompileError(r.reports)
                null
            }
        }

        return null
    }

    @JvmOverloads
    fun execAndWait(
        s: String,
        onCompileError: (List<ScriptDiagnostic>) -> Unit = {},
        onEvalError: (List<ScriptDiagnostic>) -> Unit = {},
        onEvalException: (Throwable) -> Unit = {}
    ): ResultValue? {
        return runBlocking {
            exec(s, onCompileError, onEvalError, onEvalException)
        }
    }

    fun exec2(s: String) {

        var code = SourceCodeImpl(id++, s)
        when (val compileResultWithDiagnostics = runBlocking {
            var q: ResultWithDiagnostics<LinkedSnippet<KJvmCompiledScript>>
            val t = measureNanoTime {

                q = compiler.compile(code, compilerConfig)
            }
            println("\t\t compile ${t / 1_000_000.0}ms")
            q
        }) {
            is ResultWithDiagnostics.Success -> {
                val resultWithDiagnostics = runBlocking {
                    var q: ResultWithDiagnostics<LinkedSnippet<KJvmEvaluatedSnippet>>
                    var t = measureNanoTime {
                        q = evaluator.eval(compileResultWithDiagnostics.value, evalConfig)
                    }
                    println("\t\t eval ${t / 1_000_000.0}ms")
                    q
                }
                print(resultWithDiagnostics)
            }
            is ResultWithDiagnostics.Failure -> {
                print(compileResultWithDiagnostics)
            }
        }
        var tt = measureNanoTime {
            contextUpdater.update()
        }
        println("\t context update ${tt / 1_000_000.0}ms")
    }

    enum class CompletionStatus(private val value: String) {
        OK("ok"), ERROR("error");

        override fun toString(): String {
            return value
        }
    }


    abstract class CompletionResult(private val status: CompletionStatus) {
//        open fun toJson(): JsonObject {
//            return jsonObject("status" to status.toString())
//        }

        open class Success(
            val matches: List<String>,
            val bounds: CompletionTokenBounds,
            val metadata: List<SourceCodeCompletionVariant>,
            val text: String,
            val cursor: Int
        ) : CompletionResult(CompletionStatus.OK) {
            init {
                assert(matches.size == metadata.size)
            }

//            override fun toJson(): JsonObject {
//                val res = super.toJson()
//                res["matches"] = matches
//                res["cursor_start"] = bounds.start
//                res["cursor_end"] = bounds.end
//                res["metadata"] = mapOf(
//                        "_jupyter_types_experimental" to metadata.map {
//                            mapOf(
//                                    "text" to it.text,
//                                    "type" to it.tail,
//                                    "start" to bounds.start,
//                                    "end" to bounds.end
//                            )
//                        },
//                        "_jupyter_extended_metadata" to metadata.map {
//                            mapOf(
//                                    "text" to it.text,
//                                    "displayText" to it.displayText,
//                                    "icon" to it.icon,
//                                    "tail" to it.tail
//                            )
//                        }
//                )
//                res["paragraph"] = mapOf(
//                        "cursor" to cursor,
//                        "text" to text
//                )
//                return res
//            }


        }

        class Empty(text: String, cursor: Int) :
            Success(emptyList(), CompletionTokenBounds(cursor, cursor), emptyList(), text, cursor)

        class Error(val errorName: String, val errorValue: String, val traceBack: String) :
            CompletionResult(CompletionStatus.ERROR) {

        }
    }


    companion object {
        fun functionSignature(function: KFunction<*>) = function.toString().replace("Line_\\d+\\.".toRegex(), "")

        fun shortenType(name: String) = name.replace("(\\b[_a-zA-Z$][_a-zA-Z0-9$]*\\b\\.)+".toRegex(), "")

        data class CompletionTokenBounds(val start: Int, val end: Int)


        private fun getTokenBounds(buf: String, cursor: Int): CompletionTokenBounds {
            require(cursor <= buf.length) { "Position $cursor does not exist in code snippet <$buf>" }

            val startSubstring = buf.substring(0, cursor)

            val filter = { c: Char -> !c.isLetterOrDigit() && c != '_' }

            val start = startSubstring.indexOfLast(filter) + 1

            return CompletionTokenBounds(start, cursor)
        }

        fun getResult(
            code: String,
            cursor: Int,
            completions: List<SourceCodeCompletionVariant>
        ): CompletionResult.Success {
            val bounds = getTokenBounds(code, cursor)
            return CompletionResult.Success(completions.map { it.text }, bounds, completions, code, cursor)
        }


        @JvmStatic
        fun main3(a: Array<String>) {

            var a1 = KotlinInterface(mapOf("BANANA" to Int::class), mapOf("BANANA" to 10))
            var a2 = KotlinInterface(mapOf("BANANA" to Int::class), mapOf("BANANA" to 10))
            println("###   " + a1.execAndWait("val x = mutableListOf(1,2,3)"))
            println("###   " + a1.execAndWait("x"))
            runBlocking {

                val cc = a1.complete("x.", 2)

                println("###   " + cc)
            }

        }

        @JvmStatic
        fun main(a: Array<String>) {

            var a1 = KotlinInterface(mapOf("BANANA" to Int::class), mapOf("BANANA" to 10))
            println(
                "###   " + a1.parseAndRewrite(
                    """
                val x = mutableListOf(1,2,3)
                fun banana() : Int {
                    return 5 
                }
                val y = banana()
                """.trimIndent(), emptyList()
                )
            )

        }

        @JvmStatic
        fun main2(a: Array<String>) {
            println("hello world")
            var a = KotlinInterface(mapOf("BANANA" to Int::class), mapOf("BANANA" to 10))
            for (i in 0 until 2) {
                runBlocking {
                    val r = a.exec("var x = ${i} + BANANA \n println(\"from code \"+x)")
                    println("R1 = $r")
                    val r2 = a.exec("var x = ${i} + BANANA2 \n println(\"from code \"+x)", onCompileError = {

                        print("R2(onCompileError) = $it")
                    })
                    println("R2 = $r2")

                    val r3 = a.exec("var x = ${i} + BANANA \n println(\"from code \"+x/(1-1))", onEvalException = {
                        it.printStackTrace()
                    })
                    println("R3 = $r3")

                    val r4 = a.exec("var x = ${i} + BANANA \n x")
                    println("R4 = $r4")

                }

            }
        }
    }

    class KotlinContext(
        val vars: HashMap<String, KotlinVariableInfo> = HashMap(),
        val functions: HashMap<String, KotlinFunctionInfo> = HashMap()
    )


    class KotlinFunctionInfo(val function: KFunction<*>, val line: Any) : Comparable<KotlinFunctionInfo> {

        val name: String
            get() = function.name

        fun toString(shortenTypes: Boolean): String {
            return if (shortenTypes) {
                shortenType(toString())
            } else toString()
        }

        override fun toString(): String {
            return functionSignature(function)
        }

        override fun compareTo(other: KotlinFunctionInfo): Int {
            return this.toString().compareTo(other.toString())
        }

        override fun hashCode(): Int {
            return this.toString().hashCode()
        }

        override fun equals(other: Any?): Boolean {
            return if (other is KotlinFunctionInfo) {
                this.toString() == other.toString()
            } else false
        }
    }

    class KotlinVariableInfo(val value: Any?, val descriptor: KProperty<*>, val line: Any) {

        val name: String
            get() = descriptor.name

        @Suppress("MemberVisibilityCanBePrivate")
        val type: String
            get() = descriptor.returnType.toString()

        fun toString(shortenTypes: Boolean): String {
            var type: String = type
            if (shortenTypes) {
                type = shortenType(type)
            }
            return "$name: $type = $value"
        }

        override fun toString(): String {
            return toString(false)
        }
    }

    class ContextUpdater(val context: KotlinContext, private val evaluator: BasicJvmReplEvaluator) {

        private var lastProcessedSnippet: LinkedSnippet<KJvmEvaluatedSnippet>? = null

        fun update() {
            try {
                var lastSnippet = evaluator.lastEvaluatedSnippet
                val newSnippets = mutableListOf<Any>()
                while (lastSnippet != lastProcessedSnippet && lastSnippet != null) {
                    val line = lastSnippet.get().result.scriptInstance
                    if (line != null) newSnippets.add(line)
                    lastSnippet = lastSnippet.previous
                }
                newSnippets.reverse()
                refreshVariables(newSnippets)
                refreshMethods(newSnippets)
                lastProcessedSnippet = evaluator.lastEvaluatedSnippet
            } catch (e: ReflectiveOperationException) {
                println("Exception updating current variables $e")
            } catch (e: NullPointerException) {
                println("Exception updating current variables $e")
            }
        }

        private fun refreshMethods(lines: List<Any>) {
            for (line in lines) {
                val methods = line.javaClass.methods
                for (method in methods) {
                    if (objectMethods.contains(method) || method.name == "main") {
                        continue
                    }
                    val function = method.kotlinFunction ?: continue
                    context.functions[function.name] = KotlinFunctionInfo(function, line)
                }
            }
        }

        @Throws(ReflectiveOperationException::class)
        private fun refreshVariables(lines: List<Any>) {
            for (line in lines) {
                val fields = line.javaClass.declaredFields
                findVariables(fields, line)
            }
        }

        @Throws(IllegalAccessException::class)
        private fun findVariables(fields: Array<Field>, o: Any) {
            for (field in fields) {
                val fieldName = field.name
                if (fieldName.contains("$\$implicitReceiver") || fieldName.contains("script$")) {
                    continue
                }

                field.isAccessible = true
                val value = field.get(o)
                val descriptor = field.kotlinProperty
                if (descriptor != null) {
                    context.vars[fieldName] = KotlinVariableInfo(value, descriptor, o)
                }
            }
        }

        companion object {
            private val objectMethods = HashSet(listOf(*Any::class.java.methods))
        }
    }

}