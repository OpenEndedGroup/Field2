package fieldbox.boxes.plugins

import field.app.RunLoop
import field.utility.Options
import fieldbox.boxes.Box

class Startup(val root: Box) {

    val startString = Options.getString("startup", { null })

    init {

        if (startString != null) {
            RunLoop.main.delayTicks({

                root.find(Pseudo.named, root.both()).forEach {
                    val b = it.apply(root).asMap_get(startString) as Collection<Box>
                    b.forEachIndexed { index, box ->

                        RunLoop.main.delayTicks({
                            println("\n\n*** starting $box *** \n\n")
                            box.find(Chorder.begin, box.both()).forEach {
                                it.apply(box)
                            }
                        }, index)

                    }
                }

            }, 20)

        }
    }


}