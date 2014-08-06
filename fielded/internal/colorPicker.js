function addColorPicker() {
		var pos = cm.doc.getCursor()
		var li = cm.lineInfo(pos.line)
		console.log(pos)
    var d = $('<div class="'+li.textClass+'"><form><input type="text" id="color'+colorpickernum+'" name="color" value="#123456" /></form><div id="colorpicker'+colorpickernum+'"></div></div>')[0]
		bm = cm.addWidget(pos, d, false)
		$(document).ready(function() {
        $('#colorpicker'+colorpickernum).farbtastic('#color'+colorpickernum);
    });
}