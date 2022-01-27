
var Definitions = Java.type('auw.Definitions')
var DynamicScope = Java.type('auw.DynamicScope')

var Sin = Java.type('auw.standard.Sin')
var Sqr = Java.type('auw.standard.Sqr')
var Saw = Java.type('auw.standard.Saw')
var Tri = Java.type('auw.standard.Tri')
var Pulse = Java.type('auw.standard.Pulse')


var Play = Java.type('auw.standard.Play')
var Line = Java.type('auw.standard.Line')
var Line0 = Java.type('auw.standard.Line0')
var FilterLow = Java.type('auw.standard.FilterLow')
var FilterHigh = Java.type('auw.standard.FilterHigh')
var FilterNotch = Java.type('auw.standard.FilterNotch')
var FilterBand = Java.type('auw.standard.FilterBand')
var FilterPeak = Java.type('auw.standard.FilterPeak')
var FilterAll = Java.type('auw.standard.FilterAll')

var Grainulator =Java.type("auw.standard.Grainulator")
var AutoGain = Java.type('auw.standard.AutoGain')
var Delay = Java.type('auw.standard.Delay')
var Distort = Java.type('auw.standard.Distort')

var Amplitude = Java.type("auw.standard.Amplitude")

var Interpolate = Java.type("auw.FInterpolator")

var DoubleFade = Java.type("auw.standard.DoubleFade")
var Convolve = Java.type("auw.standard.Convolve")
var Correlate = Java.type("auw.standard.Correlate")
var TimeDelay = Java.type("auw.standard.TimeDelay")
var Microphone = Java.type("auw.standard.Microphone")
var MicrophoneLeft = Java.type("auw.standard.MicrophoneLeft")

var Poly= Java.type("auw.standard.Poly")


var Peak = Java.type("auw.standard.Peak")
var PeakAt = Java.type("auw.standard.PeakAt")

var GrainulatorDelay = Java.type("auw.standard.GrainulatorDelay")
var Waveshaper= Java.type("auw.standard.Waveshaper")

_.withFunctionRewriting=true

_.functionRewriteTrap = (box, name, hash) => {

    if (_.mixer.lib.isStandard(name))
    {
        return `DynamicScope.getOrConstructClass("${name}+${hash}", ${name}.class).apply`
    }
    else if (name==="Interpolate")
    {
        return `DynamicScope.getOrConstructClass("${name}+${hash}", Interpolate.class).next`
    }
    else {
        let bx = _._mixerRoot.findByName(name)
        if (bx) {
            box._boxBindings.setAttribute(name, bx, 200)
            return `DynamicScope.runInScope("${name}+${hash}", ${name})`
        }
    }
    return name
}

