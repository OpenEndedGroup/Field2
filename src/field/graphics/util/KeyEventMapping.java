package field.graphics.util;

import com.badlogic.jglfw.Glfw;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import java.awt.event.KeyEvent;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class KeyEventMapping {

	BiMap<Integer, Integer> keyCodes = HashBiMap.create();
	Set<Integer> glfwModifiers = new LinkedHashSet<>();

	Map<Integer, Character> forceTyped = new LinkedHashMap<>();

	public KeyEventMapping() {
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
		keyCodes.put(KeyEvent.VK_TAB, Glfw.GLFW_KEY_TAB);


		keyCodes.put(KeyEvent.VK_CONTROL, Glfw.GLFW_KEY_LEFT_CONTROL);
//		keyCodes.put(KeyEvent.VK_CONTROL, Glfw.GLFW_KEY_RIGHT_CONTROL);
		keyCodes.put(KeyEvent.VK_SHIFT, Glfw.GLFW_KEY_LEFT_SHIFT);
		keyCodes.put(KeyEvent.VK_ALT, Glfw.GLFW_KEY_LEFT_ALT);
		keyCodes.put(KeyEvent.VK_META, Glfw.GLFW_KEY_LEFT_SUPER);
//		keyCodes.put(KeyEvent.VK_QUOTE, Glfw.GLFW_KEY_APOSTROPHE);
//		keyCodes.put(KeyEvent.VK_QUOTEDBL, Glfw.GLFW_KEY_APOSTROPHE);



		keyCodes.put(KeyEvent.VK_0, Glfw.GLFW_KEY_0);
		keyCodes.put(KeyEvent.VK_Z, Glfw.GLFW_KEY_Z);
		keyCodes.put(KeyEvent.VK_C, Glfw.GLFW_KEY_C);
		keyCodes.put(KeyEvent.VK_A, Glfw.GLFW_KEY_A);
		keyCodes.put(KeyEvent.VK_V, Glfw.GLFW_KEY_V);
		keyCodes.put(KeyEvent.VK_X, Glfw.GLFW_KEY_X);
		keyCodes.put(KeyEvent.VK_I, Glfw.GLFW_KEY_I);

		keyCodes.put(KeyEvent.VK_1, Glfw.GLFW_KEY_1);
		keyCodes.put(KeyEvent.VK_2, Glfw.GLFW_KEY_2);
		keyCodes.put(KeyEvent.VK_3, Glfw.GLFW_KEY_3);
		keyCodes.put(KeyEvent.VK_4, Glfw.GLFW_KEY_4);
		keyCodes.put(KeyEvent.VK_5, Glfw.GLFW_KEY_5);
		keyCodes.put(KeyEvent.VK_6, Glfw.GLFW_KEY_6);
		keyCodes.put(KeyEvent.VK_7, Glfw.GLFW_KEY_7);
		keyCodes.put(KeyEvent.VK_8, Glfw.GLFW_KEY_8);
		keyCodes.put(KeyEvent.VK_9, Glfw.GLFW_KEY_9);

		keyCodes.put(KeyEvent.VK_SPACE, Glfw.GLFW_KEY_SPACE);
		keyCodes.put(KeyEvent.VK_PERIOD, Glfw.GLFW_KEY_PERIOD);

//		forceTyped.put(Glfw.GLFW_KEY_C, 'c');
//		forceTyped.put(Glfw.GLFW_KEY_V, 'v');
//		forceTyped.put(Glfw.GLFW_KEY_F, 'f');

		glfwModifiers.add(Glfw.GLFW_KEY_LEFT_CONTROL);
		glfwModifiers.add(Glfw.GLFW_KEY_RIGHT_CONTROL);
		glfwModifiers.add(Glfw.GLFW_KEY_LEFT_SHIFT);
		glfwModifiers.add(Glfw.GLFW_KEY_RIGHT_SHIFT);
		glfwModifiers.add(Glfw.GLFW_KEY_LEFT_ALT);
		glfwModifiers.add(Glfw.GLFW_KEY_RIGHT_ALT);
		glfwModifiers.add(Glfw.GLFW_KEY_LEFT_SUPER);
		glfwModifiers.add(Glfw.GLFW_KEY_RIGHT_SUPER);

	}


	public Integer translateCode(int glfwcode) {
		return keyCodes.inverse()
			       .get(glfwcode);
	}

	public boolean isModifier(int glfwcode) {
		return glfwModifiers.contains(glfwcode);
	}

	static public String lookup(int num) {
		Field[] ff = Glfw.class.getFields();
		for (Field fff : ff) {
			try {
				if (fff.getName()
				       .startsWith("GLFW_KEY") && ((Number)fff.get(null)).intValue() == num) return fff.getName();
			} catch (IllegalAccessException e) {
			} catch(ClassCastException e)
			{

			}
		}
		return null;
	}

	public Character isForcedTyped(int glfwcode)
	{
		return forceTyped.get(glfwcode);
	}

}
