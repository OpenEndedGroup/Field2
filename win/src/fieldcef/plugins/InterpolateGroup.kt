package fieldcef.plugins

import field.utility.Dict
import fieldbox.boxes.Box

class InterpolateGroup
{


    // we'll need to cache quite a bit of this to make it run fast
    fun interpolate(time: Double, children: Collection<Box>) : Double?
    {
        if (children.isEmpty()) return null;

        val search = children.sortedBy { it.properties.get(Box.frame).x }
        if (children.size==1) return valueOf(search[0])

        val index = search.binarySearchBy(time, 0, search.size, { tOf(it) })

        if (index==0)
        {
            return valueOf(search[0])
        }
        else if (index==search.size)
        {
            return valueOf(search[search.size-1])
        }
        else if (index>0)
        {
            return valueOf(search[index])
        }
        else
        {
            val left = -index-1
            val right = -index

            return _interpolate((time-tOf(search[left]))/(tOf(search[right])-tOf(search[left])), valueOf(search[left]), valueOf(search[right]))

        }

    }

    private fun _interpolate(alpha: Double, left: Double, right: Double): Double {

        return left*(1-alpha)+alpha*right;
    }

    val value = Dict.Prop<Number>("value")

    private fun valueOf(it: Box): Double {
        return it.properties.getOr(value, {0f}).toDouble()
    }

    private fun tOf(it: Box): Double {
        return it.properties.get(Box.frame).x.toDouble()
    }
}