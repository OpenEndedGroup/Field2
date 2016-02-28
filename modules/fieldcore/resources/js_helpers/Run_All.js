fragment = cm.getValue();
anchorLine = cm.lineCount() - 1;

_field.sendWithReturn("execution.all", {
	box: cm.currentbox,
	property: cm.currentproperty,
	text: fragment,
	disabledRanges: "["+allDisabledBracketRanges()+"]"
}, function (d, e) {
	if (d.type == 'error')
		appendRemoteOutputToLine(anchorLine, d.line + " : " + d.message, "Field-remoteOutput", "Field-remoteOutput-error", 1);
	else
		appendRemoteOutputToLine(anchorLine, d.message, "Field-remoteOutput-error", "Field-remoteOutput", 1)
});