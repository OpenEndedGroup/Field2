cm.on("change", function (cm, change) {
    console.log(cm)
    console.log(change)
    // debounce send to field
    if (cm.currentbox && cm.currentproperty)
        _messageBus.publish("toField.text.updated", {
            box: cm.currentbox,
            property: cm.currentproperty,
            text: cm.getValue()
        })
})

// cm.getHistory / cm.setHistory to serialize out histories

_messageBus.subscribe("focus", function (d, e) {
    cm.focus()
})

_messageBus.subscribe("selection.changed", function (d, e) {

    if (cm.currentbox) {
        cookie = {}
        cookie.brackets = serializeAllBrackets()
        cookie.output = serializeAllOutput()
        cookie.currentpos = cm.getCursor()
        _messageBus.publish("toField.store.cookie", {
            box: cm.currentbox,
            property: cm.currentproperty,
            "cookie": cookie
        })
        clearOutputs(0, cm.lineCount())
        raph.clear();
    }

    cm.currentbox = d.box;
    cm.currentproperty = d.property;
    cm.setValue(d.text);

    if (d.cookie) {
        if (d.cookie.brackets) {
		        raph.clear();
            eval(d.cookie.brackets)
            setTimeout(updateAllBrackets, 50)
        }
        if (d.cookie.output) {
            eval(d.cookie.output)
            setTimeout(updateAllBrackets, 50)
        }
        if (d.cookie.currentpos) {
            cm.setCursor(d.cookie.currentpos);
            cm.scrollIntoView();
        }
    }

    if (!d.box) {
        cm.setOption("readOnly", "nocursor");
        $(".CodeMirror").hide()
        _messageBus.publish("status", "(no selection)")

        document.title = "Field Editor (No Selection)";
    } else {
        cm.setOption("readOnly", false);
        $(".CodeMirror").show()
        cm.refresh();
        _messageBus.publish("status", "Selected '" + d.name + "'")

        document.title = d.name + "/" + d.property + " - Field Editor";

		if (d.languageName)
	        cm.setOption("mode", d.languageName);
    }

    if (d.cookie) {
        if (d.cookie.currentpos) {
            cm.scrollIntoView({line:0,ch:0}, 100);
            cm.setCursor(d.cookie.currentpos);
            cm.scrollIntoView(null, 100);
            console.log(" scrollde into view?");
        }
    }

    if (boxOutputs[d.box]) {
        appendRemoteOutputToLine(cm.lineCount() - 1, boxOutputs[d.box], "Field-remoteOutput-error", "Field-remoteOutput", false)
    }

})