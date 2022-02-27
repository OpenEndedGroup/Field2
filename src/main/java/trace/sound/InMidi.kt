package trace.sound

import field.app.RunLoop
import field.utility.IdempotencyMap
import javax.sound.midi.*

class InMidi(val nameHint: String?) {

    val transmitter: Transmitter?

    interface MidiFunction {
        fun apply(c : Int, a : Int, b : Double) : Boolean
    }

    val on = IdempotencyMap<MidiFunction>(MidiFunction::class.java)
    val off = IdempotencyMap<MidiFunction>(MidiFunction::class.java)
    val cc = IdempotencyMap<MidiFunction>(MidiFunction::class.java)

    init {
        if (nameHint != null) {
            val dev = MidiSystem.getMidiDeviceInfo()
                .find { it.javaClass.name.contains("MidiInDevice") && it.name.contains(nameHint) }
            if (dev != null) {
                val theDev = MidiSystem.getMidiDevice(dev)
                theDev.open()
                transmitter = theDev.transmitter!!
                transmitter.receiver = object : MidiDeviceReceiver {
                    override fun close() {
                    }

                    override fun send(message: MidiMessage?, timeStamp: Long) {

                        when (message) {
                            is ShortMessage -> {
                                val c = message.channel
                                val b1 = message.data1
                                val b2 = message.data2

                                when (message.command) {
                                    ShortMessage.NOTE_ON -> {
                                        println("\n\nnote on $c $b1 $b2")
                                        RunLoop.main.once {
                                            println(" service $this ${on.keys}")
                                            if (b2 > 0)
                                                _on(c, b1, b2)
                                            else
                                                _off(c, b1, b2)
                                        }
                                    }
                                    ShortMessage.NOTE_OFF -> {
                                        println("note off $c $b1 $b2")
                                        RunLoop.main.once {
                                            _off(c, b1, b2)
                                        }
                                    }
                                    ShortMessage.CONTROL_CHANGE -> {
                                        RunLoop.main.once {
                                            _cc(c, b1, b2)
                                        }
                                    }
                                }
                            }
                        }

                    }

                    override fun getMidiDevice(): MidiDevice = theDev

                }
            } else throw IllegalArgumentException(" can't open input device ? Make sure IAC bus is on in Audio Midi setup")
        } else {
            transmitter = null
        }
    }

    fun _on(channel: Int, pitch: Int, velocity: Int) {
        on.values.removeIf {
            println(" calling ... $it")
            !it.apply(channel, pitch, velocity / 127.0)
        }
    }

    fun _off(channel: Int, pitch: Int, velocity: Int) {
        off.values.removeIf {
            !it.apply(channel, pitch, velocity / 127.0)
        }

    }

    fun _cc(channel: Int, controller: Int, value: Int) {
        cc.values.removeIf {
            !it.apply(channel, controller, value / 127.0)
        }

    }

}