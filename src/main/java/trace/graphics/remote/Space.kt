package trace.graphics.remote

import field.linalg.Vec3
import java.io.File

class Space(val r: RemoteServer) {

    inner class Layer(val name: String) {

        fun play(time: Double) {
            r.execute("channelMap['$name'].LaudioElement.currentTime=$time; channelMap['$name'].LaudioElement.play()", requiresSandbox=false)
        }

        fun play() {
            r.execute(" channelMap['$name'].LaudioElement.play()", requiresSandbox=false)
        }

        fun pause() {
            r.execute(" channelMap['$name'].LaudioElement.pause()", requiresSandbox=false)
        }

        fun position(x: Double, y: Double, z: Double) {
            position(Vec3(x, y, z))
        }

        fun position(v: Vec3) {
            r.execute("channelMap['$name'].Lsource.setPosition(${v.x},${-v.y},${v.z})", requiresSandbox=false)
        }

        fun volume(v: Double) {
            r.execute("channelMap['$name'].LaudioElement.volume=$v", requiresSandbox=false)
        }

        fun loop(b: Boolean) {
            r.execute("channelMap['$name'].LaudioElement.loop=$b", requiresSandbox=false)
        }

    }

    /*
    n.send("songbird.setListenerPosition(0,0,0)")
n.send("songbird.setListenerOrientation(0,1,0, 0,0,1)")


_.orientation = (origin, forward, up) => {
_.send(`songbird.setListenerPosition(${origin.x}, ${origin.y}, ${origin.z}); songbird.setListenerOrientation(${forward.x},${forward.y},${forward.z},${up.x},${up.y},${up.z})`)
}


_.source = (num, angle, radius, d, h) => {

	var x0 = Math.sin(Math.PI*(angle-d)/180)*radius
	var y0 = h
	var z0 = Math.cos(Math.PI*(angle-d)/180)*radius
	var x1 = Math.sin(Math.PI*(angle+d)/180)*radius
	var y1 = h
	var z1 = Math.cos(Math.PI*(angle+d)/180)*radius

	var lab = angle.toFixed(1)+" @ "+radius.toFixed(1)+" ("+d+") "+h.toFixed(1)
	_.send(`channels[${num}].Rsource.setPosition(${x0},${y0},${z0});channels[${num}].Lsource.setPosition(${x1},${y1},${z1});label(${num}, "${lab}")`)

}

_.sourceLevel = (num, level) => {
_.send(`channels[${num}].RaudioElement.volume=${level};channels[${num}].LaudioElement.volume=${level};`)
}
     */

    fun setRoomDimensions(width: Double, height: Double, depth: Double) {
        r.execute("var dimensions = {\n" +
                "    width: $width,\n" +
                "    height: $height,\n" +
                "    depth: $depth\n" +
                "};\nsongbird.setRoomProperties(dimensions, materials)", requiresSandbox=false)
    }

    fun setRoomMaterial(mat: String) {
        r.execute("var material = '$mat'\n" +
                "var material2 = '$mat'\n" +
                "\n" +
                "var materials = {\n" +
                "    left: material,\n" +
                "    right: material,\n" +
                "    front: material,\n" +
                "    back: material2,\n" +
                "    down: material,\n" +
                "    up: material\n" +
                "};\n" + "songbird.setRoomProperties(dimensions, materials)", requiresSandbox=false)
    }

    fun withFile(s: String): Layer {
        if (!File(s).exists()) return throw IllegalArgumentException(" can't find sound file $s")

        val url = r.declareResource(s)

        r.execute("var _s = makeSource('$s', '$url')", requiresSandbox=false)

        return Layer(s)
    }


    fun playSoASet(prefix: String) {

        var urlList = "["
        for (i in 1 until 10) {
            urlList += "'" + r.declareResource(prefix + "_$i.wav") + "', "
        }
        urlList += "]"

        r.execute("Omnitone.createBufferList(audioContext, $urlList).then(function(v){ " +
                "contentBuffer = Omnitone.mergeBufferListByChannel(audioContext, v)\n " +
                "inputGain = audioContext.createGain()\n" +
                "currentBufferSource = audioContext.createBufferSource();\n" +
                "currentBufferSource.loop = true\n" +
                "currentBufferSource.buffer = contentBuffer\n" +
                "currentBufferSource.connect(inputGain)\n" +
                "inputGain.connect(songbird.ambisonicInput)\n" +
                "console.log(\'starting SoA source\')\n" +
                "currentBufferSource.start()\n" +
                "   })", requiresSandbox=false)


    }


}