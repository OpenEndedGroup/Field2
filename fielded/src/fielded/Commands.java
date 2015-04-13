package fielded;

import field.utility.Log;
import field.utility.Pair;
import fieldbox.boxes.Box;
import org.json.JSONStringer;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Helper class for the commands system
 */
public class Commands {




	public LinkedHashMap<String, Runnable> callTable = new LinkedHashMap<>();
	public RemoteEditor.ExtendedCommand callTable_alternative = null;

	public void requestCommands(Optional<Box> box, String property, String text, Consumer<String> ret, int line, int ch) {

		// now we need to ask everybody if they have any commands to offer based on the above.

		//todo: handle no box case
		List<Map.Entry<Pair<String, String>, Runnable>> commands
			    = (List<Map.Entry<Pair<String, String>, Runnable>>) box.get()
										   .find(RemoteEditor.commands,
											 box.get()
											    .both())
										   .flatMap(m -> m.get()
												  .entrySet()
												  .stream())
										   .collect(Collectors.toList());

		Log.log("remote.trace", " commands are :" + commands);

		JSONStringer stringer = new JSONStringer();
		stringer.array();
		callTable.clear();
		for (Map.Entry<Pair<String, String>, Runnable> r : commands) {
			String u = UUID.randomUUID()
				       .toString();
			callTable.put(u, r.getValue());
			stringer.object();
			stringer.key("name")
				.value(r.getKey().first);
			stringer.key("info")
				.value(r.getKey().second);
			stringer.key("call")
				.value(u);
			stringer.endObject();
		}


		Log.log("remote.trace", " call table looks like :" + callTable);

		stringer.endArray();

		ret.accept(stringer.toString());

	}

	public RemoteEditor.SupportsPrompt supportsPrompt(Consumer<String> ret) {

		return (prompt, commands1, alternative) -> {
			JSONStringer stringer = new JSONStringer();
			stringer.object();
			stringer.key("prompt");
			stringer.value(prompt);
			stringer.key("commands");
			stringer.array();
			callTable.clear();
			for (Map.Entry<Pair<String, String>, Runnable> r : commands1.entrySet()) {
				String u = UUID.randomUUID()
					       .toString();
				callTable.put(u, r.getValue());
				stringer.object();
				stringer.key("name")
					.value(r.getKey().first);
				stringer.key("info")
					.value(r.getKey().second);
				stringer.key("call")
					.value(u);
				stringer.endObject();
			}
			stringer.endArray();

			if (alternative != null) {
				stringer.key("alternative");
				String u = UUID.randomUUID()
					       .toString();
				callTable_alternative = alternative;
				stringer.value(u);
			} else {
				callTable_alternative = null;
				stringer.key("alternative");
				callTable_alternative = alternative;
				stringer.value(null);
			}
			stringer.endObject();

			ret.accept(stringer.toString());
		};

	}

}
