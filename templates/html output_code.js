_.menu.clear();

// set up some marking menus to pause, suspend and renable this box
pause = function()
{
	_.pauseNow();
	_.menu.remove("Pause_n2");
	_.menu.remove("Suspend_n2");
	_.menu.Enable_n2 = enable
};
suspend = function()
{
	_.pauseAndIgnoreNow();
	_.menu.remove("Pause_n2");
	_.menu.remove("Suspend_n2");
	_.menu.Enable_n2 = enable
};
enable = function()
{
	_.unpauseNow();
	_.menu.remove("Enable_n2");
	_.menu.Pause_n2 = pause;
	_.menu.Suspend_n2 = suspend
};

_.menu.Pause_n2 = pause;
_.menu.Suspend_n2 = suspend;
