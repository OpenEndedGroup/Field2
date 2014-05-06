package field.graphics;

import com.badlogic.jglfw.GlfwCallback;
import com.badlogic.jglfw.GlfwCallbackAdapter;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.badlogic.jglfw.Glfw.GLFW_RELEASE;
import static com.badlogic.jglfw.Glfw.glfwInit;
import static com.badlogic.jglfw.Glfw.glfwSetCallback;

/**
 * Created by marc on 4/14/14.
 */
public class Windows {

	static public Windows windows = new Windows();

	private Windows()
	{
		glfwInit();
		glfwSetCallback(makeCallback());
	}

	public void init() {
		// this is already done once in the singleton constructor
	}


	Map<Long, GlfwCallback> adaptors = new LinkedHashMap<>();

	public void register(long window, GlfwCallback adaptor)
	{
		adaptors.put(window, adaptor);
	}


	protected GlfwCallback makeCallback() {
		return new GlfwCallbackAdapter() {

			@Override
			public void error(int error, String description) {
				System.err.println(" ERROR in GLFW windowing system :" + error + " / " + description);
				new Exception().printStackTrace();
			}

			@Override
			public void windowRefresh(long window) {
				GlfwCallback a = adaptors.get(window);
				if (a != null) a.windowRefresh(window);
			}

			@Override
			public void mouseButton(long window, int button, boolean pressed, int mods) {
				GlfwCallback a = adaptors.get(window);
				if (a != null) a.mouseButton(window, button, pressed, mods);
			}


			@Override
			public void scroll(long window, double scrollX, double scrollY) {
				GlfwCallback a = adaptors.get(window);
				if (a != null) a.scroll(window, scrollX, scrollY);
			}

			@Override
			public void cursorPos(long window, double x, double y) {
				GlfwCallback a = adaptors.get(window);
				if (a != null) a.cursorPos(window, x, y);
			}

			@Override
			public void key(long window, int key, int scancode, int action, int mods) {
				GlfwCallback a = adaptors.get(window);
				if (a != null) a.key(window, key, scancode, action, mods);
			}

			@Override
			public void character(long window, char character) {
				GlfwCallback a = adaptors.get(window);
				if (a != null) a.character(window, character);
			}
		};
	}

}
