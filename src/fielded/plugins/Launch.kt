package fielded.plugins

import field.utility.Dict
import field.utility.Options
import fieldbox.FieldBox
import fieldbox.boxes.Box
import fieldbox.boxes.Drawing
import fieldbox.io.IO.pad
import fieldagent.Main
import fielded.Commands
import fieldnashorn.Nashorn
import org.lwjgl.util.tinyfd.TinyFileDialogs
import java.io.File
import java.util.*
import org.lwjgl.PointerBuffer
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryStack.stackPush
import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.io.FileOutputStream
import java.io.BufferedOutputStream
import java.io.IOException
import java.util.zip.ZipInputStream
import java.util.zip.ZipEntry
import java.io.FileInputStream
import java.util.function.Supplier

class Launch(val root: Box) : Box() {

    init {
        properties.put(Commands.commands, Supplier<Map<field.utility.Pair<String, String>, Runnable>> {

            val m = LinkedHashMap<field.utility.Pair<String, String>, Runnable>()

            m.put(field.utility.Pair("Open", "Open a `.field2` file in a new Field"), Runnable {
                if (openField2File())
                {
                    Drawing.notify("Opening...", root, 200);
                }
            })

            m
        })
    }


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

    fun getSaveFile(): String? {
        return stackPush().use {
            val aFilterPatterns = it.mallocPointer(1)

            aFilterPatterns.put(it.UTF8("*.zip"))

            aFilterPatterns.flip()
            val fn = TinyFileDialogs.tinyfd_saveFileDialog("Save Bundle", System.getProperty("user.home"), aFilterPatterns, ".field2.zip bundle file")
            fn
        }
    }

    fun downloadDecompressAndOpen(name: String, url: String): Boolean {

        val workspace = FieldBox.workspace
        val targetName = if (File(workspace, name).exists()) {
            var index = 0
            while (File(workspace, stripSuffix(name).first + "_" + pad(index++) + stripSuffix(name).second).exists());

            name + "_" + pad(index)
        } else name

        val target = File(workspace, targetName).toPath()

        val website = URL(url)
        website.openStream().use({
            Files.copy(it, target, StandardCopyOption.REPLACE_EXISTING)
        })

        UnzipUtility().unzip(target.toFile().absolutePath, target.toFile().absolutePath + ".dir")

        val found = File(target.toFile().absolutePath + ".dir").listFiles { n ->
            println("${n} = ${n.endsWith(".field2")}")
            n.name.endsWith(".field2")
        }
        openInNewProcess(found[0].name, found[0].parentFile.absolutePath)

        return true
    }

    private fun stripSuffix(name: String): Pair<String, String> {
        val at = name.lastIndexOf(".")
        if (at == -1) return name to ""
        return name.substring(0, at) to name.substring(at, name.length)
    }

    fun openInNewProcess(fn: String, workspace: String = FieldBox.workspace) {

        var launcher = System.getenv("FIELD2_LAUNCH")
        if (Main.os == Main.OS.mac) {
            if (!launcher.endsWith("nodebug"))
                launcher = launcher + "_nodebug"

            val u = UUID.randomUUID()
            ProcessBuilder().command(launcher, "fieldbox.FieldBox", "-file", fn, "-workspace", workspace)
                    .redirectError(ProcessBuilder.Redirect.appendTo(File("/var/tmp/field_" + u + "." + fn + ".error.log")))
                    .redirectOutput(ProcessBuilder.Redirect.appendTo(File("/var/tmp/field_" + u + "." + fn + ".out.log"))).start()
        }
        else if (Main.os == Main.OS.windows)
        {
            val u = UUID.randomUUID()

            println(" does this file exist? ${File("c:\\windows\\system32\\cmd.exe").exists()}")

            val e1 = File.createTempFile("field",".error.log")
            val s1 = File.createTempFile("field",".out.log")

            ProcessBuilder().command("c:\\windows\\system32\\cmd.exe", "/c", "start", "\"\"", launcher, "fieldbox.FieldBox", "-file", fn, "-strict", if (Options.dict().isTrue(Dict.Prop<Number>("strict"), false)) "1" else "0", "-workspace", workspace)
                    .redirectError(ProcessBuilder.Redirect.appendTo(e1))
                    .redirectOutput(ProcessBuilder.Redirect.appendTo(s1)).start()

        }
    }


    inner class UnzipUtility {

        @Throws(IOException::class)
        fun unzip(zipFilePath: String, destDirectory: String) {
            val destDir = File(destDirectory)
            if (!destDir.exists()) {
                destDir.mkdirs()
            }
            val zipIn = ZipInputStream(FileInputStream(zipFilePath))
            var entry: ZipEntry? = zipIn.nextEntry
            // iterates over entries in the zip file
            while (entry != null) {
                val filePath = destDirectory + File.separator + entry.name
                if (!entry.isDirectory) {
                    // if the entry is a file, extracts it
                    extractFile(zipIn, filePath)
                } else {
                    // if the entry is a directory, make the directory
                    val dir = File(filePath)
                    dir.mkdirs()
                }
                zipIn.closeEntry()
                entry = zipIn.nextEntry
            }
            zipIn.close()
        }

        @Throws(IOException::class)
        private fun extractFile(zipIn: ZipInputStream, filePath: String) {
            val bos = BufferedOutputStream(FileOutputStream(filePath))
            val bytesIn = ByteArray(4096)
            var read = 0
            while (true) {
                read = zipIn.read(bytesIn)
                if (read < 0) break;
                bos.write(bytesIn, 0, read)
            }
            bos.close()
        }

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
