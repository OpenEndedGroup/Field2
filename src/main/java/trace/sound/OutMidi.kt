package trace.sound

import field.app.RunLoop
import field.utility.Documentation
import javax.sound.midi.MidiSystem
import javax.sound.midi.Receiver
import javax.sound.midi.ShortMessage

class OutMidi {
    private var receiver: Receiver?

    init {
        val dev = MidiSystem.getMidiDeviceInfo().find { it.javaClass.name.contains("MidiOutDevice") }
        if (dev != null) {
            val theDev = MidiSystem.getMidiDevice(dev)
            theDev.open()
            receiver = theDev.receiver
        } else throw IllegalArgumentException(" can't open output device ? Make sure IAC bus is on in Audio Midi setup")
    }

    constructor(contains: String) {

        val dev = MidiSystem.getMidiDeviceInfo().find {
            it.javaClass.name.contains("MidiOutDevice") && it.name.contains(contains)
        }
        if (dev != null) {
            val theDev = MidiSystem.getMidiDevice(dev)
            theDev.open()
            receiver = theDev.receiver
        } else throw IllegalArgumentException(" can't open output device ? Make sure IAC bus is on in Audio Midi setup")
    }

    val generationNumbers = mutableMapOf<Int, Int>()


    @Documentation("`note(pitch, velocity, duration, channel)` sends a note. Durations are in seconds, pitch is 1-127, velocity is 0 to 1. channel from 0-15")
    fun note(pitch: Int, velocity: Double, duration: Double, channel: Int) {
        val on = ShortMessage()
        on.setMessage(ShortMessage.NOTE_ON, channel, Math.max(0, Math.min(127, pitch)), Math.max(0, Math.min(127, (127 * velocity).toInt())))
        val off = ShortMessage()
        off.setMessage(ShortMessage.NOTE_OFF, channel, Math.max(0, Math.min(127, pitch)), 0)
        receiver!!.send(on, 0);

        generationNumbers.computeIfAbsent(pitch, { 0 })
        val generation = generationNumbers.compute(pitch, { k, v ->
            v!! + 1
        })

        RunLoop.main.delay({
                               if (generationNumbers.get(pitch) != generation) return@delay
                               receiver!!.send(off, 0);
                           }, (duration * 1000).toInt())
    }


    @Documentation("`noteOff(pitch, channel)` sends a note off. pitch is 1-127, channel from 0-15")
    fun noteOff(pitch: Int, channel: Int) {
        val off = ShortMessage()
        off.setMessage(ShortMessage.NOTE_OFF, channel, Math.max(0, Math.min(127, pitch)), 0)
        receiver!!.send(off, 0);
        off.setMessage(ShortMessage.NOTE_ON, channel, Math.max(0, Math.min(127, pitch)), 0)
        receiver!!.send(off, 0);

        generationNumbers.computeIfAbsent(pitch, { 0 })
        val generation = generationNumbers.compute(pitch, { k, v ->
            v!! + 1
        })

    }

    @Documentation("`control(controller, value, channel)` sends a MIDI control change")
    fun control(controller: Int, velocity: Double, channel: Int) {
        val on = ShortMessage()
        on.setMessage(ShortMessage.CONTROL_CHANGE, channel, Math.max(0, Math.min(127, controller)), Math.max(0, Math.min(127, (127 * velocity).toInt())))
        receiver!!.send(on, 0);
    }


}