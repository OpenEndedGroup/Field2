package fielded.live

import com.github.gumtreediff.gen.Generators
import com.github.gumtreediff.gen.TreeGenerator
import com.github.gumtreediff.gen.js.RhinoTreeGenerator
import com.github.gumtreediff.matchers.Matchers

class MappingGenerator {
    fun gum(a: String, b: String): MutableMap<Pair<Int, Int>, Pair<Int, Int>> {
        val t1 = RhinoTreeGenerator().generateFromString(a).root;
        val t2 = RhinoTreeGenerator().generateFromString(b).root;

        val m = Matchers.getInstance().getMatcher(t1, t2)
        m.match()


        val mout = mutableMapOf<Pair<Int, Int>, Pair<Int, Int>>()

        m.mappings.asSet().forEach {

            if (it.first.pos!=-1 && it.second.pos!=-1) {
                println("mapping: ${a.substring(it.first.pos until it.first.endPos)} -> ${b.substring(it.second.pos until it.second.endPos)}")
                mout.put(Pair(it.first.pos, it.first.endPos), Pair(it.second.pos, it.second.endPos))
            }
        }

        return mout;
    }
}