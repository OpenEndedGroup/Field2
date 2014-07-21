package fielded.windowmanager;

import com.badlogic.jglfw.Glfw;
import field.graphics.RunLoop;
import field.graphics.Window;
import field.utility.Dict;
import field.utility.Log;
import fieldbox.boxes.Box;
import fieldbox.boxes.Boxes;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * for OSX
 * Tracks the editor opened in the Chrome process.
 *
 * This allows us to programmatically switch focus to it (by double tapping shift) and close it on exit
 *
 */
public class OSXWindowTricks extends Box {
	// TODO: there should be a discovery mechanism for this
	static public final String field_editor_title = "Field Editor";
	public static Dict.Prop<Integer> lostFocus = new Dict.Prop<>("lostFocus");


	public OSXWindowTricks(Box root) {
		connect(root);

		first(Boxes.window, both()).ifPresent(x -> x.addKeyboardHandler(event -> {
			Set<Integer> kpressed = Window.KeyboardState.keysPressed(event.before, event.after);
			if (kpressed.contains(Glfw.GLFW_KEY_LEFT_SHIFT) || kpressed.contains(Glfw.GLFW_KEY_RIGHT_SHIFT)) {
				if (event.after.keysDown.size() == 1) trigger();
			}

			return true;
		}));
		RunLoop.main.once(() -> afterOpen());

		Runtime.getRuntime().addShutdownHook(new Thread(this::closeAll));
	}

	long lastTriggerAt = -1;

	public void trigger() {
		long now = System.currentTimeMillis();
		if (now - lastTriggerAt < 500) {
			pinEditorOnTop();
		}
		lastTriggerAt = now;
	}

	public void closeAll() {
		String CloseAll =
			    "try\n" +
			    "\ttell application \"Google Chrome\"\n" +
			    "\t\tclose (every window whose title contains \"Field Editor\")\n" +
			    "\tend tell\n" +
			    "end try\n" +
			    "try\n" +
			    "\ttell application \"System Events\"\n" +
			    "\t\ttell application process \"Trampoline\"\n" +
			    "\t\t\tclose (every window whose name contains \"Field\")\n" +
			    "\t\tend tell\n" +
			    "\tend tell\n" +
			    "end try\n" +
			    "\n";
		ProcessBuilder pb = new ProcessBuilder("osascript", "-e", CloseAll);
		try {
			pb.start();
			Log.log("osxwindowtricks.trace", "****Close All****");
		} catch (IOException e) {
			Log.log("osxwindowtricks.error", "****Close Oops****", e);

		}

	}



	public void afterOpen() {
		//TODO: should be preference.
		pinEditorOnTop();
	}

	/**
	 * returns error string on failure
	 *
	 * @return
	 */


	/**
	 * returns error string on failure
	 *
	 * @return
	 */
	public String pinEditorOnTop()
	{
		String EditorForward =
			    "tell application \"Google Chrome\"\n" +
			    "\tactivate\n" +
			    "\tset visible of first window whose title contains \"Field Editor\" to true\n" +
			    "end tell\n";
		ProcessBuilder pb = new ProcessBuilder("osascript", "-e", EditorForward);
		try {
			pb.start();
			Log.log("osxwindowtricks.trace", "****Editor forward****");
		} catch (IOException e) {
			Log.log("osxwindowtricks.error", "****Editor Oops****", e);
		}
		return null;
	}


}
