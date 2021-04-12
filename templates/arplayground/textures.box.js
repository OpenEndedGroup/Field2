
clearAFrame()

var assets = aCreate("assets", "a-assets")
assets.innerHTML = `
	<img id='myimage' src='masthead.jpg'>
`

for(var i=0;i<30;i++)
{
	var plane = aCreate("plane"+i, "a-plane")

	plane.setAttribute("color", "white")
	plane.position = vec(Math.random()*2-1,Math.random()*2-1,-3+i/40)*2
	plane.setAttribute("width", 3)
	plane.setAttribute("height", 2)
	plane.setAttribute("opacity", 0.3)

	
	plane.setAttribute("src", "#myimage")
}
