var ignoreChange = false;

cm.on("change", function (cm, change) {
	if (ignoreChange) return;

	console.log(">>change");

	if (cm.currentbox && cm.currentproperty) {
		_messageBus.publish("toField.text.updated", {
			box: cm.currentbox,
			property: cm.currentproperty,
			text: cm.getValue(),
			disabledRanges: "[" + allDisabledBracketRanges() + "]"
		})

		cookie = {}
		cookie.brackets = serializeAllBrackets()
		cookie.output = serializeAllOutput()
		cookie.currentpos = cm.getCursor()
		cookie.widgets = serializeAllWidgets()

		//cookie.history = cm.getDoc().getHistory()

		_messageBus.publish("toField.store.cookie", {
			box: cm.currentbox,
			property: cm.currentproperty,
			"cookie": cookie
		})
	}
	console.log("<<change (" + cookie.widgets + "\n||"+cm.getValue()+"||");

})

_messageBus.subscribe("focus", function (d, e) {
	cm.focus()
})
_messageBus.subscribe("defocus", function (d, e) {
	cm.blur()
})

serializeAllWidgets = function () {
	return jQuery.makeArray($('*').filter(function () {
		return $(this).data('serialization') !== undefined;
	}).map(function () {
		return $(this).data("serialization")()
	}))
}

_messageBus.subscribe("selection.changed", function (d, e) {

	ignoreChange = true;

	console.log(">>Selection.changed");
	if (cm.currentbox) {
		cookie = {}
		cookie.brackets = serializeAllBrackets()
		cookie.output = serializeAllOutput()
		cookie.currentpos = cm.getCursor()
		cookie.widgets = serializeAllWidgets()
		cookie.history = cm.getDoc().getHistory()
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
		if (d.cookie.history)
			cm.getDoc().setHistory(d.cookie.history);
		else
			cm.getDoc().clearHistory();

		if (d.cookie.output) {
			try {
				eval(d.cookie.output)
			}
			catch(e)
			{
				console.log(e);
			}
			setTimeout(updateAllBrackets, 50)
		}
		if (d.cookie.brackets) {
			raph.clear();

			d.cookie.brackets.replace(/cm.getLineHandle/g, "cmGetLineHandle");

			try{
				eval(d.cookie.brackets)
			}
			catch(e)
			{
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
				try{
				eval(d.cookie.widgets[i]);
				}
				catch(e)
				{
					console.log(e);
				}
			}
		}

	}

	// need to be able to delete these outputs for good

	//if (boxOutputs[d.box]) {
	//	appendRemoteOutputToLine(cm.lineCount() - 1, boxOutputs[d.box], "Field-remoteOutput-error", "Field-remoteOutput", false)
	//}

	console.log("<<Selection.changed");
	ignoreChange = false;

})