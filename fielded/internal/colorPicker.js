function addColorPicker() {
	var pos = cm.getCursor()
	var li = cm.lineInfo(pos.line)
	var d = $('<div class="rm_color"><form><input type="text" id="color'+colorpickernum+'" name="color" value="#000000" /></form><div class="colorpicker" id="colorpicker'+colorpickernum+'"></div></div>')[0]
	bm = cm.addLineWidget(pos.line, d, { })
	$(".rm_color").click(function() {
  	var placeholder = $(this).find('.colorpicker');
  	var input = $(this).find('input[type=text]');
  	$.farbtastic(placeholder,input);
  });
}