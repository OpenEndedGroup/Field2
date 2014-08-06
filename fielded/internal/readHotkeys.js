function addHotkey(shortcut, command) {
	extraKeys[shortcut] = function (cm) {
		command
	}
}