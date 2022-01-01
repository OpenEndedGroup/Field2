package fieldkotlin

import field.utility.Dict
import fieldbox.boxes.Box
import fieldbox.boxes.plugins.BoxPair.m

class Magics {

    interface Magic {
        fun run(inside: Box, executionService: (String) -> Any?): Any?
    }

    companion object {
        val prefix = "//<"
        val globalPrefix = "//>"
        val magics = Dict.Prop<Magics>("__magics__")
    }

    val seen = mutableSetOf<String>()
    val global = mutableSetOf<String>()


    fun checkMagic(s: String, inside: Box, executionService: (String) -> Any?) {
        val x = 0..3

        val lines = s.split("\n")
        val remaining = lines.map { if (it.trim().startsWith(prefix)) "" else it }.joinToString(separator = "\n")

        val new = lines.filter { it.trim().startsWith(prefix) }.filter {
            val vv = it.replace(prefix, "").trim()
            !seen.contains(vv) || vv.startsWith("_")
        }
        if (new.size == 0) {
            doGlobals(s, inside, executionService)
            return
        }

        new.forEach {
            println(" ----------> $it")
            try {
                val q = it.replace(prefix, "").trim()
                seen.add(q)
                val t = executionService(interpretMagic(q))
                when (t) {
                    is Throwable -> throw t
                    null -> return@forEach
                    is Magic -> {
                        val ran = t.run(inside, executionService)
                        when (ran) {
                            is Throwable -> throw ran
                            null -> return@forEach
                            else -> {
                                println("WARNING: couldn't understand the return of magic $it / $t = $ran, continuing on")
                            }
                        }
                    }
                    is String -> {

                        if (q.startsWith("_"))
                            if (seen.contains(t))
                                return@forEach

                        val ran = executionService(t)
                        when (ran) {
                            is Throwable -> throw ran
                            null -> return@forEach
                            else -> {
                                println("WARNING: couldn't understand the return of magic string $it as $ran, continuing on")
                            }
                        }
                    }
                    else -> {
                        println("WARNING: couldn't understand the return of magic factory $it as $t, continuing on")
                    }
                }
            } finally {
                println(" <---------- $it")

            }
        }

        doGlobals(s, inside, executionService)
    }

    fun doGlobals(s: String, inside: Box, executionService: (String) -> Any?, done: MutableSet<Box> = mutableSetOf()) {
        if (done.contains(inside)) return
//        done.add(inside)
        val lines = s.split("\n")
        val remaining = lines.map { if (it.trim().startsWith(globalPrefix)) "" else it }.joinToString(separator = "\n")

        val new = lines.filter { it.trim().startsWith(globalPrefix) }
            .filter { !global.contains(it.replace(globalPrefix, "").trim()) }

        doGlobals(new.toSet(), inside, executionService)
    }

    fun doGlobals(
        s: Set<String>,
        inside: Box,
        executionService: (String) -> Any?,
        done: MutableSet<Box> = mutableSetOf()
    ) {
        if (done.contains(inside)) return
        done.add(inside)

        val new = s.filter { !global.contains(it) }

        new.forEach {
            println(" ----------> $it")
            try {
                val q = it.replace(globalPrefix, "").trim()
                global.add(q)
                println(" -- executing magic $m")
                val m = interpretMagic(q)
                val t = executionService(m)
                println(" -- got " + t)
                when (t) {
                    is Throwable -> throw t
                    null -> return@forEach
                    is Magic -> {
                        val ran = t.run(inside, executionService)
                        when (ran) {
                            is Throwable -> throw ran
                            null -> return@forEach
                            else -> {
                                println("WARNING: couldn't understand the return of magic $it / $t = $ran, continuing on")
                            }
                        }
                    }
                    is String -> {
                        val ran = executionService(t)
                        when (ran) {
                            is Throwable -> throw ran
                            null -> return@forEach
                            else -> {
                                println("WARNING: couldn't understand the return of magic string $it as $ran, continuing on")
                            }
                        }
                    }
                    else -> {
                        println("WARNING: couldn't understand the return of magic factory $it as $t, continuing on")
                    }
                }
            } finally {
                println(" <---------- $it")

            }
        }

        inside.parents.forEach {
            val m = it.properties.get(magics)
            if (m != null) {
                doGlobals(m.global, it, executionService, done)
            }
        }

    }

    private fun interpretMagic(it: String): String {
        if (it.startsWith("`")) {
            var name = it.substring(1)
            return "var $name = \"$name\""
        }
        return it
    }
}