package auw

import auw.BufferTools.zero
import fieldbox.boxes.Box
import fieldbox.execution.Errors
import fieldbox.io.IO
import fieldnashorn.Nashorn
import org.jetbrains.kotlin.konan.file.use
import org.openjdk.nashorn.api.scripting.ScriptObjectMirror
import java.nio.Buffer
import java.nio.FloatBuffer
import java.util.concurrent.Callable
import java.util.function.Supplier
import javax.script.ScriptContext

class BoxTools {

    //lateinit var output: Buffer

    class StackAllocator(val size: Int) {
        val j = mutableListOf<FloatBuffer>()
        val stack = mutableListOf<Int>()
        val names = mutableListOf<String>()
        var at = 0
        var name = "/"

        fun push(n: String): AutoCloseable {
            stack.add(at)
            names.add(name)
            name = name + "/" + n

            return AutoCloseable { pop() }
        }

        fun pop() {
            at = stack.removeAt(stack.lastIndex)
            name = names.removeAt(names.lastIndex)
        }

        fun allocate(): FBuffer {
            return if (at >= j.size) {
                val r = FloatBuffer.allocate(size)
                j.add(r)
                val b = j.get(at++)
                // not going to like vector size change!
                zero(FBuffer(b, this))
            } else {
                val b = j.get(at++)
                // not going to like vector size change!
                zero(FBuffer(b, this))
            }
        }
    }

    companion object {
        val testScopes = mutableMapOf<Box, DynamicScope.Scope>();

        val size = 2048

        @JvmStatic
        val stack = object : ThreadLocal<StackAllocator>() {
            override fun initialValue(): StackAllocator {
                return StackAllocator(size)
            }
        }

        val stack2 = object : ThreadLocal<StackAllocator>() {
            override fun initialValue(): StackAllocator {
                return StackAllocator(size * 2)
            }
        }

        fun runBox(name: String, output: FBuffer, b: Box): Boolean {

            var keepGoing = true

            stack.get().push(name).use {
                //            this.output = output.a

                val bb = b.properties.get(Nashorn.boxBindings)

                DynamicScope.push(b.properties.getOrConstruct(IO.id)).use {

                    bb?.setAttribute("$", DynamicScope.at.get(), ScriptContext.ENGINE_SCOPE)
                    DynamicScope.at.get().remove("output")

                    b.properties.getOrConstruct(Definitions.a).entries.forEach {
                        try {
                            val o = it.value.apply(output)

                            val _output = DynamicScope.at.get().get("output")

                            val fin = DynamicScope.at.get().get("finished")
                            if (fin != null)
                                if (fin is Boolean) keepGoing = !fin

                            if (_output is FBuffer) {
                                output.copyFrom(_output)
                            } else if (_output is _FBuffer) {
                                output.copyFrom(_output.get())
                            } else if (o == null || ScriptObjectMirror.isUndefined(o)) {
                            } else if (o is FBuffer) {
                                output.copyFrom(o)
                            }
                        } catch (e: Throwable) {
                            Errors.tryToReportTo(e, "Exception in _.a.${it.key}, called from box `$b`", null)
                        }
                    }

                    bb?.setAttribute(
                        "$",
                        testScopes.computeIfAbsent(b, { DynamicScope.Scope(null, "scratch", "scratch", "scratch") }),
                        ScriptContext.ENGINE_SCOPE
                    )
                }
            }

            return keepGoing
        }

        fun runBox(name: String, output: FBuffer, b: Box, c: Callable<out Any?>, ret: () -> Any?): Boolean {
            var keepGoing = true
            stack.get().push(name).use {
                //            this.output = output.a

                val bb = b.properties.get(Nashorn.boxBindings)

                DynamicScope.push(b.properties.getOrConstruct(IO.id)).use {

//                    bb?.setAttribute("$", DynamicScope.at.get(), ScriptContext.ENGINE_SCOPE)
                    bb?.setAttribute("$", DynamicScope.Dollars(), ScriptContext.ENGINE_SCOPE)

                    DynamicScope.at.get().remove("output")

                    c.call()
                    val o = ret()

                    val _output = DynamicScope.at.get().get("output")

                    val fin = DynamicScope.at.get().get("finished")
                    if (fin != null)
                        if (fin is Boolean) keepGoing = !fin

                    if (_output is FBuffer) {
                        output.copyFrom(_output)
                    } else if (_output is _FBuffer) {
                        output.copyFrom(_output.get())
                    } else if (o == null || ScriptObjectMirror.isUndefined(o)) {
                    } else if (o is FBuffer) {
                        output.copyFromShaped(o)
                    } else if (o is _FBuffer) {
                        output.copyFromShaped(o.get())
                    }

                    bb?.setAttribute(
                        "$",
                        testScopes.computeIfAbsent(b, { DynamicScope.Scope(null, "scratch", "scratch", "scratch") }),
                        ScriptContext.ENGINE_SCOPE
                    )
                }
            }
            return keepGoing
        }

        fun runBox(name: String, output: FBuffer, b: Box, c: Supplier<Boolean>): Boolean {
            stack.get().push(name).use {
                //            this.output = output.a

                val bb = b.properties.get(Nashorn.boxBindings)


                DynamicScope.push(b.properties.getOrConstruct(IO.id)).use {

                    DynamicScope.at.get().remove("output")

                    bb?.setAttribute("$", DynamicScope.Dollars(), ScriptContext.ENGINE_SCOPE)
//                    bb?.setAttribute("$", DynamicScope.at.get(), ScriptContext.ENGINE_SCOPE)

                    var o = c.get()

                    val _output = DynamicScope.at.get().get("output")

                    val fin = DynamicScope.at.get().get("finished")
                    if (fin != null)
                        if (fin is Boolean) o = false

                    if (_output is FBuffer) {
                        output.copyFrom(_output)
                    } else if (_output is _FBuffer) {
                        output.copyFrom(_output.get())
                    } else if (_output != null) {
                        System.err.println(" can't understand output $_output")

                    }

                    bb?.setAttribute(
                        "$",
                        testScopes.computeIfAbsent(b, { DynamicScope.Scope(null, "scratch", "scratch", "scratch") }),
                        ScriptContext.ENGINE_SCOPE
                    )

                    return o
                }
            }
        }

        fun buffer(): FBuffer {
            return stack.get().allocate()
        }

        fun name(): String {
            return stack.get().name
        }
    }


}
