package auw.signal

import auw.*
import auw.BufferTools.zero
import java.lang.IllegalArgumentException
/*


what kind of code do we want to write for triggering samples?


// babel?

yield callOnTheNextBeat

q = yield sync name




 */
class Slice
{
    // need to mess with time somehow for this?
    var previous : FBuffer? = null
    var toGo = 0
    var firstOffset = 0

    var first = true

    fun apply(input : _FBuffer, offset : Int) : _FBuffer
    {
        if (first)
        {
            first = false
            toGo = offset
            firstOffset = offset
        }
        else
        {
            if (offset!=firstOffset)
            {
                throw IllegalArgumentException(" can't change offset after playback has started, $offset != $firstOffset")
            }
        }

        return FBufferSource(this) {

            if (toGo< IO.vectorSize)
            {
                if (previous == null)
                {
                    // time inside scope?
                    previous = input.get()
                    while(toGo<0) {
                        previous = input.get()
                        toGo += IO.vectorSize
                    }
                    var f = BoxTools.stack.get().allocate()
                    zero(f)
                    insert1(f, previous!!, toGo)
                    f
                }
                else
                {
                    var f = BoxTools.stack.get().allocate()
                    insert2(f, previous!!, toGo)
                    previous = input.get()
                    insert1(f, previous!!, toGo)
                    f
                }
            }
            else
            {
                toGo -= IO.vectorSize
                silence()
            }

        }
    }

    private fun insert1(dest: FBuffer, src: FBuffer, from: Int) {
        dest.a.position(from)
        src.a.limit(IO.vectorSize-from)
        dest.a.put(src.a)
        dest.a.clear()
        src.a.clear()
    }
    private fun insert2(dest: FBuffer, src: FBuffer, from: Int) {
        dest.a.position(0)
        src.a.position(from)
        dest.a.put(src.a)
        dest.a.clear()
        src.a.clear()
    }

    private fun silence(): FBuffer {
        var f = BoxTools.stack.get().allocate()
        zero(f)
        return f
    }
}