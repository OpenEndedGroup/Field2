var lineNumbers = $(".CodeMirror-linenumbers").get(0)

raph = Raphael(lineNumbers, "100%", "100%")

function executeCurrentBracket() {
    if (currentBracket != null) {

        currentBracket.attr({
            fill: "#afc"
        }).animate({
            fill: "#fff"
        }, 500)

        anchorLine = Math.max(cm.getLineNumber(currentBracket.h1), cm.getLineNumber(currentBracket.h2) + 1)

        c = cm.getCursor()
        cm.setSelection({
            line: cm.getLineNumber(currentBracket.h1),
            ch: 0
        }, {
            line: cm.getLineNumber(currentBracket.h2) + 1,
            ch: 0
        })

        fragment = cm.getSelections()[0]

        _field.sendWithReturn("execution.fragment", {
            box: cm.currentbox,
            property: cm.currentproperty,
            text: fragment
        }, function(d, e) {
            if (d.type == 'error')
                appendRemoteOutputToLine(anchorLine, d.line + " : " + d.message, "Field-remoteOutput", "Field-remoteOutput-error", 1)
            else
                appendRemoteOutputToLine(anchorLine, d.message, "Field-remoteOutput-error", "Field-remoteOutput", 1)
        });
    }
}

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

        z.top -= $(lineNumbers).offset().top - 8
        z.bottom -= $(lineNumbers).offset().top - 8

        return z;
    }
    return null;
}

function pathStringForTwoLineHandles(lh1, lh2, level) {
    r1 = rectForLineHandle(lh1)
    r2 = rectForLineHandle(lh2)
    console.log("rect for line " + cm.getLineNumber(lh1) + " is " + r1.bottom + " " + r1.top)
    if (r1 && r2) {
        sz = (r2.bottom - r1.top) / 8
        sz = -18 + level * 7
        console.log(" sz is " + sz + " " + level);
        r2.bottom -= 8
        r1.top -= 8

        w = 30 + sz - 10
        w2 = 48
        w3 = 10 + sz - 10
        w3 = sz;
        //								return "M" + w2 + "," + r1.top + "L" + w + "," + r1.top + "C" + (-8 + w3) + "," + r1.top + "," + (20 + w3) + "," + ((r1.top + r2.bottom) / 2) + "," + w3 + "," + ((r1.top + r2.bottom) / 2) + "C" + (20 + w3) + "," + ((r1.top + r2.bottom) / 2) + "," + (-8 + w3) + "," + r2.bottom + "," + w + "," + r2.bottom + "L" + w2 + "," + r2.bottom;
        //						 return "M"+w2+","+r1.top+"L"+w+","+r1.top+"L"+w+","+r2.bottom+"L"+w2+","+r2.bottom;

        rr = -10;

        return "M" + w2 + "," + r1.top + "L" + (w - rr) + "," + r1.top + "Q" + w + "," + r1.top + "," + w + "," + (r1.top - rr) + "L" + w + "," + (r2.bottom + rr) + "Q" + w + "," + (r2.bottom) + "," + (w - rr) + "," + (r2.bottom) + "L" + w2 + "," + r2.bottom;
    }
    return null;
}

raph.clear()


function makePathForHandles(h1, h2, level) {
    f = findPathForLines(h1, h2)
    if (f) return f;

    ps = pathStringForTwoLineHandles(h1, h2, level)
    if (ps) {
        var path = raph.path()
        console.log(path)
        path.attr({
            "stroke-opacity": 0.0
        })
        path.attr({
            "fill-opacity": 0.25
        })
        path.attr({
            fill: "#fff"
        })
        path.attr({
            stroke: "#000"
        })
        path.attr({
            opacity: 0.4
        })
        path.attr({
            "stroke-width": 2.0
        })
        path.attr({
            path: ps
        })
        path.attr({
            'stroke-dasharray': '.'
        })

        path.mouseover(function() {
            path.attr({
                "stroke-opacity": 1.0,
                stroke: "#fff"
            })
        })
        path.mouseout(function() {
            path.attr({
                "stroke-opacity": 0.0,
                stroke: "#000"
            })
        })
        path.mousedown(function(e) {
            console.log("down?", e, e.altDown)
            if (e.altKey) {
                currentBracket = path;
                executeCurrentBracket();
            }
        })

        path.h1 = h1
        path.h2 = h2
        path.isHandleDecorator = 1

        h1.on("delete", function(x) {
            path.remove()
        })
        h2.on("delete", function(x) {
            path.remove()
        })

        return path
    }
    return null;
}

function serializeAllBrackets() {
    ret = ""
    updateAllBrackets()
    raph.forEach(function(e) {
        if ("isHandleDecorator" in e) {
            ret += "makePathForHandles(cm.getLineHandle(" + cm.getLineNumber(e.h1) + "), cm.getLineHandle(" + cm.getLineNumber(e.h2) + "), " + e.level + ")\n"
        }
    })
    return ret
}

function findPathForLines(h1, h2) {
    var found;
    raph.forEach(function(e) {
        if ("isHandleDecorator" in e) {
            if (cm.getLineNumber(e.h1) == h1 && cm.getLineNumber(e.h2) == h2)
                found = e
        }
    })
    return found;
}

function recurSortConflictsOf(at, level) {
    var a = (cm.getLineNumber(at.h1))
    var b = (cm.getLineNumber(at.h2))
    at.level = level
    at.deltWith = 1
    raph.forEach(function(e) {
        if ("isHandleDecorator" in e && e.deltWith == 0 && e != at) {
            var a2 = (cm.getLineNumber(e.h1))
            var b2 = (cm.getLineNumber(e.h2))
            if (a2 <= b && b2 >= a) {
                recurSortConflictsOf(e, level + 1);
            }
        }
    })
}

function sortConflicts() {
    raph.forEach(function(e) {
        if ("isHandleDecorator" in e)
            e.deltWith = 0
    })
    raph.forEach(function(e) {
        if ("isHandleDecorator" in e && e.deltWith == 0) {
            recurSortConflictsOf(e, 0);
        }
    })
    raph.forEach(function(e) {
        if ("isHandleDecorator" in e) {
            console.log("bracket level is " + e.level);
        }
    })
}


function findEnclosingPathForLine(line) {
    var found = null;
    raph.forEach(function(e) {
        if ("isHandleDecorator" in e) {
            if (cm.getLineNumber(e.h1) <= line && cm.getLineNumber(e.h2) >= line) {
                if (found == null)
                    found = e
                else if (Math.abs(cm.getLineNumber(found.h1) - cm.getLineNumber(found.h2)) > Math.abs(cm.getLineNumber(e.h1) - cm.getLineNumber(e.h1)))
                    found = e
            }
        }
    })
    return found;
}

raph.clear()

var currentBracket = null;

function updateAllBrackets() {

    sortConflicts();
    raph.forEach(function(e) {
        if ("isHandleDecorator" in e) {
            var ps = pathStringForTwoLineHandles(e.h1, e.h2, e.level)
            if (ps) {
                e.attr({
                    path: ps,
                    "stroke-opacity": 0.0,
                    'stroke-dasharray': ''
                })
            } else {
                e.attr({
                    path: ""
                })
                e.isHandleDecorator = 0
            }

        }
    })

    var f = findEnclosingPathForLine(cm.getCursor().line)
    if (f != null) {
        f.attr({
            "stroke-opacity": 1.0
        })
        f.attr({
            'stroke-dasharray': '-'
        })
        currentBracket = f
    } else {
        currentBracket = null
    }

}

updateAllBrackets()

cm.on("change", function(x, c) {
    updateAllBrackets()
})

cm.on("cursorActivity", function(x, c) {
    var f = findEnclosingPathForLine(cm.getCursor().line)
    if (f != currentBracket) {
        updateAllBrackets();
    }
})

cm.on("fold", function(x, c) {
    updateAllBrackets()
})
cm.on("unfold", function(x, c) {
    updateAllBrackets()
})

$(window).resize(function() {
    updateAllBrackets()
})