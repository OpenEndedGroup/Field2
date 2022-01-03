@file:JvmName("FLineSerializationHelper")

package field.graphics

import field.linalg.Vec3
import java.awt.BasicStroke
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable

class FLineSerializationHelper {
    fun writeObject(f: FLine, oos: ObjectOutputStream) {
        val i = ArrayList<Vec3>()

        var instructions = "";
        for (nn in f.nodes) {
            when (nn) {
                is FLine.CubicTo -> {
                    instructions += "c"
                    i += nn.c1
                    i += nn.c2
                    i += nn.to
                }
                is FLine.LineTo -> {
                    instructions += "l"
                    i += nn.to
                }
                is FLine.MoveTo -> {
                    instructions += "m"
                    i += nn.to
                }
            }
        }

        oos.writeObject(instructions to i)

        val props = LinkedHashMap<String, Serializable>();

        f.attributes.map.forEach { k, v ->
            if (v is Serializable) {
                props.put(k.name, v);
            }
            if (k.name.equals("thicken"))
            {
                props.put(k.name, (v as BasicStroke).lineWidth)
            }
        }

        oos.writeObject(props);

    }

    fun readObject(f: FLine, ois: ObjectInputStream) {
        val p = ois.readObject()

        if (p is Pair<*, *>) {
            f.data("" + p.first, p.second)
        }

        val props = ois.readObject() as Map<String, Serializable>

        props.forEach { k, v ->
            f.asMap_set(k, v)
        }

    }
}
