package field.graphics;

import fieldagent.Main;

/**
 * will contain logic for going fullscreen automatically, right now, it doesn't
 */
public class FullScreenWindow extends Window {

	public FullScreenWindow(int x, int y, int w, int h, String title) {
		super(x, y, w - (Main.os == Main.OS.mac ? 1 : 0), h, title, true);

		if (Main.os==Main.OS.mac)
		{
			setBounds(x, y, w, h);
		}
	}

}
