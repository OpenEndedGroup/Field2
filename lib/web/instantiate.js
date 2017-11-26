var cm = CodeMirror(document.body);
cm.setValue("");
cm.setOption("mode", "javascript");
cm.setOption("theme", "default");
cm.setOption("indentWithTabs", true);
cm.setOption("indentUnit", 4);
cm.setOption("tabSize", 4);
cm.setOption("lineNumbers", true);
cm.setOption("lineWrapping", true);
cm.setOption("foldGutter", true);
cm.setOption("gutters", ["CodeMirror-foldgutter", "CodeMirror-lint-markers", "CodeMirror-linenumbers"]);
cm.setOption("matchBrackets", true);
cm.setOption("autoCloseBrackets", true);
cm.setOption("cursorBlinkRate", 0);
cm.setOption("lint", {options: {asi: true, moz: true, esversion: 6}});

colorpickernum = 0;

function fuzzy(pat) {
    m = pat.split(" ");
    var s = "";
    for (var i = 0; i < m.length; i++) {
        s += "(" + m[i] + ")(.*)"
    }
    pattern = new RegExp(s, "i");
    return pattern;
}

function replacer() {
    prefix = arguments[arguments.length - 1].substring(0, arguments[arguments.length - 1]);
    for (var i = 1; i < arguments.length - 2; i += 2) {
        prefix += "<span class='matched'>" + arguments[i] + "</span>" + arguments[i + 1];
    }
    return prefix;
}

globalCommands = [];

var __extraCompletions = []; // set by BridgedTernSupport

goCommands = function () {

    _field.sendWithReturn("request.commands", {
            box: cm.currentbox,
            property: cm.currentproperty,
            text: cm.getValue(),
            line: cm.listSelections()[0].anchor.line,
            ch: cm.listSelections()[0].anchor.ch
        },
        function (d, e) {
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

            for (var i = 0; i < globalCommands.length; i++) {
                if (globalCommands[i].guard == undefined || globalCommands[i].guard())
                    completions.push(globalCommands[i]);
            }

            completionFunction = function (e) {
                var m = [];

                var fuzzyPattern = fuzzy(e);

                for (var i = 0; i < completions.length; i++) {
                    if (completions[i].name.search(fuzzyPattern) != -1) {
                        matched = completions[i].name.replace(fuzzyPattern, replacer);
                        m.push({
                            text: matched + " <span class=doc>" + completions[i].info + "</span>",
                            callback: function () {
                                completions[this.i].callback()
                            }.bind({"i": i})
                        })
                    }
                }
                return m
            };
            if (completions.length > 0)
                runModal("Commands...", completionFunction, "Field-Modal")
        }
    );
};

hotkeys = function () {
    JSCommands = {};

    for (var i = 0; i < globalCommands.length; i++) {
        var currCommand = globalCommands[i];
        //This regex trims the function () { and } from the beginning and end of the call, reducing it to just the command in the body
        JSCommands[currCommand.name] = [currCommaFFnd.info, currCommand.callback.toString().replace(/[^{]*{|}[^}]*$/g, "").trim()]
    }

    _field.sendWithReturn("request.hotkeyCommands", {
            box: cm.currentbox,
            property: cm.currentproperty,
            text: cm.getValue(),
            line: cm.listSelections()[0].anchor.line,
            ch: cm.listSelections()[0].anchor.ch,
            allJSCommands: JSCommands
        },
        function (d, e) {
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
            // completions.sort(function (a, b) {
            //     return a.name < b.name ? -1 : 1;
            // });

            completionFunction = function (e) {
                var m = [];

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
            };
            if (completions.length > 0)
                runModal("Set Hotkeys", completionFunction, "Field-Modal")
        }
    );
};

_messageBus.subscribe("begin.commands", function (d, e) {

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
    //     return a.name < b.name ? -1 : 1;
    // });

    completionFunction = function (e) {
        var m = [];

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
    };

    if (d.alternative) {
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

cmdKey = (navigator.appVersion.indexOf("Mac") != -1) ? "Cmd-" : "Ctrl-";

extraKeys = {
    "Alt-Left": function (cm) {
        Current_Bracket();
    },
    "Shift-Alt-Left": function (cm) {
        executeCurrentBracketToHere();
    },
    "Alt-Enter": function (cm) {
        Run_Selection();
    },
    "Shift-Alt-Enter": function (cm) {
        Run_Selection_variant(".print");
    },
    "Alt-0": function (cm) {
        Run_All();
    },
    "Alt-Down": function (cm) {
        Run_End();
    },
    "Alt-Up": function (cm) {
        Run_Begin();
    },
    "Ctrl-.": function (cm) {
        Autocomplete();
    },
    "Ctrl-Space": function (cm) {
        Commands();
    },
    "Ctrl-/": function (cm) {
        Hotkeys();
    },
    "Ctrl-I": function (cm) {
        Import();
    },
    "Ctrl-R": function (cm) {
        Reindent();
    },
    "Ctrl-C": function (cm) {
        copy();
    },
    "Ctrl-X": function (cm) {
        cut();
    },
    "Ctrl-V": function (cm) {
        paste();
    },
    "Cmd-C": function (cm) {
        copy();
    },
    "Cmd-X": function (cm) {
        cut();
    },
    "Cmd-V": function (cm) {
        paste();
    },
    "Ctrl-F":function (cm) {
        console.log("about to findPersistent")
        cm.execCommand("findPersistent")
    },
    "Cmd-A": function (cm) {
        cm.execCommand("selectAll")
    },
    "Ctrl-A": function (cm) {
        cm.execCommand("selectAll")
    },
    "Alt-V": function (cm) {
        pasteAgain();
    }
};

// iterate through overrides and set to no functionality
for (i = 0; i < overrides.length; i++) {
    extraKeys[overrides[i]] = function (cm) {
    };
}

cm.setOption("extraKeys", extraKeys);

function performCommand(nameOfCommand) {
    _field.send("call.commandByName", {
        command: nameOfCommand,
        rebuild: true
    });
}

cm.setOption("keymap", "sublime");
