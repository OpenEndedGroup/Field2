function Autocomplete() {
    __extraCompletions = [];

    _field.sendWithReturn("request.completions", {
            box: cm.currentbox,
            property: cm.currentproperty,
            text: cm.getValue(),
            line: cm.listSelections()[0].anchor.line,
            ch: cm.listSelections()[0].anchor.ch
        },
        function (d, e) {
            var completions = d;
            completionFunction = function (e) {
                var m = [];
                for (var i = 0; i < completions.length; i++) {
                    if (completions[i].replaceWith.toLowerCase().indexOf(e.toLowerCase()) > -1) {
                        pattern = new RegExp("(" + e + ")", "i");
                        matched = completions[i].replaceWith.replace(pattern, "<span class='matched'>$1</span>");
                        m.push({
                            text: matched + " " + completions[i].info,
                            callback: function () {
                                cm.replaceRange(completions[this.i].replaceWith, cm.posFromIndex(completions[this.i].start), cm.posFromIndex(completions[this.i].end))
                                _field.send("notify.completion", {uuid:completions[this.i].uuid})
                            }.bind({
                                "i": i
                            }),
                            ratio: matched.length / completions[i].length
                        })

                    }
                }

                for (var i = 0; i < __extraCompletions.length; i++) {
                    if (__extraCompletions[i][2].toLowerCase().indexOf(e.toLowerCase()) > -1) {
                        pattern = new RegExp("(" + e + ")", "i");
                        matched = __extraCompletions[i][2].replace(pattern, "<span class='matched'>$1</span>");
                        m.push({
                            text: matched + " " + __extraCompletions[i][3],
                            callback: function () {
                                cm.replaceRange(__extraCompletions[this.i][2], cm.posFromIndex(__extraCompletions[this.i][0]), cm.posFromIndex(__extraCompletions[this.i][1]))
                            }.bind({
                                "i": i
                            }),
                            ratio: matched.length / __extraCompletions[i][2].length
                        })
                    }
                }

                // m.sort(function (a, b) {
                //     return (a.ratio==b.ratio) ? 0 : (a.ratio < b.ratio ? 1 : -1)
                // });

                return m
            };

            prefix = "";

            if (completions.length > 0)
                prefix = cm.getValue().substring(completions[0].start, completions[0].end);
            else if (__extraCompletions.length > 0)
                prefix = cm.getValue().substring(__extraCompletions[0][0], __extraCompletions[0][1]);

            if (completionFunction(prefix).length == 0)
                prefix = "";

            runModalAtCursor("completion", completionFunction, prefix)
        }
    );
}

function Commands() {
    goCommands();
}

function Current_Bracket() {
    executeCurrentBracket();
    cm.setCursor(c);
}

function Hotkeys() {
    hotkeys();
}

function Import() {
    _field.sendWithReturn("request.imports", {
            box: cm.currentbox,
            property: cm.currentproperty,
            text: cm.getValue(),
            line: cm.listSelections()[0].anchor.line,
            ch: cm.listSelections()[0].anchor.ch
        },
        function (d, e) {
            var completions = d;
            completionFunction = function (e) {
                var m = [];
                for (var i = 0; i < completions.length; i++) {

                    if (completions[i].replaceWith.indexOf(e) > -1) {
                        pattern = new RegExp("(" + e + ")", "i");
                        matched = completions[i].replaceWith.replace(pattern, "<span class='matched'>$1</span>");
                        m.push({
                            text: matched + " " + completions[i].info,
                            callback: function () {
                                cm.replaceRange(completions[this.i].replaceWith, cm.posFromIndex(completions[this.i].start), cm.posFromIndex(completions[this.i].end));
                                cm.replaceRange(completions[this.i].header + "\n", {
                                    line: 0,
                                    ch: 0
                                });

                                _field.sendWithReturn("execution.fragment", {
                                    box: cm.currentbox,
                                    property: cm.currentproperty,
                                    text: completions[this.i].header,
                                    disabledRanges: "[" + allDisabledBracketRanges() + "]"
                                }, function (d, e) {
                                    if (d.type == 'error')
                                        appendRemoteOutputToLine(anchorLine, d.line + " : " + d.message, "Field-remoteOutput", "Field-remoteOutput-error", 0);
                                    else
                                        appendRemoteOutputToLine(anchorLine, d.message, "Field-remoteOutput-error", "Field-remoteOutput", 1)
                                });

                            }.bind({
                                    "i": i
                                }
                            ),
                            ratio: matched.length / completions[i].replaceWith.length

                        })
                    }
                }

                // m.sort(function (a, b) {
                //     return (a.ratio==b.ratio) ? 0 : (a.ratio < b.ratio ? 1 : -1);
                // });

                return m
            };
            if (completions.length > 0)
                runModalAtCursor("import java", completionFunction, cm.getValue().substring(completions[0].start, completions[0].end))
        }
    );
}

function Run_All() {
    fragment = cm.getValue();
    anchorLine = cm.lineCount() - 1;

    _field.sendWithReturn("execution.all", {
        box: cm.currentbox,
        property: cm.currentproperty,
        text: fragment,
        disabledRanges: "[" + allDisabledBracketRanges() + "]"
    }, function (d, e) {
        if (d.type == 'error') {
            _messageBus.publish("error.line", d);
            appendRemoteOutputToLine(anchorLine, d.line + " : " + d.message, "Field-remoteOutput", "Field-remoteOutput-error", 0)
        }
        else
            appendRemoteOutputToLine(anchorLine, d.message, "Field-remoteOutput-error", "Field-remoteOutput", 1)
    });
}

function Run_Begin() {
    fragment = cm.getValue();
    anchorLine = cm.lineCount() - 1;

    _field.sendWithReturn("execution.begin", {
        box: cm.currentbox,
        property: cm.currentproperty,
        text: fragment,
        disabledRanges: "[" + allDisabledBracketRanges() + "]"
    }, function (d, e) {
        if (d.type == 'error') {
            _messageBus.publish("error.line", d);
            appendRemoteOutputToLine(anchorLine, d.line + " : " + d.message, "Field-remoteOutput", "Field-remoteOutput-error", 0)
        }
        else
            appendRemoteOutputToLine(anchorLine, d.message, "Field-remoteOutput-error", "Field-remoteOutput", 1)
    });
}

function Run_End() {
    fragment = cm.getValue();
    anchorLine = cm.lineCount() - 1;

    _field.sendWithReturn("execution.end", {
        box: cm.currentbox,
        property: cm.currentproperty,
        text: fragment
    }, function (d, e) {
        if (d.type == 'error') {
            _messageBus.publish("error.line", d);
            appendRemoteOutputToLine(anchorLine, d.line + " : " + d.message, "Field-remoteOutput", "Field-remoteOutput-error", 0)
        }
        else
            appendRemoteOutputToLine(anchorLine, d.message, "Field-remoteOutput-error", "Field-remoteOutput", 1)
    });
}

function Run_Selection() {
    Run_Selection_variant("");
}

function Run_Selection_variant(variant) {
    anchorLine = Math.max(cm.listSelections()[0].anchor.line, cm.listSelections()[0].head.line);

    if (cm.listSelections()[0].anchor.line == cm.listSelections()[0].head.line && cm.listSelections()[0].anchor.ch == cm.listSelections()[0].head.ch) {
        fragment = cm.getLine(cm.listSelections()[0].anchor.line);

        lh1 = cm.listSelections()[0].head;
        lh2 = cm.listSelections()[0].anchor;
        var off = 0;
        if (lh1.line > lh2.line) {
            var t = lh2;
            lh2 = lh1;
            lh1 = t
        }

        if (lh2.ch == 0 && lh2.line > 0) {
            off = -1;
            //anchorLine -= 1;
        }

        var path = findPathForLines(lh1.line, lh2.line + off);
        if (!path) {
            makePathForHandles(cmGetLineHandle(lh1.line), cmGetLineHandle(lh2.line))
        } else {
            // record an execution here?
        }

    } else if (cm.listSelections()[0].anchor.line == cm.listSelections()[0].head.line) {
        lh1 = cm.listSelections()[0].head;
        fragment = cm.getSelections()[0]
    }
    else {
        fragment = cm.getSelections()[0];

        lh1 = cm.listSelections()[0].head;
        lh2 = cm.listSelections()[0].anchor;
        var off = 0;
        if (lh1.line > lh2.line) {
            var t = lh2;
            lh2 = lh1;
            lh1 = t
        }

        if (lh2.ch == 0 && lh2.line > 0) {
            off = -1;
        }

        var path = findPathForLines(lh1.line, lh2.line + off);
        if (!path) {
            makePathForHandles(cmGetLineHandle(lh1.line), (cmGetLineHandle(lh2.line + off)));
        } else {
            // record an execution here?
        }
        clearOutputs(Math.min(cm.listSelections()[0].head.line, cm.listSelections()[0].anchor.line), Math.max(cm.listSelections()[0].head.line, cm.listSelections()[0].anchor.line));
    }

    _field.sendWithReturn("execution.fragment" + variant, {
        box: cm.currentbox,
        property: cm.currentproperty,
        text: fragment,
        lineoffset: lh1.line,
        disabledRanges: "[" + allDisabledBracketRanges() + "]"
    }, function (d, e) {
        if (d.type == 'error') {
            _messageBus.publish("error.line", d);
            appendRemoteOutputToLine(anchorLine, d.line + " : " + d.message, "Field-remoteOutput", "Field-remoteOutput-error", 1)
        }
        else
            appendRemoteOutputToLine(anchorLine, d.message, "Field-remoteOutput-error", "Field-remoteOutput", 1)
    });
}

var Reindent = function () {
    if (cm.listSelections()[0].anchor.line == cm.listSelections()[0].head.line && cm.listSelections()[0].anchor.pos == cm.listSelections()[0].head.pos) {
        for (var v = 0; v < cm.lineCount(); v++) {
            cm.indentLine(v);
        }
    }
    else {
        for (var v = Math.min(cm.listSelections()[0].anchor.line, cm.listSelections()[0].head.line); v < Math.max(cm.listSelections()[0].anchor.line, cm.listSelections()[0].head.line); v++) {
            cm.indentLine(v);
        }
    }
};

globalCommands.push({
        "name": "Autocomplete",
        "info": "Shows valid completion options for current text",
        "callback": function () {
            Autocomplete()
        }
    },
    {
        "name": "Reindent",
        "info": "Reindents the selection or the whole file",
        "callback": function () {
            Reindent()
        }
    },

    {
        "name": "Commands",
        "info": "Displays a menu of command options",
        "callback": function () {
            Commands()
        }
    },

    {
        "name": "Current Bracket",
        "info": "Executes the current bracket of code",
        "callback": function () {
            Current_Bracket()
        }
    },

    {
        "name": "Hotkeys",
        "info": "Displays a menu that allows you to configure your hotkeys",
        "callback": function () {
            Hotkeys()
        }
    },

    {
        "name": "Import",
        "info": "Get Java import help",
        "callback": function () {
            Import()
        }
    },

    {
        "name": "Run All",
        "info": "Runs all code in the current box",
        "callback": function () {
            Run_All()
        }
    },

    {
        "name": "Run Begin",
        "info": "Runs all the code in the current box, and adds <code>_r</code> to the animation loop",
        "callback": function () {
            Run_Begin()
        }
    },

    {
        "name": "Run End",
        "info": "Stops animating this box",
        "callback": function () {
            Run_End()
        }
    },

    {
        "name": "Run Selection",
        "info": "Runs the currently selected code",
        "callback": function () {
            Run_Selection()
        }
    },

    {
        "name": "Run and Print Selection",
        "info": "Runs the currently selected code wrapped in a language-appropriate 'print' statement",
        "callback": function () {
            Run_Selection_variant(".print")
        }
    }
);