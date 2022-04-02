
_r = () => {
	var s = _.analysis.getRawSamples(_.time.frame.x/24)

	var f = new FLine()

	// for every entry in 's' add it to 'f'
	for(var i=0;i<s.length;i++)
	{
		// scale it so that it fits into the 100x100 canvas 
		f.lineTo(i/20, 50-s[i]*50)	
	}

	f.color = vec(1,1,1,1)

	_.stage.lines.f = f

}

