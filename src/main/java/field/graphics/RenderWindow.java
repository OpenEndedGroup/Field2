package field.graphics;

import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVidMode;

public class RenderWindow extends Window {

	static int[][] bounds;

	static {
		PointerBuffer monitors = GLFW.glfwGetMonitors();
		bounds = new int[monitors.limit()][4];
		for (int i = 0; i < monitors.limit(); i++) {
			int[] w = {0};
			int[] h = {0};

			GLFWVidMode mode = GLFW.glfwGetVideoMode(monitors.get(i));
			bounds[i][2] = mode.width();
			bounds[i][3] = mode.height();

			GLFW.glfwGetMonitorPos(monitors.get(i), w, h);
			bounds[i][0] = w[0];
			bounds[i][1] = h[0];
			System.out.println(" screen :"+i+" "+bounds[i][0]+" "+bounds[i][1]+" "+bounds[i][2]+" "+bounds[i][3]);
		}
	}

	public RenderWindow(int x, int y, int w, int h, String title) {
		super(x, y, w, h, title, true);
	}

	public RenderWindow(int x, int y, int w, int h) {
		super(x, y, w, h, null, true);
	}

	public RenderWindow(int screen) {
		super(bounds[screen%bounds.length][0], bounds[screen%bounds.length][1], bounds[screen%bounds.length][2], bounds[screen%bounds.length][3], null, true);
	}

	public RenderWindow(int screen, String title) {
		super(bounds[screen%bounds.length][0], bounds[screen%bounds.length][1], bounds[screen%bounds.length][2], bounds[screen%bounds.length][3], title, true);
	}


}
