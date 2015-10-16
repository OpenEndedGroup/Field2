package fieldbox.boxes.plugins;

import field.utility.Dict;
import field.utility.Log;
import field.utility.Triple;
import fieldbox.boxes.Box;
import fielded.Commands;
import fielded.RemoteEditor;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Plugin offers _.runCommand("match") and associated functionality.
 *
 * Note we return boolean as to whether matches were found and ran, but Nashorn right now just eats the return value and turns it into Undefined
 *
 * We need a way of chaining these things together, but it's hard to do without access to the return value of runCommand...
 */
public class RunCommand extends Box {

	static public final Dict.Prop<BiFunctionOfBoxAnd<String, Boolean>> runCommand = new Dict.Prop<>("runCommand").doc("`_.runCommand(x)` runs commands for box `_` that match string `x`")
															      .type()
															      .toCannon();

	public RunCommand(Box root) {
		this.properties.put(runCommand, this::run);
	}

	protected boolean run(Box box, String of) {

		Log.log("run.command",()-> box + " " + of);

		Pattern p = Pattern.compile(of);
		List<Triple<String, String, Runnable>> commands = Commands.getCommandsAndDocs(box);

		Log.log("run.command", ()->"command size is " + commands.size());
		if (commands.size() == 0) return false;

		// Nashorn doesn't like a lambda here

		boolean[] found  = {false};

		commands.stream().filter(x -> stripFormatting(x.first.toLowerCase()).equals(of.toLowerCase())).forEach(r -> {
			if (r.third instanceof RemoteEditor.ExtendedCommand) {
				((RemoteEditor.ExtendedCommand) r.third).begin((pr, o, a) -> {
					a.begin(null, null); // SHOULD BE AN ARG
					a.run();
					found[0] = true;
				}, null);
				((RemoteEditor.ExtendedCommand) r.third).run();
			} else {
				r.third.run();
				found[0] = true;
			}
		});
		return found[0];
	}

	private String stripFormatting(String s) {
		return s.replaceAll("<[^>]*>", "");
	}


}


