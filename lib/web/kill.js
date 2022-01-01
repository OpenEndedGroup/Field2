// CEF has problems with access the native clipboard. So here we'll duplicate some of the emacs mode kill ring functionality, dress it up like
// command-c,v,x, and sync it with the native clipboard via our websocket
var killRing = [];

function addToRing(str) {
    killRing.push(str);
    if (killRing.length > 50) killRing.shift();
}

function growRingTop(str) {
    if (!killRing.length) return addToRing(str);
    killRing[killRing.length - 1] += str;
}

function getFromRing(n) {
    return killRing[killRing.length - (n ? Math.min(n, 1) : 1)] || "";
}

function popFromRing() {
    if (killRing.length > 1) killRing.pop();
    return getFromRing();
}

var lastKill = null;

function kill(cm, from, to, mayGrow, text) {
    if (text == null) text = cm.getRange(from, to);

    if (mayGrow && lastKill && lastKill.cm == cm && posEq(from, lastKill.pos) && cm.isClean(lastKill.gen))
        growRingTop(text);
    else
        addToRing(text);
    cm.replaceRange("", from, to, "+delete");

    if (mayGrow) lastKill = {
        cm: cm,
        pos: from,
        gen: cm.changeGeneration()
    };
    else lastKill = null;
}

function copy() {
	console.log("COPY");
    _field.send("clipboard.setClipboard", {value: cm.getSelection()});
    addToRing(cm.getSelection())
}

function cut() {
    _field.send("clipboard.setClipboard", {value: cm.getSelection()});
    kill(cm, cm.getCursor("start"), cm.getCursor("end"));
}

function getNewClipboard(contin) {
    _field.sendWithReturn("clipboard.getNewClipboard", {}, contin);
}

function paste() {
    _field.log("about to paste");
    var newText = getNewClipboard(function (newText) {
        _field.log("in continuation");
        if (newText != null)
            addToRing(newText);

        var start = cm.getCursor();
        cm.replaceRange(getFromRing(0), cm.getCursor("start"), cm.getCursor("end"), "paste");
        cm.setSelection(cm.getCursor("end"), cm.getCursor("end"));
    })
}

function pasteAgain() {
    cm.replaceSelection(popFromRing(), "around", "paste");
}