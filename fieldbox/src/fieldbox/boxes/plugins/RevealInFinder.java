package fieldbox.boxes.plugins;

import apple.applescript.AppleScriptEngineFactory;
import field.utility.Pair;
import fieldagent.Main;
import fieldbox.FieldBox;
import fieldbox.Open;
import fieldbox.boxes.Box;
import fieldbox.boxes.Mouse;
import fielded.RemoteEditor;

import javax.script.ScriptException;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Code for showing files in the finder
 */
public class RevealInFinder extends Box {

	public RevealInFinder(Box root) {
		if (!Main.os.equals(Main.OS.mac)) return;

		root.properties.put(RemoteEditor.commands, () -> {

			Map<Pair<String, String>, Runnable> r = new HashMap<>();

			List<Box> c = selection().collect(Collectors.toList());
			if (c.size() > 0) {

				String title = "Reveal box in Finder";
				String desc = "";
				if (c.size() > 1) desc = "Select these " + c.size() + " boxes in Finder";
				else desc = "Select this box called '" + c.get(0).properties.get(Box.name) + "' in Finder";

				r.put(new Pair<>(title, desc), () -> {
					revealInFinder(c, null);
				});
			}

			String title = "Reveal all files in Finder";
			String desc = "Select all the files for this document in Finder";

			r.put(new Pair<>(title, desc), this::revealAllInFinder);

			return r;
		});
	}


	private void revealAllInFinder() {

		List<Box> l = this.breadthFirst(both())
					.filter(x -> x.properties.has(Box.name) && x.properties.has(Box.frame))
					.collect(Collectors.toList());

		revealInFinder(l, Collections.singletonList(FieldBox.fieldBox.io.filenameFor(FieldBox.fieldBox.io.WORKSPACE + "/" + find(Open.fieldFilename, both()).findFirst().get()).getAbsolutePath()));


	}
	private void revealInFinder(List<Box> c, List<String> and) {

		List<String> names = c.stream()
				      .flatMap(k -> k.properties.getMap()
								.entrySet()
								.stream()
								.map(kk -> {
									if (kk.getKey()
									      .getName()
									      .startsWith("__filename__") || kk.getKey()
													       .getName()
													       .startsWith("__datafilename__")) {
										return kk.getValue();
									}
									return null;
								})
								.filter(x -> x != null))
				      .map(x -> FieldBox.fieldBox.io.filenameFor(x.toString())
								    .getAbsolutePath())
				      .collect(Collectors.toList());

		Map<String, List<String>> dirs = new HashMap<>();

		if (and!=null)
		names.addAll(and);

		names.stream()
		     .forEach(x -> {
			     File f = new File(x);
			     String p = f.getParent();

			     dirs.computeIfAbsent(p, (k) -> new ArrayList<String>())
				 .add(f.getName());
		     });

		dirs.entrySet().forEach(e -> revealInFinder(e.getKey(), e.getValue()));
	}

	private void revealInFinder(String dir, List<String> names) {

		List<String> c = names.stream()
					    .map(x -> "\"" + x + "\"")
					    .collect(Collectors.toList());

		String theApplescript = "set p to \"" +dir+"\"\n" +
			    "set my_list to {"+String.join(",", c)+"}\n" +
			    "tell application \"Finder\"\n" +
			    "\tset f to reveal folder (POSIX file p as text)\n" +
			    "\tselect (every item of f whose name is in my_list)\n" +
			    "activate\n"+
			    "end tell\n";

		System.out.println(" executing\n" + theApplescript);

		try {
			Object r = new AppleScriptEngineFactory().getScriptEngine()
								    .eval(theApplescript);
		} catch (ScriptException e) {
			e.printStackTrace();
		}

	}


	private Stream<Box> selection() {
		return breadthFirst(both()).filter(x -> x.properties.isTrue(Mouse.isSelected, false));
	}

}
