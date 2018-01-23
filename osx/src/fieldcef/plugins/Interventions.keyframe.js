// keyframe set by manipulating code
_.value = 0.4

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
var SimpleConnection = Java.type('field.graphics.util.onsheetui.SimpleConnection')

_.withOverloading = true

_.frameDrawing.clear()

// this has got to be made simplier and more direct on FLine
var aCircle = new FLine().circle(0,10,7)
var aRectangle = new FLine().rect(0,0,_.frame.w, _.frame.h)

// intersect the circle with the rectangle to make a half-rectangle
aCircle = aCircle * aRectangle
aCircle.filled = true

aCircle.color = new Vec4(0.5,0.4,0.15,1)
aCircle.thicken=1.5
_.lines.circ = FLineDrawing.boxOrigin(aCircle, new Vec2(0,0), _)

_.redraw()

// don't show connection decoration
_.shyConnections = true

// show this connection decoration instead
new SimpleConnection().leftXToMiddleY("__toParent__", _, _.parents.first, new Vec4(0.5,0.45,0.15,1))

// automatically execute this when the document is loaded
_.auto = 1
