package field;

import com.badlogic.jglfw.Glfw;
import field.graphics.Window;
import field.utility.Log;

import java.util.LinkedHashSet;
import java.util.Set;

import static com.badlogic.jglfw.Glfw.*;

/**
 * GLFW sometimes has issues with modifier keys sticking down.
 */
public class CannonicalModifierKeys {

	private final long window;
	private boolean shift;
	private boolean control;
	private boolean supr;
	private boolean option;

	public CannonicalModifierKeys(long window)
	{
		this.window = window;
	}

	public void event(int key, int scancode, int action, int mods, Set<Integer> down)
	{
		/*switch (key)
		{
			case Glfw.GLFW_KEY_LEFT_SHIFT:
			case Glfw.GLFW_KEY_RIGHT_SHIFT: shift = action== Glfw.GLFW_PRESS; break;
			case Glfw.GLFW_KEY_LEFT_CONTROL:
			case Glfw.GLFW_KEY_RIGHT_CONTROL: control = action== Glfw.GLFW_PRESS; break;
			case Glfw.GLFW_KEY_LEFT_SUPER:
			case Glfw.GLFW_KEY_RIGHT_SUPER: supr= action== Glfw.GLFW_PRESS; break;
			case Glfw.GLFW_KEY_LEFT_ALT:
			case Glfw.GLFW_KEY_RIGHT_ALT: option= action== Glfw.GLFW_PRESS; break;
			default:
				break;
		}
*/
		switch (key)
		{
			case Glfw.GLFW_KEY_LEFT_SHIFT:
			case Glfw.GLFW_KEY_RIGHT_SHIFT: shift = (mods & Glfw.GLFW_MOD_SHIFT)!=0; break;
			case Glfw.GLFW_KEY_LEFT_CONTROL:
			case Glfw.GLFW_KEY_RIGHT_CONTROL: control = (mods & Glfw.GLFW_MOD_CONTROL)!=0; break;
			case Glfw.GLFW_KEY_LEFT_SUPER:
			case Glfw.GLFW_KEY_RIGHT_SUPER: supr = (mods & Glfw.GLFW_MOD_SUPER)!=0; break;
			case Glfw.GLFW_KEY_LEFT_ALT:
			case Glfw.GLFW_KEY_RIGHT_ALT: option = (mods & Glfw.GLFW_MOD_ALT)!=0; break;
			default:
				break;
		}


		shift = (mods & Glfw.GLFW_MOD_SHIFT)!=0;
		supr = (mods & Glfw.GLFW_MOD_SUPER)!=0;
		control = (mods & Glfw.GLFW_MOD_CONTROL)!=0;
		option = (mods & Glfw.GLFW_MOD_ALT)!=0;

		if (shift)
			down.add(Glfw.GLFW_KEY_LEFT_SHIFT);
		else
			down.remove(Glfw.GLFW_KEY_LEFT_SHIFT);

		if (supr)
			down.add(Glfw.GLFW_KEY_LEFT_SUPER);
		else
			down.remove(Glfw.GLFW_KEY_LEFT_SUPER);

		if (control)
			down.add(Glfw.GLFW_KEY_LEFT_CONTROL);
		else
			down.remove(Glfw.GLFW_KEY_LEFT_CONTROL);

		if (option)
			down.add(Glfw.GLFW_KEY_LEFT_ALT);
		else
			down.remove(Glfw.GLFW_KEY_LEFT_ALT);

		Log.log("finalkey",()-> scancode+" / "+action+" -> "+shift + " " + control + " " + supr + " " + option);

	}

	public boolean isShift()
	{
		boolean a = glfwGetKey(window, GLFW_KEY_LEFT_SHIFT);
		boolean b = glfwGetKey(window, GLFW_KEY_RIGHT_SHIFT);

		return /*(a | b) &*/ shift;
	}

	public boolean isAlt()
	{
		boolean a = glfwGetKey(window, GLFW_KEY_LEFT_ALT);
		boolean b = glfwGetKey(window, GLFW_KEY_RIGHT_ALT);

		return /*(a | b) &*/ option;
	}

	public boolean isControl()
	{
		boolean a = glfwGetKey(window, GLFW_KEY_LEFT_CONTROL);
		boolean b = glfwGetKey(window, GLFW_KEY_RIGHT_CONTROL);

		return /*(a | b) &*/ control;
	}

	public boolean isSuper()
	{
		boolean a = glfwGetKey(window, GLFW_KEY_LEFT_SUPER);
		boolean b = glfwGetKey(window, GLFW_KEY_RIGHT_SUPER);

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
