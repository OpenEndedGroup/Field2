package fieldcef.plugins;

import field.app.RunLoop;
import field.utility.*;
import fieldagent.Main;
import fieldbox.boxes.Box;
import fieldbox.boxes.Boxes;
import fieldbox.boxes.Drawing;
import fieldbox.boxes.Mouse;
import fieldbox.boxes.plugins.Templates;
import fieldbox.execution.Completion;
import fieldbox.execution.Execution;
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
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * An OutputBox is a box that just lets you print html to it. This is a plugin that lets you make them.
 */
public class OutputBox extends Box implements IO.Loaded {


	static public final Dict.Prop<FunctionOfBoxValued<TemplateMap<Browser>>> output = new Dict.Prop<>("output").type().doc("`_.output.blah.print('something')` will print to (and, if necessary, create) an html output box")
														   .toCannon();

	private final Box root;

	Out out = null;

	List<String> playlist = Arrays.asList("preamble.js", "jquery-2.1.0.min.js", "jquery.autosize.input.js", "modal.js");
	String styleSheet = "field-codemirror.css";

	public String styles;

	public OutputBox(Box root) {
		this.root = root;

		this.properties.put(output, (box) -> new TemplateMap<Browser>(box, "output", Browser.class, x -> make(400, 300, x)).makeCallable((map, param) -> {

			((Browser) map.asMap_get("default")).print(toHTML(param));

			return null;
		}));

	}

	private String toHTML(Object param) {
		if (out==null)
			return ""+param;
		else
			return out.convert(param);
	}

	int tick = 0;

	@Override
	public void loaded() {

		out = find(Out.__out, both()).findAny().orElseGet(() -> null);

		System.out.println(" found output as :"+out);

	}


	protected Browser make(int w, int h, Box b) {
		Log.log("OutputBox.debug", ()->"initializing browser");

		Browser browser = (Browser)find(Templates.templateChild, both()).findFirst()
								       .map(x -> x.apply(b, "html output"))
								       .orElseGet(() -> new Browser());

		Rect f = b.properties.get(frame);

		float inset = 10;
		browser.properties.put(Box.frame, new Rect(f.x + f.w - inset, f.y + f.h - inset, w, h));
		browser.properties.put(Boxes.dontSave, true);
		b.connect(browser);
		browser.loaded();
		this.properties.put(Boxes.dontSave, true);

		browser.properties.put(Box.name, "outputbox");
		styles = findAndLoad(styleSheet, false);

		long[] t = {0};

		boot(browser);


		browser.pauseForBoot();


		TextEditorExecution ed = new TextEditorExecution(browser);
		ed.connect(browser);

		return browser;
	}

	String postamble = "</body></html>";

	public void boot(Browser browser) {
		Server s = this.find(ServerSupport.server, both())
			       .findFirst()
			       .orElseThrow(() -> new IllegalArgumentException(" Server not found "));


		String bootstrap
			    = "<html class='outputbox' style='background:rgba(0,0,0,0.2);padding:8px;'><head><style>" + styles + "</style></head><body class='outputbox' style='background:rgba(0,0,0,0.02);'>" + postamble;
		String res = UUID.randomUUID()
				 .toString();
		s.setFixedResource("/" + res, bootstrap);
		browser.properties.put(browser.url, "http://localhost:"+s.port+"/" + res);


		tick = 0;
		RunLoop.main.getLoop()
			    .attach(x -> {
				    tick++;
				    if (browser.browser.getURL()
						       .equals("http://localhost:"+s.port+"/" + res)) {

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
				    Log.log("glassBrowser.boot", ()->"WAITING url:" + browser.browser.getURL());
				    Drawing.dirty(this);



				    return tick < 100;
			    });

		Drawing.dirty(this);
	}

	int ignoreHide = 0;

	public void inject2(Browser browser) {
		Log.log("glassbrowser.debug", ()->"inject 2 is happening");
		for (String s : playlist) {
			Log.log("glassbrowser.debug", ()->"executing :" + s);
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

		String[] roots = {Main.app + "/fielded/internal/", Main.app + "/fielded/external/", Main.app + "/fieldcef/internal"};
		for (String s : roots) {
			if (new File(s + "/" + f).exists()) return readFile(s + "/" + f, append);
		}
		Log.log("glassbrowser.error", ()->"Couldnt' find file in playlist :" + f);
		return null;
	}

	private Stream<Box> selection() {
		return breadthFirst(both()).filter(x -> x.properties.isTrue(Mouse.isSelected, false));
	}


	public class TextEditorExecution extends Execution {

		private final Browser delegate;
		private List<Runnable> queue;

		public TextEditorExecution(Browser delegate) {
			super(null);

			this.properties.put(Boxes.dontSave, true);
			this.delegate= delegate;

		}

		@Override
		public Execution.ExecutionSupport support(Box box, Dict.Prop<String> prop) {
			if (box instanceof Browser) return null;

			return wrap(box);
		}

		private Execution.ExecutionSupport wrap(Box box) {

			return new Execution.ExecutionSupport() {

				protected Util.ExceptionlessAutoCloasable previousPush = null;

				@Override
				public Object getBinding(String name) {
					return null;
				}

				@Override
				public void executeTextFragment(String textFragment, String suffix, Consumer<String> success, Consumer<Pair<Integer, String>> lineErrors) {

					delegate.executeJavaScript(textFragment);
				}


				@Override
				public void executeAll(String allText, Consumer<Pair<Integer, String>> lineErrors, Consumer<String> success) {
					executeTextFragment(allText, "", success, lineErrors);
				}

				@Override
				public String begin(Consumer<Pair<Integer, String>> lineErrors, Consumer<String> success, Map<String, Object> initiator) {
					//TODO
/*
					System.out.println(" WRAPPED (begin)");
					String name = s.begin(lineErrors, success);
					if (name == null) return null;
					Supplier<Boolean> was = box.properties.removeFromMap(Boxes.insideRunLoop, name);
					String newName = name.replace("main.", "editor.");
					box.properties.putToMap(Boxes.insideRunLoop, newName, was);
					box.first(IsExecuting.isExecuting).ifPresent(x -> x.accept(box, newName));

					return name;
					*/
					return null;
				}

				@Override
				public void end(Consumer<Pair<Integer, String>> lineErrors, Consumer<String> success) {
					//TODO
				}

				@Override
				public void setConsoleOutput(Consumer<String> stdout, Consumer<String> stderr) {
				}

				@Override
				public void completion(String allText, int line, int ch, Consumer<List<Completion>> results) {
//					tern.completion(x -> delegateTo.sendJavaScript(x), "remoteFieldProcess", allText, line, ch);
//					results.accept(Collections.emptyList());
				}

				@Override
				public void imports(String allText, int line, int ch, Consumer<List<Completion>> results) {
				}

				@Override
				public String getCodeMirrorLanguageName() {
					return "javascript";
				}

				@Override
				public String getDefaultFileExtension() {
					return ".js";
				}
			};
		}
	}
}
