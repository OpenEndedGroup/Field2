package fieldbox.boxes.plugins;

import com.google.common.reflect.Invokable;
import com.sun.corba.se.impl.activation.CommandHandler;
import field.utility.Dict;
import field.utility.Log;
import field.utility.Pair;
import fieldbox.boxes.Box;
import fielded.Commands;
import fielded.RemoteEditor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Plugin offers _.runCommand("match") and associated functionality.
 *
 * Note we return boolean as to whether matches were found and ran, but Nashorn right now just eats the return value and turns it into Undefined
 *
 * We need a way of chaining these things together, but it's hard to do without access to the return value of runCommand...
 */
public class RunCommand extends Box {

	static public final Dict.Prop<BiFunctionOfBoxAnd<String, Boolean>> runCommand = new Dict.Prop<>("runCommand").doc("_.runCommand(x) runs commands for box '_' that match string 'x'")
															      .type()
															      .toCannon();

	public RunCommand(Box root) {
		this.properties.put(runCommand, this::run);
	}

	protected boolean run(Box box, String of) {

		Log.log("run.command", box + " " + of);

		Pattern p = Pattern.compile(of);
		List<Map.Entry<Pair<String, String>, Runnable>> commands = box.find(RemoteEditor.commands, box.both())
									      .flatMap(m -> m.get()
											     .entrySet()
											     .stream())
									      .map(x -> {
										      Log.log("run.command", "found :" + x);
										      return x;
									      })
									      .filter(x -> p.matcher(x.getKey().first)
											    .matches())
									      .map(x -> {
										      Log.log("run.command", "filtered :" + x);
										      return x;

									      })
									      .collect(Collectors.toList());

		Log.log("run.command", "command size is " + commands.size());
		if (commands.size() == 0) return false;

		// Nashorn doesn't like a lambda here

		commands.forEach(r -> {
			if (r.getValue() instanceof RemoteEditor.ExtendedCommand) {
				((RemoteEditor.ExtendedCommand) r.getValue()).begin((pr, o, a) -> {
					a.begin(null, null); // SHOULD BE AN ARG
					a.run();
				}, null);
				((RemoteEditor.ExtendedCommand) r.getValue()).run();
			} else {
				r.getValue()
				 .run();
			}
		});
		return true;
	}


}


