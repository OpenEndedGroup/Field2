package fieldbox.boxes.plugins;

import field.utility.Dict;
import fieldbox.FieldBox;
import fieldbox.boxes.Box;

/**
 * Let's you edit the plugin file (~/.field/plugins.edn). This is the smallest possible plugin, it's all static. But these definitions allow you to
 * edit EDN and .txt files directly in Field by dragging them onto the canvas.
 * <p>
 * Later on we might have some UI for automatically adding a box for some of the more common config files and a reload action for them.
 */
public class PluginsPlugin extends Box {

	static public final Dict.Prop<String> edn = new Dict.Prop<String>("edn").toCanon().doc("EDN (or Field configuration file)").type();
	static public final Dict.Prop<String> txt = new Dict.Prop<String>("txt").toCanon().doc("TXT (or Field configuration file)").type();

	static {
		FieldBox.fieldBox.io.addFilespec("edn", "edn", "clojure");
		FieldBox.fieldBox.io.addFilespec("txt", "txt", "text");
	}

	public PluginsPlugin(Box root) {

	}
}
