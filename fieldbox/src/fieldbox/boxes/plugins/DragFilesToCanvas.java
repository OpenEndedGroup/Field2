package fieldbox.boxes.plugins;

import field.graphics.Window;
import field.linalg.Vec2;
import field.utility.Dict;
import field.utility.Log;
import field.utility.Rect;
import fieldbox.FieldBox;
import fieldbox.boxes.Box;
import fieldbox.boxes.Callbacks;
import fieldbox.boxes.Drawing;
import fieldbox.boxes.Drops;
import fieldbox.execution.Execution;
import fieldbox.io.IO;
import fielded.RemoteEditor;

import java.io.File;
import java.util.*;

/**
 * Plugin that lets you drag files onto the canvas. Files are now kept out of the workspace.
 */
public class DragFilesToCanvas extends Box {

	private Box root;

	public DragFilesToCanvas(Box root) {
		this.root = root;

		this.properties.putToMap(Drops.onDrop, "__dragfiles__", (d) -> {
			if (d.properties.isTrue(Window.consumed, false)) return;
			if (drop(d.after.files, d.after.mouseState.position().orElseGet(() -> new Vec2(0, 0))))
			{
				d.properties.put(Window.consumed, true);
			}
		});
	}

	private boolean drop(String[] files, Vec2 position) {

		boolean r = false;
		position = new Vec2(position);
		List<Box> loaded = new ArrayList<>();
		for (String f : files) {

			boolean isPartOfWorkspace = false;
			String ff = new File(f).getAbsolutePath();
			if (ff.startsWith(FieldBox.fieldBox.io.getDefaultDirectory())) {
				ff = IO.WORKSPACE + "/" + ff.substring(FieldBox.fieldBox.io.getDefaultDirectory().length());
				Log.log("drags", "filename is part of workspace "+ff);
				isPartOfWorkspace = true;
			}

			if (ff.startsWith(FieldBox.fieldBox.io.getTemplateDirectory())) {
				ff = IO.TEMPLATES + "/" + ff.substring(FieldBox.fieldBox.io.getTemplateDirectory().length());
				Log.log("drags", "filename is part of workspace "+ff);
				isPartOfWorkspace = true;
			}


			Box b1;

			if (FieldBox.fieldBox.io.isBoxFile(f))
			{
				b1  = loadBox(f, position);
				r = true;
			}
			else if (FieldBox.fieldBox.io.isFieldFile(f))
			{
				Set<Box> ll =  loadBoxes(f, position);
				loaded.addAll(ll);

				Vec2 center = new Vec2();
				ll.stream().map(x -> x.properties.get(Box.frame)).filter(x -> x!=null).forEach(x-> center.add(new Vec2(x.x+x.w/2, x.y+x.h/2)));
				center.mul(1f/ll.size());

				final Vec2 finalPosition = position;
				ll.stream().map(x -> x.properties.get(Box.frame)).filter(x -> x != null).forEach(x -> {
					x.x -= center.x;
					x.x += finalPosition.x;
					x.y -= center.x;
					x.y += finalPosition.y;
				});

				continue;
			} else {

				b1 = new Box();
				root.connect(b1);

				Dict.Prop<String> code = FieldBox.fieldBox.io.lookupFileSuffix(f, root);

				Log.log("drags", "looked up code suffix for " + f + " got " + code);

				if(code==null) code = Execution.code;
				else {
					Log.log("drags", "set default editor property to "+code);
					b1.properties.put(RemoteEditor.defaultEditorProperty, code.getName());
				}
				b1.properties.put(code, IO.readFromFile(new File(f)));


				Log.log("drags", "final filename is :"+ff);
				b1.properties.put(new Dict.Prop<String>("__filename__" + code.getName()), ff);
				b1.properties.put(Box.name, (isPartOfWorkspace ? ""  : new File(f).getParentFile().getName() + "/") + new File(f).getName());

			}
			float w = 25;
			b1.properties.put(frame, new Rect(position.x - w * 4, position.y - w, w * 8, w * 2));
			Drawing.dirty(b1);

			position.y += w + 5;

			loaded.add(b1);
			r = true;
		}

		loaded.forEach(x -> {
			IO.uniqify(x);
			if (x instanceof IO.Loaded) ((IO.Loaded) x).loaded();
			Callbacks.load(x);
		});
		return r;
	}

	private Set<Box> loadBoxes(String f, Vec2 position) {
		Map<String, Box> special = new LinkedHashMap<>();
		special.put(">>root<<", root);
		Set<Box> created = new LinkedHashSet<Box>();
		IO.Document doc = FieldBox.fieldBox.io.readDocument(f, special, created);
		created.forEach(x -> IO.uniqify(x));
		return created;
	}

	private Box loadBox(String f, Vec2 position) {

		Box b = FieldBox.fieldBox.io.loadSingleBox(f, root);

		root.connect(b);
		Drawing.dirty(b);

		return b;
	}

}
