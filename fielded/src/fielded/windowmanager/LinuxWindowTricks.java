package fielded.windowmanager;

import static org.lwjgl.glfw.GLFW.*;
import field.app.RunLoop;
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
 * Tracks the editor opened in the Chrome process.
 *
 * This allows us to programmatically switch focus to it (by double tapping shift) and close it on exit
 *
 * todo: figure out how to mimic this on OS X
 * todo: on startup consider closing all windows called "Field / Disconnected" since they are left over from a crash
 */
public class LinuxWindowTricks extends Box {

	// TODO: there should be a discovery mechanism for this
	static public final String wmctrl = "/usr/bin/wmctrl";
	static public final String transset = "/usr/bin/transset";
	static public final String field_editor_title = "Field Editor";
	public static Dict.Prop<Integer> lostFocus = new Dict.Prop<>("lostFocus");

	Set<String> previously = new LinkedHashSet<>();
	Set<String> editors = new LinkedHashSet<>();

	public LinuxWindowTricks(Box root) {
		connect(root);
		first(Boxes.window, both()).ifPresent(x -> x.addKeyboardHandler(event -> {
			Set<Integer> kpressed = Window.KeyboardState.keysPressed(event.before, event.after);
			if (kpressed.contains(GLFW_KEY_LEFT_SHIFT) || kpressed.contains(GLFW_KEY_RIGHT_SHIFT)) {
				if (event.after.keysDown.size() == 1) trigger();
			}

			return true;
		}));
		beforeOpen();
		RunLoop.main.once(() -> afterOpen());

		Runtime.getRuntime().addShutdownHook(new Thread(this::closeAll));
	}

	long lastTriggerAt = -1;

	public void trigger() {
		long now = System.currentTimeMillis();
		if (now - lastTriggerAt < 500) {
			beginEditorForward();
		}
		lastTriggerAt = now;
	}

	public void closeAll() {
		for (String id : editors) {
			ProcessBuilder pb = new ProcessBuilder(wmctrl, "-i", "-c", id);
			try {
				pb.start();
			} catch (IOException e) {
			}
		}

	}

	public void beforeOpen() {
		String[] previously = listWindows();
		if (previously == null) {
			System.err.println(" WARNING: couldn't track editor window opening, won't be able to switch between windows and do other tricks");
		} else {
			for (String s : previously) {
				String[] parts = s.split(" ", 2);
				if (parts[1].contains(field_editor_title)) {
					LinuxWindowTricks.this.previously.add(parts[0]);
				}
			}
		}
	}

	protected String[] listWindows() {
		ProcessBuilder pb = new ProcessBuilder(wmctrl, "-l");
		try {
			Process p = pb.start();
			BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
			StringBuilder builder = new StringBuilder();
			String line = null;
			while ((line = br.readLine()) != null) {
				builder.append(line);
				builder.append(System.getProperty("line.separator"));
			}
			String result = builder.toString();
			return result.split("\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public void afterOpen() {
		long a = System.currentTimeMillis();
		RunLoop.main.mainLoop.attach(i -> {

			long b = System.currentTimeMillis();
			if (b - a < 4000) return true;
			String[] now = listWindows();

			if (now == null) {
				Log.log("windows.general", ()->" WARNING: couldn't track editor window opening, won't be able to switch between windows and do other tricks");
			} else {
				for (String s : now) {
					String[] parts = s.split(" ", 2);
					if (parts[1].contains(field_editor_title) && !previously.contains(parts[0])) {
						Log.log("windows.general", ()->"  tracking editor " + parts[0] + " / " + parts[1]);
						editors.add(parts[0]);
					}
				}
			}

			Log.log("windows.general", ()->" registered :" + editors + " editors out of " + previously);

			//TODO: should be preference.
			pinEditorOnTop();

			return false;
		});
	}

	/**
	 * returns error string on failure
	 *
	 * @return
	 */
	public String beginEditorForward() {
		if (editors.size() == 0) return "No editor found";

		properties.put(lostFocus, properties.getOr(lostFocus, () -> 0)+1);
		// the order here is odd, because we want the first wmctrl to be executed speculatively (before the listing of all the windows) in the very likely case that the window is still around in order to minimize latency
		Set<String> still = null;
		Iterator<String> i = editors.iterator();
		String r = null;
		while (i.hasNext()) {
			String id = i.next();
			ProcessBuilder pb = new ProcessBuilder(wmctrl, "-i", "-a", id);
			try {
				pb.start();
			} catch (IOException e) {
			}

			if (still == null) {
				still = new LinkedHashSet<>();
				String[] previously = listWindows();
				for (String s : previously) {
					String[] parts = s.split(" ", 2);
					if (parts[1].contains(field_editor_title) && editors.contains(parts[0])) {
						still.add(parts[0]);
					}
				}
			}
			if (!still.contains(id)) {
				i.remove();
				r = "Couldn't find editor";
			} else {
				return null;
			}
		}
		return r;
	}

	/**
	 * returns error string on failure
	 *
	 * @return
	 */
	public String pinEditorOnTop()
	{
		if (editors.size() == 0) return "No editor found";
		for(String id : editors) {
			ProcessBuilder pb = new ProcessBuilder(wmctrl, "-i", "-r", id, "-b", "add,above");
			try {
				pb.start();
			} catch (IOException e) {
			}
			pb = new ProcessBuilder(transset, "-i", id, "0.8");
			try {
				pb.start();
			} catch (IOException e) {
			}
		}
		return null;
	}



}
