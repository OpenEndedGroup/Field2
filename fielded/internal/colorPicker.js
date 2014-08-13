function addColorPicker() {
	var selection_start = cm.getCursor(true);
	var selection_end   = cm.getCursor(false);

//	var widget_div = $('<div class="colorpicker-container"><div class="Field-remoteColor"><form><input type="text" id="color'+selection_end.line+'" name="color" value="'+cm.getSelection()+'" /></form><span class="colorpicker-warning"> !</span><div class="colorpicker" id="colorpicker'+colorpickernum+'"></div></div><div class="close-expand"><div class="Field-closebox">&#x2715;</div><div class="Field-expandBox">&#x21A7;</div></div></div>')[0]
	var widget_div = $('<div class="Field-remoteColor"><div class="Field-closebox">&#x2715;</div><form><input type="text" id="color'+selection_end.line+'" name="color" value="'+cm.getSelection()+'" /></form><span class="colorpicker-warning"> !</span><div class="colorpicker" id="colorpicker'+colorpickernum+'"></div></div>')[0]


	console.log(cm.lineInfo(selection_end.line).widgets)

	if (($(".Field-remoteColor").is(":visible"))){
		console.log($("#colorpicker"+selection_end.line));
		$(widget_div).show();
	} else {
		var widget = cm.addLineWidget(selection_end.line, widget_div)
		$(widget_div).css("float", "top");
		console.log(cm.getCursor());

		$(".Field-remoteColor").find('input[type=text]').focus();
		var farb = $.farbtastic($(".colorpicker"), $(".Field-remoteColor").find('input[type=text]'))

		$(".Field-remoteColor").keypress(function(e) {
			var code = e.keyCode || e.which
			if (code == 13) {
				e.preventDefault();
				cm.replaceRange(farb.color, selection_start, selection_end);
			}
		});

		$(".Field-remoteColor").click(function() {
			$(".Field-remoteColor").children()[0].focus()
			console.log($(".Field-remoteColor").children()[0].focus())
		});

		var closeBox = $($(widget_div).find(".Field-closeBox"))
		var expandBox = $($(widget_div).find(".Field-expandBox"))

		if (widget) {
			expandBox.click(function () {
				if ($(".Field-remoteColor").find(".colorpicker").is(":visible")) {
					$(".Field-remoteColor").find(".colorpicker").hide();
				} else {
					$(".Field-remoteColor").find(".colorpicker").show();
				}
			})
			closeBox.click(function () {
				widget.clear();
				$(widget_div).remove()
			})
		}
	}
}

