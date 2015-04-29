
// -- tap code --
// this box is hooked into a widget in a text editor somewhere
// since can "call" this box (just like any other box) we 
// can make this box do something useful when it's called.
// here we store and print statistics about it's argument

var SummaryStatistics = Java.type('org.apache.commons.math3.stat.descriptive.SummaryStatistics')

_.ss = new SummaryStatistics()

// this is the code that's run when the "tap" is called
_.main.theTap = function()
{
	// add the value to SummaryStatistics
	_.ss.addValue(_.arg)
	// print the SummaryStatistics
	_.next.clear = function() _.output.o.clear()
	_.next.update = function() _.output.o.print(_.ss)
	// update the widget in the text editor (if it's visible)
	// with a count of the number of things we've seen
	if (_.tapBinding)
		_.tapBinding.execute(<<$)
			var canvas = canvasForID("${_.tapBinding.canvasId}")
			canvas.clear()
			// note use of string interpolation to get the number of items
			// in SummaryStatistics
			canvas.text(16,15, ${_.ss.getN()}).attr("font-size", 10)
		$

	// pass the argument through
	return _.arg*100
}
_.output.o.print("asdf")

// clear the widget on first execution
if (_.tapBinding)
	_.tapBinding.execute(<<$)
		var canvas = canvasForID("${_.tapBinding.canvasId}")
		canvas.clear()
	$

// -- custom drawing code ---
// this code makes the box look different from other boxes

var FLine = Java.type('field.graphics.FLine')
var Vec4 = Java.type('field.linalg.Vec4')
var FLine = Java.type('field.graphics.FLine')
var FLineDrawing = Java.type('fieldbox.boxes.FLineDrawing')

// set this box to be smaller than your usual box
_.frame.w = 40
_.frame.h = 40

// custom drawing for this box's frame
f = function(bx)
{
	f = new FLine()
	
	// we draw this in "box frame coordinates" 
	//(0,0) is the top left, (1,1) is the bottom right, 
	// no matter how large the box is
	f.roundedRect(0,0,1,1,0.4)
	f.filled=1
	
	// change appearence based on whether we are selected or not
	if (_.isSelected)
		f.fillColor = new Vec4(0,0,0,-0.2) // negative alpha means stripey
	else
		f.fillColor = new Vec4(0,0,0,0.2)
	
	return f
}

// remove any other lines attached to this box that we might have picked up by default
_.frameDrawing.clear()

// this causes the that drawing to be transformed by the size and position of the box
_.frameDrawing.ourOutlineDrawer = FLineDrawing.boxScale(f)

// this clears the output canvas
_.output.o.clear()
_.auto=0
