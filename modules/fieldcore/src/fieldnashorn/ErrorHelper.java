package fieldnashorn;

import field.app.RunLoop;
import field.utility.Pair;
import fieldbox.boxes.Box;
import fieldbox.boxes.FrameManipulation;
import fieldbox.boxes.plugins.PluginUtils;
import fieldbox.execution.JavaSupport;
import fieldbox.io.IO;
import fielded.EditorUtils;
import fielded.RemoteEditor;
import fielded.boxbrowser.TransientCommands;
import fielded.plugins.Out;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Offers help on common errors --- typos, missing imports and the like
 */
public class ErrorHelper {

	public ErrorHelper() {

	}

	Pattern boxFinder = Pattern.compile("bx\\[(.+)\\]/([_0123456789abcdef]+)");
	Pattern refFinder = Pattern.compile("ReferenceError: \"(.+)\" is not defined");

	public Consumer
		<Pair<Integer, String>> errorHelper(Consumer<Pair<Integer, String>> wrap, Box box) {

		return (line) -> {

			// do something smart here

			// let's offer a link for all boxes

			line = replaceBoxReferences(box, line);
			line = replaceMissingRefs(box, line);


			wrap.accept(line);
		};
	}

	private Pair<Integer, String> replaceMissingRefs(Box box, Pair<Integer, String> line) {
		Matcher m = refFinder.matcher(line.second);

		while (m.find()) {
			String found = m.group(0);
			System.out.println(" found :" + found + " in " + box);

			String missing = m.group(1);

			// is there an import which matches exactly?

			List<Pair<String, String>> possible = JavaSupport.javaSupport.getPossibleJavaClassesFor(missing);

			List<Pair<String, String>> matches = possible.stream().filter(x -> {
				String[] f = x.first.split("\\.");
				return f[f.length - 1].toLowerCase().equals(missing.toLowerCase());
			}).collect(Collectors.toList());

			if (matches.size() == 0) return line;

			String text = line.second + "<br>Do you mean to import ";
			for (Pair<String, String> p : matches) {
				text += TransientCommands.transientCommands.refForCommand(p.first, () -> {
					EditorUtils ed = box.first(RemoteEditor.editorUtils, box.both()).orElseThrow(() -> new IllegalStateException(" no editortools ? "));
					RunLoop.workerPool.submit(() -> {

						int s = ed.getCursorPosition();

						String[] f = p.first.split("\\.");
						String insert = "var "+f[f.length - 1]+" = Java.type('"+p.first+"')\\n";
						ed.insertAtStart(insert);

					});
				});

			}

			text+="?<br>";

			return new Pair<Integer, String>(line.first, text);

		}
		return line;
	}

	private Pair<Integer, String> replaceBoxReferences(Box box, Pair<Integer, String> line) {
		Matcher m = boxFinder.matcher(line.second);

		while (m.find()) {
			String found = m.group(0);
			System.out.println(" found :" + found + " in " + box);

			String name = m.group(1);
			String uid = m.group(2);


			Box foundBox = find(box, uid);
			if (foundBox == box) {
				line = new Pair<>(line.first, m.replaceAll(TransientCommands.transientCommands.refForCommand(name + " (this box)", () -> {
					FrameManipulation.setSelectionTo(box, Collections.singleton(foundBox));
				})));
			} else if (foundBox != null) {
				// need a link which is simply "send this message"

				line = new Pair<>(line.first, m.replaceAll(TransientCommands.transientCommands.refForCommand(name, () -> {
					FrameManipulation.setSelectionTo(box, Collections.singleton(foundBox));
				})));
			} else {
				line = new Pair<>(line.first, m.replaceAll(TransientCommands.transientCommands.refForCommand(name + " (unknown box)", () -> {
				})));
			}
		}
		return line;
	}

	private Box find(Box origin, String uid) {
		return origin.breadthFirst(origin.both())
			.filter(x -> x.properties.has(IO.id))
			.filter(x -> x.properties.get(IO.id)
				.equals(uid))
			.findFirst()
			.orElse(null);
	}


}
