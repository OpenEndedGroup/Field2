var BasicStroke = Java.type('java.awt.BasicStroke')
var HashSet = Java.type('java.util.HashSet')
var Collectors = Java.type('java.util.stream.Collectors')

_.lockWidth=true
_.frame.w=25
_.frame.h=1000
_.name=""

var FLine = Java.type('field.graphics.FLine')
var Vec4 = Java.type('field.linalg.Vec4')
var FLineDrawing = Java.type('fieldbox.boxes.FLineDrawing')
var Box = Java.type('fieldbox.boxes.Box')

_.frameDrawing.clear()

_.frameDrawing.redfill = FLineDrawing.boxScale( (box) => {
	f = new FLine().rect(0,0,1,1)
	f.color=new Vec4(1,0,0,box.isSelected ? -0.5 : 0.1)
	f.filled=true		
	return f
})

_.frameDrawing.redline = FLineDrawing.boxScale( (box) => {
	f = new FLine().rect(0,0,0.05,1)
	f.color=new Vec4(1,0,0,box.isSelected ? 0.5 : 0.1)
	f.thicken=new BasicStroke(5)
	return f
})


exclude = new HashSet([_.time, _])

_.onFrameChanged.move = (inside, newRect) => {
	newRect.y=inside.frame.y

	a = inside.frame.x
	b = newRect.x
	
	outgoing = __.children.stream()
		.filter(x => !exclude.contains(x))
		.filter(x => x.frame!=null)
		.filter(x => x.frame.intersectsX(a) && !x.frame.intersectsX(b))
		.forEach(x => x.end())
	
	incomming = __.children.stream()
		.filter(x => !exclude.contains(x))
		.filter(x => x.frame!=null)
		.filter(x => !x.frame.intersectsX(a) && x.frame.intersectsX(b))
		.forEach(x => x.begin())
	
	return newRect
}
