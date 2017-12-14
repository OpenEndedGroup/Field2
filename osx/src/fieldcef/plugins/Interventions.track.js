var Vec4 = Java.type('field.linalg.Vec4')
var FLineDrawing = Java.type('fieldbox.boxes.FLineDrawing')
var FLine = Java.type('field.graphics.FLine')
var InterpolateGroup = Java.type('fieldcef.plugins.InterpolateGroup')
var Interventions = Java.type('fieldcef.plugins.Interventions')

// interpolation logic
var ig = new InterpolateGroup()

_.evalInterpolation = (time) => {
    return ig.interpolate(time, _.children)
}

// ---------------
// appearance & interface behavior
_.frame.h = 20

// resize with children
Interventions.coversChildren(_)

// don't resize height
_.lockHeight = true

// don't draw normally
_.frameDrawing.clear()

// draw like this instead
var f = new FLine()
f.rect(0,0,1,1)

f.filled = true
f.fillColor = new Vec4(0,0,0,0.2)
f.strokeColor = new Vec4(0,0,0,0.4)

_.lines.f = FLineDrawing.boxScale(f, _)

// don't draw connection markers unless selected
_.shyConnections = true

// automatically run all this on load
_.auto =1.0