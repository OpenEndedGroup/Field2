package fieldbox.boxes.plugins;

import static org.lwjgl.glfw.GLFW.*;
import field.graphics.FLine;
import field.graphics.StandardFLineDrawing;
import field.graphics.Window;
import field.linalg.Vec4;
import field.utility.Cached;
import field.utility.Pair;
import field.utility.Rect;
import fieldbox.boxes.*;
import fieldbox.execution.Execution;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * A port of the old typing plugin for Field. Let's you write code that is dispathced to the selected boxes
 */
public class Typing extends Box {

	private final Box root;

	String ct = "";

	boolean listening = false;

	public Typing(Box root) {
		this.root = root;
		this.properties.put(FLineDrawing.layer, "glass2");
		this.properties.putToMap(FLineDrawing.frameDrawing, "__currenttext__", new Cached<Box, Object, FLine>((b, was) -> {
			FLine f = new FLine();

			if (ct == null) return f;
			if (!listening) return f;

			Optional<Rect> v = getViewRect();
			if (!v.isPresent()) return f;

			Rect r = v.get();

			f.attributes.put(StandardFLineDrawing.font, "source-code-pro-regular-92.fnt");
				    f.attributes.put(FLineDrawing.layer, "glass2");
			f.attributes.put(StandardFLineDrawing.hasText, true);
			f.moveTo(r.x + r.w / 2, r.y + r.h / 2 + 14);
			f.last().attributes.put(StandardFLineDrawing.textScale, 8f);
			f.attributes.put(StandardFLineDrawing.fillColor, new Vec4(1, 1, 1, 1));
			f.last().attributes.put(StandardFLineDrawing.text, ">"+ct);

			return f;
		}, b -> {
			return new Pair<>(listening+ct, getViewRect());
		}));

		this.properties.putToMap(FLineDrawing.frameDrawing, "__currenttextBack__", new Cached<Box, Object, FLine>((b, was) -> {
			FLine f = new FLine();

			if (ct == null) return f;
			if (!listening) return f;

			Optional<Rect> v = getViewRect();
			if (!v.isPresent()) return f;

			Rect r = v.get();

			f.attributes.put(FLineDrawing.layer, "glass2");
			f.rect(r.x + r.w / 2 - 20, r.y + r.h / 2 + 14 - 20, 40, 40);
			f.attributes.put(StandardFLineDrawing.fillColor, new Vec4(0,0,0,0.2));
			f.attributes.put(StandardFLineDrawing.filled, true);
			f.attributes.put(StandardFLineDrawing.stroked, true);

			return f;
		}, b -> {
			return new Pair<>(listening+ct, getViewRect());
		}));

		this.properties.putToMap(Keyboard.onCharTyped, "__typing__", (e, v) -> {

			if (e.properties.isTrue(Window.consumed, false)) return;

			if (listening) {
				ct += v;
				Drawing.dirty(this);
				e.properties.put(Window.consumed, true);
			} else {
				if (v == '\'' ) {
					listening = true;
					ct = "";
					Drawing.dirty(this);
					e.properties.put(Window.consumed, true);
				}
			}
		});

		this.properties.putToMap(Keyboard.onKeyDown, "__typing__", (e, v) -> {

			if (!listening) return null;

			if (v == GLFW_KEY_ENTER)
			{
				try{
					go(ct);
				}
				catch(Throwable t)
				{
					t.printStackTrace();
				}
				ct = "";
				Drawing.dirty(this);
				listening = false;
			}
			if (v==GLFW_KEY_BACKSPACE && ct.length()>0)
			{
				ct = ct.substring(0, ct.length()-1);
				Drawing.dirty(this);
			}
			if (v== GLFW_KEY_ESCAPE)
			{
				listening = false;
				ct = "";
				Drawing.dirty(this);
			}

			return null;
		});

	}

	private void go(String ct) {


		Consumer<Pair<Integer, String>> error = x -> {Drawing.notify(x.second, this, 500);};
		Consumer<String> success = x -> {
			if (x.trim().equals("&#10003;")) x = "[ok]";
			Drawing.notify(x, this, 100);
		};


		selection().forEach(box -> {
			box.first(Execution.execution)
			   .ifPresent(x -> x.support(box, Execution.code)
					    .executeTextFragment(ct, "", success, error));
		});
	}



	private Stream<Box> selection() {
		return breadthFirst(both()).filter(x -> x.properties.isTrue(Mouse.isSelected, false));
	}
	public Optional<Rect> getViewRect() {
		return this.first(Drawing.drawing, both())
			   .map(d -> d.getCurrentViewBounds(this));
	}
}
