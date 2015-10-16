globalCommands.push({
    "name": "Insert default tap here",
    "info": "Inserts a tap (a function in a box that refers to this position in the text)",
    "callback": function () {
        insertTapHere("default", 30, 30);
    }
});

globalCommands.push({
    "name": "Insert execution marker here",
    "info": "Inserts an execution marker here (a function in a box that refers to this position in the text",
    "callback": function () {
        insertTapHere("exec", 30, 30);
    }
});

insertTap = function (selection_start, selection_end, uid, w, h) {

    canvas = newMarkingCanvas(selection_start, selection_end, w, h);
    var fragment = cm.getDoc().getRange(selection_start, selection_end);

    // execute this immediately to make the tap (and potentially draw into this canvas)
    _field.sendWithReturn("execution.fragment", {
        box: cm.currentbox,
        property: cm.currentproperty,
        text: fragment,
        lineoffset: -1
    }, function (d, e) {

    });

    var marks = canvas.mark;

    $(canvas.div).data("serialization", function () {
        fc = marks.find().from.ch
        tc = marks.find().to.ch
        fl = marks.find().from.line
        tl = marks.find().to.line

        // todo, add guard to make sure that this text hasn't changed....
        return "insertTap({'line':" + fl + ",'ch':" + fc + "},{'line':" + tl + ",'ch':" + tc + "},'" + uid + "'," + w + "," + h + ")";
    })

    $(canvas.div).data("canvasID", uid);

}

canvasForID = function (name) {
    q = $('*').filter(function () {
        return $(this).data('canvasID') == name;
    })[0];
    if (q) return q.canvas;
    return null;
}

insertTapHere = function (ty, w, h) {
    var selection_start = cm.getCursor("from");
    var uid = ty + ":" + generateUUID();
    var fragment = "_.tap('" + uid + "')";
    cm.replaceRange(" " + fragment + " ", selection_start) // we pad with spaces, otherwise when we replace this text we blow away our widget as well
    var selection_end = cm.getCursor("to");
    tap = insertTap(selection_start, selection_end, uid, w, h);
    return tap;
}

cm.on("change", function (x, c) {

    setTimeout(function () {

        var all = $('*').filter(function () {
            return $(this).data('canvasID') != null;
        });

        var namePos = []

        for (var i = 0; i < all.size(); i++) {
            var n = $(all[i]).data('canvasID');
            namePos.push({name: n, x: $(all[i]).offset().left, y: $(all[i]).offset().top, w: $(all[i]).width(), h: $(all[i]).height()});
        }

        var activeSet = {active: namePos};

        _messageBus.publish("toField.taps.activeset", activeSet);
    }, 100)

})