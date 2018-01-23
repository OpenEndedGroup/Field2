
// let any box talk about this box by saying _.play
__.play = _

var timeStart = System.currentTimeMillis()	
var pixelsToSeconds = 24

// note where we started from
__.lastPlayAt = _.time.frame.x

__.shortcut.ctrl_p = () => {
	if (_.replay)
		_.replay.end()
	if (_.play)
		_.play.begin()
	else
		__.named.play[0].begin()
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
