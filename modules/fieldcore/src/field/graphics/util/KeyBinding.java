package field.graphics.util;

import static org.lwjgl.glfw.GLFW.*;
import org.lwjgl.glfw.GLFW;
import field.graphics.Window;

import javax.xml.crypto.dsig.keyinfo.KeyName;
import java.lang.reflect.Field;

/**
 * Helper class for containing a key binding. Currently used in the camera class.
 */
public class KeyBinding {

	static public final String[] n_alt = {"alt"};
	static public final String[] n_shift = {"shift"};
	static public final String[] n_command = {"command", "meta", "super"};
	static public final String[] n_control = {"control", "ctrl"};

	static public class KeyName
	{
		boolean shift;
		boolean alt;
		boolean control;
		boolean supper;

		int key;

		public KeyName(String name)
		{
			name = name.toLowerCase();
			for(String a : n_alt)
			{
				if (name.contains(a))
				{
					name = name.replace(a, "");
					alt = true;
				}
			}
			for(String a : n_shift)
			{
				if (name.contains(a))
				{
					name = name.replace(a, "");
					shift = true;
				}
			}
			for(String a : n_command)
			{
				if (name.contains(a))
				{
					name = name.replace(a, "");
					supper = true;
				}
			}
			for(String a : n_control)
			{
				if (name.contains(a))
				{
					name = name.replace(a, "");
					control = true;
				}
			}
			name = name.replace("-", "");
			name = name.trim();
			Field[] ff = GLFW.class.getFields();
			for(Field f : ff)
			{
				if (f.getName().startsWith("GLFW_KEY_"))
				{
					if (f.getName().replace("GLFW_KEY_", "").toLowerCase().equals(name))
					{
						try {
							this.key = (int) f.get(null);
							break;
						} catch (IllegalAccessException e) {
						}
					}
				}
			}
			if (this.key==0)
				throw new IllegalArgumentException(" no such key called "+name);
		}
		public KeyName(int key, boolean shift, boolean alt, boolean control, boolean supper)
		{
			this.key = key;
			this.shift = shift;
			this.alt = alt;
			this.control = control;
			this.supper = supper;
		}

		public boolean matches(Window.KeyboardState state)
		{
			if (!state.keysDown.contains(key)) return false;
			if (!(shift==state.isShiftDown())) return false;
			if (!(alt==state.isAltDown())) return false;
			if (!(control==state.isControlDown())) return false;
			return supper == state.isSuperDown();
		}

		public boolean transitionInto(Window.Event<Window.KeyboardState> event)
		{
			return (!matches(event.before) && matches(event.after));
		}

		public boolean transitionOutof(Window.Event<Window.KeyboardState> event)
		{
			return (matches(event.before) && !matches(event.after));
		}
	}

}
