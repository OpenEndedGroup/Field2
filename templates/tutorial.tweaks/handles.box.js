var FLinesAndJavaShapes = Java.type('field.graphics.FLinesAndJavaShapes')
var FLinesAndJavaText = Java.type('field.graphics.FLinesAndJavaText')
var Vec3 = Java.type('field.linalg.Vec3')
var Vec4 = Java.type('field.linalg.Vec4')

_.exec(__.named.tweaks[0].code)

var FLine = Java.type('field.graphics.FLine')

f = new FLine().moveTo(40,0).lineTo(100,100).cubicTo(50,40,30,20,200,100)

_.lines.clear()

prop = "tweaks"


beginTweaks(prop)


f.color = new Vec4(1,0,0,0.4)
f.thicken = 2

_.lines.f = f




endTweaks(prop)

f2 = new FLine().data('ms*', FLinesAndJavaShapes.samplePoints(f, 0.1))
f = f2
f2.color = new Vec4(0,0,0,0.1)

for(let n=1;n<10;n++)
{
	f = f.byTransforming(x => {
		return new Vec3(x.x+Math.random()*15, x.y+50+Math.random()*15, 0)
	})
	
	_.lines.add(f)
}

_.redraw()