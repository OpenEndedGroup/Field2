package fieldbox.boxes.plugins;

import field.graphics.Shader;
import field.utility.Dict;
import field.utility.Pair;
import fieldbox.FieldBox;
import fieldbox.boxes.Box;
import fieldbox.execution.Execution;
import fieldbox.io.IO;
import fielded.Commands;
import fielded.RemoteEditor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registers a property as editable
 * Created by marc on 1/8/17.
 */
public class AsEditable extends Box {

	static public final Dict.Prop<Box.TriFunctionOfBoxAnd<String, String, Boolean>> makeEditable = new Dict.Prop<>("makeEditable").doc("makes a property editable as source code in the editor. Specifically, `_.makeEditable('luaSource', 'lua') will make a property called `_.luaSource` editable, and the source code will be stored in files with the `.custom.lua` file extension and syntax highlighted as Lua.").toCanon();

	private final Box root;

	Map<String, String> knownModes = new LinkedHashMap<>();

	public AsEditable(Box root) {
		this.root = root;

		// file extensions to official CodeMirror language names
		knownModes.put("html", "htmlmixed");
		knownModes.put("js", "javascript");

		this.properties.put(makeEditable, (bx, propName, ext) -> {

			Dict.Prop<String> p = new Dict.Prop<>(propName);
			FieldBox.fieldBox.io.addFilespec(propName, ".custom." + ext, knownModes.getOrDefault(ext, ext));
			p.toCanon().set(IO.persistent, true);

			bx.properties.put(Commands.commands, () -> {
				Map<Pair<String, String>, Runnable> m = new LinkedHashMap<>();
				RemoteEditor ed = this.find(RemoteEditor.editor, both()).findFirst().get();

				Box box = ed.getCurrentlyEditing();
				Dict.Prop<String> cep = ed.getCurrentlyEditingProperty();

				if (box == null) return m;

				if (!cep.equals(p))
					m.put(new Pair<>("Edit " + p.getName() + "", "Switch to editing the "+p.getName()+" property in this box"), () -> {
						ed.setCurrentlyEditingProperty(p);
					});

				//GraphicsSupport has the command for switching back

				return m;
			});
			return true;
		});


	}


}
