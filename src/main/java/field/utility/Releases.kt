package field.utility

import field.app.RunLoop
import org.apache.commons.compress.archivers.examples.Expander
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.concurrent.thread
import kotlin.system.exitProcess

/**
 * tools for checking to see if Field should be updated
 */
class Releases {


    fun read(url: String): String {
        val con = URL(url).openConnection()
        return String(con.getInputStream().readAllBytes())
    }

    fun deserialize(url: String): JSONObject = JSONObject(read(url))


    fun run(
        url: String,
        check: (JSONObject) -> kotlin.Pair<String, String>?,
        onCompletion: () -> Unit,
        onPassedCheck: () -> Unit,
        onError: (Throwable) -> Unit
    ) {

        var progress = true
        var decompress = true
        thread {
            try {
                val o = deserialize(url)
                check(o)?.let {

                    val con = URL(URL(url), it.first).openConnection()

                    var len = con.contentLength
                    var f = File.createTempFile("field_download_", "." + it.first.split(".").last())
                    f.deleteOnExit()
                    println()
                    thread {
                        while (progress) {
                            print("   downloading ${f.length()} / $len (bytes)       \r")
                            Thread.sleep(100)
                        }
                        print("\n   decompressing ...")
                        while (decompress) {
                            print(".")
                            Thread.sleep(1000)
                        }
                    }
                    Files.copy(con.getInputStream(), f.toPath(), StandardCopyOption.REPLACE_EXISTING)
                    progress = false

                    var a = BufferedInputStream(FileInputStream(f))

                    Expander().expand(a, File(it.second)) {
                        it.close()
                        decompress = false
                        progress = false
                        RunLoop.main.once { onCompletion() }
                    }
                    true
                } ?: RunLoop.main.once { onPassedCheck() }

            } catch (e: Throwable) {
                progress = false
                decompress = false
//                e.printStackTrace()
                RunLoop.main.once { onError(e) }
            }
        }
    }

    companion object {

        var dependencyVersion = 1
        var classesVersion = 1


        fun test() {
            Releases().run("http://openendedgroup.com/field/releases/manifest.json", check = {
                if (it.getInt("version") > 0) {
                    it.getString("source") to "/Users/marc/temp/releases/downloadTo/"
                } else null
            }, onCompletion = {}, onPassedCheck = {}, onError = {})
        }


        fun upgrade() {

            var app = System.getProperty("appDir")

            app = "/Users/marc/temp/releases/downloadTo/"

            Releases().run("http://openendedgroup.com/field/releases/manifest.json", check = {
                if (it.has("classesVersion") && it.getInt("classesVersion") > classesVersion) {
                    println("\n   -- downloading classfile upgrade --")
                    it.getString("classes") to "$app/target_preflight/"
                } else
                    if (it.has("dependencyVersion") && it.getInt("dependencyVersion") > dependencyVersion) {
                        println("\n   -- downloading dependency upgrade --")
                        it.getString("deps") to "$app/target_preflight/"
                    } else null
            }, onCompletion = {
                println("\n\n\n   -- upgrade complete 👍 -- \n\n\n")
                exitProcess(0)
            }, onPassedCheck = {
                println("\n\n\n   -- no upgrade available ✅ (your Field is up to date) -- \n")
                exitProcess(0)
            }, onError = {
                println("\n\n\n   -- error checking for update `${it.javaClass} | ${it.message}` 👎 --\n")
                exitProcess(0)
            })

        }

    }

}