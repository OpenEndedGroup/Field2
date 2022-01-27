package fieldkotlin

import field.utility.Dict
import fieldbox.boxes.Box
import fielded.boxbrowser.BoxBrowser
import kotlin.reflect.KParameter

class VariablesIntrospection(var context: KotlinInterface.KotlinContext) : BoxBrowser.HasMarkdownInformation {

    override fun generateMarkdown(inside: Box, property: Dict.Prop<*>): String {

        var v = ""
        context.vars.forEach {
            v += "<b>" + it.key + "</b> = <i>" + it.value.value + "</i> ("+clean(it.value.type)+")<br>"
        }
        var f = ""
        context.functions.forEach {
            f += "<b>" + it.key + "</b>(" + it.value.function.parameters.filter { it.kind != KParameter.Kind.INSTANCE }
                .map { clean(it.type.toString()) }
                .joinToString(separator = ",") + ") -> " + clean(it.value.function.returnType.toString()) + " <br>"
        }

        return """
            <h2> Variables</h2> 
            $v
            <h2> Functions</h2>
            $f
        """.trimIndent()

    }

    val elide = setOf("kotlin.", "fieldbox.boxes.")

    fun clean(s: String): String {
        var ss = s
        elide.forEach {
//            ss = ss.replace(it, "&hellip;")
            ss = ss.replace(it, "")
        }

        return ss
    }
}