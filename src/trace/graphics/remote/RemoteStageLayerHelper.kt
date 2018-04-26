package trace.graphics.remote

import org.java_websocket.WebSocket
import org.java_websocket.server.WebSocketServer
import trace.graphics.Stage

class RemoteStageLayerHelper(val websocket: WebSocketServer, val max_vertex: Int, val max_element: Int, val element_dim: Int, var channel_name: String) {
    var fill: RemoteLayer? = null
    var stroke: RemoteLayer? = null
    var points: RemoteLayer? = null

    var seenConnections = mutableSetOf<WebSocket>()

    var previousSide = 0

    fun update(s: Stage.ShaderGroup) {
        if (s.line.vertexLimit > 0 || stroke!=null) {
            if (stroke == null)
                stroke = RemoteLayer(websocket, max_vertex, max_element, 2, channel_name + "_s")
            stroke!!.copy(s.line, s.doTexture)
            stroke!!.send()
        }
        if (s.planes.vertexLimit > 0 || fill!=null) {
            if (fill == null)
                fill = RemoteLayer(websocket, max_vertex, max_element, 3, channel_name + "_f")
            fill!!.copy(s.planes, s.doTexture)
            fill!!.send()
        }
        if (s.points.vertexLimit > 0 || points!=null) {
            if (points == null)
                points = RemoteLayer(websocket, max_vertex, max_element, 0, channel_name + "_p")
            points!!.copy(s.points, s.doTexture)
            points!!.send()
        }

        if (previousSide!=s.sides)
        {
            previousSide = s.sides
            websocket.connections().forEach {
                setSide(it, channel_name+"_s", s.sides)
                setSide(it, channel_name+"_f", s.sides)
                setSide(it, channel_name+"_p", s.sides)
            }
        }
        else
        {
            websocket.connections().forEach {
                if (!seenConnections.contains(it))
                {
                    setSide(it, channel_name+"_s", s.sides)
                    setSide(it, channel_name+"_f", s.sides)
                    setSide(it, channel_name+"_p", s.sides)
                }
            }

        }

        if (s.textureFilename!=null)
        {
            websocket.connections().forEach {
                if (!seenConnections.contains(it))
                {
                    RemoteTexture(websocket, channel_name+"_s").sendFile(s.textureFilename!!)
                    RemoteTexture(websocket, channel_name+"_f").sendFile(s.textureFilename!!)
                    RemoteTexture(websocket, channel_name+"_p").sendFile(s.textureFilename!!)
                }
            }
        }

        seenConnections.clear()
        seenConnections.addAll(websocket.connections())

    }

    private fun setSide(websocket: WebSocket, s: String, sides: Int) {
        println(" limiting side of $s to $sides")
        var s2 = mangleName(s)
        websocket.send("if (meshes[\"${s2}\"]) meshes[\"${s2}\"].layers.set($sides)")
    }

    private fun mangleName(channel_name: String): String{

        if (channel_name.length > 30) {
            return channel_name.substring(0, 15) + channel_name.hashCode()  + channel_name.substring(channel_name.length-2)
        }
        return channel_name
    }


}