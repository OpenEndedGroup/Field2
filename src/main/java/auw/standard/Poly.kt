package auw.standard

import auw.*
import field.utility.Dict
import org.jetbrains.kotlin.konan.file.use
import kotlin.math.max

class Poly {

    var maxPol = 5

    var state = false
    var notes = 0

    // need way of saying that this sample is 'over' and we should kill the channel
    // need note stealing (with fade out)
    // this is a little like Oscs

    inner class Ongoing(val f: () -> _FBuffer, val name: String, val offset: Int, val overspill: FloatArray) {
        var over = false
        var overcount = 0
        var age = 0
    }

    val ongoing = mutableMapOf<Int, Ongoing>()

    // there are probably other ways of triggering this
    fun apply(edge: _FBuffer, from: () -> _FBuffer): _FBuffer {

        return FBufferSource(this)
        {

            val output = BoxTools.stack.get().allocate()

            val e = edge.get()

            for (read in 0 until IO.vectorSize) {
                if (e.a[read] > 0.0 && !state) {

                    println(" triggered ${e.a[read]} $state / $read")
                    // trigger here
                    // we need to open a new box context for this evaluation

                    notes++
                    DynamicScope.push("note$notes").use {
                        val o = Ongoing(from, "note$notes", read, FloatArray(read))
                        ongoing.put(notes, o)
                    }

                    state = true

                } else if (e.a[read] <= 0.0) {
//                    println(" released ${e.a[read]} / $read")
                    state = false
                }
            }

            if (ongoing.size > maxPol) {
                print(" killing ")

                ongoing.values.sortedBy { -it.age }.take(1).forEach {
                    println(" killing note that has ${it.age}")

                    it.over = true
                }
            }

            ongoing.values.removeIf { ongoing ->
                DynamicScope.push(ongoing.name).use {
                    val next = ongoing.f().get()

                    if (next.info.isTrue(Dict.Prop<Number>("over"), false))
                        ongoing.over = true

                    if (ongoing.over) {

                        for (i in 0 until ongoing.offset) {
                            output.a[i] += ongoing.overspill[i] * f(++ongoing.overcount)
                        }
                        for (i in ongoing.offset until IO.vectorSize) {
                            output.a[i] += next.a[i - ongoing.offset] * f(++ongoing.overcount)
                        }
                        for (i in 0 until ongoing.offset) {
                            ongoing.overspill[i] = next.a[IO.vectorSize - ongoing.offset + i]
                        }

                    } else {

                        for (i in 0 until ongoing.offset) {
                            output.a[i] += ongoing.overspill[i]
                        }
                        for (i in ongoing.offset until IO.vectorSize) {
                            output.a[i] += next.a[i - ongoing.offset]
                        }
                        for (i in 0 until ongoing.offset) {
                            ongoing.overspill[i] = next.a[IO.vectorSize - ongoing.offset + i]
                        }
                    }

                    ongoing.age++

                    val k = ongoing.over && ongoing.overcount >= IO.vectorSize * 1

                    if (k) {
                        // DELETE SCOPE ?
                        DynamicScope.delete(ongoing.name)
                    }

                    k
                }
            }

            output

        }

    }

    private fun f(x: Int): Float {
        return max(0f, 1 - x / (IO.vectorSize.toFloat() * 1))
    }
}