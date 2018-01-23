
__.shortcut.ctrl_p = () => {
	if (_.replay)
		_.replay.end()
	if (_.play)
		_.play.begin()
	else
		__.named.play[0].begin()
}

__.shortcut.ctrl_shift_p = () => {
	if (_.play)
		_.play.end()
	if (_.replay)
		_.begin()
	else
		__.named.replay[0].begin()
}

_.auto=1

_.boxBackground = vec(0.8, 0.5, 0.23, 0.2)
_.windowSpace = vec(0,0)
