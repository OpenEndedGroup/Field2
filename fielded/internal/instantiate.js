var cm = CodeMirror(document.body)
cm.setValue("var banana = 'peach'")
cm.setOption("mode", "javascript")
cm.setOption("theme", "default")
cm.setOption("indentWithTabs", true)
cm.setOption("indentUnit", 4)
cm.setOption("tabSize", 4)
cm.setOption("lineNumbers", true)
cm.setOption("lineWrapping", true)
cm.setOption("foldGutter", true)
cm.setOption("gutters", ["CodeMirror-foldgutter", "CodeMirror-linenumbers"])

function fuzzy(pat) {
    m = pat.split(" ")
    var s = ""
    for (var i = 0; i < m.length; i++) {
        s += "(" + m[i] + ")(.*)"
    }
    pattern = new RegExp(s)
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

extraKeys = {
    "Ctrl-Enter": function (cm) {
        _field.log(cm.getSelections())
        _field.log(cm.listSelections())

        anchorLine = Math.max(cm.listSelections()[0].anchor.line, cm.listSelections()[0].head.line)

        if (cm.listSelections()[0].anchor.line == cm.listSelections()[0].head.line && cm.listSelections()[0].anchor.pos == cm.listSelections()[0].head.pos) {
            fragment = cm.getLine(cm.listSelections()[0].anchor.line)
            makePathForHandles(cm.getLineHandle(cm.listSelections()[0].anchor.line), cm.getLineHandle(cm.listSelections()[0].anchor.line))
        } else {
            fragment = cm.getSelections()[0]
            makePathForHandles(cm.getLineHandle(cm.listSelections()[0].head.line), cm.getLineHandle(cm.listSelections()[0].anchor.line))
            clearOutputs(Math.min(cm.listSelections()[0].head.line, cm.listSelections()[0].anchor.line), Math.max(cm.listSelections()[0].head.line, cm.listSelections()[0].anchor.line));
        }

        _field.sendWithReturn("execution.fragment", {
            box: cm.currentbox,
            property: cm.currentproperty,
            text: fragment
        }, function (d, e) {
            if (d.type == 'error')
                appendRemoteOutputToLine(anchorLine, d.line + " : " + d.message, "Field-remoteOutput", "Field-remoteOutput-error", 1)
            else
                appendRemoteOutputToLine(anchorLine, d.message, "Field-remoteOutput-error", "Field-remoteOutput", 1)
        });
    },
    "Ctrl-0": function (cm) {
        _field.log(cm.getSelections())
        _field.log(cm.listSelections())

        fragment = cm.getValue()
        anchorLine = cm.lineCount() - 1

        _field.sendWithReturn("execution.all", {
            box: cm.currentbox,
            property: cm.currentproperty,
            text: fragment
        }, function (d, e) {
            if (d.type == 'error')
                appendRemoteOutputToLine(anchorLine, d.line + " : " + d.message, "Field-remoteOutput", "Field-remoteOutput-error", 1)
            else
                appendRemoteOutputToLine(anchorLine, d.message, "Field-remoteOutput-error", "Field-remoteOutput", 1)
        });
    },

    "Ctrl-PageUp": function (cm) {
        _field.log(cm.getSelections())
        _field.log(cm.listSelections())

        fragment = cm.getValue()
        anchorLine = cm.lineCount() - 1

        _field.sendWithReturn("execution.begin", {
            box: cm.currentbox,
            property: cm.currentproperty,
            text: fragment
        }, function (d, e) {
            if (d.type == 'error')
                appendRemoteOutputToLine(anchorLine, d.line + " : " + d.message, "Field-remoteOutput", "Field-remoteOutput-error", 1)
            else
                appendRemoteOutputToLine(anchorLine, d.message, "Field-remoteOutput-error", "Field-remoteOutput", 1)
        });
    },
    "Ctrl-PageDown": function (cm) {
        _field.log(cm.getSelections())
        _field.log(cm.listSelections())

        fragment = cm.getValue()
        anchorLine = cm.lineCount() - 1

        _field.sendWithReturn("execution.end", {
            box: cm.currentbox,
            property: cm.currentproperty,
            text: fragment
        }, function (d, e) {
            if (d.type == 'error')
                appendRemoteOutputToLine(anchorLine, d.line + " : " + d.message, "Field-remoteOutput", "Field-remoteOutput-error", 1)
            else
                appendRemoteOutputToLine(anchorLine, d.message, "Field-remoteOutput-error", "Field-remoteOutput", 1)
        });
    },
    "Ctrl-.": function (cm) {
        _field.log(cm.getSelections())
        _field.log(cm.listSelections())

        _field.sendWithReturn("request.completions", {
                box: cm.currentbox,
                property: cm.currentproperty,
                text: cm.getValue(),
                line: cm.listSelections()[0].anchor.line,
                ch: cm.listSelections()[0].anchor.ch
            },
            function (d, e) {
                var completions = d
                completionFunction = function (e) {
                    var m = []
                    for (var i = 0; i < completions.length; i++) {
                        if (completions[i].replaceWith.contains(e)) {
                            pattern = new RegExp("(" + e + ")");
                            matched = completions[i].replaceWith.replace(pattern, "<span class='matched'>$1</span>");
                            m.push({
                                text: matched + " " + completions[i].info,
                                callback: function () {
                                    cm.replaceRange(completions[this.i].replaceWith, cm.posFromIndex(completions[this.i].start), cm.posFromIndex(completions[this.i].end))
                                }.bind({
                                    "i": i
                                })
                            })
                        }
                    }
                    return m
                }
                if (completions.length > 0)
                    runModalAtCursor("completion", completionFunction, cm.getValue().substring(completions[0].start, completions[0].end))
            }
        );
    },
    "Ctrl-Space": function (cm) {
        goCommands();
    },
    "Ctrl-I": function (cm) {
        _field.sendWithReturn("request.imports", {
                box: cm.currentbox,
                property: cm.currentproperty,
                text: cm.getValue(),
                line: cm.listSelections()[0].anchor.line,
                ch: cm.listSelections()[0].anchor.ch
            },
            function (d, e) {
                var completions = d
                completionFunction = function (e) {
                    var m = []
                    for (var i = 0; i < completions.length; i++) {
                        if (completions[i].replaceWith.contains(e)) {
                            pattern = new RegExp("(" + e + ")");
                            matched = completions[i].replaceWith.replace(pattern, "<span class='matched'>$1</span>");
                            m.push({
                                text: matched + " " + completions[i].info,
                                callback: function () {
                                    cm.replaceRange(completions[this.i].replaceWith, cm.posFromIndex(completions[this.i].start), cm.posFromIndex(completions[this.i].end))
                                    cm.replaceRange(completions[this.i].header + "\n", {
                                        line: 0,
                                        ch: 0
                                    })

                                    _field.sendWithReturn("execution.fragment", {
                                        box: cm.currentbox,
                                        property: cm.currentproperty,
                                        text: completions[this.i].header
                                    }, function (d, e) {
                                        if (d.type == 'error')
                                            appendRemoteOutputToLine(anchorLine, d.line + " : " + d.message, "Field-remoteOutput", "Field-remoteOutput-error", 1)
                                        else
                                            appendRemoteOutputToLine(anchorLine, d.message, "Field-remoteOutput-error", "Field-remoteOutput", 1)
                                    });

                                }.bind({
                                    "i": i
                                })
                            })
                        }
                    }
                    return m
                }
                if (completions.length > 0)
                    runModalAtCursor("import java", completionFunction, cm.getValue().substring(completions[0].start, completions[0].end))
            }
        );
    }
}

cm.setOption("extraKeys", extraKeys)