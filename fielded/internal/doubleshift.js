_doubleshift = {
    downAt: 0
}

$(document).keydown(function (e) {
    if (e.keyCode == 16) {
        now = new Date().valueOf()
        if (now - _doubleshift.downAt < 250) {
            _messageBus.publish("toField.focus.window", {})
        }

        _doubleshift.downAt = now
    } else {
        _doubleshift.downAt = 0
    }
})