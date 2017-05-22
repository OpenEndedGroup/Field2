package fieldcef.plugins;

import field.app.RunLoop;
import field.utility.Log;
import fieldbox.boxes.Box;
import fieldbox.boxes.Drawing;
import fieldbox.boxes.Mouse;
import fieldbox.boxes.Watches;
import fieldbox.boxes.plugins.PresentationMode;
import fieldbox.io.IO;
import fieldcef.browser.Browser;
import fielded.ServerSupport;
import fieldnashorn.annotations.HiddenInAutocomplete;

import java.util.Optional;
import java.util.stream.Stream;

import static field.app.ThreadSync.enabled;

/**
 * Created by marc on 2/2/17.
 */
public class TextEditor_boxBrowser2 extends Box implements IO.Loaded {

	private final Box root;
	private TextEditor editor;

	public TextEditor_boxBrowser2(Box root) {
		this.root = root;

		this.properties.putToMap(PresentationMode.onEnterPresentationMode, "__enableTextEditor_browser", this::enable);
		this.properties.putToMap(PresentationMode.onExitPresentationMode, "__disableTextEditor_browser", this::disable);


	}

	boolean enabled = false;

	protected void enable() {
		enabled = true;
		if (selection().count() == 1) {
			selection().forEach(x -> {
				String u = "http://localhost:" + find(ServerSupport.server, both()).findFirst().get().port + "/id/" + x.properties.get(IO.id);
				editor.browser_.properties.put(Browser.url, u);
				RunLoop.main.delay(() -> {
					System.out.println(" delay called");
					editor.browser_.properties.put(Box.hidden, false);
					Drawing.dirty(editor.browser_);
				}, 500);
			});
		} else {
			String u = "http://localhost:" + find(ServerSupport.server, both()).findFirst().get().port + "/id/none";
			editor.browser_.properties.put(Browser.url, u);
			editor.browser_.properties.put(Box.hidden, false);
			Drawing.dirty(this);
		}
	}

	protected void disable() {
		enabled = false;
		String u = "http://localhost:" + find(ServerSupport.server, both()).findFirst().get().port + "/init";
		editor.browser_.properties.put(Browser.url, u);
		RunLoop.main.delay(() -> {
			editor.updateSize();
			Drawing.dirty(editor.browser_);
		}, 500);

	}

	@HiddenInAutocomplete
	private Stream<Box> selection() {
		return breadthFirst(both()).filter(x -> x.properties.isTrue(Mouse.isSelected, false)).filter(x -> x != editor.browser_).filter(x -> x.properties.has(Box.name)).filter(x -> !x.properties.get(Box.name).equals("__texteditor__")).filter(x -> x != this);
	}


	@Override
	public void loaded() {

		editor = find(TextEditor.textEditor, this.both()).findFirst().get();

		find(Watches.watches, both()).forEach(w -> {

			w.getQueue()
				.register(x -> x.equals("selection.changed"), c -> {
					Log.log("shy", () -> "selection is now" + selection().count());


					if (selection().count() == 1 && enabled) {
						enable();
					}

				});
		});
	}
}
