package fieldcef.plugins;

import com.badlogic.jglfw.Glfw;
import field.graphics.RunLoop;
import field.graphics.Window;
import field.utility.Dict;
import field.utility.Log;
import field.utility.Rect;
import fieldagent.Main;
import fieldbox.boxes.*;
import fieldbox.io.IO;
import fieldbox.ui.FieldBoxWindow;
import fieldcef.browser.Browser;
import fielded.Commands;
import fielded.RemoteEditor;
import fielded.ServerSupport;
import fielded.webserver.Server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * This is a browser, created by default, that covers the whole window in the glass layer. We can then either center it in the window, or crop into
 * it.
 */
public class GlassBrowser extends Box implements IO.Loaded {

	static public final Dict.Prop<GlassBrowser> glassBrowser = new Dict.Prop<>("glassBrowser").toCannon()
												  .type()
												  .doc("The Browser that is stuck in front of the window, in window coordinates");

	List<String> playlist = Arrays.asList("preamble.js", "jquery-2.1.0.min.js", "jquery.autosize.input.js", "modal.js");
	String styleSheet = "field-codemirror.css";

	// we'll need to make sure that this is centered on larger screens
	int maxw = 2560;
	int maxh = 1600;
	public Browser browser;
	public String styles;

	public GlassBrowser(Box root) {
		this.properties.put(glassBrowser, this);
	}

	int tick = 0;

	Commands commandHelper = new Commands();

	public void loaded() {
		Log.log("glassbrowser.debug", "initializing browser");
		browser = new Browser();
		browser.properties.put(Box.frame, new Rect(0, 0, maxw, maxh));
		browser.properties.put(FLineDrawing.layer, "glass");
		browser.properties.put(Drawing.windowSpace, true);
		browser.properties.put(Boxes.dontSave, true);
		browser.connect(this);
		browser.loaded();
		this.properties.put(Boxes.dontSave, true);
		styles = findAndLoad(styleSheet, false);


//		boot();
		// we've been having an incredibly hard time tracking down a problem on OS X where sometimes the CefSystem will fail to initialize the browser.

		long[] t = {0};
		RunLoop.main.getLoop()
			    .connect(x -> {
				    if (t[0] == 0) t[0] = System.currentTimeMillis();
				    if (System.currentTimeMillis() - t[0] > 5000) {
					    boot();

					    return false;
				    }
				    return true;
			    });


		// I've been looking forward to this for a while
		this.properties.putToList(Keyboard.onKeyDown, (e, k) -> {

			if (selection().findFirst()
				       .isPresent() && !e.properties.isTrue(Window.consumed, false)) {
				if (e.after.keysDown.contains(Glfw.GLFW_KEY_SPACE) && e.after.isControlDown()) {
					center();
					runCommands();
				}
			}

			return null;
		});
	}

	public void boot() {
		Server s = this.find(ServerSupport.server, both())
			       .findFirst()
			       .orElseThrow(() -> new IllegalArgumentException(" Server not found "));


		String bootstrap = "<html style='background:rgba(0,0,0,0.02);'><head><style>" + styles + "</style></head><body style='background:rgba(0,0,0,0.02);'></body></html>";
		String res =  UUID.randomUUID()
							    .toString();
		s.setFixedResource("/" + res, bootstrap);
		browser.properties.put(browser.url, "http://localhost:8080/"+res);


		tick = 0;
		RunLoop.main.getLoop()
			    .connect(x -> {
				    tick++;
				    if (browser.browser.getURL()
						       .equals("http://localhost:8080/"+res)) {
					    inject2();
					    return false;
				    }
				    Log.log("glassBrowser.boot", "WAITING url:" + browser.browser.getURL());
				    Drawing.dirty(this);
				    return tick < 100;
			    });

		Drawing.dirty(this);
	}

	int ignoreHide = 0;

	public void inject2() {
		Log.log("glassbrowser.debug", "inject 2 is happening");
		for (String s : playlist) {
			Log.log("glassbrowser.debug", "executing :" + s);
			browser.executeJavaScript(findAndLoad(s, true));
		}
		hide();

		browser.addHandler(x -> x.equals("focus"), (address, payload, ret) -> {
			if (ignoreHide > 0) ignoreHide--;
			else hide();
			ret.accept("OK");
		});

		browser.addHandler(x -> x.equals("request.commands"), (address, paylod, ret) -> {
			commandHelper.requestCommands(selection().findFirst(), null, null, ret, -1, -1);
		});

		browser.addHandler(x -> x.equals("call.command"), (address, payload, ret) -> {
			String command = payload.getString("command");
			Runnable r = commandHelper.callTable.get(command);
			if (r != null) {
				if (r instanceof RemoteEditor.ExtendedCommand)
					((RemoteEditor.ExtendedCommand) r).begin(commandHelper.supportsPrompt(x -> {
						Log.log("glassbrowser.debug", "continue commands " + x + "");
						browser.executeJavaScript("continueCommands(JSON.parse('" + x + "'))");
						ignoreHide = 4;
						show();
					}), null);
				r.run();
			}
			ret.accept("OK");
		});

	}

	public void show() {
		browser.properties.put(Box.hidden, false);
		browser.setFocus(true);
		Drawing.dirty(browser);
	}


	public void hide() {
		tick = 0;
		RunLoop.main.getLoop()
			    .connect(x -> {
				    if (tick == 5) {
					    browser.properties.put(Box.hidden, true);
					    Drawing.dirty(this);
				    }
				    tick++;
				    return tick != 5;
			    });
		browser.setFocus(false);
		Drawing.dirty(browser);
	}

	public void runCommands() {
		browser.executeJavaScript("goCommands()");
		show();
	}

	public void center() {
		FieldBoxWindow window = this.find(Boxes.window, both())
					    .findFirst()
					    .get();
		Rect f = browser.properties.get(Box.frame);
		f.x = (window.getWidth() - f.w) / 2;
		f.y = (window.getHeight() - f.h) / 2;
		if (!browser.properties.isTrue(Box.hidden, false)) Drawing.dirty(this);
	}


	private static String readFile(String s, boolean append) {
		try (BufferedReader r = new BufferedReader(new FileReader(new File(s)))) {
			String line = "";
			while (r.ready()) {
				line += r.readLine() + "\n";
			}

			if (append) line += "\n//# sourceURL=" + s;
			return line;

		} catch (IOException e) {
			e.printStackTrace();
		}
		return "";
	}


	private String findAndLoad(String f, boolean append) {

		String[] roots = {Main.app + "/fielded/internal/", Main.app + "/fielded/external/", Main.app + "/fieldcef/internal"};
		for (String s : roots) {
			if (new File(s + "/" + f).exists()) return readFile(s + "/" + f, append);
		}
		Log.log("glassbrowser.error", "Couldnt' find file in playlist :" + f);
		return null;
	}

	private Stream<Box> selection() {
		return breadthFirst(both()).filter(x -> x.properties.isTrue(Mouse.isSelected, false));
	}

}
