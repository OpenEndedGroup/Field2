var ignoreChange = false;


var restArgs = function(func, startIndex) {
    startIndex = startIndex == null ? func.length - 1 : +startIndex;
    return function() {
        var length = Math.max(arguments.length - startIndex, 0);
        var rest = Array(length);
        for (var index = 0; index < length; index++) {
            rest[index] = arguments[index + startIndex];
        }
        switch (startIndex) {
            case 0: return func.call(this, rest);
            case 1: return func.call(this, arguments[0], rest);
            case 2: return func.call(this, arguments[0], arguments[1], rest);
        }
        var args = Array(startIndex + 1);
        for (index = 0; index < startIndex; index++) {
            args[index] = arguments[index];
        }
        args[startIndex] = rest;
        return func.apply(this, args);
    };
};

debounce = function(func, wait, immediate) {
    var timeout, result;

    var later = function(context, args) {
        timeout = null;
        if (args) result = func.apply(context, args);
    };

    var debounced = restArgs(function(args) {
        if (timeout) clearTimeout(timeout);
        if (immediate) {
            var callNow = !timeout;
            timeout = setTimeout(later, wait);
            if (callNow) result = func.apply(this, args);
        } else {
            timeout = _.delay(later, wait, this, args);
        }

        return result;
    });

    debounced.cancel = function() {
        clearTimeout(timeout);
        timeout = null;
    };

    return debounced;
};

fireChange = debounce(function(cm)
{
    if (cm.currentbox && cm.currentproperty) {
        _messageBus.publish("toField.text.updated", {
            box: cm.currentbox,
            property: cm.currentproperty,
            text: cm.getValue(),
            ch: cm.getCursor().ch,
            line: cm.getCursor().line,
            disabledRanges: "[" + allDisabledBracketRanges() + "]"
        });

        cookie = {};
        cookie.brackets = serializeAllBrackets();
        cookie.output = serializeAllOutput();
        cookie.currentpos = cm.getCursor();
        cookie.widgets = serializeAllWidgets();

        //cookie.history = cm.getDoc().getHistory()

        _messageBus.publish("toField.store.cookie", {
            box: cm.currentbox,
            property: cm.currentproperty,
            "cookie": cookie
        })
    }

}, 250);


cm.on("change", function (cm, change) {
    if (ignoreChange) return;

    fireChange(cm);

});

_messageBus.subscribe("extra.help", function (d, e) {
    setHelpBox(d.message);
});

_messageBus.subscribe("focus", function (d, e) {
    cm.focus()
});
_messageBus.subscribe("defocus", function (d, e) {
    // cm.blur()
});

serializeAllWidgets = function () {
    return jQuery.makeArray($('*').filter(function () {
        return $(this).data('serialization') !== undefined;
    }).map(function () {
        return $(this).data("serialization")()
    }))
};

_messageBus.subscribe("selection.changed", function (d, e) {

    ignoreChange = true;

    console.log(">>Selection.changed");
    if (cm.currentbox) {
        cookie = {};
        cookie.brackets = serializeAllBrackets();
        cookie.output = serializeAllOutput();
        cookie.currentpos = cm.getCursor();
        cookie.widgets = serializeAllWidgets();
        cookie.history = cm.getDoc().getHistory();
        _messageBus.publish("toField.store.cookie", {
            box: cm.currentbox,
            property: cm.currentproperty,
            "cookie": cookie
        });
        clearOutputs(0, cm.lineCount());
        raph.clear();
    }

    cm.currentbox = d.box;
    cm.currentproperty = d.property;
    cm.setValue(d.text);

    if (d.cookie) {
        if (d.cookie.history)
            cm.getDoc().setHistory(d.cookie.history);
        else
            cm.getDoc().clearHistory();

        if (d.cookie.output) {
            try {
                eval(d.cookie.output)
            }
            catch (e) {
                console.log(e);
            }
            setTimeout(updateAllBrackets, 50)
        }
        if (d.cookie.brackets) {
            raph.clear();

            d.cookie.brackets.replace(/cm.getLineHandle/g, "cmGetLineHandle");

            try {
                eval(d.cookie.brackets)
            }
            catch (e) {
                console.log(e);
            }

            setTimeout(updateAllBrackets, 50)
        }

        if (d.cookie.currentpos) {
            cm.setCursor(d.cookie.currentpos);
            cm.scrollIntoView();
        }
    }

    if (!d.box) {
        cm.setOption("readOnly", "nocursor");
        $(".CodeMirror").hide();

        _messageBus.publish("status", "(no selection)");

        document.title = "Field Editor (No Selection)";
        setHelpBox("");

    } else {
        cm.setOption("readOnly", false);
        $(".CodeMirror").show();
        cm.refresh();
        _messageBus.publish("status", "Selected '" + d.name + "'");

        document.title = d.name + "/" + d.property + " - Field Editor";
        setHelpBox("<b>"+d.name+"</b>/"+d.property);

        if (d.languageName)
            console.log(" setting language to " + d.languageName);
        cm.setOption("mode", d.languageName);
    }

    if (d.cookie) {
        if (d.cookie.currentpos) {
            cm.scrollIntoView({line: 0, ch: 0}, 100);
            cm.setCursor(d.cookie.currentpos);
            cm.scrollIntoView(null, 100);
        }

        if (d.cookie.widgets) {

            for (var i = 0; i < d.cookie.widgets.length; i++) {
                console.log(" evaluating widget " + i + " " + d.cookie.widgets[i]);
                try {
                    eval(d.cookie.widgets[i]);
                }
                catch (e) {
                    console.log(e);
                }
            }
        }

    }

    ignoreChange = false;

});