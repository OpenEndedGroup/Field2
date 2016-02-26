fragment = cm.getValue();
anchorLine = cm.lineCount() - 1;

_field.sendWithReturn("execution.begin", {
	box: cm.currentbox,
	property: cm.currentproperty,
	text: fragment
}, function (d, e) {
	if (d.type == 'error')
		appendRemoteOutputToLine(anchorLine, d.line + " : " + d.message, "Field-remoteOutput", "Field-remoteOutput-error", 1);
	else
		appendRemoteOutputToLine(anchorLine, d.message, "Field-remoteOutput-error", "Field-remoteOutput", 1)
});