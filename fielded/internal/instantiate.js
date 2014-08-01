var cm = CodeMirror(document.body)
cm.setValue("")
cm.setOption("mode", "javascript")
cm.setOption("theme", "default")
cm.setOption("indentWithTabs", true)
cm.setOption("indentUnit", 4)
cm.setOption("tabSize", 4)
cm.setOption("lineNumbers", true)
cm.setOption("lineWrapping", true)
cm.setOption("foldGutter", true)
cm.setOption("gutters", ["CodeMirror-foldgutter", "CodeMirror-linenumbers"])
cm.setOption("matchBrackets", true)
cm.setOption("closeBrackets", true)

function fuzzy(pat) {
    m = pat.split(" ")
    var s = ""
    for (var i = 0; i < m.length; i++) {
        s += "(" + m[i] + ")(.*)"
    }
    pattern = new RegExp(s, "i")
    return pattern
}

function replacer() {
    console.log(arguments)
    prefix = arguments[arguments.length - 1].substring(0, arguments[arguments.length - 1])
    for (var i = 1; i < arguments.length - 2; i += 2) {
        prefix += "<span class='matched'>" + arguments[i] + "</span>" + arguments[i + 1];
    }
    return prefix;
}

globalCommands = []

var __extraCompletions = [] // set by BridgedTernSupport



goCommands = function () {
    _field.sendWithReturn("request.commands", {
            box: cm.currentbox,
            property: cm.currentproperty,
            text: cm.getValue(),
            line: cm.listSelections()[0].anchor.line,
            ch: cm.listSelections()[0].anchor.ch
        },
        function (d, e) {
            var completions = []
            for (var i = 0; i < d.length; i++) {
                d[i].callback = function () {
                    _field.send("call.command", {
                        command: this.call
                    });
                }.bind({
                    "call": d[i].call
                })
                d[i].callback.remote = 1
                completions.push(d[i])
            }
            completions = completions.concat(globalCommands)
            completions.sort(function (a, b) {
                return a.name < b.name ? -1 : 1;
            })

            completionFunction = function (e) {
                var m = []

                var fuzzyPattern = fuzzy(e);

                for (var i = 0; i < completions.length; i++) {
                    if (completions[i].name.search(fuzzyPattern) != -1) {
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
            }
            if (completions.length > 0)
                runModal("Commands...", completionFunction, "Field-Modal")
        }
    );
}

testCommand = function () {
    _field.sendWithReturn("request.hotkeyCommands", {
            box: cm.currentbox,
            property: cm.currentproperty,
            text: cm.getValue(),
            line: cm.listSelections()[0].anchor.line,
            ch: cm.listSelections()[0].anchor.ch,
            allJSCommands: {"Autocomplete": "Documentation for Autocomplete", "Commands": "Documentation for Commands", "Current Bracket": "Documentation for Current Bracket", "Hotkeys": "Documentation for Hotkeys", "Import": "Documentation for Import", "Run All": "Documentation for Run All", "Run Begin": "Documentation for Run Begin", "Run End": "Documentation for Run End", "Run Selection": "Documentation for Run Selection"}
        },
        function (d, e) {
						var completions = []
						for (var i = 0; i < d.length; i++) {
								d[i].callback = function () {
										_field.send("call.command", {
												command: this.call
										});
								}.bind({
										"call": d[i].call
								})
								d[i].callback.remote = 1
								completions.push(d[i])
						}
						completions.sort(function (a, b) {
								return a.name < b.name ? -1 : 1;
						})

						completionFunction = function (e) {
								var m = []

								var fuzzyPattern = fuzzy(e);

								for (var i = 0; i < completions.length; i++) {
										if (completions[i].name.search(fuzzyPattern) != -1) {
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
						}
						if (completions.length > 0)
								runModal("Set Hotkeys", completionFunction, "Field-Modal")
				}
    );
}

_messageBus.subscribe("begin.commands", function (d, e) {

    var completions = []
    for (var i = 0; i < d.commands.length; i++) {
        d.commands[i].callback = function () {
            _field.send("call.command", {
                command: this.call
            });
        }.bind({
            "call": d.commands[i].call
        })
        d.commands[i].callback.remote = 1
        completions.push(d.commands[i])
    }
    completions.sort(function (a, b) {
        return a.name < b.name ? -1 : 1;
    })

    completionFunction = function (e) {
        var m = []

        var fuzzyPattern = fuzzy(e);

        for (var i = 0; i < completions.length; i++) {
            if (completions[i].name.search(fuzzyPattern) != -1) {
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
    }

    console.log("alternative is " + d.alternative)

    if (d.alternative) {
        console.log(" going with modal ");
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

});

// list of Chrome default hotkeys to override and set to null function. To add more, simply add strings to this array.
overrides = ["Ctrl-H", "Shift-Ctrl-O", "Ctrl-W", "Ctrl-J", "Ctrl-N", "Shift-Ctrl-N", "Ctrl-P", "Ctrl-T", "Shift-Ctrl-T"];

extraKeys = {
// should this be alt-Left on Linux?
    "Ctrl-Left": function (cm) {
    		$.getScript("/field/filesystem/js_helpers/Current_Bracket.js");
    },
    "Ctrl-Enter": function (cm) {
    		$.getScript("/field/filesystem/js_helpers/Run_Selection.js");
    },
    "Ctrl-0": function (cm) {
    		$.getScript("/field/filesystem/js_helpers/Run_All.js");
    },
    "Ctrl-PageDown": function (cm) {
    		$.getScript("/field/filesystem/js_helpers/Run_End.js");
    },
    "Ctrl-PageUp": function (cm) {
        $.getScript("/field/filesystem/js_helpers/Run_Begin.js");
    },
    "Ctrl-.": function (cm) {
    		$.getScript("/field/filesystem/js_helpers/Autocomplete.js");
    },
    "Ctrl-Space": function (cm) {
        $.getScript("/field/filesystem/js_helpers/Commands.js");
    },

    "Ctrl-/": function(cm) {
        $.getScript("/field/filesystem/js_helpers/Hotkeys.js");
    },
    "Ctrl-I": function (cm) {
        $.getScript("/field/filesystem/js_helpers/Import.js");
    }
}

// iterate through overrides and set to no functionality
for (i = 0; i < overrides.length; i++) {
		extraKeys[overrides[i]] = function (cm) {};
}

cm.setOption("extraKeys", extraKeys)