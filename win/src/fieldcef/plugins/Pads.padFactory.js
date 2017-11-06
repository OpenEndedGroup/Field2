var color = "#a22"

_._isPad=true

var SimpleCanvas = Java.type('field.graphics.util.onsheetui.SimpleCanvas')
var PadGroup = Java.type("fieldbox.boxes.plugins.PadGroup")

// set this box to be a SimpleCanvas. This lets us draw things using 'circle' etc.
_.setClass(SimpleCanvas.class)

// constrain this box to lie on the edges of its parent
var pg = new PadGroup("pd", _.tapBinding.inside)
pg.add(_)

// fix the dimensions of this box
var W = 28

_.frame.w = W
_.frame.h = W

_.lockWidth = true
_.lockHeight = true


// how to draw the contents of this box
_.clear()

var circle = _.circle(W/2,W/2, W/4-0.5)
circle.attr({fill:color, "fill-opacity":1})
circle.attr({stroke:color, "stroke-opacity":0.5, "stroke-width":1})

var circle2 = _.circle(W/2,W/2, W/2-0.5)
circle2.attr({stroke:color, "stroke-opacity":0.25, "stroke-width":1})

// don't decorate it with other things
_.shyConnections=true
_.noFrame = true

// how to draw the contents of the box inside the text editor. We'll use the same color
var id = _.tapBinding.canvasId
_.tapBinding.execute(`
	var c = canvasForID("${id}")
	if(c)
	{
		var circle = c.circle(14/2,14/2, 14/2-2.5)
		circle.attr({fill:"${color}", "fill-opacity":1})
		circle.attr({stroke:"${color}", "stroke-opacity":0.5, "stroke-width":1})
	}
	else
	{
		console.log(" can't find tap canvas")
	}
`)

