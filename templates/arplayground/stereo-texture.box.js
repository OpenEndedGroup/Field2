
clearAFrame()

var assets = aCreate("assets", "a-assets")
assets.innerHTML = `
	<img id='left' src='masthead.jpg'>
	<img id='right' src='masthead.jpg'>
`

var plane = aCreate("plane-left", "a-plane")

plane.setAttribute("color", "white")
plane.position = vec(0,1,-3)
plane.setAttribute("width", 3)
plane.setAttribute("height", 2)
plane.setAttribute("opacity", 1)
plane.setAttribute("src", "#left")
plane.setAttribute("stereo", "eye:left;")


var plane = aCreate("plane-right", "a-plane")

plane.setAttribute("color", "white")
plane.position = vec(0,1,-3)
plane.setAttribute("width", 3)
plane.setAttribute("height", 2)
plane.setAttribute("opacity", 1)
plane.setAttribute("src", "#right")
plane.setAttribute("stereo", "eye:right;")

