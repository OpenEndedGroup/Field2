
clearAFrame()

var assets = aCreate("assets", "a-assets")
assets.innerHTML = `
	<video id='myimage' muted autoplay loop src='source.mp4'>
`

for(var i=0;i<30;i++)
{
	var plane = aCreate("plane"+i, "a-video")

	plane.setAttribute("color", "white")
	plane.position = vec(Math.random()*8-4,Math.random()*8-4,-3+i/40)*2
	plane.setAttribute("width", 3)
	plane.setAttribute("height", 2)
	plane.setAttribute("opacity", 0.7)

	
	plane.setAttribute("src", "#myimage")
}

document.querySelector("video").play()
document.querySelector("video").pause()
