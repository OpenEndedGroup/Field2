package fieldbox.boxes.plugins

import field.utility.Pair
import fieldbox.FieldBox
import fieldbox.boxes.Box
import fieldbox.boxes.Boxes
import fielded.Commands
import fielded.plugins.use
import org.lwjgl.system.MemoryStack
import org.lwjgl.util.tinyfd.TinyFileDialogs
import java.io.File
import java.util.function.Supplier

class InsertPath : Box() {

    init {
        properties.put(Commands.commands, Supplier<Map<Pair<String, String>, Runnable>> {

            val m = LinkedHashMap<Pair<String, String>, Runnable>()

            m.put(Pair("Copy Path to Clipboard", "Select a path using a file dialog and put it on the clipboard"), Runnable {
                getOpenFile()?.apply {
                    first(Boxes.window, both()).ifPresent {
                        it.currentClipboard = "\"" + this + "\""
                    }
                }
            })

            m
        })
    }

    fun getOpenFile(): String? {
        return MemoryStack.stackPush().use {
            val aFilterPatterns = it.mallocPointer(1)

//            aFilterPatterns.put(it.UTF8("*.field2"))

            aFilterPatterns.flip()
            var fn = TinyFileDialogs.tinyfd_openFileDialog("Open File(s)", FieldBox.workspace + "/", aFilterPatterns, ".field2 files", false)
            if (fn != null) {
                val ff = File(fn)
                if (ff.exists()) {
                    return@use ff.absolutePath
                }
            }
            null
        }
    }

}