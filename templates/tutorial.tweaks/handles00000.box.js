var FLinesAndJavaShapes = Java.type('field.graphics.FLinesAndJavaShapes')
var FLinesAndJavaText = Java.type('field.graphics.FLinesAndJavaText')
var Vec3 = Java.type('field.linalg.Vec3')
var Vec4 = Java.type('field.linalg.Vec4')
var FLine = Java.type('field.graphics.FLine')

// import contents of tweaks box
_.exec(__.named.tweaks[0].code)


// here's a line to edit
f = new FLine().moveTo(40,0).lineTo(100,100).cubicTo(50,40,30,20,200,100)
_.lines.clear()


// we'll save our edits to this property
prop = "tweaks"

// set things up
beginTweaks(prop)

// change the way that it looks
f.color = new Vec4(1,0,0,0.4)

// make it look better (OpenGL thin lines generally look bad)
f.thicken = 2

// add this to the set of lines to be drawn
_.lines.f = f

// make lines editable, apply all edits to date. The whole box will be reexecuted on deselection
endTweaks(prop)

// now 'f' has been edited, use the edited lines.
f2 = new FLine().data('ms*', FLinesAndJavaShapes.samplePoints(f, 0.1))

f = f2
f2.color = new Vec4(0,0,0,0.1)

for(let n=1;n<10;n++)
{
	f = f.byTransforming(x => {
		return new Vec3(x.x+Math.random()*15*Math.sqrt(n), x.y+50+Math.random()*15*Math.sqrt(n), 0)
	})
	
	_.lines.add(f)
}

_.redraw()