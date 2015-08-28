_field.sendWithReturn("request.imports", {
                box: cm.currentbox,
                property: cm.currentproperty,
                text: cm.getValue(),
                line: cm.listSelections()[0].anchor.line,
                ch: cm.listSelections()[0].anchor.ch
            },
            function (d, e) {
                var completions = d
                anchorLine = cm.lineCount() - 1

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