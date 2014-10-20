package fieldcef.plugins;

import field.graphics.RunLoop;
import field.linalg.Vec2;
import field.utility.Dict;
import field.utility.Log;
import field.utility.Rect;
import fieldagent.Main;
import fieldbox.boxes.*;
import fieldbox.io.IO;
import fieldbox.ui.FieldBoxWindow;
import fieldcef.browser.Browser;
import fielded.Commands;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * This is a browser, created by default, that embeds the text editor. We're checking for latency here
 */
public class TextEditor extends Box implements IO.Loaded {

	static public final Dict.Prop<TextEditor> textEditor = new Dict.Prop<>("textEditor").toCannon()
											    .type()
											    .doc("The TextEditor that is stuck in front of the window, in window coordinates");
	private final Box root;

	List<String> playlist = Arrays.asList("preamble.js", "jquery-2.1.0.min.js", "jquery.autosize.input.js", "modal.js");
	String styleSheet = "field-codemirror.css";

	// we'll need to make sure that this is centered on larger screens
	int maxw = 800;
	int maxh = 900;
	public Browser browser;
	public String styles;

	public TextEditor(Box root) {
		this.properties.put(textEditor, this);
		this.root = root;
	}

	int tick = 0;

	Commands commandHelper = new Commands();

	public void loaded() {
		Log.log("texteditor.debug", "initializing browser");

		FieldBoxWindow window = this.find(Boxes.window, this.both())
						    .findFirst()
						    .get();

		maxh = window.getHeight()-25-10-10-10-50;

		browser = new Browser();
		browser.properties.put(Box.frame, new Rect(window.getWidth()-maxw-10, 10, maxw, maxh));
		browser.properties.put(FLineDrawing.layer, "glass");
		browser.properties.put(Drawing.windowSpace, new Vec2(1,0));
		browser.properties.put(Boxes.dontSave, true);
		browser.properties.put(Box.hidden, false);
		browser.properties.put(Mouse.isSticky, true);

		browser.connect(root);
		browser.loaded();
		this.properties.put(Boxes.dontSave, true);
		styles = findAndLoad(styleSheet, false);
//		browser.properties.removeFromMap(FLineDrawing.frameDrawing, "__outline__");

		long[] t = {0};
		RunLoop.main.getLoop()
			    .attach(x -> {
				    if (t[0] == 0) t[0] = System.currentTimeMillis();
				    if (System.currentTimeMillis() - t[0] > 1000) {
					    boot();

					    return false;
				    }
				    return true;
			    });

//		find(Watches.watches, both()).map(w -> w.addWatch(Mouse.isSelected, q -> {
//			if (q.second == browser) {
//				browser.setFocus(q.fourth);
//			}
//		}));

	}

	public void boot() {
		browser.properties.put(browser.url, "http://localhost:8080/init");
		Drawing.dirty(this);
	}

	int ignoreHide = 0;

	public void show() {
		browser.properties.put(Box.hidden, false);
		browser.setFocus(true);
		Drawing.dirty(browser);
	}


	public void hide() {
		tick = 0;
		RunLoop.main.getLoop()
			    .attach(x -> {
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
		f.x = (int) ((window.getWidth() - f.w) / 2);
		f.y = (int) ((window.getHeight() - f.h) / 2);
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
