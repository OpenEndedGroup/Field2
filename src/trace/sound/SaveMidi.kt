package trace.sound

import field.utility.Documentation
import fieldbox.boxes.TimeSlider.velocity
import java.io.File
import javax.sound.midi.*


class SaveMidi {
    private var sequence: Sequence
    private var track: Track

    init {
        // 16 ticks per quarter note.
        sequence = Sequence(Sequence.PPQ, 16) // 16 ticks equals a quarter note = 0.5s
        track = sequence.createTrack() // Begin with a new track
    }

    @Documentation("`note(time, pitch, velocity, duration)` adds a note to this file. Times are in seconds, pitch is 1-127, velocity is 0 to 1.")
    fun note(time: Double, pitch: Int, velocity: Double, duration: Double) {
        val on = ShortMessage()
        on.setMessage(ShortMessage.NOTE_ON, 0, Math.max(0, Math.min(127, pitch)), Math.max(0, Math.min(127, (127 * velocity).toInt())))
        val off = ShortMessage()
        off.setMessage(ShortMessage.NOTE_OFF, 0, Math.max(0, Math.min(127, pitch)), 0)
        track.add(MidiEvent(on, secondsToTicks(time)))
        track.add(MidiEvent(off, secondsToTicks(time + duration)))
    }

    @Documentation("`note(time, pitch, velocity, duration, channel)` adds a note to this file. Times are in seconds, pitch is 1-127, velocity is 0 to 1. channel from 0-15")
    fun note(time: Double, pitch: Int, velocity: Double, duration: Double, channel: Int) {
        val on = ShortMessage()
        on.setMessage(ShortMessage.NOTE_ON, channel, Math.max(0, Math.min(127, pitch)), Math.max(0, Math.min(127, (127 * velocity).toInt())))
        val off = ShortMessage()
        off.setMessage(ShortMessage.NOTE_OFF, channel, Math.max(0, Math.min(127, pitch)), 0)
        track.add(MidiEvent(on, secondsToTicks(time)))
        track.add(MidiEvent(off, secondsToTicks(time + duration)))
    }

    private fun secondsToTicks(time: Double): Long {
        return (time*32+Math.random()*0.5).toLong();// dither
    }

    @Documentation("`save(filename)` actually saves this to disk")
    fun save(fn: String) {
        val allowedTypes = MidiSystem.getMidiFileTypes(sequence);
        MidiSystem.write(sequence, allowedTypes[0], File(fn));
    }

}