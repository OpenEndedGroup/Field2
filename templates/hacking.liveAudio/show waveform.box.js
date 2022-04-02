
_r = () => {
	m = Microphone()
	var f = new FLine()
	for(var i=0;i<m.length;i++)
	{
		f.lineTo(i, -m[i]*1000)
	}
	_.lines.f = f
}