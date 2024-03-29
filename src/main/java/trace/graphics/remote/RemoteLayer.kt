package trace.graphics.remote

import field.graphics.BaseMesh
import fielded.webserver.NewNanoHTTPD
import java.net.SocketException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer

class RemoteLayer(val websocket: NewNanoHTTPD, val max_vertex: Int, val max_element: Int, val element_dim: Int, var channel_name: String) {

    var vertexBufferB: ByteBuffer
    var vertexBufferF: FloatBuffer
    var vertexChanged = false

    var colorBufferB: ByteBuffer
    var colorBufferF: FloatBuffer
    var colorChanged = false

    var textureBufferB: ByteBuffer
    var textureBufferF: FloatBuffer
    var textureChanged = false

    var elementBufferB: ByteBuffer
    var elementBufferI: IntBuffer
    var elementChanged = false

    var numVertex = 0
    var numElement = 0

    init {

        if (channel_name.length > 30) {
            channel_name = channel_name.substring(0, 15) + channel_name.hashCode() + channel_name.substring(channel_name.length - 2)
        }

        vertexBufferB = ByteBuffer.allocate(4 * max_vertex * 3 + 2 * 64).order(ByteOrder.nativeOrder())
        vertexBufferF = vertexBufferB.position(2 * 64).asFloatBuffer()

        colorBufferB = ByteBuffer.allocate(4 * max_vertex * 4 + 2 * 64).order(ByteOrder.nativeOrder())
        colorBufferF = colorBufferB.position(2 * 64).asFloatBuffer()

        textureBufferB = ByteBuffer.allocate(4 * max_vertex * 2 + 2 * 64).order(ByteOrder.nativeOrder())
        textureBufferF = textureBufferB.position(2 * 64).asFloatBuffer()

        elementBufferB = ByteBuffer.allocate(4 * max_element * element_dim + 2 * 64).order(ByteOrder.nativeOrder())
        elementBufferI = elementBufferB.position(2 * 64).asIntBuffer()


    }


    fun writeHeaders() {
        writeHeader('V', vertexBufferB, numVertex, 3);
        writeHeader('C', colorBufferB, numVertex, 4);
        writeHeader('T', textureBufferB, numVertex, 2);
        writeHeader('E', elementBufferB, numElement, element_dim);
    }

    fun writeHeader(c: Char, b: ByteBuffer, max: Int, dim: Int) {
        b.rewind()
        b.putInt(max)
        b.putChar(c)
        b.put(channel_name.length.toByte())
        b.putInt(dim);
        for (i in 0 until channel_name.length)
            b.putChar(channel_name.get(i))
        b.rewind()
    }

    fun copy(m: BaseMesh, doTexture: Boolean) {

        val v = m.vertex(true)
        vertexBufferF.rewind()
        for (i in 0 until Math.min(m.vertexLimit, max_vertex)) {
            val x = v.get(i * 3 + 0)
            val y = -v.get(i * 3 + 1)
            val z = v.get(i * 3 + 2)

            if (!vertexChanged) {
                val x2 = vertexBufferF.get(i * 3 + 0)
                val y2 = vertexBufferF.get(i * 3 + 1) // left handed!
                val z2 = vertexBufferF.get(i * 3 + 2)

                if (x2 != x || y2 != y || z2 != z)
                    vertexChanged = true;
            }
            vertexBufferF.put(i * 3 + 0, x)
            vertexBufferF.put(i * 3 + 1, y)
            vertexBufferF.put(i * 3 + 2, z)
        }

        val n = Math.min(m.vertexLimit, max_vertex)
        if (n != numVertex) {
            numVertex = n;
            vertexChanged = true
            textureChanged = doTexture
            colorChanged = true
        }

        val color = m.aux(1, 4)
        colorBufferF.rewind()
        for (i in 0 until Math.min(m.vertexLimit, max_vertex)) {
            val w = color.get(i * 4 + 3)


            var x = color.get(i * 4 + 0) * w
            val y = color.get(i * 4 + 1) * w
            val z = color.get(i * 4 + 2) * w

            if (!colorChanged) {
                val x2 = colorBufferF.get(i * 4 + 0)
                val y2 = colorBufferF.get(i * 4 + 1)
                val z2 = colorBufferF.get(i * 4 + 2)
                val w2 = colorBufferF.get(i * 4 + 3)

                if (x2 != x || y2 != y || z2 != z || w2 != w)
                    colorChanged = true;
            }
            colorBufferF.put(i * 4 + 0, x)
            colorBufferF.put(i * 4 + 1, y)
            colorBufferF.put(i * 4 + 2, z)
            colorBufferF.put(i * 4 + 3, w)
        }

        if (doTexture) {
            val texture = m.aux(4, 2)
            textureBufferF.rewind()
            for (i in 0 until Math.min(m.vertexLimit, max_vertex)) {
                val x = texture.get(i * 2 + 0)
                val y = 1 - texture.get(i * 2 + 1)

                if (!textureChanged) {
                    val x2 = textureBufferF.get(i * 2 + 0)
                    val y2 = textureBufferF.get(i * 2 + 1)

                    if (x2 != x || y2 != y)
                        textureChanged = true;
                }
                textureBufferF.put(i * 2 + 0, x)
                textureBufferF.put(i * 2 + 1, y)
            }
        }

        if (m.elementDimension > 0) {
            val element = m.elements()
            elementBufferI.rewind()
            for (i in 0 until Math.min(elementBufferI.limit(), Math.min(0xffff / m.elementDimension, m.elementLimit * m.elementDimension))) {
                val x = element.get(i)

                if (!elementChanged) {
                    val x2 = elementBufferI.get(i)

                    if (x2 != x)
                        elementChanged = true;
                }
                elementBufferI.put(i, x)

            }

//            print("checking $channel_name for size change $numElement =?= ${m.elementLimit} max is ${max_element/m.elementDimension}")
            val next = Math.min(max_element / m.elementDimension, Math.min(0xffff / m.elementDimension, m.elementLimit))

            if (numElement != next)
                elementChanged = true

            numElement = next;

        } else {
            elementChanged = numElement != 0
            numElement = 0
        }



        vertexBufferF.rewind()
        colorBufferF.rewind()
        textureBufferF.rewind()
        elementBufferI.rewind()

        writeHeaders()

    }

    val seenConnections = mutableSetOf<org.nanohttpd.protocols.websockets.WebSocket>()

    fun send() {
//        println("$vertexChanged $textureChanged $colorChanged $elementChanged")

        send(vertexBufferB, numVertex * 3, vertexChanged)
        send(colorBufferB, numVertex * 4, colorChanged)
        send(textureBufferB, numVertex * 2, textureChanged)
        send(elementBufferB, numElement * element_dim, elementChanged)

        vertexChanged = false
        colorChanged = false
        textureChanged = false
        elementChanged = false

        seenConnections.clear()
        seenConnections.addAll(websocket.openWebsockets)

    }


    private fun send(v: ByteBuffer, mx: Int, realUpdate: Boolean) {

        websocket.openWebsockets.forEach { x ->
            try {
//                (x as WebSocketImpl).socket.tcpNoDelay = true
            } catch (e: SocketException) {
                e.printStackTrace()
            }
        }


        v.position(0)
        v.limit(mx * 4 + 2 * 64)
        v.order(ByteOrder.BIG_ENDIAN)

        //!!
        // we've lost a lot of efficiency here with the .array()!!!
        //!!


        if (realUpdate)
            websocket.openWebsockets.forEach {
                it.send(v.array())
            }
        else {
            websocket.openWebsockets.forEach {
                if (!seenConnections.contains(it))
                    it.send(v.array())
            }
        }

        v.order(ByteOrder.LITTLE_ENDIAN)
        v.clear()
    }


}