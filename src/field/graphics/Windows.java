package field.graphics;

import com.badlogic.jglfw.GlfwCallback;
import com.badlogic.jglfw.GlfwCallbackAdapter;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.badlogic.jglfw.Glfw.GLFW_RELEASE;
import static com.badlogic.jglfw.Glfw.glfwInit;
import static com.badlogic.jglfw.Glfw.glfwSetCallback;

/**
 * All Window instances must be registered with this singleton Windows
 *
 * GLFW multiplexes all of it's events through a singlecallback, this class demultiplexes them.
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


			boolean fakeButton1 = false;

			@Override
			public void mouseButton(long window, int button, boolean pressed, int mods) {

//				System.out.println(" mouseButton :"+button+" "+pressed+" "+mods);

				if (button==0 && mods==2 && pressed)
				{
					button=1;
					mods = 0;
					fakeButton1 = true;
				}
				else if (button==0 && !pressed && fakeButton1)
				{
					button = 1;
					mods = 0;
					fakeButton1 = false;
				}

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

			@Override
			public void drop(long window, String[] files) {
				// cursorPos is given just before drop to tell us where the drop has occured.
				GlfwCallback a = adaptors.get(window);
				if (a != null) a.drop(window, files);
			}


			@Override
			public boolean windowClose(long window) {
				GlfwCallback a = adaptors.get(window);
				if (a != null) return a.windowClose(window);
				return true;
			}
		};
	}

}
