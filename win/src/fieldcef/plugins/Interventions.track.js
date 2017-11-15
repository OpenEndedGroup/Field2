var FLine = Java.type('field.graphics.FLine')


// appearance
_.frame.h = 20
_.lockHeight = true

_.frameDrawing.clear()

f = new FLine()
f.rect(0,0,1,1)

f.filled = true
f.fillColor = new Vec4(0,0,0,0.2)
f.strokeColor = new Vec4(0,0,0,0.4)

_.shyConnections = true
