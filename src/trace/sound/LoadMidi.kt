package trace.sound

import javax.sound.midi.ShortMessage.NOTE_OFF
import javax.sound.midi.ShortMessage.NOTE_ON
import javax.sound.midi.ShortMessage
import java.io.File
import javax.sound.midi.MidiSystem

class LoadMidi(val fn: String) {

    data class Note(var pitch: Int, var velocity: Double, var time: Double, var duration: Double)

    val notes = mutableListOf<Note>()

    init {

        val sequence = MidiSystem.getSequence(File(fn))

        val ttms = ((sequence.microsecondLength / sequence.tickLength) / 1000.0)/1000.0
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
    }

    fun notesAtTime(t: Double): List<Note> {
        return notes.filter { it.time <= t && (it.time + it.duration) >= t }
    }

    fun notesBetweenTimes(tStart: Double, tEnd: Double): List<Note> {
        return notes.filter { it.time <= tEnd && (it.time + it.duration) >= tStart }
    }

    var time = 0.0
    fun read(t: Double): List<Note> {
        if (t < time) {
            time = t
        }
        val ret = notesBetweenTimes(time, t)
        time = t
        return ret
    }

}