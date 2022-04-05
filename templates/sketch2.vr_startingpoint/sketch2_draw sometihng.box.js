

// let's make a layer for our drawing to go into
var layer = _.stage.withName("ourAwesomeLayer")

// make sure that it's a VR layer in real meters 
layer.vrDefaults()

// let's make a geometry container
var f = new FLine()


// we'll do this forever!
// or until we stop the box
while(true)
{
	// where is the hand?
	var handAt = layer.vrRightHandPosition()
	
	// how 'down' is the trigger?
	var trigger = layer.vrRightHandButtons().axis1_right_x

	// if it's down at all
	if (trigger>0)
	{
		// add a line segment to where the hand is
		f.lineTo(handAt)
		
		// make sure it's white
		f.color = vec(1,1,1,1)
		
		// add it to the set of lines that this layer has in it
		// overwriting anything previously called 'ourLine'
		layer.lines.ourLine = f
	}
	else {
		// if it isn't down, make an empty line
		f = new FLine()
	}

	// wait here for an animation frame to head on out
	// as photons to your eyes.
	_.stage.frame()
}
