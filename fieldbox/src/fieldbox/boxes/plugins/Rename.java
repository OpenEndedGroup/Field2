package fieldbox.boxes.plugins;

import field.utility.Pair;
import fieldbox.boxes.Box;
import fieldbox.boxes.Drawing;
import fieldbox.boxes.Mouse;
import fielded.RemoteEditor;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Adds: a command to rename a box
 * <p>
 * TODO: ideally we'd have both a prompt and some placeholder text TODO: specifying chained "parameterized" commands such as this ought to be more
 * straightforward.
 */
public class Rename extends Box {

	public Rename(Box root_unused) {
		properties.put(RemoteEditor.commands, () -> {

			Map<Pair<String, String>, Runnable> m = new LinkedHashMap<>();
			m.put(new Pair<>("Rename box", "Sets name of box"), new RemoteEditor.ExtendedCommand() {

				public RemoteEditor.SupportsPrompt p;

				@Override
				public void begin(RemoteEditor.SupportsPrompt prompt, String alternativeChosen/*, Consumer<String> feedback*/) {
					this.p = prompt;
				}

				@Override
				public void run() {

					Map<Pair<String, String>, Runnable> m = new LinkedHashMap<>();

					p.prompt("rename box to...", m, new RemoteEditor.ExtendedCommand() {
						String altWas = null;
						Consumer<String> feedback;

						@Override
							public void begin(RemoteEditor.SupportsPrompt prompt, String alternativeChosen/*, Consumer<String> feedback*/) {
							altWas = alternativeChosen;
							this.feedback = feedback;
						}

						@Override
						public void run() {
							if (altWas != null) selection().forEach(x -> {
								x.properties.put(Box.name, altWas);
								Drawing.dirty(x);
								feedback.accept("Renamed to \"" + altWas + "\"");
							});
						}
					});
				}
			});


			return m;
		});
	}

	private Stream<Box> selection() {
		return breadthFirst(both()).filter(x -> x.properties.isTrue(Mouse.isSelected, false));
	}


}
