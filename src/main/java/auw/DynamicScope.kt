package auw

import field.utility.Dict
import field.utility.IdempotencyMap
import fieldbox.boxes.Box
import fieldnashorn.Nashorn.boxBindings
import org.jetbrains.kotlin.konan.file.use
import org.openjdk.nashorn.api.scripting.AbstractJSObject
import org.openjdk.nashorn.api.scripting.JSObject
import org.openjdk.nashorn.api.scripting.ScriptObjectMirror
import org.openjdk.nashorn.api.scripting.ScriptUtils
import java.lang.IllegalArgumentException
import java.util.function.Supplier
import javax.script.ScriptContext

// e.g lpf(a,b,c)
// compiles to lpf("/uid-in-current-scope")(a0,b0,c0)(a,b,c)
// effectively $.getOrConstruct("uid", lpf, a,b,c)([a,b,c])

// how about:
// lpf | [a,b,c]
// ast transforms to __or__(DynamicScope.getOrConstruct("uid-in-current-scope", lpf), [a,b,c])
// lpf | (a,b,c)
// or, if we were really good DynamicScope.getOrConstruct("uid-in-current-scope", lpf).apply(a,b,c)

// or, the original plan of 'reserved functions'
// lpf(a,b,c)
// turns into DynamicScope.getOrConstruct("uid-in-current-scope", lpf.__constructor__).apply(a,b,c)
// that's not so bad

// you could also have
// `lpf`(a,b,c)
// turns into DynamicScope.getOrConstruct("uid-in-current-scope", f`lpf`.__constructor__).apply(a,b,c)


// secondly we need $ to be DynamicScope.at.get() in javascript not just whatever scope the box is

class DynamicScope {
    class Scope(val parent: Scope?, val name: String, val path: String, val value: Any) : HashMap<String, Any>() {

        val children = mutableMapOf<String, Scope>()

        @JvmOverloads
        fun getInitialize(): Map<String, Any> {
            val s = this
            return object : IdempotencyMap<Any>(Any::class.java) {

                override fun _put(key: String?, v: Any?): Any? {
                    val r = super._put(key, v)

                    if (s.containsKey(key)) {
                        val out = s.get(key)

                        if (out != null && !ScriptObjectMirror.isUndefined(out))
                            return out
                    }

                    var vn = if (v is ScriptObjectMirror && v.isFunction)
                        v.call(v)
                    else v

                    if (vn != null)
                        s.put(key!!, vn!!)
                    else
                        s.remove(key!!)

                    return v
                }
            }
        }

        fun newBuffer(): FBuffer {
            return BoxTools.stack.get().allocate()
        }

        override fun clear() {
            super.clear()
            children.clear()
        }

        override fun toString(): String {
            return "scope`" + path + "`"
        }
    }

    class Dollars : AbstractJSObject() {
        override fun getMember(name: String?): Any? {
            if (name == "initialize")
                return DynamicScope.at.get().getInitialize()
            if (name == "newBuffer")
                return DynamicScope.at.get().newBuffer()
            if (name == "clear")
                return DynamicScope.at.get().clear()
            return DynamicScope.at.get().get(name)
        }

        override fun setMember(name: String?, value: Any?) {
            DynamicScope.at.get().put(name!!, value!!)
        }

        override fun toString(): String {
            return "Scope'" + DynamicScope.at.get().path + "'"
        }
    }

    companion object : AbstractJSObject() {

        @JvmStatic
        var root = Scope(null, "/", "/", "/")

        @JvmStatic
        val at = object : ThreadLocal<Scope>() {
            override fun initialValue(): Scope {
                return root
            }
        }

        @JvmStatic
        fun push(name: String): AutoCloseable {
            var here = at.get()

            var next = here.children.computeIfAbsent(name, { Scope(here, name, here.path + "/" + name, name) })

            at.set(next)
            return AutoCloseable { at.set(here) }

        }

        @JvmStatic
        fun <T> getOrConstruct(name: String, c: () -> T): T {
            val S = at.get()
            @Suppress("UNCHECKED_CAST")
            return S.children.getOrPut(name, {
                Scope(S, name, S.path + "/" + name, c()!!)
            }).value as T
        }


        @JvmStatic
        fun <T> getOrConstructClass(name: String, c: Class<T>): T {
            val S = at.get()
            @Suppress("UNCHECKED_CAST")
            return S.children.getOrPut(name, {
                Scope(S, name, S.path + "/" + name, c.newInstance()!!)
            }).value as T
        }

        @JvmStatic
        fun runInScope(name: String, b: Box): JSObject {
            val audio = b.properties.get(Definitions.audio)
            val len = (audio.getMember("length") as Number)?.toInt()
            return object : AbstractJSObject() {
                override fun call(thiz: Any?, vararg args: Any?): Any? {

                    val n = if (args == null) 0 else args.size
                    if (n != len)
                        throw IllegalArgumentException("audio function is expecting $len arguments but you provided $n")

                    return push(name).use {

                        val prev = b.properties.get(boxBindings).getAttribute("$")
                        var r = try {
//                            b.properties.get(boxBindings)
//                                .setAttribute("$", DynamicScope.at.get(), ScriptContext.ENGINE_SCOPE)

                            b.properties.get(boxBindings)
                                .setAttribute("$", Dollars(), ScriptContext.ENGINE_SCOPE)

                            audio.call(b, *args)
                        } finally {
                            if (prev != null)
                                b.properties.get(boxBindings).setAttribute("$", prev, ScriptContext.ENGINE_SCOPE)
                            else
                                b.properties.get(boxBindings).removeAttribute("$", ScriptContext.ENGINE_SCOPE)
                        }

                        if (r == null) return@use null
                        if (ScriptObjectMirror.isUndefined(r)) return@use null
                        if (r is FBuffer) return@use r
                        if (r is _FBuffer) return@use r.get()
                        return@use null
                    }
                }

                override fun isFunction(): Boolean {
                    return true
                }
            }
        }

        fun delete(name: String) {
            var here = at.get()
            val s = here.children.remove(name)

            // TODO: do something with s?
        }


    }

}