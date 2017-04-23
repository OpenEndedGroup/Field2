var HandlesForFLines = Java.type('fieldbox.boxes.plugins.HandlesForFLines')
var DraggableNode = Java.type('fieldbox.boxes.plugins.HandlesForFLines.DraggableNode')



// call this function at some point near the begining of the box
beginTweaks = (prop) => {
	_.once[prop] = () => ""
}

// call this function to apply by any hand alterations, and to make all lines editable
endTweaks = (prop) => {	
	_.exec(_[prop])

	for each (var e in _.lines.entrySet())
	{
		name = e.getKey()
		for each (let n in e.getValue().nodes)
			d = new DraggableNode(f, n, 
								  s => {
				desc = s.describe("HandlesForFLines", "_.lines."+name)
				_.tweaks += desc+"\n"
				_.next.update = () => {
					_()
				}
			})
	}
}