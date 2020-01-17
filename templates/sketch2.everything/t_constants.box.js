
__.t_constants = _
_.width = 1920
_.height = 1080

_.currentUnit = 0

_.nextUnit = () => {
	return _.t_constants.currentUnit++
}

_.updateAllFBO = () => {
	for(var a of __.children.stream().filter( b => b.textureOperator && b.here.textureOperator).toArray())
	{
		if (a.fbo && a.fbo.draw)
		{
			a.fbo.draw()
		}
	}
}

