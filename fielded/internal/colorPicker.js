function addColorPicker() {
	var selection_start = cm.getCursor(true);
	var selection_end   = cm.getCursor(false);

	var widget_div = $('<div class="change_color"><form><input type="text" id="color'+colorpickernum+'" name="color" value="'+cm.getSelection()+'" /></form><div class="colorpicker" id="colorpicker'+colorpickernum+'"></div></div>')[0]
	var widget          = cm.addWidget(selection_start, widget_div);

	$(".change_color").find('input[type=text]').focus();
	var farb = $.farbtastic($(".colorpicker"), $(".change_color").find('input[type=text]'))

	$(".change_color").focusout(function() {
		cm.replaceRange(farb.color, selection_start, selection_end);
		(this).remove();
		console.log("Hello!");
	});
}