package fieldcef.plugins;

import field.app.RunLoop;
import field.graphics.Window;
import field.graphics.util.KeyEventMapping;
import fieldbox.boxes.Keyboard;
import org.cef.browser.CefBrowser_N;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.LinkedHashSet;

/**
 */
public class BrowserKeyboardHacks {

	private final CefBrowser_N target;
	HashSet<Integer> keysDown = new LinkedHashSet<Integer>();
	HashSet<Character> charsDown = new HashSet<Character>();
	private final KeyEventMapping mapper = new KeyEventMapping();

	public BrowserKeyboardHacks(CefBrowser_N target) {
		this.target = target;
	}

	// AWT's MouseEvent constructor throws an NPE unless you give it a component.
	Component component = new Component() {
		@Override
		public Point getLocationOnScreen() {
			return new Point(0, 0);
		}
	};

	long lastEventAt = 0;

	public Keyboard.Hold onKeyDown(Window.Event<Window.KeyboardState> e, int key) {
		HashSet<Character> c = new HashSet<Character>(e.after.charsDown.values());
		c.removeAll(e.before.charsDown.values());

		HashSet<Integer> m = new HashSet<>(e.after.keysDown);
		m.removeAll(keysDown);

//		Log.log("keyboard2", "onKeyDown new keys:" + m + " new chars:" + c);
		if (m.size() == 0 && e.after.charsDown.size()==0 && e.before.charsDown.size()==0 && RunLoop.tick -lastEventAt>0) {
			// why send the event? its a keyboard repeat for a non-char character, that's why
			m.addAll(e.after.keysDown);
		} else {
			c.removeAll(charsDown);
			m.removeAll(keysDown);
		}
//		Log.log("keyboard2", "onKeyDown new2 keys:" + m + " new chars:" + c);
		if (m.size() == 0) {
			return null;
		}
		lastEventAt = RunLoop.tick;
		// now we have m new keypresses to deal with and c new characters

		int mod = (e.after.isAltDown() ? KeyEvent.ALT_DOWN_MASK : 0);
		mod |= (e.after.isShiftDown() ? KeyEvent.SHIFT_DOWN_MASK : 0);
		mod |= (e.after.isControlDown() ? KeyEvent.CTRL_DOWN_MASK : 0);
		mod |= (e.after.isSuperDown() ? KeyEvent.META_DOWN_MASK : 0);

		int fmod = mod;

		for (Integer mm : m) {

			if (mapper.isForcedTyped(mm)!=null && mod!=0)
			{
				KeyEvent ke = new KeyEvent(component, KeyEvent.KEY_TYPED, 0, mod, KeyEvent.VK_UNDEFINED, mapper.isForcedTyped(mm));
				System.out.println(" forced typed :"+(char) mapper.isForcedTyped(mm));
					e.properties.put(Window.consumed, true);
				target.sendKeyEvent(ke);
				keysDown.add(mm);
				continue;
			}


			Integer translated = mapper.translateCode(mm);
			keysDown.add(mm);

			if (translated != null) {

				KeyEvent ke = new KeyEvent(component, KeyEvent.KEY_PRESSED, 0, mod, translated, (char) translated.intValue());
				e.properties.put(Window.consumed, true);
				target.sendKeyEvent(ke);

				ke = new KeyEvent(component, KeyEvent.KEY_RELEASED, 0, fmod, translated, (char) translated.intValue());
				e.properties.put(Window.consumed, true);
				target.sendKeyEvent(ke);

			}
		}

		for (Character cc : c) {
			charsDown.add(cc);
		}

		return (e2, term) -> {

			if (term) {
//				Log.log("keyboard2", "onKeyUp releasing keys:" + m + " chars:" + c);

				for (Integer mm : m) {
					keysDown.remove(mm);
					Integer translated = mapper.translateCode(mm);
					if (translated != null) {

					}

				}

				for (Character cc : c) {
					charsDown.remove(cc);
				}
			}
			return !term;
		};
	}

	public void onCharDown(Window.Event<Window.KeyboardState> e, char key) {
		HashSet<Character> c = new HashSet<Character>(e.after.charsDown.values());
		c.removeAll(e.before.charsDown.values());

		HashSet<Integer> m = new HashSet<>(e.after.keysDown);
		m.removeAll(keysDown);

		// now we have m new keypresses to deal with and c new characters

		int mod = (e.after.isAltDown() ? KeyEvent.ALT_DOWN_MASK : 0);
		mod |= (e.after.isShiftDown() ? KeyEvent.SHIFT_DOWN_MASK : 0);
		mod |= (e.after.isControlDown() ? KeyEvent.CTRL_DOWN_MASK : 0);
		mod |= (e.after.isSuperDown() ? KeyEvent.META_DOWN_MASK : 0);

		int fmod = mod;


//		Log.log("keyboard2", "onCharDown char :" + c + " with mod " + fmod+"/"+key+" "+(int)key);

		if ((int)key>63000)
		{
//			Log.log("keyboard2", "ignoring extended keypress");
			return;
		}

		for(Character cc : c) {
			KeyEvent ke = new KeyEvent(component, KeyEvent.KEY_TYPED, 0, mod, KeyEvent.VK_UNDEFINED, cc);
			System.out.println("fire char :"+mod+" "+cc);
			e.properties.put(Window.consumed, true);

			if (e.after.isAltDown()) continue;
			if (e.after
				    .isControlDown()) continue;

			target.sendKeyEvent(ke);
		}

	}

}
