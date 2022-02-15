


// here's the filename we're using

var filename = "/Users/marc/Desktop/Pockets_w_excerpt.wav"

// -----------------------

var Sound = Java.type("trace.sound.Sound")
var SoundAnalysis = Java.type("trace.sound.SoundAnalysis")

var s = new Sound()
s.init(10)
buffer = s.makeBufferFromFile(filename)

var source = s.allocate()

// export these things
_.source = source
_.buffer = buffer
_.sound = s

// make this box a funny color
_.boxBackground = vec(0.23, 0.5, 0.8, 0.6)

// stick this box to the screen, not the canvas
_.windowSpace = vec(0,0)

// automatically run this box on load
_.auto = 1

__.analysis = new SoundAnalysis(filename)

