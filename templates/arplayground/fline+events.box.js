
mouseMove.down = (e) => {

	middle = e.ray.origin + e.ray.direction
		
}

var down = false
mouseDown.isDown = (e) => {
	down = true
}

mouseUp.isDown = (e) => {
	down = false
}


f = new FLine()
_r = () => {

	if (!down) return 

	f.lineTo(middle)

	f.color = vec(0,0,0.0,0.5)
	
	stage.myLine = f
}
