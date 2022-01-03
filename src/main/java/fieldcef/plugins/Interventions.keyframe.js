// keyframe set by manipulating code
_.value = ${value}

// everything else below this concerns drawing this box

// don't let the height and width of this box change
_.frame.w = 20
_.frame.h = 20
_.lockWidth = true
_.lockHeight = true

var FLine = Java.type('field.graphics.FLine')
var Vec4 = Java.type('field.linalg.Vec4')
var Area = Java.type('java.awt.geom.Area')
var FLinesAndJavaShapes = Java.type('field.graphics.FLinesAndJavaShapes')
var Vec2 = Java.type('field.linalg.Vec2')
var FLineDrawing = Java.type('fieldbox.boxes.FLineDrawing')

var f = new FLine()
f.moveTo(0,-20).lineTo(0,40)
f.thicken=0.25

f.color = new Vec4(1,1,1,0.5)

_.frameDrawing.clear()

_.lines.left = FLineDrawing.boxOrigin(f, new Vec2(0,0), _)

f = new FLine().circle(0,10,7)

// this has got to be made simplier and more direct on FLine
var s1 = new Area(FLinesAndJavaShapes.flineToJavaShape(f))
var s2 = new Area(FLinesAndJavaShapes.flineToJavaShape(new FLine().rect(0,0,_.frame.w, _.frame.h)))
s1.intersect(s2)
var circ = FLinesAndJavaShapes.javaShapeToFLine(s1)



circ.filled = true
circ.color = new Vec4(0.5,0.5,0.5,1)
circ.thicken=1.5
_.lines.circ = FLineDrawing.boxOrigin(circ, new Vec2(0,0), _)

_.redraw()

_.shyConnections = true

_.auto = 1
