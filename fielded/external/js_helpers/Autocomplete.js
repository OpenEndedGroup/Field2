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
				};

				if (completions.length > 0)
						runModalAtCursor("completion", completionFunction, cm.getValue().substring(completions[0].start, completions[0].end));
				else if (__extraCompletions.length>0)
						runModalAtCursor("completion", completionFunction, cm.getValue().substring(__extraCompletions[0][0], __extraCompletions[0][1]))

		}
);