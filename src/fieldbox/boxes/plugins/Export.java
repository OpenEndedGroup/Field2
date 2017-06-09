package fieldbox.boxes.plugins;

import field.utility.Dict;
import field.utility.Log;
import fieldbox.FieldBox;
import fieldbox.Open;
import fieldbox.boxes.Box;
import fieldbox.boxes.Drawing;
import fieldbox.io.IO;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * Plugin to let you "save as" and "package to new Workspace"
 */
public class Export extends Box {

	public static final Dict.Prop<BiFunction<String, String, Boolean>> exportToWorkspace = new Dict.Prop("exportToWorkspace")
		.type().doc("`_.exportToWorkspace('someDir/somewhere', 'great.field2')` exports this whole document to a new workspace, saving it as `great.field2` and copying all the files into it");

	Box root;

	public Export(Box root) {
		this.root = root;

		this.properties.put(exportToWorkspace, (d, f) -> exportToNewWorkspace(d, f, null));
	}

	public boolean exportToNewWorkspace(String workspaceDir, String filename, String prefix) {
		try {
			String was = FieldBox.fieldBox.io.getDefaultDirectory();
			try {
				FieldBox.fieldBox.io.setDefaultDirectory(workspaceDir);

				save(filename, prefix);

				return true;
			} finally {
				try {
					FieldBox.fieldBox.io.setDefaultDirectory(was);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	private void save(String filename, String prefix) {
//		if (filename.endsWith(".field2"))
		{

			Log.println("io.debug", " saving .... ");
			Map<Box, String> special = new LinkedHashMap<>();
			special.put(root, ">>root<<");

			String path = "";
			String fn = filename;
			if (filename.contains("/")) {

				path = filename.substring(0, filename.lastIndexOf("/"));
				fn = filename.substring(filename.lastIndexOf("/") + 1);
			}

			IO.Document doc = FieldBox.fieldBox.io.compileDocument(path, root, special);

			boolean error = false;
			try {
				FieldBox.fieldBox.io.writeOutDocument(IO.WORKSPACE + "/" + path + "/" + fn, doc);
			} catch (IOException e) {
				e.printStackTrace();
				Drawing.notify("Error saving " + e.getMessage(), this, 200);
				error = true;
			}

			if (!error) {
				Log.println("io.debug", " going to notify ...");
				Drawing.notify("Saved to " + filename, this, 200);
				Log.println("io.debug", " ... notified ");
			}
		}
	}

}
