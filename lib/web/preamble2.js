// now that window.cefQuery has stopped working reliably this file, which was trying to avoid running
// a websocket to each of our browser instances, is going to do just that.

_field = {};

_field.log = function (s) {
    _field.send("log", s);
};

_field.error = function (s) {
    _field.send("error", s);
};

var wsAt = "ws://localhost:///WSPORT///"
_field.socket = new WebSocket(wsAt);
_field.socket.binaryType = "arraybuffer";


_field.socket.onclose = function (e) {
    console.error(" -- socket has been closed --", e, wsAt)
};

_field.socket.onerror = function (e) {
    console.error(" -- socket has errored --", e, wsAt)
}

_field.send = function (address, obj) {
    console.error(" _field.send  is about to send something " + address + " " + obj)
    // window.cefQuery({
    // 	 request: JSON.stringify({
    // 			 address: address,
    // 			 payload: obj,
    // 			 from: _field.id
    // 	 }),
    // 	 persistent: false,
    // 	 onSuccess: function(r) {},
    // 	 error: function(e, em) {
    // 			 console.log("ERROR?", e, em);
    // 	 }
    // });

    _field.socket.send(JSON.stringify({
            address: address,
            payload: obj,
            from: _field.id
        })
    )
};

function generateUUID() {
    var d = new Date().getTime();
    var uuid = 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
        var r = (d + Math.random() * 16) % 16 | 0;
        d = Math.floor(d / 16);
        return (c == 'x' ? r : (r & 0x7 | 0x8)).toString(16);
    });
    return uuid;
}


_field.sendWithReturn = function (address, payload, returnFunc) {
    _field.sendWithExplicitReturn(address, payload, generateUUID(), 1000 * 60, returnFunc)
};

var __callback = null

_field.sendWithExplicitReturn = function (address, payload, returnAddress, ttl, returnFunc) {
    __callback = returnFunc

    _field.send(address, payload)
};

// _field.sendWithReturn = function (address, obj, callback) {
//     console.error(" _field.send  is about to send something " + address + " " + obj)
//     window.cefQuery({
//         request: JSON.stringify({
//             address: address,
//             payload: obj,
//             from: _field.id
//         }),
//         persistent: false,
//         onSuccess: function (r) {
//             callback(JSON.parse(r), "");
//         },
//         error: function (e, em) {
//             console.log("ERROR?", e, em);
//         }
//     });
// };

// there is no onmessage, because we can eval directly into the browser
//_field.socket.onmessage

// stubs for functions that we expect to have in 'cm' -- for now we are focused on getting the commands system up and running
var cm = {
    focus: function () {
        _field.send("focus", {});
    },
    lostFocus: function () {
        _field.send("lostFocus", {});
    }
};

function fuzzy(pat) {
    m = pat.split(" ");
    var s = "";
    for (var i = 0; i < m.length; i++) {
        s += "(" + m[i] + ")([^\<\>]*)"
    }
    pattern = new RegExp(s, "i");
    return pattern;
}

stripTags = function (n) {
    a = new RegExp("<[^>]*>", "ig")
    return n.replace(a)
}

function replacer() {
    prefix = arguments[arguments.length - 1].substring(0, arguments[arguments.length - 1]);
    for (var i = 1; i < arguments.length - 2; i += 2) {
        prefix += "<span class='matched'>" + arguments[i] + "</span>" + arguments[i + 1];
    }
    return prefix;
}


goCommands = function () {
    console.error(" -- preamble goCommands is running --")
    _field.sendWithReturn("request.commands", {},
        function (d, e) {
            console.error(" -- preamble goCommands callback is running --")
            var completions = [];
            for (var i = 0; i < d.length; i++) {
                d[i].callback = function () {
                    _field.send("call.command", {
                        command: this.call
                    });
                }.bind({
                    "call": d[i].call
                });
                d[i].callback.remote = 1;
                completions.push(d[i])
            }
            // completions.sort(function(a, b) {
            // 	 return a.name < b.name ? -1 : 1;
            // });

            completionFunction = function (e) {
                var m = [];

                var fuzzyPattern = fuzzy(e);

                for (var i = 0; i < completions.length; i++) {
                    if (stripTags(completions[i].name).search(fuzzyPattern) != -1) {
                        matched = completions[i].name.replace(fuzzyPattern, replacer);
                        m.push({
                            text: matched + " <span class=doc>" + completions[i].info + "</span>",
                            callback: function () {
                                completions[this.i].callback()
                            }.bind({
                                "i": i
                            })
                        })
                    } else if (stripTags(completions[i].info).search(fuzzyPattern) != -1) {
                        matched = completions[i].name;
                        m.push({
                            text: matched + " <span class=doc>" + completions[i].info.replace(fuzzyPattern, replacer) + "</span>",
                            callback: function () {
                                completions[this.i].callback()
                            }.bind({
                                "i": i
                            })
                        })
                    }
                }
                return m
            };
            console.error(" -- preamble goCommands completions is about to show  --", completions)

            if (completions.length > 0)
                runModal("Commands... [esc exits]", completionFunction, "Field-Modal", undefined, false, function () {
                    console.log(" BLUR: lost focus from box")
                    cm.lostFocus();
                })
        }
    );
    console.error(" -- preamble goCommands has finished running, having set a handler request to Field? --", _field)
};

continueCommands = function (d) {

    var completions = [];
    for (var i = 0; i < d.commands.length; i++) {
        d.commands[i].callback = function () {
            _field.send("call.command", {
                command: this.call
            });
        }.bind({
            "call": d.commands[i].call
        });
        d.commands[i].callback.remote = 1;
        completions.push(d.commands[i])
    }
    // completions.sort(function (a, b) {
    // 	 return a.name < b.name ? -1 : 1;
    // });

    completionFunction = function (e) {
        var m = [];

        var fuzzyPattern = fuzzy(e);

        for (var i = 0; i < completions.length; i++) {
            if (stripTags(completions[i].name).search(fuzzyPattern) != -1) {
                matched = completions[i].name.replace(fuzzyPattern, replacer);
                m.push({
                    text: matched + " <span class=doc>" + completions[i].info + "</span>",
                    callback: function () {
                        completions[this.i].callback()
                    }.bind({
                        "i": i
                    })
                })
            }
        }
        return m
    };

//		 console.log("alternative is " + d.alternative)

    if (d.alternative) {
//				 console.log(" going with modal ");
        runModal(d.prompt, completionFunction, "Field-Modal", "", function (t) {
            _field.send("call.alternative", {
                command: this.call,
                "text": t
            })
        }.bind({
            "call": d.alternative
        }))
    } else if (completions.length > 0)
        runModal(d.prompt, completionFunction, "Field-Modal", "")

};