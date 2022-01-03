package fieldnashorn;

import field.app.RunLoop;
import field.utility.Pair;
import fieldbox.boxes.Box;
import fieldbox.boxes.FrameManipulation;
import fieldbox.execution.JavaSupport;
import fieldbox.io.IO;
import fielded.EditorUtils;
import fielded.RemoteEditor;
import fielded.boxbrowser.TransientCommands;
import fielded.plugins.Out;
import jdk.dynalink.beans.StaticClass;

import javax.script.Bindings;
import javax.script.ScriptContext;
import java.util.*;
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
	Pattern staticClassIssues = Pattern.compile("StaticClass cannot be cast to java.base/java.lang.Class");
	Pattern notAFunction = Pattern.compile(" ([_a-zA-Z][_a-zA-Z0-9]*) is not a function");
	Pattern syntaxLineCol = Pattern.compile("<<internal>>\\:(.+)\\:");

	Pattern undefined = Pattern.compile("jdk.nashorn.internal.runtime.Undefined");

	public Consumer
		<Pair<Integer, String>> errorHelper(Consumer<Pair<Integer, String>> wrap, Box box) {

		return (line) -> {

			line = replaceBoxReferences(box, line);
			line = replaceMissingRefs(box, line);
			line = replaceStaticClassCasts(box, line);
			line = replaceMissingNew(box, line);
			line = replaceSyntaxLineSpec(box, line);
			line = replaceUndefined(box, line);

			line = new Pair<>(line.first, line.second.trim() + "\n");

			wrap.accept(line);
		};
	}

	private Pair<Integer, String> replaceSyntaxLineSpec(Box box, Pair<Integer, String> line) {
		Matcher m = syntaxLineCol.matcher(line.second);
		if (m.find()) {
			int actualLine = Integer.parseInt(m.group(1));
			if (line.first == -1) {
				return new Pair<Integer, String>(actualLine, line.second.substring(line.second.indexOf(" ")));
			}
		}
		return line;
	}

	private Pair<Integer, String> replaceUndefined(Box box, Pair<Integer, String> line) {
		Matcher m = undefined.matcher(line.second);
		if (m.find()) {
			return new Pair<Integer, String>(line.first, m.replaceAll("Undefined"));
		}
		return line;
	}

	private Pair<Integer, String> replaceStaticClassCasts(Box box, Pair<Integer, String> line) {

		Matcher m = staticClassIssues.matcher(line.second);
		if (m.find()) {
			return new Pair<>(line.first, line.second + "<br>" + "Did you omit a <code>.class</code> suffix?");
		}
		return line;
	}

	private Pair<Integer, String> replaceMissingNew(Box box, Pair<Integer, String> line) {

		Matcher m = notAFunction.matcher(line.second);
		if (m.find()) {
			ScriptContext bindings = box.properties.get(Nashorn.boxBindings);
			Bindings b = bindings.getBindings(100);
			Object found = b.get(m.group(1));
			if (found != null)
				return new Pair<>(line.first, line.second + "<br>" + "Did you mean to write <code>new " + m.group(1) + "</code>?");
		}
		return line;
	}

	private Pair<Integer, String> replaceMissingRefs(Box box, Pair<Integer, String> line) {
		Matcher m = refFinder.matcher(line.second);

		while (m.find()) {
			String found = m.group(0);

			String missing = m.group(1);

			// is there an import which matches exactly?

			List<Pair<String, String>> possible = JavaSupport.javaSupport.getPossibleJavaClassesFor(missing);

			List<Pair<String, String>> matches = possible.stream().filter(x -> {
				String[] f = x.first.split("\\.");
				return f[f.length - 1].toLowerCase().equals(missing.toLowerCase());
			}).collect(Collectors.toList());

			String text = "";
			if (matches.size() != 0) {

				text = line.second + "<br><div class='advice'>Do you mean to import ";
				for (Pair<String, String> p : matches) {
					text += "<br>" + TransientCommands.transientCommands.refForCommand(p.first, () -> {
						EditorUtils ed = box.first(RemoteEditor.editorUtils, box.both()).orElseThrow(() -> new IllegalStateException(" no editortools ? "));
						RunLoop.workerPool.submit(() -> {

							String[] f = p.first.split("[\\.$]");
							String typeName = p.first.replaceAll("$", ".");
							if (typeName.endsWith("."))
								typeName = typeName.substring(0, typeName.length() - 1);
							String insert = "var " + f[f.length - 1] + " = Java.type('" + typeName + "')\\n";
							ed.insertAtStart(insert);

						});
					});
				}

				text += "?<br></div>";
				return new Pair<Integer, String>(line.first, text);
			}

			ScriptContext bindings = box.properties.get(Nashorn.boxBindings);
			List<Pair<String, Object>> possibleNames = new ArrayList<>();
			if (bindings != null) {
				List<String> q = editsOfString(missing.toLowerCase());

				Map<String, String> mm = new LinkedHashMap<>();
				bindings.getBindings(100).forEach((k, v) -> {
					if (k != null && k instanceof String && v != null)
						mm.put(k.toLowerCase(), k);
				});

				for (String qq : q) {
					if (qq == null) continue;
					try {
						String foundKey = mm.get(qq);
						if (foundKey != null) {
							possibleNames.add(new Pair<>(foundKey, bindings.getBindings(100).get(foundKey)));
						}
					}
					// Nashorn has started throwing this for some .get(qq)'s
					catch (IllegalArgumentException e) {
					}
				}
			}

			if (possibleNames.size() > 0 && possibleNames.size() < 10) {
				text = line.second + "<br><div class='advice'><div style='margin-bottom:0px'>Do you mean :</div>";
				for (Pair<String, Object> o : possibleNames) {
					text += "<br><b>" + o.first + "</b> ->" + html(box, o.second);
				}
				text += "?<br></div>";
				return new Pair<Integer, String>(line.first, text);
			}

			text = line.second + "<br><div class='advice'><div style='margin-bottom:0px'>Did you mean to declare this ? If so, use <i>var</i></div></div>";

			return new Pair<Integer, String>(line.first, text);

		}
		return line;
	}

	private String html(Box box, Object second) {

		if (second instanceof StaticClass) {
			return "for example `new " + ((StaticClass) second).getRepresentedClass().getSimpleName() + "`";
		}

		return box.first(Out.__out).map(x -> x.convert(second)).orElseGet(() -> "" + second);
	}

	private Pair<Integer, String> replaceBoxReferences(Box box, Pair<Integer, String> line) {
		Matcher m = boxFinder.matcher(line.second);

		while (m.find()) {
			String found = m.group(0);

			String name = m.group(1);
			String uid = m.group(2);


			Box foundBox = find(box, uid);
			if (foundBox == box) {
				line = new Pair<>(line.first, m.replaceFirst(TransientCommands.transientCommands.refForCommand(name + " (this box)", () -> {
					FrameManipulation.setSelectionTo(box, Collections.singleton(foundBox));
				})));
				return replaceBoxReferences(box, line);
			} else if (foundBox != null) {
				// need a link which is simply "send this message"

				line = new Pair<>(line.first, m.replaceFirst(TransientCommands.transientCommands.refForCommand(name, () -> {
					FrameManipulation.setSelectionTo(box, Collections.singleton(foundBox));
				})));
				return replaceBoxReferences(box, line);
			} else {
				line = new Pair<>(line.first, m.replaceFirst(TransientCommands.transientCommands.refForCommand(name + " (unknown box)", () -> {
				})));
				return replaceBoxReferences(box, line);
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

	public List<String> editsOfString(String n) {

		String alphabet = "abcdefghijklmnopqrstuvwxyz";
		n = n.toLowerCase();

		List<Pair<String, String>> splits = new ArrayList<>();
		for (int i = 0; i < n.length(); i++) {
			splits.add(new Pair<>(n.substring(0, i), n.substring(i, n.length())));
		}
		splits.add(new Pair<>(n, ""));


		Set<String> deletes = new LinkedHashSet<>();
		for (Pair<String, String> p : splits) {
			if (p.second.length() > 0)
				deletes.add(p.first + p.second.substring(1));
		}

		Set<String> transposes = new LinkedHashSet<>();
		for (Pair<String, String> p : splits) {
			if (p.second.length() > 1)
				transposes.add(p.first + p.second.charAt(1) + p.second.charAt(0) + p.second.substring(2));
		}

		Set<String> replaces = new LinkedHashSet<>();
		for (Pair<String, String> p : splits) {
			if (p.second.length() > 0)
				for (int q = 0; q < alphabet.length(); q++)
					replaces.add(p.first + alphabet.charAt(q) + p.second.substring(1));
		}

		Set<String> inserts = new LinkedHashSet<>();
		for (Pair<String, String> p : splits) {
			for (int q = 0; q < alphabet.length(); q++)
				inserts.add(p.first + alphabet.charAt(q) + p.second.substring(0));
		}


		deletes.addAll(transposes);
		deletes.addAll(replaces);
		deletes.addAll(inserts);
		return new ArrayList<>(deletes);
	}


}
