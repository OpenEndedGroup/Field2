package fieldbox.boxes.plugins;

import field.utility.Dict;
import field.utility.Pair;
import fieldbox.FieldBox;
import fieldbox.boxes.Box;
import fieldbox.io.IO;
import fielded.Commands;
import fielded.RemoteEditor;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by marc on 4/15/15.
 */
public class InternalCSS extends Box {

	static public final Dict.Prop<String> css = new Dict.Prop<>("css").toCannon();

	static
	{
		IO.persist(css);
		FieldBox.fieldBox.io.addFilespec("css", ".css", "css");
	}

	public InternalCSS()
	{
		properties.put(Commands.commands, () -> {
			Map<Pair<String, String>, Runnable> m = new LinkedHashMap<>();
			RemoteEditor ed = this.find(RemoteEditor.editor, both()).findFirst().get();

			Box box = ed.getCurrentlyEditing();
			Dict.Prop<String> cep = ed.getCurrentlyEditingProperty();

			if (box == null) return m;

			if (!cep.equals(css) && (box.first(css, box.upwards()).isPresent()))
				m.put(new Pair<>("Edit <i>CSS</i>", "Switch to editing css file associated with this box. You can inject this css into a browser with `_.injectCSS(_.css)`. So, for example, `_.textEditor.injectCSS(_.css)` will dump the css in this box into the `_.textEditor`"), () -> {
					ed.setCurrentlyEditingProperty(css);
				});
			return m;
		});
	}

}
