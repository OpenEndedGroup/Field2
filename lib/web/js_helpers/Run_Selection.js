anchorLine = Math.max(cm.listSelections()[0].anchor.line, cm.listSelections()[0].head.line);

if (cm.listSelections()[0].anchor.line == cm.listSelections()[0].head.line && cm.listSelections()[0].anchor.pos == cm.listSelections()[0].head.pos) {

    fragment = cm.getLine(cm.listSelections()[0].anchor.line);

    lh1 = cm.listSelections()[0].head;
    lh2 = cm.listSelections()[0].anchor;
    var off = 0;
    if (lh1.line > lh2.line) {
        var t = lh2;
        lh2 = lh1;
        lh1 = t
    }

    if (lh2.ch == 0 && lh2.line > 0) {
        off = -1;
    }

    var path = findPathForLines(lh1.line, lh2.line + off);
    if (!path) {
        makePathForHandles(cm.getLineHandle(lh1.line), cm.getLineHandle(lh2.line))
    } else {
        // record an execution here?
    }

}
else if (cm.listSelections()[0].anchor.line == cm.listSelections()[0].head.line) {
    fragment = cm.getSelections()[0];
    lh1 = cm.listSelections()[0].head
}
else {
    fragment = cm.getSelections()[0];

    lh1 = cm.listSelections()[0].head;
    lh2 = cm.listSelections()[0].anchor;
    var off = 0;
    if (lh1.line > lh2.line) {
        var t = lh2;
        lh2 = lh1;
        lh1 = t
    }

    if (lh2.ch == 0 && lh2.line > 0) {
        off = -1;
    }

    var path = findPathForLines(lh1.line, lh2.line + off);
    if (!path) {
        makePathForHandles(cm.getLineHandle(lh1.line), cm.getLineHandle(lh2.line + off))
    } else {
        // record an execution here?
    }
    clearOutputs(Math.min(cm.listSelections()[0].head.line, cm.listSelections()[0].anchor.line), Math.max(cm.listSelections()[0].head.line, cm.listSelections()[0].anchor.line));
}

_field.sendWithReturn("execution.fragment", {
    box: cm.currentbox,
    property: cm.currentproperty,
    text: fragment
}, function (d, e) {
    if (d.type == 'error')
        appendRemoteOutputToLine(anchorLine, "<b class='errorlinenumber'>"+d.line + "</b>" + d.message, "Field-remoteOutput", "Field-remoteOutput-error", 1);
    else
        appendRemoteOutputToLine(anchorLine, d.message, "Field-remoteOutput-error", "Field-remoteOutput", 1)
});