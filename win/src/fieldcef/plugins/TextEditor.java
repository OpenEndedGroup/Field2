package fieldcef.plugins;

import static org.lwjgl.glfw.GLFW.*;

import field.app.RunLoop;
import field.graphics.Window;
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
import fielded.ServerSupport;
import fieldnashorn.annotations.HiddenInAutocomplete;
import org.lwjgl.glfw.GLFW;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This is a browser, created by default, that embeds the text editor. We're checking for latency here
 */
public class TextEditor extends Box implements IO.Loaded {

	static public final Dict.Prop<TextEditor> textEditor = new Dict.Prop<>("textEditor").toCannon()
		.type()
		.doc("The TextEditor that is stuck in front of the window, in window coordinates");
	private final Box root;
	@HiddenInAutocomplete
	public Browser browser;
	@HiddenInAutocomplete
	public String styles;
	String styleSheet = "field-codemirror.css";

	// we'll need to make sure that this is centered on larger screens
	int maxw = 900;
	int maxh = 900;
	int heightLast = -1;
	int tick = 0;
	Commands commandHelper = new Commands();
	long lastTriggerAt = -1;
	int ignoreHide = 0;
	private int maxhOnCreation = 0;
	private String setHeightCode = "";
	private String setWidthCode = "";

	public TextEditor(Box root) {
		this.properties.put(textEditor, this);
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

	int frameLast = 0;

	@HiddenInAutocomplete
	public void loaded() {
		Log.log("texteditor.debug", () -> "initializing browser");

		RunLoop.main.delay(() -> {

			FieldBoxWindow window = this.find(Boxes.window, this.both())
				.findFirst()
				.get();

			Drawing drawing = root.first(Drawing.drawing)
				.orElseThrow(() -> new IllegalArgumentException(" can't install text-drawing into something without drawing support"));


			maxh = window.getHeight() - 25 - 10 - 10 - 2;

			browser = new Browser();

			Vec2 v = drawing.windowSystemToDrawingSystem(new Vec2(window.getWidth() - maxw - 10, 10));
			Vec2 vd = drawing.windowSystemToDrawingSystemDelta(new Vec2(maxw, 1080 * 1));

			frameLast = (int) vd.x;
			browser.properties.put(Box.frame, new Rect(v.x, v.y, vd.x, vd.y));

			maxhOnCreation = 1080 * 1;

			browser.pauseForBoot();

			this.properties.put(FLineDrawing.layer, "glass");
			browser.properties.put(FLineDrawing.layer, "glass");

			browser.properties.put(Drawing.windowSpace, new Vec2(1, 0));
			browser.properties.put(Drawing.windowScale, new Vec2(1, 1));

			browser.properties.put(Boxes.dontSave, true);
			browser.properties.put(Box.hidden, true);
			browser.properties.put(Mouse.isSticky, true);

			browser.properties.put(Box.undeletable, true);

			browser.connect(root);
			browser.loaded();

			browser.properties.put(Box.name, "__texteditor__");

			executeJavaScript("$(\".CodeMirror\").height(" + (maxh - 10) + ")");
			executeJavaScript("$(\".CodeMirror\").width(" + (maxw - 28 * 2) + ")");

			this.properties.put(Boxes.dontSave, true);
			styles = findAndLoad(styleSheet, false);

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


			find(Watches.watches, both()).forEach(w -> {

				w.getQueue()
					.register(x -> x.equals("selection.changed"), c -> {

						Log.log("shy", () -> "selection is now" + selection().count());

						if (!pinned) {
							browser.executeJavaScript("_messageBus.publish('defocus', {})");
							browser.setFocus(false);
						}

						if (selection().count() != 1 && !pinned) {
							browser.properties.put(Box.hidden, true);
							Drawing.dirty(this);
						} else {

							executeJavaScript(setHeightCode);
							executeJavaScript(setWidthCode);

							browser.properties.put(Box.hidden, false);
							Drawing.dirty(this);
						}

					});

			});

			first(Boxes.window, both()).ifPresent(x -> x.addKeyboardHandler(event -> {
				Set<Integer> kpressed = Window.KeyboardState.keysPressed(event.before, event.after);
				if ((kpressed.contains(GLFW_KEY_LEFT_SHIFT) || kpressed.contains(GLFW_KEY_RIGHT_SHIFT)) && event.after.keysDown.size() == 1) {
					trigger();
				} else if (event.after.keysDown.size() == 1) {
					lastTriggerAt = 0;
				}

				return true;
			}));

			RunLoop.main.getLoop()
				.attach(x -> {

					int maxh = window.getHeight();// - 25 - 10 - 10 - 2;
					Rect f = browser.properties.get(Box.frame);

					f = f.duplicate();

					Vec2 d1 = drawing.drawingSystemToWindowSystem(new Vec2(f.x, f.y));
					Vec2 d2 = drawing.drawingSystemToWindowSystem(new Vec2(f.x + f.w, f.y + f.h));

					f.x = (float) d1.x;
					f.y = (float) d1.y;
					f.w = (float) (d2.x - d1.x);
					f.h = (float) (d2.y - d1.y);



					if ((int) f.h != heightLast) {
						heightLast = (int) f.h;
						f = f.duplicate();

						System.out.println(" h = "+Math.min(f.h, maxhOnCreation - 40));

						setHeightCode = "$(\"body\").height(" + Math.min(f.h*0.83, maxhOnCreation - 40) + ");cm.refresh();";
						setHeightCode += "$(\".CodeMirror\").height(" + Math.min(f.h*0.83, maxhOnCreation - 40) + ");cm.refresh();";
						executeJavaScript(setHeightCode);
					}


					if ((int) f.w != frameLast) {
						frameLast = (int) f.w;
						f = f.duplicate();

						setWidthCode = "$(\"body\").width(" + Math.min(maxw - 28 * 2, (int) (f.w - 28)) + ");cm.refresh();";
						executeJavaScript(setWidthCode);

					}


					return true;
				});
		}, 100);
	}


	@HiddenInAutocomplete
	public void trigger() {
		long now = System.currentTimeMillis();
		if (now - lastTriggerAt < 500) {
			if (!browser.getFocus()) browser.executeJavaScript_queued("_messageBus.publish('focus', {})");
			browser.setFocus(!browser.getFocus());
		}
		lastTriggerAt = now;
	}

	@HiddenInAutocomplete
	public void boot() {
		browser.properties.put(Browser.url, "http://localhost:" + find(ServerSupport.server, both()).findFirst().get().port + "/init");
		Drawing.dirty(this);
		browser.finishBooting();
	}

	@HiddenInAutocomplete
	public void show() {
//		System.out.println(" showing because show() called ");
		browser.properties.put(Box.hidden, false);
		browser.setFocus(true);
		Drawing.dirty(browser);
	}

	@HiddenInAutocomplete
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

	@HiddenInAutocomplete
	public void runCommands() {
		browser.executeJavaScript("goCommands()");
		show();
	}

	@HiddenInAutocomplete
	public void center() {
		FieldBoxWindow window = this.find(Boxes.window, both())
			.findFirst()
			.get();
		Rect f = browser.properties.get(Box.frame);
		f.x = (int) ((window.getWidth() - f.w) / 2);
		f.y = (int) ((window.getHeight() - f.h) / 2);
		if (!browser.properties.isTrue(Box.hidden, false)) Drawing.dirty(this);
	}

	@HiddenInAutocomplete
	private String findAndLoad(String f, boolean append) {

		String[] roots = {Main.app + "/modules/fieldcore/resources/", Main.app + "/modules/fieldcef_macosx/resources/"};
		for (String s : roots) {
			if (new File(s + "/" + f).exists()) return readFile(s + "/" + f, append);
		}
		Log.log("glassbrowser.error", () -> "Couldnt' find file in playlist :" + f);
		return null;
	}

	@HiddenInAutocomplete
	private Stream<Box> selection() {
		return breadthFirst(both()).filter(x -> x.properties.isTrue(Mouse.isSelected, false)).filter(x -> x != browser).filter(x -> x.properties.has(Box.name)).filter(x -> !x.properties.get(Box.name).equals("__texteditor__")).filter(x -> x != this);
	}


	/**
	 * Injects css into the text editor. For example '_.textEditor.injectCSS("body {font-size:20px;}"' will give you a markedly bigger font.
	 */
	public void injectCSS(String css) {
		browser.injectCSS(css);
	}

	/**
	 * Executes some javascript directly in the text editor. For larger amounts of TextEditor coding, mark a box as "Bridge to Editor" with the command menu.
	 */
	public void executeJavaScript(String js) {
		browser.executeJavaScript_queued(js);
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

	boolean pinned = false;

	public void pin() {
		pinned = true;
	}

	public void unpin() {
		pinned = false;
	}

	public boolean isPinned() {
		return pinned;
	}
}
