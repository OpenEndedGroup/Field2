

_.stage.background.a = 0.2
while(_.stage.frame())
{
	var f = new FLine()

	var X = _.amp * _.amp
	var Y = _.amp2 * 3
	
	for(var i=0;i<100;i++)
	{
		f.lineTo( 50 + (Math.random()-0.5) * X,  50 + (Math.random()-0.5) * Y )
	}

	f = f * rotate(_.amp3*10.0, 50, 50)
	
	_.stage.background.a = _.amp3/220
	
	f.color = vec(1,1,1,1)
	_.stage.lines.f = f
}

