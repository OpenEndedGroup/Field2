
clearAFrame()

var tick = 0

mouseMove.down = (e) => {

	var box = aCreate("box"+(tick++), "a-box")

	box.setAttribute("color", "red")
	box.setAttribute("opacity", 0.5)
	
	middle = e.ray.origin + e.ray.direction*8

	box.position = middle
}


