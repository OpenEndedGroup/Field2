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




extraKeys = {
    "Ctrl-Left": function (cm) {
        if (currentBracket != null) {

            currentBracket.attr({
                fill: "#afc"
            }).animate({
                fill: "#fff"
            }, 500)

            anchorLine = Math.max(cm.getLineNumber(currentBracket.h1), cm.getLineNumber(currentBracket.h2) + 1)

            c = cm.getCursor()
            cm.setSelection({
                line: cm.getLineNumber(currentBracket.h1),
                ch: 0
            }, {
                line: cm.getLineNumber(currentBracket.h2) + 1,
                ch: 0
            })

            fragment = cm.getSelections()[0]

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
        }
        cm.setCursor(c);
    },
    "Ctrl-Enter": function (cm) {

        anchorLine = Math.max(cm.listSelections()[0].anchor.line, cm.listSelections()[0].head.line)

        if (cm.listSelections()[0].anchor.line == cm.listSelections()[0].head.line && cm.listSelections()[0].anchor.pos == cm.listSelections()[0].head.pos) {
            fragment = cm.getLine(cm.listSelections()[0].anchor.line)

            lh1 = cm.listSelections()[0].head
            lh2 = cm.listSelections()[0].anchor
            var off = 0;
            if (lh1.line > lh2.line) {
                var t = lh2
                lh2 = lh1
                lh1 = t
            }

            if (lh2.ch == 0 && lh2.line > 0) {
                off = -1;
            }


            var path = findPathForLines(lh1.line, lh2.line + off)
            if (!path) {
                makePathForHandles(cm.getLineHandle(lh1.line), cm.getLineHandle(lh2.line))
            } else {
                // record an execution here?
            }

        } else {
            fragment = cm.getSelections()[0]

            lh1 = cm.listSelections()[0].head
            lh2 = cm.listSelections()[0].anchor
            var off = 0;
            if (lh1.line > lh2.line) {
                var t = lh2
                lh2 = lh1
                lh1 = t
            }

            if (lh2.ch == 0 && lh2.line > 0) {
                off = -1;
            }


            var path = findPathForLines(lh1.line, lh2.line + off)
            if (!path) {
                makePathForHandles(cm.getLineHandle(lh1.line), cm.getLineHandle(lh2.line + off))
            } else {
                // record an execution here?
            }
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
        __extraCompletions = []

        _field.sendWithReturn("request.completions", {
                box: cm.currentbox,
                property: cm.currentproperty,
                text: cm.getValue(),
                line: cm.listSelections()[0].anchor.line,
                ch: cm.listSelections()[0].anchor.ch
            },
            function (d, e) {

            	console.log(" -- about to go completion --", __extraCompletions, completions);

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

					for (var i = 0; i < __extraCompletions.length; i++) {
                        if (__extraCompletions[i][2].contains(e)) {
                            pattern = new RegExp("(" + e + ")");
                            matched = __extraCompletions[i][2].replace(pattern, "<span class='matched'>$1</span>");
                            m.push({
                                text: matched + " " + __extraCompletions[i][3],
                                callback: function () {
                                    cm.replaceRange(__extraCompletions[this.i][2], cm.posFromIndex(__extraCompletions[this.i][0]), cm.posFromIndex(__extraCompletions[this.i][1]))
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
                else if (__extraCompletions.length>0)
                    runModalAtCursor("completion", completionFunction, cm.getValue().substring(__extraCompletions[0][0], __extraCompletions[0][1]))

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