function addColorPicker() {
		var pos = cm.getCursor()
		var li = cm.lineInfo(pos.line)
		console.log(pos)
    var d = $('<div class="'+li.textClass+'"><form><input type="text" id="color'+colorpickernum+'" name="color" value="#000000" /></form><div id="colorpicker'+colorpickernum+'"></div></div>')[0]
		bm = cm.addLineWidget(pos.line, d, { handleMouseEvents: true })
		$(document).ready(function() {
        $.farbtastic('#colorpicker'+colorpickernum, '#color'+colorpickernum);
    });
}