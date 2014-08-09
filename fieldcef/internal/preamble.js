// adapts the CEF environment to something that looks a lot like our websocket environment
// given this, we ought to be able to run our in-browser environment completely without websockets
_field.log = function(s) {
    _field.send("log", s);
}

_field.error = function(s) {
    _field.send("error", encodeURIComponent(s));
}

_field.send = function(address, obj) {
    window.cefQuery({
        request: JSON.stringify({
            address: address,
            payload: obj,
            from: _field.id
        }),
        persistent: false,
        onSuccess: function(r) {},
        error: function(e, em) {
            console.log("ERROR?", e, em);
        }
    });
}

// there is no onmessage, because we can eval directly into the browser
//_field.socket.onmessage

// stubs for functions that we expect to have in 'cm' -- for now we are focused on getting the commands system up and running
var cm = {
	focus: function(){}
}