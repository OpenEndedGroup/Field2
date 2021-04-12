package field.graphics

import field.app.RunLoop
import field.utility.Options
import fieldbox.FieldBox
import fieldbox.LoggingDefaults
import fieldbox.io.IO
import fieldcef.browser.CefSystem
import java.awt.Toolkit
import kotlin.concurrent.thread

class TestWindow2 {

    companion object {

        val fieldBox = FieldBox()
        var args: Array<String>? = null
        var workspace: String? = null

        var io: IO? = null

        private var window: Long = 0

        @JvmStatic
        fun main(s: Array<String>) {

            val cl = this.javaClass.classLoader

            println(cl)

//		SwingUtilities.invokeLater(() -> {
            FieldBox.args = s


            System.setProperty("java.awt.headless", "true")
            val t = Toolkit.getDefaultToolkit()
            System.out.println(" --> $t")

            LoggingDefaults.initialize()
            Options.parseCommandLine(s)
            FieldBox.workspace = Options.getDirectory("workspace") { System.getProperty("user.home") + "/Documents/FieldWorkspace/" }

            Windows.windows.init()
            val w = Window(50, 50, 500, 500, "hello")

            thread {
                val hello = CefSystem.cefSystem
                println("final cef system is called $hello")
            }

            RunLoop.main.enterMainLoop()



        }
    }

}