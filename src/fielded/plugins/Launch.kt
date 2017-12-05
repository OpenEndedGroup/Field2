package fielded.plugins

import fieldbox.FieldBox
import org.lwjgl.util.tinyfd.TinyFileDialogs
import java.io.File
import java.util.*
import org.lwjgl.PointerBuffer
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryStack.stackPush


class Launch {

    fun openField2File(): Boolean {
        return stackPush().use {
            val aFilterPatterns = it.mallocPointer(1)

            aFilterPatterns.put(it.UTF8("*.field2"))

            aFilterPatterns.flip()
            var fn = TinyFileDialogs.tinyfd_openFileDialog("Open File(s)", FieldBox.workspace + "/", aFilterPatterns, ".field2 files", false)
            if (fn != null) {
                val ff = File(fn)
                if (ff.exists()) {
                    openInNewProcess(ff.name, ff.parentFile.absolutePath + "/")
                    return@use true
                }
            }
            false
        }
    }

    fun openInNewProcess(fn: String, workspace: String = FieldBox.workspace) {

        var launcher = System.getenv("FIELD2_LAUNCH")
        if (!launcher.endsWith("nodebug"))
            launcher = launcher + "_nodebug"

        val u = UUID.randomUUID()
        ProcessBuilder().command(launcher, "fieldbox.FieldBox", "-file", fn, "-workspace", workspace)
                .redirectError(ProcessBuilder.Redirect.appendTo(File("/var/tmp/field_" + u + "." + fn + ".error.log")))
                .redirectOutput(ProcessBuilder.Redirect.appendTo(File("/var/tmp/field_" + u + "." + fn + ".out.log"))).start()
    }


}

private fun <T> MemoryStack.use(a: (MemoryStack) -> T): T {
    this.push()
    try {
        return a(this)
    } finally {
        this.pop()
    }

}
