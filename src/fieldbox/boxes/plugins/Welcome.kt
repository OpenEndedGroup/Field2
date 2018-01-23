package fieldbox.boxes.plugins

import field.app.ThreadSync2
import field.utility.Dict
import field.utility.Options
import field.utility.Rect
import field.utility.plusAssign
import fieldbox.DefaultMenus
import fieldbox.boxes.Box
import fieldbox.boxes.Boxes
import fieldbox.boxes.FrameManipulation
import fieldbox.execution.Execution
import fieldbox.io.IO
import fielded.RemoteEditor
import java.util.*
import java.util.concurrent.Callable
import java.util.function.Consumer

class Welcome(val root: Box) : Box(), IO.Loaded {

    override fun loaded() {
        val welcomeTo: String? = Options.getString("welcomeTo", { null })
        if (welcomeTo == null) return;

        val f = ThreadSync2.sync!!
                .launchAndServiceOnce("__welcomeService__", Callable {

                    ThreadSync2.yield()
                    // make a non-saving box called 'welcome' in the top left-hand corner of the document
                    // select it
                    // tell the text editor to go to the welcome url

                    val newBox = (this both DefaultMenus.newBox)!!.apply(root)
                    newBox += name to "Welcome"
                    newBox += frame to Rect(10.0, 10.0, 150.0, 50.0)
                    newBox += Execution.code to "// code goes here, but this box will not be saved"
                    newBox += Boxes.dontSave to true

                    FrameManipulation.setSelectionTo(root, Collections.singleton(newBox));

                    ThreadSync2.yield()

                    while ((this both RemoteEditor.editor) == null) ThreadSync2.yield()

                    (this both RemoteEditor.editor)!!.sendJavaScriptNow("showInfoURL('"+welcomeTo+"')");

                }, Consumer<Throwable> {
                    println(" exception thrown while welcoming ... ");
                    it.printStackTrace()
                })
    }

    private inline infix fun <T> Box.both(next: Dict.Prop<T>): T? {
        return this.find(next, this.both()).findFirst().orElseGet { null }
    }


}