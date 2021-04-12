
clearAFrame()

var box = aCreate("box", "a-box")

box.setAttribute("color", "red")
box.setAttribute("opacity", 0.5)

var box2 = aCreate("box2", "a-box", box)

box2.setAttribute("color", "green")
box2.setAttribute("opacity", 1)

var box3 = aCreate("box3", "a-box", box2)

box3.setAttribute("color", "green")
box3.setAttribute("opacity", 1)

var t = 0
_r = () => {
	
	box.position = vec(-5,0, 0*Math.random())
	box2.position = vec(0,2,0)
	box3.position = vec(1,2,0)
	box.rotation = vec(t++,t/9,0)
	box2.rotation = vec(t/3,t/9,t*4)
	
}

