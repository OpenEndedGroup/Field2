package field.graphics.util;

import field.graphics.Window;

import javax.xml.crypto.dsig.keyinfo.KeyName;

public class KeyBinding {

	static public class KeyName
	{
		boolean shift;
		boolean alt;
		boolean control;
		boolean supper;

		int key;

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
			if (!(supper==state.isSuperDown())) return false;
			return true;
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
