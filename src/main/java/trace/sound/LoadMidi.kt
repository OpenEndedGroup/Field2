package trace.sound

import field.linalg.Vec2
import java.io.File
import javax.sound.midi.MidiSystem
import javax.sound.midi.ShortMessage
import javax.sound.midi.ShortMessage.NOTE_OFF
import javax.sound.midi.ShortMessage.NOTE_ON

class LoadMidi(var fn: String) {

    data class Note(var pitch: Int, var velocity: Double, var time: Double, var duration: Double) {
        var previous: Note? = null
        var next: Note? = null
    }

    val notes = mutableListOf<Note>()

    fun copy(): LoadMidi {
        val r = LoadMidi("")
        r.notes.addAll(this.notes)
        return r
    }

    init {

        if (fn != "") {
            val sequence = MidiSystem.getSequence(File(fn))

            val ttms = ((sequence.microsecondLength / sequence.tickLength) / 1000.0) / 1000.0
            var trackNumber = 0
            for (track in sequence.getTracks()) {
                trackNumber++
                println("Track " + trackNumber + ": size = " + track.size())
                println()

                val table = arrayOfNulls<Note>(127);

                for (i in 0 until track.size()) {
                    val event = track.get(i)
                    print("@" + event.getTick() * ttms + " ")
                    val message = event.getMessage()
                    if (message is ShortMessage) {
                        val sm = message as ShortMessage
                        print("Channel: " + sm.channel + " ")
                        if (sm.command == NOTE_ON) {
                            val key = sm.data1
                            val octave = key / 12 - 1
                            val note = key % 12
                            val velocity = sm.data2
                            if (velocity > 0) {
                                println("Note on, $note$octave key=$key velocity: $velocity")
                                if (table[key] == null) {
                                    table[key] = Note(key, velocity / 127.0, event.tick * ttms, 0.0);
                                }
                            } else {
                                if (table[key] != null) {
                                    table[key]!!.duration = event.tick * ttms - table[key]!!.time
                                    notes.add(table[key]!!)
                                    table[key] = null
                                }
                            }

                        } else if (sm.command == NOTE_OFF) {
                            val key = sm.data1
                            val octave = key / 12 - 1
                            val note = key % 12
                            val velocity = sm.data2
                            println("Note off, $note$octave key=$key velocity: $velocity")
                            if (table[key] != null) {
                                table[key]!!.duration = event.tick * ttms - table[key]!!.time
                                notes.add(table[key]!!)
                                table[key] = null
                            }
                        } else {
                            println("Command:" + sm.command)
                        }
                    } else {
                        println("Other message: " + message.javaClass)
                    }
                }

                println()
            }

            notes.sortBy { it.time }
            notes.forEachIndexed { i, n ->
                if (i > 0)
                    n.previous = notes[i - 1]
                if (i < notes.size - 1)
                    n.next = notes[i + 1]
            }
        }
    }

    fun firstNoteAfter(t: Double): Note? {
        return notes.filter { it.time > t }.firstOrNull()
    }

    fun notesAtTime(t: Double): List<Note> {
        return notes.filter { it.time <= t && (it.time + it.duration) >= t }
    }

    fun notesBetweenTimes(tStart: Double, tEnd: Double): List<Note> {
        return notes.filter { it.time <= tEnd && it.time >= tStart }
    }

    fun noteOffsBetweenTimes(tStart: Double, tEnd: Double): List<Note> {
        return notes.filter { (it.time + it.duration) >= tStart && (it.time + it.duration) < tEnd }
    }

    fun notes(): List<Vec2> {
        return notes.map {
            Vec2(it.time, it.time + it.duration)
        }
    }
    
    fun notes2(): List<Vec2> {
        return notes.map {
            Vec2(it.time, it.time + it.duration)
        }
    }

    var time = 0.0
    var previouslyReturned = mutableSetOf<Note>()

    fun read(t: Double): List<Note> {
        if (t < time) {
            time = t
            previouslyReturned.clear()
        }
        var ret = notesBetweenTimes(time, t)

        ret = ret.filter { !previouslyReturned.contains(it) }
        previouslyReturned.addAll(ret)

        time = t
        return ret
    }

    var timeOff = 0.0
    var previouslyReturnedOff = mutableSetOf<Note>()

    fun readOffs(t: Double): List<Note> {
        if (t < timeOff) {
            timeOff = t
            previouslyReturnedOff.clear()
        }
        var ret = noteOffsBetweenTimes(timeOff, t)

        ret = ret.filter { !previouslyReturnedOff.contains(it) }
        previouslyReturnedOff.addAll(ret)

        timeOff = t
        return ret
    }

}