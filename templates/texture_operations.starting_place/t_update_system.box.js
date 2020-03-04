
_.scene[-20].update_operators = () => {

	var s = __.children.stream().filter( b => b.textureOperator && b.here.textureOperator)

	s.forEach( a => {
		try{
			if (a.fbo && a.fbo.draw)
				a.fbo.draw()
		}
		catch(e)
		{
			System.out.println(e)
		}
	})
	return true
}



