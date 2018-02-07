var Sound = Java.type("trace.sound.Sound")

var s = new Sound()
s.init(10)
buffer = s.makeBufferFromFile("/Users/marc/Desktop/Portal2-OST-Volume1/Music/Portal2-19-Turret_Wife_Serenade.wav")

var source = s.allocate()

_.source = source
_.buffer = buffer
_.sound = s

_.boxBackground = vec(0.23, 0.5, 0.8, 0.6)
_.windowSpace = vec(0,0)
