var Vec4 = Java.type('field.linalg.Vec4')
var FLineDrawing = Java.type('fieldbox.boxes.FLineDrawing')
var FLine = Java.type('field.graphics.FLine')
var InterpolateGroup = Java.type('fieldcef.plugins.InterpolateGroup')

// interpolation logic

var ig = new InterpolateGroup()

_.evalInterpolation = (time) => {
    return ig.interpolate(time, _.children)
}

// appearance
_.frame.h = 20
_.lockHeight = true

_.frameDrawing.clear()

var f = new FLine()
f.rect(0,0,1,1)

f.filled = true
f.fillColor = new Vec4(0,0,0,0.2)
f.strokeColor = new Vec4(0,0,0,0.4)

_.shyConnections = true

_.lines.f = FLineDrawing.boxScale(f, _)

_.auto =1.0