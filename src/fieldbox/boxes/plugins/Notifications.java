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

	static public final Dict.Prop<Box.BiFunctionOfBoxAnd<String, String>> badge = new Dict.Prop<>("badge").toCanon(); // TODO
	static public final Dict.Prop<FunctionOfBoxValued<IdempotencyMap<String>>> badges = new Dict.Prop<>("badges").toCanon(); // TODO

	static public final Dict.Prop<ArrayList<String>> _badgeList = new Dict.Prop<>("_badgeList");
	static public final Dict.Prop<IdempotencyMap> _badgesList = new Dict.Prop<>("_badgesList");

	public Notifications(Box root_unused) {
		this.properties.put(badge, this::badge);
		this.properties.put(badges, x -> x.properties.computeIfAbsent(_badgesList, (kk) -> {

			IdempotencyMap<String> s = new IdempotencyMap<String>(String.class) {
				public String _put(String key, String v) {
					badge(x, v, key, () -> {
						this.remove(key);
					}, -1);
					String c = super._put(key, v);
					return c;
				}

				@Override
				protected void _removed(Object y) {
					super._removed(y);
					x.properties.removeFromMap(FLineDrawing.frameDrawing, "__badge__" + y);
					x.properties.removeFromMap(FLineDrawing.frameDrawing, "__nameGlass__" + y);
				}

				@Override
				public void clear() {
					keySet().forEach(y -> {
						x.properties.removeFromMap(FLineDrawing.frameDrawing, "__badge__" + y);
						x.properties.removeFromMap(FLineDrawing.frameDrawing, "__nameGlass__" + y);
						x.properties.removeFromCollection(_badgeList, y);
					});
					Drawing.dirty(x);
					super.clear();
				}
			};

			return s;
		}
		));
	}


	protected String badge(Box box, String text) {
		String prefix = UUID.randomUUID()
				    .toString();
		return badge(box, text, prefix, () -> {
		}, 300);
	}

	protected String badge(Box box, String text, String id, Runnable exit, int duration) {
		ArrayList<String> b = box.properties.get(_badgeList);
		if (b == null || !b.contains(id)) box.properties.putToList(_badgeList, id, ArrayList::new);
		box.properties.putToMap(FLineDrawing.frameDrawing, "__badge__" + id, FLineDrawing.expires(x -> {
			int i = box.properties.get(_badgeList)
					      .indexOf(id);
			Rect rect = box.properties.get(frame);
			if (rect == null) return null;

			FLine f = new FLine();
			f.moveTo(rect.x + rect.w, rect.y + rect.h + 12 + (i) * 31);

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

		box.properties.putToMap(FLineDrawing.frameDrawing, "__nameGlass__" + id, FLineDrawing.expires(x -> {
			int i = box.properties.get(_badgeList)
					      .indexOf(id);
			Rect rect = box.properties.get(frame);
			if (rect == null) return null;

			FLine f = new FLine();

			String name = text;
			if (box.properties.isTrue(FileBrowser.isLinked, false)) name = "{ " + name + " }";
			Vec2 d = fs.font.dimensions(name, 0.15f);

			f.rect((int) (rect.x + rect.w - d.x / 2 - 10), (int) (rect.y + rect.h + 12 + (i) * 31 - 36 / 2), (int) d.x + 20, 30);

			f.attributes.put(filled, true);
			f.attributes.put(stroked, false);
			f.attributes.put(fillColor, new Vec4(Colors.statusBarBackground));
			f.attributes.put(strokeColor, new Vec4(0, 0, 0, 1f));

			return f;
		}, duration));

		return id;
	}

}
