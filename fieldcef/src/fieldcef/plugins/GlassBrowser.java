package fieldcef.plugins;

import field.utility.Rect;
import fieldbox.boxes.Box;
import fieldbox.boxes.Drawing;
import fieldbox.boxes.FLineDrawing;
import fieldcef.browser.Browser;

/**
 * This is a browser, created by default, that covers the whole window in the glass layer. We can then either center it in the window, or crop into it.
 */
public class GlassBrowser extends Box {

	// we'll need to make sure that this is centered on larger screens
	int maxw = 2560;
	int maxh = 1600;

	public GlassBrowser(Box root)
	{
	}

	public void loaded()
	{
		Browser browser = new Browser();
		browser.properties.put(Box.frame, new Rect(0,0, maxw, maxh));
		browser.properties.put(FLineDrawing.layer, "glass");
		browser.properties.put(Drawing.windowSpace, true);
		browser.connect(this);
		browser.loaded();
	}
}
