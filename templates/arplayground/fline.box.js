
clearAFrame()
var f = new FLine()

for(var i=0;i<1000;i++)
{
	f.lineTo(Math.random(), Math.random()+1, -1+ Math.random())
}

f.color = vec(0,0,0,0.2)
f.pointed=true
f.stroked=true
stage.f = f