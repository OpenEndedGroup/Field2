var lineNumbers = $(".CodeMirror-linenumbers").get(0)

raph = Raphael(lineNumbers, "100%", "100%")

function rectForLineHandle(lh) {
    var y = cm.getLineNumber(lh);
    if (y > -1) {
        z = cm.charCoords({
            line: y
        });

        li = cm.lineInfo(lh)
        if (li) {
            w = li.widgets
            if (w && w.length > 0) {
                for (var i = 0; i < w.length; i++) {
                    z.bottom += $(w[i].node).height()
                }
            }
        }

        z.top -= $(lineNumbers).offset().top-8
        z.bottom -= $(lineNumbers).offset().top-8

        return z;
    }
    return null;
}

function pathStringForTwoLineHandles(lh1, lh2) {
    r1 = rectForLineHandle(lh1)
    r2 = rectForLineHandle(lh2)
    console.log("rect for line "+cm.getLineNumber(lh1)+" is "+r1.bottom+" "+r1.top)
    if (r1 && r2) {
        sz = (r2.bottom - r1.top) / 8
        sz = 25;
        r2.bottom -= 8
        r1.top -= 5

        w = 30 + sz - 10
        w2 = 48
        w3 = 10 + sz - 10
        return "M" + w2 + "," + r1.top + "L" + w + "," + r1.top + "C" + (-8 + w3) + "," + r1.top + "," + (20 + w3) + "," + ((r1.top + r2.bottom) / 2) + "," + w3 + "," + ((r1.top + r2.bottom) / 2) + "C" + (20 + w3) + "," + ((r1.top + r2.bottom) / 2) + "," + (-8 + w3) + "," + r2.bottom + "," + w + "," + r2.bottom + "L" + w2 + "," + r2.bottom;
    }
    return null;
}

raph.clear()


function makePathForHandles(h1, h2) {
    ps = pathStringForTwoLineHandles(h1, h2)
    if (ps) {
        var path = raph.path()
        console.log(path)
        path.attr({
            "stroke-width": 1
        })
        path.attr({
            "stroke-opacity": 0.5
        })
        path.attr({
            "fill-opacity": 0.25
        })
        path.attr({
            fill: "#fff"
        })
        path.attr({
            stroke: "#fff"
        })
        path.attr({
            opacity: 0.4
        })
        path.attr({
            path: ps
        })

        path.mouseover(function () {
            path.attr({
                "stroke-opacity": 1.0
            })
        })
        path.mouseout(function () {
            path.attr({
                "stroke-opacity": 0.5
            })
        })

        path.h1 = h1
        path.h2 = h2
        path.isHandleDecorator = 1

        h1.on("delete", function (x) {
            path.remove()
        })
        h2.on("delete", function (x) {
            path.remove()
        })

        return path
    }
    return null;
}

function serializeAllBrackets() {
    ret = ""
    updateAllBrackets()
    raph.forEach(function (e) {
        if ("isHandleDecorator" in e) {
            ret += "makePathForHandles(cm.getLineHandle(" + cm.getLineNumber(e.h1) + "), cm.getLineHandle(" + cm.getLineNumber(e.h2) + "))\n"
        }
    })
    return ret
}

function findPathForLines(h1, h2)
{
	var found;
	raph.forEach(function (e) {
        if ("isHandleDecorator" in e) {
        	if (cm.getLineNumber(e.h1)==h1 && cm.getLineNumber(e.h2)==h2)
	        	found = e
        }
    })
	return found;
}



raph.clear()

function updateAllBrackets() {
    raph.forEach(function (e) {
        if ("isHandleDecorator" in e) {
            var ps = pathStringForTwoLineHandles(e.h1, e.h2)
            if (ps) {
                e.attr({
                    path: ps
                })
            } else {
                e.attr({
                    path: ""
                })
                e.isHandleDecorator = 0
            }

        }
    })
}

updateAllBrackets()

cm.on("change", function (x, c) {
    updateAllBrackets()
})
cm.on("fold", function (x, c) {
    updateAllBrackets()
})
cm.on("unfold", function (x, c) {
    updateAllBrackets()
})

$(window).resize(function () {
    updateAllBrackets()
})