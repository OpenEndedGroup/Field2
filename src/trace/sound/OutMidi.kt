package trace.sound

import field.app.RunLoop
import field.utility.Documentation
import javax.sound.midi.MidiSystem
import javax.sound.midi.Receiver
import javax.sound.midi.ShortMessage

class OutMidi {
    private var receiver: Receiver?

    init {
        /*
        ar MidiSystem = Java.type("javax.sound.midi.MidiSystem")
var ShortMessage = Java.type("javax.sound.midi.ShortMessage")


var ii = MidiSystem.getMidiDeviceInfo()
ii[3]
         */
        val dev = MidiSystem.getMidiDeviceInfo().find { it.javaClass.name.contains("MidiOutDevice") }
        if (dev != null) {
            val theDev = MidiSystem.getMidiDevice(dev)
            theDev.open()
            receiver = theDev.receiver
        } else
            throw IllegalArgumentException(" can't open output device ? Make sure IAC bus is on in Audio Midi setup")
    }

    @Documentation("`note(pitch, velocity, duration, channel)` sends a note. Durations are in seconds, pitch is 1-127, velocity is 0 to 1. channel from 0-15")
    fun note(pitch: Int, velocity: Double, duration: Double, channel: Int) {
        val on = ShortMessage()
        on.setMessage(ShortMessage.NOTE_ON, channel, Math.max(0, Math.min(127, pitch)), Math.max(0, Math.min(127, (127 * velocity).toInt())))
        val off = ShortMessage()
        off.setMessage(ShortMessage.NOTE_OFF, channel, Math.max(0, Math.min(127, pitch)), 0)
        receiver!!.send(on, 0);
        RunLoop.main.delay({
            receiver!!.send(off, 0);
        }, (duration * 1000).toInt())
    }

    @Documentation("`control(controller, value, channel)` sends a MIDI control change")
    fun control(controller: Int, velocity: Double, channel: Int) {
        val on = ShortMessage()
        on.setMessage(ShortMessage.CONTROL_CHANGE, channel, Math.max(0, Math.min(127, controller)), Math.max(0, Math.min(127, (127 * velocity).toInt())))
        receiver!!.send(on, 0);
    }


}