
// let any box talk about this box by saying _.play
__.replay = _

var timeStart = System.currentTimeMillis()	
var pixelsToSeconds = 24

// if we have previously started from somewhere, go back there
// otherwise, note this as the new start
if (_.lastPlayAt)
	_.time.frame.x = _.lastPlayAt
else
	_.lastPlayAt = _.time.frame.x

// install replay shortcut 
_.shortcut.ctrl_shift_p = () => {
	if (_.play)
		_.play.end()
	_.begin()
}


while(true) {

	var timeNow = System.currentTimeMillis()
	var timeDifference = (timeNow-timeStart)/1000
	var delta = timeDifference*pixelsToSeconds
	
	_.time.frame.x += delta
	_.redraw()
	_.wait()
	timeStart = timeNow
}

_.boxBackground = vec(0.23, 0.5, 0.8, 0.6)
_.windowSpace = vec(0,0)
