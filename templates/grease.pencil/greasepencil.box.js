
// we make sure that we save the 'pencil' property with any box that has one
_.ref.pencil.persistent=true

// and we'll show it if it's already set:
for (var b of __.children)
	if (b.pencil) b.lines.pencil = b.pencil


// here's an example of adding some interaction to the canvas
// onMouseDown we'll do the following:
__.onMouseDown.dragToDraw = (event) => {

	// if the box isn't selected, no nothing
	if (_.selection.size()!=1) return null;
	
	// if we're holding down the 'p' key 
	if (_.keysDown.contains('p'))
	{
		// don't do anything other than this
		event.properties.consumed=true
		
		// start drawing a new line
		if (_.selection[0].lines.pencil)
			target = _.selection[0].lines.pencil
		else
			target = new FLine()
		
		// this is the position, in drawing coordinates, of the mouse event
		x = event.after.mx 
		y = event.after.my 

		// start at this position
		target.moveTo(x,y)
		//target.thicken=3
		target.color=vec(1,0.8,0.3,0.5)

		// show this line and set _.pencil to be it
		_.selection[0].lines.pencil = target
		_.selection[0].pencil = target
		
		// mark it as notation
		target.notation = true
		
		// return a function that will be called while the mouse is dragged around		
		return (e2, end) => {
			x = e2.after.mx
			y = e2.after.my
			
			// extend the line
			target.lineTo(x,y)
			
			// repaint the screen
			_.redraw()
			
			// we'll keep going while the mouse is down
			return !end;
		}
		
	}
	
	return null;
}

__.onFrameChanged.updatePencil = (target, to) => {

	if (target.pencil)
	{
		target.lines.pencil = target.pencil = target.pencil.byTransforming( x => x+vec(to.x-target.frame.x, to.y-target.frame.y, 0))
	}
	return to
}



__.children.greasepencil.boxBackground=vec(0.3,0.7,1,0.2)
__.children.greasepencil.boxOutline=vec(0.3,0.7,1,-0.4)
_.auto=1
