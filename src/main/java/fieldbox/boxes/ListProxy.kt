package fieldbox.boxes

import field.utility.Conversions
import fieldbox.execution.Completion
import fieldbox.execution.HandlesCompletion
import fielded.boxbrowser.ObjectToHTML
import fieldlinker.AsMap

class ListProxy<T>(val of: List<T>) : fieldlinker.AsMap, ObjectToHTML.MasqueradesAs, HandlesCompletion, Collection<T> {
    override val size: Int
        get() = of.size

    override fun contains(element: T): Boolean {
        return of.contains(element)
    }

    override fun containsAll(elements: Collection<T>): Boolean {
        return of.containsAll(elements)
    }

    override fun isEmpty(): Boolean {
        return of.isEmpty()
    }

    override fun iterator(): Iterator<T> {
        return of.iterator()
    }

    override fun getCompletionsFor(prefix: String): List<Completion>? {
        return null
    }

    override fun masqueradesAs(): Any {
        return of
    }

    override fun asMap_isProperty(p: String): Boolean {
        return of.any {
            when (it) {
                is AsMap -> it.asMap_isProperty(p)
                else -> false
            }
        }
    }

    override fun asMap_call(a: Any, b: Any): Any? {
        return ListProxy<Any?>(of.map {
            when (it) {
                is AsMap -> it.asMap_call(a, b)
                else -> {
                    val it2 = Conversions.convert(it, java.util.function.Function::class.java);
                    if (it2==null || it2==it)
                    {
                        null
                    }
                    else
                    {
                        (it2 as java.util.function.Function<Any, *>).apply(b)
                    }
                }
            }
        }.toList<Any?>())
    }

    override fun asMap_get(p: String): Any? {
        return ListProxy<Any?>(of.map {
            when (it) {
                is AsMap -> it.asMap_get(p)
                else -> null
            }
        }.toList())
    }

    override fun asMap_set(p: String, o: Any): Any? {
        return ListProxy<Any?>(of.map {
            when (it) {
                is AsMap -> it.asMap_set(p, o)
                else -> null
            }
        }.toList())
    }

    override fun asMap_new(a: Any): Any? {
        return ListProxy<Any?>(of.map {
            when (it) {
                is AsMap -> it.asMap_new(a)
                else -> null
            }
        }.toList())
    }

    override fun asMap_new(a: Any, b: Any): Any? {
        return ListProxy<Any?>(of.map {
            when (it) {
                is AsMap -> it.asMap_new(a, b)
                else -> null
            }
        }.toList())
    }

    override fun asMap_getElement(element: Int): Any? {
        return of[element]
    }

    override fun asMap_getElement(element: Any) : Any?{
        return of[(element as Number).toInt()]
    }


       override fun asMap_setElement(element: Int, o: Any): Any? {
        throw IllegalArgumentException(" can't set an element of this array")
    }

    override fun asMap_delete(p: Any): Boolean {
        return of.any {
            when (it) {
                is AsMap -> it.asMap_delete(p)
                else -> false
            }
        }
    }


}
