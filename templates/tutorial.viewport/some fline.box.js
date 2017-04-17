var Vec4 = Java.type('field.linalg.Vec4')
var FLine = Java.type('field.graphics.FLine')


// FLines work in 3d as well, although, you should be warned, it's possible to ask for a tesselation that's ambiguous or get odd results with fills that overlap in 2d but don't intersect in 3d.

_.lines3.clear()

f = new FLine()
f.moveTo(0,0,0).lineTo(1,0,0).lineTo(0,1,0).cubicTo(1,1,0,-2,0,0,0,0,0)
f.filled=true
f.color=new Vec4(1,1,1,0.1)
_.lines3.f = f

f = new FLine()
f.moveTo(0,0,1).lineTo(1,0,1).lineTo(0,1,1).cubicTo(1,1,1,-2,0,1,0,0,1)
f.filled=true
f.color=new Vec4(1,1,1,0.1)
_.lines3.f2 = f

_.redraw()