
clearAFrame()

var box = aCreate("box", "a-box")

box.setAttribute("color", "red")
box.setAttribute("opacity", 0.85)

var t = 0
_r = () => {
	
	box.position = vec(-1,1, -2+0*Math.random())
	box.rotation = vec(t++,t/9,0)
	box.scale = vec(0.1, 0.1, 0.1)
	
}

