package field.graphics;

/**
 * Created by marc on 2/21/16.
 */
public class GlfwCallback {
	public void error(int error, String description) {
	}

	public void windowFocus(long window, boolean focused) {
	}

	public void windowRefresh(long window) {
	}

	public void mouseButton(long window, int button, int pressed, int mods) {
	}

	public void scroll(long window, double scrollX, double scrollY) {
	}


	public void cursorPos(long window, double x, double y) {
	}

	public void key(long window, int key, int scancode, int action, int mods) {
	}

	public void character(long window, int character) {
	}

	public void drop(long window, String[] files) {
	}


	public boolean windowClose(long window) {
		return false;
	}

	public void windowPos(long window, int x, int y) {
	}

	public void windowSize(long window, int w, int h) {
	}

	public void framebufferSize(long window, int w, int h) {
	}

	public void monitor(long l, boolean b) {
	}

	public void windowIconify(long l, boolean b) {
	}

	public void cursorEnter(long l, boolean b) {

	}
}
