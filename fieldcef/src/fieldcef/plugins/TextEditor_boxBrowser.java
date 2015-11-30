package fieldcef.plugins;

import com.badlogic.jglfw.Glfw;
import field.app.RunLoop;
import field.graphics.Window;
import field.linalg.Vec2;
import field.utility.Dict;
import field.utility.Log;
import field.utility.Rect;
import fieldagent.Main;
import fieldbox.boxes.*;
import fieldbox.boxes.plugins.PresentationMode;
import fieldbox.io.IO;
import fieldbox.ui.FieldBoxWindow;
import fieldcef.browser.Browser;
import fielded.Commands;
import fielded.ServerSupport;
import fieldnashorn.annotations.HiddenInAutocomplete;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * A varient of the text editor that can take over when html resources are being browsed. The text editor carries too much state to be rapidly booted up, so we basically open a new tab
 */
public class TextEditor_boxBrowser extends Box implements IO.Loaded {

	static public final Dict.Prop<TextEditor_boxBrowser> textEditor_boxBrowser = new Dict.Prop<>("textEditor_boxBrowser").toCannon()
															     .type()
															     .doc("The TextEditor browser variant  that is stuck in front of the window, in window coordinates");
	private final Box root;
	@HiddenInAutocomplete
	public Browser browser;
	@HiddenInAutocomplete
	public String styles;
	List<String> playlist = Arrays.asList("preamble.js", "jquery-2.1.0.min.js", "jquery.autosize.input.js", "modal.js");
	String styleSheet = "field-codemirror.css";
	// we'll need to make sure that this is centered on larger screens
	int maxw = 800;
	int maxh = 900;
	int heightLast = 0;
	int tick = 0;
	Commands commandHelper = new Commands();
	long lastTriggerAt = -1;
	int ignoreHide = 0;
	private int maxhOnCreation = 0;
	private TextEditor textEditor;
	private Box prevSelection;


	public TextEditor_boxBrowser(Box root) {
		this.properties.put(textEditor_boxBrowser, this);
		this.root = root;
	}

	@HiddenInAutocomplete
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

	int lastWidth = 0;

	@HiddenInAutocomplete
	public void loaded() {
		Log.log("texteditor.debug", ()->"initializing browser");

		textEditor = this.find(TextEditor.textEditor, this.both())
				 .findFirst()
				 .orElseThrow(() -> new IllegalArgumentException("can't find embedded text editor"));


		FieldBoxWindow window = this.find(Boxes.window, this.both())
					    .findFirst()
					    .get();

		Drawing drawing = root.first(Drawing.drawing)
				      .orElseThrow(() -> new IllegalArgumentException(" can't install text-drawing into something without drawing support"));


		maxh = window.getHeight() - 25 - 10 - 10 - 2;


		browser = new Browser();

		Vec2 v = drawing.windowSystemToDrawingSystem(new Vec2(window.getWidth() - maxw - 10, 10));
		Vec2 vd = drawing.windowSystemToDrawingSystemDelta(new Vec2(maxw, 1500));

		browser.properties.put(Box.frame, new Rect(v.x, v.y, vd.x, vd.y));

		lastWidth = (int)vd.x;

		maxhOnCreation = 1500;


		this.properties.put(FLineDrawing.layer, "glass");

		browser.properties.put(FLineDrawing.layer, "glass");
		browser.properties.put(Drawing.windowSpace, new Vec2(1, 0));
		browser.properties.put(Boxes.dontSave, true);
		browser.properties.put(Box.hidden, true);
		browser.properties.put(Mouse.isSticky, true);

		browser.properties.put(FrameManipulation.lockHeight, true);
//		browser.properties.put(FrameManipulation.lockWidth, true);
//		browser.properties.put(FrameManipulation.lockX, true);
		browser.properties.put(FrameManipulation.lockY, true);

		browser.properties.put(Box.undeletable, true);

		browser.properties.put(Box.name, "texteditor_boxBrowser");

		browser.connect(root);
		browser.loaded();

		this.properties.put(Boxes.dontSave, true);


		find(Watches.watches, both()).forEach(w -> {

			w.getQueue()
			 .register(x -> x.equals("selection.changed"), c -> {
				 Log.log("shy", () -> "selection is now" + selection().count());


				 if (selection().count() != 1) {
					 browser.properties.put(Box.hidden, true);
					 Drawing.dirty(this);
					 prevSelection = null;

				 } else {
					 if (prevSelection != selection().findFirst()
									 .get()) {

						 prevSelection = selection().findFirst()
									    .get();

						 if (enabled) {
							 String u = "http://localhost:" + ServerSupport.webserverPort + "/id/" + prevSelection.properties.get(IO.id);
							 setURL(u, () -> {
								 browser.properties.put(Box.hidden, false);
								 Drawing.dirty(this);
								 browser.executeJavaScript("$(\"body\").height( " + Math.min(maxh, maxhOnCreation - 40) + ")");
								 browser.executeJavaScript("$(\"body\").width(" + lastWidth + ")");
							 });
						 } else {
							 browser.properties.put(Box.hidden, true);
						 }
					 }
				 }

			 });
		});

		RunLoop.main.getLoop()
			    .attach(x -> {

				    int maxh = window.getHeight() - 25 - 10 - 10 - 2;
				    Rect f = browser.properties.get(Box.frame);


				    if ((int)f.h != heightLast) {
					    f = f.duplicate();
					    heightLast = (int) f.h;
					    browser.executeJavaScript("$(\"body\").height( " + Math.min(maxh, maxhOnCreation - 40) + ")");
				    }

				    if ((int)f.w != lastWidth)
				    {
					    browser.executeJavaScript("$(\"body\").width(" + lastWidth + ")");
					    lastWidth =  (int)f.w;
				    }

				    return true;
			    });

		this.properties.putToMap(PresentationMode.onEnterPresentationMode, "__enableTextEditor_browser", this::enable);
		this.properties.putToMap(PresentationMode.onExitPresentationMode, "__disableTextEditor_browser", this::disable);

	}

	boolean enabled = false;

	public void enable() {
		enabled = true;
		if (prevSelection != null) {
			String u = "http://localhost:" + ServerSupport.webserverPort + "/id/" + prevSelection.properties.get(IO.id);
			System.out.println(" setting url to be :" + u);
			setURL(u, () -> {
				browser.properties.put(Box.hidden, false);
				Drawing.dirty(this);
				browser.executeJavaScript("$(\"body\").height( " + Math.min(maxh, maxhOnCreation - 40) + ")");
				browser.executeJavaScript("$(\"body\").width(" + lastWidth + ")");
			});
		}
		textEditor.hide();
		textEditor.disconnected = true;
		show();
	}

	public void disable() {
		hide();
		enabled = false;
		textEditor.disconnected = false;
	}


	@HiddenInAutocomplete
	public void show() {
		if (!enabled) hide();
		else {
			browser.properties.put(Box.hidden, false);
			browser.setFocus(true);
			Drawing.dirty(browser);
		}

	}

	@HiddenInAutocomplete
	public void hide() {
		tick = 0;
		browser.properties.put(Box.hidden, true);
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
		textEditor.show();
	}


	@HiddenInAutocomplete
	private Stream<Box> selection() {
		return breadthFirst(both()).filter(x -> x.properties.isTrue(Mouse.isSelected, false));
	}


	/**
	 * reloads this text editor. Useful if you are hacking on the CSS or JavaScript that backs the editor
	 */
	public void reload() {
		browser.reload();
	}


	public void setURL(String url) {
		browser.properties.put(Browser.url, url);
	}

	public void setURL(String url, Runnable callback) {
		String was = browser.properties.get(Browser.url);
		// if (was==url) then there will be no reload

		if (was!=null && was.equals(url))
		{
			callback.run();
		}
		else {
			browser.properties.put(Browser.url, url);
			browser.callbackOnNextReload = callback;
		}
	}
}

