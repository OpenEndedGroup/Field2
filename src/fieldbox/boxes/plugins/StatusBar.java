package fieldbox.boxes.plugins;

import field.graphics.FLine;
import field.graphics.StandardFLineDrawing;
import field.linalg.Vec2;
import field.linalg.Vec4;
import field.utility.*;
import fieldbox.boxes.*;
import fieldbox.ui.FieldBoxWindow;

import java.util.*;
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
	int insetW = 0;
	int insetH = 0;
	int height = 25;

	public StatusBar(Box b) {

		this.properties.put(Planes.plane, "__always__");
		this.properties.put(statusBar, this);

		this.properties.putToMap(Boxes.insideRunLoop, "main.__updateStatusBarWidth__", () -> {

			Optional<FieldBoxWindow> window = find(Boxes.window, both()).findFirst();
			if (window.isPresent()) {
				this.properties.put(frame, new Rect(0, window.get().getHeight() - height, window.get().getWidth(), height));
				return false;
			}

			return true;
		});

		this.properties.put(Drawing.windowSpace, new Vec2(0, 1));
		this.properties.put(Drawing.windowScale, new Vec2(1, 1));

		this.properties.put(Box.frame, new Rect(0, 0, 10, height));

		this.properties.putToMap(FLineDrawing.frameDrawing, "__name__", new Cached<Box, Object, FLine>((box, previously) -> {
			Rect rect = box.properties.get(frame);
			if (rect == null) return null;

			FLine f = new FLine();
			f.moveTo(rect.x + rect.w / 2, rect.y + rect.h / 2 + 25 / 5.0f);

			f.attributes.put(hasText, true);
			f.attributes.put(color, new Vec4(1, 1, 1, 0.25f));
			List<String> spans = Arrays.asList(statusText().split(" "));
			f.nodes.get(f.nodes.size() - 1).attributes.put(textSpans, spans);
			List<Vec4> colors = new ArrayList<>();
			for (int i = 0; i < spans.size(); i++) {
				if (i % 2 == 0)
					colors.add(new Vec4(1, 0.9, 0.8, 0.3f));
				else
					colors.add(new Vec4(1, 0.9, 0.8, 0.4f));
			}
			f.nodes.get(f.nodes.size() - 1).attributes.put(textColorSpans, colors);
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
			if (dd == 0)
				f.attributes.put(StandardFLineDrawing.hint_noDepth, true);
			else
				f.nodes.forEach(x -> x.setZ(dd));

			return f;
		}, (box) -> new Pair(box.properties.get(frame), statusText())));


		b.find(Watches.watches, both()).findFirst()
			.ifPresent(x -> x.addWatch(statuses, q -> {
				update();
			}));

		this.properties.putToMap(statuses, "_default_", () -> " control / right-click menus option / alt execution / ctrl-space commands g attach n new");

	}

	String statusText = "";

	private String statusText() {
		return statusText;
	}


	public void update() {
		String s = "[ alpha.17 ] ";
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
				else s += (s.length() > 0 ? " | " : "") + q;
			}
		}

		s = s.trim();

		if (!statusText.equals(s)) {
			statusText = s;
			Drawing.dirty(this);
		}

	}
}
