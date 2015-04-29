package fieldbox.boxes.plugins;

import field.graphics.FLine;
import field.graphics.StandardFLineDrawing;
import field.linalg.Vec2;
import field.linalg.Vec4;
import field.utility.Dict;
import field.utility.IdempotencyMap;
import field.utility.Rect;
import fieldbox.boxes.*;

import java.util.ArrayList;
import java.util.UUID;

import static field.graphics.StandardFLineDrawing.*;

/**
 */
public class Notifications extends Box {

	static public final Dict.Prop<Box.BiFunctionOfBoxAnd<String, String>> badge = new Dict.Prop<>("badge").toCannon(); // TODO
	static public final Dict.Prop<FunctionOfBoxValued<IdempotencyMap>> badges = new Dict.Prop<>("badges").toCannon(); // TODO

	static public final Dict.Prop<ArrayList<String>> _badgeList = new Dict.Prop<>("_badgeList");

	public Notifications(Box root_unused) {
		this.properties.put(badge, this::badge);
		this.properties.put(badges, x -> {
			IdempotencyMap<String> s = new IdempotencyMap<String>(String.class)
			{
				protected String _put(String key, String v)
				{
					badge(x, v, key, () -> this.remove(key), -1);
					return super._put(key, v);
				}
			};

			return s;
		});
	}



	protected String badge(Box box, String text) {
		String prefix =  UUID.randomUUID()
		    .toString();
		return badge(box, text, prefix, () -> {}, 300);
	}
	protected String badge(Box box, String text, String id, Runnable exit, int duration) {
		box.properties.putToList(_badgeList, id, ArrayList::new);
		box.properties.putToMap(FLineDrawing.frameDrawing, "__badge__" + id, FLineDrawing.expires(x -> {
			int i = box.properties.get(_badgeList)
					      .indexOf(id);
			Rect rect = box.properties.get(frame);
			if (rect == null) return null;

			FLine f = new FLine();
			f.moveTo(rect.x + rect.w , rect.y + rect.h + 12+(i ) * 31 );

			f.attributes.put(hasText, true);
			f.attributes.put(color, new Vec4(1, 1, 1, 0.75f));
			String name = text;

			f.nodes.get(f.nodes.size() - 1).attributes.put(StandardFLineDrawing.text, name);

			return f;

		}, duration, () -> {
			box.properties.removeFromCollection(_badgeList, id);
			exit.run();
		}));

		TextDrawing td = first(TextDrawing.textDrawing, both()).get();
		TextDrawing.FontSupport fs = td.getFontSupport("source-sans-pro-regular-92.fnt");

		box.properties.putToMap(FLineDrawing.frameDrawing, "__nameGlass__"+id, FLineDrawing.expires(x -> {
			int i = box.properties.get(_badgeList)
					      .indexOf(id);
			Rect rect = box.properties.get(frame);
			if (rect == null) return null;

			boolean selected = box.properties.isTrue(Mouse.isSelected, false);

			FLine f = new FLine();

			String name = text;
			if (box.properties.isTrue(FileBrowser.isLinked, false)) name = "{ " + name + " }";
			Vec2 d = fs.font.dimensions(name, 0.15f);

			f.rect((int) (rect.x + rect.w  - d.x / 2 - 10), (int) (rect.y + rect.h + 12 + (i ) * 31  - 36 / 2), (int) d.x + 20, (int) 30);

			f.attributes.put(filled, true);
			f.attributes.put(stroked, false);
			f.attributes.put(fillColor, new Vec4(Colors.statusBarBackground));
			f.attributes.put(strokeColor, new Vec4(0, 0, 0, 1f));

			return f;
		}, duration));

		return id;
	}

}
