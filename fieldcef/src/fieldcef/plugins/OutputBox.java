package fieldcef.plugins;

import field.graphics.RunLoop;
 import field.utility.Dict;
 import field.utility.Log;
 import field.utility.Rect;
 import fieldagent.Main;
 import fieldbox.boxes.*;
 import fieldbox.io.IO;
 import fieldcef.browser.Browser;
 import fielded.Commands;
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
  * An OutputBox is a box that just lets you print html to it. This is a plugin that lets you make them.
  */
 public class OutputBox extends Box implements IO.Loaded {

	 static public final Dict.Prop<FunctionOfBox<Box>> newOutput = new Dict.Prop<FunctionOfBox<Box>>("newOutput").type()
														     .toCannon()
														     .doc("creates a new HTML output box, parented to this box");

	 private final Box root;

	 List<String> playlist = Arrays.asList("preamble.js", "jquery-2.1.0.min.js", "jquery.autosize.input.js", "modal.js");
	 String styleSheet = "field-codemirror.css";

	 public Browser browser;
	 public String styles;

	 public OutputBox(Box root) {
		 this.root = root;
		 this.properties.put(newOutput, x -> make(400, 300, x));
	 }

	 int tick = 0;

	 Commands commandHelper = new Commands();

	 @Override
	 public void loaded() {

	 }

	 public Box make(int w, int h, Box b) {
		 Log.log("OutputBox.debug", "initializing browser");
		 browser = new Browser();

		 Rect f = b.properties.get(frame);

		 float inset = 10;
		 browser.properties.put(Box.frame, new Rect(f.x + f.w - inset, f.y + f.h - inset, w, h));
		 browser.properties.put(Boxes.dontSave, true);
		 b.connect(browser);
		 browser.loaded();
		 this.properties.put(Boxes.dontSave, true);
		 styles = findAndLoad(styleSheet, false);

		 long[] t = {0};

		 boot();


		 find(Watches.watches, both()).findFirst()
					      .ifPresent(watch -> watch.addWatch(Box.frame, q -> {
						      if (q.second.equals(b)) {
							      float dx = q.fourth.x + q.fourth.w - q.third.x - q.third.w;
							      float dy = q.fourth.y + q.fourth.h - q.third.y - q.third.h;

							      if (Math.abs(dx) > 0 || Math.abs(dy) > 0) {
								      Rect fr = browser.properties.get(Box.frame);

								      fr.x += dx;
								      fr.y += dy;

								      browser.properties.put(Box.frame, fr);
								      Drawing.dirty(browser);
							      }

						      }
					      }));

		 return browser;
	 }

	 public void boot() {
		 Server s = this.find(ServerSupport.server, both())
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException(" Server not found "));


		 String bootstrap = "<html class='outputbox' style='background:rgba(0,0,0,0.2);'><head><style>" + styles + "</style></head><body class='outputbox' style='background:rgba(0,0,0,0.02);'></body></html>";
		 String res = UUID.randomUUID()
				  .toString();
		 s.setFixedResource("/" + res, bootstrap);
		 browser.properties.put(browser.url, "http://localhost:8080/" + res);


		 tick = 0;
		 RunLoop.main.getLoop()
			     .attach(x -> {
				     tick++;
				     if (browser.browser.getURL()
							.equals("http://localhost:8080/" + res)) {
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
		 //		 hide();

	 }
	 //
	 //	 public void show() {
	 //		 browser.properties.put(Box.hidden, false);
	 //		 browser.setFocus(true);
	 //		 Drawing.dirty(browser);
	 //	 }
	 //
	 //
	 //	 public void hide() {
	 //		 tick = 0;
	 //		 RunLoop.main.getLoop()
	 //			     .connect(x -> {
	 //				     if (tick == 5) {
	 //					     browser.properties.put(Box.hidden, true);
	 //					     Drawing.dirty(this);
	 //				     }
	 //				     tick++;
	 //				     return tick != 5;
	 //			     });
	 //		 browser.setFocus(false);
	 //		 Drawing.dirty(browser);
	 //	 }

	 //	 public void runCommands() {
	 //		 browser.executeJavaScript("goCommands()");
	 //		 show();
	 //	 }


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
