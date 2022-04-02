
var analysis = _.analysis

var f = new FLine()

analysis.listNames()

for(var i of Anim.lineRange(0, analysis.duration,100000))
{
    // get the analysis scaled form 0 to 1
	var value = analysis.getNormalized("spectral_energyband_middle_high", i)	


//	analysis.listNames()
    // plot it, converting seconds to pixels so that it lines up with the red line
	f.lineTo(i*24, 100-10*Math.log(value))
}

_.lines.clear()
_.lines.f = f