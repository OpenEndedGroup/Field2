package trace.scratch;

import field.CanonicalModifierKeys;
import field.app.RunLoop;
import field.app.ThreadSync2;
import field.graphics.GlfwCallback;
import field.graphics.Windows;
import field.utility.Dict;
import field.utility.Options;
import fieldagent.Main;
import fieldcef.browser.CefSystem;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL43;
import org.lwjgl.opengl.GLUtil;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFW.GLFW_STICKY_MOUSE_BUTTONS;

public class Scratch implements Runnable {


	private long window;

	@Override
	public void run() {

		// experimenting with moving this initialization first. Seems to remove the occasional crash on startup?
//		new Thread(() -> {
//			System.err.println(" building the CefSystem");
			CefSystem sys = CefSystem.cefSystem;
//			System.err.println(" finished building the CefSystem");
//		}).start();

		glfwInit();

		glfwWindowHint(GLFW_DEPTH_BITS, 24);
		glfwWindowHint(GLFW_COCOA_GRAPHICS_SWITCHING, 1);
		glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
		glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, 1);
		glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
		glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 2);
		glfwWindowHint(GLFW_DOUBLEBUFFER, 1);
		glfwWindowHint(GLFW_DECORATED, 1);
		window = glfwCreateWindow(500, 500, "Hello?", 0, 0);

		glfwSetWindowPos(window, 50, 50);

		glfwSetCursorPosCallback(window, new GLFWCursorPosCallback(){
			@Override
			public void invoke(long window, double xpos, double ypos) {
				System.out.println(" cursor post :"+window+" "+xpos+" "+ypos);
			}
		});
		glfwShowWindow(window);

		glfwMakeContextCurrent(window);
		/*glfwSwapInterval(0);

		GL11.glClearColor(0.25f, 0.25f, 0.25f, 1);
		GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

		glfwSwapBuffers(window);

		glfwSetInputMode(window, GLFW_STICKY_MOUSE_BUTTONS, GL11.GL_TRUE);
		*/

		GL.createCapabilities();

		// Set the clear color
		GL11.glClearColor(1.0f, 0.0f, 0.0f, 0.0f);

		// Run the rendering loop until the user has attempted to close
		// the window or has pressed the ESCAPE key.
		while ( !glfwWindowShouldClose(window) ) {
			GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT); // clear the framebuffer

			glfwSwapBuffers(window); // swap the color buffers

			// Poll for window events. The key callback above will only be
			// invoked during this call.
			glfwPollEvents();
		}
	}
}
