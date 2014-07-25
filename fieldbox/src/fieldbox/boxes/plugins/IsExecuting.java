package fieldbox.boxes.plugins;

import field.graphics.FLine;
import field.linalg.Vec4;
import field.utility.*;
import fieldbox.boxes.*;

import java.util.function.BiConsumer;
import java.util.function.Supplier;
import static fieldbox.boxes.StandardFLineDrawing.*;
import static fieldbox.boxes.FLineDrawing.*;

/**
 * Adds: a default decorator for setting a drawer appropriately to give feedback (currently a stripey green frame
 * decoration).
 * <p>
 * This decorator can be retrieved from the property "isExecuting". It has the the signature (box, name) and this
 * decorator stays while there's a callback called name in property Boxes.insideRunLoop
 * <p>
 * see NashornExecution for an example of it's use (other back-ends will need to use this decorator, so it's refactored
 * out here rather than being burried inside NashornExecution).
 */
public class IsExecuting extends Box {

	static public final Dict.Prop<BiConsumer<Box, String>> isExecuting = new Dict.Prop<>("isExecuting").type().toCannon().doc("Given a Box and a name of an run loop, install a drawer that decorates this box (green) while this run loop routine exists");
	static public final Dict.Prop<Integer> executionCount = new Dict.Prop<>("_executionCount").type().toCannon().doc("How many times is this box running? non-zero if this box is currently executing (and is, thus, green).");

	public IsExecuting(Box root_unused) {
		this.properties.put(isExecuting, (box, name) -> {

			box.properties.put(executionCount, Math.max(1, 1 + box.properties.computeIfAbsent(executionCount, (k) -> 0)));

			box.properties.putToMap(frameDrawing, "_animationFeedback_"+name, new Cached<Box, Object, FLine>((b, was) -> {

				Rect rect = box.properties.get(frame);

				if (rect == null) return null;

				Supplier<Boolean> x = b.properties.getFromMap(Boxes.insideRunLoop, name);
				if (x == null) {
					box.properties.put(executionCount, Math.max(0, -1 + box.properties.computeIfAbsent(executionCount, (k) -> 0)));
					Drawing.dirty(this);
					return null;
				}

				if (box.properties.get(executionCount)<1)
				{
					Drawing.dirty(this);
					return null;
				}

				FLine f = new FLine();
				f.rect(rect.x, rect.y, rect.w, rect.h);
				f.attributes.put(filled, true);
				f.attributes.put(fillColor, new Vec4(0.2f, 0.5f, 0.3f, -0.2f));
				f.attributes.put(color, new Vec4(0.2f, 0.5f, 0.3f, 0.8f));

				return f;

			}, (b) -> new Triple(box.properties.get(Boxes.insideRunLoop), b.properties.get(frame), b.properties.getFromMap(Boxes.insideRunLoop, name))));
			Drawing.dirty(box);
		});
	}


}
