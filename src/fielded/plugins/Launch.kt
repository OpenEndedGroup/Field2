package fielded.plugins

import fieldbox.FieldBox
import java.io.File
import java.util.*

class Launch {
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