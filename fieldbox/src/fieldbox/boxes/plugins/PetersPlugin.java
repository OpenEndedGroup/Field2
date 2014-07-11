package fieldbox.boxes.plugins;

import field.utility.Pair;
import fieldbox.boxes.Box;
import fieldbox.boxes.Drawing;
import fieldbox.boxes.Mouse;
import fielded.RemoteEditor;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Adds: a command to rename a box
 *
 * TODO: ideally we'd have both a prompt and some placeholder text
 * TODO: specifying chained "parameterized" commands such as this ought to be more straightforward.
 */
public class PetersPlugin extends Box {

	public PetersPlugin(Box root_unused) {
		properties.put(RemoteEditor.commands, () -> {

			Map<Pair<String, String>, Runnable> m = new LinkedHashMap<>();
			m.put(new Pair<>("Test Plugin", "Peter's Test Plugin"), new RemoteEditor.ExtendedCommand() {

				public RemoteEditor.SupportsPrompt p;

				@Override
				public void begin(RemoteEditor.SupportsPrompt prompt, String alternativeChosen) {
					System.out.println("jkaldjfsajdflka");
				}

				@Override
				public void run() {
					if(false){
						System.out.println("Don't Run Me");
					}
				}
			});


			return m;
		});
	}

	private Stream<Box> selection() {
		return breadthFirst(both()).filter(x -> x.properties.isTrue(Mouse.isSelected, false));
	}


}
