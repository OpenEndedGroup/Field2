package fieldjython;

import field.graphics.FLine;
import field.graphics.RunLoop;
import field.linalg.Vec4;
import field.utility.Cached;
import field.utility.Log;
import field.utility.Pair;
import field.utility.Rect;
import fieldbox.boxes.Box;
import fieldbox.boxes.Drawing;
import fieldbox.boxes.Mouse;
import fielded.RemoteEditor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static field.graphics.StandardFLineDrawing.fillColor;
import static field.graphics.StandardFLineDrawing.hasText;
import static field.graphics.StandardFLineDrawing.text;
import static fieldbox.boxes.FLineDrawing.frameDrawing;

/**
 * Created by marc on 10/23/14.
 */
public class FieldJython extends Box {

	private final JythonExecution jythonExecution;

	public FieldJython(Box root) {
		Log.log("startup.jython", "Jython plugin is starting up ");


		jythonExecution = new JythonExecution();

//		Shims.init();

		root.connect(jythonExecution);

		properties.put(RemoteEditor.commands, () -> {

			Map<Pair<String, String>, Runnable> m = new LinkedHashMap<>();
			List<Box> selected = selection().collect(Collectors.toList());
			if (selected.size() == 1) {
				if (selected.get(0).properties.isTrue(JythonExecution.bridgedToJython, false)) {
					m.put(new Pair<>("Remove bridge to Jython", "No longer will this box execute be written in Jython"), () -> {
						disconnectFromProcessing(selected.get(0));
					});
				} else {
					m.put(new Pair<>("Bridge to Python/Jython", "This box will be written in Python/Jython"), () -> {
						connectToProcessing(selected.get(0));
					});
				}
			}
			return m;
		});


		Log.log("startup.jython", " searching for boxes that need jython support ");

		// we delay this for one update cycle to make sure that everybody has loaded everything that they are going to load
		RunLoop.main.once(() -> {
			root.breadthFirst(both()).forEach(box -> {
				if (box.properties.isTrue(JythonExecution.bridgedToJython, false)) {
					connectToProcessing(box);
				}
			});
		});

		Log.log("startup.jython", "Jython plugin has finished starting up ");

	}


	protected void connectToProcessing(Box box) {
		jythonExecution.connect(box);
		box.properties.put(JythonExecution.bridgedToJython, true);

		box.properties.putToMap(frameDrawing, "_jythonBadge_", new Cached<Box, Object, FLine>((b, was) -> {

			Rect rect = box.properties.get(Box.frame);
			if (rect == null) return null;

			FLine f = new FLine();
			f.attributes.put(hasText, true);
			f.attributes.put(fillColor, new Vec4(0, 0, 0.25f, 0.5f));
			f.moveTo(rect.x + rect.w - 7, rect.y + rect.h - 5);
			f.nodes.get(f.nodes.size() - 1).attributes.put(text, "P");

			return f;

		}, (b) -> new Pair(b.properties.get(JythonExecution.bridgedToJython), b.properties.get(Box.frame))));
		Drawing.dirty(box);

	}

	protected void disconnectFromProcessing(Box box) {
		jythonExecution.disconnect(box);
		box.properties.remove(JythonExecution.bridgedToJython);
		box.properties.removeFromMap(frameDrawing, "_jythonBadge_");
	}

	private Stream<Box> selection() {
		return breadthFirst(both()).filter(x -> x.properties.isTrue(Mouse.isSelected, false));
	}

}

