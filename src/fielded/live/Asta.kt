package fielded.live

import field.linalg.Vec2
import field.linalg.Vec3
import field.linalg.Vec4
import field.utility.OverloadedMath
import field.utility.Vec2
import field.utility.Vec3
import fieldnashorn.babel.SourceTransformer
import jdk.nashorn.api.tree.*
import java.lang.reflect.Method
import java.util.*
import java.util.function.Function

/*
    AST based rewriting
 */
class Asta {

    var currentMapping = mutableMapOf<Pair<Int, Int>, Pair<Int, Int>>()
    var lastReturn = mutableMapOf<Pair<Int, Int>, Double>()
    var currentContents: String? = null
    var baseContents: String? = null

    fun __LNC__(start: Int, end: Int, def: Double): Double {
        val c = currentContents
        if (c != null) {
            val m = currentMapping.get(start to end)
            if (m != null) {
                val newText = c.substring(m.first until m.second)
                val dnl = newText.toDoubleOrNull()
                if (dnl != null) {
                    lastReturn.put(start to end, dnl)
                    return dnl
                } else {
                    if (debug) println(" text updated to non-number " + newText + " we could try evaluating it?")
                    return lastReturn.getOrDefault(start to end, def)
                }
            } else {
                if (currentMapping.size == 0) // no changes
                {
                    return def
                } else {
                    // indicate failure with closest matching area ?
                    if (debug) println(" couldn't find mapping for number, structural change? ")
                    return lastReturn.getOrDefault(start to end, def)
                }

            }
        }
        return def
    }

    companion object {

        @JvmStatic
        var debug = false

        @JvmStatic
        fun __MINUS__(a: Any?, b: Any?): Any? {

            if (a is Number && b is Number) return a.toDouble() - b.toDouble()

            if (a == null) throw NullPointerException("can't subtract `$a` and `$b`")
            if (b == null) throw NullPointerException("can't subtract `$a` and `$b`")

            return when (a) {
                is Int -> return when (b) {
                    is Int -> a - b
                    is Float -> a - b
                    is Double -> a - b
                    is Long -> a - b
                    is Vec2 -> Vec2(a, a).sub(b)
                    is Vec3 -> Vec3(a, a, a).sub(b)
                    is Vec4 -> Vec4(a.toDouble(), a.toDouble(), a.toDouble(), 1.0).sub(b)
                    is OverloadedMath -> b.__rsub__(a)
                    else -> throw NullPointerException("can't subtract `$a` and `$b`")
                }
                is Float -> return when (b) {
                    is Int -> a - b
                    is Float -> a - b
                    is Double -> a - b
                    is Long -> a - b
                    is Vec2 -> Vec2(a, a).sub(b)
                    is Vec3 -> Vec3(a, a, a).sub(b)
                    is Vec4 -> Vec4(a.toDouble(), a.toDouble(), a.toDouble(), 1.0).sub(b)
                    is OverloadedMath -> b.__rsub__(a)
                    else -> throw NullPointerException("can't subtract `$a` and `$b`")
                }
                is Double -> return when (b) {
                    is Int -> a - b
                    is Float -> a - b
                    is Double -> a - b
                    is Long -> a - b
                    is Vec2 -> Vec2(a, a).sub(b)
                    is Vec3 -> Vec3(a, a, a).sub(b)
                    is Vec4 -> Vec4(a.toDouble(), a.toDouble(), a.toDouble(), 1.0).sub(b)
                    is OverloadedMath -> b.__rsub__(a)
                    else -> throw NullPointerException("can't subtract `$a` and `$b`")
                }
                is Long -> return when (b) {
                    is Int -> a - b
                    is Float -> a - b
                    is Double -> a - b
                    is Long -> a - b
                    is Vec2 -> Vec2(a, a).sub(b)
                    is Vec3 -> Vec3(a, a, a).sub(b)
                    is Vec4 -> Vec4(a.toDouble(), a.toDouble(), a.toDouble(), 1.0).sub(b)
                    is OverloadedMath -> b.__rsub__(a)
                    else -> throw NullPointerException("can't subtract `$a` and `$b`")
                }
                is Vec2 -> return when (b) {
                    is Int -> Vec2(a).sub(Vec2(b, b))
                    is Float -> Vec2(a).sub(Vec2(b, b))
                    is Double -> Vec2(a).sub(Vec2(b, b))
                    is Long -> Vec2(a).sub(Vec2(b, b))
                    is Vec2 -> Vec2(a).sub(b)
                    is Vec3 -> a.toVec3().sub(b)
                    is Vec4 -> Vec4(a.x, a.y, 0.0, 1.0).sub(b)
                    is OverloadedMath -> b.__rsub__(a)
                    else -> throw NullPointerException("can't subtract `$a` and `$b`")
                }
                is Vec3 -> return when (b) {
                    is Int -> Vec3(a).sub(Vec3(b, b, b))
                    is Float -> Vec3(a).sub(Vec3(b, b, b))
                    is Double -> Vec3(a).sub(Vec3(b, b, b))
                    is Long -> Vec3(a).sub(Vec3(b, b, b))
                    is Vec2 -> Vec3(a).sub(Vec3(b.x, b.y, 0))
                    is Vec3 -> Vec3(a).sub(b)
                    is Vec4 -> Vec4(a.x, a.y, a.z, 1.0).sub(b)
                    is OverloadedMath -> b.__rsub__(a)
                    else -> throw NullPointerException("can't subtract `$a` and `$b`")
                }
                is Vec4 -> return when (b) {
                    is Int -> Vec4(a).sub(Vec4(b.toDouble(), 0.0, 0.0, 0.0))
                    is Float -> Vec4(a).sub(Vec4(b.toDouble(), 0.0, 0.0, 0.0))
                    is Double -> Vec4(a).sub(Vec4(b.toDouble(), 0.0, 0.0, 0.0))
                    is Long -> Vec4(a).sub(Vec4(b.toDouble(), 0.0, 0.0, 0.0))
                    is Vec2 -> Vec4(a).sub(Vec4(b.x, b.y, 0.0, 0.0))
                    is Vec3 -> Vec4(a).sub(Vec4(b.x, b.y, b.z, 0.0))
                    is Vec4 -> Vec4(a).sub(b)
                    is OverloadedMath -> b.__rsub__(a)
                    else -> throw NullPointerException("can't subtract `$a` and `$b`")
                }
                is OverloadedMath -> a.__sub__(b)
                else -> throw NullPointerException("can't subtract `$a` and `$b`")
            }
        }

        @JvmStatic
        fun __PLUS__(a: Any?, b: Any?): Any? {

            if (a is Number && b is Number) return a.toDouble() + b.toDouble()

            if (a == null) throw NullPointerException("can't add `$a` and `$b`")
            if (b == null) throw NullPointerException("can't add `$a` and `$b`")

            return when (a) {
                is Int -> return when (b) {
                    is Int -> a + b
                    is Float -> a + b
                    is Double -> a + b
                    is Long -> a + b
                    is Vec2 -> Vec2(a, a).add(b)
                    is Vec3 -> Vec3(a, a, a).add(b)
                    is Vec4 -> Vec4(a.toDouble(), a.toDouble(), a.toDouble(), 1.0).add(b)
                    is OverloadedMath -> b.__radd__(a)
                    else -> throw NullPointerException("can't add `$a` and `$b`")
                }
                is Float -> return when (b) {
                    is Int -> a + b
                    is Float -> a + b
                    is Double -> a + b
                    is Long -> a + b
                    is Vec2 -> Vec2(a, a).add(b)
                    is Vec3 -> Vec3(a, a, a).add(b)
                    is Vec4 -> Vec4(a.toDouble(), a.toDouble(), a.toDouble(), 1.0).add(b)
                    is OverloadedMath -> b.__radd__(a)
                    else -> throw NullPointerException("can't add `$a` and `$b`")
                }
                is Double -> return when (b) {
                    is Int -> a + b
                    is Float -> a + b
                    is Double -> a + b
                    is Long -> a + b
                    is Vec2 -> Vec2(a, a).add(b)
                    is Vec3 -> Vec3(a, a, a).add(b)
                    is Vec4 -> Vec4(a.toDouble(), a.toDouble(), a.toDouble(), 1.0).add(b)
                    is OverloadedMath -> b.__radd__(a)
                    else -> throw NullPointerException("can't add `$a` and `$b`")
                }
                is Long -> return when (b) {
                    is Int -> a + b
                    is Float -> a + b
                    is Double -> a + b
                    is Long -> a + b
                    is Vec2 -> Vec2(a, a).add(b)
                    is Vec3 -> Vec3(a, a, a).add(b)
                    is Vec4 -> Vec4(a.toDouble(), a.toDouble(), a.toDouble(), 1.0).add(b)
                    is OverloadedMath -> b.__radd__(a)
                    else -> throw NullPointerException("can't add `$a` and `$b`")
                }
                is Vec2 -> return when (b) {
                    is Int -> Vec2(a).add(Vec2(b, b))
                    is Float -> Vec2(a).add(Vec2(b, b))
                    is Double -> Vec2(a).add(Vec2(b, b))
                    is Long -> Vec2(a).add(Vec2(b, b))
                    is Vec2 -> Vec2(a).add(b)
                    is Vec3 -> a.toVec3().add(b)
                    is Vec4 -> Vec4(a.x, a.y, 0.0, 1.0).add(b)
                    is OverloadedMath -> b.__radd__(a)
                    else -> throw NullPointerException("can't add `$a` and `$b`")
                }
                is Vec3 -> return when (b) {
                    is Int -> Vec3(a).add(Vec3(b, b, b))
                    is Float -> Vec3(a).add(Vec3(b, b, b))
                    is Double -> Vec3(a).add(Vec3(b, b, b))
                    is Long -> Vec3(a).add(Vec3(b, b, b))
                    is Vec2 -> Vec3(a).add(Vec3(b.x, b.y, 0))
                    is Vec3 -> Vec3(a).add(b)
                    is Vec4 -> Vec4(a.x, a.y, a.z, 1.0).add(b)
                    is OverloadedMath -> b.__radd__(a)
                    else -> throw NullPointerException("can't add `$a` and `$b`")
                }
                is Vec4 -> return when (b) {
                    is Int -> Vec4(a).add(Vec4(b.toDouble(), 0.0, 0.0, 0.0))
                    is Float -> Vec4(a).add(Vec4(b.toDouble(), 0.0, 0.0, 0.0))
                    is Double -> Vec4(a).add(Vec4(b.toDouble(), 0.0, 0.0, 0.0))
                    is Long -> Vec4(a).add(Vec4(b.toDouble(), 0.0, 0.0, 0.0))
                    is Vec2 -> Vec4(a).add(Vec4(b.x, b.y, 0.0, 0.0))
                    is Vec3 -> Vec4(a).add(Vec4(b.x, b.y, b.z, 0.0))
                    is Vec4 -> Vec4(a).add(b)
                    is OverloadedMath -> b.__radd__(a)
                    else -> throw NullPointerException("can't add `$a` and `$b`")
                }
                is OverloadedMath -> a.__add__(b)
                else -> throw NullPointerException("can't add `$a` and `$b`")
            }
        }

        @JvmStatic
        fun __MULTIPLY__(a: Any?, b: Any?): Any? {

            if (a is Number && b is Number) return a.toDouble() * b.toDouble()

            if (a == null) throw NullPointerException("can't multiply `$a` and `$b`")
            if (b == null) throw NullPointerException("can't multiply `$a` and `$b`")

            return when (a) {
                is Int -> return when (b) {
                    is Int -> a * b
                    is Float -> a * b
                    is Double -> a * b
                    is Long -> a * b
                    is Vec2 -> Vec2(a, a).mul(b)
                    is Vec3 -> Vec3(a, a, a).mul(b)
                    is Vec4 -> Vec4(a.toDouble(), a.toDouble(), a.toDouble(), 1.0).mul(b)
                    is OverloadedMath -> b.__rmul__(a)
                    else -> throw NullPointerException("can't multiply `$a` and `$b`")
                }
                is Float -> return when (b) {
                    is Int -> a * b
                    is Float -> a * b
                    is Double -> a * b
                    is Long -> a * b
                    is Vec2 -> Vec2(a, a).mul(b)
                    is Vec3 -> Vec3(a, a, a).mul(b)
                    is Vec4 -> Vec4(a.toDouble(), a.toDouble(), a.toDouble(), 1.0).mul(b)
                    is OverloadedMath -> b.__rmul__(a)
                    else -> throw NullPointerException("can't multiply `$a` and `$b`")
                }
                is Double -> return when (b) {
                    is Int -> a * b
                    is Float -> a * b
                    is Double -> a * b
                    is Long -> a * b
                    is Vec2 -> Vec2(a, a).mul(b)
                    is Vec3 -> Vec3(a, a, a).mul(b)
                    is Vec4 -> Vec4(a.toDouble(), a.toDouble(), a.toDouble(), 1.0).mul(b)
                    is OverloadedMath -> b.__rmul__(a)
                    else -> throw NullPointerException("can't multiply `$a` and `$b`")
                }
                is Long -> return when (b) {
                    is Int -> a * b
                    is Float -> a * b
                    is Double -> a * b
                    is Long -> a * b
                    is Vec2 -> Vec2(a, a).mul(b)
                    is Vec3 -> Vec3(a, a, a).mul(b)
                    is Vec4 -> Vec4(a.toDouble(), a.toDouble(), a.toDouble(), 1.0).mul(b)
                    is OverloadedMath -> b.__rmul__(a)
                    else -> throw NullPointerException("can't multiply `$a` and `$b`")
                }
                is Vec2 -> return when (b) {
                    is Int -> Vec2(a).mul(Vec2(b, b))
                    is Float -> Vec2(a).mul(Vec2(b, b))
                    is Double -> Vec2(a).mul(Vec2(b, b))
                    is Long -> Vec2(a).mul(Vec2(b, b))
                    is Vec2 -> Vec2(a).mul(b)
                    is Vec3 -> a.toVec3().mul(b)
                    is Vec4 -> Vec4(a.x, a.y, 0.0, 1.0).mul(b)
                    is OverloadedMath -> b.__rmul__(a)
                    else -> throw NullPointerException("can't multiply `$a` and `$b`")
                }
                is Vec3 -> return when (b) {
                    is Int -> Vec3(a).mul(Vec3(b, b, b))
                    is Float -> Vec3(a).mul(Vec3(b, b, b))
                    is Double -> Vec3(a).mul(Vec3(b, b, b))
                    is Long -> Vec3(a).mul(Vec3(b, b, b))
                    is Vec2 -> Vec3(a).mul(Vec3(b.x, b.y, 0))
                    is Vec3 -> Vec3(a).mul(b)
                    is Vec4 -> Vec4(a.x, a.y, a.z, 1.0).mul(b)
                    is OverloadedMath -> b.__rmul__(a)
                    else -> throw NullPointerException("can't multiply `$a` and `$b`")
                }
                is Vec4 -> return when (b) {
                    is Int -> Vec4(a).mul(Vec4(b.toDouble(), 0.0, 0.0, 0.0))
                    is Float -> Vec4(a).mul(Vec4(b.toDouble(), 0.0, 0.0, 0.0))
                    is Double -> Vec4(a).mul(Vec4(b.toDouble(), 0.0, 0.0, 0.0))
                    is Long -> Vec4(a).mul(Vec4(b.toDouble(), 0.0, 0.0, 0.0))
                    is Vec2 -> Vec4(a).mul(Vec4(b.x, b.y, 0.0, 0.0))
                    is Vec3 -> Vec4(a).mul(Vec4(b.x, b.y, b.z, 0.0))
                    is Vec4 -> Vec4(a).mul(b)
                    is OverloadedMath -> b.__rmul__(a)
                    else -> throw NullPointerException("can't multiply `$a` and `$b`")
                }
                is OverloadedMath -> a.__mul__(b)
                else -> throw NullPointerException("can't multiply `$a` and `$b`")
            }
        }
    }

    val accessorMap: Map<Tree.Kind, List<Method>> = Tree.Kind.values().filter { it.asInterface() != null }.map {
        val access = it.asInterface().declaredMethods.filter {
            (it.parameterCount == 0 && Tree::class.java.isAssignableFrom(it.returnType)) || (it.parameterCount == 0 && List::class.java.isAssignableFrom(it.returnType))
        }.toList()
        it to access
    }.toMap()

    val trapKinds = setOf(Tree.Kind.PLUS, Tree.Kind.MINUS, Tree.Kind.MULTIPLY, Tree.Kind.XOR)

    fun parse(v: String) {
        val p = Parser.create("--language=es6")
        val r = p.parse("<<internal>>", v, {
            print("diagnostic: $it")
        })

        r.sourceElements.forEach {
            print("\n\n sourceElement: $it\n\n")
            recurOver(it, 0, v)
        }

    }

    fun transformer(): SourceTransformer {

        val r = object : SourceTransformer {
            override fun transform(c: String, fragment: Boolean): field.utility.Pair<String, Function<Int, Int>> {
                val v = parseAndReconstruct(c, {
                    if (it.kind.ordinal < Diagnostic.Kind.WARNING.ordinal) {
                        throw SourceTransformer.TranslationFailedException(it.message + " on line " + it.lineNumber + " at col " + it.columnNumber)
                    }
                    print("Diagnostic: $it")
                })

                if (!fragment) {
                    currentContents = c
                    baseContents = c
                    currentMapping = mutableMapOf()
                }

                return field.utility.Pair(v, Function { x: Int -> x })
            }

            override fun incrementalUpdate(now: String?) {

                if (debug) println(" incrementalUpdate! ")
                if (baseContents != null && now != null) {
                    currentMapping = MappingGenerator().gum(baseContents!!, now)
                    currentContents = now
                }

            }
        }

        return r
    }

    var withOverloading = false
    var withLiveNumbers = false


    @JvmOverloads
    fun parseAndReconstruct(v: String, d: (Diagnostic) -> Unit = { print("diagnostic: $it") }): String {

        if (Regex("//(?:(?!\\n|\\r)\\s)aster").find(v) != null) return v
        withOverloading = (Regex("//(?:(?!\\n|\\r)\\s)aster(.*)overload").find(v) != null)
        withLiveNumbers = (Regex("//(?:(?!\\n|\\r)\\s)aster(.*)live").find(v) != null)

        val p = Parser.create("--language=es6")
        val r = p.parse("<<internal>>", v + "\n", d)

        var actions = mutableListOf<() -> Unit>()

        var replaced = v

        var on = false;

        r.sourceElements.forEach {
            print("\n\n sourceElement: $it\n\n")

            var (s, e) = startAndEndPositionFor(it)

            actions.add { replaced = replaced.replaceRange(s, e, reconstructOver(it, 0, v)) }
        }

        actions.reversed().forEach { it() }

//        if (debug) println(" final text:\n$replaced\n------")

        return replaced
    }

    private fun recurOver(tree: Tree, i: Int, v: String) {
        if (debug) println("${indent(i)}" + tree.kind)
        val children = childrenFor(tree)

        var (start, end) = startAndEndPositionFor(tree)

        var text = v.substring(start, end)

//        if (debug) println(" node covers ${start} -> ${end} = $text")

        var n = children.size - 1
        for (c in children.asReversed()) {

//            if (debug) println(" replace range ${(c.startPosition - start).toInt()..(c.endPosition - start - 1).toInt() - 1}  : '" + text + "'")

            val (s, e) = startAndEndPositionFor(c)

            text = text.replaceRange((c.startPosition - start).toInt()..(c.endPosition - start - 1).toInt(), "<<$n>>")
            n--
        }

        if (tree is BinaryTree) {
            if (debug) println(" ** binary operator :" + tree)
        }

        if (debug) println("${indent(i)}'" + text + "'")

        children.forEach { recurOver(it!!, i + 2, v) }
    }

    private fun reconstructOver(tree: Tree, i: Int, v: String): String {
        if (debug) println("${indent(i)}" + tree.kind)
        val children = childrenFor(tree)

        var (start, end) = startAndEndPositionFor(tree, true)

        var text = v.substring(start, end)

        if (debug) println("${indent(i)} node covers ${start} -> ${end} = $text")

        if (withOverloading && tree is BinaryTree && tree.kind in trapKinds) {
            if (debug) println(" ** binary operator :" + tree)

            val left = children.get(0)
            val right = children.get(1)

            val (sr, er) = startAndEndPositionFor(right)
            val rightR = reconstructOver(right, i + 5, v)//text.replaceRange((sr - start).toInt()..(er - start - 1).toInt(), )


            val (sc, ec) = startAndEndPositionFor(left)
            val leftR = reconstructOver(left, i + 5, v)//rightR.replaceRange((sc - start).toInt()..(ec - start - 1).toInt())

            if (debug) println(" -> leftR : $leftR")

            return "__" + tree.kind + "__((" + leftR + "),(" + rightR + "))"
        }

        if (withLiveNumbers && tree is LiteralTree && tree.kind.equals(Tree.Kind.NUMBER_LITERAL)) {
            return "__NUMBER_LITERAL__(" + start + ", " + end + ", " + text + ")"
        }


//        if (tree is CompoundAssignmentTree && tree.kind in trapKinds) {
//            if (debug) println(" ** binary operator :" + tree);
//
//            val left = children.get(0)
//            val right = children.get(1)
//
//            val (sr, er) = startAndEndPositionFor(right);
//            val rightR = reconstructOver(right, i + 5, v);//text.replaceRange((sr - start).toInt()..(er - start - 1).toInt(), )
//
//
//            val (sc, ec) = startAndEndPositionFor(left);
//            val leftR = reconstructOver(left, i + 5, v);//rightR.replaceRange((sc - start).toInt()..(ec - start - 1).toInt())
//
//            if (debug) println(" -> leftR : $leftR")
//
//            return "__" + tree.kind + "__((" + leftR + "),(" + rightR + "))"
//        }


        var n = children.size - 1
        for (c in children.asReversed()) {

            val (s, e) = startAndEndPositionFor(c)

            if (debug) println("${indent(i + 2)} replace range ${(s - start).toInt()..(e - start - 1).toInt()}  : '" + text.substring((s - start).toInt()..(e - start - 1).toInt()) + "'")


//            text = text.replaceRange((c.startPosition - start).toInt()..(c.endPosition - start - 1).toInt(), reconstructOver(c, i + 5, v))
            text = text.replaceRange((s - start).toInt()..(e - start - 1).toInt(), reconstructOver(c, i + 5, v))
            n--
        }


        if (debug) println("${indent(i)} becomes = '" + text + "'")

//        children.forEach { recurOver(it!!, i + 2, v) }
        return text
    }

    private fun childrenFor(tree: Tree): List<Tree> {
        if (debug) println(" children for :" + tree + " " + tree.javaClass + " " + tree.kind)
        return accessorMap[tree.kind]!!.flatMap {
            if (debug) println(it)
            try {
                val rr = it.invoke(tree)
                when (rr) {
                    null -> {
                        if (debug) println("warning: $it returned null on ${tree.kind} / $tree")
                        Collections.EMPTY_LIST
                    }
                    is Tree -> Collections.singleton(rr)
                    is List<*> -> rr
                    else -> {
                        if (debug) println("warning: $it returned $rr (unknown) on ${tree.kind} / $tree")
                        Collections.EMPTY_LIST
                    }
                }
            } catch (e: java.lang.IllegalArgumentException) {
                e.printStackTrace()
                Collections.EMPTY_LIST
            }
        }.filter { it != null }.map { it as Tree }.sortedBy { it!!.startPosition }.toList()
    }

    private fun startAndEndPositionFor(c: Tree, correct: Boolean = true): Pair<Int, Int> {
        var start = c.startPosition.toInt()
        var end = c.endPosition.toInt()
        if (start == end && correct) {
            val cc = childrenFor(c)
            if (cc.size == 0) return start to end

            val ccSE = cc.map { startAndEndPositionFor(it) }.toList()
            val min = ccSE.minBy { it.first }!!.first
            val max = ccSE.maxBy { it.second }!!.second
            return min to max
        }
        return start to end
    }

    private fun indent(i: Int): String {
        var q = ""
        while (q.length < i) q = q + " "
        return q
    }
}