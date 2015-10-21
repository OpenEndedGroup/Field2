package fieldbox.boxes.plugins;

import field.utility.Pair;
import fieldbox.boxes.Box;
import fieldbox.boxes.Boxes;
import fieldbox.boxes.Mouse;
import fielded.Commands;
import fielded.RemoteEditor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A handy box for grouping (and naming) a collection of boxes
 */
public class Group extends Box {

	private final Box root;

	public Group(Box root)
	{

		this.root = root;

		properties.put(Commands.commands, () -> {

			Map<Pair<String, String>, Runnable> m = new LinkedHashMap<>();

			long s = selection().count();
			if (s>1)
			{
				m.put(new Pair<>("Make group from selection", "Turns the " + s + " selected boxes into a group &mdash; a handy, named, unit"), new RemoteEditor.ExtendedCommand() {
					public RemoteEditor.SupportsPrompt prompt;

					@Override
					public void begin(RemoteEditor.SupportsPrompt prompt, String alternativeChosen) {
						this.prompt = prompt;
					}

					@Override
					public void run() {
						prompt.prompt("Name for group...", new LinkedHashMap<>(), new RemoteEditor.ExtendedCommand() {
							public String a;
							@Override
							public void begin(RemoteEditor.SupportsPrompt prompt, String alternativeChosen) {
								this.a = alternativeChosen;
							}

							@Override
							public void run() {
								makeSelectionIntoGroup(a);
							}
						});
					}
				});
			}
			return m;

		});
	}

	private void makeSelectionIntoGroup(String a) {
		newGroup(a, selection().collect(Collectors.toList()));
	}

	static public Box newGroup(String a, List<Box> collect) {

		if (collect.size()<1) return null;

		BoxGroup g = new BoxGroup();

		for(Box bb : collect)
		{
			g.connect(bb);
		}

		g.properties.put(Box.name, a);

//		collect.get(0).find(Boxes.root, collect.get(0).both()).findFirst().get().connect(g);

		return g;
	}

	private Stream<Box> selection() {
		return breadthFirst(both()).filter(x -> x.properties.isTrue(Mouse.isSelected, false));
	}

}
