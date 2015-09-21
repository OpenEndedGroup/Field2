function stringify(obj, replacer, spaces, cycleReplacer) {
	return JSON.stringify(obj, serializer(replacer, cycleReplacer), spaces)
}

function serializer(replacer, cycleReplacer) {
	var stack = [], keys = []

	if (cycleReplacer == null) cycleReplacer = function (key, value) {
		if (stack[0] === value) return "[Circular ~]"
		return "[Circular ~." + keys.slice(0, stack.indexOf(value)).join(".") + "]"
	}

	return function (key, value) {
		if (stack.length > 0) {
			var thisPos = stack.indexOf(this)
			~thisPos ? stack.splice(thisPos + 1) : stack.push(this)
			~thisPos ? keys.splice(thisPos, Infinity, key) : keys.push(key)
			if (~stack.indexOf(value)) value = cycleReplacer.call(this, key, value)
		}
		else stack.push(value)

		return replacer == null ? value : replacer.call(this, key, value)
	}
}

function newWidgetCanvas(w, h) {
	var div = $('<div class="Field-remoteWidget" style="width:' + w + 'px; height:' + h + 'px;"><div class="Field-closebox">&#x2715;</div></div>')[0];

	var selection_start = cm.getCursor(true);
	var selection_end = cm.getCursor(false);

	var widget = cm.addLineWidget(cm.getLineHandle(selection_end.line), div);

	var closeBox = $($(div).find(".Field-closeBox"))
	closeBox.click(function () {
		updateAllBrackets()
		widget.clear();
		updateAllBrackets()
	})

	var canvas = Raphael(div, w, h);
	updateAllBrackets()

	div.serialization_1 = "newWidgetCanvas(" + w + ", " + h + ")";

	return canvas;
}

function newMarkingCanvas(start, stop, w, h) {
	var div = $('<div class="Field-remoteWidgetInline" style="position:relative; padding:0px; padding-left:0px; overflow:display; margin:0px; display: inline-block; vertical-align:middle; width:' + w + 'px; height:' + h + 'px;"><div class="Field-expandBoxInline">&#x2715;</div></div>')[0];

	var selection_start = start;
	var selection_end = stop;

	var widget = cm.markText(start, stop, {atomic: true, replacedWith: div});

	var canvas = Raphael(div, w, h);
	updateAllBrackets()

	div.canvas = canvas;

	var closeBox = $($(div).find(".Field-expandBoxInline"))
	closeBox.click(function () {
		updateAllBrackets()
		widget.clear();
		updateAllBrackets()
	})

	div.lastOutputAt = new Date().valueOf();
	div.bm = widget;

	return {"div": div, "canvas": canvas, "mark": widget};
}

dragThang = function (into, get, setAndGet, diameter) {

	var internalSet = function (target, x, y) {
		target.attr({
			"cx": x,
			"cy": y
		});
	}

	var internalGet = function (target) {
		return [target.attr("cx"), target.attr("cy")];
	}

	var down = function () {
		o = internalGet(this)
		this.ox = o[0];
		this.oy = o[1];
		this.wx = o[0];
		this.wy = o[1];
		this.animate({
			"fill-opacity": 0.5
		}, 50);

		ignoreBracketChanges = true

	}
	var move = function (dx, dy) {
		var width = this.paper.width,
			height = this.paper.height,
			thisBox = this.getBBox()

		var ddx = this.ox + dx;
		var ddy = this.oy + dy;

		constrained = setAndGet([this.ox, this.oy], [this.wx, this.wy], [ddx, ddy])

		internalSet(this, constrained[0], constrained[1])

		this.wx = constrained[0]
		this.wy = constrained[1]
		this.attr({"stroke": "#300"})
	}

	var up = function () {
		this.animate({
			"fill-opacity": 0.2,
			"stroke": "#000"
		}, 500);
		ignoreBracketChanges = false
		updateAllBrackets();
	}

	initial = get()
	console.log(" new drag thang :" + initial[0] + " " + initial[1]);

	var shapes = into.circle(0, 0, diameter / 2)
	internalSet(shapes, initial[0], initial[1])

	setAndGet(initial, initial, initial)

	var color = Raphael.getColor();
	shapes.attr({
		fill: "eee",
		"fill-opacity": 0.2,
		"stroke-width": 2,
		cursor: "move",
		stroke: "#000"
	});
	shapes.hover(
		function () {
			this.attr({"stroke": "#300"})
		},
		function () {
			this.attr({"stroke": "#000"})
		})
	shapes.drag(move, down, up);
	shapes.move = move
	return shapes
}

insetW = 0

makeSlider = function (rrRaph, w, h, get, setAndConstrain) {
	for (var q = 1; q < 10; q++)
		rrRaph.path("M" + (insetW + (q / 10.0) * (w - insetW * 2)) + "," + 1 + "L" + (insetW + (q / 10.0) * (w - insetW * 2)) + "," + (h)).attr({
			stroke: "#111",
			"stroke-width": 0.5
		})
	rrRaph.path("M" + insetW + "," + (h / 2) + "L" + (w - insetW) + "," + (h / 2)).attr({stroke: "#000", "stroke-width": 2})

	oo = dragThang(
		rrRaph,
		function () {
			return [insetW + get() * (w - insetW * 2), h / 2]
		},
		function (originally, previously, now) {
			c = now[0]
			if (c < insetW) c = insetW
			if (c > w - insetW) c = w - insetW

			v = setAndConstrain((originally[0] - insetW) / (w - insetW * 2), (previously[0] - insetW) / (w - insetW * 2), (c - insetW) / (w - insetW * 2))

			c = v * (w - insetW * 2) + insetW

			return [c, originally[1]];
		},
		10
	);
}

//makeSlider(rrRaph, w, h, function(){return 0.5}, function(o,p,n){_field.log(n);return n})

insertFloatSlider = function (selection_start, selection_end) {
	var w = 300;
	var h = 20;

	console.log(" inserting a new marking canvas into the editor between " + stringify(selection_start) + " and " + stringify(selection_end));
	canvas = newMarkingCanvas(selection_start, selection_end, w, h);

	var marks = canvas.mark;

	get = function () {
		f = parseFloat(cm.getDoc().getRange(marks.find().from, marks.find().to));
		return f;
	}
	set = function (o, p, x) {

		ff = {line: marks.find().from.line, ch: marks.find().from.ch}
		ff.ch += 1;
		ft = {line: marks.find().to.line, ch: marks.find().to.ch}
		ft.ch -= 1;

		if (cm.getDoc().getRange(ff, ft) != "" + x) {
			cm.getDoc().replaceRange("" + x, ff, ft)
			//cm.refresh();

			f = findEnclosingPathForLine(selection_start.line)
			if (f != null) {
				executeBracket(f, true)
			}

		}

		return x;
	}

	makeSlider(canvas.canvas, w, h, get, set)

	// serialize this

//	console.log("about to set serialization")
	$(canvas.div).data("serialization", function () {
		fc = marks.find().from.ch
		tc = marks.find().to.ch
		fl = marks.find().from.line
		tl = marks.find().to.line

		// todo, add guard to make sure that this text hasn't changed....
		return "insertFloatSlider({'line':" + fl + ",'ch':" + fc + "},{'line':" + tl + ",'ch':" + tc + "})";
	})
	updateAllBrackets();

	return canvas;
}

insertFloatSliderHere = function () {
	var selection_start = cm.getCursor("from");
	cm.replaceRange(" 0.0 ", selection_start) // we pad with spaces, otherwise when we replace this text we blow away our widget as well
	var selection_end = cm.getCursor("to");
	return insertFloatSlider(selection_start, selection_end);
}

insertFloatSliderAtSelection = function () {
	var selection_start = cm.getCursor("from");
	var selection_end = cm.getCursor("to");
	var r = cm.getDoc().getRange(selection_start, selection_end);
	if (r == "") {
		return insertFloatSliderHere();
	}
	else if (!isNaN(parseFloat(r))) {

		var start = cm.setBookmark(selection_start);
		var end = cm.setBookmark(selection_end);

		if (r[0] != " ") r = " " + r
		if (r[r.length - 1] != " ") r = r + " "
		cm.getDoc().replaceRange(r, selection_start, selection_end);

		insertFloatSlider(start.find(), end.find());
	}
	else {

	}
}

globalCommands.push({
	"name": "Insert float slider",
	"info": "Inserts a slider that goes from 0.0 to 1.0 at the cursor, or (if possible) over the text selected",
	"callback": function () {
		insertFloatSliderAtSelection()
	}
});

makeXYSlider = function (rrRaph, w, h, get, setAndConstrain, bounds, fiducial) {
	if (bounds)
		rrRaph.path("M" + insetW + "," + insetW + "L" + insetW + "," + (h - insetW) + "L" + (w - insetW) + "," + (h - insetW) + "L" + (w - insetW) + "," + insetW + "z").attr({
			"stroke": "#000",
			"stroke-width": 0.5
		})
	var fid = rrRaph.path("M20,30L40,50").attr({"stroke-width": (fiducial ? 2 : 0.5), "stroke": "#000"})

	var current = get()

	oo = dragThang(
		rrRaph,
		function () {
			return [insetW + get()[0] * (w - insetW * 2), insetW + get()[1] * (w - insetW * 2)]
		},
		function (originally, previously, now) {
			var c = [now[0], now[1]]
			if (c[0] < insetW) c[0] = insetW
			if (c[0] > w - insetW) c[0] = w - insetW
			if (c[1] < insetW) c[1] = insetW
			if (c[1] > h - insetW) c[1] = h - insetW

			var v = setAndConstrain([(originally[0] - insetW) / (w - insetW * 2), (originally[1] - insetW) / (h - insetW * 2)], [(previously[0] - insetW) / (w - insetW * 2), (previously[1] - insetW) / (h - insetW * 2)], [(c[0] - insetW) / (w - insetW * 2), (c[1] - insetW) / (h - insetW * 2)])

			c[0] = v[0] * (w - insetW * 2) + insetW
			c[1] = v[1] * (w - insetW * 2) + insetW

			console.log(" path will be " + c[0] + " " + c[1]);

			fid.attr({path: "M0," + c[1] + "L" + w + "," + c[1] + "M" + c[0] + ",0L" + c[0] + "," + h})
			current[0] = v[0]
			current[1] = v[1]

			return [c[0], c[1]];
		},
		10
	);

	return function () {
		return current;
	}
}

insertXYSlider = function (selection_start, selection_end) {
	var w = 300;
	var h = 300;

	canvas = newMarkingCanvas(selection_start, selection_end, w, h);

	var marks = canvas.mark;

	try {
		get = function () {

			//f = parseFloat(cm.getDoc().getRange(marks.find().from, marks.find().to));
			rr = cm.getDoc().getRange(marks.find().from, marks.find().to);
			console.log(" rr <- " + rr);
			f = eval(rr);
			return f;
		}
		set = function (o, p, x) {

			ff = {line: marks.find().from.line, ch: marks.find().from.ch}
			ff.ch += 1;
			ft = {line: marks.find().to.line, ch: marks.find().to.ch}
			ft.ch -= 1;

			rr = cm.getDoc().getRange(marks.find().from, marks.find().to);
			console.log(" rr <beginSet ||" + rr + "||");

			norm = " [" + ("" + x).trim() + "] ";
			console.log(" rr <to ||" + norm + "||");
			if (cm.getDoc().getRange(ff, ft) != norm) {
				console.log(" rr <was ||" + cm.getDoc().getRange(ff, ft) + "||");
				cm.getDoc().replaceRange(norm, ff, ft)
				//cm.refresh();

				f = findEnclosingPathForLine(selection_start.line)

				if (f != null) {
					executeBracket(f, true)
				}

			}

			rr = cm.getDoc().getRange(marks.find().from, marks.find().to);
			console.log(" rr <endSet ||" + rr + "||");

			return x;
		}

		makeXYSlider(canvas.canvas, w, h, get, set)

		// serialize this

		$(canvas.div).data("serialization", function () {
			fc = marks.find().from.ch
			tc = marks.find().to.ch
			fl = marks.find().from.line
			tl = marks.find().to.line
			console.log(" inside serialization range is " + fc + " " + tc + "  -> " + fl + " " + tl);

			// todo, add guard to make sure that this text hasn't changed....
			return "insertXYSlider({'line':" + fl + ",'ch':" + fc + "},{'line':" + tl + ",'ch':" + tc + "})";
		})
		updateAllBrackets();
	}
	catch (e) {
		console.log(" error on inserting xy slider ");
		console.log(e);
		marks.clear();
	}

	return canvas;
}

insertXYSliderHere = function () {
	var selection_start = cm.getCursor("from");
	cm.replaceRange(" [0.5,0.5] ", selection_start) // we pad with spaces, otherwise when we replace this text we blow away our widget
	// as well
	var selection_end = cm.getCursor("to");
	return insertXYSlider(selection_start, selection_end);
}

insertXYSliderAtSelection = function () {
	var selection_start = cm.getCursor("from");
	var selection_end = cm.getCursor("to");
	var r = cm.getDoc().getRange(selection_start, selection_end);
	if (r == "") {
		return insertXYSliderHere();
	}
	else //if (!isNaN(parseFloat(r)))
	{
		var start = cm.setBookmark(selection_start);
		var end = cm.setBookmark(selection_end);
		r = r.trim();
		if (r[0] != " ") r = " " + r
		if (r[r.length - 1] != " ") r = r + " "
		cm.getDoc().replaceRange(r, selection_start, selection_end);

		insertXYSlider(start.find(), end.find());
	}
}

globalCommands.push({
	"name": "Insert XY slider",
	"info": "Inserts a slider that goes from (0.0, 0.0) to (1.0, 1.0) at the cursor, or (if possible) over the text selected",
	"callback": function () {
		insertXYSliderAtSelection()
	}
});

make4Graph = function (rrRaph, w, h, get, setAndConstrain) {
	var vz = get()

	console.log(" initial value for 4 graph is " + stringify(vz));

	var curve = rrRaph.path().attr({"stroke-width": 4, "stroke": "#000"})

	var updatePath = function () {
		console.log(" path geometry will be " + stringify(vz));

		var z = "M" + (vz[0][0] * (w - insetW * 2) + insetW) + "," + (vz[0][1] * (h - insetW * 2) + insetW);
		z += "T" + (vz[1][0] * (w - insetW * 2) + insetW) + "," + (vz[1][1] * (h - insetW * 2) + insetW);
		z += "T" + (vz[2][0] * (w - insetW * 2) + insetW) + "," + (vz[2][1] * (h - insetW * 2) + insetW);
		z += "T" + (vz[3][0] * (w - insetW * 2) + insetW) + "," + (vz[3][1] * (h - insetW * 2) + insetW);
		console.log(" path geometry is  " + stringify(vz));
		curve.attr({path: z})
	}
	var s0 = makeXYSlider(rrRaph, w, h, function () {
		vz = get()
		return vz[0]
	}, function (o, p, n) {
		vz[0] = n;
		updatePath();
		q = setAndConstrain(vz, vz, vz);
		return q[0];
	}, true, false)
	var s1 = makeXYSlider(rrRaph, w, h, function () {
		vz = get()
		return vz[1]
	}, function (o, p, n) {
		vz[1] = n;
		updatePath();
		return setAndConstrain(vz, vz, vz)[1];
	}, false, false)
	var s2 = makeXYSlider(rrRaph, w, h, function () {
		vz = get()
		return vz[2]
	}, function (o, p, n) {
		vz[2] = n;
		updatePath();
		return setAndConstrain(vz, vz, vz)[2];
	}, false, false)
	var s3 = makeXYSlider(rrRaph, w, h, function () {
		vz = get()
		return vz[3]
	}, function (o, p, n) {
		vz[3] = n;
		updatePath();
		return setAndConstrain(vz, vz, vz)[3];
	}, false, false)

	return function () {
		return vz;
	}
}

//g = make4Graph(rrRaph, w, h, function(){return [[0.0,0.5],[0.25,0.5],[0.75,0.5],[1.0,0.5]]}, function(o,p,n){return n}, true)

insert4GraphSlider = function (selection_start, selection_end) {
	var w = 300;
	var h = 300;

	canvas = newMarkingCanvas(selection_start, selection_end, w, h);

	var marks = canvas.mark;

	//try {
		get = function () {

			rr = cm.getDoc().getRange(marks.find().from, marks.find().to);
			console.log(" rr4 <- ||" + rr + "||");
			f = JSON.parse(rr);
			console.log(" rr (f) <- " + stringify(f));
			return f;
		}
		set = function (o, p, x) {

			ff = {line: marks.find().from.line, ch: marks.find().from.ch}
			ff.ch += 1;
			ft = {line: marks.find().to.line, ch: marks.find().to.ch}
			ft.ch -= 1;

			rr = cm.getDoc().getRange(marks.find().from, marks.find().to);
			console.log(" rr <beginSet ||" + rr + "||");
			console.log(" rr <to ||" +x + "||");

			norm = " " + (stringify(x)).trim() + " ";
			console.log(" rr <to ||" + norm + "||");
			if (cm.getDoc().getRange(ff, ft) != norm) {
				console.log(" rr <was ||" + cm.getDoc().getRange(ff, ft) + "||");
				cm.getDoc().replaceRange(norm, ff, ft)
				//cm.refresh();

				f = findEnclosingPathForLine(selection_start.line)

				if (f != null) {
					executeBracket(f, true)
				}

			}

			rr = cm.getDoc().getRange(marks.find().from, marks.find().to);
			console.log(" rr <endSet ||" + rr + "||");

			return x;
		}

		make4Graph(canvas.canvas, w, h, get, set)

		// serialize this

		$(canvas.div).data("serialization", function () {
			fc = marks.find().from.ch
			tc = marks.find().to.ch
			fl = marks.find().from.line
			tl = marks.find().to.line
			console.log(" inside serialization range is " + fc + " " + tc + "  -> " + fl + " " + tl);

			// todo, add guard to make sure that this text hasn't changed....
			return "insert4GraphSlider({'line':" + fl + ",'ch':" + fc + "},{'line':" + tl + ",'ch':" + tc + "})";
		})
		updateAllBrackets();
	//}
	//catch (e) {
	//	console.log(" error on inserting 4graphslider slider ");
	//	console.log(e);
	//	marks.clear();
	//}

	return canvas;
}

insert4GraphSliderHere = function () {
	var selection_start = cm.getCursor("from");
	cm.replaceRange(" [[0.0,0.0],[0.333,0.333],[0.666,0.666],[1,1]] ", selection_start) // we pad with spaces, otherwise when we replace
						    // this text we blow away our widget
	// as well
	var selection_end = cm.getCursor("to");
	return insert4GraphSlider(selection_start, selection_end);
}

insert4GraphSliderAtSelection = function () {
	var selection_start = cm.getCursor("from");
	var selection_end = cm.getCursor("to");
	var r = cm.getDoc().getRange(selection_start, selection_end);
	if (r == "") {
		return insert4GraphSliderHere();
	}
	else //if (!isNaN(parseFloat(r)))
	{
		var start = cm.setBookmark(selection_start);
		var end = cm.setBookmark(selection_end);
		r = r.trim();
		if (r[0] != " ") r = " " + r
		if (r[r.length - 1] != " ") r = r + " "
		cm.getDoc().replaceRange(r, selection_start, selection_end);

		insert4GraphSlider(start.find(), end.find());
	}
}

globalCommands.push({
	"name": "Insert 2d, 4-point graph",
	"info": "Inserts a four breakpoint graph that goes from (0.0, 0.0) to (1.0, 1.0) at the cursor, or (if possible) over the text selected",
	"callback": function () {
		insert4GraphSliderAtSelection()
	}
});


make14Graph = function (rrRaph, w, h, get, setAndConstrain) {
	var vz = get()

	console.log(" initial value for 4 graph is " + stringify(vz));

	var curve = rrRaph.path().attr({"stroke-width": 4, "stroke": "#000"})
	var curve2 = rrRaph.path().attr({"stroke-width": 1, "stroke": "#555"})

	var updatePath = function () {
		var z = "M" + (0 * (w - insetW * 2) + insetW) + "," + (vz[0] * (h - insetW * 2) + insetW);
		z += "C " + (0.3333 * (w - insetW * 2) + insetW) + "," + (vz[1] * (h - insetW * 2) + insetW);
		z +=" "+ (0.6666 * (w - insetW * 2) + insetW) + "," + (vz[2] * (h - insetW * 2) + insetW);
		z +=  " "+(1 * (w - insetW * 2) + insetW) + "," + (vz[3] * (h - insetW * 2) + insetW);
		curve.attr({path: z})

		z = "M"+(0 * (w - insetW * 2) + insetW) + "," + (vz[0] * (h - insetW * 2) + insetW);
		z += "L " + (0.3333 * (w - insetW * 2) + insetW) + "," + (vz[1] * (h - insetW * 2) + insetW);
		z +="M"+ (0.6666 * (w - insetW * 2) + insetW) + "," + (vz[2] * (h - insetW * 2) + insetW);
		z +=  "L"+(1 * (w - insetW * 2) + insetW) + "," + (vz[3] * (h - insetW * 2) + insetW);
		curve2.attr({path: z})
	}
	var s0 = makeXYSlider(rrRaph, w, h, function () {
		vz = get()
		return [0, vz[0]]
	}, function (o, p, n) {
		n[0] = 0
		vz[0] = n[1];
		updatePath();
		q = setAndConstrain(vz, vz, vz);
		return [0, q[0]];
	}, true, false)
	var s1 = makeXYSlider(rrRaph, w, h, function () {
		vz = get()
		return [0.3333, vz[1]]
	}, function (o, p, n) {
		n[0] = 0.3333
		vz[1] = n[1];
		updatePath();
		q = setAndConstrain(vz, vz, vz);
		return [0.3333, q[1]];
	}, false, false)
	var s2 = makeXYSlider(rrRaph, w, h, function () {
		vz = get()
		return [0.6666, vz[2]]
	}, function (o, p, n) {
		n[0] = 0.6666
		vz[2] = n[1];
		updatePath();
		q = setAndConstrain(vz, vz, vz);
		return [0.6666, q[2]];
	}, false, false)
	var s3 = makeXYSlider(rrRaph, w, h, function () {
		vz = get()
		return [1, vz[3]]
	}, function (o, p, n) {
		n[0] = 1
		vz[3] = n[1];
		updatePath();
		q = setAndConstrain(vz, vz, vz);
		return [1, q[3]];
	}, false, false)

	return function () {
		return vz;
	}
}

//g = make4Graph(rrRaph, w, h, function(){return [[0.0,0.5],[0.25,0.5],[0.75,0.5],[1.0,0.5]]}, function(o,p,n){return n}, true)

insert14GraphSlider = function (selection_start, selection_end) {
	var w = 300;
	var h = 300;

	canvas = newMarkingCanvas(selection_start, selection_end, w, h);

	var marks = canvas.mark;

	//try {
	get = function () {

		rr = cm.getDoc().getRange(marks.find().from, marks.find().to);
		console.log(" rr4 <- ||" + rr + "||");
		f = JSON.parse(rr);
		console.log(" rr (f) <- " + stringify(f));
		return f;
	}
	set = function (o, p, x) {

		ff = {line: marks.find().from.line, ch: marks.find().from.ch}
		ff.ch += 1;
		ft = {line: marks.find().to.line, ch: marks.find().to.ch}
		ft.ch -= 1;

		rr = cm.getDoc().getRange(marks.find().from, marks.find().to);
		console.log(" rr <beginSet ||" + rr + "||");
		console.log(" rr <to ||" +x + "||");

		norm = " " + (stringify(x)).trim() + " ";
		console.log(" rr <to ||" + norm + "||");
		if (cm.getDoc().getRange(ff, ft) != norm) {
			console.log(" rr <was ||" + cm.getDoc().getRange(ff, ft) + "||");
			cm.getDoc().replaceRange(norm, ff, ft)
			//cm.refresh();

			f = findEnclosingPathForLine(selection_start.line)

			if (f != null) {
				executeBracket(f, true)
			}

		}

		rr = cm.getDoc().getRange(marks.find().from, marks.find().to);
		console.log(" rr <endSet ||" + rr + "||");

		return x;
	}

	make14Graph(canvas.canvas, w, h, get, set)

	// serialize this

	$(canvas.div).data("serialization", function () {
		fc = marks.find().from.ch
		tc = marks.find().to.ch
		fl = marks.find().from.line
		tl = marks.find().to.line
		console.log(" inside serialization range is " + fc + " " + tc + "  -> " + fl + " " + tl);

		// todo, add guard to make sure that this text hasn't changed....
		return "insert14GraphSlider({'line':" + fl + ",'ch':" + fc + "},{'line':" + tl + ",'ch':" + tc + "})";
	})
	updateAllBrackets();
	//}
	//catch (e) {
	//	console.log(" error on inserting 4graphslider slider ");
	//	console.log(e);
	//	marks.clear();
	//}

	return canvas;
}

insert14GraphSliderHere = function () {
	var selection_start = cm.getCursor("from");
	cm.replaceRange(" [0, 0.3333, 0.6666, 1] ", selection_start) // we pad with spaces, otherwise when we replace
						    // this text we blow away our widget
	// as well
	var selection_end = cm.getCursor("to");
	return insert14GraphSlider(selection_start, selection_end);
}

insert14GraphSliderAtSelection = function () {
	var selection_start = cm.getCursor("from");
	var selection_end = cm.getCursor("to");


	var r = cm.getDoc().getRange(selection_start, selection_end);
	if (r == "") {
		return insert14GraphSliderHere();
	}
	else //if (!isNaN(parseFloat(r)))
	{
		var start = cm.setBookmark(selection_start);
		var end = cm.setBookmark(selection_end);
		r = r.trim();
		if (r[0] != " ") r = " " + r
		if (r[r.length - 1] != " ") r = r + " "
		cm.getDoc().replaceRange(r, selection_start, selection_end);

		insert14GraphSlider(start.find(), end.find());
	}
}

globalCommands.push({
	"name": "Insert 1d, 4-point graph",
	"info": "Inserts a four breakpoint graph that goes from (0.0, 0.0) to (1.0, 1.0) at the cursor, or (if possible) over the text selected",
	"callback": function () {
		insert14GraphSliderAtSelection()
	}
});