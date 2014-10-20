//b = $("body").append("<div id='somethingelse'><div>")
//rrRaph = Raphael("somethingelse", 600, 24)

addCanvasToBody = function(name, w, h)
{
	b = $("body").append("<div id='"+name+"'><div>")
	return Raphael(name, w, h);
}

dragThang = function(into, get,setAndGet, diameter)
{
	var internalSet = function(target, x,y)
	{
		target.attr({
			"cx": x,
			"cy": y
		});
	}

	var internalGet = function(target)
	{
		return [target.attr("cx"),target.attr("cy")];
	}

	var down = function() {
		o = internalGet(this)
		this.ox = o[0];
		this.oy = o[1];
		this.wx = o[0];
		this.wy = o[1];
		this.animate({
			"fill-opacity": 0.5
		}, 50);
	}
	var move = function(dx, dy) {
		var width = this.paper.width,
			height = this.paper.height,
			thisBox = this.getBBox()

		var ddx = this.ox + dx;
		var ddy = this.oy + dy;

		constrained = setAndGet([this.ox, this.oy], [this.wx, this.wy], [ddx,ddy])

		internalSet(this, constrained[0], constrained[1])

		this.wx = constrained[0]
		this.wy = constrained[1]
		this.attr({"stroke":"#300"})
	}

	var up = function() {
		this.animate({
			"fill-opacity": 0.2,
			"stroke":"#000"
		}, 500);
	}

	initial = get()

	var shapes = into.circle(0,0, diameter/2)
	internalSet(shapes, initial[0], initial[1])

	setAndGet(initial, initial, initial)

	var color = Raphael.getColor();
	shapes.attr({
		fill: "eee",
		"fill-opacity": 0.2,
		"stroke-width": 2,
		cursor: "move",
		stroke:"#000"
	});
	shapes.hover(
		function(){this.attr({"stroke":"#300"})},
		function(){this.attr({"stroke":"#000"})})
	shapes.drag(move, down, up);
	shapes.move = move
	return shapes
}

rrRaph.clear()
rrRaph.setSize(300,300)

h = 24
w = 300

insetW = 5

makeSlider = function(rrRaph, w, h, get, setAndConstrain)
{
	rrRaph.path("M"+insetW+","+1+"L"+insetW+","+(h-1)).attr({"stroke":"#000","stroke-width":0.5})
	rrRaph.path("M"+(w-insetW)+","+1+"L"+(w-insetW)+","+(h-1)).attr({stroke:"#000", "stroke-width":0.5})
	rrRaph.path("M"+insetW+","+(h/2)+"L"+(w-insetW)+","+(h/2)).attr({stroke:"#000","stroke-width":2})

	oo = dragThang(
		rrRaph,
		function(){
			return [insetW+get()*(w-insetW*2),h/2]
		},
		function(originally, previously, now)
		{
			c = now[0]
			if (c<insetW) c = insetW
			if (c>w-insetW) c = w-insetW

			v = setAndConstrain((originally[0]-insetW)/(w-insetW*2), (previously[0]-insetW)/(w-insetW*2), (c-insetW)/(w-insetW*2))

			c = v*(w-insetW*2)+insetW

			return [c, originally[1]];
		},
		insetW*2
	);
}

//makeSlider(rrRaph, w, h, function(){return 0.5}, function(o,p,n){_field.log(n);return n})

h = 200
w = 200

makeXYSlider = function(rrRaph, w, h, get, setAndConstrain, bounds, fiducial)
{
	if (bounds)
		rrRaph.path("M"+insetW+","+insetW+"L"+insetW+","+(h-insetW)+"L"+(w-insetW)+","+(h-insetW)+"L"+(w-insetW)+","+insetW+"z").attr({"stroke":"#000","stroke-width":0.5})
	var fid = rrRaph.path("M20,30L40,50").attr({"stroke-width":(fiducial ? 2 : 0.5), "stroke":"#000"})

	var current = get()

	oo = dragThang(
		rrRaph,
		function(){
			return [insetW+get()[0]*(w-insetW*2),insetW+get()[1]*(w-insetW*2)]
		},
		function(originally, previously, now)
		{
			var c = [now[0],now[1]]
			if (c[0]<insetW) c[0] = insetW
			if (c[0]>w-insetW) c[0] = w-insetW
			if (c[1]<insetW) c[1] = insetW
			if (c[1]>h-insetW) c[1] = h-insetW

			var v = setAndConstrain([(originally[0]-insetW)/(w-insetW*2),(originally[1]-insetW)/(h-insetW*2)], [(previously[0]-insetW)/(w-insetW*2),(previously[1]-insetW)/(h-insetW*2)], [(c[0]-insetW)/(w-insetW*2),(c[1]-insetW)/(h-insetW*2)])

			c[0] = v[0]*(w-insetW*2)+insetW
			c[1] = v[1]*(w-insetW*2)+insetW

			fid.attr({path:"M0,"+c[1]+"L"+w+","+c[1]+"M"+c[0]+",0L"+c[0]+","+h})
			current[0] = v[0]
			current[1] = v[1]

			return [c[0], c[1]];
		},
		insetW*2
	);

	return function(){return current;}
}

//makeXYSlider(rrRaph, w, h, function(){return [0.5,0.5]}, function(o,p,n){return n}, true)

make4Graph = function(rrRaph, w, h, get, setAndConstrain)
{
	var vz = get()

	var curve = rrRaph.path().attr({"stroke-width":4, "stroke":"#000"})

	var updatePath = function()
	{
		var z = "M"+(vz[0][0]*(w-insetW*2)+insetW)+","+(vz[0][1]*(h-insetW*2)+insetW);
		z += "T"+(vz[1][0]*(w-insetW*2)+insetW)+","+(vz[1][1]*(h-insetW*2)+insetW);
		z += "T"+(vz[2][0]*(w-insetW*2)+insetW)+","+(vz[2][1]*(h-insetW*2)+insetW);
		z += "T"+(vz[3][0]*(w-insetW*2)+insetW)+","+(vz[3][1]*(h-insetW*2)+insetW);
		_field.log(z)
		curve.attr({path:z})
	}
	var s0 = makeXYSlider(rrRaph, w, h, function(){return vz[0]}, function(o, p, n){vz[0]=n; updatePath(); return n;}, true, false)
	var s1 = makeXYSlider(rrRaph, w, h, function(){return vz[1]}, function(o, p, n){vz[1]=n; updatePath(); return n;}, false, false)
	var s2 = makeXYSlider(rrRaph, w, h, function(){return vz[2]}, function(o, p, n){vz[2]=n; updatePath(); return n;}, false, false)
	var s3 = makeXYSlider(rrRaph, w, h, function(){return vz[3]}, function(o, p, n){vz[3]=n; updatePath(); return n;}, false, false)

	return function(){return vz;}
}


//g = make4Graph(rrRaph, w, h, function(){return [[0.0,0.5],[0.25,0.5],[0.75,0.5],[1.0,0.5]]}, function(o,p,n){return n}, true)


