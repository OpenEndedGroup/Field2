package fieldcef.plugins;

import fieldbox.boxes.Box;
import fieldbox.boxes.Mouse;
import fieldbox.boxes.plugins.PresentationMode;
import fieldbox.io.IO;
import fieldcef.browser.Browser;
import fielded.ServerSupport;
import fieldnashorn.annotations.HiddenInAutocomplete;

import java.util.Optional;
import java.util.stream.Stream;

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

	protected void enable() {

		if (selection().count()==1)
		{
			selection().forEach(x -> {
				String u = "http://localhost:" + find(ServerSupport.server, both()).findFirst().get().port+ "/id/" + x.properties.get(IO.id);
				editor.browser_.properties.put(Browser.url, u);
			});
		}
	}

	protected void disable() {
		String u = "http://localhost:" + find(ServerSupport.server, both()).findFirst().get().port+ "/init";
		editor.browser_.properties.put(Browser.url, u);

	}

	@HiddenInAutocomplete
	private Stream<Box> selection() {
		return breadthFirst(both()).filter(x -> x.properties.isTrue(Mouse.isSelected, false)).filter(x -> x != editor.browser_).filter(x -> x.properties.has(Box.name)).filter(x -> !x.properties.get(Box.name).equals("__texteditor__")).filter(x -> x != this);
	}


	@Override
	public void loaded() {

		editor = find(TextEditor.textEditor, this.both()).findFirst().get();
	}
}
