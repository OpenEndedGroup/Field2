package field.graphics

import field.linalg.Vec3
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

class FLineSerializationHelper
{
    fun writeObject(f : FLine, oos : ObjectOutputStream)
    {
        val i = ArrayList<Vec3>()

        var instructions = "";
        for(nn in f.nodes)
        {
            when(nn)
            {
                is FLine.CubicTo -> {
                    instructions+="c"
                    i += nn.c1
                    i += nn.c2
                    i += nn.to
                }
                is FLine.LineTo -> {
                    instructions+="l"
                    i+= nn.to
                }
                is FLine.MoveTo-> {
                    instructions+="m"
                    i+= nn.to
                }
            }
        }

        oos.writeObject(instructions to i)
    }

    fun readObject(f : FLine, ois : ObjectInputStream)
    {
        val p = ois.readObject()

        if (p is Pair<*, *>)
        {
            f.data(""+p.first, p.second)
        }

    }
}
