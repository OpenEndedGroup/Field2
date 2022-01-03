package field.graphics;

import fieldagent.Main;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVidMode;

public class FullScreenWindow extends RenderWindow {

	/**
	 * opens a full screen window on `screen`
	 * @param screen
	 */
	public FullScreenWindow(int screen) {
		super(screen);
	}

	/**
	 * opens a full screen window on `screen` with `title`
	 * @param screen
	 */
	public FullScreenWindow(int screen, String title) {
		super(screen, title);
	}

	/**
	 * opens a window that, despite the name, is at x,y and has dimensions w,h
	 */
	public FullScreenWindow(int x, int y, int w, int h, String title) {
		super(x, y, w, h, title);
	}


}
