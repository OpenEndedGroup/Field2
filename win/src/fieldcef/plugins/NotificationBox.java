package fieldcef.plugins;

import field.app.RunLoop;
import field.graphics.StandardFLineDrawing;
import field.linalg.Vec2;
import field.utility.Dict;
import field.utility.Log;
import field.utility.Rect;
import fieldagent.Main;
import fieldbox.boxes.*;
import fieldbox.io.IO;
import fieldcef.browser.Browser;
import fielded.ServerSupport;
import fielded.plugins.Out;
import fielded.webserver.Server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * An OutputBox is a box that just lets you print html to it. This is a plugin that lets you make them.
 */
public class NotificationBox extends Box implements IO.Loaded {

	static public final Dict.Prop<Consumer<String>> note = new Dict.Prop<>("note").type().toCannon().doc("...");
	static public final Dict.Prop<Runnable> clearNote = new Dict.Prop<>("clearNote").type().toCannon().doc("...");

	private final Box root;
	private Browser theBox;

	Out out = null;

	List<String> playlist = Arrays.asList("preamble.js", "jquery-2.1.0.min.js", "jquery.autosize.input.js", "modal.js");
	String styleSheet = "field-codemirror.css";

	public String styles;

	public NotificationBox(Box root) {
		this.root = root;
		this.properties.put(note, this::print);
		this.properties.put(clearNote, this::clear);
		this.properties.putToMap(Boxes.insideRunLoop, "main.__updateOpacity__", () -> {

			float next = 1f;
			theBox.properties.put(StandardFLineDrawing.opacity, next = 0.999f * theBox.properties.getOr(StandardFLineDrawing.opacity, () -> 1f));
			if (next < 0.9)
				theBox.properties.put(StandardFLineDrawing.opacity, next = 0.995f * theBox.properties.getOr(StandardFLineDrawing.opacity, () -> 1f));
			if (next < 0.5)
				theBox.properties.put(StandardFLineDrawing.opacity, next = 0.99f * theBox.properties.getOr(StandardFLineDrawing.opacity, () -> 1f));

			if (next < 0.11f) {
				theBox.properties.put(FLineDrawing.hidden, true);
			} else {
				theBox.properties.put(FLineDrawing.hidden, false);
				Drawing.dirty(this, 1);
			}

			return true;
		});
	}

	int tick = 0;

	@Override
	public void loaded() {
		theBox = make(400, 100);
	}

	static public final void notification(Box from, String html) {
		if (!html.endsWith("<br>")) html += "<br>";
		html = "<div class='notification'>"+html+"</div>";
		from.first(note, from.both()).orElse(x -> System.err.println(x)).accept(html);
	}
	public static void clearNotifications(Box from) {
		from.first(clearNote, from.both()).orElse(() -> {}).run();
	}



	protected Browser make(int w, int h) {
		Log.log("OutputBox.debug", () -> "initializing browser");

		Browser browser = new Browser();

		this.properties.put(FLineDrawing.layer, "glass");
		browser.properties.put(FLineDrawing.layer, "glass");

		Rect bounds = root.first(Drawing.drawing).map(x -> x.getCurrentViewBounds(root)).orElseGet(() -> new Rect(0, 0, 500, 500));

		float inset = 10;
		browser.properties.put(Box.frame, new Rect(bounds.x + inset, bounds.y + bounds.h - h - inset, w, h));
		browser.properties.put(Boxes.dontSave, true);

		browser.properties.put(Drawing.windowSpace, new Vec2(0, 1));

		root.connect(browser);
		browser.loaded();
		this.properties.put(Boxes.dontSave, true);

		browser.properties.put(Box.name, "outputbox");
		styles = findAndLoad(styleSheet, false);

		long[] t = {0};

		boot(browser);
		browser.pauseForBoot();

//		browser.printHTML("<div class='notification'>huh?</div>");

		return browser;
	}


	public void print(String note) {
		theBox.properties.put(StandardFLineDrawing.opacity, 1f);
		theBox.printHTML(note);
	}
	public void clear() {
		theBox.clear();
	}

	String postamble = "</body></html>";

	public void boot(Browser browser) {
		Server s = this.find(ServerSupport.server, both())
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException(" Server not found "));


		String bootstrap
			= "<html class='outputbox' style='background:rgba(0,0,0,0.2);padding:8px;'><head><style>" + styles + "</style></head><body class='outputbox' style='border-radius: 5px; background:rgba(0,0,0,0.02);'>" + postamble;
		String res = UUID.randomUUID()
			.toString();
		s.setFixedResource("/" + res, bootstrap);
		browser.properties.put(Browser.url, "http://localhost:" + s.port + "/" + res);


		tick = 0;
		RunLoop.main.getLoop()
			.attach(x -> {
				tick++;
				if (browser.browser.getURL()
					.equals("http://localhost:" + s.port + "/" + res)) {

					inject2(browser);
//					    try {
//						    Callbacks.call(browser, Callbacks.main, null);
//					    }
//					    catch(Throwable e)
//					    {
//						    e.printStackTrace();
//					    };
					return false;
				}
				Log.log("glassBrowser.boot", () -> "WAITING url:" + browser.browser.getURL());
				Drawing.dirty(this);


				return tick < 100;
			});

		Drawing.dirty(this);
	}

	int ignoreHide = 0;

	public void inject2(Browser browser) {
		Log.log("glassbrowser.debug", () -> "inject 2 is happening");
		for (String s : playlist) {
			Log.log("glassbrowser.debug", () -> "executing :" + s);
			browser.executeJavaScript(findAndLoad(s, true));
		}
		//		 hide();

		browser.finishBooting();

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

		String[] roots = {Main.app + "/modules/fieldcore/resources/"};
		for (String s : roots) {
			if (new File(s + "/" + f).exists()) return readFile(s + "/" + f, append);
		}
		Log.log("glassbrowser.error", () -> "Couldnt' find file in playlist :" + f);
		return null;
	}

	private Stream<Box> selection() {
		return breadthFirst(both()).filter(x -> x.properties.isTrue(Mouse.isSelected, false));
	}

}
