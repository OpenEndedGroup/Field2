package field.graphics.util;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import java.awt.event.KeyEvent;
import java.lang.reflect.Field;
import java.util.*;

import static org.lwjgl.glfw.GLFW.*;

public class KeyEventMapping {

	static HashMap<Integer, String> lookupMap = null;
	BiMap<Integer, Integer> keyCodes = HashBiMap.create();
	Set<Integer> glfwModifiers = new LinkedHashSet<>();
	Map<Integer, Character> forceTyped = new LinkedHashMap<>();


	public KeyEventMapping() {
		keyCodes.put(KeyEvent.VK_ENTER, GLFW_KEY_ENTER);
		keyCodes.put(KeyEvent.VK_UP, GLFW_KEY_UP);
		keyCodes.put(KeyEvent.VK_DOWN, GLFW_KEY_DOWN);
		keyCodes.put(KeyEvent.VK_LEFT, GLFW_KEY_LEFT);
		keyCodes.put(KeyEvent.VK_RIGHT, GLFW_KEY_RIGHT);

		keyCodes.put(KeyEvent.VK_PAGE_UP, GLFW_KEY_PAGE_UP);
		keyCodes.put(KeyEvent.VK_PAGE_DOWN, GLFW_KEY_PAGE_DOWN);


		keyCodes.put(KeyEvent.VK_ESCAPE, GLFW_KEY_ESCAPE);
		keyCodes.put(KeyEvent.VK_END, GLFW_KEY_END);
		keyCodes.put(KeyEvent.VK_HOME, GLFW_KEY_HOME);
		keyCodes.put(KeyEvent.VK_BACK_SPACE, GLFW_KEY_BACKSPACE);
		keyCodes.put(KeyEvent.VK_DELETE, GLFW_KEY_DELETE);
		keyCodes.put(KeyEvent.VK_TAB, GLFW_KEY_TAB);


		keyCodes.put(KeyEvent.VK_CONTROL, GLFW_KEY_LEFT_CONTROL);
//		keyCodes.put(KeyEvent.VK_CONTROL, GLFW_KEY_RIGHT_CONTROL);
		keyCodes.put(KeyEvent.VK_SHIFT, GLFW_KEY_LEFT_SHIFT);
		keyCodes.put(KeyEvent.VK_ALT, GLFW_KEY_LEFT_ALT);
		keyCodes.put(KeyEvent.VK_META, GLFW_KEY_LEFT_SUPER);
//		keyCodes.put(KeyEvent.VK_QUOTE, GLFW_KEY_APOSTROPHE);
//		keyCodes.put(KeyEvent.VK_QUOTEDBL, GLFW_KEY_APOSTROPHE);


		keyCodes.put(KeyEvent.VK_0, GLFW_KEY_0);
		keyCodes.put(KeyEvent.VK_Z, GLFW_KEY_Z);
		keyCodes.put(KeyEvent.VK_C, GLFW_KEY_C);
		keyCodes.put(KeyEvent.VK_G, GLFW_KEY_G);
		keyCodes.put(KeyEvent.VK_A, GLFW_KEY_A);
		keyCodes.put(KeyEvent.VK_V, GLFW_KEY_V);
		keyCodes.put(KeyEvent.VK_X, GLFW_KEY_X);
		keyCodes.put(KeyEvent.VK_I, GLFW_KEY_I);

		keyCodes.put(KeyEvent.VK_1, GLFW_KEY_1);
		keyCodes.put(KeyEvent.VK_2, GLFW_KEY_2);
		keyCodes.put(KeyEvent.VK_3, GLFW_KEY_3);
		keyCodes.put(KeyEvent.VK_4, GLFW_KEY_4);
		keyCodes.put(KeyEvent.VK_5, GLFW_KEY_5);
		keyCodes.put(KeyEvent.VK_6, GLFW_KEY_6);
		keyCodes.put(KeyEvent.VK_7, GLFW_KEY_7);
		keyCodes.put(KeyEvent.VK_8, GLFW_KEY_8);
		keyCodes.put(KeyEvent.VK_9, GLFW_KEY_9);

		keyCodes.put(KeyEvent.VK_SPACE, GLFW_KEY_SPACE);
		keyCodes.put(KeyEvent.VK_PERIOD, GLFW_KEY_PERIOD);


		glfwModifiers.add(GLFW_KEY_LEFT_CONTROL);
		glfwModifiers.add(GLFW_KEY_RIGHT_CONTROL);
		glfwModifiers.add(GLFW_KEY_LEFT_SHIFT);
		glfwModifiers.add(GLFW_KEY_RIGHT_SHIFT);
		glfwModifiers.add(GLFW_KEY_LEFT_ALT);
		glfwModifiers.add(GLFW_KEY_RIGHT_ALT);
		glfwModifiers.add(GLFW_KEY_LEFT_SUPER);
		glfwModifiers.add(GLFW_KEY_RIGHT_SUPER);

	}

	static public String lookup(int num) {

		if (lookupMap == null) {

			lookupMap = new HashMap<>();
			Field[] ff = org.lwjgl.glfw.GLFW.class.getFields();
			for (Field fff : ff) {
				try {
					if (fff.getName()
					       .startsWith("GLFW_KEY")) lookupMap.put(((Number) fff.get(null)).intValue(), fff.getName());
				} catch (IllegalAccessException e) {
				} catch (ClassCastException e) {

				}
			}
		}
		return lookupMap.get(num);
	}

	public Integer translateCode(int glfwcode) {
		return keyCodes.inverse()
			       .get(glfwcode);
	}

	public boolean isModifier(int glfwcode) {
		return glfwModifiers.contains(glfwcode);
	}

	public Character isForcedTyped(int glfwcode) {
		return forceTyped.get(glfwcode);
	}

}
