_doubleshift = {
    downAt: 0,
    escDownAt: 0
};

$(document).keydown(function (e) {
		if (e.keyCode==67 && e.metaKey && !e.shiftKey && !e.altKey && cm.getSelection()=="")
		{
	    _field.send("clipboard.setClipboard", {value: window.getSelection().toString()});
		}
		else
    if (e.keyCode == 16) {
        now = new Date().valueOf();
        if (now - _doubleshift.downAt < 250) {
            _messageBus.publish("toField.focus.window", {})
        }

        _doubleshift.downAt = now;
        _doubleshift.escDownAt = 0
    } else if (e.keyCode == 27) {
        now = new Date().valueOf();
        if (now - _doubleshift.escDownAt < 250) {

            $.grep($('div'), function (a, n) {
                return a.lastOutputAt && !a.dontRemove;
            }).sort(function (a, b) {
                return b.lastOutputAt - a.lastOutputAt;
            })[0].bm.clear();
            updateAllBrackets();
        }

        _doubleshift.escDownAt = now;
        _doubleshift.downAt = 0
    } else {
        _doubleshift.downAt = 0;
        _doubleshift.escDownAt = 0
    }
});