package fieldbox.boxes.plugins;

import field.graphics.FLine;
import field.utility.Cached;
import field.utility.Dict;
import fieldbox.boxes.Box;
import fieldbox.boxes.FLineDrawing;
import fieldbox.boxes.Mouse;

import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * It seems that there's an emerging cluster of plugins that are all interested in updating
 * something when any boxes frame moves or when the selection changes This plugin monitors for that,
 * and sends a message when this "basic hash" of the UI changes.
 */
public class FrameChangedHash extends Box {

	long hashWas = 0;

	static public final Dict.Prop<Long> sceneHash = new Dict.Prop<>("_sceneHash");

	public FrameChangedHash(Box root) {
		properties.put(Planes.plane, "__always__");
		properties.putToMap(FLineDrawing.frameDrawing, "__updateHash__", (box) -> {
			properties.put(sceneHash, hashWas = hash());

			return new FLine();
		});
	}

	private long hash() {
		long h = breadthFirst(both()).filter(x -> x.properties.has(Box.frame))
					     .filter(x -> !x.properties.isTrue(Box.hidden, false))
					     .filter(x -> x.properties.has(Box.name))
					     .filter(x -> !x.properties.isTrue(Mouse.isSticky,
									       false))
					     .reduce(0L,
						     (w, frame) -> 31L * w + (frame.properties.isTrue(
								 Mouse.isSelected,
								 false) ? 1 : 0) + frame.properties.get(
								 Box.frame)
												   .hashCode(),
						     (x, y) -> 31 * x + y);
		return h;
	}


	static public <t_input, t_output> Cached<t_input, Long, t_output> getCached(Box from, BiFunction<t_input, t_output, t_output> work, Supplier<Long> salt) {

		return new Cached<>(work, (t) -> {

			long h = from.find(sceneHash, from.both())
				     .findFirst()
				     .orElse(0L) * 31 + salt.get();

//			System.out.println("SH:"+h);

			return h;
		});
	}

	static public <t_input, t_output> Cached<t_input, Long, t_output> getCached(Box from, BiFunction<t_input, t_output, t_output> work) {
		return getCached(from, work, () -> 0l);
	}
}
