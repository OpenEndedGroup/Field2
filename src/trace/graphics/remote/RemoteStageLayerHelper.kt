package trace.graphics.remote

import org.java_websocket.WebSocket
import org.java_websocket.server.WebSocketServer
import trace.graphics.Stage

class RemoteStageLayerHelper(val websocket: WebSocketServer, val max_vertex: Int, val max_element: Int, val element_dim: Int, var channel_name: String) {
    var fill: RemoteLayer? = null
    var stroke: RemoteLayer? = null
    var points: RemoteLayer? = null

    var seenConnections = mutableSetOf<WebSocket>()

    fun update(s: Stage.ShaderGroup) {
        if (s.line.vertexLimit > 0) {
            if (stroke == null)
                stroke = RemoteLayer(websocket, max_vertex, max_element, 2, channel_name + "_s")
            stroke!!.copy(s.line, s.doTexture)
            stroke!!.send()
        }
        if (s.planes.vertexLimit > 0) {
            if (fill == null)
                fill = RemoteLayer(websocket, max_vertex, max_element, 3, channel_name + "_f")
            stroke!!.copy(s.planes, s.doTexture)
            stroke!!.send()
        }
        if (s.points.vertexLimit > 0) {
            if (points == null)
                points = RemoteLayer(websocket, max_vertex, max_element, 0, channel_name + "_p")
            stroke!!.copy(s.points, s.doTexture)
            stroke!!.send()
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
            seenConnections.clear()
            seenConnections.addAll(websocket.connections())
        }
    }


}