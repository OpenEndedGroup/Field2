package fieldbox.boxes.plugins;

import field.utility.Dict;
import field.utility.Rect;
import fieldbox.boxes.Box;
import fieldbox.io.IO;

import java.util.Optional;

/**
 * Plugin that gives options for making boxes on the canvas
 */
public class Create extends Box {

	static public final Dict.Prop<TriFunctionOfBoxAnd<String, Class, Box>> _new = new Dict.Prop<>("new").toCanon();
	static public final Dict.Prop<String> tag = new Dict.Prop<>("tag").toCanon();

	static {
		IO.persist(tag);
	}

	public Create(Box root) {
		this.properties.put(_new, (box, tag, clazz) -> {

			Optional<Box> o = box.children()
					     .stream()
					     .filter(x -> isTagged(x, tag))
					     .findAny();

			if (o.isPresent()) return o.get();

			try {
				Box c = (Box) (clazz == null ? Box.class : clazz).newInstance();

				c.properties.put(Box.name, tag);
				Rect parent = box.properties.get(Box.frame);

				c.properties.put(Box.frame, new Rect(parent.x + parent.w - 10, parent.y + parent.h - 10, parent.w, parent.h));
				c.properties.put(Create.tag, tag);
				box.connect(c);
				if (c instanceof IO.Loaded) ((IO.Loaded) c).loaded();

				return c;
			} catch (InstantiationException | IllegalAccessException e) {
				e.printStackTrace();
			}
			return null;

		});
	}

	protected boolean isTagged(Box b, String tag) {
		if (!b.properties.has(Create.tag)) return false;

		return b.properties.get(Create.tag)
				   .endsWith(tag);
	}
}
