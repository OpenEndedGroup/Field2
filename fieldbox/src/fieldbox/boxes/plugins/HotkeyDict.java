package fieldbox.boxes.plugins;

//This class will wrap a dictionary that hardcodes in the js functions as strings and maps them to a shorter string.
//This makes our coding life easier as we can pass around short strings for a while and only at the end use the whole
//function


import java.util.LinkedHashMap;

public class HotkeyDict{
	//This is the bulk of this class. It is the map that is being wrapped
	private LinkedHashMap<String, String> functions;

	//This adds a command to the dictionary, it takes in the key and value
	public void addCommand(final String key, final String value){
		functions.put(key, value);
	}

	//this "deletes" a command by setting the command value to null
	public void deleteCommand(final String key){
		functions.put(key, null);
	}

	//this is the constructor. It starts off the dictionary in the default state
	public HotkeyDict(){
		functions.put("cmd1","function (cm) {\n" +
			    "        if (currentBracket != null) {\n" +
			    "\n" +
			    "            currentBracket.attr({\n" +
			    "                fill: \"#afc\"\n" +
			    "            }).animate({\n" +
			    "                fill: \"#fff\"\n" +
			    "            }, 500)\n" +
			    "\n" +
			    "            anchorLine = Math.max(cm.getLineNumber(currentBracket.h1), cm.getLineNumber(currentBracket.h2) + 1)\n" +
			    "\n" +
			    "            c = cm.getCursor()\n" +
			    "            cm.setSelection({\n" +
			    "                line: cm.getLineNumber(currentBracket.h1),\n" +
			    "                ch: 0\n" +
			    "            }, {\n" +
			    "                line: cm.getLineNumber(currentBracket.h2) + 1,\n" +
			    "                ch: 0\n" +
			    "            })\n" +
			    "\n" +
			    "            fragment = cm.getSelections()[0]\n" +
			    "\n" +
			    "            _field.sendWithReturn(\"execution.fragment\", {\n" +
			    "                box: cm.currentbox,\n" +
			    "                property: cm.currentproperty,\n" +
			    "                text: fragment\n" +
			    "            }, function (d, e) {\n" +
			    "                if (d.type == 'error')\n" +
			    "                    appendRemoteOutputToLine(anchorLine, d.line + \" : \" + d.message, \"Field-remoteOutput\", \"Field-remoteOutput-error\", 1)\n" +
			    "                else\n" +
			    "                    appendRemoteOutputToLine(anchorLine, d.message, \"Field-remoteOutput-error\", \"Field-remoteOutput\", 1)\n" +
			    "            });\n" +
			    "        }\n" +
			    "        cm.setCursor(c);\n" +
			    "    }");
		functions.put("cmd2","function (cm) {\n" +
			    "        _field.log(cm.getSelections())\n" +
			    "        _field.log(cm.listSelections())\n" +
			    "\n" +
			    "        anchorLine = Math.max(cm.listSelections()[0].anchor.line, cm.listSelections()[0].head.line)\n" +
			    "\n" +
			    "        if (cm.listSelections()[0].anchor.line == cm.listSelections()[0].head.line && cm.listSelections()[0].anchor.pos == cm.listSelections()[0].head.pos) {\n" +
			    "            fragment = cm.getLine(cm.listSelections()[0].anchor.line)\n" +
			    "\n" +
			    "            lh1 = cm.listSelections()[0].head\n" +
			    "            lh2 = cm.listSelections()[0].anchor\n" +
			    "            var off = 0;\n" +
			    "            if (lh1.line > lh2.line) {\n" +
			    "                var t = lh2\n" +
			    "                lh2 = lh1\n" +
			    "                lh1 = t\n" +
			    "            }\n" +
			    "\n" +
			    "            if (lh2.ch == 0 && lh2.line > 0) {\n" +
			    "                off = -1;\n" +
			    "            }\n" +
			    "\n" +
			    "\n" +
			    "            var path = findPathForLines(lh1.line, lh2.line + off)\n" +
			    "            if (!path) {\n" +
			    "                makePathForHandles(cm.getLineHandle(lh1.line), cm.getLineHandle(lh2.line))\n" +
			    "            } else {\n" +
			    "                // record an execution here?\n" +
			    "            }\n" +
			    "\n" +
			    "        } else {\n" +
			    "            fragment = cm.getSelections()[0]\n" +
			    "\n" +
			    "            lh1 = cm.listSelections()[0].head\n" +
			    "            lh2 = cm.listSelections()[0].anchor\n" +
			    "            var off = 0;\n" +
			    "            if (lh1.line > lh2.line) {\n" +
			    "                var t = lh2\n" +
			    "                lh2 = lh1\n" +
			    "                lh1 = t\n" +
			    "            }\n" +
			    "\n" +
			    "            if (lh2.ch == 0 && lh2.line > 0) {\n" +
			    "                off = -1;\n" +
			    "            }\n" +
			    "\n" +
			    "\n" +
			    "            var path = findPathForLines(lh1.line, lh2.line + off)\n" +
			    "            if (!path) {\n" +
			    "                makePathForHandles(cm.getLineHandle(lh1.line), cm.getLineHandle(lh2.line + off))\n" +
			    "            } else {\n" +
			    "                // record an execution here?\n" +
			    "            }\n" +
			    "            clearOutputs(Math.min(cm.listSelections()[0].head.line, cm.listSelections()[0].anchor.line), Math.max(cm.listSelections()[0].head.line, cm.listSelections()[0].anchor.line));\n" +
			    "        }\n" +
			    "\n" +
			    "        _field.sendWithReturn(\"execution.fragment\", {\n" +
			    "            box: cm.currentbox,\n" +
			    "            property: cm.currentproperty,\n" +
			    "            text: fragment\n" +
			    "        }, function (d, e) {\n" +
			    "            if (d.type == 'error')\n" +
			    "                appendRemoteOutputToLine(anchorLine, d.line + \" : \" + d.message, \"Field-remoteOutput\", \"Field-remoteOutput-error\", 1)\n" +
			    "            else\n" +
			    "                appendRemoteOutputToLine(anchorLine, d.message, \"Field-remoteOutput-error\", \"Field-remoteOutput\", 1)\n" +
			    "        });\n" +
			    "    }");
		functions.put("cmd3","function (cm) {\n" +
			    "        _field.log(cm.getSelections())\n" +
			    "        _field.log(cm.listSelections())\n" +
			    "\n" +
			    "        fragment = cm.getValue()\n" +
			    "        anchorLine = cm.lineCount() - 1\n" +
			    "\n" +
			    "        _field.sendWithReturn(\"execution.all\", {\n" +
			    "            box: cm.currentbox,\n" +
			    "            property: cm.currentproperty,\n" +
			    "            text: fragment\n" +
			    "        }, function (d, e) {\n" +
			    "            if (d.type == 'error')\n" +
			    "                appendRemoteOutputToLine(anchorLine, d.line + \" : \" + d.message, \"Field-remoteOutput\", \"Field-remoteOutput-error\", 1)\n" +
			    "            else\n" +
			    "                appendRemoteOutputToLine(anchorLine, d.message, \"Field-remoteOutput-error\", \"Field-remoteOutput\", 1)\n" +
			    "        });\n" +
			    "    }");
		functions.put("cmd4","function (cm) {\n" +
			    "        _field.log(cm.getSelections())\n" +
			    "        _field.log(cm.listSelections())\n" +
			    "\n" +
			    "        fragment = cm.getValue()\n" +
			    "        anchorLine = cm.lineCount() - 1\n" +
			    "\n" +
			    "        _field.sendWithReturn(\"execution.begin\", {\n" +
			    "            box: cm.currentbox,\n" +
			    "            property: cm.currentproperty,\n" +
			    "            text: fragment\n" +
			    "        }, function (d, e) {\n" +
			    "            if (d.type == 'error')\n" +
			    "                appendRemoteOutputToLine(anchorLine, d.line + \" : \" + d.message, \"Field-remoteOutput\", \"Field-remoteOutput-error\", 1)\n" +
			    "            else\n" +
			    "                appendRemoteOutputToLine(anchorLine, d.message, \"Field-remoteOutput-error\", \"Field-remoteOutput\", 1)\n" +
			    "        });\n" +
			    "    }");
		functions.put("cmd5","function (cm) {\n" +
			    "        _field.log(cm.getSelections())\n" +
			    "        _field.log(cm.listSelections())\n" +
			    "\n" +
			    "        fragment = cm.getValue()\n" +
			    "        anchorLine = cm.lineCount() - 1\n" +
			    "\n" +
			    "        _field.sendWithReturn(\"execution.end\", {\n" +
			    "            box: cm.currentbox,\n" +
			    "            property: cm.currentproperty,\n" +
			    "            text: fragment\n" +
			    "        }, function (d, e) {\n" +
			    "            if (d.type == 'error')\n" +
			    "                appendRemoteOutputToLine(anchorLine, d.line + \" : \" + d.message, \"Field-remoteOutput\", \"Field-remoteOutput-error\", 1)\n" +
			    "            else\n" +
			    "                appendRemoteOutputToLine(anchorLine, d.message, \"Field-remoteOutput-error\", \"Field-remoteOutput\", 1)\n" +
			    "        });\n" +
			    "    }");
		functions.put("cmd6","function (cm) {\n" +
			    "        _field.log(cm.getSelections())\n" +
			    "        _field.log(cm.listSelections())\n" +
			    "\n" +
			    "        _field.sendWithReturn(\"request.completions\", {\n" +
			    "                box: cm.currentbox,\n" +
			    "                property: cm.currentproperty,\n" +
			    "                text: cm.getValue(),\n" +
			    "                line: cm.listSelections()[0].anchor.line,\n" +
			    "                ch: cm.listSelections()[0].anchor.ch\n" +
			    "            },\n" +
			    "            function (d, e) {\n" +
			    "                var completions = d\n" +
			    "                completionFunction = function (e) {\n" +
			    "                    var m = []\n" +
			    "                    for (var i = 0; i < completions.length; i++) {\n" +
			    "                        if (completions[i].replaceWith.contains(e)) {\n" +
			    "                            pattern = new RegExp(\"(\" + e + \")\");\n" +
			    "                            matched = completions[i].replaceWith.replace(pattern, \"<span class='matched'>$1</span>\");\n" +
			    "                            m.push({\n" +
			    "                                text: matched + \" \" + completions[i].info,\n" +
			    "                                callback: function () {\n" +
			    "                                    cm.replaceRange(completions[this.i].replaceWith, cm.posFromIndex(completions[this.i].start), cm.posFromIndex(completions[this.i].end))\n" +
			    "                                }.bind({\n" +
			    "                                    \"i\": i\n" +
			    "                                })\n" +
			    "                            })\n" +
			    "                        }\n" +
			    "                    }\n" +
			    "                    return m\n" +
			    "                }\n" +
			    "                if (completions.length > 0)\n" +
			    "                    runModalAtCursor(\"completion\", completionFunction, cm.getValue().substring(completions[0].start, completions[0].end))\n" +
			    "            }\n" +
			    "        );\n" +
			    "    }");
		functions.put("cmd7","function (cm) {\n" +
			    "        goCommands();\n" +
			    "    }");

		functions.put("cmd8","function (cm) {\n" +
			    "        _field.sendWithReturn(\"request.imports\", {\n" +
			    "                box: cm.currentbox,\n" +
			    "                property: cm.currentproperty,\n" +
			    "                text: cm.getValue(),\n" +
			    "                line: cm.listSelections()[0].anchor.line,\n" +
			    "                ch: cm.listSelections()[0].anchor.ch\n" +
			    "            },\n" +
			    "            function (d, e) {\n" +
			    "                var completions = d\n" +
			    "                completionFunction = function (e) {\n" +
			    "                    var m = []\n" +
			    "                    for (var i = 0; i < completions.length; i++) {\n" +
			    "                        if (completions[i].replaceWith.contains(e)) {\n" +
			    "                            pattern = new RegExp(\"(\" + e + \")\");\n" +
			    "                            matched = completions[i].replaceWith.replace(pattern, \"<span class='matched'>$1</span>\");\n" +
			    "                            m.push({\n" +
			    "                                text: matched + \" \" + completions[i].info,\n" +
			    "                                callback: function () {\n" +
			    "                                    cm.replaceRange(completions[this.i].replaceWith, cm.posFromIndex(completions[this.i].start), cm.posFromIndex(completions[this.i].end))\n" +
			    "                                    cm.replaceRange(completions[this.i].header + \"\\n\", {\n" +
			    "                                        line: 0,\n" +
			    "                                        ch: 0\n" +
			    "                                    })\n" +
			    "\n" +
			    "                                    _field.sendWithReturn(\"execution.fragment\", {\n" +
			    "                                        box: cm.currentbox,\n" +
			    "                                        property: cm.currentproperty,\n" +
			    "                                        text: completions[this.i].header\n" +
			    "                                    }, function (d, e) {\n" +
			    "                                        if (d.type == 'error')\n" +
			    "                                            appendRemoteOutputToLine(anchorLine, d.line + \" : \" + d.message, \"Field-remoteOutput\", \"Field-remoteOutput-error\", 1)\n" +
			    "                                        else\n" +
			    "                                            appendRemoteOutputToLine(anchorLine, d.message, \"Field-remoteOutput-error\", \"Field-remoteOutput\", 1)\n" +
			    "                                    });\n" +
			    "\n" +
			    "                                }.bind({\n" +
			    "                                    \"i\": i\n" +
			    "                                })\n" +
			    "                            })\n" +
			    "                        }\n" +
			    "                    }\n" +
			    "                    return m\n" +
			    "                }\n" +
			    "                if (completions.length > 0)\n" +
			    "                    runModalAtCursor(\"import java\", completionFunction, cm.getValue().substring(completions[0].start, completions[0].end))\n" +
			    "            }\n" +
			    "        );\n" +
			    "    }");

	}
}
