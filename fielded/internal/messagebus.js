var _messageBus = postal.channel("main")

postal.addWireTap(function (d, e) {
    console.log(d);
    console.log(JSON.stringify(e, null, 4));
});

_messageBus.subscribe("toField.#", function (d, e) {
    _field.send(e.topic.replace("toField.", ""), d)
})

_messageBus.subscribe("toField_debounce.#", function (d, e) {
    _field.send(e.topic.replace("toField_debounce.", ""), d)
}).withDebounce(500)

function generateUUID() {
    var d = new Date().getTime();
    var uuid = 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
        var r = (d + Math.random() * 16) % 16 | 0;
        d = Math.floor(d / 16);
        return (c == 'x' ? r : (r & 0x7 | 0x8)).toString(16);
    });
    return uuid;
};

_messageBus_ttl = {}
_messageBus_forever = {}

function messageBusTTLChecker() {
    for (var key in _messageBus_ttl) {
        if (_messageBus_ttl.hasOwnProperty(key)) {
            if (new Date().valueOf() - _messageBus_ttl[key][0] > 0 && !(key in _messageBus_forever)) {
                _messageBus_ttl[key][1].unsubscribe()
                delete _messageBus_ttl[key]
            } else {
//                _field.log(" channel has longer to live");
            }
        }
    }

    timer = setTimeout(arguments.callee, 20000)
}

setTimeout(messageBusTTLChecker, 2000)

_field.sendWithReturn = function (address, payload, returnFunc) {
    _field.sendWithExplicitReturn(address, payload, generateUUID(), 1000 * 60, returnFunc)
}

_field.sendWithExplicitReturn = function (address, payload, returnAddress, ttl, returnFunc) {
    ret = returnAddress

    payload.returnAddress = ret
    sub = _messageBus.subscribe(ret, function (d, e) {
        returnFunc(d, e)
    })
    _messageBus_ttl[ret] = [new Date().valueOf() + ttl, sub]

    _field.send(address, payload)
}