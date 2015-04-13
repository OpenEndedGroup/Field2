package fieldbox.boxes.plugins;

import field.linalg.Vec2;
import field.utility.Dict;
import field.utility.Rect;
import fieldbox.boxes.Box;
import fieldbox.boxes.Callbacks;
import fieldbox.boxes.Drawing;
import fieldbox.io.IO;

/**
 * Created by marc on 3/24/15.
 */
public class Templates extends Box {

	static public final Dict.Prop<Box.BiFunctionOfBoxAnd<String, Box>> templateChild = new Dict.Prop<Box.BiFunctionOfBoxAnd<Class, Box>>("templateChild").toCannon()
																			     .doc("_.templateChild(\"template\") create a new box that's a child of this one, copied from 'template'");
	private final Box root;

	public Templates(Box root) {

		this.root = root;

		properties.put(templateChild, (of, name) -> {

			String path = fieldbox.FieldBox.fieldBox.io.findTemplateCalled(name);

			System.err.println(" about to load :" + path);

			Box c = loadBox(path, of.properties.get(Box.frame)
							   .convert(0.9, 0.9));

			System.out.println(" loaded box :" + c + " of class " + c.getClass());

			IO.uniqifyIfNecessary(root, c);

			of.connect(c);

			return c;

		});
	}

	private Box loadBox(String f, Vec2 position) {

		Box b = fieldbox.FieldBox.fieldBox.io.loadSingleBox(f, root);

		Rect fr = b.properties.get(Box.frame);
		fr.x = (float) (position.x - fr.w / 2);
		fr.y = (float) (position.y - fr.h / 2);

		root.connect(b);
		if (b instanceof IO.Loaded) {
			((IO.Loaded) b).loaded();
		}
		Callbacks.load(b);

		Drawing.dirty(b);

		return b;
	}
}
