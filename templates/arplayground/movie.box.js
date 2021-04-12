
clearAFrame()

var assets = aCreate("assets", "a-assets")
assets.innerHTML = `
	<video id='myimage' muted autoplay loop src='source.mp4'>
`

var plane = aCreate("plane", "a-video")

plane.setAttribute("color", "white")
plane.position = vec(0,0,-1)
plane.setAttribute("width", 1)
plane.setAttribute("height", 0.5)
plane.setAttribute("opacity", 1)


plane.setAttribute("src", "#myimage")
