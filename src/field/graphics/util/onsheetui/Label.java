package field.graphics.util.onsheetui;

import field.app.RunLoop;
import field.graphics.FLine;
import field.graphics.StandardFLineDrawing;
import field.linalg.Vec2;
import field.linalg.Vec4;
import field.utility.Dict;
import field.utility.IdempotencyMap;
import fieldbox.boxes.Box;
import fieldbox.boxes.Boxes;
import fieldbox.boxes.FLineDrawing;

import java.util.LinkedHashSet;
import java.util.Set;

import static field.graphics.StandardFLineDrawing.color;
import static field.graphics.StandardFLineDrawing.hasText;

/**
 * plugin for labelling boxes with pieces of text and badges
 *
 */
public class Label extends Box {

	static public Dict.Prop<Box.FunctionOfBoxValued<IdempotencyMap<String>>> label =
		new Dict.Prop<>("label").type().doc("`_.label.n = 'hello world' puts that text on the top (the 'north') of a box — likewise `e,w,s`. ");

	static public Dict.Prop<IdempotencyMap<String>> _label =
		new Dict.Prop<>("_label").type();

	Set<Box> rebuildBox = new LinkedHashSet<>();

	public Label(Box b) {
		this.properties.put(label, box -> box.properties.computeIfAbsent(_label, k -> new IdempotencyMap<String>(String.class) {
			@Override
			protected void _removed(Object v) {
				rebuildBox.add(box);
				super._removed(v);
			}

			@Override
			protected String massageKey(String k) {
				rebuildBox.add(box);
				return super.massageKey(k);
			}
		}));

		this.properties.putToMap(Boxes.insideRunLoop, "main.__labelupdator__", () -> {

			if (rebuildBox.size() > 0) {
				rebuildBox.forEach(this::rebuild);
			}
			rebuildBox.clear();

			return true;
		});
	}

	private void rebuild(Box box) {

		IdempotencyMap<String> ll = box.properties.get(_label);
		if (ll == null) return;

		ll.keySet().forEach(x -> {

			Vec2 origin = originForName(x);
			Vec2 offset = offsetForName(x);
			float align = alignForName(x);

			box.properties.putToMap(FLineDrawing.frameDrawing, "__label__" + x, bx -> FLineDrawing.boxOrigin(() -> {

				FLine f = new FLine();
				f.moveTo(offset);

				f.attributes.put(hasText, true);
				f.attributes.put(color, new Vec4(1, 1, 1, 0.45f));
				String name = ll.get(x);

				f.nodes.get(f.nodes.size() - 1).attributes.put(StandardFLineDrawing.text, name);
				f.nodes.get(f.nodes.size() - 1).attributes.put(StandardFLineDrawing.textAlign, align);
				f.nodes.get(f.nodes.size() - 1).attributes.put(StandardFLineDrawing.textScale, 1.0f);

				return f;
			}, origin, box).get());


		});

	}

	private float alignForName(String x) {
		switch (x) {
			case "e":
				return 0;
			case "w":
				return 1;
			default:
				return 0.5f;

		}
	}

	private Vec2 offsetForName(String x) {

		float O = 10;

		switch (x) {
			case "e":
				return new Vec2(O, 5);
			case "w":
				return new Vec2(-O, 5);
			case "n":
				return new Vec2(0, -O);
			case "s":
				return new Vec2(0, O + 10);
			default:
				return new Vec2();

		}
	}

	private Vec2 originForName(String x) {

		switch (x) {
			case "e":
				return new Vec2(1, 0.5f);
			case "w":
				return new Vec2(0, 0.5f);
			case "n":
				return new Vec2(0.5f, 0);
			case "s":
				return new Vec2(0.5f, 1);
			default:
				return new Vec2();

		}
	}

}
