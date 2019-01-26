function escapeHtml(str) {
    var div = document.createElement('div');
    div.appendChild(document.createTextNode(str));
    return div.innerHTML;
}

function safeRenderJSON(text)
{
    try{
        console.log(" about to safe render json for text :")
        text = text.substring("<json>".length)
        console.log(text)
        var tt = eval("renderjson.set_show_to_level(1)("+text+")")

        console.log(" safe render succeeded with :")
        console.log(tt)
        return tt
    }
    catch(e)
    {
        console.log(" safe render failed ")
        console.log(e)
        return text.length>1000 ? text.substring(0, 1000)+" ... " : text
    }
}

if (!String.prototype.startsWith) {
    String.prototype.startsWith = function(searchString, position){
        return this.substr(position || 0, searchString.length) === searchString;
    };
}


function appendRemoteOutputToLine(line, text, checkClass, lineClass, append, tick) {

    text = text.trim()

    console.log(" will safe render json? ||"+text.startsWith("<json>")+"||")
    if (text.startsWith("<json>"))
    {
        text = safeRenderJSON(text)
        append = false
    }

    console.log("hello")
    lh = cm.getLineHandle(line);
    if (!lh) {
        return;
    }

    w = cm.lineInfo(lh).widgets;
    found = null;

    if (w)
        for (var i = 0; i < w.length; i++) {
            var ele = $(w[i].node);
            if (ele.hasClass(checkClass) || ele.hasClass(lineClass)) {
                // if (!append) {
                // 	w[i].clear();
                // 	i--;
                // } else {
                // 	append = false;
                found = w[i];
                // }
            }
        }

    var bm;

    if (found != null) {
        d = found.node;
        d.lastOutputAt = new Date().valueOf();
        d.tick = tick

        var D = $(d)
        D.removeClass(checkClass);
        D.removeClass(lineClass);
        D.addClass(lineClass);

        if (!append) {
            D.find(".outputline").remove();
        }
        else {
            // console.log(" children length is " + $(d).children().length)
            console.log(" appending ? ")
            if (D.children().length > 32) {
                D.find(".outputline").remove();
                D.append("<div class='outputline'>[...]</div>");
            }
        }
        D.append($("<div class='outputline'></div>").append(text))

        /*
        D.animate({
            scrollTop: D[0].scrollHeight
        }, 0);
*/
        D.css({
            opacity: 1,
            "background-color": "#666"
        });
        D.stop().animate({
            // opacity: 0.75,
            "background-color": "#000"
        }, 500);
        found.changed();

        D.scrollTop(D[0].scrollHeight)

    } else {
        d = $("<div class='" + lineClass + "'><div class='Field-closeBox'>&#x2715;</div><div class='Field-expandBox'>&#x21A7;</div></div>")[0];
        d.lastOutputAt = new Date().valueOf();
        d.tick = tick

        bm = cm.addLineWidget(lh, d, {
            showIfHidden: true,
            handleMouseEvents: true
        });

        // console.log(" line is "+cm.lineInfo(lh).n)
        // bm = cm.addWidget({line:line, ch:cm.getLine(line).length}, d);
        d.bm = bm;

        // $(d).append("<div class='outputline'>" + text + "</div>");
        $(d).append($("<div class='outputline'></div>").append(text));

        if (text.length < 1 || text == "&#10003;") {
            $(d).animate({opacity: 0.0, "max-height": "0%"}, {
                "duration": 400, "progress": function () {
                    bm.changed()
                }, "done": function () {
                    bm.clear();
                    updateAllBrackets();
                }
            });
        }
        else {
            setTimeout(function () {
                $(d).scrollTop($(d)[0].scrollHeight)
            }, 0);
        }

    }

    var thisDiv = $(d);
    var closeBox = $($(d).children()[0]);
    var expandBox = $($(d).children()[1]);
    //var hideBox = $($(d).children()[2])

    updateAllBrackets();

    if (bm) {
        closeBox.click(function () {
            bm.clear();
            updateAllBrackets()
        });
        closeBox.hover(function () {
            closeBox.css("color", "#f55")
        }, function () {
            closeBox.css("color", "#fff")
        });
        expandBox.hover(function () {
            $(this).css("color", "#ff5")
        }, function () {
            $(this).css("color", "#fff")
        });
        //hideBox.hover(function () {
        //	$(this).css("color", "#ff5")
        //}, function () {
        //	$(this).css("color", "#fff")
        //})
        expandBox.click(function () {
            if (thisDiv.maxSize().height > 2000) {
                thisDiv.css("max-height", "180");
                expandBox.html("&#x21A7;");
                updateAllBrackets();
                bm.changed();
                updateAllBrackets();
            } else {
                thisDiv.css("max-height", "2500");
                expandBox.html("&#x21A5;");
                updateAllBrackets();
                bm.changed();
                updateAllBrackets();
            }
        });
        //hideBox.click(function () {
        //	console.log("hello?");
        //	if (thisDiv.maxSize().height > 0) {
        //		thisDiv.css("max-height", "0")
        //		thisDiv.css("display", "none");
        //		bm.changed()
        //		hideBox.html("v")
        //		updateAllBrackets()
        //	} else {
        //		thisDiv.css("max-height", "50")
        //		thisDiv.css("display", "block");
        //		hideBox.html("h")
        //		updateAllBrackets()
        //		bm.changed()
        //	}
        //})
    }

}

function serializeAllOutput() {
    ret = "";

    for (var line = 0; line < cm.lineCount(); line++) {
        w = cm.lineInfo(line).widgets;

        if (w)
            for (var i = 0; i < w.length; i++) {
                var ele = $(w[i].node);
                if (ele.hasClass("Field-remoteOutput")) {
                    text = ele.clone().find(".Field-closebox").remove().end().find(".Field-expandBox").remove().end();
                    if ("html" in text) {
                        text = $(text).html();
                    }
                    else if ("text" in text) {
                        text = text.text()
                    }
                    else {
                        text = "";
                    }
                    ret += "appendRemoteOutputToLine(" + line + ", " + JSON.stringify(text) + ", 'Field-remoteOutput-error', 'Field-remoteOutput', false,0)\n";
                }
                if (ele.hasClass("Field-remoteOutput-error")) {
                    text = ele.clone().find(".Field-closebox").remove().end().find(".Field-expandBox").remove().end();
                    if ("html" in text) {
                        text = $(text).html();
                    }
                    else if ("text" in text) {
                        text = text.text()
                    }
                    else {
                        text = "";
                    }
                    ret += "appendRemoteOutputToLine(" + line + ", " + JSON.stringify(text) + ", 'Field-remoteOutput', 'Field-remoteOutput-error', false,0)\n";
                }
            }
    }
    return ret;
}

function clearOutputs(startLine, endLine) {
    for (var line = startLine; line < endLine; line++) {
        w = cm.lineInfo(line).widgets;
        if (w) {
            console.log(" CO :"+w.tick)
            for (var i = 0; i < w.length; i++) {
                var ele = $(w[i].node);
                console.log(" CO2 :" + ele.tick)
                if (ele.hasClass("Field-remoteOutput") || ele.hasClass("Field-remoteOutput-error")) {
                    w[i].clear();
                }
            }
        }
    }
    updateAllBrackets();
}
function clearOutputsOlderThan(startLine, endLine, tick) {
    for (var line = startLine; line < endLine; line++) {
        w = cm.lineInfo(line).widgets;

        if (w) {
            for (var i = 0; i < w.length; i++) {
                var ele = $(w[i].node);
                if (w[i].node)
                    if (ele.hasClass("Field-remoteOutput-error"))
                    {
                        w[i].clear();
                    }
                    if (ele.hasClass("Field-remoteOutput") && w[i].node.tick<tick) {
                         // w[i].clear();
                        $(w[i].node).css({background:"#222"})
                    }
            }
        }
    }
    updateAllBrackets();
}


globalCommands.push({
    "name": "Clear Outputs",
    "info": "Clear all output areas in this editor.",
    "callback": function () {
        clearOutputs(0, cm.lineCount())
    }
});

boxOutputs = {};

_messageBus.subscribe("box.output", function (d, e) {
    box = d.box;
    append = d.append
    console.log("HELLO??")
    if (cm.currentbox === box) {
        if (d.tick)
            clearOutputsOlderThan(0, cm.lineCount(), d.tick)

        console.log(" tick is "+d.tick)

        appendRemoteOutputToLine(cm.lineCount() - 1, d.message, "Field-remoteOutput-error", "Field-remoteOutput", append, d.tick)
    } else {
    }
    if (boxOutputs[box] === undefined)
        boxOutputs[box] = d.message;
    else
        boxOutputs[box] += "\n" + d.message
});

_messageBus.subscribe("box.output.clearAll", function (d, e) {
    box = d.box;
    if (cm.currentbox === box) {
        clearOutputs(0, cm.lineCount())
    }
});

_messageBus.subscribe("box.output.directed", function (d, e) {
    box = d.box;
    append = d.append

    if (cm.currentbox === box) {

        console.log(" tick is "+d.tick)

        if (d.tick)
            clearOutputsOlderThan(0, cm.lineCount(), d.tick)

        if (d.line == -1) {
            appendRemoteOutputToLine(cm.lineCount() - 1, d.message, "Field-remoteOutput-error", "Field-remoteOutput", append, d.tick)
        }
        else {
            appendRemoteOutputToLine(d.line - 1, d.message, "Field-remoteOutput-error", "Field-remoteOutput", append, d.tick)
        }
    } else {
    }
});

_messageBus.subscribe("box.error", function (d, e) {
    var box = d.box;
    var append = d.append

    if (cm.currentbox === box) {

        if (d.tick)
            clearOutputsOlderThan(0, cm.lineCount(), d.tick)

        if (d.clearAll)
            clearOutputs(0, cm.lineCount())

        if (d.line != undefined)
            appendRemoteOutputToLine(d.line - 1, d.message, "Field-remoteOutput", "Field-remoteOutput-error", false, d.tick)
        else
            appendRemoteOutputToLine(cm.lineCount() - 1, d.message, "Field-remoteOutput", "Field-remoteOutput-error", false, d.tick)


    } else {
    }
    if (boxOutputs[box] === undefined)
        boxOutputs[box] = d.message;
    else
        boxOutputs[box] += "\n" + d.message
});