var Line = Java.type('auw.standard.Line')
var Osc = Java.type('auw.simple.Osc')

var t = 0

var osc
_r = () => {
	if (t++%100==0)
	{
		osc = new Osc()
		osc.frequency=880/2
		osc.decay = 0.95
	}
	osc.square = Math.sin(t/200)
}

