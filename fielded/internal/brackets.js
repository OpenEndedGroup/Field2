var lineNumbers = $(".CodeMirror-linenumbers").get(0)

raph = Raphael(lineNumbers, "100%", "100%")

function executeBracket(bra) {
    bra.attr({
        fill: "#afc"
    }).animate({
        fill: "#fff"
    }, 500)

    anchorLine = Math.max(cm.getLineNumber(bra.h1), cm.getLineNumber(bra.h2))

    c = cm.getCursor()

    fragment = cm.getDoc().getRange({
        line: cm.getLineNumber(bra.h1),
        ch: 0
    }, {
        line: cm.getLineNumber(bra.h2) + 1,
        ch: 0
    })

    _field.sendWithReturn("execution.fragment", {
        box: cm.currentbox,
        property: cm.currentproperty,
        text: fragment,
        lineoffset: cm.getLineNumber(bra.h1)
    }, function (d, e) {
        if (d.type == 'error')
            appendRemoteOutputToLine(anchorLine, d.line + " : " + d.message, "Field-remoteOutput", "Field-remoteOutput-error", 1)
        else
            appendRemoteOutputToLine(anchorLine, d.message, "Field-remoteOutput-error", "Field-remoteOutput", 1)
    });
}
function executeCurrentBracket() {
    if (currentBracket != null) {
        executeBracket(currentBracket)
    }
}

function rectForLineHandle(lh) {
    var y = cm.getLineNumber(lh);

    if (y > -1) {
        z = cm.charCoords({
            line: y,
            ch: 1
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

    if (r1 && r2) {
        sz = (r2.bottom - r1.top) / 8
        sz = -18 + level * 7
        r2.bottom -= 8
        r1.top -= 8

        w = 30 + sz - 10
        w2 = 48
        w3 = 10 + sz - 10
        w3 = sz;
        //								return "M" + w2 + "," + r1.top + "L" + w + "," + r1.top + "C" + (-8 + w3) + "," + r1.top + "," + (20 + w3)
        // + "," + ((r1.top + r2.bottom) / 2) + "," + w3 + "," + ((r1.top + r2.bottom) / 2) + "C" + (20 + w3) + "," + ((r1.top + r2.bottom) / 2) +
        // "," + (-8 + w3) + "," + r2.bottom + "," + w + "," + r2.bottom + "L" + w2 + "," + r2.bottom; return
        // "M"+w2+","+r1.top+"L"+w+","+r1.top+"L"+w+","+r2.bottom+"L"+w2+","+r2.bottom;

        rr = -10;

        return "M" + w2 + "," + r1.top + "L" + (w - rr) + "," + r1.top + "Q" + w + "," + r1.top + "," + w + "," + (r1.top - rr) + "L" + w + "," + (r2.bottom + rr) + "Q" + w + "," + (r2.bottom) + "," + (w - rr) + "," + (r2.bottom) + "L" + w2 + "," + r2.bottom;
    }
    return null;
}

raph.clear()

function makePathForHandles(h1, h2, level, disabled) {
    f = findPathForLines(h1, h2)
    if (f) return f;

    ps = pathStringForTwoLineHandles(h1, h2, level)
    if (ps) {
        var path = raph.path()
        path.attr({
            "stroke-opacity": 0.25
        })
        path.attr({
            "fill-opacity": (disabled ? 0.1 : 0.25)
        })
        path.attr({"stroke-dasharray": disabled ? ". " : ""})
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

        path.mouseover(function () {
            path.attr({
                "stroke-opacity": 1.0,
            })
        })
        path.mouseout(function () {
            path.attr({
                "stroke-opacity": 0.25,
            })
        })
        path.mousedown(function (e) {
            if (e.altKey) {
                currentBracket = path;
                executeCurrentBracket();
            }
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
            ret += "makePathForHandles(cm.getLineHandle(" + cm.getLineNumber(e.h1) + "), cm.getLineHandle(" + cm.getLineNumber(e.h2) + "), " + e.level + ", "+e.disabled+")\n"
        }
    })
    return ret
}

function allDisabledBracketRanges() {
    ret = ""
    updateAllBrackets()
    raph.forEach(function (e) {
    			if (e.disabled)
            ret += "["+cm.getLineNumber(e.h1)+", "+cm.getLineNumber(e.h2)+"], ";
    })
    return ret
}


function findPathForLines(h1, h2) {
    var found;
    raph.forEach(function (e) {
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
    raph.forEach(function (e) {
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
    raph.forEach(function (e) {
        if ("isHandleDecorator" in e)
            e.deltWith = 0
        e.kill = 0
    })
    raph.forEach(function (e) {
        if ("isHandleDecorator" in e && e.deltWith == 0) {
            recurSortConflictsOf(e, 0);
        }
    })
    raph.forEach(function (e) {
        if ("isHandleDecorator" in e) {
//            console.log("bracket level is " + e.level);
        }
    })

    raph.forEach(function (e) {
        var a = (cm.getLineNumber(e.h1))
        var b = (cm.getLineNumber(e.h2))
        raph.forEach(function (e2) {
            if ("isHandleDecorator" in e && e != e2) {
                var a2 = (cm.getLineNumber(e2.h1))
                var b2 = (cm.getLineNumber(e2.h2))
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
    });
    raph.forEach(function (e) {
        if ("isHandleDecorator" in e) {
            if (e.kill == 1) {
                e.remove();
            }
        }
    })

}

function findEnclosingPathForLine(line) {
    var found = null;
    raph.forEach(function (e) {
        if ("isHandleDecorator" in e) {
            if (cm.getLineNumber(e.h1) <= line && cm.getLineNumber(e.h2) >= line) {
                if (found == null)
                    found = e
                else if (Math.abs(cm.getLineNumber(found.h1) - cm.getLineNumber(found.h2)) > Math.abs(cm.getLineNumber(e.h1) - cm.getLineNumber(e.h2)))
                    found = e
            }
        }
    })
    return found;
}

raph.clear()

var currentBracket = null;

// while dragging
var ignoreBracketChanges = false;

function updateAllBrackets() {
    if (ignoreBracketChanges) return;

    sortConflicts();
    raph.forEach(function (e) {
        if ("isHandleDecorator" in e) {
            var ps = pathStringForTwoLineHandles(e.h1, e.h2, e.level)


            if (ps) {
                e.attr({
                    path: ps,
                    "stroke-opacity": 0.25,
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
        currentBracket = f
    } else {
        currentBracket = null
    }

}

updateAllBrackets()

var currentErrorLine = null;

cm.on("change", function (x, c) {
    updateAllBrackets()
    if (currentErrorLine) {
        cm.removeLineClass(currentErrorLine, "background")
        currentErrorLine = null;
    }
})

_messageBus.subscribe("error.line", function (d, e) {
    if (d.line) {
        if (currentErrorLine) {
            cm.removeLineClass(currentErrorLine, "background")
            currentErrorLine = null;
        }
        currentErrorLine = cm.addLineClass(d.line - 1, "background", "FieldError-line");
    }
});

cm.on("cursorActivity", function (x, c) {
    var f = findEnclosingPathForLine(cm.getCursor().line)
    if (f != currentBracket) {
        updateAllBrackets();
    }
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

globalCommands.push({
    "name": "Remove current bracket",
    "info": "Deletes the bracket that the cursor is currently inside",
    "callback": function () {
        updateAllBrackets();
        if (currentBracket != null)
            currentBracket.remove();
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
        if (currentBracket!=null)
        {
        	currentBracket.disabled=true;
	        currentBracket.attr({"stroke-dasharray": (currentBracket.disabled ? "- " : "")})
	        currentBracket.attr({"fill-opacity": (currentBracket.disabled ? 0.1 : 0.25)})
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
        if (currentBracket!=null)
        {
        	currentBracket.disabled=false;
	        currentBracket.attr({"stroke-dasharray": (currentBracket.disabled ? "- " : "")})
	        currentBracket.attr({"fill-opacity": (currentBracket.disabled ? 0.1 : 0.25)})
	        currentBracket.attr({"stroke": (currentBracket.disabled ? "#fff" : "#000")})
        }
    },
    "guard": function () {
        updateAllBrackets();
        return currentBracket != null && currentBracket.disabled;
    }
});

