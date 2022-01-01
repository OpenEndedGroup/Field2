package fieldbox.boxes.plugins;

import com.google.common.io.Files;
import field.utility.Pair;
import field.utility.SimpleCommand;
import fieldagent.Main;
import fieldbox.FieldBox;
import fieldbox.Open;
import fieldbox.boxes.Box;
import fieldbox.boxes.Mouse;
import fieldbox.io.IO;
import fielded.Commands;

import java.io.File;
import java.nio.charset.Charset;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Code for showing files in the finder
 */
public class RevealInFinder extends Box {

	public RevealInFinder(Box root) {
		if (!Main.os.equals(Main.OS.mac)) return;

		root.properties.put(Commands.commands, () -> {

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

		revealInFinder(l, Collections.singletonList(FieldBox.fieldBox.io.filenameFor(IO.WORKSPACE + "/" + find(Open.fieldFilename, both()).findFirst().get()).getAbsolutePath()));


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

		String theApplescript ="set myValues to {"+String.join(",", c)+"}\n" +
			    "set p to \""+dir+"\"\n" +
			    "tell application \"Finder\" to reveal folder (POSIX file p as text)\n" +
			    "\n" +
			    "tell application \"Finder\" to set fileList to files of target of front Finder window as alias list\n" +
			    "set matchedFiles to {}\n" +
			    "repeat with aFile in my fileList\n" +
			    "\trepeat with aValue in myValues\n" +
			    "\t\ttell application \"System Events\" to if aFile's name contains (contents of aValue) then set end of matchedFiles to (aFile as text)\n" +
			    "\tend repeat\n" +
			    "end repeat\n" +
			    "\n" +
			    "if matchedFiles ≠ {} then\n" +
			    "\ttell application \"Finder\"\n" +
			    "\t\tselect matchedFiles\n" +
			    "\tend tell\n" +
			    "end if";


		try {

			File tmp = File.createTempFile("field",".applescript");
			tmp.deleteOnExit();
			Files.write(theApplescript, tmp, Charset.defaultCharset());

			SimpleCommand.go(new File("."), "/usr/bin/osascript", tmp.getAbsolutePath());
//			Object r = new AppleScriptEngineFactory().getScriptEngine()
//								    .eval(theApplescript);

//			ScriptEngine engine = new ScriptEngineManager().getEngineByName("AppleScript");
//			engine.eval(theApplescript);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}


	private Stream<Box> selection() {
		return breadthFirst(both()).filter(x -> x.properties.isTrue(Mouse.isSelected, false));
	}

}
