package trace.sound

import fieldagent.Main
import java.io.File

class RunAnalysis(val fn : String)
{
    init {
        val f = File(fn)
        val f2 = File(fn+ ".yaml_frames")

        if (f.exists() && f2.exists() /*&& f.lastModified()<f2.lastModified()*/)
        {
            System.out.println("already there...")
        }
        else
        {

            val process = ProcessBuilder().command(Main.app + "/osx/lib/essentia_analysis/essentia_streaming_extractor_music", fn, fn + ".yaml", Main.app + "/osx/lib/essentia_analysis/all_config_frames.yaml")
                    .redirectError(ProcessBuilder.Redirect.appendTo(File("/var/tmp/field_" + "." + File(fn).name + ".error.log")))
                    .redirectOutput(ProcessBuilder.Redirect.appendTo(File("/var/tmp/field_" + "." + File(fn).name + ".out.log"))).start()

            process.waitFor()

        }

    }
}