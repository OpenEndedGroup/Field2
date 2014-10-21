// CEF has problems with access the native clipboard. So here we'll duplicate some of the emacs mode kill ring functionality, dress it up like command-c,v,x, and, later on, sync it with the native clipboard via our websocket
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
    addToRing(cm.getSelection())
}

function cut() {
    kill(cm, cm.getCursor("start"), cm.getCursor("end"));
}

function paste() {
    var start = cm.getCursor();
    cm.replaceRange(getFromRing(0), start, start, "paste");
    cm.setSelection(start, cm.getCursor());
}

function pasteAgain() {
    cm.replaceSelection(popFromRing(), "around", "paste");
}