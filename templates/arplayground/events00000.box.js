
clearAFrame()
var box = aCreate("box", "a-box")

box.setAttribute("color", "red")
box.setAttribute("opacity", 0.5)


mouseMove.down = (e) => {

	middle = e.ray.origin + e.ray.direction*0.2

	box.position = middle
	box.scale = vec(1,1,1)*0.01
}


