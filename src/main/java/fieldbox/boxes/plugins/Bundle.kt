package fieldbox.boxes.plugins

import field.graphics.util.onsheetui.get
import fieldbox.DefaultMenus
import fieldbox.FieldBox
import fieldbox.Open
import fieldbox.boxes.Box
import fieldbox.io.IO
import fielded.Commands
import fielded.plugins.Launch
import java.awt.Desktop
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream


class Bundle(val root: Box) : Box() {

    init {
        this.properties.putToMap(Commands.command, "Export Bundle", java.util.function.Function<Box, Unit> {
            bundleNow()
        })
        this.properties.putToMap(Commands.commandDoc, "Export Bundle",
                "Save this document as a self contained .zip file (i.e. for email etc.)"
        )
    }

    fun bundleNow() {
        var v = Launch(root).getSaveFile()
        if (v != null) {
            if (!v.endsWith(".zip")) v = v+".zip"
            // TODO: notification and feedback
            val success = saveAndBundle(v);
            showInFinder(v)
        }
    }

    fun saveAndBundle(outputTo: String): Boolean {
        DefaultMenus.save(root, (root get Open.fieldFilename)!!)
        return zipFiles(FieldBox.fieldBox.io.filesTouched, outputTo)
    }

    fun showInFinder(outputTo: String) {
        Desktop.getDesktop().browseFileDirectory(File(outputTo))
    }

    fun zipFiles(files: Collection<File>, output: String): Boolean {

        var fos: FileOutputStream? = null
        try {
            fos = FileOutputStream(output)
            var zipOut = ZipOutputStream(BufferedOutputStream(fos))
            for (filePath in files) {
                val input = filePath
                var fis = FileInputStream(input)
                val ze = ZipEntry(input.getName())
                zipOut.putNextEntry(ze)
                val tmp = ByteArray(4 * 1024)
                var size = 0
                while (true) {
                    size = fis.read(tmp)
                    if (size == -1) break;

                    zipOut.write(tmp, 0, size)
                }
                zipOut.flush()
                fis.close()
            }
            zipOut.close()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            return false
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        } finally {
            try {
                if (fos != null) fos.close()
            } catch (ex: Exception) {
                ex.printStackTrace()
                return false
            }
        }
        return true
    }

}