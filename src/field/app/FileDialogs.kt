package field.app

import field.app.ThreadSync2.Companion.inMainThread
import org.lwjgl.system.MemoryStack
import org.lwjgl.util.tinyfd.TinyFileDialogs
import java.util.concurrent.Callable

class FileDialogs {

    @JvmOverloads
    fun openFile(title: String, initialPath: String = "", pattern: String = "*.*", description: String = "All Files"): String {

        val path = inMainThread  {

            MemoryStack.stackPush().use { stack: MemoryStack ->
                val filterPatterns = stack.mallocPointer(1)
                filterPatterns.put(stack.UTF8(pattern))
                filterPatterns.flip()

                TinyFileDialogs.tinyfd_openFileDialog(title, initialPath, filterPatterns, description, false)
            }
        }
        return path
    }

    @JvmOverloads
    fun saveFile(title:String, initialPath: String = "", pattern: String, description: String): String {

        val path = inMainThread  {

            MemoryStack.stackPush().use { stack: MemoryStack ->
                val filterPatterns = stack.mallocPointer(1)
                filterPatterns.put(stack.UTF8(pattern))
                filterPatterns.flip()

                TinyFileDialogs.tinyfd_saveFileDialog(title, initialPath, filterPatterns, description)
            }
        }
        return path
    }

}

private fun <T> MemoryStack.use(function: (MemoryStack) -> T): T {
    val t: T = function(this)
    this.close()
    return t
}
