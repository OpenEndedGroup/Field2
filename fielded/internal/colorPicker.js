function addColorPicker() {
	var selection_start = cm.getCursor(true);
	var selection_end   = cm.getCursor(false);

	var widget_div = $('<div style="height: 100px"><div class="expand-close"><div class="Field-closebox">&#x2715;</div><div class="Field-expandBox">&#x21A7;</div></div><div class="Field-remoteColor"><form><input type="text" id="color'+selection_end.line+'" name="color" value="'+cm.getSelection()+'" /></form><div class="colorpicker" id="colorpicker'+colorpickernum+'"></div></div></div>')[0]

	console.log(cm.lineInfo(selection_end.line).widgets)
	console.log("HI")
	if (($(".Field-remoteColor").is(":visible"))){
		console.log($("#colorpicker"+selection_end.line));
		$(widget_div).show();
	} else {
		var parent = $($(".CodeMirror-code").children()[selection_end.line]).find("pre");
    console.log("Parent:" + $(parent));
		var line = cm.getLineHandle(selection_end.line)
		var widgets = line.widgets || (line.widgets = [])
		var widget = $(parent).append($(widget_div))
		widgets.push(widget)

		$(".Field-remoteColor").find('input[type=text]').focus();
		var farb = $.farbtastic($(".colorpicker"), $(".Field-remoteColor").find('input[type=text]'))

		/*$(".Field-remoteColor").focusout(function() {
			cm.replaceRange(farb.color, selection_start, selection_end);
			//(this).remove();
			console.log("Hello!");
		});*/

		$(".Field-remoteColor").keypress(function(e) {
			var code = e.keyCode || e.which
			if (code == 13) {
				e.preventDefault();
				cm.replaceRange(farb.color, selection_start, selection_end);
			}
		});

		var closeBox = $($(widget_div).children()[0])
		var expandBox = $($(widget_div).children()[1])

		if (widget) {
			expandBox.click(function () {
				if ($(".Field-remoteColor").find(".colorpicker").is(":visible")) {
					$(".Field-remoteColor").find(".colorpicker").hide();
				} else {
					$(".Field-remoteColor").find(".colorpicker").show();
				}
			})
			closeBox.click(function () {
				$(widget_div).hide()
			})
		}
	}
}

