package fieldbox.boxes.plugins;

import field.graphics.FLine;
import field.linalg.Vec4;
import field.utility.Cached;
import field.utility.Pair;
import field.utility.Rect;
import fieldbox.boxes.*;
import fieldbox.ui.FieldBoxWindow;

import static fieldbox.boxes.StandardFLineDrawing.color;
import static fieldbox.boxes.StandardFLineDrawing.hasText;
import static fieldbox.boxes.StandardFLineDrawing.text;
import static fieldbox.boxes.StandardFLineDrawing.filled;

/**
 * Created by marc on 8/8/14.
 */
public class StatusBar extends Box {

	int insetW = 10;
	int insetH = 15;
	int height = 25;

	public StatusBar(Box b)
	{
		this.properties.putToMap(Boxes.insideRunLoop, "main.__updateStatusBarWidth__", () -> {

			float w = this.properties.get(Box.frame).w;

			FieldBoxWindow window = find(Boxes.window, both()).findFirst().get();
			Drawing drawing = find(Drawing.drawing, both()).findFirst().get();


			float w2 = window.getWidth()-insetW*2;

			if (Math.abs(w-w2)>1)
			{
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

		this.properties.put(Drawing.windowSpace, true);

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

			f.attributes.put(color, new Vec4(0, 0, 0, 0.25f));
			f.attributes.put(filled, true);
			f.nodes.get(f.nodes.size() - 1).attributes.put(text, statusText());

			return f;
		}, (box) -> new Pair(box.properties.get(frame), statusText())));

	}

	private String statusText() {
		return "-";
	}

}
