
// builder is imported from the box above
// _.builder takes a lot more CPU time than local variable access

b = _.builder
b.open()
b.v(0,0,0)
b.v(4,0,0)
b.v(4,4,-1)

// this makes a triangle over the last three vertices (counting backwards from the most recent one '0'
b.e(0,1,2) 
b.close()



// note: Field is extremely lazy about actually "redrawing" the window. This text editor and even some of the UI elements to the left are composited together with the base layer. Without an explicit redraw you might find that things don't appear to update until you deselect things or pan around a little
_.redraw()


