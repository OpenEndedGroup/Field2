/*
    code for surrounding text in hidden code and handling callbacks on editing the the parts that remain unhidden
eg. 203.4 becomes _.intervention("type:uid", 203.4) and editing 203.4 results in messages like:
_messageBus.publish("toField.interventions.changed", {
     box: cm.currentbox,
     property: cm.currentproperty,
     uid: uid,
     textNow: 205.4
});

and

_messageBus.publish("toField.interventions.list", {
     box: cm.currentbox,
     property: cm.currentproperty,
     uids: [uid ...]
});

for a complete list of interventions that are still in a box
// note, that the '_.intervention("type:uid",' and the ')' can't be completely invisible.
They have to be rendered as a '|' or we wont be able to delete them properly (since we won't know that they are there)

 */

globalCommands.push({
    "name": "Insert interpolation here",
    "info": "Inserts an interpolation marker here (a function in a box that refers to this position in the text",
    "callback": function () {
        insertInterventionHere("default", "" + generateUUID());
    }
});

window.insertInterventionHere = function (name, uid, from, to) {

    console.log(" insert intervention here " + name + " " + uid + " " + from + " " + to)

    var c = from ? from : cm.getCursor()
    var h = cm.getTokenAt(c)

    if (to) {
        h = {start: from.ch, end: to.ch}
        console.log(" explict position given " + h)
    }
    else {

    }

    var left = "_.intervention('" + name + ":" + uid + "', "
    var right = ")"

    var current = cm.getRange({line: c.line, ch: h.start}, {line: c.line, ch: h.end})
    var currentExpanded = cm.getRange({line: c.line, ch: h.start - left.length}, {
        line: c.line,
        ch: h.end + right.length
    })

    if (currentExpanded.startsWith(left) && currentExpanded.endsWith(right)) {
        console.log(" already there ")
        h = {start: h.start - left.length, end: h.end + right.length}
    }
    else {
        console.log(" wrapper missing ")
        console.log(" compared :'" + currentExpanded + "' with '" + left + "'")
        cm.replaceRange(left + current + right, {line: c.line, ch: h.start}, {line: c.line, ch: h.end})
    }

    var divStart = $('<span class="Field-interventionHiddenLeft">&#9668;</span>')[0];
    var divEnd = $('<span class="Field-interventionHiddenRight">&#9658;</span>')[0];

    var ll = cm.markText({line: c.line, ch: h.start}, {
        line: c.line,
        ch: h.start + left.length
    }, {replacedWith: divStart, atomic: true})


    // goes unstyled, since its really fragile

    var marks = cm.markText({line: c.line, ch: h.start + left.length}, {
        line: c.line,
        ch: h.start + left.length + current.length
    }, {
        className: "Field-interventionMiddleXX",
        collapsed: false,
        atomic: false,
        inclusiveLeft: true,
        inclusiveRight: true
    })


    var rr = cm.markText({line: c.line, ch: h.start + left.length + current.length}, {
        line: c.line,
        ch: h.start + left.length + current.length + right.length
    }, {replacedWith: divEnd, atomic: true})

    $(divStart).data("serialization", function () {

        try {
            var fc = ll.find().to.ch;
            var fl = ll.find().to.line;
            var tc = rr.find().from.ch;
            var tl = rr.find().from.line;

            // todo, add guard to make sure that this text hasn't changed....
            return "insertInterventionHere('" + name + "', '" + uid + "', {line:" + fl + ", ch:" + (fc ) + "}, {line:" + tl + ", ch:" + tc + "})";
        }
        catch (e) {
            console.log("\n\n******* serialization of intervention failed, marks.find() returned " + marks.find() + "********\n\n");
            console.log(e)
        }
    })

    $(divStart).data("intervention_name", name)
    $(divStart).data("intervention_uid", uid)
    $(divStart).data("intervention_marks", [ll, marks, rr])
    $(divStart).data("intervention_current", current)

    _messageBus.publish("toField.interventions.changed", {
        box: cm.currentbox,
        property: cm.currentproperty,
        uid: uid,
        name: name,
        textNow: current,
        textWas: current,
        newlyAdded: true
    });

}

cm.on("change", function () {

    var uidsInPlay = jQuery.makeArray($('*').filter(function () {
        return $(this).data('intervention_marks') !== undefined;
    }).map(function () {
        var textWas = $(this).data("intervention_current")
        var left = $(this).data("intervention_marks")[0]
        var right = $(this).data("intervention_marks")[2]
        var uid = $(this).data("intervention_uid")
        var name = $(this).data("intervention_name")

        if (!left.find() || !right.find()) {
            console.log(" marker for intervention lost?")
            return null;
        }
        else {
            var textIs = cm.getRange(left.find().to, right.find().from)

            if (textWas != textIs) {
                _messageBus.publish("toField.interventions.changed", {
                    box: cm.currentbox,
                    property: cm.currentproperty,
                    uid: uid,
                    name: name,
                    textNow: textIs,
                    textWas: textWas,
                    newlyAdded: false
                });

                $(this).data("intervention_current", textIs)
            }
            else {
                console.log(" marker stayed same " + textWas + " " + textIs)
            }
            return $(this).data("intervention_uid")
        }
    }))

    _messageBus.publish("toField.interventions.inplay", {
        box: cm.currentbox,
        property: cm.currentproperty,
        uids: uidsInPlay
    });

})

window.setIntervention = function (uid, textTo) {
    var uidsInPlay = jQuery.makeArray($('*').filter(function () {
        return $(this).data('intervention_marks') !== undefined;
    }).map(function () {
        var u = $(this).data("intervention_uid")
        if (u == uid) {
            $(this).data("intervention_current", textTo)
            var left = $(this).data("intervention_marks")[0]
            var right = $(this).data("intervention_marks")[2]

            if (!left.find())
                console.log(" lost left marker for intervention, can't keyframe any more")
            if (!right.find())
                console.log(" lost right marker for intervention, can't keyframe any more")

            if (left.find() && right.find())
                cm.replaceRange(textTo, left.find().to, right.find().from)
        }
    }))
}
