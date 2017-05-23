var lineNumbers = $(".CodeMirror-linenumbers").get(0);

raph = Raphael(lineNumbers, "100%", "100%");

function cmGetLineNumber(n) {
	//console.log(" got a " + Object.keys(n) + " in getlinenumber");
	//console.log((new Error()).stack);

	if (n.find())
		return n.find().line;
	else
		return undefined;

//	return cm.getLineNumber(n)
}

function cmGetLineHandle(n, ending) {

	if (ending)
		return cm.setBookmark({line: n, pos: 1000}, {insertLeft:true});
	else
		return cm.setBookmark({line: n, pos: 0}, {insertLeft:false});

//	return cm.getLineHandle(n);
}
function executeBracket(bra, disablePrint) {
	bra.attr({
		fill: "#afc"
	}).animate({
		fill: "#eee"
	}, 500);

	anchorLine = Math.max(cmGetLineNumber(bra.h1), cmGetLineNumber(bra.h2));

	c = cm.getCursor();

	fragment = cm.getDoc().getRange({
		line: cmGetLineNumber(bra.h1),
		ch: 0
	}, {
		line: cmGetLineNumber(bra.h2) + 1,
		ch: 0
	});

	addr = "execution.fragment";
	if (disablePrint)
		addr = "execution.fragment.noprint";

	_field.sendWithReturn(addr, {
		box: cm.currentbox,
		property: cm.currentproperty,
		text: fragment,
		lineoffset: cmGetLineNumber(bra.h1),
		disabledRanges: "[" + allDisabledBracketRanges() + "]"
	}, function (d, e) {
		if (d.type == 'error')
			appendRemoteOutputToLine(anchorLine, d.line + " : " + d.message, "Field-remoteOutput", "Field-remoteOutput-error", 1);
		else
			appendRemoteOutputToLine(anchorLine, d.message, "Field-remoteOutput-error", "Field-remoteOutput", 1)
	});
}
function executeBracketLimited(bra, lineLimit) {
	bra.attr({
		fill: "#afc"
	}).animate({
		fill: "#eee"
	}, 500);

	anchorLine = Math.max(cmGetLineNumber(bra.h1), cmGetLineNumber(bra.h2));

	c = cm.getCursor();

	fragment = cm.getDoc().getRange({
		line: cmGetLineNumber(bra.h1),
		ch: 0
	}, {
		line: lineLimit+ 1,
		ch: 0
	});

	addr = "execution.fragment";
	//if (disablePrint)
	//	addr = "execution.fragment.noprint"

	_field.sendWithReturn(addr, {
		box: cm.currentbox,
		property: cm.currentproperty,
		text: fragment,
		lineoffset: cmGetLineNumber(bra.h1),
		disabledRanges: "[" + allDisabledBracketRanges() + "]"
	}, function (d, e) {
		if (d.type == 'error')
			appendRemoteOutputToLine(anchorLine, d.line + " : " + d.message, "Field-remoteOutput", "Field-remoteOutput-error", 1);
		else
			appendRemoteOutputToLine(anchorLine, d.message, "Field-remoteOutput-error", "Field-remoteOutput", 1)
	});
}


function executeCurrentBracket() {
	if (currentBracket != null) {
		executeBracket(currentBracket)
	}
}
function executeCurrentBracketToHere(){
	if (currentBracket!=null)
	{
		executeBracketLimited(currentBracket, cm.getCursor(false).line);
	}
}

function rectForLineHandle(lh) {
	var y = cmGetLineNumber(lh);

	if (y > -1) {
		z = cm.charCoords({
			line: y,
			ch: 1
		});

		li = cm.lineInfo(y);
		if (li) {
			w = li.widgets;
			if (w && w.length > 0) {
				for (var i = 0; i < w.length; i++) {
					z.bottom += $(w[i].node).height()
				}
			}
		}

		z.top -= $(lineNumbers).offset().top - 8;
		z.bottom -= $(lineNumbers).offset().top - 8;

		return z;
	}
	return null;
}

function pathStringForTwoLineHandles(lh1, lh2, level) {
	r1 = rectForLineHandle(lh1);
	r2 = rectForLineHandle(lh2);

	if (r1 && r2) {
		sz = (r2.bottom - r1.top) / 8;
		sz = -18 + level * 7;
		r2.bottom -= 8;
		r1.top -= 8;

		w = 30 + sz - 10;
		w2 = 48;
		w3 = 10 + sz - 10;
		w3 = sz;
		//								return
		// "M" + w2 + "," + r1.top + "L" + w + "," + r1.top + "C" + (-8 + w3) + "," + r1.top + "," + (20 + w3) + "," +
		// ((r1.top + r2.bottom) / 2) + "," + w3 + "," + ((r1.top + r2.bottom) / 2) + "C" + (20 + w3) + "," + ((r1.top +
		// r2.bottom) / 2) + "," + (-8 + w3) + "," + r2.bottom + "," + w + "," + r2.bottom + "L" + w2 + "," + r2.bottom;
		// return "M"+w2+","+r1.top+"L"+w+","+r1.top+"L"+w+","+r2.bottom+"L"+w2+","+r2.bottom;

		rr = -10;

		return "M" + w2 + "," + (r1.top+1) + "L" + (w - rr) + "," + (r1.top+1) + "Q" + w
			+ "," + (r1.top+1) + "," + w + "," + (r1.top +1 - rr) + "L" + w + "," + (r2.bottom -1+ rr)
			+ "Q" + w + "," + (r2.bottom+1-1) + "," + (w - rr) + "," + (r2.bottom+1-1) + "L" + w2 + "," + (r2.bottom+1-1);
	}
	return null;
}

raph.clear();

function makePathForHandles(h1, h2, level, disabled, annotation) {
	var f = findPathForLines(h1, h2);
	if (f) {
		f.disabled=disabled;
		f.annotation = annotation;
		if (f.t)
			f.t.remove();

		// var r1 = rectForLineHandle(h1);
		// var r2 = rectForLineHandle(h2);
		// var t = raph.text(5, (r1.top+r2.bottom)/2, annotation="PEACH")
		// t.attr({"text-align":"right"});
		// t.attr({"fill":"#bbb"});
		// t.attr({"fill-opacity":0.3});
		// f.annotation = annotation;
		// t.isHandleAnnotation = 1;
		// f.t = t;

		return f;
	}

	ps = pathStringForTwoLineHandles(h1, h2, level);
	if (ps) {
		var path = raph.path();
		path.attr({
			"stroke-opacity": 0.25
		});
		path.attr({
			fill: "#eee"
			//fill:"45-#fff-#fff:0-#000:0-#000:10-#fff:10-#fff:20-#000:20-#000:30-#fff:30-#fff:40-#000:40-#000:50-#fff:50-#fff:60-#000:60-#000:70-#fff:70-#fff:80-#000:80-#000:90-#fff:90"
		});
		path.attr({
			stroke: "#000"
		});
		path.attr({
			opacity: 0.4
		});
		path.attr({
			"stroke-width": 2.0
		});
		path.attr({
			path: ps
		});


		path.attr({"stroke-dasharray": (disabled ? "- " : "")});
		path.attr({"fill-opacity": (disabled ? 0.2 : 0.5)});
		path.attr({"stroke": (disabled ? "#fff" : "#000")});

		path.mouseover(function () {
			path.attr({
				"stroke-opacity": 1.0
			})
		});

		path.mouseout(function () {
			path.attr({
				"stroke-opacity": 0.25
			})
		});

		path.mousedown(function (e) {
			if (e.altKey) {
				currentBracket = path;
				executeCurrentBracket();
			}
		});

		path.h1 = h1;
		path.h2 = h2;
		path.isHandleDecorator = 1;
		path.disabled=disabled;

		h1.on("delete", function (x) {
			path.remove()
			path.t.remove()
		});
		h2.on("delete", function (x) {
			path.remove()
			path.t.remove()
		});

		// var r1 = rectForLineHandle(h1);
		// var r2 = rectForLineHandle(h2);
		// var t = raph.text(5, (r1.top+r2.bottom)/2, annotation="PEACH")
		// t.attr({"text-align":"right"});
		// t.attr({"fill":"#bbb"});
		// t.attr({"fill-opacity":0.3});
		// path.annotation = annotation;
		// t.isHandleAnnotation = 1;
		// path.t = t;

		return path
	}
	return null;
}

function serializeAllBrackets() {
	ret = "";
	updateAllBrackets();
	raph.forEach(function (e) {
		if ("isHandleDecorator" in e) {
			ret += "makePathForHandles(cmGetLineHandle(" + cmGetLineNumber(e.h1) + ", false), cmGetLineHandle(" + cmGetLineNumber(e.h2) + ", true), " + e.level + ", " + e.disabled + ", '"+e.annotation+"')\n"
		}
	});
	return ret
}

function allDisabledBracketRanges() {
	ret = "";
	updateAllBrackets();
	raph.forEach(function (e) {
		if (e.disabled)
			ret += "[" + cmGetLineNumber(e.h1) + ", " + cmGetLineNumber(e.h2) + "], ";
	});
	return ret
}

function findPathForLines(h1, h2) {
	var found;
	raph.forEach(function (e) {
		if ("isHandleDecorator" in e) {
			if (cmGetLineNumber(e.h1) == h1 && cmGetLineNumber(e.h2) == h2)
				found = e
		}
		else 			console.log("didn't find isHandleDecorator in ",e)

	});
	return found;
}

function recurSortConflictsOf(at, level) {
	var a = (cmGetLineNumber(at.h1));
	var b = (cmGetLineNumber(at.h2));
	at.level = level;
	at.deltWith = 1;
	raph.forEach(function (e) {
		if ("isHandleDecorator" in e && e.deltWith == 0 && e != at) {
			var a2 = (cmGetLineNumber(e.h1));
			var b2 = (cmGetLineNumber(e.h2));

			if (a2 <= b && b2 >= a) {
				recurSortConflictsOf(e, level + 1);
			}
		}
	})
}

function sortConflicts() {
	raph.forEach(function (e) {
		if ("isHandleDecorator" in e)
			e.deltWith = 0;
		e.kill = 0
	});
	raph.forEach(function (e) {
		if ("isHandleDecorator" in e && e.deltWith == 0) {
			recurSortConflictsOf(e, 0);
		}
	});
	raph.forEach(function (e) {
		if ("isHandleDecorator" in e) {
		}
	});

	raph.forEach(function (e) {
		if ("isHandleDecorator" in e) {
			var a = (cmGetLineNumber(e.h1));
			var b = (cmGetLineNumber(e.h2));
			raph.forEach(function (e2) {
				if ("isHandleDecorator" in e2 && e != e2) {
					var a2 = (cmGetLineNumber(e2.h1));
					var b2 = (cmGetLineNumber(e2.h2));
					if (a == a2 && b == b2) {
						if (e.level > e2.level) {
							e.kill = 1
						}
						else {
							e2.kill = 1
						}
					}
				}
			})
		}
	});
	raph.forEach(function (e) {
		if ("isHandleDecorator" in e) {
			if (e.kill == 1) {
				if (e.t)
					e.t.remove();
				e.remove();
			}
		}
	})

}

function findEnclosingPathForLine(line) {
	var found = null;
	raph.forEach(function (e) {
		if ("isHandleDecorator" in e) {
			if (cmGetLineNumber(e.h1) <= line && cmGetLineNumber(e.h2) >= line) {
				if (found == null)
					found = e;
				else if (Math.abs(cmGetLineNumber(found.h1) - cmGetLineNumber(found.h2)) > Math.abs(cmGetLineNumber(e.h1) - cmGetLineNumber(e.h2)))
					found = e
			}
		}
	});
	return found;
}

raph.clear();

var currentBracket = null;

// while dragging
var ignoreBracketChanges = false;

function updateAllBrackets() {
	if (ignoreBracketChanges) return;

	var ln = cm.lineCount();
	for(var i=0;i<ln;i++)
	{
		cm.removeLineClass(i, "wrap", "FieldDisabled-line");
		cm.removeLineClass(i, "wrap", "FieldDisabled-line-top");
		cm.removeLineClass(i, "wrap", "FieldDisabled-line-bottom");
	}

	sortConflicts();
	raph.forEach(function (e) {
		if ("isHandleDecorator" in e) {
			var ps = pathStringForTwoLineHandles(e.h1, e.h2, e.level);

			if (ps) {
				e.attr({
					path: ps,
					"stroke-opacity": 0.25
				})
			} else {
				e.attr({
					path: ""
				});
				e.isHandleDecorator = 0
			}
		}
		if (e.disabled)
		{
			console.log(" found a disabled line, trying to set its background");
			console.log(" at line "+e.h1+" "+cmGetLineNumber(e.h1));
			cm.addLineClass(cmGetLineNumber(e.h1), "wrap", "FieldDisabled-line-top");
			cm.addLineClass(cmGetLineNumber(e.h2), "wrap", "FieldDisabled-line-bottom");

			for(var a=cmGetLineNumber(e.h1)+1;a<cmGetLineNumber(e.h2);a++)
				cm.addLineClass(a, "wrap", "FieldDisabled-line");
		}
	});

	var f = findEnclosingPathForLine(cm.getCursor().line);
	if (f != null) {
		f.attr({
			"stroke-opacity": 1.0,
			"stroke":"#fa4"
		});
		currentBracket = f
	} else {
		currentBracket = null
	}

}

updateAllBrackets();

var currentErrorLine = null;

cm.on("change", function (x, c) {
	updateAllBrackets();
	if (currentErrorLine) {
		cm.removeLineClass(currentErrorLine, "background");
		currentErrorLine = null;
	}
});

_messageBus.subscribe("error.line", function (d, e) {
	if (d.line) {
		if (currentErrorLine) {
			cm.removeLineClass(currentErrorLine, "background");
			currentErrorLine = null;
		}
		currentErrorLine = cm.addLineClass(d.line - 1, "background", "FieldError-line");
	}
});

cm.on("cursorActivity", function (x, c) {
	var f = findEnclosingPathForLine(cm.getCursor().line);
	if (f != currentBracket) {
		updateAllBrackets();
	}
});

cm.on("fold", function (x, c) {
	updateAllBrackets()
});
cm.on("unfold", function (x, c) {
	updateAllBrackets()
});

$(window).resize(function () {
	updateAllBrackets()
});

globalCommands.push({
	"name": "Remove current bracket",
	"info": "Deletes the bracket that the cursor is currently inside",
	"callback": function () {
		updateAllBrackets();
		if (currentBracket != null)
			currentBracket.remove();
		updateAllBrackets();
	},
	"guard": function () {
		updateAllBrackets();
		return currentBracket != null;
	}
});

globalCommands.push({
	"name": "Disable current bracket",
	"info": "Comments out the current bracket, preventing execution with alt-0 and .begin()",
	"callback": function () {
		updateAllBrackets();
		if (currentBracket != null) {
			currentBracket.disabled = true;
			currentBracket.attr({"stroke-dasharray": (currentBracket.disabled ? "- " : "")});
			currentBracket.attr({"fill-opacity": (currentBracket.disabled ? 0.2 : 0.5)});
			currentBracket.attr({"stroke": (currentBracket.disabled ? "#fff" : "#000")})
		}
	},
	"guard": function () {
		updateAllBrackets();
		return currentBracket != null && !currentBracket.disabled;
	}
});
globalCommands.push({
	"name": "Enable current bracket",
	"info": "un-Comments out the current bracket, allowing execution with alt-0 and .begin()",
	"callback": function () {
		updateAllBrackets();
		if (currentBracket != null) {
			currentBracket.disabled = false;
			currentBracket.attr({"stroke-dasharray": (currentBracket.disabled ? "- " : "")});
			currentBracket.attr({"fill-opacity": (currentBracket.disabled ? 0.2 : 0.5)});
			currentBracket.attr({"stroke": (currentBracket.disabled ? "#fff" : "#000")})
		}
	},
	"guard": function () {
		updateAllBrackets();
		return currentBracket != null && currentBracket.disabled;
	}
});
globalCommands.push({
	"name": "Execute bracket to here",
	"info": "Executes the contents of this bracket up to, and including, the line that the cursor is on",
	"callback": function () {
		executeCurrentBracketToHere();
	},
	"guard": function () {
		updateAllBrackets();
		return currentBracket != null;
	}
});

