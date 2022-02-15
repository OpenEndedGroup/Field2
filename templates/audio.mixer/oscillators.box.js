var Line = Java.type('auw.standard.Line')

_r = () => {

	var f = 440
	
	var ll = Sin(1)
	var b = (Sin(f*2)*0.5 + Sin(f*3)*ll + Sin(f*4)*0.3 + Sin(f*5)*0.2 + Sin(f))
			 	
	$.output = AutoGain(b)
	
}
