package fieldkotlin

import fieldlinker.AsMap
import kotlin.reflect.KProperty

class KIUtil {
    class Prop<T> {

        operator fun getValue(thisRef: AsMap, property: KProperty<*>) = thisRef.asMap_get(property.name) as T

        operator fun setValue(thisRef: AsMap, property: KProperty<*>, value: T) =
            thisRef.asMap_set(property.name, value)
    }
}
