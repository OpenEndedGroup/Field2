package trace.graphics.remote

import com.google.common.collect.HashBiMap
import field.utility.IdempotencyMap
import org.json.JSONObject
import org.json.JSONString
import org.json.JSONWriter
import org.nanohttpd.protocols.websockets.WebSocket
import java.util.function.Consumer

class RPC {

    val newServer = IdempotencyMap<Consumer<String>>(Consumer::class.java)

    val messages = IdempotencyMap<Consumer<Any>>(Consumer::class.java)

    val store = HashMap<String, Any>()

    fun map(server: WebSocket, id: String) {
        if (id != null && !mapping.containsKey(server))
            newServer(server, id)

        mapping.put(server, id)
    }

    var ongoingID = object : ThreadLocal<String>() {
        override fun initialValue(): String {
            return ""
        }
    }

    fun newServer(server: WebSocket, id: String) {
        newServer.values.forEach {
            ongoingID.set(id)
            try {
                it.accept(id)
            } finally {
                ongoingID.set("")
            }
        }
    }

    fun handle(payload: JSONObject) {
        val name = payload.getString("name")
        val id = payload.getString("__originalid__")
        val value = payload.get("value")

        // don't know if jsonobjects are usable in Nashorn?!

        ongoingID.set(id)
        try {

            val handle = messages.get(name)
            if (handle == null) {
                if (store.containsKey(name) && store.get(name)!=value) {
                    store.put(name, value)
                    toOthers(name, value)
                }
            } else {
                handle.accept(value)
            }
        } finally {
            ongoingID.set("")
        }

    }

    fun toOthers(message: String, value: Any) {
        val id = ongoingID.get()

        toAll(message, value, { it.value != id })
    }

    fun toAll(message: String, value: Any, predicate: (Map.Entry<WebSocket, String>) -> Boolean = { true }) {
        var code = "__kv.recieve('" + message + "', _field.__var0)"

        val q = JSONObject()
        q.put("code", code)
        q.put("returnTo", "kv")
        q.put("__var0", value)
        q.put("launchable", false)
        q.put("noSandbox", true)

        mapping.entries.forEach {
            if (it.key.isOpen && predicate(it)) {
                it.key.send(code)
            }
        }

    }

    val mapping = HashBiMap.create<WebSocket, String>()


}