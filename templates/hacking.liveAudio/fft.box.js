var SimpleFFT = Java.type("trace.sound.SimpleFFT")

_r = () => {
	m = Microphone()
	freq = new SimpleFFT().process(m, 16)
	var f = new FLine()
	for(var i=0;i<freq.length/2;i++)
	{
		f.lineTo(i*10, -Math.log(freq[i])*10)
	}
	_.lines.f = f
}

