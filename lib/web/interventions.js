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
        insertInterventionHere("default", ""+generateUUID());
    }
});

insertInterventionHere= function(name, uid)
{
    var c = cm.getCursor()
    var h = cm.getTokenAt(c)


    var left = "_.intervention('"+name+":"+uid+"', "
    var right = ")"

    var current = cm.getRange({line:c.line, ch:h.start}, {line:c.line, ch:h.end})
    cm.replaceRange(left+current+right, {line:c.line, ch:h.start}, {line:c.line, ch:h.end})


    console.log(" replaced range ")
    cm.markText({line:c.line, ch:h.start}, {line:c.line, ch:h.start+left.length}, {className:"Field-interventionHiddenLeft", collapsed:true, atomic:true})
    cm.markText({line:c.line, ch:h.start+left.length}, {line:c.line, ch:h.start+left.length+current.length}, {className:"Field-interventionMiddle", collapsed:false, atomic:false})
    cm.markText({line:c.line, ch:h.start+left.length+current.length}, {line:c.line, ch:h.start+left.length+current.length+right.length}, {className:"Field-interventionHiddenRight", collapsed:true, atomic:true})

}