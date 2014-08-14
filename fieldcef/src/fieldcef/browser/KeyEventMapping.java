package fieldcef.browser;

import com.badlogic.jglfw.Glfw;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import java.awt.event.KeyEvent;

public class KeyEventMapping {

	BiMap<Integer, Integer> keyCodes = HashBiMap.create();

	public KeyEventMapping()
	{
		keyCodes.put(KeyEvent.VK_ENTER, Glfw.GLFW_KEY_ENTER);
		keyCodes.put(KeyEvent.VK_UP, Glfw.GLFW_KEY_UP);
		keyCodes.put(KeyEvent.VK_DOWN, Glfw.GLFW_KEY_DOWN);
		keyCodes.put(KeyEvent.VK_LEFT, Glfw.GLFW_KEY_LEFT);
		keyCodes.put(KeyEvent.VK_RIGHT, Glfw.GLFW_KEY_RIGHT);

		keyCodes.put(KeyEvent.VK_PAGE_UP, Glfw.GLFW_KEY_PAGE_UP);
		keyCodes.put(KeyEvent.VK_PAGE_DOWN, Glfw.GLFW_KEY_PAGE_DOWN);

		keyCodes.put(KeyEvent.VK_ESCAPE, Glfw.GLFW_KEY_ESCAPE);
		keyCodes.put(KeyEvent.VK_END, Glfw.GLFW_KEY_END);
		keyCodes.put(KeyEvent.VK_HOME, Glfw.GLFW_KEY_HOME);
		keyCodes.put(KeyEvent.VK_BACK_SPACE, Glfw.GLFW_KEY_BACKSPACE);
		keyCodes.put(KeyEvent.VK_DELETE, Glfw.GLFW_KEY_DELETE);


		keyCodes.put(KeyEvent.VK_CONTROL, Glfw.GLFW_KEY_LEFT_CONTROL);
//		keyCodes.put(KeyEvent.VK_CONTROL, Glfw.GLFW_KEY_RIGHT_CONTROL);
		keyCodes.put(KeyEvent.VK_SHIFT, Glfw.GLFW_KEY_LEFT_SHIFT);
		keyCodes.put(KeyEvent.VK_ALT, Glfw.GLFW_KEY_LEFT_ALT);
		keyCodes.put(KeyEvent.VK_META, Glfw.GLFW_KEY_LEFT_SUPER);

	}


	public Integer translateCode(int glfwcode)
	{
		return keyCodes.inverse().get(glfwcode);
	}

}
