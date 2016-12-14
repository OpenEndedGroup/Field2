package field;

import field.graphics.Window;
import field.utility.Log;
import static org.lwjgl.glfw.GLFW.*;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * GLFW sometimes has issues with modifier keys sticking down.
 */
public class CanonicalModifierKeys {

	private final long window;
	private boolean shift;
	private boolean control;
	private boolean supr;
	private boolean option;

	public CanonicalModifierKeys(long window)
	{
		this.window = window;
	}

	public void event(int key, int scancode, int action, int mods, Set<Integer> down)
	{
		/*switch (key)
		{
			case GLFW_KEY_LEFT_SHIFT:
			case GLFW_KEY_RIGHT_SHIFT: shift = action== GLFW_PRESS; break;
			case GLFW_KEY_LEFT_CONTROL:
			case GLFW_KEY_RIGHT_CONTROL: control = action== GLFW_PRESS; break;
			case GLFW_KEY_LEFT_SUPER:
			case GLFW_KEY_RIGHT_SUPER: supr= action== GLFW_PRESS; break;
			case GLFW_KEY_LEFT_ALT:
			case GLFW_KEY_RIGHT_ALT: option= action== GLFW_PRESS; break;
			default:
				break;
		}
*/
		switch (key)
		{
			case GLFW_KEY_LEFT_SHIFT:
			case GLFW_KEY_RIGHT_SHIFT: shift = (mods & GLFW_MOD_SHIFT)!=0; break;
			case GLFW_KEY_LEFT_CONTROL:
			case GLFW_KEY_RIGHT_CONTROL: control = (mods & GLFW_MOD_CONTROL)!=0; break;
			case GLFW_KEY_LEFT_SUPER:
			case GLFW_KEY_RIGHT_SUPER: supr = (mods & GLFW_MOD_SUPER)!=0; break;
			case GLFW_KEY_LEFT_ALT:
			case GLFW_KEY_RIGHT_ALT: option = (mods & GLFW_MOD_ALT)!=0; break;
			default:
				break;
		}


		shift = (mods & GLFW_MOD_SHIFT)!=0;
		supr = (mods & GLFW_MOD_SUPER)!=0;
		control = (mods & GLFW_MOD_CONTROL)!=0;
		option = (mods & GLFW_MOD_ALT)!=0;

		if (shift)
			down.add(GLFW_KEY_LEFT_SHIFT);
		else
			down.remove(GLFW_KEY_LEFT_SHIFT);

		if (supr)
			down.add(GLFW_KEY_LEFT_SUPER);
		else
			down.remove(GLFW_KEY_LEFT_SUPER);

		if (control)
			down.add(GLFW_KEY_LEFT_CONTROL);
		else
			down.remove(GLFW_KEY_LEFT_CONTROL);

		if (option)
			down.add(GLFW_KEY_LEFT_ALT);
		else
			down.remove(GLFW_KEY_LEFT_ALT);

		Log.log("finalkey",()-> scancode+" / "+action+" -> "+shift + " " + control + " " + supr + " " + option);

	}

	public boolean isShift()
	{
		boolean a = glfwGetKey(window, GLFW_KEY_LEFT_SHIFT)!=0;
		boolean b = glfwGetKey(window, GLFW_KEY_RIGHT_SHIFT)!=0;

		return /*(a | b) &*/ shift;
	}

	public boolean isAlt()
	{
		boolean a = glfwGetKey(window, GLFW_KEY_LEFT_ALT)!=0;
		boolean b = glfwGetKey(window, GLFW_KEY_RIGHT_ALT)!=0;

		return /*(a | b) &*/ option;
	}

	public boolean isControl()
	{
		boolean a = glfwGetKey(window, GLFW_KEY_LEFT_CONTROL)!=0;
		boolean b = glfwGetKey(window, GLFW_KEY_RIGHT_CONTROL)!=0;

		return /*(a | b) &*/ control;
	}

	public boolean isSuper()
	{
		boolean a = glfwGetKey(window, GLFW_KEY_LEFT_SUPER)!=0;
		boolean b = glfwGetKey(window, GLFW_KEY_RIGHT_SUPER)!=0;

		return /*(a | b) &*/ supr;
	}

	public Window.KeyboardState cleanModifiers(Window.KeyboardState ks)
	{
		Set<Integer> k = new LinkedHashSet<>(ks.keysDown);
		if (!isShift()) k.remove(GLFW_KEY_LEFT_SHIFT); else k.add(GLFW_KEY_LEFT_SHIFT);
		if (!isShift()) k.remove(GLFW_KEY_RIGHT_SHIFT);
		if (!isAlt()) k.remove(GLFW_KEY_LEFT_ALT);else k.add(GLFW_KEY_LEFT_ALT);
		if (!isAlt()) k.remove(GLFW_KEY_RIGHT_ALT);
		if (!isControl()) k.remove(GLFW_KEY_LEFT_CONTROL);else k.add(GLFW_KEY_LEFT_CONTROL);
		if (!isControl()) k.remove(GLFW_KEY_RIGHT_CONTROL);
		if (!isSuper()) k.remove(GLFW_KEY_LEFT_SUPER);else k.add(GLFW_KEY_LEFT_SUPER);
		if (!isSuper()) k.remove(GLFW_KEY_RIGHT_SUPER);

		return new Window.KeyboardState(k, ks.charsDown, ks.time);
	}

}
