package field.utility

import fieldbox.boxes.Box
import fieldbox.io.IO

class Versioning {
    val VERSION = "__VERSION__"
    val PARENT = "__PARENT__"

    fun commit(box: Box) {
        val inplay = box.properties.map.keys.filter { it.name.startsWith(VERSION) }.map { it.name.replaceFirst(VERSION, "") }
        if (inplay.isEmpty()) return

        inplay.forEach {
            // PARENT is now the hand edited file, this is the edited version of VERSION
            box.properties.put(Dict.Prop(PARENT + it), copy<Any>(box.properties.get(Dict.Prop(it))))
        }
    }

    fun clearAllFailure(root: Box) {
        root.breadthFirst(root.allDownwardsFrom()).forEach { clearFailure(it) }
    }

    fun merge(box: Box) {
        val inplay = box.properties.map.keys.filter { it.name.startsWith(VERSION) }.map { it.name.replaceFirst(VERSION, "") }
        if (inplay.isEmpty()) return

        clearFailure(box)

        inplay.forEach {
            val A = box.properties.get<Any>(Dict.Prop(VERSION + it))
            val B = box.properties.get<Any>(Dict.Prop(PARENT + it))
            val C = box.properties.get<Any>(Dict.Prop(it))

            val ff = threeWayMerge(A, B, C)
            box.properties.put(Dict.Prop(it), ff.first)
            // VERSION is the code that the machine want's it to be
            box.properties.put(Dict.Prop(VERSION + it), C)


            if (ff.second)
                markFailure(box)
        }
    }

    private fun markFailure(box: Box) {

    }

    private fun clearFailure(box: Box) {

    }

    fun version(p: Dict.Prop<*>, b: Box) {
        p.attributes.put(IO.persistent, true)
        val cc = Dict.Prop<Any>(VERSION + p.getName()).toCanon<Any>()
        cc.attributes.put(IO.persistent, true)
        if (!b.properties.has(cc) && b.properties.has(p))
            b.properties.put(cc, copy<Any>(b.properties.get(p)))
    }

    private fun <T> threeWayMerge(a: T, b: Any, c: Any): kotlin.Pair<T, Boolean> {

        if (a!=b || a!=c)
        {
            println(" three way merge ")
            println(a)
            println(b)
            println(c)
        }
        else
            return (c as T) to false

        when (a) {
            is String -> {
                val patches = MergeThreeWays().patch_make("" + a, "" + c);
                val applied = MergeThreeWays().patch_apply(patches, "" + b)

                println(" merged and got")
                println(applied.first)

                return (applied.first as T) to (applied.second.any { !it })
            }
            is Double -> {
                val d = (b as Number).toDouble() - (a).toDouble()
                return (((c as Number).toDouble() + d) as T) to false
            }
            is Float -> {
                val d = (b as Number).toDouble() - (a).toDouble()
                return (((c as Number).toDouble() + d).toFloat() as T) to false
            }
            is Integer -> {
                val d = (b as Number).toDouble() - (a).toDouble()
                return (((c as Number).toDouble() + d).toInt() as T) to false
            }
            is Rect -> {

                val ar = a;
                val br = b as Rect
                val cr = c as Rect

                val (nx, _) = threeWayMerge(ar.x, br.x, cr.x)
                val (ny, _) = threeWayMerge(ar.y, br.y, cr.y)
                val (nxw, _) = threeWayMerge(ar.xw, br.xw, cr.xw)
                val (nyh, _) = threeWayMerge(ar.yh, br.yh, cr.yh)

                return (Rect(nx, ny, nxw - nx, nyh - ny) as T) to false

            }
        }

        return (c as T) to true
    }

    private fun <T> copy(x: T): T {

        if (x == null) return x

        when (x) {
            is String -> {
                return x
            }
            is Number -> {
                return x
            }
            is Mutable<*> -> {
                return x.duplicate() as T
            }
        }

        throw IllegalArgumentException("can't copy a $x")

    }

}

val Rect.xw: Float
    get() {
        return this.x + this.w
    }

val Rect.yh: Float
    get() {
        return this.y + this.h
    }

