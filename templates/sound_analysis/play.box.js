
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


var at = _.time.frame.x/24.0

if (at<0) at = 0

_.sound.stop(_.source)

var gg = _.sound.play(_.buffer, _.source, 1, 1, vec(0,0,0), vec(0,0,0), at*48000)

var DELAY = 0

_r = [ 
	() => {},
	() => {
		_.time.frame.x = gg.get()*24-DELAY
		_.redraw()
	},
	() => _.sound.stop(_.source)
	]


_.boxBackground = vec(0.23, 0.5, 0.8, 0.6)
_.windowSpace = vec(0,0)
