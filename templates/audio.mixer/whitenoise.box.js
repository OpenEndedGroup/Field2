



_.audio =  (sc, power) => {

	$.initialize.p = 0

	let output = $.newBuffer()
	
	var c = output.length
		
	for(var i=0;i<c;i++)
	{
		output[i] = Math.pow(Math.sin($.p+=0.1), power)
	}

	return AutoGain(output*sc)
}


_r = () => {
	
	$.output = _.audio(0.2, 1.0)
	
}