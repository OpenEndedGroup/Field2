package fieldbox.boxes.plugins;

import field.linalg.Vec2;
import field.utility.Dict;
import field.utility.Rect;
import fieldbox.FieldBox;
import fieldbox.boxes.Box;
import fieldbox.boxes.Drawing;
import fieldbox.boxes.Drops;
import fieldbox.io.IO;
import fielded.Execution;

import java.io.File;

/**
 * Created by marc on 7/3/14.
 */
public class DragFilesToCanvas extends Box {

	private Box root;

	public DragFilesToCanvas(Box root) {
		this.root = root;

		this.properties.putToList(Drops.onDrop, (d) -> {
			drop(d.after.files, d.after.mouseState.position().orElseGet(() -> new Vec2(0, 0)));
		});
	}

	private void drop(String[] files, Vec2 position) {

		position = new Vec2(position);
		for (String f : files) {

			//TODO, we're assuming this is a textfile full of code, but if it was a .box file it could link to many files instead (think about a box with a complete shader in it. For that we'd want to call stuff inside IO directly.

			Box b1 = new Box();
			root.connect(b1);
			float w = 25;
			b1.properties.put(frame, new Rect(position.x - w * 4, position.y - w, w * 8, w * 2));
			b1.properties.put(Box.name, new File(f).getParentFile().getName() + "/" + new File(f).getName());
			Drawing.dirty(b1);

			position.y += w + 5;

			//TODO what if there's already a sidecar .box file. Need to read that in a set properties (a job for IO)
			//TODO what property to set given a file? Look at the file extension... Here we just assume it's code, but we should use IO

			b1.properties.put(Execution.code, IO.readFromFile(new File(f)));

			String ff = new File(f).getAbsolutePath();
			if (ff.startsWith(FieldBox.fieldBox.io.getDefaultDirectory())) {
				ff = IO.WORKSPACE + "/" + ff.substring(FieldBox.fieldBox.io.getDefaultDirectory().length());
			}
			b1.properties.put(new Dict.Prop<String>("__filename__" + Execution.code), ff);
		}
	}

}
