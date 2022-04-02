
//$.clear

var t = 0
_r = () => {
	t++
	
	var p = Play("/Users/marc/blue.wav", 1.0 ) * 1.0
	
	p = FilterLow(p, (Math.sin(t/10)+1)*1+50000, 0.90) * 1.0

	p2 = FilterLow(p, 500, 0.90) * 1.0
	p3 = FilterLow(p, 5000, 0.90) * 1.0
	p4 = FilterBand(p, 1950, 0.99) * 1.0

	__.amp = Amplitude(p2)
	__.amp2 = Amplitude(p3) - __.amp
	__.amp3 = Amplitude(p4) - __.amp2
	
	$.output = p

	_.scope(p)
}
