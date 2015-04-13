// ---- the Field/Processing plugin ----

// _.P refers to the Processing applet

_.P

// (you can get completion on _.P.

// how to clear the background of the window to black
_.P.background(0)


// mouse events are handled Field-style, not Processing style, with Idempotent maps
_.P.onMouseClicked.doSomething = function(inside, event)
{
	print("mouse clicked at "+event.x+" "+event.y)
}
