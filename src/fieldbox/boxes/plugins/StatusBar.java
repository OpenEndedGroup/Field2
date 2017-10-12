package fieldbox.boxes.plugins;

import field.graphics.FLine;
import field.graphics.StandardFLineDrawing;
import field.linalg.Vec2;
import field.linalg.Vec4;
import field.utility.*;
import fieldbox.boxes.*;
import fieldbox.ui.FieldBoxWindow;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static field.graphics.StandardFLineDrawing.*;

/**
 * Created by marc on 8/8/14.
 */
public class StatusBar extends Box {

	static public Dict.Prop<StatusBar> statusBar = new Dict.Prop<StatusBar>("statusBar").type()
											    .toCanon()
											    .doc("The status-bar plugin");
	static public Dict.Prop<IdempotencyMap<Supplier<String>>> statuses = new Dict.Prop<>("statuses").type()
												     .toCanon()
												     .autoConstructs(() -> new IdempotencyMap<>(Supplier.class))
												     .doc("Add things here to the status bar, and call `_.statusBar.update()` to update/repaint");
	int insetW = 10;
	int insetH = 10;
	int height = 25;

	public StatusBar(Box b) {

		this.properties.put(statusBar, this);

		this.properties.putToMap(Boxes.insideRunLoop, "main.__updateStatusBarWidth__", () -> {

			float w = this.properties.get(Box.frame).w;

			FieldBoxWindow window = find(Boxes.window, both()).findFirst()
									  .get();
			Drawing drawing = find(Drawing.drawing, both()).findFirst()
								       .get();


			float w2 = window.getWidth() - insetW * 2;

			if (Math.abs(w - w2) > 1) {
				this.properties.get(Box.frame).w = w2;
				Drawing.dirty(this);
			}

			float y = this.properties.get(Box.frame).y;
			float h = this.properties.get(Box.frame).h;


			double y2 = window.getHeight()-insetH-h-drawing.getTranslation().y;

			if (Math.abs(y-y2)>1)
			{
				this.properties.get(Box.frame).y = (float) y2;
				Drawing.dirty(this);
			}

			return true;
		});

		this.properties.put(Drawing.windowSpace, new Vec2(0,1));

		this.properties.put(Box.frame, new Rect(insetW, 0, 10, height));

		this.properties.putToMap(FLineDrawing.frameDrawing, "__name__", new Cached<Box, Object, FLine>((box, previously) -> {
			Rect rect = box.properties.get(frame);
			if (rect == null) return null;

			boolean selected = box.properties.isTrue(Mouse.isSelected, false);

			FLine f = new FLine();
			f.moveTo(rect.x + rect.w / 2, rect.y + rect.h / 2 + 25 / 5.0f);

			f.attributes.put(hasText, true);
			f.attributes.put(color, new Vec4(1, 1, 1, 0.25f));
			f.nodes.get(f.nodes.size() - 1).attributes.put(text, statusText());

			return f;
		}, (box) -> new Pair(box.properties.get(frame), statusText())));

		this.properties.putToMap(FLineDrawing.frameDrawing, "__background", new Cached<Box, Object, FLine>((box, previously) -> {
			Rect rect = box.properties.get(frame);
			if (rect == null) return null;

			boolean selected = box.properties.isTrue(Mouse.isSelected, false);

			FLine f = new FLine();
			f.rect(rect);

			f.attributes.put(color, Colors.statusBarBackground);
			f.attributes.put(filled, true);
			f.nodes.get(f.nodes.size() - 1).attributes.put(text, statusText());

			float dd = this.properties.getFloat(depth, 0f);
			if (dd==0)
				f.attributes.put(StandardFLineDrawing.hint_noDepth, true);
			else
				f.nodes.forEach(x -> x.setZ(dd));

			return f;
		}, (box) -> new Pair(box.properties.get(frame), statusText())));


		b.find(Watches.watches, both()).findFirst()
					     .ifPresent(x -> x.addWatch(statuses, q -> {
						     update();
					     }));

		this.properties.putToMap(statuses, "_default_", () -> "[ctrl] menus [alt] execution [command-space] commands [g] attach [n] new");

	}

	String statusText = "";

	private String statusText() {
		return statusText;
	}


	public void update() {
		String s = "";
		List<Map<String, Supplier<String>>> maps = breadthFirst(both()).filter(x -> x.properties.get(statuses) != null)
									       .map(x -> x.properties.get(statuses))
									       .collect(Collectors.toList());
		for (Map<String, Supplier<String>> m : maps) {
			Iterator<Map.Entry<String, Supplier<String>>> ii = m.entrySet()
									    .iterator();
			while (ii.hasNext()) {
				String q = ii.next()
					     .getValue()
					     .get();
				if (q == null) ii.remove();
				else s += (s.length()>0 ? " | " : "")+ q;
			}
		}

		s = s.trim();

		if (!statusText.equals(s)) {
			statusText = s;
			Drawing.dirty(this);
		}

	}
}
