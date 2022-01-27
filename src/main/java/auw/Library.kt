package auw

import auw.standard.*
import fieldbox.boxes.Box
import fieldbox.boxes.plugins.BoxDefaultCode
import fieldbox.boxes.plugins.Missing
import fieldnashorn.Nashorn
import java.util.function.BiConsumer

class Library {
    val standard = mutableMapOf<String, Class<*>>()

    var _stdlib = BoxDefaultCode.findSource(this.javaClass, "stdlib")

    init {

        standard["Sin"] = Sin::class.java

        standard["Saw"] = Saw::class.java
        standard["Sqr"] = Sqr::class.java
        standard["Tri"] = Tri::class.java
        standard["Pulse"] = Pulse::class.java

        standard["Play"] = Play::class.java
        standard["Line"] = Line::class.java
        standard["Line0"] = Line0::class.java
        standard["FilterPeak"] = FilterPeak::class.java
        standard["FilterNotch"] = FilterNotch::class.java
        standard["FilterLow"] = FilterLow::class.java
        standard["FilterHigh"] = FilterHigh::class.java
        standard["FilterBand"] = FilterBand::class.java
        standard["FilterAll"] = FilterAll::class.java

        standard["AutoGain"] = AutoGain::class.java
        standard["Delay"] = Delay::class.java
        standard["Distort"] = Distort::class.java

        standard["Amplitude"] = Amplitude::class.java

        standard["DoubleFade"] = DoubleFade::class.java
        standard["Convolve"] = Convolve::class.java
        standard["Correlate"] = Convolve::class.java

        standard["Microphone"] = Microphone::class.java
        standard["MicrophoneLeft"] = MicrophoneLeft::class.java
        standard["MicrophoneRight"] = Microphone::class.java

        standard["TimeDelay"] = TimeDelay::class.java

        standard["Peak"] = Peak::class.java
        standard["PeakAt"] = PeakAt::class.java

        standard["GrainulatorDelay"] = GrainulatorDelay::class.java
        standard["Waveshaper"] = Waveshaper::class.java

        standard["Poly"] = Poly::class.java

    }

    fun isStandard(name: String): Class<*>? = standard.get(name)


}
