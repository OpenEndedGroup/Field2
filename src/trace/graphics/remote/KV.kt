package trace.graphics.remote

import org.json.JSONArray
import org.json.JSONObject

/** the world's most straightforward key-value store */
class KV(val channel : String, val syncService : (Int, String, String, JSONObject) -> Unit) {

    val store = mutableMapOf<String, JSONObject>()

    fun put(from: Int, name: String, value: JSONObject): Boolean {
        val changed = store.get(name).let {
            store.put(name, value);
            it == null || !it.equals(value)
        }

        if (changed)
            syncService(-from, channel, name, value); // everything but 'from'

        return changed
    }

    fun get(name: String): JSONObject? {
        return store.get(name);
    }

    fun syncTo(to: Int)
    {
        store.forEach {
            syncService(to, channel, it.key, it.value)
        }
    }


}